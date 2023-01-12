/*
 * Copyright 2010 dorkbox, llc
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

import dorkbox.os.OS
import mu.KotlinLogging
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.PrintWriter
import java.io.RandomAccessFile
import java.io.Reader
import java.nio.file.DirectoryIteratorException
import java.util.*
import java.util.zip.*

/**
 * File related utilities.
 *
 *
 * Contains code from FilenameUtils.java (normalize + dependencies) - Apache 2.0 License
 * http://commons.apache.org/proper/commons-io/
 * Copyright 2013 ASF
 * Authors: Kevin A. Burton, Scott Sanders, Daniel Rall, Christoph.Reck,
 * Peter Donald, Jeff Turner, Matthew Hawthorne, Martin Cooper,
 * Jeremias Maerki, Stephen Colebourne
 */
@Suppress("unused")
object FileUtil {
    interface Action {
        fun onLineRead(line: String)
        fun finished()
    }

    /**
     * Gets the version number.
     */
    val version = "1.37"

    private val log = KotlinLogging.logger(FileUtil::class.java.name)

    private const val DEBUG = false

    /**
     * The Unix separator character.
     */
    private const val UNIX_SEPARATOR = '/'

    /**
     * The Windows separator character.
     */
    private const val WINDOWS_SEPARATOR = '\\'

    /**
     * The system separator character.
     */
    private val SYSTEM_SEPARATOR = File.separatorChar

    /**
     * The separator character that is the opposite of the system separator.
     */
    private val OTHER_SEPARATOR = if (OS.isWindows) { UNIX_SEPARATOR } else { WINDOWS_SEPARATOR }

    var ZIP_HEADER = byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 0x3.toByte(), 0x4.toByte())

    fun prepend(file: File, vararg strings: String) {
        // make sure we can write to the file
        file.parentFile?.mkdirs()

        val contents = LinkedList<String>()

        for (string in strings) {
            contents.add(string)
        }

        // have to read ORIGINAL file, since we can't prepend it any other way....
        readOnePerLine(file, contents, false)

        write(file, contents)
    }

    fun append(file: File, vararg text: String) {
        // make sure we can write to the file
        file.parentFile?.mkdirs()

        // wooooo for auto-closable and try-with-resources
        try {
            FileWriter(file, true).use { fw ->
                BufferedWriter(fw).use { bw ->
                    PrintWriter(bw).use { out ->

                        for (s in text) {
                            out.println(s)
                        }
                    }
                }
            }
        }
        catch (e: IOException) {
            log.error("Error appending text", e)
        }
    }

    fun write(file: File, vararg text: String) {
        // make sure we can write to the file
        file.parentFile?.mkdirs()

        // wooooo for auto-closable and try-with-resources
        try {
            FileWriter(file, false).use { fw ->
                BufferedWriter(fw).use { bw ->
                    PrintWriter(bw).use { out ->

                        for (s in text) {
                            out.println(s)
                        }
                    }
                }
            }
        }
        catch (e: IOException) {
            log.error("Error appending text", e)
        }

    }

    fun write(file: File, text: List<String>) {
        // make sure we can write to the file
        file.parentFile?.mkdirs()

        // wooooo for auto-closable and try-with-resources
        try {
            FileWriter(file, false).use { fw ->
                BufferedWriter(fw).use { bw ->
                    PrintWriter(bw).use { out ->

                        for (s in text) {
                            out.println(s)
                        }
                    }
                }
            }
        }
        catch (e: IOException) {
            log.error("Error appending text", e)
        }

    }

    fun append(file: File, text: List<String>) {
        // make sure we can write to the file
        file.parentFile?.mkdirs()

        // wooooo for auto-closable and try-with-resources
        try {
            FileWriter(file, true).use { fw ->
                BufferedWriter(fw).use { bw ->
                    PrintWriter(bw).use { out ->

                        for (s in text) {
                            out.println(s)
                        }
                    }
                }
            }
        }
        catch (e: IOException) {
            log.error("Error appending text", e)
        }
    }

    /**
     * Converts the content of a file into a list of strings. Lines are trimmed.
     *
     * @param file the input file to read. Throws an error if this file cannot be read.
     * @param includeEmptyLines true if you want the resulting list of String to include blank/empty lines from the file
     *
     * @return A list of strings, one line per string, of the content
     */
    @Throws(IOException::class)
    fun read(file: File, includeEmptyLines: Boolean): List<String> {
        val lines: MutableList<String> = ArrayList()

        if (includeEmptyLines) {
            file.reader().use {
                it.forEachLine { line ->
                    lines.add(line)
                }
            }
        } else {
            file.reader().use {
                it.forEachLine { line ->
                    if (line.isNotEmpty()) {
                        lines.add(line)
                    }
                }
            }
        }

        return lines
    }

    /**
     * Convenience method that converts the content of a file into a giant string.
     *
     * @param file the input file to read. Throws an error if this file cannot be read.
     *
     * @return A string, matching the contents of the file
     */
    @Throws(IOException::class)
    fun readAsString(file: File): String {
        return file.readText()
    }

    /**
     * @return contents of the file if we could read the file without errors. Null if we could not
     */
    fun read(file: String): String? {
        return read(File(file))
    }

    /**
     * @return contents of the file if we could read the file without errors. Null if we could not
     */
    fun read(file: File): String? {
        val text = file.readText()

        if (text.isEmpty()) {
            return null
        }

        return text
    }

    /**
     * Reads the content of a file to the passed in StringBuilder.
     *
     * @param file the input file to read. Throws an error if this file cannot be read.
     * @param stringBuilder the stringBuilder this file will be written to
     */
    @Throws(IOException::class)
    fun read(file: File, stringBuilder: StringBuilder) {
        FileReader(file).use {
            val bin = BufferedReader(it)
            var line: String?
            while (bin.readLine().also { line = it } != null) {
                stringBuilder.append(line).append(OS.LINE_SEPARATOR)
            }
        }
    }

    /**
     * Reads the content of a file to the passed in StringBuilder.
     *
     * @return true if we could read the file without errors. False if there were errors.
     */
    fun read(file: File, builder: StringBuilder, lineSeparator: String?): Boolean {
        if (!file.canRead()) {
            return false
        }

        try {
            file.reader().use { reader ->
                reader.forEachLine { line ->
                    if (lineSeparator != null) {
                        builder.append(line).append(lineSeparator)
                    }
                    else {
                        builder.append(line)
                    }
                }
            }
        }
        catch (ignored: Exception) {
            return false
        }

        return true
    }

    /**
     * Reads each line in a file, performing ACTION for each line.
     *
     * @return true if we could read the file without errors. False if there were errors.
     */
    fun read(file: File, action: Action): Boolean {
        if (!file.canRead()) {
            return false
        }

        try {
            file.reader().use { reader ->
                reader.forEachLine { line ->
                    action.onLineRead(line)
                }
            }
        }
        catch (ignored: Exception) {
            return false
        }
        action.finished()

        return true
    }

    /**
     * Will always return a String.
     *
     * @param file the file to read
     *
     * @return the first line in the file, excluding the "new line" character.
     */
    fun readFirstLine(file: File): String {
        if (!file.canRead()) {
            return ""
        }

        return file.reader().use { reader ->
            reader.buffered().lineSequence().firstOrNull()
        } ?: ""
    }

    fun getPid(pidFileName: String): String? {
        val stringBuilder = StringBuilder()
        return if (read(File(pidFileName), stringBuilder, null)) {
            stringBuilder.toString()
        }
        else {
            null
        }
    }

    /**
     * Reads the contents of the supplied input stream into a list of lines.
     *
     * @return Always returns a list, even if the file does not exist, or there are errors reading it.
     */
    fun readLines(file: File): List<String> {
        val fileReader = try {
            FileReader(file)
        } catch (ignored: FileNotFoundException) {
            return ArrayList()
        }
        return readLines(fileReader)
    }

    /**
     * Reads the contents of the supplied input stream into a list of lines.
     *
     *
     * Closes the reader on successful or failed completion.
     *
     * @return Always returns a list, even if the file does not exist, or there are errors reading it.
     */
    fun readLines(`in`: Reader): List<String> {
        val lines: MutableList<String> = ArrayList()

        BufferedReader(`in`).use {
            val bin = BufferedReader(`in`)
            var line: String
            try {
                while (bin.readLine().also { line = it } != null) {
                    lines.add(line)
                }
            } catch (ignored: IOException) {
            }
        }

        return lines
    }

    /**
     * @return a list of the contents of a file, one line at a time. Ignores lines that start with #
     */
    fun readOnePerLine(file: File): ArrayList<String> {
        val list = ArrayList<String>()
        readOnePerLine(file, list, true)

        return list
    }

    /**
     * @return a list of the contents of a file, one line at a time. Ignores lines that start with #
     */
    fun readOnePerLine(file: File, trimStrings: Boolean): ArrayList<String> {
        val list = ArrayList<String>()
        readOnePerLine(file, list, trimStrings)

        return list
    }

    /**
     * @return a list of the contents of a file, one line at a time. Ignores lines that start with #
     */
    fun readOnePerLine(file: File, list: MutableList<String>, trimStrings: Boolean) {
        if (trimStrings) {
            read(file, object : Action {
                var lineNumber = 0

                override fun onLineRead(line: String) {
                    if (line.isNotEmpty() && !line.startsWith("#")) {
                        val newLine = line.trim()

                        if (newLine.isNotEmpty()) {
                            list.add(newLine)
                        }
                    }

                    lineNumber++
                }

                override fun finished() {}
            })
        }
        else {
            read(file, object : Action {
                var lineNumber = 0

                override fun onLineRead(line: String) {
                    list.add(line)
                    lineNumber++
                }

                override fun finished() {}
            })
        }
    }

    /**
     * @return true if the directory was fully deleted. A false indicates that a partial delete has occurred
     */
    fun deleteDirectory(dir: File): Boolean {
        try {
            return dir.deleteRecursively()
        }
        catch (e: IOException) {
            log.error("Error deleting the contents of dir $dir", e)
        }
        catch (e: DirectoryIteratorException) {
            log.error("Error deleting the contents of dir $dir", e)
        }

        return false
    }

    /**
     * Renames a file. Windows has all sorts of problems which are worked around.
     *
     * @return true if successful, false otherwise
     */
    fun renameTo(source: File, dest: File): Boolean {
        // if we're on a civilized operating system we may be able to simple
        // rename it
        if (source.renameTo(dest)) {
            return true
        }

        // fall back to trying to rename the old file out of the way, rename the
        // new file into
        // place and then delete the old file
        if (dest.exists()) {
            val temp = File(dest.path + "_old")
            if (temp.exists()) {
                if (!temp.delete()) {
                    if (DEBUG) {
                        System.err.println("Failed to delete old intermediate file: $temp")
                    }

                    // the subsequent code will probably fail
                }
            }
            if (dest.renameTo(temp)) {
                if (source.renameTo(dest)) {
                    if (temp.delete()) {
                        if (DEBUG) {
                            System.err.println("Failed to delete intermediate file: $temp")
                        }
                    }
                    return true
                }
            }
        }

        // as a last resort, try copying the old data over the new
        return try {
            source.copyTo(dest)
            if (!source.delete()) {
                if (DEBUG) {
                    System.err.println("Failed to delete '$source' after brute force copy to '$dest'.")
                }
            }
            true
        } catch (ioe: IOException) {
            if (DEBUG) {
                System.err.println("Failed to copy '$source' to '$dest'.")
                ioe.printStackTrace()
            }
            false
        }
    }

    /**
     * Copies a files from one location to another.  Overwriting any existing file at the destination.
     */
    @Throws(IOException::class)
    fun copyFile(`in`: String, out: File): File {
        return copyFile(File(`in`), out)
    }

    /**
     * Copies a files from one location to another.  Overwriting any existing file at the destination.
     */
    @Throws(IOException::class)
    fun copyFile(`in`: File, out: String): File {
        return copyFile(`in`, File(out))
    }

    /**
     * Copies a files from one location to another.  Overwriting any existing file at the destination.
     */
    @Throws(IOException::class)
    fun copyFile(`in`: String, out: String): File {
        return copyFile(File(`in`), File(out))
    }

    /**
     * Copies a files from one location to another.  Overwriting any existing file at the destination.
     */
    @Throws(IOException::class)
    fun copyFileToDir(`in`: String, out: String): File {
        return copyFileToDir(File(`in`), File(out))
    }

    /**
     * Copies a files from one location to another. Overwriting any existing file at the destination.
     * If the out file is a directory, then the in file will be copied to the directory
     */
    @Throws(IOException::class)
    fun copyFileToDir(`in`: File, out: File): File {
        // copy the file to the directory instead
        if (!out.isDirectory) {
            throw IOException("Out file is not a directory! '" + out.absolutePath + "'")
        }
        return copyFile(`in`, File(out, `in`.name))
    }

    /**
     * Copies a files from one location to another. Overwriting any existing file at the destination.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun copyFile(`in`: File, out: File): File {
        val normalizedIn = normalize(`in`)!!.absolutePath
        val normalizedout = normalize(out)!!.absolutePath
        if (normalizedIn.equals(normalizedout, ignoreCase = true)) {
            if (DEBUG) {
                System.err.println("Source equals destination! $normalizedIn")
            }
            return out
        }


        // if out doesn't exist, then create it.
        val parentOut: File? = out.parentFile
        if (parentOut?.canWrite() == false) {
            parentOut.mkdirs()
        }
        if (DEBUG) {
            System.err.println("Copying file: '$`in`'  -->  '$out'")
        }

        `in`.copyTo(`out`)
        out.setLastModified(`in`.lastModified())
        return out
    }

    /**
     * Copies the contents of file two onto the END of file one.
     */
    fun concatFiles(one: File, two: File): File {
        if (DEBUG) {
            System.err.println("Concat'ing file: '$one'  -->  '$two'")
        }

        one.appendBytes(two.readBytes())

        one.setLastModified(System.currentTimeMillis())
        return one
    }

    /**
     * Moves a file, overwriting any existing file at the destination.
     */
    @Throws(IOException::class)
    fun moveFile(`in`: String, out: File): File {
        return moveFile(File(`in`), out)
    }

    /**
     * Moves a file, overwriting any existing file at the destination.
     */
    @Throws(IOException::class)
    fun moveFile(`in`: File, out: String): File {
        return moveFile(`in`, File(out))
    }

    /**
     * Moves a file, overwriting any existing file at the destination.
     */
    @Throws(IOException::class)
    fun moveFile(`in`: String, out: String): File {
        return moveFile(File(`in`), File(out))
    }

    /**
     * Moves a file, overwriting any existing file at the destination.
     */
    @Throws(IOException::class)
    fun moveFile(`in`: File, out: File): File {
        if (out.canRead()) {
            out.delete()
        }
        val renameSuccess = renameTo(`in`, out)
        if (!renameSuccess) {
            throw IOException("Unable to move file: '" + `in`.absolutePath + "' -> '" + out.absolutePath + "'")
        }
        return out
    }

    /**
     * Copies a directory from one location to another
     */
    @Throws(IOException::class)
    fun copyDirectory(src: String, dest: String, vararg namesToIgnore: String) {
        copyDirectory(File(src), File(dest), *namesToIgnore)
    }

    /**
     * Copies a directory from one location to another
     */
    @Throws(IOException::class)
    fun copyDirectory(src_: File, dest_: File, vararg namesToIgnore: String) {
        val src = normalize(src_)
        val dest = normalize(dest_)

        requireNotNull(src) { "Source must be valid" }
        requireNotNull(dest) { "Destination must be valid" }

        if (namesToIgnore.isNotEmpty()) {
            val name = src.name
            for (ignore in namesToIgnore) {
                if (name == ignore) {
                    return
                }
            }
        }
        if (src.isDirectory) {
            // if directory not exists, create it
            if (!dest.exists()) {
                dest.mkdir()
                if (DEBUG) {
                    System.err.println("Directory copied from  '$src'  -->  '$dest'")
                }
            }

            // list all the directory contents
            val files = src.list()
            if (files != null) {
                for (file in files) {
                    // construct the src and dest file structure
                    val srcFile = File(src, file)
                    val destFile = File(dest, file)

                    // recursive copy
                    copyDirectory(srcFile, destFile, *namesToIgnore)
                }
            }
        } else {
            // if file, then copy it
            copyFile(src, dest)
        }
    }

    /**
     * Safely moves a directory from one location to another (by copying it first, then deleting the original).
     */
    @Throws(IOException::class)
    fun moveDirectory(src: String, dest: String, vararg fileNamesToIgnore: String) {
        moveDirectory(File(src), File(dest), *fileNamesToIgnore)
    }

    /**
     * Safely moves a directory from one location to another (by copying it first, then deleting the original).
     */
    @Throws(IOException::class)
    fun moveDirectory(src: File, dest: File, vararg fileNamesToIgnore: String) {
        if (fileNamesToIgnore.size > 0) {
            val name = src.name
            for (ignore in fileNamesToIgnore) {
                if (name == ignore) {
                    return
                }
            }
        }
        if (src.isDirectory) {
            // if directory not exists, create it
            if (!dest.exists()) {
                dest.mkdir()
                if (DEBUG) {
                    System.err.println("Directory copied from  '$src'  -->  '$dest'")
                }
            }

            // list all the directory contents
            val files = src.list()
            if (files != null) {
                for (file in files) {
                    // construct the src and dest file structure
                    val srcFile = File(src, file)
                    val destFile = File(dest, file)

                    // recursive copy
                    moveDirectory(srcFile, destFile, *fileNamesToIgnore)
                }
            }
        } else {
            // if file, then copy it
            moveFile(src, dest)
        }
    }

    /**
     * Deletes a file or directory and all files and sub-directories under it.
     *
     * @param fileNamesToIgnore if prefaced with a '/', it will ignore as a directory instead of file
     * @return true iff the file/dir was deleted
     */
    fun delete(fileName: String, vararg fileNamesToIgnore: String): Boolean {
        return delete(File(fileName), *fileNamesToIgnore)
    }

    /**
     * Deletes a file, directory + all files and sub-directories under it. The directory is ALSO deleted if it because empty as a result
     * of this operation
     *
     * @param namesToIgnore if prefaced with a '/', it will treat the name to ignore as a directory instead of file
     *
     * @return true IFF the file/dir was deleted or didn't exist at first
     */
    @JvmStatic
    fun delete(file: File, vararg namesToIgnore: String): Boolean {
        if (!file.exists()) {
            return true
        }
        var thingsDeleted = false
        var ignored = false
        if (file.isDirectory) {
            val files = file.listFiles()
            if (files != null) {
                var i = 0
                val n = files.size
                while (i < n) {
                    var delete = true
                    val file2 = files[i]
                    val name2 = file2.name
                    val name2Full = normalize(file2)!!.absolutePath
                    if (file2.isDirectory) {
                        for (name in namesToIgnore) {
                            if (name[0] == UNIX_SEPARATOR && name == name2) {
                                // only name match if our name To Ignore starts with a / or \
                                if (DEBUG) {
                                    System.err.println("Skipping delete dir: $file2")
                                }
                                ignored = true
                                delete = false
                                break
                            } else if (name == name2Full) {
                                // full path match
                                if (DEBUG) {
                                    System.err.println("Skipping delete dir: $file2")
                                }
                                ignored = true
                                delete = false
                                break
                            }
                        }
                        if (delete) {
                            if (DEBUG) {
                                System.err.println("Deleting dir: $file2")
                            }
                            delete(file2, *namesToIgnore)
                        }
                    } else {
                        for (name in namesToIgnore) {
                            if (name[0] != UNIX_SEPARATOR && name == name2) {
                                // only name match
                                if (DEBUG) {
                                    System.err.println("Skipping delete file: $file2")
                                }
                                ignored = true
                                delete = false
                                break
                            } else if (name == name2Full) {
                                // full path match
                                if (DEBUG) {
                                    System.err.println("Skipping delete file: $file2")
                                }
                                ignored = true
                                delete = false
                                break
                            }
                        }
                        if (delete) {
                            if (DEBUG) {
                                System.err.println("Deleting file: $file2")
                            }
                            thingsDeleted = thingsDeleted or file2.delete()
                        }
                    }
                    i++
                }
            }
        }

        // don't try to delete the dir if there was an ignored file in it
        if (ignored) {
            if (DEBUG) {
                System.err.println("Skipping deleting file: $file")
            }
            return false
        }
        if (DEBUG) {
            System.err.println("Deleting file: $file")
        }
        thingsDeleted = thingsDeleted or file.delete()
        return thingsDeleted
    }

    /**
     * @return the contents of the file as a byte array
     */
    fun toBytes(file: File): ByteArray? {
        return file.readBytes()
    }

    /**
     * Creates the directories in the specified location.
     */
    fun mkdir(location: File): String {
        val path = normalize(location)!!.absoluteFile
        if (location.mkdirs()) {
            if (DEBUG) {
                System.err.println("Created directory: $path")
            }
        }
        return path.path
    }

    /**
     * Creates the directories in the specified location.
     */
    fun mkdir(location: String): String {
        return mkdir(File(location))
    }

    /**
     * Creates a temp file
     */
    @Throws(IOException::class)
    fun tempFile(fileName: String): File {
        return normalize(File.createTempFile(fileName, null))!!.absoluteFile
    }

    /**
     * Creates a temp directory
     */
    @Throws(IOException::class)
    fun tempDirectory(directoryName: String): String {
        val file = File.createTempFile(directoryName, null)
        if (!file.delete()) {
            throw IOException("Unable to delete temp file: $file")
        }
        if (!file.mkdir()) {
            throw IOException("Unable to create temp directory: $file")
        }
        return normalize(file)!!.absolutePath
    }

    /**
     * @return true if the inputStream is a zip/jar stream. DOES NOT CLOSE THE STREAM
     */
    fun isZipStream(`in`: InputStream): Boolean {
        @Suppress("NAME_SHADOWING")
        var `in` = `in`
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
        } catch (e: Exception) {
            isZip = false
        }
        return isZip
    }

    /**
     * @return true if the named file is a zip/jar file
     */
    fun isZipFile(fileName: String): Boolean {
        return isZipFile(File(fileName))
    }

    /**
     * @return true if the file is a zip/jar file
     */
    fun isZipFile(file: File): Boolean {
        var isZip = true
        val buffer = ByteArray(ZIP_HEADER.size)
        var raf: RandomAccessFile? = null
        try {
            raf = RandomAccessFile(file, "r")
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
    fun unzip(zipFile: String, outputDir: String) {
        unzipJar(zipFile, outputDir, true)
    }

    /**
     * Unzips a ZIP file
     */
    @Throws(IOException::class)
    fun unzip(zipFile: File, outputDir: File) {
        unzipJar(zipFile, outputDir, true)
    }

    /**
     * Unzips a ZIP file. Will close the input stream.
     */
    @Throws(IOException::class)
    fun unzip(inputStream: ZipInputStream, outputDir: String) {
        unzip(inputStream, File(outputDir))
    }

    /**
     * Unzips a ZIP file. Will close the input stream.
     */
    @Throws(IOException::class)
    fun unzip(inputStream: ZipInputStream, outputDir: File) {
        unzipJar(inputStream, outputDir, true)
    }

    /**
     * Unzips a ZIP file
     */
    @Throws(IOException::class)
    fun unzipJar(zipFile: String, outputDir: String, extractManifest: Boolean) {
        unjarzip0(File(zipFile), File(outputDir), extractManifest)
    }

    /**
     * Unzips a ZIP file
     */
    @Throws(IOException::class)
    fun unzipJar(zipFile: File, outputDir: File, extractManifest: Boolean) {
        unjarzip0(zipFile, outputDir, extractManifest)
    }

    /**
     * Unzips a ZIP file. Will close the input stream.
     */
    @Throws(IOException::class)
    fun unzipJar(inputStream: ZipInputStream, outputDir: File, extractManifest: Boolean) {
        unjarzip1(inputStream, outputDir, extractManifest)
    }

    /**
     * Unzips a ZIP or JAR file (and handles the manifest if requested)
     */
    @Throws(IOException::class)
    private fun unjarzip0(zipFile: File, outputDir: File, extractManifest: Boolean) {
        val fileLength = zipFile.length()
        if (fileLength > Int.MAX_VALUE - 1) {
            throw RuntimeException("Source filesize is too large!")
        }
        val inputStream = ZipInputStream(FileInputStream(zipFile))
        unjarzip1(inputStream, outputDir, extractManifest)
    }

    /**
     * Unzips a ZIP file
     */
    @Throws(IOException::class)
    private fun unjarzip1(inputStream: ZipInputStream, outputDir: File, extractManifest: Boolean) {

        inputStream.use {
            var entry: ZipEntry
            while (inputStream.nextEntry.also { entry = it } != null) {
                val name = entry.name
                if (!extractManifest && name.startsWith("META-INF/")) {
                    continue
                }
                val file = File(outputDir, name)
                if (entry.isDirectory) {
                    mkdir(file.path)
                    continue
                }
                mkdir(file.parent)

                FileOutputStream(file).use {
                    inputStream.copyTo(it)
                }
            }
        }
    }

    /**
     * Parses the specified root directory for **ALL** files that are in it. All of the sub-directories are searched as well.
     *
     *
     * *This is different, in that it returns ALL FILES, instead of ones that just match a specific extension.*
     *
     * @return the list of all files in the root+sub-dirs.
     */
    @Throws(IOException::class)
    fun parseDir(rootDirectory: String): List<File> {
        return parseDir(File(rootDirectory))
    }

    /**
     * Parses the specified root directory for **ALL** files that are in it. All of the sub-directories are searched as well.
     *
     *
     * *This is different, in that it returns ALL FILES, instead of ones that just match a specific extension.*
     *
     * @return the list of all files in the root+sub-dirs.
     */
    @Throws(IOException::class)
    fun parseDir(rootDirectory: File): List<File> {
        return parseDir(rootDirectory)
    }

    /**
     * Parses the specified root directory for files that end in the extension to match. All of the sub-directories are searched as well.
     *
     * @return the list of all files in the root+sub-dirs that match the given extension.
     */
    @Throws(IOException::class)
    fun parseDir(rootDirectory: File, vararg extensionsToMatch: String): List<File> {
        val jarList: MutableList<File> = LinkedList()
        val directories = LinkedList<File?>()

        @Suppress("NAME_SHADOWING")
        val rootDirectory = normalize(rootDirectory) ?: throw IOException("Root directory was invalid!")

        if (!rootDirectory.exists()) {
            throw IOException("Location does not exist: " + rootDirectory.absolutePath)
        }

        if (rootDirectory.isDirectory) {
            directories.add(rootDirectory)
            while (directories.peek() != null) {
                val dir = directories.poll()
                val listFiles = dir!!.listFiles()
                if (listFiles != null) {
                    for (file in listFiles) {
                        if (file.isDirectory) {
                            directories.add(file)
                        } else {
                            if (extensionsToMatch.isEmpty()) {
                                jarList.add(file)
                            } else {
                                for (e in extensionsToMatch) {
                                    if (file.absolutePath.endsWith(e)) {
                                        jarList.add(file)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            throw IOException("Cannot search directory children if the dir is a file name: " + rootDirectory.absolutePath)
        }
        return jarList
    }

    /**
     * Gets the relative path of a file to a specific directory in it's hierarchy.
     *
     *
     * For example: getChildRelativeToDir("/a/b/c/d/e.bah", "c") -> "d/e.bah"
     *
     * @return null if there is no child
     */
    fun getChildRelativeToDir(fileName: String, dirInHeirarchy: String): String? {
        require(fileName.isEmpty()) { "fileName cannot be empty." }
        return getChildRelativeToDir(File(fileName), dirInHeirarchy)
    }

    /**
     * Gets the relative path of a file to a specific directory in it's hierarchy.
     *
     *
     * For example: getChildRelativeToDir("/a/b/c/d/e.bah", "c") -> "d/e.bah"
     *
     * @return null if there is no child
     */
    fun getChildRelativeToDir(file: File, dirInHeirarchy: String): String? {
        require(dirInHeirarchy.isEmpty()) { "dirInHeirarchy cannot be empty." }

        val split = dirInHeirarchy.split(File.separator).toTypedArray()
        var splitIndex = split.size - 1
        val absolutePath = file.absolutePath
        var parent: File? = file
        var parentName: String

        if (splitIndex == 0) {
            // match on ONE dir
            while (parent != null) {
                parentName = parent.name
                if (parentName == dirInHeirarchy) {
                    parentName = parent.absolutePath
                    return absolutePath.substring(parentName.length + 1)
                }
                parent = parent.parentFile
            }
        } else {
            // match on MANY dir. They must be "in-order"
            var matched = false
            while (parent != null) {
                parentName = parent.name
                if (matched) {
                    if (parentName == split[splitIndex]) {
                        splitIndex--
                        if (splitIndex < 0) {
                            // this means the ENTIRE path matched
                            return if (absolutePath.length == dirInHeirarchy.length) {
                                null
                            } else absolutePath.substring(dirInHeirarchy.length + 1, absolutePath.length)

                            // +1 to account for the separator char
                        }
                    } else {
                        // because it has to be "in-order", if it doesn't match, we immediately abort
                        return null
                    }
                } else {
                    if (parentName == split[splitIndex]) {
                        matched = true
                        splitIndex--
                    }
                }
                parent = parent.parentFile
            }
        }
        return null
    }

    /**
     * Gets the PARENT relative path of a file to a specific directory in it's hierarchy.
     *
     *
     * For example: getParentRelativeToDir("/a/b/c/d/e.bah", "c") -> "/a/b"
     */
    fun getParentRelativeToDir(fileName: String, dirInHeirarchy: String): String? {
        require(fileName.isEmpty()) { "fileName cannot be empty." }

        return getParentRelativeToDir(File(fileName), dirInHeirarchy)
    }

    /**
     * Gets the relative path of a file to a specific directory in it's hierarchy.
     *
     *
     * For example: getParentRelativeToDir("/a/b/c/d/e.bah", "c") -> "/a/b"
     *
     * @return null if it cannot be found
     */
    fun getParentRelativeToDir(file: File, dirInHeirarchy: String): String? {
        require(dirInHeirarchy.isEmpty()) { "dirInHeirarchy cannot be empty." }

        val split = dirInHeirarchy.split(File.separator).toTypedArray()
        var splitIndex = split.size - 1
        var parent: File? = file
        var parentName: String
        if (splitIndex == 0) {
            // match on ONE dir
            while (parent != null) {
                parentName = parent.name
                if (parentName == dirInHeirarchy) {
                    parent = parent.parentFile
                    parentName = parent.absolutePath
                    return parentName
                }
                parent = parent.parentFile
            }
        } else {
            // match on MANY dir. They must be "in-order"
            var matched = false
            while (parent != null) {
                parentName = parent.name
                if (matched) {
                    if (parentName == split[splitIndex]) {
                        splitIndex--
                        if (splitIndex < 0) {
                            parent = parent.parentFile
                            parentName = parent.absolutePath
                            return parentName
                        }
                    } else {
                        // because it has to be "in-order", if it doesn't match, we immediately abort
                        return null
                    }
                } else {
                    if (parentName == split[splitIndex]) {
                        matched = true
                        splitIndex--
                    }
                }
                parent = parent.parentFile
            }
        }
        return null
    }

    /**
     * Extracts a file from a zip into a TEMP file, if possible. The TEMP file is deleted upon JVM exit.
     *
     * @return the location of the extracted file, or NULL if the file cannot be extracted or doesn't exist.
     */
    @Throws(IOException::class)
    fun extractFromZip(zipFile: String, fileToExtract: String): String? {
        ZipInputStream(FileInputStream(zipFile)).use { inputStream ->
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

    /**
     * Touches a file, so that it's timestamp is right now. If the file is not created, it will be created automatically.
     *
     * @return true if the touch succeeded, false otherwise
     */
    fun touch(file: String): Boolean {
        val timestamp = System.currentTimeMillis()
        return touch(File(file).absoluteFile, timestamp)
    }

    /**
     * Touches a file, so that it's timestamp is right now. If the file is not created, it will be created automatically.
     *
     * @return true if the touch succeeded, false otherwise
     */
    fun touch(file: File): Boolean {
        val timestamp = System.currentTimeMillis()
        return touch(file, timestamp)
    }

    /**
     * Touches a file, so that it's timestamp is right now. If the file is not created, it will be created automatically.
     *
     * @return true if the touch succeeded, false otherwise
     */
    fun touch(file: File, timestamp: Long): Boolean {
        if (!file.exists()) {
            val mkdirs = file.parentFile.mkdirs()
            if (!mkdirs) {
                // error creating the parent directories.
                return false
            }
            try {
                FileOutputStream(file).close()
            } catch (ignored: IOException) {
                return false
            }
        }
        return file.setLastModified(timestamp)
    }



    //-----------------------------------------------------------------------
    /*
     * FilenameUtils.java (normalize + dependencies) - Apache 2.0 License
     *   http://commons.apache.org/proper/commons-io/
     *   Copyright 2013 ASF
     *   Authors: Kevin A. Burton, Scott Sanders, Daniel Rall, Christoph.Reck,
     *            Peter Donald, Jeff Turner, Matthew Hawthorne, Martin Cooper,
     *            Jeremias Maerki, Stephen Colebourne
     */
    /**
     * Normalizes a path, removing double and single dot path steps.
     *
     *
     * THIS IS DIFFERENT in that it might not be a path that resolves to anything
     *
     *
     * This method normalizes a path to a standard format.
     * The input may contain separators in either Unix or Windows format.
     * The output will contain separators in the format of the system.
     *
     *
     * A trailing slash will be retained.
     * A double slash will be merged to a single slash (but UNC names are handled).
     * A single dot path segment will be removed.
     * A double dot will cause that path segment and the one before to be removed.
     * If the double dot has no parent path segment to work with, `null`
     * is returned.
     *
     *
     * The output will be the same on both Unix and Windows except
     * for the separator character.
     * <pre>
     * /foo//               -->   /foo/
     * /foo/./              -->   /foo/
     * /foo/../bar          -->   /bar
     * /foo/../bar/         -->   /bar/
     * /foo/../bar/../baz   -->   /baz
     * //foo//./bar         -->   /foo/bar
     * /../                 -->   null
     * ../foo               -->   null
     * foo/bar/..           -->   foo/
     * foo/../../bar        -->   null
     * foo/../bar           -->   bar
     * //server/foo/../bar  -->   //server/bar
     * //server/../bar      -->   null
     * C:\foo\..\bar        -->   C:\bar
     * C:\..\bar            -->   null
     * ~/foo/../bar/        -->   ~/bar/
     * ~/../bar             -->   null
    </pre> *
     * (Note the file separator returned will be correct for Windows/Unix)
     *
     * @param filename the filename to normalize, null returns null
     *
     * @return the normalized filename, or null if invalid
     */
    fun normalizeRaw(filename: String): String? {
        return doNormalize(filename, SYSTEM_SEPARATOR, true)
    }



    /*
     * FilenameUtils.java (normalize + dependencies) - Apache 2.0 License
     *   http://commons.apache.org/proper/commons-io/
     *   Copyright 2013 ASF
     *   Authors: Kevin A. Burton, Scott Sanders, Daniel Rall, Christoph.Reck,
     *            Peter Donald, Jeff Turner, Matthew Hawthorne, Martin Cooper,
     *            Jeremias Maerki, Stephen Colebourne
     */
    /**
     * Normalizes a path, removing double and single dot path steps.
     *
     *
     * This method normalizes a path to a standard format.
     * The input may contain separators in either Unix or Windows format.
     * The output will contain separators in the format of the system.
     *
     *
     * A trailing slash will be retained.
     * A double slash will be merged to a single slash (but UNC names are handled).
     * A single dot path segment will be removed.
     * A double dot will cause that path segment and the one before to be removed.
     * If the double dot has no parent path segment to work with, `null`
     * is returned.
     *
     *
     * The output will be the same on both Unix and Windows except
     * for the separator character.
     * <pre>
     * /foo//               -->   /foo/
     * /foo/./              -->   /foo/
     * /foo/../bar          -->   /bar
     * /foo/../bar/         -->   /bar/
     * /foo/../bar/../baz   -->   /baz
     * //foo//./bar         -->   /foo/bar
     * /../                 -->   null
     * ../foo               -->   null
     * foo/bar/..           -->   foo/
     * foo/../../bar        -->   null
     * foo/../bar           -->   bar
     * //server/foo/../bar  -->   //server/bar
     * //server/../bar      -->   null
     * C:\foo\..\bar        -->   C:\bar
     * C:\..\bar            -->   null
     * ~/foo/../bar/        -->   ~/bar/
     * ~/../bar             -->   null
    </pre> *
     * (Note the file separator returned will be correct for Windows/Unix)
     *
     * @param filename the file to normalize, null returns null
     * @return the normalized file, or null if invalid
     */
    fun normalize(filename: String): File? {
        val asString = doNormalize(File(filename).absolutePath, SYSTEM_SEPARATOR, true) ?: return null
        return File(asString).absoluteFile
    }



    /*
     * FilenameUtils.java (normalize + dependencies) - Apache 2.0 License
     *   http://commons.apache.org/proper/commons-io/
     *   Copyright 2013 ASF
     *   Authors: Kevin A. Burton, Scott Sanders, Daniel Rall, Christoph.Reck,
     *            Peter Donald, Jeff Turner, Matthew Hawthorne, Martin Cooper,
     *            Jeremias Maerki, Stephen Colebourne
     */
    /**
     * Normalizes a path, removing double and single dot path steps.
     *
     *
     * This method normalizes a path to a standard format.
     * The input may contain separators in either Unix or Windows format.
     * The output will contain separators in the format of the system.
     *
     *
     * A trailing slash will be retained.
     * A double slash will be merged to a single slash (but UNC names are handled).
     * A single dot path segment will be removed.
     * A double dot will cause that path segment and the one before to be removed.
     * If the double dot has no parent path segment to work with, `null`
     * is returned.
     *
     *
     * The output will be the same on both Unix and Windows except
     * for the separator character.
     * <pre>
     * /foo//               -->   /foo/
     * /foo/./              -->   /foo/
     * /foo/../bar          -->   /bar
     * /foo/../bar/         -->   /bar/
     * /foo/../bar/../baz   -->   /baz
     * //foo//./bar         -->   /foo/bar
     * /../                 -->   null
     * ../foo               -->   null
     * foo/bar/..           -->   foo/
     * foo/../../bar        -->   null
     * foo/../bar           -->   bar
     * //server/foo/../bar  -->   //server/bar
     * //server/../bar      -->   null
     * C:\foo\..\bar        -->   C:\bar
     * C:\..\bar            -->   null
     * ~/foo/../bar/        -->   ~/bar/
     * ~/../bar             -->   null
    </pre> *
     * (Note the file separator returned will be correct for Windows/Unix)
     *
     * @param file the file to normalize, null returns null
     * @return the normalized file, or null if invalid
     */
    fun normalize(file: File): File? {
        val asString = doNormalize(file.absolutePath, SYSTEM_SEPARATOR, true) ?: return null
        return File(asString).absoluteFile
    }



    /*
     * FilenameUtils.java (normalize + dependencies) - Apache 2.0 License
     *   http://commons.apache.org/proper/commons-io/
     *   Copyright 2013 ASF
     *   Authors: Kevin A. Burton, Scott Sanders, Daniel Rall, Christoph.Reck,
     *            Peter Donald, Jeff Turner, Matthew Hawthorne, Martin Cooper,
     *            Jeremias Maerki, Stephen Colebourne
     */
    /**
     * Normalizes a path, removing double and single dot path steps.
     *
     *
     * This method normalizes a path to a standard format.
     * The input may contain separators in either Unix or Windows format.
     * The output will contain separators in the format specified.
     *
     *
     * A trailing slash will be retained.
     * A double slash will be merged to a single slash (but UNC names are handled).
     * A single dot path segment will be removed.
     * A double dot will cause that path segment and the one before to be removed.
     * If the double dot has no parent path segment to work with, `null`
     * is returned.
     *
     *
     * The output will be the same on both Unix and Windows except
     * for the separator character.
     * <pre>
     * /foo//               -->   /foo/
     * /foo/./              -->   /foo/
     * /foo/../bar          -->   /bar
     * /foo/../bar/         -->   /bar/
     * /foo/../bar/../baz   -->   /baz
     * //foo//./bar         -->   /foo/bar
     * /../                 -->   null
     * ../foo               -->   null
     * foo/bar/..           -->   foo/
     * foo/../../bar        -->   null
     * foo/../bar           -->   bar
     * //server/foo/../bar  -->   //server/bar
     * //server/../bar      -->   null
     * C:\foo\..\bar        -->   C:\bar
     * C:\..\bar            -->   null
     * ~/foo/../bar/        -->   ~/bar/
     * ~/../bar             -->   null
    </pre> *
     * The output will be the same on both Unix and Windows including
     * the separator character.
     *
     * @param filename      the filename to normalize, null returns null
     * @param unixSeparator `true` if a unix separator should
     * be used or `false` if a windows separator should be used.
     * @return the normalized filename, or null if invalid
     */
    fun normalize(filename: String, unixSeparator: Boolean): String? {
        val separator = if (unixSeparator) UNIX_SEPARATOR else WINDOWS_SEPARATOR
        return doNormalize(filename, separator, true)
    }



    //-----------------------------------------------------------------------
    /*
     * FilenameUtils.java (normalize + dependencies) - Apache 2.0 License
     *   http://commons.apache.org/proper/commons-io/
     *   Copyright 2013 ASF
     *   Authors: Kevin A. Burton, Scott Sanders, Daniel Rall, Christoph.Reck,
     *            Peter Donald, Jeff Turner, Matthew Hawthorne, Martin Cooper,
     *            Jeremias Maerki, Stephen Colebourne
     */
    /**
     * Normalizes a path, removing double and single dot path steps,
     * and removing any final directory separator.
     *
     *
     * This method normalizes a path to a standard format.
     * The input may contain separators in either Unix or Windows format.
     * The output will contain separators in the format of the system.
     *
     *
     * A trailing slash will be removed.
     * A double slash will be merged to a single slash (but UNC names are handled).
     * A single dot path segment will be removed.
     * A double dot will cause that path segment and the one before to be removed.
     * If the double dot has no parent path segment to work with, `null`
     * is returned.
     *
     *
     * The output will be the same on both Unix and Windows except
     * for the separator character.
     * <pre>
     * /foo//               -->   /foo
     * /foo/./              -->   /foo
     * /foo/../bar          -->   /bar
     * /foo/../bar/         -->   /bar
     * /foo/../bar/../baz   -->   /baz
     * //foo//./bar         -->   /foo/bar
     * /../                 -->   null
     * ../foo               -->   null
     * foo/bar/..           -->   foo
     * foo/../../bar        -->   null
     * foo/../bar           -->   bar
     * //server/foo/../bar  -->   //server/bar
     * //server/../bar      -->   null
     * C:\foo\..\bar        -->   C:\bar
     * C:\..\bar            -->   null
     * ~/foo/../bar/        -->   ~/bar
     * ~/../bar             -->   null
    </pre> *
     * (Note the file separator returned will be correct for Windows/Unix)
     *
     * @param filename the filename to normalize, null returns null
     * @return the normalized filename, or null if invalid
     */
    fun normalizeNoEndSeparator(filename: String): String? {
        return doNormalize(filename, SYSTEM_SEPARATOR, false)
    }



    /*
     * FilenameUtils.java (normalize + dependencies) - Apache 2.0 License
     *   http://commons.apache.org/proper/commons-io/
     *   Copyright 2013 ASF
     *   Authors: Kevin A. Burton, Scott Sanders, Daniel Rall, Christoph.Reck,
     *            Peter Donald, Jeff Turner, Matthew Hawthorne, Martin Cooper,
     *            Jeremias Maerki, Stephen Colebourne
     */
    /**
     * Normalizes a path, removing double and single dot path steps,
     * and removing any final directory separator.
     *
     *
     * This method normalizes a path to a standard format.
     * The input may contain separators in either Unix or Windows format.
     * The output will contain separators in the format specified.
     *
     *
     * A trailing slash will be removed.
     * A double slash will be merged to a single slash (but UNC names are handled).
     * A single dot path segment will be removed.
     * A double dot will cause that path segment and the one before to be removed.
     * If the double dot has no parent path segment to work with, `null`
     * is returned.
     *
     *
     * The output will be the same on both Unix and Windows including
     * the separator character.
     * <pre>
     * /foo//               -->   /foo
     * /foo/./              -->   /foo
     * /foo/../bar          -->   /bar
     * /foo/../bar/         -->   /bar
     * /foo/../bar/../baz   -->   /baz
     * //foo//./bar         -->   /foo/bar
     * /../                 -->   null
     * ../foo               -->   null
     * foo/bar/..           -->   foo
     * foo/../../bar        -->   null
     * foo/../bar           -->   bar
     * //server/foo/../bar  -->   //server/bar
     * //server/../bar      -->   null
     * C:\foo\..\bar        -->   C:\bar
     * C:\..\bar            -->   null
     * ~/foo/../bar/        -->   ~/bar
     * ~/../bar             -->   null
    </pre> *
     *
     * @param filename      the filename to normalize, null returns null
     * @param unixSeparator `true` if a unix separator should
     * be used or `false` if a windows separtor should be used.
     * @return the normalized filename, or null if invalid
     */
    fun normalizeNoEndSeparator(filename: String, unixSeparator: Boolean): String? {
        val separator = if (unixSeparator) UNIX_SEPARATOR else WINDOWS_SEPARATOR
        return doNormalize(filename, separator, false)
    }



    /*
     * FilenameUtils.java (normalize + dependencies) - Apache 2.0 License
     *   http://commons.apache.org/proper/commons-io/
     *   Copyright 2013 ASF
     *   Authors: Kevin A. Burton, Scott Sanders, Daniel Rall, Christoph.Reck,
     *            Peter Donald, Jeff Turner, Matthew Hawthorne, Martin Cooper,
     *            Jeremias Maerki, Stephen Colebourne
     */
    /**
     * Internal method to perform the normalization.
     *
     * @param filename      the filename
     * @param separator     The separator character to use
     * @param keepSeparator true to keep the final separator
     * @return the normalized filename
     */
    private fun doNormalize(filename: String, separator: Char, keepSeparator: Boolean): String? {
        var size = filename.length
        if (size == 0) {
            return filename
        }

        val prefix = getPrefixLength(filename)
        if (prefix < 0) {
            return null
        }

        val array = CharArray(size + 2) // +1 for possible extra slash, +2 for arraycopy
        filename.toCharArray(array, 0, 0, filename.length)

        // fix separators throughout
        val otherSeparator = if (separator == SYSTEM_SEPARATOR) OTHER_SEPARATOR else SYSTEM_SEPARATOR
        for (i in array.indices) {
            if (array[i] == otherSeparator) {
                array[i] = separator
            }
        }

        // add extra separator on the end to simplify code below
        var lastIsDirectory = true
        if (array[size - 1] != separator) {
            array[size++] = separator
            lastIsDirectory = false
        }

        // adjoining slashes
        run {
            var i = prefix + 1
            while (i < size) {
                if (array[i] == separator && array[i - 1] == separator) {
                    System.arraycopy(array, i, array, i - 1, size - i)
                    size--
                    i--
                }
                i++
            }
        }

        // dot slash
        var i = prefix + 1
        while (i < size) {
            if (array[i] == separator && array[i - 1] == '.' && (i == prefix + 1 || array[i - 2] == separator)) {
                if (i == size - 1) {
                    lastIsDirectory = true
                }
                System.arraycopy(array, i + 1, array, i - 1, size - i)
                size -= 2
                i--
            }
            i++
        }

        i = prefix + 2
        outer@ while (i < size) {
            if (array[i] == separator && array[i - 1] == '.' && array[i - 2] == '.' && (i == prefix + 2 || array[i - 3] == separator)) {
                if (i == prefix + 2) {
                    return null
                }
                if (i == size - 1) {
                    lastIsDirectory = true
                }
                var j: Int
                j = i - 4
                while (j >= prefix) {
                    if (array[j] == separator) {
                        // remove b/../ from a/b/../c
                        System.arraycopy(array, i + 1, array, j + 1, size - i)
                        size -= i - j
                        i = j + 1
                        i++
                        continue@outer
                    }
                    j--
                }
                // remove a/../ from a/../c
                System.arraycopy(array, i + 1, array, prefix, size - i)
                size -= i + 1 - prefix
                i = prefix + 1
            }
            i++
        }

        if (size <= 0) {  // should never be less than 0
            return ""
        }

        if (size <= prefix) {  // should never be less than prefix
            return String(array, 0, size)
        }

        return if (lastIsDirectory && keepSeparator) {
            String(array, 0, size) // keep trailing separator
        } else String(array, 0, size - 1)
        // lose trailing separator
    }



    //-----------------------------------------------------------------------
    /*
     * FilenameUtils.java (normalize + dependencies) - Apache 2.0 License
     *   http://commons.apache.org/proper/commons-io/
     *   Copyright 2013 ASF
     *   Authors: Kevin A. Burton, Scott Sanders, Daniel Rall, Christoph.Reck,
     *            Peter Donald, Jeff Turner, Matthew Hawthorne, Martin Cooper,
     *            Jeremias Maerki, Stephen Colebourne
     */
    /**
     * Returns the length of the filename prefix, such as `C:/` or `~/`.
     *
     *
     * This method will handle a file in either Unix or Windows format.
     *
     *
     * The prefix length includes the first slash in the full filename
     * if applicable. Thus, it is possible that the length returned is greater
     * than the length of the input string.
     ````
     Windows:
     a\b\c.txt           --> ""          --> relative
     \a\b\c.txt          --> "\"         --> current drive absolute
     C:a\b\c.txt         --> "C:"        --> drive relative
     C:\a\b\c.txt        --> "C:\"       --> absolute
     \\server\a\b\c.txt  --> "\\server\" --> UNC

     Unix:
     a/b/c.txt           --> ""          --> relative
     /a/b/c.txt          --> "/"         --> absolute
     ~/a/b/c.txt         --> "~/"        --> current user
     ~                   --> "~/"        --> current user (slash added)
     ~user/a/b/c.txt     --> "~user/"    --> named user
     ~user               --> "~user/"    --> named user (slash added)
     ````
     *
     *
     *
     * The output will be the same irrespective of the machine that the code is running on.
     * ie. both Unix and Windows prefixes are matched regardless.
     *
     * @param filename the filename to find the prefix in, null returns -1
     * @return the length of the prefix, -1 if invalid or null
     */
    fun getPrefixLength(filename: String): Int {
        val len = filename.length
        if (len == 0) {
            return 0
        }

        var ch0 = filename[0]
        if (ch0 == ':') {
            return -1
        }

        return if (len == 1) {
            if (ch0 == '~') {
                return 2 // return a length greater than the input
            }
            if (isSeparator(ch0)) 1 else 0
        } else {
            if (ch0 == '~') {
                var posUnix = filename.indexOf(UNIX_SEPARATOR, 1)
                var posWin = filename.indexOf(WINDOWS_SEPARATOR, 1)
                if (posUnix == -1 && posWin == -1) {
                    return len + 1 // return a length greater than the input
                }
                posUnix = if (posUnix == -1) posWin else posUnix
                posWin = if (posWin == -1) posUnix else posWin
                return Math.min(posUnix, posWin) + 1
            }
            val ch1 = filename[1]
            if (ch1 == ':') {
                ch0 = ch0.uppercaseChar()
                if (ch0 >= 'A' && ch0 <= 'Z') {
                    return if (len == 2 || isSeparator(filename[2]) == false) {
                        2
                    } else 3
                }
                -1
            } else if (isSeparator(ch0) && isSeparator(ch1)) {
                var posUnix = filename.indexOf(UNIX_SEPARATOR, 2)
                var posWin = filename.indexOf(WINDOWS_SEPARATOR, 2)
                if (posUnix == -1 && posWin == -1 || posUnix == 2 || posWin == 2) {
                    return -1
                }
                posUnix = if (posUnix == -1) posWin else posUnix
                posWin = if (posWin == -1) posUnix else posWin
                Math.min(posUnix, posWin) + 1
            } else {
                if (isSeparator(ch0)) 1 else 0
            }
        }
    }


    //-----------------------------------------------------------------------
    /*
     * FilenameUtils.java (normalize + dependencies) - Apache 2.0 License
     *   http://commons.apache.org/proper/commons-io/
     *   Copyright 2013 ASF
     *   Authors: Kevin A. Burton, Scott Sanders, Daniel Rall, Christoph.Reck,
     *            Peter Donald, Jeff Turner, Matthew Hawthorne, Martin Cooper,
     *            Jeremias Maerki, Stephen Colebourne
     */
    /**
     * Checks if the character is a separator.
     *
     * @param ch the character to check
     * @return true if it is a separator character
     */
    private fun isSeparator(ch: Char): Boolean {
        return ch == UNIX_SEPARATOR || ch == WINDOWS_SEPARATOR
    }

    /**
     * Gets the extension of a file (text after the last '.')
     *
     * @return "" if there is no extension
     */
    fun getExtension(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        return if (dot > -1) {
            fileName.substring(dot + 1)
        } else {
            ""
        }
    }

    /**
     * Gets the name of a file that is before the extension (text before the last '.')
     *
     * @return non-null
     */
    fun getNameWithoutExtension(fileName: String): String {
        val dot = fileName.lastIndexOf('.')
        return if (dot > -1) {
            fileName.substring(0, dot)
        } else {
            fileName
        }
    }
}
