/*
 * Copyright 2023 dorkbox, llc
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

import dorkbox.os.OS.LINE_SEPARATOR
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.concurrent.*

@Suppress("unused")
object Sys {
    /**
     * Gets the version number.
     */
    val version = "1.42"

    init {
        // Add this project to the updates system, which verifies this class + UUID + version information
        dorkbox.updates.Updates.add(Sys::class.java, "aebbb926aeb144739e9f3cab90ffaa72", version)
    }


    const val KILOBYTE = 1024
    const val MEGABYTE = 1024 * KILOBYTE
    const val GIGABYTE = 1024 * MEGABYTE
    const val TERABYTE = 1024L * GIGABYTE
    val HEX_CHARS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

    fun convertStringToChars(string: String): CharArray {
        val charArray = string.toCharArray()
        eraseString(string)
        return charArray
    }

    fun eraseString(string: String?) {
        // You can change the value of the inner char[] using reflection.
        //
        // You must be careful to either change it with an array of the same length,
        // or to also update the count field.
        //
        // If you want to be able to use it as an entry in a set or as a value in map,
        // you will need to recalculate the hash code and set the value of the hashCode field.
        try {
            val valueField = String::class.java.getDeclaredField("value")
            valueField.isAccessible = true
            val chars = valueField[string] as CharArray
            Arrays.fill(chars, '*') // asterisk it out in case of GC not picking up the old char array.
            valueField[string] = CharArray(0) // replace it.

            // set count to 0
            try {
                // newer versions of java don't have this field
                val countField = String::class.java.getDeclaredField("count")
                countField.isAccessible = true
                countField[string] = 0
            } catch (ignored: Exception) {
            }

            // set hash to 0
            val hashField = String::class.java.getDeclaredField("hash")
            hashField.isAccessible = true
            hashField[string] = 0
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * FROM: https://www.cqse.eu/en/blog/string-replace-performance/
     *
     *
     * Replaces all occurrences of keys of the given map in the given string with the associated value in that map.
     *
     *
     * This method is semantically the same as calling [String.replace] for each of the
     * entries in the map, but may be significantly faster for many replacements performed on a short string, since
     * [String.replace] uses regular expressions internally and results in many String
     * object allocations when applied iteratively.
     *
     *
     * The order in which replacements are applied depends on the order of the map's entry set.
     */
    fun replaceStringFast(string: String?, replacements: Map<String, String>): String {
        val sb = StringBuilder(string)
        for ((key, value) in replacements) {
            var start = sb.indexOf(key, 0)
            while (start > -1) {
                val end = start + key.length
                val nextSearchStart = start + value.length
                sb.replace(start, end, value)
                start = sb.indexOf(key, nextSearchStart)
            }
        }
        return sb.toString()
    }

    /**
     * Quickly finds a char in a string.
     *
     * @return index if it's there, -1 if not there
     */
    fun searchStringFast(string: String, c: Char): Int {
        val length = string.length
        for (i in 0 until length) {
            if (string[i] == c) {
                return i
            }
        }
        return -1
    }

    fun getSizePretty(size: Long): String {
        if (size > TERABYTE) {
            return String.format("%2.2fTB", size.toDouble() / TERABYTE)
        }
        if (size > GIGABYTE) {
            return String.format("%2.2fGB", size.toDouble() / GIGABYTE)
        }
        if (size > MEGABYTE) {
            return String.format("%2.2fMB", size.toDouble() / MEGABYTE)
        }
        return if (size > KILOBYTE) {
            String.format("%2.2fKB", size.toDouble() / KILOBYTE)
        } else size.toString() + "B"
    }

    fun getSizePretty(size: Int): String {
        if (size > GIGABYTE) {
            return String.format("%2.2fGB", size.toDouble() / GIGABYTE)
        }
        if (size > MEGABYTE) {
            return String.format("%2.2fMB", size.toDouble() / MEGABYTE)
        }
        return if (size > KILOBYTE) {
            String.format("%2.2fKB", size.toDouble() / KILOBYTE)
        } else size.toString() + "B"
    }

    /**
     * Returns a PRETTY string representation of the specified time.
     */
    fun getTimePretty(nanoSeconds: Long): String {
        val unit: TimeUnit
        val text: String
        if (TimeUnit.DAYS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.DAYS
            text = "d"
        } else if (TimeUnit.HOURS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.HOURS
            text = "h"
        } else if (TimeUnit.MINUTES.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.MINUTES
            text = "min"
        } else if (TimeUnit.SECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.SECONDS
            text = "s"
        } else if (TimeUnit.MILLISECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.MILLISECONDS
            text = "ms"
        } else if (TimeUnit.MICROSECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.MICROSECONDS
            text = "\u03bcs" // μs
        } else {
            unit = TimeUnit.NANOSECONDS
            text = "ns"
        }

        // convert the unit into the largest time unit possible (since that is often what makes sense)
        val value = nanoSeconds.toDouble() / TimeUnit.NANOSECONDS.convert(1, unit)
        return String.format("%.4g$text", value)
    }

    /**
     * Returns a PRETTY string representation of the specified time.
     */
    fun getTimePrettyFull(nanoSeconds: Long): String {
        val unit: TimeUnit
        var text: String
        if (TimeUnit.DAYS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.DAYS
            text = "day"
        } else if (TimeUnit.HOURS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.HOURS
            text = "hour"
        } else if (TimeUnit.MINUTES.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.MINUTES
            text = "minute"
        } else if (TimeUnit.SECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.SECONDS
            text = "second"
        } else if (TimeUnit.MILLISECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.MILLISECONDS
            text = "milli-second"
        } else if (TimeUnit.MICROSECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.MICROSECONDS
            text = "micro-second"
        } else {
            unit = TimeUnit.NANOSECONDS
            text = "nano-second"
        }

        // convert the unit into the largest time unit possible (since that is often what makes sense)
        val value = nanoSeconds.toDouble() / TimeUnit.NANOSECONDS.convert(1, unit)
        if (value > 1.0) {
            text += "s"
        }
        return String.format("%.4g $text", value)
    }

    private fun <T : Throwable> throwException0(t: Throwable) {
        @Suppress("UNCHECKED_CAST")
        throw t as T
    }

    /**
     * Converts a Thrown exception, to bypasses the compiler checks for the checked exception. This uses type erasure to work.
     */
    fun Throwable.unchecked() {
        throwException0<RuntimeException>(this)
    }




    /**
     * Convert the contents of the input stream to a byte array.
     */
    @Throws(IOException::class)
    fun getBytesFromStream(inputStream: InputStream): ByteArray {
        val baos = ByteArrayOutputStream(8192)
        val buffer = ByteArray(4096)
        var read: Int
        while (inputStream.read(buffer).also { read = it } > 0) {
            baos.write(buffer, 0, read)
        }
        baos.flush()
        inputStream.close()
        return baos.toByteArray()
    }

    @JvmOverloads
    fun copyBytes(src: ByteArray, position: Int = 0): ByteArray {
        val length = src.size - position
        val b = ByteArray(length)
        System.arraycopy(src, position, b, 0, length)
        return b
    }

    @JvmStatic
    fun concatBytes(vararg arrayBytes: ByteArray): ByteArray {
        var length = 0
        for (bytes in arrayBytes) {
            length += bytes.size
        }
        val concatBytes = ByteArray(length)
        length = 0
        for (bytes in arrayBytes) {
            System.arraycopy(bytes, 0, concatBytes, length, bytes.size)
            length += bytes.size
        }
        return concatBytes
    }

    @JvmOverloads
    fun bytesToHex(bytes: ByteArray, startPosition: Int = 0, length: Int = bytes.size, padding: Boolean = false): String {
        val endPosition = startPosition + length
        return if (padding) {
            val hexString = CharArray(3 * length)
            var j = 0
            for (i in startPosition until endPosition) {
                hexString[j++] = HEX_CHARS[bytes[i].toInt() and 0xF0 shr 4]
                hexString[j++] = HEX_CHARS[bytes[i].toInt() and 0x0F]
                hexString[j++] = ' '
            }
            String(hexString)
        } else {
            val hexString = CharArray(2 * length)
            var j = 0
            for (i in startPosition until endPosition) {
                hexString[j++] = HEX_CHARS[bytes[i].toInt() and 0xF0 shr 4]
                hexString[j++] = HEX_CHARS[bytes[i].toInt() and 0x0F]
            }
            String(hexString)
        }
    }

    /**
     * Converts an ASCII character representing a hexadecimal
     * value into its integer equivalent.
     */
    fun hexByteToInt(b: Byte): Int {
        return when (b.toInt()) {
            '0'.code -> 0
            '1'.code -> 1
            '2'.code -> 2
            '3'.code -> 3
            '4'.code -> 4
            '5'.code -> 5
            '6'.code -> 6
            '7'.code -> 7
            '8'.code -> 8
            '9'.code -> 9
            'A'.code, 'a'.code -> 10
            'B'.code, 'b'.code -> 11
            'C'.code, 'c'.code -> 12
            'D'.code, 'd'.code -> 13
            'E'.code, 'e'.code -> 14
            'F'.code, 'f'.code -> 15
            else -> throw IllegalArgumentException("Error decoding byte")
        }
    }

    /**
     * Converts an ASCII character representing a hexadecimal
     * value into its integer equivalent.
     */
    fun hexCharToInt(b: Char): Int {
        return when (b) {
            '0' -> 0
            '1' -> 1
            '2' -> 2
            '3' -> 3
            '4' -> 4
            '5' -> 5
            '6' -> 6
            '7' -> 7
            '8' -> 8
            '9' -> 9
            'A', 'a' -> 10
            'B', 'b' -> 11
            'C', 'c' -> 12
            'D', 'd' -> 13
            'E', 'e' -> 14
            'F', 'f' -> 15
            else -> throw IllegalArgumentException("Error decoding byte")
        }
    }

    /**
     * A 4-digit hex result.
     */
    fun hex4(c: Char, sb: StringBuilder) {
        sb.append(HEX_CHARS[c.code and 0xF000 shr 12])
        sb.append(HEX_CHARS[c.code and 0x0F00 shr 8])
        sb.append(HEX_CHARS[c.code and 0x00F0 shr 4])
        sb.append(HEX_CHARS[c.code and 0x000F])
    }

    /**
     * Returns a string representation of the byte array as a series of
     * hexadecimal characters.
     *
     * @param bytes byte array to convert
     * @return a string representation of the byte array as a series of
     * hexadecimal characters
     */
    fun toHexString(bytes: ByteArray): String {
        val hexString = CharArray(2 * bytes.size)
        var j = 0
        for (i in bytes.indices) {
            hexString[j++] = HEX_CHARS[bytes[i].toInt() and 0xF0 shr 4]
            hexString[j++] = HEX_CHARS[bytes[i].toInt() and 0x0F]
        }
        return String(hexString)
    }

    /**
     * from netty 4.1, apache 2.0, https://netty.io
     */
    fun hexToByte(s: CharSequence, pos: Int): Byte {
        val hi = hexCharToInt(s[pos])
        val lo = hexCharToInt(s[pos + 1])
        require(!(hi == -1 || lo == -1)) {
            String.format(
                "invalid hex byte '%s' at index %d of '%s'", s.subSequence(pos, pos + 2), pos, s
            )
        }
        return ((hi shl 4) + lo).toByte()
    }

    /**
     * Decodes a string with [hex dump](http://en.wikipedia.org/wiki/Hex_dump)
     *
     * @param hex a [CharSequence] which contains the hex dump
     */
    fun hexToBytes(hex: CharSequence): ByteArray {
        return hexToBytes(hex, 0, hex.length)
    }

    /**
     * Decodes part of a string with [hex dump](http://en.wikipedia.org/wiki/Hex_dump)
     *
     * from netty 4.1, apache 2.0, https://netty.io
     *
     * @param hexDump a [CharSequence] which contains the hex dump
     * @param fromIndex start of hex dump in `hexDump`
     * @param length hex string length
     */
    fun hexToBytes(hexDump: CharSequence, fromIndex: Int, length: Int): ByteArray {
        require(!(length < 0 || length and 1 != 0)) { "length: $length" }
        if (length == 0) {
            return ByteArray(0)
        }
        val bytes = ByteArray(length ushr 1)
        var i = 0
        while (i < length) {
            bytes[i ushr 1] = hexToByte(hexDump, fromIndex + i)
            i += 2
        }
        return bytes
    }



    fun encodeStringArray(array: List<String>): ByteArray {
        var length = 0
        for (s in array) {
            val bytes = s.toByteArray()
            length += bytes.size
        }
        if (length == 0) {
            return ByteArray(0)
        }
        val bytes = ByteArray(length + array.size)
        length = 0
        for (s in array) {
            val sBytes = s.toByteArray()
            System.arraycopy(sBytes, 0, bytes, length, sBytes.size)
            length += sBytes.size
            bytes[length++] = 0x01.toByte()
        }
        return bytes
    }

    fun decodeStringArray(bytes: ByteArray): ArrayList<String> {
        val length = bytes.size
        var position = 0
        val token = 0x01.toByte()
        val list = ArrayList<String>(0)
        var last = 0
        while (last + position < length) {
            val b = bytes[last + position++]
            if (b == token) {
                val xx = ByteArray(position - 1)
                System.arraycopy(bytes, last, xx, 0, position - 1)
                list.add(String(xx))
                last += position
                position = 0
            }
        }
        return list
    }

    @JvmOverloads
    fun printArrayRaw(bytes: ByteArray, lineLength: Int = 0): String {
        return if (lineLength > 0) {
            val length = bytes.size
            val comma = length - 1
            val builder = StringBuilder(length + length / lineLength)
            for (i in 0 until length) {
                builder.append(bytes[i].toInt())
                if (i < comma) {
                    builder.append(",")
                }
                if (i > 0 && i % lineLength == 0) {
                    builder.append(LINE_SEPARATOR)
                }
            }
            builder.toString()
        } else {
            val length = bytes.size
            val comma = length - 1
            val builder = StringBuilder(length + length)
            for (i in 0 until length) {
                builder.append(bytes[i].toInt())
                if (i < comma) {
                    builder.append(",")
                }
            }
            builder.toString()
        }
    }

    @JvmOverloads
    fun printArray(bytes: ByteArray, length: Int = bytes.size, includeByteCount: Boolean = true) {
        printArray(bytes, 0, length, includeByteCount, 40, null)
    }

    @JvmOverloads
    fun printArray(
        bytes: ByteArray,
        inputOffset: Int,
        length: Int,
        includeByteCount: Boolean,
        lineLength: Int = 40,
        header: String? = null
    ) {
        val comma = length - 1
        var builderLength = length + comma + 2
        if (includeByteCount) {
            builderLength += 7 + Integer.toString(length).length
        }
        if (lineLength > 0) {
            builderLength += length / lineLength
        }
        if (header != null) {
            builderLength += header.length + 2
        }
        val builder = StringBuilder(builderLength)
        if (header != null) {
            builder.append(header).append(LINE_SEPARATOR)
        }
        if (includeByteCount) {
            builder.append("Bytes: ").append(length).append(LINE_SEPARATOR)
        }
        builder.append("{")
        for (i in inputOffset until length) {
            builder.append(bytes[i].toInt())
            if (i < comma) {
                builder.append(",")
            }
            if (i > inputOffset && lineLength > 0 && i % lineLength == 0) {
                builder.append(LINE_SEPARATOR)
            }
        }
        builder.append("}")
        System.err.println(builder.toString())
    }


}
