package dorkbox.util.crypto

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object OpenSSLDecryptor {
    private val INDEX_KEY = 0
    private val INDEX_IV = 1
    private val ITERATIONS = 1

    private val ARG_INDEX_FILENAME = 0
    private val ARG_INDEX_PASSWORD = 1

    private val SALT_OFFSET = 8
    private val SALT_SIZE = 8
    private val CIPHERTEXT_OFFSET = SALT_OFFSET + SALT_SIZE

    private val KEY_SIZE_BITS = 256

    /**
     * Thanks go to Ola Bini for releasing this source on his blog.
     * The source was obtained from [here](http://olabini.com/blog/tag/evp_bytestokey/) .
     */
    fun EVP_BytesToKey(key_len: Int, iv_len: Int, md: MessageDigest, salt: ByteArray?, data: ByteArray?, count: Int): Array<ByteArray> {
        val key = ByteArray(key_len)
        var key_ix = 0

        val iv = ByteArray(iv_len)
        var iv_ix = 0

        val both = arrayOf(key, iv)
        if (data == null) {
            return both
        }

        var md_buf: ByteArray? = null
        var nkey = key_len
        var niv = iv_len
        var i: Int

        var addmd = 0


        while (true) {
            md.reset()
            if (addmd++ > 0) {
                md.update(md_buf!!)
            }

            md.update(data)
            if (null != salt) {
                md.update(salt, 0, 8)
            }

            md_buf = md.digest()
            i = 1
            while (i < count) {
                md.reset()
                md.update(md_buf!!)
                md_buf = md.digest()
                i++
            }

            i = 0
            if (nkey > 0) {
                while (true) {
                    if (nkey == 0) {
                        break
                    }
                    if (i == md_buf!!.size) {
                        break
                    }
                    key[key_ix++] = md_buf[i]
                    nkey--
                    i++
                }
            }

            if (niv > 0 && i != md_buf!!.size) {
                while (true) {
                    if (niv == 0) {
                        break
                    }
                    if (i == md_buf.size) {
                        break
                    }
                    iv[iv_ix++] = md_buf[i]
                    niv--
                    i++
                }
            }
            if (nkey == 0 && niv == 0) {
                break
            }
        }

        i = 0
        while (i < md_buf!!.size) {
            md_buf[i] = 0
            i++
        }

        return both
    }


    @JvmStatic
    fun main(args: Array<String>) {
        // /usr/bin/openssl enc -d -aes-256-cbc -md sha256 -in update_file.encrypted -out update_file.bin -pass pass:xyfjWNl6yPIZfRYLu64L2sleiF8vD5xgHsJ3sa3Ya6sY01

        // NON-PBKDF2, WITH BASE64
        // openssl enc -aes-256-cbc -a -salt -md sha256 -in password.txt -out password.txt.enc -pass pass:xyfjWNl6yPIZfRYLu64L2sleiF8vD5xgHsJ3sa3Ya6sY01
        // openssl enc -aes-256-cbc -a -d -md sha256 -in password.txt.enc -out password.txt.new -pass pass:xyfjWNl6yPIZfRYLu64L2sleiF8vD5xgHsJ3sa3Ya6sY01 && cat password.txt.new

        // NON-PBKDF2, WITHOUT BASE64
        // openssl enc -aes-256-cbc -salt -md sha256 -in password.txt -out password.txt.enc -pass pass:xyfjWNl6yPIZfRYLu64L2sleiF8vD5xgHsJ3sa3Ya6sY01
        // openssl enc -aes-256-cbc -d -md sha256 -in password.txt.enc -out password.txt.new -pass pass:xyfjWNl6yPIZfRYLu64L2sleiF8vD5xgHsJ3sa3Ya6sY01 && cat password.txt.new



        // PBKDF2 (NOT WORKING)
        // openssl aes-256-cbc -a -salt -pbkdf2 -iter 1 -md sha256 -in password.txt -out password.txt.enc -pass pass:xyfjWNl6yPIZfRYLu64L2sleiF8vD5xgHsJ3sa3Ya6sY01
        // openssl aes-256-cbc -d -a -md sha256 -pbkdf2 -iter 1 -in password.txt.enc -out password.txt.new -pass pass:xyfjWNl6yPIZfRYLu64L2sleiF8vD5xgHsJ3sa3Ya6sY01 && cat password.txt.new

        val decrypt = false
        try {
            val password = "xyfjWNl6yPIZfRYLu64L2sleiF8vD5xgHsJ3sa3Ya6sY01"
            val passwordBytes = password.toByteArray(Charsets.US_ASCII)



            // openssl enc -aes-256-cbc -d -md sha256 -in install_2019.1.bin.enc -out install_2019.1.bin.new -pass pass:xyfjWNl6yPIZfRYLu64L2sleiF8vD5xgHsJ3sa3Ya6sY01
//            val plaintextFileName = "build/install_2019.1.bin"
//            val encryptedFileName = "build/install_2019.1.bin.enc"
//            val fileOutputStream = FileOutputStream(File(encryptedFileName))
//            OpenSSLPBEOutputStream(fileOutputStream, password).use { outputStream ->
//                Files.copy(Path.of(plaintextFileName), outputStream)
//            }
//
//            if (true) {
//                return
//            }


            val plaintextFileName = "password.txt"
            val encryptedFileName = "password.txt.enc"
            if (decrypt) {
                // --- read base 64 encoded file ---

                val encryptedFile = File(encryptedFileName).absoluteFile

                // this is WITH BASE64 (with openssl -a)
//                val lines = Files.readAllLines(encryptedFile.toPath(), Charsets.US_ASCII)
//                val sb = StringBuilder()
//                for (line in lines) {
//                    sb.append(line.trim())
//                }
//                val dataBase64 = sb.toString()
//                val headerSaltAndCipherText = Base64.getDecoder().decode(dataBase64) // when base64 encoded

                // this is NOT base64
                val headerSaltAndCipherText = encryptedFile.readBytes()

                // --- extract salt & encrypted ---

                // header is "Salted__", ASCII encoded, if salt is being used (the default)
                val salt = Arrays.copyOfRange(headerSaltAndCipherText, SALT_OFFSET, SALT_OFFSET + SALT_SIZE)
                val encrypted = Arrays.copyOfRange(headerSaltAndCipherText, CIPHERTEXT_OFFSET, headerSaltAndCipherText.size)

                // --- specify cipher and digest for EVP_BytesToKey method ---

                val aesCBC = Cipher.getInstance("AES/CBC/PKCS5Padding")
                val md = MessageDigest.getInstance("SHA-256")

                // --- create key and IV  ---

                // the IV is useless, OpenSSL might as well have use zero's
//            val keyAndIV = EVP_BytesToKey(KEY_SIZE_BITS / java.lang.Byte.SIZE, aesCBC.blockSize, md, salt, passString.toByteArray(ASCII), ITERATIONS)
//            val key = SecretKeySpec(keyAndIV[INDEX_KEY], "AES")
//            val iv = IvParameterSpec(keyAndIV[INDEX_IV])
/////////////////////
//                val openssl = OpenSSLPBEParametersGenerator()
//                openssl.init(passString.toByteArray(Charsets.US_ASCII), salt)
//                val keyAndIV = openssl.generateDerivedParameters(256)
//
//                val keyBytes = Arrays.copyOfRange(keyAndIV, 0, 32)
//                val ivBytes = Arrays.copyOfRange(keyAndIV, 32, 48)
//
//                val key = SecretKeySpec(keyBytes, "AES")
//                val iv = IvParameterSpec(ivBytes)
//////////////////////
                md.update(passwordBytes)
                md.update(salt)

                var hash = md.digest()
                var keyAndIV = hash.clone()

                // 1 round
                md.update(hash)
                md.update(passwordBytes)
                md.update(salt)

                hash = md.digest()
                keyAndIV = concat(keyAndIV, hash)

                val keyBytes = Arrays.copyOfRange(keyAndIV, 0, 32)
                val ivBytes = Arrays.copyOfRange(keyAndIV, 32, 48)

                val key = SecretKeySpec(keyBytes, "AES")
                val iv = IvParameterSpec(ivBytes)
///////////////////
//            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
//            val spec = PBEKeySpec(passString.toCharArray(), salt, ITERATIONS, KEY_SIZE_BITS)
//            val tmp = factory.generateSecret(spec)
//            val key = SecretKeySpec(tmp.encoded, "AES")
//
//
//            val ivBytes = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
//            val iv = IvParameterSpec(ivBytes)
/////////////////////////

                // --- initialize cipher instance and decrypt ---
                aesCBC.init(Cipher.DECRYPT_MODE, key, iv)
                val decrypted = aesCBC.doFinal(encrypted)
                val answer = String(decrypted, Charsets.UTF_8)
                println(answer)
            }
            else {
                // read plaintext file
                val plainTextFile = File(plaintextFileName).absoluteFile
                val data= plainTextFile.readBytes()

                // --- create salt ---
                val salt = ByteArray(8)
                val secureRandom = SecureRandom()
                secureRandom.nextBytes(salt)

                // --- specify cipher and digest for EVP_BytesToKey method ---

                val aesCBC = Cipher.getInstance("AES/CBC/PKCS5Padding")
                val md = MessageDigest.getInstance("SHA-256")


//////////////////////////////////////////
                // the IV is useless, OpenSSL might as well have use zero's
//                val keyAndIV = EVP_BytesToKey(KEY_SIZE_BITS / java.lang.Byte.SIZE, aesCBC.blockSize, md, salt,
//                                              passString.toByteArray(Charsets.US_ASCII), ITERATIONS)
//                val key = SecretKeySpec(keyAndIV[INDEX_KEY], "AES")
//                val iv = IvParameterSpec(keyAndIV[INDEX_IV])
//////////////////////////////////////////
                md.update(passwordBytes)
                md.update(salt)

                var hash = md.digest()
                var keyAndIV = hash.clone()

                // 1 round
                md.update(hash)
                md.update(passwordBytes)
                md.update(salt)

                hash = md.digest()
                keyAndIV = concat(keyAndIV, hash)

                val keyBytes = Arrays.copyOfRange(keyAndIV, 0, 32)
                val ivBytes = Arrays.copyOfRange(keyAndIV, 32, 48)

                val key = SecretKeySpec(keyBytes, "AES")
                val iv = IvParameterSpec(ivBytes)
//////////////////////////////////////////
//            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
//            val spec = PBEKeySpec(passString.toCharArray(), salt, ITERATIONS, KEY_SIZE_BITS)
//            val tmp = factory.generateSecret(spec)
//            val key = SecretKeySpec(tmp.encoded, "AES")
//
//
//            val ivBytes = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
//            val iv = IvParameterSpec(ivBytes)
/////////////////////////


                // --- initialize cipher instance and encrypt ---

                aesCBC.init(Cipher.ENCRYPT_MODE, key, iv)
                val encrypted = aesCBC.doFinal(data)

//                val cipher = OpenSSLPBECommon.initializeCipher(password, salt, Cipher.ENCRYPT_MODE)
//                val encrypted = cipher.doFinal(data)

                // "Salted__" + salt + encrypted
                val a1 = "Salted__".toByteArray(Charsets.US_ASCII)

                val finalEncrypted = concat(concat(a1, salt), encrypted)

                val encryptedFile = File(encryptedFileName).absoluteFile
                Files.deleteIfExists(encryptedFile.toPath())

                // WITH BASE64 By default the encoded file has a line break every 64 characters
//                encryptedFile.writeBytes(Base64.getMimeEncoder(64, "\n".toByteArray(Charsets.US_ASCII)).encode(finalEncrypted))

                // No base64
                encryptedFile.writeBytes(finalEncrypted)
            }
        }
        catch (e: BadPaddingException) {
            // AKA "something went wrong"
            throw IllegalStateException("Bad password, algorithm, mode or padding;" + " no salt, wrong number of iterations or corrupted ciphertext.")
        }
        catch (e: IllegalBlockSizeException) {
            throw IllegalStateException("Bad algorithm, mode or corrupted (resized) ciphertext.")
        }
        catch (e: GeneralSecurityException) {
            throw IllegalStateException(e)
        }
        catch (e: IOException) {
            throw IllegalStateException(e)
        }
    }

    fun concat(a: ByteArray, b: ByteArray): ByteArray {
        val c = ByteArray(a.size + b.size)
        System.arraycopy(a, 0, c, 0, a.size)
        System.arraycopy(b, 0, c, a.size, b.size)
        return c
    }
}
