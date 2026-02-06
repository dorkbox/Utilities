/*
 * Copyright 2026 dorkbox, llc
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

@file:Suppress("unused")

package dorkbox.util

import dorkbox.util.ZipUtil.ZIP_HEADER
import java.io.*
import java.util.zip.*

object ZipUtil {
    var ZIP_HEADER = byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 0x3.toByte(), 0x4.toByte())


    /**
     * Unzips a ZIP or JAR file (and handles the manifest if requested)
     */
    @Throws(IOException::class)
    fun unzip(zipFile: File, outputDir: File, extractManifest: Boolean) {
        val fileLength = zipFile.length()
        if (fileLength > Int.MAX_VALUE - 1) {
            throw Exception("Source filesize is too large!")
        }

        val inputStream = ZipInputStream(FileInputStream(zipFile))
        unzip(inputStream, outputDir, extractManifest)
    }

    /**
     * Unzips a ZIP file
     */
    @Throws(IOException::class)
    fun unzip(inputStream: ZipInputStream, outputDir: File, extractManifest: Boolean) {
        inputStream.use {
            var entry: ZipEntry?
            while (inputStream.nextEntry.also { entry = it } != null) {
                val name = entry!!.name
                if (!extractManifest && name.startsWith("META-INF/")) {
                    continue
                }
                val file = File(outputDir, name)
                if (entry.isDirectory) {
                    File(file.path).mkdirs()
                    continue
                }
                File(file.parent).mkdirs()

                FileOutputStream(file).use {
                    inputStream.copyTo(it)
                }
            }
        }
    }
}


/**
 * @return true if the inputStream is a zip/jar stream. DOES NOT CLOSE THE STREAM
 */
fun InputStream.isZip(): Boolean {
    @Suppress("NAME_SHADOWING")
    var `in` = this
    if (!`in`.markSupported()) {
        `in` = BufferedInputStream(`in`)
    }
    var isZip = true
    try {
        `in`.mark(ZIP_HEADER.size)
        for (i in ZIP_HEADER.indices) {
            if (ZIP_HEADER[i] != `in`.read().toByte()) {
                isZip = false
                break
            }
        }
        `in`.reset()
    } catch (_: Exception) {
        isZip = false
    }
    return isZip
}

/**
 * @return true if the file is a zip/jar file
 */
fun File.isZip(): Boolean {
    var isZip = true
    val buffer = ByteArray(ZIP_HEADER.size)
    var raf: RandomAccessFile? = null
    try {
        raf = RandomAccessFile(this, "r")
        raf.readFully(buffer)
        for (i in ZIP_HEADER.indices) {
            if (buffer[i] != ZIP_HEADER[i]) {
                isZip = false
                break
            }
        }
    } catch (e: Exception) {
        isZip = false
        (e as? FileNotFoundException)?.printStackTrace()
    } finally {
        if (raf != null) {
            try {
                raf.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    return isZip
}

/**
 * Unzips a ZIP file
 */
@Throws(IOException::class)
fun File.unzip(outputDir: File, extractManifest: Boolean = true) {
    ZipUtil.unzip(this, outputDir, extractManifest)
}

/**
 * Unzips a ZIP file. Will close the input stream.
 */
@Throws(IOException::class)
fun ZipInputStream.unzip(outputDir: File, extractManifest: Boolean = true) {
    ZipUtil.unzip(this, outputDir, extractManifest)
}


/**
 * Extracts a file from a zip into a TEMP file, if possible. The TEMP file is deleted upon JVM exit.
 *
 * @return the location of the extracted file, or NULL if the file cannot be extracted or doesn't exist.
 */
@Throws(IOException::class)
fun FileInputStream.extract(fileToExtract: String): String? {
    ZipInputStream(this).use { inputStream ->
        while (true) {
            val entry = inputStream.nextEntry ?: break
            val name = entry.name
            if (entry.isDirectory) {
                continue
            }

            if (name == fileToExtract) {
                val tempFile = tempFile(name)
                tempFile.deleteOnExit()
                val tempOutput = FileOutputStream(tempFile)
                tempOutput.use {
                    inputStream.copyTo(it)
                }
                return tempFile.absolutePath
            }
        }
    }

    return null
}
