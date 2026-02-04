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
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

class BoxHandler : URLStreamHandler(), Cloneable {
    companion object {
        //
        // This is also in the (ClassLoader project) Node!!!, but I didn't want to force a dependency just because of this.
        //
        //
        // The following must ALL be valid URI symbols, defined by RFC 3986: http://tools.ietf.org/html/rfc3986#section-2
        //
        // ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~:/?#[]@!$&'()*+,;=.
        //
        // Any other character needs to be encoded with the percent-encoding (%hh). Each part of the URI has further restrictions about
        // what characters need to be represented by an percent-encoded word.
        /** This is exclusively used to identify if a resource we are requesting is inside of a jar that was already parsed  */
        const val jarUrlSeperator: Char = '*'
        const val jarPathToken: Char = '/'
        const val packageToken: Char = '.'

        const val protocol: String = "box"

        val protocolFull: String = "$protocol:/"
        val protocolLength: Int = protocolFull.length
    }

    @Throws(IOException::class)
    override fun openConnection(url: URL): URLConnection {
        return BoxURLConnection(url)
    }

    /**
     * Makes sure that when creating paths, etc, from this URL, that we also make sure to add a token, so
     * our classloader knows where to find the resource.
     * 
     * This absolutely MUST not end in special characters. it must be the letters/numbers or a "/". NOTHING ELSE.
     */
    override fun toExternalForm(url: URL): String {
        // ONLY append jarUrlSeperator if we haven't already done so!
        val externalForm = super.toExternalForm(url)

        val jarurlseperator: Char = jarUrlSeperator

        if (externalForm.indexOf(jarurlseperator) == -1) {
            val length = externalForm.length
            val stringBuilder = StringBuilder(length + 1)
            stringBuilder.append(externalForm)
            if (length > 1 && externalForm[length - 1] == jarPathToken) {
                stringBuilder.insert(length, jarurlseperator)
            }
            else {
                stringBuilder.append(jarurlseperator)
            }
            return stringBuilder.toString()
        }
        else {
            // we've already modified it, don't do it again.
            return externalForm
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
