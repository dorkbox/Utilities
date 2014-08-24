/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// pruned/limited version from libGDX

package dorkbox.util;


public class MathUtils {

 // ---

    private static ThreadLocal<MersenneTwisterFast> random = new ThreadLocal<MersenneTwisterFast>();

    /** Returns a random integer */
    static public final int randomInt () {
        return random.get().nextInt();
    }

    /** Returns a random number between 0 (inclusive) and the specified value (inclusive). */
    static public final int randomInt (int range) {
        return random.get().nextInt(range + 1);
    }

    /** Returns a random number between start (inclusive) and end (inclusive). */
    static public final int randomInt (int start, int end) {
        return start + random.get().nextInt(end - start + 1);
    }

    /** Returns a random boolean value. */
    static public final boolean randomBoolean () {
        return random.get().nextBoolean();
    }

    /** Returns random number between 0.0 (inclusive) and 1.0 (exclusive). */
    static public final float randomFloat () {
        return random.get().nextFloat();
    }

    /** Returns a random number between 0 (inclusive) and the specified value (exclusive). */
    static public final float randomFloat (float range) {
        return random.get().nextFloat() * range;
    }

    /** Returns a random number between start (inclusive) and end (exclusive). */
    static public final float randomFloat (float start, float end) {
        return start + random.get().nextFloat() * (end - start);
    }

    // ---


    /**
     * Returns the next power of two. Returns the specified value if the value
     * is already a power of two.
     */
    public static int nextPowerOfTwo(int value) {
        if (value == 0) {
            return 1;
        }

        value--;

        value |= value >> 1;
        value |= value >> 2;
        value |= value >> 4;
        value |= value >> 8;
        value |= value >> 16;

        return value + 1;
    }

    public static boolean isPowerOfTwo(int value) {
        return value != 0 && (value & value - 1) == 0;
    }
}
