/*
 * Copyright 2010 dorkbox, llc
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

package dorkbox.util;


@SuppressWarnings("unused")
public
class MathUtil {

    // ---

    private static final ThreadLocal<MersenneTwisterFast> random = new ThreadLocal<MersenneTwisterFast>();

    /**
     * Creates the thread local MersenneTwister (as it's not thread safe), if necessary
     */
    public static
    MersenneTwisterFast random() {
        MersenneTwisterFast mersenneTwisterFast = random.get();

        if (mersenneTwisterFast == null) {
            mersenneTwisterFast = new MersenneTwisterFast();
            random.set(mersenneTwisterFast);
            return mersenneTwisterFast;
        }
        else {
            return mersenneTwisterFast;
        }
    }

    /** Returns a random integer */
    public static
    int randomInt() {
        return random().nextInt();
    }

    /** Returns a random number between 0 (inclusive) and the specified value (inclusive). */
    public static
    int randomInt(int range) {
        return random().nextInt(range + 1);
    }

    /** Returns a random number between start (inclusive) and end (inclusive). */
    public static
    int randomInt(int start, int end) {
        return start + random().nextInt(end - start + 1);
    }

    /** Returns a random boolean value. */
    public static
    boolean randomBoolean() {
        return random().nextBoolean();
    }

    /** Returns random number between 0.0 (inclusive) and 1.0 (exclusive). */
    public static
    float randomFloat() {
        return random().nextFloat();
    }

    /** Returns a random number between 0 (inclusive) and the specified value (exclusive). */
    public static
    float randomFloat(float range) {
        return random().nextFloat() * range;
    }

    /** Returns a random number between start (inclusive) and end (exclusive). */
    public static
    float randomFloat(float start, float end) {
        return start + random().nextFloat() * (end - start);
    }

    // ---


    /**
     * Returns the next power of two. Returns the specified value if the value is already a power of two.
     */
    public static
    int nextPowerOfTwo(int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    public static
    boolean isPowerOfTwo(int value) {
        return value != 0 && (value & value - 1) == 0;
    }
}
