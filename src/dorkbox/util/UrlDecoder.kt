/*
 * Copyright (c) 2005-2012, Paul Tuckey
 * All rights reserved.
 * ====================================================================
 * Licensed under the BSD License. Text as follows.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   - Neither the name tuckey.org nor the names of its contributors
 *     may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 * https://www.talisman.org/%7Eerlkonig/misc/lunatech%5Ewhat-every-webdev-must-know-about-url-encoding/
 */
/*
 * Copyright 2021 dorkbox, llc
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 * Derivative code has been released as Apache 2.0, used with permission.
 *
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


import java.io.UnsupportedEncodingException
import java.net.URISyntaxException
import java.nio.charset.Charset

object URLDecoder {
    private const val byte_0 = '0'.code.toByte()
    private const val byte_1 = '1'.code.toByte()
    private const val byte_2 = '2'.code.toByte()
    private const val byte_3 = '3'.code.toByte()
    private const val byte_4 = '4'.code.toByte()
    private const val byte_5 = '5'.code.toByte()
    private const val byte_6 = '6'.code.toByte()
    private const val byte_7= '7'.code.toByte()
    private const val byte_8 = '8'.code.toByte()
    private const val byte_9 = '9'.code.toByte()
    private const val byte_a = 'a'.code.toByte()
    private const val byte_b = 'b'.code.toByte()
    private const val byte_c = 'c'.code.toByte()
    private const val byte_d = 'd'.code.toByte()
    private const val byte_e = 'e'.code.toByte()
    private const val byte_f = 'f'.code.toByte()
    private const val byte_A = 'A'.code.toByte()
    private const val byteB = 'B'.code.toByte()
    private const val byteC = 'C'.code.toByte()
    private const val byte_D= 'D'.code.toByte()
    private const val byte_E= 'E'.code.toByte()
    private const val byte_F = 'F'.code.toByte()

    @Throws(URISyntaxException::class)
    fun decodeURL(url: String, charset: Charset): String {
        val queryPart = url.indexOf('?')
        var query: String? = null
        var path = url
        if (queryPart != -1) {
            query = url.substring(queryPart + 1)
            path = url.substring(0, queryPart)
        }
        val decodedPath = decodePath(path, charset)
        return if (query != null) decodedPath + '?' + decodeQuery(query, charset) else decodedPath
    }

    @Throws(URISyntaxException::class)
    fun decodePath(path: String, charset: Charset): String {
        return decodeURLEncoded(path, false, charset)
    }

    @Throws(URISyntaxException::class)
    fun decodeQuery(query: String, charset: Charset): String {
        return decodeURLEncoded(query, true, charset)
    }

    @Throws(URISyntaxException::class)
    fun decodeURLEncoded(part: String, query: Boolean, charset: Charset): String {
        return try {
            val ascii = part.toByteArray(Charsets.US_ASCII)
            val decoded = ByteArray(ascii.size)
            var j = 0
            var i = 0
            while (i < ascii.size) {
                if (ascii[i] == '%'.code.toByte()) {
                    if (i + 2 >= ascii.size) throw URISyntaxException(part, "Invalid URL-encoded string at char $i")
                    // get the next two bytes
                    val first = ascii[++i]
                    val second = ascii[++i]
                    decoded[j] = (hexToByte(first) * 16 + hexToByte(second)).toByte()
                } else if (query && ascii[i] == '+'.code.toByte()) decoded[j] = ' '.code.toByte() else decoded[j] = ascii[i]
                i++
                j++
            }
            // now decode
            String(decoded, 0, j, charset)
        } catch (x: UnsupportedEncodingException) {
            throw URISyntaxException(part, "Invalid encoding: $charset")
        }
    }



    @Throws(URISyntaxException::class)
    private fun hexToByte(b: Byte): Byte {
        when (b) {
            byte_0 -> return 0
            byte_1 -> return 1
            byte_2 -> return 2
            byte_3 -> return 3
            byte_4 -> return 4
            byte_5 -> return 5
            byte_6 -> return 6
            byte_7 -> return 7
            byte_8 -> return 8
            byte_9 -> return 9
            byte_a, byte_A -> return 10
            byte_b, byteB -> return 11
            byte_c, byteC -> return 12
            byte_d, byte_D -> return 13
            byte_e, byte_E -> return 14
            byte_f, byte_F -> return 15
        }
        throw URISyntaxException(b.toString(), "Invalid URL-encoded string")
    }
}
