package dorkbox.util.crypto

import java.io.IOException
import java.io.OutputStream
import java.security.InvalidAlgorithmParameterException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import kotlin.jvm.Throws

// https://stackoverflow.com/questions/11783062/how-to-decrypt-file-in-java-encrypted-with-openssl-command-using-aes/11786924#11786924
class OpenSSLPBEOutputStream @Throws(IOException::class)
constructor(private val outStream: OutputStream, password: String) : OutputStream() {
    companion object {
        private const val BUFFER_SIZE = 5 * 1024 * 1024
    }

    private val cipher: Cipher
    private val buffer = ByteArray(BUFFER_SIZE)
    private var bufferIndex: Int = 0

    init {
        try {
            // Create and use a random SALT for each instance of this output stream.
            val salt = ByteArray(OpenSSLPBECommon.SALT_SIZE_BYTES)
            val secureRandom = SecureRandom()
            secureRandom.nextBytes(salt)
            cipher = OpenSSLPBECommon.initializeCipher(password, salt, Cipher.ENCRYPT_MODE)

            // Write header
            outStream.write(OpenSSLPBECommon.OPENSSL_HEADER_STRING_BYTES)
            outStream.write(salt)
        }
        catch (e: InvalidKeySpecException) {
            throw IOException(e)
        }
        catch (e: NoSuchPaddingException) {
            throw IOException(e)
        }
        catch (e: NoSuchAlgorithmException) {
            throw IOException(e)
        }
        catch (e: InvalidAlgorithmParameterException) {
            throw IOException(e)
        }
    }

    @Throws(IOException::class)
    override fun write(b: Int) {
        buffer[bufferIndex] = b.toByte()
        bufferIndex++

        // only update the digest and write out the buffer if it's enough (this is a slow operation)
        if (bufferIndex == BUFFER_SIZE) {
            val result = cipher.update(buffer, 0, bufferIndex)
            outStream.write(result)
            bufferIndex = 0
        }
    }

    @Throws(IOException::class)
    override fun flush() {
        if (bufferIndex > 0) {
            val result: ByteArray
            try {
                result = cipher.doFinal(buffer, 0, bufferIndex)
                outStream.write(result)
            }
            catch (e: IllegalBlockSizeException) {
                throw IOException(e)
            }
            catch (e: BadPaddingException) {
                throw IOException(e)
            }

            bufferIndex = 0
        }
    }

    @Throws(IOException::class)
    override fun close() {
        flush()
        outStream.close()
    }
}
