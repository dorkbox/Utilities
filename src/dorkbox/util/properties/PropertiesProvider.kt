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
package dorkbox.util.properties

import java.awt.Color
import java.io.*
import java.util.*

class PropertiesProvider(propertiesFile: File) {
    private val properties: Properties = SortedProperties()
    private val propertiesFile: File
    private var comments = "Settings and configuration file. Strings must be escape formatted!"

    constructor(propertiesFile: String) : this(File(propertiesFile))

    init {
        @Suppress("NAME_SHADOWING")
        val propertiesFile = propertiesFile.normalize()

        // make sure the parent dir exists...
        val parentFile = propertiesFile.parentFile
        if (parentFile != null && !parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                throw RuntimeException("Unable to create directories for: $propertiesFile")
            }
        }

        this.propertiesFile = propertiesFile
        _load()
    }

    fun setComments(comments: String) {
        this.comments = comments
    }

    private fun _load() {
        if (!propertiesFile.canRead() || !propertiesFile.exists()) {
            // in this case, our properties file doesn't exist yet... create one!
            _save()
        }

        try {
            val fis = FileInputStream(propertiesFile)
            properties.load(fis)
            fis.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            // oops!
            println("Properties cannot load!")
            e.printStackTrace()
        }
    }

    private fun _save() {
        try {
            val fos = FileOutputStream(propertiesFile)
            properties.store(fos, comments)
            fos.flush()
            fos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            println("Properties cannot save!")
        } catch (e: IOException) {
            // oops!
            println("Properties cannot save!")
            e.printStackTrace()
        }
    }

    @Synchronized
    fun remove(key: String) {
        properties.remove(key)
        _save()
    }

    @Synchronized
    fun save(key: String?, value: Any?) {
        @Suppress("NAME_SHADOWING")
        var value = value
        if (key == null || value == null) {
            return
        }

        if (value is Color) {
            value = value.rgb
        }

        properties.setProperty(key, value.toString())
        _save()
    }

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    operator fun <T> get(key: String?, clazz: Class<T>?): T? {
        if (key == null || clazz == null) {
            return null
        }
        val property = properties.getProperty(key) ?: return null

        // special cases
        return try {
            if (clazz == Int::class.java) {
                return Integer.valueOf(property.toInt()) as T
            }
            if (clazz == Long::class.java) {
                return java.lang.Long.valueOf(property.toLong()) as T
            }
            if (clazz == Color::class.java) {
                Color(property.toInt(), true) as T
            } else {
                property as T
            }
        } catch (e: Exception) {
            throw RuntimeException("Properties Loader for property: " + key + System.getProperty("line.separator") + e.message)
        }
    }

    override fun toString(): String {
        return "PropertiesProvider [" + propertiesFile + "]"
    }
}
