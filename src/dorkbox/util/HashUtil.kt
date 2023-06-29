/*
 * Copyright 2023 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.util

import dorkbox.util.Sys.charToBytes16
import dorkbox.util.Sys.concatBytes
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Java native hashes!
 */
object HashUtil {
    private val digestLocal = ThreadLocal.withInitial {
        try {
            return@withInitial MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Unable to initialize hash algorithm. SHA-256 digest doesn't exist?!? (This should not happen")
        }
    }

    /**
     * gets the SHA256 hash + SALT of the specified username, as UTF-16
     */
    fun getSha256WithSalt(username: String?, saltBytes: ByteArray?): ByteArray? {
        if (username == null) {
            return null
        }
        val charToBytes = charToBytes16(username.toCharArray())
        val userNameWithSalt = concatBytes(charToBytes, saltBytes!!)

        val sha256 = digestLocal.get()
        val usernameHashBytes = ByteArray(sha256.digestLength)
        sha256.update(userNameWithSalt, 0, userNameWithSalt.size)
        sha256.digest(usernameHashBytes)

        return usernameHashBytes
    }

    /**
     * gets the SHA256 hash of the specified string, as UTF-16
     */
    fun getSha256(string: String): ByteArray {
        val charToBytes = charToBytes16(string.toCharArray())

        val sha256 = digestLocal.get()
        val usernameHashBytes = ByteArray(sha256.digestLength)
        sha256.update(charToBytes, 0, charToBytes.size)
        sha256.digest(usernameHashBytes)

        return usernameHashBytes
    }

    /**
     * gets the SHA256 hash of the specified byte array
     */
    fun getSha256(bytes: ByteArray): ByteArray {
        val sha256 = digestLocal.get()
        val hashBytes = ByteArray(sha256.digestLength)
        sha256.update(bytes, 0, bytes.size)
        sha256.digest(hashBytes)

        return hashBytes
    }

    fun getSha256WithSalt(bytes: ByteArray?, saltBytes: ByteArray?): ByteArray? {
        if (bytes == null || saltBytes == null) {
            return null
        }

        val bytesWithSalt = concatBytes(bytes, saltBytes)

        val sha256 = digestLocal.get()
        val usernameHashBytes = ByteArray(sha256.digestLength)
        sha256.update(bytesWithSalt, 0, bytesWithSalt.size)
        sha256.digest(usernameHashBytes)

        return usernameHashBytes
    }
}
