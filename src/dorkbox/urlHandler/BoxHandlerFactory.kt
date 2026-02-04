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

import java.io.IOException
import java.io.NotSerializableException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory

class BoxHandlerFactory(private val transparentJar: BoxHandler) : URLStreamHandlerFactory, Cloneable {
    override fun createURLStreamHandler(protocol: String): URLStreamHandler? {
        // transparent jar handler.
        return if (BoxHandler.protocol == protocol) {
            this.transparentJar
        }
        else {
            // use the default URLStreamHandlers
            null
        }
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
