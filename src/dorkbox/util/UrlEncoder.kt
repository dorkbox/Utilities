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

package dorkbox.util

import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*

/**
 * URL-encoding utility for each URL part according to the RFC specs
 * see the rfc at http://www.ietf.org/rfc/rfc2396.txt
 *
 * @author stephane
 */
object URLEncoder {
    /**
     * mark = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
     */
    val MARK = BitSet()

    init {
        MARK.set('-'.code)
        MARK.set('_'.code)
        MARK.set('.'.code)
        MARK.set('!'.code)
        MARK.set('~'.code)
        MARK.set('*'.code)
        MARK.set('\''.code)
        MARK.set('('.code)
        MARK.set(')'.code)
    }

    /**
     * lowalpha = "a" | "b" | "c" | "d" | "e" | "f" | "g" | "h" | "i" | "j" | "k" | "l" | "m" | "n" | "o" | "p" | "q" |
     * "r" | "s" | "t" | "u" | "v" | "w" | "x" | "y" | "z"
     */
    val LOW_ALPHA = BitSet()

    init {
        LOW_ALPHA.set('a'.code)
        LOW_ALPHA.set('b'.code)
        LOW_ALPHA.set('c'.code)
        LOW_ALPHA.set('d'.code)
        LOW_ALPHA.set('e'.code)
        LOW_ALPHA.set('f'.code)
        LOW_ALPHA.set('g'.code)
        LOW_ALPHA.set('h'.code)
        LOW_ALPHA.set('i'.code)
        LOW_ALPHA.set('j'.code)
        LOW_ALPHA.set('k'.code)
        LOW_ALPHA.set('l'.code)
        LOW_ALPHA.set('m'.code)
        LOW_ALPHA.set('n'.code)
        LOW_ALPHA.set('o'.code)
        LOW_ALPHA.set('p'.code)
        LOW_ALPHA.set('q'.code)
        LOW_ALPHA.set('r'.code)
        LOW_ALPHA.set('s'.code)
        LOW_ALPHA.set('t'.code)
        LOW_ALPHA.set('u'.code)
        LOW_ALPHA.set('v'.code)
        LOW_ALPHA.set('w'.code)
        LOW_ALPHA.set('x'.code)
        LOW_ALPHA.set('y'.code)
        LOW_ALPHA.set('z'.code)
    }

    /**
     * upalpha = "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "I" | "J" | "K" | "L" | "M" | "N" | "O" | "P" | "Q" |
     * "R" | "S" | "T" | "U" | "V" | "W" | "X" | "Y" | "Z"
     */
    val UP_ALPHA = BitSet()

    init {
        UP_ALPHA.set('A'.code)
        UP_ALPHA.set('B'.code)
        UP_ALPHA.set('C'.code)
        UP_ALPHA.set('D'.code)
        UP_ALPHA.set('E'.code)
        UP_ALPHA.set('F'.code)
        UP_ALPHA.set('G'.code)
        UP_ALPHA.set('H'.code)
        UP_ALPHA.set('I'.code)
        UP_ALPHA.set('J'.code)
        UP_ALPHA.set('K'.code)
        UP_ALPHA.set('L'.code)
        UP_ALPHA.set('M'.code)
        UP_ALPHA.set('N'.code)
        UP_ALPHA.set('O'.code)
        UP_ALPHA.set('P'.code)
        UP_ALPHA.set('Q'.code)
        UP_ALPHA.set('R'.code)
        UP_ALPHA.set('S'.code)
        UP_ALPHA.set('T'.code)
        UP_ALPHA.set('U'.code)
        UP_ALPHA.set('V'.code)
        UP_ALPHA.set('W'.code)
        UP_ALPHA.set('X'.code)
        UP_ALPHA.set('Y'.code)
        UP_ALPHA.set('Z'.code)
    }

    /**
     * alpha = lowalpha | upalpha
     */
    val ALPHA = BitSet()

    init {
        ALPHA.or(LOW_ALPHA)
        ALPHA.or(UP_ALPHA)
    }

    /**
     * digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9"
     */
    val DIGIT = BitSet()

    init {
        DIGIT.set('0'.code)
        DIGIT.set('1'.code)
        DIGIT.set('2'.code)
        DIGIT.set('3'.code)
        DIGIT.set('4'.code)
        DIGIT.set('5'.code)
        DIGIT.set('6'.code)
        DIGIT.set('7'.code)
        DIGIT.set('8'.code)
        DIGIT.set('9'.code)
    }

    /**
     * alphanum = alpha | digit
     */
    val ALPHANUM = BitSet()

    init {
        ALPHANUM.or(ALPHA)
        ALPHANUM.or(DIGIT)
    }

    /**
     * unreserved = alphanum | mark
     */
    val UNRESERVED = BitSet()

    init {
        UNRESERVED.or(ALPHANUM)
        UNRESERVED.or(MARK)
    }

    /**
     * pchar = unreserved | escaped | ":" | "@" | "&" | "=" | "+" | "$" | ","
     *
     *
     * Note: we don't allow escaped here since we will escape it ourselves, so we don't want to allow them in the
     * unescaped sequences
     */
    val PCHAR = BitSet()

    init {
        PCHAR.or(UNRESERVED)
        PCHAR.set(':'.code)
        PCHAR.set('@'.code)
        PCHAR.set('&'.code)
        PCHAR.set('='.code)
        PCHAR.set('+'.code)
        PCHAR.set('$'.code)
        PCHAR.set(','.code)
    }

    /**
     * Encodes a string to be a valid path parameter URL, which means it can contain PCHAR* only (do not put the leading
     * ";" or it will be escaped.
     *
     * @throws UnsupportedEncodingException
     */
    @Throws(UnsupportedEncodingException::class)
    fun encodePathParam(pathParam: String, charset: Charset): String {
        return encodePathSegment(pathParam, charset)
    }

    /**
     * Encodes a string to be a valid path segment URL, which means it can contain PCHAR* only (do not put path
     * parameters or they will be escaped.
     *
     * @throws UnsupportedEncodingException
     */
    @Throws(UnsupportedEncodingException::class)
    fun encodePathSegment(pathSegment: String, charset: Charset): String {
        // start at *3 for the worst case when everything is %encoded on one byte
        val encoded = StringBuffer(pathSegment.length * 3)
        val toEncode = pathSegment.toCharArray()

        for (i in toEncode.indices) {
            val c = toEncode[i]
            if (PCHAR[c.code]) {
                encoded.append(c)
            } else {
                val bytes = c.toString().toByteArray(charset)
                for (j in bytes.indices) {
                    val b = bytes[j]
                    // make it unsigned (safe, since we only goto max 255, but makes conversion to hex easier)
                    val u8: Int = b.toInt() and 0xFF
                    encoded.append("%")
                    if (u8 < 16) encoded.append("0")
                    encoded.append(Integer.toHexString(u8))
                }
            }
        }

        return encoded.toString()
    }
}
