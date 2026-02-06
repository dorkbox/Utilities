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

import dorkbox.os.OS.type
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Loads the specified library, extracting it from the jar, if necessary
 */
object NativeLoader {
    /**
     * Gets the version number.
     */
    val version = Sys.version

    @Throws(IOException::class)
    fun extractLibrary(sourceFileName: String, destinationDirectory: String?, destinationName: String, version: String?): File {
        return try {
            val suffix = type.libraryNames[0]

            val outputFileName = if (version == null) {
                destinationName + suffix
            } else {
                "$destinationName.$version$suffix"
            }

            val file = File(destinationDirectory, outputFileName)
            if (!file.canRead() || file.length() == 0L || !file.canExecute()) {
                // now we copy it out
                val inputStream = LocationResolver.getResourceAsStream(sourceFileName) ?: throw IllegalArgumentException("Cannot find sourceFileName!")

                inputStream.use {
                    FileOutputStream(file).use {
                        inputStream.copyTo(it)
                    }
                }
            }
            file
        } catch (e: Exception) {
            throw IOException("Error extracting library: $sourceFileName", e)
        }
    }
}

fun File.loadAsLibrary() {
    // inject into the correct classloader
    System.load(absolutePath)
}

