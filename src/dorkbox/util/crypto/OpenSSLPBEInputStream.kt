package dorkbox.util.crypto

import java.io.IOException
import java.io.InputStream
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import kotlin.experimental.and
import kotlin.jvm.Throws

// https://stackoverflow.com/questions/11783062/how-to-decrypt-file-in-java-encrypted-with-openssl-command-using-aes/11786924#11786924
class OpenSSLPBEInputStream @Throws(IOException::class)
constructor(private val inStream: InputStream, password: String) : InputStream() {
    companion object {
        private const val READ_BLOCK_SIZE = 64 * 1024
    }

    private val cipher: Cipher
    private val bufferCipher = ByteArray(READ_BLOCK_SIZE)

    private var bufferClear: ByteArray? = null

    private var index = Integer.MAX_VALUE
    private var maxIndex = 0

    init {
        try {
            val salt = readSalt()
            cipher = OpenSSLPBECommon.initializeCipher(password, salt, Cipher.DECRYPT_MODE)
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
        catch (e: InvalidKeyException) {
            throw IOException(e)
        }
        catch (e: InvalidAlgorithmParameterException) {
            throw IOException(e)
        }
    }

    @Throws(IOException::class)
    private fun readSalt(): ByteArray {

        val headerBytes = ByteArray(OpenSSLPBECommon.OPENSSL_HEADER_STRING.length)
        inStream.read(headerBytes)
        val headerString = String(headerBytes, Charsets.US_ASCII)

        if (OpenSSLPBECommon.OPENSSL_HEADER_STRING != headerString) {
            throw IOException("unexpected magic bytes $headerString")
        }

        val salt = ByteArray(OpenSSLPBECommon.SALT_SIZE_BYTES)
        inStream.read(salt)

        return salt
    }

    @Throws(IOException::class)
    override fun read(): Int {
        if (index > maxIndex) {
            index = 0
            val read = inStream.read(bufferCipher)
            if (read != -1) {
                bufferClear = cipher.update(bufferCipher, 0, read)
            }
            if (read == -1 || bufferClear == null || bufferClear!!.isEmpty()) {
                try {
                    bufferClear = cipher.doFinal()
                }
                catch (e: IllegalBlockSizeException) {
                    bufferClear = null
                }
                catch (e: BadPaddingException) {
                    bufferClear = null
                }

            }
            if (bufferClear == null || bufferClear!!.isEmpty()) {
                return -1
            }
            maxIndex = bufferClear!!.size - 1
        }

        return (bufferClear!![index++] and 0xFF.toByte()).toInt()
    }

    @Throws(IOException::class)
    override fun available(): Int {
        return inStream.available()
    }
}
