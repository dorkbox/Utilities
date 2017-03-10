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


public
class RandomUtil {

    private static final FastThreadLocal<MersenneTwisterFast> random = new FastThreadLocal<MersenneTwisterFast>() {
        @Override
        public
        MersenneTwisterFast initialValue() {
            return new MersenneTwisterFast();
        }
    };

    /**
     * Creates the thread local MersenneTwister (as it's not thread safe), if necessary
     */
    public static
    MersenneTwisterFast get() {
        return random.get();
    }

    /**
     * Returns a get integer
     */
    public static
    int int_() {
        return get().nextInt();
    }

    /**
     * Returns a get number between 0 (inclusive) and the specified value (inclusive).
     */
    public static
    int int_(int range) {
        return get().nextInt(range + 1);
    }

    /**
     * Returns a get number between start (inclusive) and end (inclusive).
     */
    public static
    int int_(int start, int end) {
        return start + get().nextInt(end - start + 1);
    }

    /**
     * Returns a get boolean value.
     */
    public static
    boolean bool() {
        return get().nextBoolean();
    }

    /**
     * Returns get number between 0.0 (inclusive) and 1.0 (exclusive).
     */
    public static
    float float_() {
        return get().nextFloat();
    }

    /**
     * Returns a get number between 0 (inclusive) and the specified value (exclusive).
     */
    public static
    float float_(float range) {
        return get().nextFloat() * range;
    }

    /**
     * Returns a get number between start (inclusive) and end (exclusive).
     */
    public static
    float float_(float start, float end) {
        return start + get().nextFloat() * (end - start);
    }
}
