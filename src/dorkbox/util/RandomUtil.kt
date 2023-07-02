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

/**
 * This class uses the MersenneTwisterFast, which is MOSTLY random.
 */
object RandomUtil {
    /**
     * Gets the version number.
     */
    val version = Sys.version

    private val random: FastThreadLocal<MersenneTwisterFast> = object : FastThreadLocal<MersenneTwisterFast>() {
        override fun initialValue(): MersenneTwisterFast {
            return MersenneTwisterFast()
        }
    }

    /**
     * Creates the thread local MersenneTwister (as it's not thread safe), if necessary
     */
    fun get(): MersenneTwisterFast {
        return random.get()
    }

    /**
     * Returns a get integer
     */
    fun int(): Int {
        return get().nextInt()
    }

    /**
     * Returns a number between 0 (inclusive) and the specified value (inclusive).
     */
    fun int(range: Int): Int {
        return get().nextInt(range + 1)
    }

    /**
     * Returns a number between start (inclusive) and end (inclusive).
     */
    fun int(start: Int, end: Int): Int {
        return start + get().nextInt(end - start + 1)
    }

    /**
     * Returns a boolean value.
     */
    fun bool(): Boolean {
        return get().nextBoolean()
    }

    /**
     * Returns number between 0.0 (inclusive) and 1.0 (exclusive).
     */
    fun float(): Float {
        return get().nextFloat()
    }

    /**
     * Returns a number between 0 (inclusive) and the specified value (exclusive).
     */
    fun float(range: Float): Float {
        return get().nextFloat() * range
    }

    /**
     * Returns a number between start (inclusive) and end (exclusive).
     */
    fun float(start: Float, end: Float): Float {
        return start + get().nextFloat() * (end - start)
    }

    /**
     * Places random bytes in the specified byte array
     */
    fun bytes(bytes: ByteArray) {
        get().nextBytes(bytes)
    }
}
