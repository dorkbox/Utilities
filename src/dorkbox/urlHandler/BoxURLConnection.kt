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
package dorkbox.urlHandler

import java.io.*
import java.net.URL
import java.net.URLConnection

/**
 * A 'Box' URL is nothing like a JAR/ZIP, HOWEVER, it appears as though it is a jar/zip file.
 */
class BoxURLConnection(url: URL) : URLConnection(url), Cloneable {
    val containerName: String
        /**
         * @return the base name of the url. This will be the internal container (inside the main jar file) that actually contains our resource.
         * This will be empty for class files.
         */
        get() {
            val spec = this.url.path
            val separator = spec.indexOf(BoxHandler.jarUrlSeperator)
            val length = spec.length

            if (separator > 0 && separator != length) {
                if (spec[0] == '/') {
                    if (spec[separator - 1] == '/') {
                        val substring = spec.substring(1, separator - 1)
                        return substring
                    }
                    else {
                        val substring = spec.substring(1, separator)
                        return substring
                    }
                }
                else {
                    if (spec[separator - 1] == '/') {
                        val substring = spec.substring(0, separator - 1)
                        return substring
                    }
                    else {
                        val substring = spec.substring(0, separator)
                        return substring
                    }
                }
            }
            else {
                return ""
            }
        }

    val resourceName: String
        /**
         * @return the name of the entry that is nested inside an internal resource. This would be the name of a file, where the base URL would
         * be the internal resource container.
         */
        get() {
            val spec = this.url.path
            val separator = spec.indexOf(BoxHandler.jarUrlSeperator)

            if (separator > -1 && separator != spec.length) {
                if (spec[separator + 1] == '/') {
                    return spec.substring(separator + 2)
                }
                else {
                    return spec.substring(separator + 1)
                }
            }
            else {
                return ""
            }
        }

    @Throws(IOException::class)
    override fun connect() {
        this.connected = true
    }

    override fun getContentLength(): Int {
        // if we are inside our box file, this will return -1, so inputstreams will be used (which they have to be...)
        // if we return anything other than -1, then our box resource will try to be opened like a file (which we don't want)
        return -1
    }

    override fun getLastModified(): Long {
        return 0
    }

    /**
     * Loads the resources stream, if applicable. You cannot load classes using this method
     */
    @Throws(IOException::class)
    override fun getInputStream(): InputStream? {
        val path = this.url.path

        val length = BoxHandler.protocolLength
        val stringBuilder = StringBuilder(path.length + length)
        stringBuilder.append(BoxHandler.protocolFull)
        if (path[0] == '/') {
            stringBuilder.deleteCharAt(length - 1)
        }
        stringBuilder.append(path)

        val `is` = javaClass.classLoader.getResourceAsStream(stringBuilder.toString())
        return `is`
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        throw CloneNotSupportedException()
    }

    @Throws(IOException::class)
    fun writeObject(out: ObjectOutputStream?) {
        throw NotSerializableException()
    }

    @Throws(IOException::class)
    fun readObject(`in`: ObjectInputStream?) {
        throw NotSerializableException()
    }
}
