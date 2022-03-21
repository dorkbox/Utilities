package dorkbox.util.crypto

import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.util.*
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.jvm.Throws


// https://stackoverflow.com/questions/11783062/how-to-decrypt-file-in-java-encrypted-with-openssl-command-using-aes/11786924#11786924
internal object OpenSSLPBECommon {
    const val SALT_SIZE_BYTES = 8
    const val OPENSSL_HEADER_STRING = "Salted__"
    val OPENSSL_HEADER_STRING_BYTES = OPENSSL_HEADER_STRING.toByteArray(Charsets.US_ASCII)

    private val hashDigest = MessageDigest.getInstance("SHA-256")

    fun toByteArray(chars: CharArray): ByteArray {
        val bytes = ByteArray(chars.size)

        for (i in bytes.indices) {
            bytes[i] = chars[i].toByte()
        }

        return bytes
    }

    @Throws(NoSuchAlgorithmException::class,
            InvalidKeySpecException::class,
            InvalidKeyException::class,
            NoSuchPaddingException::class,
            InvalidAlgorithmParameterException::class
           )
    fun initializeCipher(password: String, salt: ByteArray, cipherMode: Int): Cipher {
        val passwordBytes = password.toByteArray(Charsets.US_ASCII)

        var hash = ByteArray(0)
        var keyAndIV = ByteArray(0)

        hashDigest.update(passwordBytes)
        hashDigest.update(salt)

        hash = hashDigest.digest()
        keyAndIV = hash.clone()

        // 1 round
        hashDigest.update(hash)
        hashDigest.update(passwordBytes)
        hashDigest.update(salt)

        hash = hashDigest.digest()
        keyAndIV = concat(keyAndIV, hash)

        val keyBytes = Arrays.copyOfRange(keyAndIV, 0, 32)
        val ivBytes = Arrays.copyOfRange(keyAndIV, 32, 48)

        val key = SecretKeySpec(keyBytes, "AES")
        val iv = IvParameterSpec(ivBytes)


        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(cipherMode, key, iv);

        return cipher
    }

    private
    fun concat(a: ByteArray, b: ByteArray): ByteArray {
        val c = ByteArray(a.size + b.size)
        System.arraycopy(a, 0, c, 0, a.size)
        System.arraycopy(b, 0, c, a.size, b.size)
        return c
    }
}
