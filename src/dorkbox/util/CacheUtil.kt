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

import dorkbox.os.OS.TEMP_DIR
import dorkbox.util.FileUtil.copyFile
import dorkbox.util.FileUtil.delete
import dorkbox.util.FileUtil.getExtension
import java.io.*
import java.math.BigInteger
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class CacheUtil(private val tempDir: String = "cache") {
    /**
     * Clears ALL saved files in the cache
     */
    fun clear() {
        // deletes all of the files (recursively) in the specified location. If the directory is empty (no locked files), then the
        // directory is also deleted.
        delete(File(TEMP_DIR, tempDir))
    }

    /**
     * Checks to see if the specified file is in the cache. NULL if it is not, otherwise specifies a location on disk.
     *
     *
     * This cache is not persisted across runs.
     */
    fun check(file: File?): File? {
        if (file == null) {
            throw NullPointerException("file")
        }

        // if we already have this fileName, reuse it
        return check(file.absolutePath)
    }

    /**
     * Checks to see if the specified file is in the cache. NULL if it is not, otherwise specifies a location on disk.
     */
    fun check(fileName: String?): File? {
        if (fileName == null) {
            throw NullPointerException("fileName")
        }

        // if we already have this fileName, reuse it
        val newFile = makeCacheFile(fileName)

        // if this file already exists (via HASH), we just reuse what is saved on disk.
        return if (newFile.canRead() && newFile.isFile) {
            newFile
        } else null
    }

    /**
     * Checks to see if the specified URL is in the cache. NULL if it is not, otherwise specifies a location on disk.
     */
    fun check(fileResource: URL?): File? {
        if (fileResource == null) {
            throw NullPointerException("fileResource")
        }
        return check(fileResource.path)
    }

    /**
     * Checks to see if the specified stream (based on the hash of the input stream) is in the cache. NULL if it is not, otherwise
     * specifies a location on disk.
     */
    @Throws(IOException::class)
    fun check(fileStream: InputStream?): File? {
        if (fileStream == null) {
            throw NullPointerException("fileStream")
        }
        return check(null, fileStream)
    }

    /**
     * Checks to see if the specified name is in the cache. NULL if it is not, otherwise specifies a location on disk. If the
     * cacheName is NULL, it will use a HASH of the fileStream
     */
    @Throws(IOException::class)
    fun check(cacheName: String?, fileStream: InputStream?): File? {
        var cacheName = cacheName
        if (fileStream == null) {
            throw NullPointerException("fileStream")
        }
        if (cacheName == null) {
            cacheName = createNameAsHash(fileStream)
        }

        // if we already have this fileName, reuse it
        val newFile = makeCacheFile(cacheName)

        // if this file already exists (via HASH), we just reuse what is saved on disk.
        return if (newFile.canRead() && newFile.isFile) {
            newFile
        } else null
    }

    /**
     * Saves the name of the file in a cache, based on the file's name.
     */
    @Throws(IOException::class)
    fun save(file: File): File {
        return save(file.absolutePath, file)
    }

    /**
     * Saves the name of the file in a cache, based on the specified name. If cacheName is NULL, it will use the file's name.
     */
    @Throws(IOException::class)
    fun save(cacheName: String?, file: File): File {
        var cacheName = cacheName
        if (cacheName == null) {
            cacheName = file.absolutePath
        }
        return save(cacheName, file.absolutePath)
    }

    /**
     * Saves the name of the file in a cache, based on the specified name.
     */
    @Throws(IOException::class)
    fun save(fileName: String): File {
        return save(null, fileName)
    }

    /**
     * Saves the name of the file in a cache, based on name. If cacheName is NULL, it will use the file's name.
     *
     * @return the newly create cache file, or an IOException if there were problems
     */
    @Throws(IOException::class)
    fun save(cacheName: String?, fileName: String): File {
        var cacheName = cacheName
        if (cacheName == null) {
            cacheName = fileName
        }

        // if we already have this fileName, reuse it
        val newFile = makeCacheFile(cacheName)

        // if this file already exists (via HASH), we just reuse what is saved on disk.
        if (newFile.canRead() && newFile.isFile) {
            return newFile
        }


        // is file sitting on drive
        val iconTest = File(fileName)
        return if (iconTest.isFile) {
            if (!iconTest.canRead()) {
                throw IOException("File exists but unable to read source file $fileName")
            }

            // have to copy the resource to the cache
            copyFile(iconTest, newFile)
            newFile
        } else {
            // suck it out of a URL/Resource (with debugging if necessary)
            val systemResource = LocationResolver.getResource(fileName) ?: throw IOException("Unable to load URL resource $fileName")
            val inStream = systemResource.openStream()

            // saves the file into our temp location, uses HASH of cacheName
            makeFileViaStream(cacheName, inStream)
        }
    }

    /**
     * Saves the name of the URL in a cache, based on it's path.
     */
    @Throws(IOException::class)
    fun save(fileResource: URL): File {
        return save(null, fileResource)
    }

    /**
     * Saves the name of the URL in a cache, based on the specified name. If cacheName is NULL, it will use the URL's path.
     */
    @Throws(IOException::class)
    fun save(cacheName: String?, fileResource: URL): File {
        var cacheName = cacheName
        if (cacheName == null) {
            cacheName = fileResource.path
        }

        // if we already have this fileName, reuse it
        val newFile = makeCacheFile(cacheName)

        // if this file already exists (via HASH), we just reuse what is saved on disk.
        if (newFile.canRead() && newFile.isFile) {
            return newFile
        }
        val inStream = fileResource.openStream()

        // saves the file into our temp location, uses HASH of cacheName
        return makeFileViaStream(cacheName, inStream)
    }

    /**
     * This caches the data based on the HASH of the input stream.
     */
    @Throws(IOException::class)
    fun save(fileStream: InputStream?): File {
        if (fileStream == null) {
            throw NullPointerException("fileStream")
        }
        return save(null, fileStream)
    }

    /**
     * Saves the name of the file in a cache, based on the cacheName. If the cacheName is NULL, it will use a HASH of the fileStream
     * as the name.
     */
    @Throws(IOException::class)
    fun save(cacheName: String?, fileStream: InputStream): File {
        var cacheName = cacheName
        if (cacheName == null) {
            cacheName = createNameAsHash(fileStream)
        }

        // if we already have this fileName, reuse it
        val newFile = makeCacheFile(cacheName)

        // if this file already exists (via HASH), we just reuse what is saved on disk.
        return if (newFile.canRead() && newFile.isFile) {
            newFile
        } else makeFileViaStream(cacheName, fileStream)
    }

    /**
     * must be called from synchronized block!
     *
     * @param cacheName needs name+extension for the resource
     * @param resourceStream the resource to copy to a file on disk
     *
     * @return the full path of the resource copied to disk, or NULL if invalid
     */
    @Throws(IOException::class)
    private fun makeFileViaStream(cacheName: String?, resourceStream: InputStream?): File {
        if (resourceStream == null) {
            throw NullPointerException("resourceStream")
        }
        if (cacheName == null) {
            throw NullPointerException("cacheName")
        }
        val newFile = makeCacheFile(cacheName)

        // if this file already exists (via HASH), we just reuse what is saved on disk.
        if (newFile.canRead() && newFile.isFile) {
            return newFile.absoluteFile
        }
        var outStream: OutputStream? = null
        try {
            var read: Int
            val buffer = ByteArray(2048)
            outStream = FileOutputStream(newFile)
            while (resourceStream.read(buffer).also { read = it } > 0) {
                outStream.write(buffer, 0, read)
            }
        } catch (e: IOException) {
            // Send up exception
            val message = "Unable to copy '" + cacheName + "' to temporary location: '" + newFile.absolutePath + "'"
            throw IOException(message, e)
        } finally {
            try {
                resourceStream.close()
            } catch (ignored: Exception) {
            }
            try {
                outStream?.close()
            } catch (ignored: Exception) {
            }
        }

        //get the name of the new file
        return newFile.absoluteFile
    }

    /**
     * @param cacheName the name of the file to use in the cache. This file name can use invalid file name characters
     *
     * @return the file on disk represented by the file name
     */
    fun create(cacheName: String?): File {
        return makeCacheFile(cacheName)
    }

    // creates the file that will be cached. It may, or may not already exist
    // must be called from synchronized block!
    // never returns null
    private fun makeCacheFile(cacheName: String?): File {
        if (cacheName == null) {
            throw NullPointerException("cacheName")
        }
        val saveDir = File(TEMP_DIR, tempDir)

        // can be wimpy, only one at a time
        val hash = hashName(cacheName)
        var extension = getExtension(cacheName)
        if (extension.isEmpty()) {
            extension = "cache"
        }
        val newFile = File(saveDir, "$hash.$extension").absoluteFile
        // make whatever dirs we need to.
        newFile.parentFile.mkdirs()
        return newFile
    }

    companion object {
        private val digestLocal = ThreadLocal.withInitial {
            try {
                return@withInitial MessageDigest.getInstance("SHA1")
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException("Unable to initialize hash algorithm. SHA1 digest doesn't exist?!? (This should not happen")
            }
        }

        fun clear(tempDir: String) {
            CacheUtil(tempDir).clear()
        }

        // hashed name to prevent invalid file names from being used
        private fun hashName(name: String): String {
            // figure out the fileName
            val bytes = name.toByteArray(StandardCharsets.UTF_8)
            val digest = digestLocal.get()
            digest.reset()
            digest.update(bytes)

            // convert to alpha-numeric. see https://stackoverflow.com/questions/29183818/why-use-tostring32-and-not-tostring36
            return BigInteger(1, digest.digest()).toString(32).uppercase()
        }

        // this is if we DO NOT have a file name. We hash the resourceStream bytes to base the name on that. The extension will be ".cache"
        @Throws(IOException::class)
        fun createNameAsHash(resourceStream: InputStream): String {
            val digest = digestLocal.get()
            digest.reset()
            return try {
                // we have to set the cache name based on the hash of the input stream ONLY...
                val outStream = ByteArrayOutputStream(4096) // will resize if necessary
                var read: Int
                val buffer = ByteArray(2048)
                while (resourceStream.read(buffer).also { read = it } > 0) {
                    digest.update(buffer, 0, read)
                    outStream.write(buffer, 0, read)
                }

                // convert to alpha-numeric. see https://stackoverflow.com/questions/29183818/why-use-tostring32-and-not-tostring36
                BigInteger(1, digest.digest()).toString(32).uppercase() + ".cache"
            } catch (e: IOException) {
                // Send up exception
                val message = "Unable to copy InputStream to memory."
                throw IOException(message, e)
            } finally {
                try {
                    resourceStream.close()
                } catch (ignored: Exception) {
                }
            }
        }
    }
}
