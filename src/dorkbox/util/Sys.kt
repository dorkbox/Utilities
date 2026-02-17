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

import dorkbox.util.Sys.GIGABYTE
import dorkbox.util.Sys.KILOBYTE
import dorkbox.util.Sys.MEGABYTE
import dorkbox.util.Sys.TERABYTE
import dorkbox.util.Sys.throwException0
import java.util.*
import java.util.concurrent.*

@Suppress("unused")
object Sys {
    /**
     * Gets the version number.
     */
    val version = "2.0"

    init {
        // Add this project to the updates system, which verifies this class + UUID + version information
        dorkbox.updates.Updates.add(Sys::class.java, "aebbb926aeb144739e9f3cab90ffaa72", version)
    }


    const val KILOBYTE = 1024
    const val MEGABYTE = 1024 * KILOBYTE
    const val GIGABYTE = 1024 * MEGABYTE
    const val TERABYTE = 1024L * GIGABYTE

    fun <T : Throwable> throwException0(t: Throwable) {
        @Suppress("UNCHECKED_CAST")
        throw t as T
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
fun String.replaceWith(replacements: Map<String, String>): String {
    val sb = StringBuilder(this)
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
 * Returns a PRETTY string representation of the specified size.
 */
fun Long.asSize(): String {
    val size = this
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

/**
 * Returns a PRETTY string representation of the specified size.
 */
fun Int.asSize(): String {
    val size = this
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
fun Long.asTime(): String {
    val nanoSeconds = this
    val unit: TimeUnit
    val text: String
    if (TimeUnit.DAYS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0L) {
        unit = TimeUnit.DAYS
        text = "d"
    } else if (TimeUnit.HOURS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0L) {
        unit = TimeUnit.HOURS
        text = "h"
    } else if (TimeUnit.MINUTES.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0L) {
        unit = TimeUnit.MINUTES
        text = "m"
    } else if (TimeUnit.SECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0L) {
        unit = TimeUnit.SECONDS
        text = "s"
    } else if (TimeUnit.MILLISECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0L) {
        unit = TimeUnit.MILLISECONDS
        text = "ms"
    } else if (TimeUnit.MICROSECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0L) {
        unit = TimeUnit.MICROSECONDS
        text = "\u03bcs" // μs
    } else {
        unit = TimeUnit.NANOSECONDS
        text = "ns"
    }

    // convert the unit into the largest time unit possible (since that is often what makes sense)
    val value = nanoSeconds.toDouble() / TimeUnit.NANOSECONDS.convert(1, unit)

    return if (value < 10) {
        String.format("%.1g $text", value)
    } else if (value < 100) {
        String.format("%.2g $text", value)
    } else if (value < 1000) {
        String.format("%.3g $text", value)
    } else {
        String.format("%.4g $text", value)
    }
}

/**
 * Returns a PRETTY string representation of the specified time.
 */
fun Long.asTimeFull(): String {
    val nanoSeconds = this
    val unit: TimeUnit
    var text: String
    if (TimeUnit.DAYS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0L) {
        unit = TimeUnit.DAYS
        text = "day"
    } else if (TimeUnit.HOURS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0L) {
        unit = TimeUnit.HOURS
        text = "hour"
    } else if (TimeUnit.MINUTES.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0L) {
        unit = TimeUnit.MINUTES
        text = "minute"
    } else if (TimeUnit.SECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0L) {
        unit = TimeUnit.SECONDS
        text = "second"
    } else if (TimeUnit.MILLISECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0L) {
        unit = TimeUnit.MILLISECONDS
        text = "milli-second"
    } else if (TimeUnit.MICROSECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0L) {
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

    return if (value < 10) {
        String.format("%.1g $text", value)
    } else if (value < 100) {
        String.format("%.2g $text", value)
    } else if (value < 1000) {
        String.format("%.3g $text", value)
    } else {
        String.format("%.4g $text", value)
    }
}



/**
 * Converts a Thrown exception to bypass the compiler checks for the checked exception. This uses type erasure to work.
 */
fun Throwable.unchecked() {
    throwException0<RuntimeException>(this)
}


/**
 * Erase the contents of a string, in-memory. This has no effect if the string has been interned.
 */
fun String.erase() {
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
        val chars = valueField[this] as CharArray
        Arrays.fill(chars, '*') // asterisk it out in case of GC not picking up the old char array.
        valueField[this] = CharArray(0) // replace it.

        // set count to 0
        try {
            // newer versions of java don't have this field
            val countField = String::class.java.getDeclaredField("count")
            countField.isAccessible = true
            countField[this] = 0
        }
        catch (ignored: Exception) {
        }

        // set hash to 0
        val hashField = String::class.java.getDeclaredField("hash")
        hashField.isAccessible = true
        hashField[this] = 0
    }
    catch (e: Exception) {
        e.printStackTrace()
    }
}
