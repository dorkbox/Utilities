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

object MathUtil {
    /**
     * Gets the version number.
     */
    val version = Sys.version
}

/**
 * Checks to see if the string is an integer
 *
 * @return true if it's an integer, false otherwise
 */
fun String.isInteger(): Boolean {
    return isNumber(10)
}

/**
 * Checks to see if the string is a long
 *
 * @return true if it's a long, false otherwise
 */
fun String.isLong(): Boolean {
    return isNumber(19)
}

/**
 * Checks to see if the character is a number
 *
 * @return true if it's a number, false otherwise
 */
fun Char.isNumber(): Boolean {
    // way faster than Character.isDigit()
    return this in '0'..'9'
}

/**
 * Checks to see if the string is a number
 *
 * @return true if it's a number, false otherwise
 */
fun String.isNumber(sizeLimit: Long): Boolean {
    if (sizeLimit <= 0) {
        return false
    }
    val length = length
    if (length == 0) {
        return false
    }
    var i = 0
    if (this[0] == '-') {
        if (length == 1) {
            return false
        }
        i = 1
    }
    if (length - i > sizeLimit) {
        return false
    }
    while (i < length) {
        val c = this[i]
        // way faster than Character.isDigit()
        if (c !in '0'..'9') {
            return false
        }
        i++
    }
    return true
}

/**
 * Gets the number of digits represented by the specified number
 *
 * @return the number of digits of the number, from 1-19.
 */
fun Long.countDigits(): Int {
    // have to make it always positive for the following checks to pass.
    var number = this
    if (number < 0L) {
        number = -number
    }

    // Guessing 4 digit numbers will be more probable.
    // They are set in the first branch.
    return if (number < 10000L) { // from 1 to 4
        if (number < 100L) { // 1 or 2
            if (number < 10L) {
                1
            } else {
                2
            }
        } else { // 3 or 4
            if (number < 1000L) {
                3
            } else {
                4
            }
        }
    } else { // from 5 to 20 (albeit longs can't have more than 18 or 19)
        if (number < 1000000000000L) { // from 5 to 12
            if (number < 100000000L) { // from 5 to 8
                if (number < 1000000L) { // 5 or 6
                    if (number < 100000L) {
                        5
                    } else {
                        6
                    }
                } else { // 7 u 8
                    if (number < 10000000L) {
                        7
                    } else {
                        8
                    }
                }
            } else { // from 9 to 12
                if (number < 10000000000L) { // 9 or 10
                    if (number < 1000000000L) {
                        9
                    } else {
                        10
                    }
                } else { // 11 or 12
                    if (number < 100000000000L) {
                        11
                    } else {
                        12
                    }
                }
            }
        } else { // from 13 to ... (18 or 20)
            if (number < 10000000000000000L) { // from 13 to 16
                if (number < 100000000000000L) { // 13 or 14
                    if (number < 10000000000000L) {
                        13
                    } else {
                        14
                    }
                } else { // 15 or 16
                    if (number < 1000000000000000L) {
                        15
                    } else {
                        16
                    }
                }
            } else { // from 17 to ... 20?
                if (number < 1000000000000000000L) { // 17 or 18
                    if (number < 100000000000000000L) {
                        17
                    } else {
                        18
                    }
                } else { // 19? Can it be?
                    // 10000000000000000000L isn't a valid long.
                    19
                }
            }
        }
    }
}

/**
 * Removes any characters from the end that are not a number
 *
 * @return the value as an integer
 */
fun String.trimNonDigits(): Int {
    if (isEmpty()) {
        return 0
    }

    var numberIndex = 0
    val length = length
    while (numberIndex < length && this[numberIndex].isNumber()) {
        numberIndex++
    }

    val substring = substring(0, numberIndex)
    try {
        return substring.toInt()
    } catch (ignored: Exception) {
    }
    return 0
}

fun Int.isEven(): Boolean {
    return this and 1 == 0
}

fun Int.isOdd(): Boolean {
    return this and 1 != 0
}

fun Long.isEven(): Boolean {
    return this and 1L == 0L
}

fun Long.isOdd(): Boolean {
    return this and 1L != 0L
}

/**
 * Returns the next power of two. Returns the specified value if the value is already a power of two.
 */
fun Int.nextPowerOfTwo(): Int {
    return 1 shl 32 - Integer.numberOfLeadingZeros(this - 1)
}

fun Int.isPowerOfTwo(): Boolean {
    return this != 0 && this and this - 1 == 0
}

fun intersectRect(x1: Double, y1: Double, w1: Double, h1: Double, x2: Double, y2: Double, w2: Double, h2: Double): Boolean {
    return intersectRange(x1, x1 + w1, x2, x2 + w2) && intersectRange(y1, y1 + h1, y2, y2 + h2)
}

fun intersectRange(ax1: Double, ax2: Double, bx1: Double, bx2: Double): Boolean {
    return ax1.coerceAtLeast(bx1) <= ax2.coerceAtMost(bx2)
}

/**
 * Clamps a value to min/max values, where it cannot exceed either.
 *
 * @return the new value, clamped
 */
fun Int.clamp(min: Int, max: Int): Int {
    return min.coerceAtLeast(max.coerceAtMost(this))
}

/**
 * Clamps a value to min/max values, where it cannot exceed either.
 *
 * @return the new value, clamped
 */
fun Long.clamp(min: Long, max: Long): Long {
    return min.coerceAtLeast(max.coerceAtMost(this))
}

/**
 * Clamps a value to min/max values, where it cannot exceed either.
 *
 * @return the new value, clamped
 */
fun Float.clamp(min: Float, max: Float): Float {
    return min.coerceAtLeast(max.coerceAtMost(this))
}

/**
 * Clamps a value to min/max values, where it cannot exceed either.
 *
 * @return the new value, clamped
 */
fun Double.clamp(min: Double, max: Double): Double {
    return min.coerceAtLeast(max.coerceAtMost(this))
}
