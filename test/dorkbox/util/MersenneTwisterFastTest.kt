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

import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.util.*

class MersenneTwisterFastTest {
    @Test
    @Throws(IOException::class)
    fun mersenneTwisterTest() {
        var j: Int
        var r: MersenneTwisterFast

        // CORRECTNESS TEST
        // COMPARE WITH
        // http://www.math.keio.ac.jp/matumoto/CODES/MT2002/mt19937ar.out
        r = MersenneTwisterFast(intArrayOf(0x123, 0x234, 0x345, 0x456))
        // System.out.println("Output of MersenneTwisterFast with new (2002/1/26) seeding mechanism");
        j = 0
        while (j < 1000) {

            // first, convert the int from signed to "unsigned"
            var l = r.nextInt().toLong()
            if (l < 0) {
                l += 4294967296L // max int value
            }
            var s = l.toString()
            while (s.length < 10) {
                s = " $s" // buffer
            }
            print("$s ")
            if (j % 5 == 4) {
                println()
            }
            j++
        }

        // SPEED TEST
        val SEED: Long = 4357
        var xx: Int
        var ms: Long
        println("\nTime to test grabbing 100000000 ints")
        val rr = Random(SEED)
        xx = 0
        ms = System.currentTimeMillis()
        j = 0
        while (j < 100000000) {
            xx += rr.nextInt()
            j++
        }
        println("java.util.Random: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx)
        r = MersenneTwisterFast(SEED)
        ms = System.currentTimeMillis()
        xx = 0
        j = 0
        while (j < 100000000) {
            xx += r.nextInt()
            j++
        }
        println(
            "Mersenne Twister Fast: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx
        )

        // TEST TO COMPARE TYPE CONVERSION BETWEEN
        // MersenneTwisterFast.java AND MersenneTwister.java
        var test = false
        println("\nGrab the first 1000 booleans")
        ms = System.currentTimeMillis()
        r = MersenneTwisterFast(SEED)
        j = 0
        while (j < 1000) {

            // System.out.print(r.nextBoolean() + " ");
            test = r.nextBoolean()
            if (j % 8 == 7) {
                // System.out.println();
                test = false
            }
            j++
        }
        if (j % 8 != 7) {
            // System.out.println();
            test = true
        }
        println(
            "Mersenne Twister Fast: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx + " " + test
        )
        println("\nGrab 1000 booleans of increasing probability using nextBoolean(double)")
        r = MersenneTwisterFast(SEED)
        ms = System.currentTimeMillis()
        j = 0
        while (j < 1000) {

            // System.out.print(r.nextBoolean(j / 999.0) + " ");
            test = r.nextBoolean(j / 999.0)
            if (j % 8 == 7) {
                // System.out.println();
                test = false
            }
            j++
        }
        if (j % 8 != 7) {
            // System.out.println();
            test = true
        }
        println(
            "Mersenne Twister Fast: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx + " " + test
        )
        println("\nGrab 1000 booleans of increasing probability using nextBoolean(float)")
        r = MersenneTwisterFast(SEED)
        ms = System.currentTimeMillis()
        j = 0
        while (j < 1000) {

            // System.out.print(r.nextBoolean(j / 999.0f) + " ");
            test = r.nextBoolean(j / 999.0f)
            if (j % 8 == 7) {
                test = false
                // println()
            }
            j++
        }
        if (j % 8 != 7) {
            // System.out.println();
            test = true
        }
        println(
            "Mersenne Twister Fast: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx + " " + test
        )

        val bytes = ByteArray(1000)
        println("\nGrab the first 1000 bytes using nextBytes")
        r = MersenneTwisterFast(SEED)
        r.nextBytes(bytes)
        j = 0
        while (j < 1000) {
            print(bytes[j].toString() + " ")
            if (j % 16 == 15) {
                // println()
                test = false
            }
            j++
        }
        if (j % 16 != 15) {
            // println()
            test = true
        }
        println(
            "Mersenne Twister Fast: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx + " " + test
        )


        var b: Byte = 0
        println("\nGrab the first 1000 bytes -- must be same as nextBytes")
        r = MersenneTwisterFast(SEED)
        j = 0
        while (j < 1000) {
            print(r.nextByte().also { b = it }.toString() + " ")
            if (b != bytes[j]) {
//                print("BAD ")
                Assert.fail("Bytes not equal!")
            }
            if (j % 16 == 15) {
                test = true
//                println()
            }
            j++
        }
        if (j % 16 != 15) {
//            println()
            test = true
        }
        println(
            "Mersenne Twister Fast: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx + " " + test
        )

        println("\nGrab the first 1000 shorts")
        r = MersenneTwisterFast(SEED)
        j = 0
        while (j < 1000) {
            print(r.nextShort().toString() + " ")
            if (j % 8 == 7) {
//                println()
                test = true
            }
            j++
        }
        if (j % 8 != 7) {
//            println()
            test = false
        }
        println(
            "Mersenne Twister Fast: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx + " " + test
        )

        println("\nGrab the first 1000 ints")
        r = MersenneTwisterFast(SEED)
        j = 0
        while (j < 1000) {
            print(r.nextInt().toString() + " ")
            if (j % 4 == 3) {
//                println()
                test = true
            }
            j++
        }
        if (j % 4 != 3) {
//            println()
            test = false
        }
        println(
            "Mersenne Twister Fast: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx + " " + test
        )

        println("\nGrab the first 1000 ints of different sizes")
        r = MersenneTwisterFast(SEED)
        var max = 1
        j = 0
        while (j < 1000) {
            print(r.nextInt(max).toString() + " ")
            max *= 2
            if (max <= 0) {
                max = 1
            }
            if (j % 4 == 3) {
//                println()
                test = true
            }
            j++
        }
        if (j % 4 != 3) {
//            println()
            test = false
        }
        println(
            "Mersenne Twister Fast: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx + " " + test
        )

        println("\nGrab the first 1000 longs")
        r = MersenneTwisterFast(SEED)
        j = 0
        while (j < 1000) {
            print(r.nextLong().toString() + " ")
            if (j % 3 == 2) {
//                println()
                test = true
            }
            j++
        }
        if (j % 3 != 2) {
//            println()
            test = false
        }
        println(
            "Mersenne Twister Fast: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx + " " + test
        )


        println("\nGrab the first 1000 longs of different sizes")
        r = MersenneTwisterFast(SEED)
        var max2: Long = 1
        j = 0
        while (j < 1000) {
            print(r.nextLong(max2).toString() + " ")
            max2 *= 2
            if (max2 <= 0) {
                max2 = 1
            }
            if (j % 4 == 3) {
//                println()
                test = true
            }
            j++
        }
        if (j % 4 != 3) {
//            println()
            test = false
        }
        println(
            "Mersenne Twister Fast: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx + " " + test
        )


        println("\nGrab the first 1000 floats")
        r = MersenneTwisterFast(SEED)
        j = 0
        while (j < 1000) {
            print(r.nextFloat().toString() + " ")
            if (j % 4 == 3) {
//                println()
                test = true
            }
            j++
        }
        if (j % 4 != 3) {
//          println()
            test = false
        }
        println(
            "Mersenne Twister Fast: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx + " " + test
        )


        println("\nGrab the first 1000 doubles")
        r = MersenneTwisterFast(SEED)
        j = 0
        while (j < 1000) {
            print(r.nextDouble().toString() + " ")
            if (j % 3 == 2) {
//                println()
                test = true
            }
            j++
        }
        if (j % 3 != 2) {
//            println()
            test = false
        }
        println(
            "Mersenne Twister Fast: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx + " " + test
        )


        println("\nGrab the first 1000 gaussian doubles")
        r = MersenneTwisterFast(SEED)
        j = 0
        while (j < 1000) {
            print(r.nextGaussian().toString() + " ")
            if (j % 3 == 2) {
//                println()
                test = true
            }
            j++
        }
        if (j % 3 != 2) {
//            println()
            test = false
        }
        println(
            "Mersenne Twister Fast: " + (System.currentTimeMillis() - ms) + "          Ignore this: " + xx + " " + test
        )
    }
}
