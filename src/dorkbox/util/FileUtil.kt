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
package dorkbox.util

import dorkbox.os.OS
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*
import java.util.*
import kotlin.time.Clock

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
    val version = Sys.version

    val log: Logger = LoggerFactory.getLogger(FileUtil::class.java)

    const val DEBUG = false

    /**
     * The Unix separator character.
     */
    const val UNIX_SEPARATOR = '/'

    /**
     * The Windows separator character.
     */
    const val WINDOWS_SEPARATOR = '\\'

    /**
     * The system separator character.
     */
    val SYSTEM_SEPARATOR = File.separatorChar

    /**
     * The separator character that is the opposite of the system separator.
     */
    val OTHER_SEPARATOR = if (OS.isWindows) {
        UNIX_SEPARATOR
    }
    else {
        WINDOWS_SEPARATOR
    }

    fun getPid(pidFileName: String): String? {
        val stringBuilder = StringBuilder()
        return if (File(pidFileName).readText(stringBuilder, null)) {
            stringBuilder.toString()
        }
        else {
            null
        }
    }
}


/**
 * Touches a file, so that it's timestamp is right now. If the file is not created, it will be created automatically.
 *
 * @return true if the touch succeeded, false otherwise
 */
fun File.touch(timestamp: Long = Clock.System.now().toEpochMilliseconds()): Boolean {
    if (!exists()) {
        val mkdirs = parentFile.mkdirs()
        if (!mkdirs) {
            // error creating the parent directories.
            return false
        }
        try {
            FileOutputStream(this).close()
        } catch (ignored: IOException) {
            return false
        }
    }
    return setLastModified(timestamp)
}

/**
 * Creates a temp file
 */
@Throws(IOException::class)
fun tempFile(fileName: String): File {
    return File.createTempFile(fileName, null).normalize().absoluteFile
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
    return file.normalize().absolutePath
}

fun File.prependText(vararg strings: String) {
    // make sure we can write to the file
    parentFile?.mkdirs()

    val contents = LinkedList<String>()

    for (string in strings) {
        contents.add(string)
    }

    // have to read ORIGINAL file, since we can't prepend it any other way....
    readOnePerLine(this, contents, false)

    writeText(contents)
}

fun File.appendText(vararg text: String) {
    // make sure we can write to the file
    parentFile?.mkdirs()

    // wooooo for auto-closable and try-with-resources
    FileWriter(this, true).use { fw ->
        BufferedWriter(fw).use { bw ->
            PrintWriter(bw).use { out ->
                for (s in text) {
                    out.println(s)
                }
            }
        }
    }
}

fun File.appendText(text: List<String>) {
    // make sure we can write to the file
    parentFile?.mkdirs()

    // wooooo for auto-closable and try-with-resources
    FileWriter(this, true).use { fw ->
        BufferedWriter(fw).use { bw ->
            PrintWriter(bw).use { out ->

                for (s in text) {
                    out.println(s)
                }
            }
        }
    }
}

fun File.write(vararg text: String) {
    // make sure we can write to the file
    parentFile?.mkdirs()

    // wooooo for auto-closable and try-with-resources
    FileWriter(this, false).use { fw ->
        BufferedWriter(fw).use { bw ->
            PrintWriter(bw).use { out ->

                for (s in text) {
                    out.println(s)
                }
            }
        }
    }
}

fun File.writeText(text: List<String>) {
    // make sure we can write to the file
    parentFile?.mkdirs()

    // wooooo for auto-closable and try-with-resources
    FileWriter(this, false).use { fw ->
        BufferedWriter(fw).use { bw ->
            PrintWriter(bw).use { out ->

                for (s in text) {
                    out.println(s)
                }
            }
        }
    }
}



/**
 * Reads the content of a file to the passed in StringBuilder.
 *
 * @param stringBuilder the stringBuilder this file will be written to
 */
@Throws(IOException::class)
fun File.readText(stringBuilder: StringBuilder) {
    FileReader(this).use {
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
fun File.readText(builder: StringBuilder, lineSeparator: String?): Boolean {
    if (!canRead()) {
        return false
    }

    try {
        reader().use { reader ->
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
fun File.readText(action: FileUtil.Action): Boolean {
    if (!canRead()) {
        return false
    }

    try {
        reader().use { reader ->
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
 * @return the first line in the file, excluding the "new line" character.
 */
fun File.readFirstLine(): String {
    if (!canRead()) {
        return ""
    }

    return reader().use { reader ->
        reader.buffered().lineSequence().firstOrNull()
    } ?: ""
}



/**
 * Reads the contents of the supplied input stream into a list of lines.
 *
 *
 * Closes the reader on successful or failed completion.
 *
 * @return Always returns a list, even if the file does not exist, or there are errors reading it.
 */
fun Reader.readLines(): List<String> {
    val lines: MutableList<String> = ArrayList()

    BufferedReader(this).use {
        val bin = BufferedReader(this)
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
    fun readOnePerLine(file: File, list: MutableList<String>, trimStrings: Boolean) {
        if (trimStrings) {
            file.readText(object : FileUtil.Action {
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
            file.readText(object : FileUtil.Action {
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
 * Renames a file. Windows has all sorts of problems which are worked around.
 *
 * @return true if successful, false otherwise
 */
fun File.renameTo(dest: File): Boolean {
    // if we're on a civilized operating system we may be able to simple
    // rename it
    if (renameTo(dest)) {
        return true
    }

    // fall back to trying to rename the old file out of the way, rename the
    // new file into
    // place and then delete the old file
    if (dest.exists()) {
        val temp = File(dest.path + "_old")
        if (temp.exists()) {
            if (!temp.delete()) {
                if (FileUtil.DEBUG) {
                    println("Failed to delete old intermediate file: $temp")
                }

                // the subsequent code will probably fail
            }
        }
        if (dest.renameTo(temp)) {
            if (renameTo(dest)) {
                if (temp.delete()) {
                    if (FileUtil.DEBUG) {
                        println("Failed to delete intermediate file: $temp")
                    }
                }
                return true
            }
        }
    }

    // as a last resort, try copying the old data over the new
    return try {
        copyTo(dest)
        if (!delete()) {
            if (FileUtil.DEBUG) {
                println("Failed to delete '$this' after brute force copy to '$dest'.")
            }
        }
        true
    } catch (ioe: IOException) {
        if (FileUtil.DEBUG) {
            println("Failed to copy '$this' to '$dest'.")
            ioe.printStackTrace()
        }
        false
    }
}

/**
 * Copies the contents of file two onto the END of file one.
 */
fun File.concat(other: File): File {
    if (FileUtil.DEBUG) {
        println("Concatenating file: '$this'  -->  '$other'")
    }

    this.appendBytes(other.readBytes())

    this.setLastModified(System.currentTimeMillis())
    return this
}

    /**
     * Copies a directory from one location to another
     */
    @Throws(IOException::class)
    fun File.copyDirectoryTo(dest: File, vararg namesToIgnore: String) {
        val src = this.normalize()
        val dest = dest.normalize()

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
                if (FileUtil.DEBUG) {
                    println("Directory copied from  '$src'  -->  '$dest'")
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
                    srcFile.copyDirectoryTo(destFile, *namesToIgnore)
                }
            }
        } else {
            // if file, then copy it
            src.copyTo(dest)
        }
    }

    /**
     * Safely moves a directory from one location to another (by copying it first, then deleting the original).
     */
    @Throws(IOException::class)
    fun File.moveDirectoryTo(dest: File, vararg fileNamesToIgnore: String) {
        val src = this.normalize()

        if (fileNamesToIgnore.isNotEmpty()) {
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
                if (FileUtil.DEBUG) {
                    println("Directory copied from  '$src'  -->  '$dest'")
                }
            }

            // list all the directory contents
            val files = src.list()
            if (files != null) {
                for (file in files) {
                    // construct the src and dest file structure
                    val srcFile = File(src, file)
                    val destFile = File(dest, file)

                    // recursive move
                    srcFile.moveDirectoryTo(destFile, *fileNamesToIgnore)
                }
            }
        } else {
            // if file, then copy it
            src.renameTo(dest)
        }
    }

/**
 * Deletes a file, directory + all files and sub-directories under it. The directory is ALSO deleted if it is empty as a result
 * of this operation
 *
 * @param namesToIgnore if prefaced with a '/', it will treat the name to ignore as a directory instead of a file
 *
 * @return true IFF the file/dir was deleted or didn't exist at first
 */
fun File.deleteRecursively(vararg namesToIgnore: String): Boolean {
    if (!exists()) {
        return true
    }

    var thingsDeleted = false
    var ignored = false
    if (isDirectory) {
        val files = listFiles()
        if (files != null) {
            var i = 0
            val n = files.size
            while (i < n) {
                var delete = true
                val file2 = files[i]
                val name2 = file2.name
                val name2Full = file2.normalize().absolutePath
                if (file2.isDirectory) {
                    for (name in namesToIgnore) {
                        if (name[0] == FileUtil.UNIX_SEPARATOR && name == name2) {
                            // only name match if our name To Ignore starts with a / or \
                            if (FileUtil.DEBUG) {
                                println("Skipping delete dir: $file2")
                            }
                            ignored = true
                            delete = false
                            break
                        } else if (name == name2Full) {
                            // full path match
                            if (FileUtil.DEBUG) {
                                println("Skipping delete dir: $file2")
                            }
                            ignored = true
                            delete = false
                            break
                        }
                    }
                    if (delete) {
                        if (FileUtil.DEBUG) {
                            println("Deleting dir: $file2")
                        }
                        file2.deleteRecursively(*namesToIgnore)
                    }
                } else {
                    for (name in namesToIgnore) {
                        if (name[0] != FileUtil.UNIX_SEPARATOR && name == name2) {
                            // only name match
                            if (FileUtil.DEBUG) {
                                println("Skipping delete file: $file2")
                            }
                            ignored = true
                            delete = false
                            break
                        } else if (name == name2Full) {
                            // full path match
                            if (FileUtil.DEBUG) {
                                println("Skipping delete file: $file2")
                            }
                            ignored = true
                            delete = false
                            break
                        }
                    }
                    if (delete) {
                        if (FileUtil.DEBUG) {
                            println("Deleting file: $file2")
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
        if (FileUtil.DEBUG) {
            println("Skipping deleting file: $this")
        }
        return false
    }
    if (FileUtil.DEBUG) {
        println("Deleting file: $this")
    }
    thingsDeleted = thingsDeleted or delete()
    return thingsDeleted
}




/**
 * Parses the specified root directory for files that end in the extension to match. All the sub-directories are searched as well.
 *
 * @return the list of all files in the root+sub-dirs that match the given extension.
 */
@Throws(IOException::class)
fun File.parseDir(vararg extensionsToMatch: String): List<File> {
    val fileList: MutableList<File> = LinkedList()
    val directories = LinkedList<File?>()

    val rootDirectory = this.normalize()
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
                            fileList.add(file)
                        } else {
                            for (e in extensionsToMatch) {
                                if (file.absolutePath.endsWith(e)) {
                                    fileList.add(file)
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
    return fileList
}


/**
 * Gets the relative path of a file to a specific directory in it's hierarchy.
 *
 *
 * For example: getChildRelativeToDir("/a/b/c/d/e.bah", "c") -> "d/e.bah"
 *
 * @return null if there is no child
 */
fun File.getChildRelativeToDir(dirInHierarchy: String): String? {
    require(dirInHierarchy.isEmpty()) { "dirInHierarchy cannot be empty." }

    val split = dirInHierarchy.split(File.separator).toTypedArray()
    var splitIndex = split.size - 1
    val absolutePath = this.absolutePath
    var parent: File? = this
    var parentName: String

    if (splitIndex == 0) {
        // match on ONE dir
        while (parent != null) {
            parentName = parent.name
            if (parentName == dirInHierarchy) {
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
                        return if (absolutePath.length == dirInHierarchy.length) {
                            null
                        } else absolutePath.substring(dirInHierarchy.length + 1, absolutePath.length)

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
 * Gets the relative path of a file to a specific directory in it's hierarchy.
 *
 *
 * For example: getParentRelativeToDir("/a/b/c/d/e.bah", "c") -> "/a/b"
 *
 * @return null if it cannot be found
 */
fun File.getParentRelativeToDir(dirInHierarchy: String): String? {
    require(dirInHierarchy.isEmpty()) { "dirInHierarchy cannot be empty." }

    val split = dirInHierarchy.split(File.separator).toTypedArray()
    var splitIndex = split.size - 1
    var parent: File? = this
    var parentName: String
    if (splitIndex == 0) {
        // match on ONE dir
        while (parent != null) {
            parentName = parent.name
            if (parentName == dirInHierarchy) {
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

