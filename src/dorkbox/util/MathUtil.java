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
class MathUtil {

    /**
     * Checks to see if the string is an integer
     *
     * @return true if it's an integer, false otherwise
     */
    public static
    boolean isInteger(final String string) {
        return isNumber(string, 10);
    }

    /**
     * Checks to see if the string is a long
     *
     * @return true if it's a long, false otherwise
     */
    public static
    boolean isLong(final String string) {
        return isNumber(string, 19);
    }

    /**
     * Checks to see if the character is a number
     *
     * @return true if it's a number, false otherwise
     */
    public static
    boolean isNumber(final char character) {
        // way faster than Character.isDigit()
        return character >= '0' && character <= '9';
    }

    /**
     * Checks to see if the string is a number
     *
     * @return true if it's a number, false otherwise
     */
    public static
    boolean isNumber(final String string, long sizeLimit) {
        if (string == null) {
            return false;
        }
        if (sizeLimit <= 0) {
            return false;
        }

        int length = string.length();
        if (length == 0) {
            return false;
        }

        int i = 0;
        if (string.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }

        if (length - i > sizeLimit) {
            return false;
        }

        for (; i < length; i++) {
            char c = string.charAt(i);
            // way faster than Character.isDigit()
            if (c < '0' || c > '9') {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the number of digits represented by the specified number
     *
     * @param number the number to check, negative numbers are also acceptable but the sign is not counted
     *
     * @return the number of digits of the number, from 1-19.
     */
    public static
    int numberOfDigits(long number) {
        // have to make it always positive for the following checks to pass.
        if (number < 0L) {
            number = -number;
        }

        // Guessing 4 digit numbers will be more probable.
        // They are set in the first branch.
        if (number < 10000L) { // from 1 to 4
            if (number < 100L) { // 1 or 2
                if (number < 10L) {
                    return 1;
                }
                else {
                    return 2;
                }
            }
            else { // 3 or 4
                if (number < 1000L) {
                    return 3;
                }
                else {
                    return 4;
                }
            }
        }
        else { // from 5 to 20 (albeit longs can't have more than 18 or 19)
            if (number < 1000000000000L) { // from 5 to 12
                if (number < 100000000L) { // from 5 to 8
                    if (number < 1000000L) { // 5 or 6
                        if (number < 100000L) {
                            return 5;
                        }
                        else {
                            return 6;
                        }
                    }
                    else { // 7 u 8
                        if (number < 10000000L) {
                            return 7;
                        }
                        else {
                            return 8;
                        }
                    }
                }
                else { // from 9 to 12
                    if (number < 10000000000L) { // 9 or 10
                        if (number < 1000000000L) {
                            return 9;
                        }
                        else {
                            return 10;
                        }
                    }
                    else { // 11 or 12
                        if (number < 100000000000L) {
                            return 11;
                        }
                        else {
                            return 12;
                        }
                    }
                }
            }
            else { // from 13 to ... (18 or 20)
                if (number < 10000000000000000L) { // from 13 to 16
                    if (number < 100000000000000L) { // 13 or 14
                        if (number < 10000000000000L) {
                            return 13;
                        }
                        else {
                            return 14;
                        }
                    }
                    else { // 15 or 16
                        if (number < 1000000000000000L) {
                            return 15;
                        }
                        else {
                            return 16;
                        }
                    }
                }
                else { // from 17 to ... 20?
                    if (number < 1000000000000000000L) { // 17 or 18
                        if (number < 100000000000000000L) {
                            return 17;
                        }
                        else {
                            return 18;
                        }
                    }
                    else { // 19? Can it be?
                        // 10000000000000000000L isn't a valid long.
                        return 19;
                    }
                }
            }
        }
    }

    /**
     * Removes any characters from the end that are not a number
     *
     * @param text the input text that may, or may not, contain a mix of numbers and letters
     * @return the value as an integer
     */
    public static
    int stripTrailingNonDigits(final String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int numberIndex = 0;
        int length = text.length();

        while (numberIndex < length && Character.isDigit(text.charAt(numberIndex))) {
            numberIndex++;
        }

        String substring = text.substring(0, numberIndex);
        try {
            return Integer.parseInt(substring);
        } catch (Exception ignored) {
        }

        return 0;
    }

    public static
    boolean isEven(int value) {
        return (value & 1) == 0;
    }

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


    public static
    boolean intersectRect(double x1, double y1, double w1, double h1, double x2, double y2, double w2, double h2) {
        return intersectRange(x1, x1 + w1, x2, x2 + w2) && intersectRange(y1, y1 + h1, y2, y2 + h2);
    }

    public static
    boolean intersectRange(double ax1, double ax2, double bx1, double bx2) {
        return Math.max(ax1, bx1) <= Math.min(ax2, bx2);
    }
}
