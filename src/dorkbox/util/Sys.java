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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import dorkbox.os.OS;

@SuppressWarnings({"unused", "WeakerAccess"})
public final
class Sys {
    public static final int KILOBYTE = 1024;
    public static final int MEGABYTE = 1024 * KILOBYTE;
    public static final int GIGABYTE = 1024 * MEGABYTE;
    public static final long TERABYTE = 1024L * GIGABYTE;

    public static final char[] HEX_CHARS = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static
    char[] convertStringToChars(String string) {
        char[] charArray = string.toCharArray();

        eraseString(string);

        return charArray;
    }

    public static
    void eraseString(String string) {
        // You can change the value of the inner char[] using reflection.
        //
        // You must be careful to either change it with an array of the same length,
        // or to also update the count field.
        //
        // If you want to be able to use it as an entry in a set or as a value in map,
        // you will need to recalculate the hash code and set the value of the hashCode field.

        //noinspection TryWithIdenticalCatches
        try {
            Field valueField = String.class.getDeclaredField("value");
            valueField.setAccessible(true);
            char[] chars = (char[]) valueField.get(string);
            Arrays.fill(chars, '*');  // asterisk it out in case of GC not picking up the old char array.

            valueField.set(string, new char[0]); // replace it.

            // set count to 0
            try {
                // newer versions of java don't have this field
                Field countField = String.class.getDeclaredField("count");
                countField.setAccessible(true);
                countField.set(string, 0);
            } catch (Exception ignored) {
            }

            // set hash to 0
            Field hashField = String.class.getDeclaredField("hash");
            hashField.setAccessible(true);
            hashField.set(string, 0);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * FROM: https://www.cqse.eu/en/blog/string-replace-performance/
     * <p/>
     * Replaces all occurrences of keys of the given map in the given string with the associated value in that map.
     * <p/>
     * This method is semantically the same as calling {@link String#replace(CharSequence, CharSequence)} for each of the
     * entries in the map, but may be significantly faster for many replacements performed on a short string, since
     * {@link String#replace(CharSequence, CharSequence)} uses regular expressions internally and results in many String
     * object allocations when applied iteratively.
     * <p/>
     * The order in which replacements are applied depends on the order of the map's entry set.
     */
    public static
    String replaceStringFast(String string, Map<String, String> replacements) {
        StringBuilder sb = new StringBuilder(string);
        for (Entry<String, String> entry : replacements.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            int start = sb.indexOf(key, 0);
            while (start > -1) {
                int end = start + key.length();
                int nextSearchStart = start + value.length();
                sb.replace(start, end, value);
                start = sb.indexOf(key, nextSearchStart);
            }
        }

        return sb.toString();
    }

    /**
     * Quickly finds a char in a string.
     *
     * @return index if it's there, -1 if not there
     */
    public static
    int searchStringFast(String string, char c) {
        int length = string.length();
        for (int i = 0; i < length; i++) {
            if (string.charAt(i) == c) {
                return i;
            }
        }

        return -1;
    }


    public static
    String getSizePretty(final long size) {
        if (size > TERABYTE) {
            return String.format("%2.2fTB", (double) size / TERABYTE);
        }
        if (size > GIGABYTE) {
            return String.format("%2.2fGB", (double) size / GIGABYTE);
        }
        if (size > MEGABYTE) {
            return String.format("%2.2fMB", (double) size / MEGABYTE);
        }
        if (size > KILOBYTE) {
            return String.format("%2.2fKB", (double) size / KILOBYTE);
        }

        return String.valueOf(size) + "B";
    }


    /**
     * Returns a PRETTY string representation of the specified time.
     */
    public static String getTimePretty(long nanoSeconds) {
        final TimeUnit unit;
        final String text;

        if (TimeUnit.DAYS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.DAYS;
            text = "d";
        }
        else if (TimeUnit.HOURS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.HOURS;
            text = "h";
        }
        else if (TimeUnit.MINUTES.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.MINUTES;
            text = "min";
        }
        else  if (TimeUnit.SECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.SECONDS;
            text = "s";
        }
        else if (TimeUnit.MILLISECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.MILLISECONDS;
            text = "ms";
        }
        else if (TimeUnit.MICROSECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.MICROSECONDS;
            text = "\u03bcs"; // μs
        }
        else {
            unit = TimeUnit.NANOSECONDS;
            text = "ns";
        }

        // convert the unit into the largest time unit possible (since that is often what makes sense)
        double value = (double) nanoSeconds / TimeUnit.NANOSECONDS.convert(1, unit);
        return String.format("%.4g" + text, value);
    }

    /**
     * Returns a PRETTY string representation of the specified time.
     */
    public static String getTimePrettyFull(long nanoSeconds) {
        final TimeUnit unit;
        String text;

        if (TimeUnit.DAYS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.DAYS;
            text = "day";
        }
        else if (TimeUnit.HOURS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.HOURS;
            text = "hour";
        }
        else if (TimeUnit.MINUTES.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.MINUTES;
            text = "minute";
        }
        else  if (TimeUnit.SECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.SECONDS;
            text = "second";
        }
        else if (TimeUnit.MILLISECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.MILLISECONDS;
            text = "milli-second";
        }
        else if (TimeUnit.MICROSECONDS.convert(nanoSeconds, TimeUnit.NANOSECONDS) > 0) {
            unit = TimeUnit.MICROSECONDS;
            text = "micro-second";
        }
        else {
            unit = TimeUnit.NANOSECONDS;
            text = "nano-second";
        }

        // convert the unit into the largest time unit possible (since that is often what makes sense)
        double value = (double) nanoSeconds / TimeUnit.NANOSECONDS.convert(1, unit);
        if (value > 1.0D) {
            text += "s";
        }
        return String.format("%.4g " + text, value);
    }

    /**
     * Convert the contents of the input stream to a byte array.
     */
    public static
    byte[] getBytesFromStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);

        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) > 0) {
            baos.write(buffer, 0, read);
        }
        baos.flush();
        inputStream.close();

        return baos.toByteArray();
    }

    public static
    byte[] copyBytes(byte[] src) {
        return copyBytes(src, 0);
    }

    public static
    byte[] copyBytes(byte[] src, int position) {
        int length = src.length - position;

        byte[] b = new byte[length];
        System.arraycopy(src, position, b, 0, length);
        return b;
    }

    public static
    byte[] concatBytes(byte[]... arrayBytes) {
        int length = 0;
        for (byte[] bytes : arrayBytes) {
            length += bytes.length;
        }

        byte[] concatBytes = new byte[length];

        length = 0;
        for (byte[] bytes : arrayBytes) {
            System.arraycopy(bytes, 0, concatBytes, length, bytes.length);
            length += bytes.length;
        }

        return concatBytes;
    }

    /**
     * this saves the char array in UTF-16 format of bytes
     */
    @SuppressWarnings("NumericCastThatLosesPrecision")
    public static
    byte[] charToBytes16(char[] text) {
        // NOTE: this saves the char array in UTF-16 format of bytes.
        byte[] bytes = new byte[text.length * 2];
        for (int i = 0; i < text.length; i++) {
            //noinspection CharUsedInArithmeticContext
            bytes[2 * i] = (byte) (text[i] >> 8);
            bytes[2 * i + 1] = (byte) text[i];
        }

        return bytes;
    }


    public static
    byte[] intsToBytes(int[] ints) {
        int length = ints.length;
        byte[] bytes = new byte[length];

        for (int i = 0; i < length; i++) {
            int intValue = ints[i];
            if (intValue < 0 || intValue > 255) {
                System.err.println("WARNING: int at index " + i + "(" + intValue + ") was not a valid byte value (0-255)");
                return new byte[length];
            }

            //noinspection NumericCastThatLosesPrecision
            bytes[i] = (byte) intValue;
        }

        return bytes;
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    public static
    byte[] charToBytesRaw(char[] chars) {
        int length = chars.length;
        byte[] bytes = new byte[length];

        for (int i = 0; i < length; i++) {
            char charValue = chars[i];
            bytes[i] = (byte) charValue;
        }

        return bytes;
    }

    public static
    int[] bytesToInts(byte[] bytes, int startPosition, int length) {
        int[] ints = new int[length];
        int endPosition = startPosition + length;
        for (int i = startPosition; i < endPosition; i++) {
            ints[i] = bytes[i] & 0xFF;
        }

        return ints;
    }

    public static
    String bytesToHex(byte[] bytes) {
        return bytesToHex(bytes, 0, bytes.length, false);
    }

    public static
    String bytesToHex(byte[] bytes, int startPosition, int length) {
        return bytesToHex(bytes, startPosition, length, false);
    }

    public static
    String bytesToHex(byte[] bytes, int startPosition, int length, boolean padding) {
        int endPosition = startPosition + length;

        if (padding) {
            char[] hexString = new char[3 * length];
            int j = 0;

            for (int i = startPosition; i < endPosition; i++) {
                hexString[j++] = HEX_CHARS[(bytes[i] & 0xF0) >> 4];
                hexString[j++] = HEX_CHARS[bytes[i] & 0x0F];
                hexString[j++] = ' ';
            }

            return new String(hexString);
        }
        else {
            char[] hexString = new char[2 * length];
            int j = 0;

            for (int i = startPosition; i < endPosition; i++) {
                hexString[j++] = HEX_CHARS[(bytes[i] & 0xF0) >> 4];
                hexString[j++] = HEX_CHARS[bytes[i] & 0x0F];
            }

            return new String(hexString);
        }
    }

    /**
     * Converts an ASCII character representing a hexadecimal
     * value into its integer equivalent.
     */
    @SuppressWarnings("DuplicatedCode")
    public static
    int hexByteToInt(byte b) {
        switch (b) {
            case '0':
                return 0;
            case '1':
                return 1;
            case '2':
                return 2;
            case '3':
                return 3;
            case '4':
                return 4;
            case '5':
                return 5;
            case '6':
                return 6;
            case '7':
                return 7;
            case '8':
                return 8;
            case '9':
                return 9;
            case 'A':
            case 'a':
                return 10;
            case 'B':
            case 'b':
                return 11;
            case 'C':
            case 'c':
                return 12;
            case 'D':
            case 'd':
                return 13;
            case 'E':
            case 'e':
                return 14;
            case 'F':
            case 'f':
                return 15;
            default:
                throw new IllegalArgumentException("Error decoding byte");
        }
    }

    /**
     * Converts an ASCII character representing a hexadecimal
     * value into its integer equivalent.
     */
    @SuppressWarnings("DuplicatedCode")
    public static
    int hexCharToInt(char b) {
        switch (b) {
            case '0':
                return 0;
            case '1':
                return 1;
            case '2':
                return 2;
            case '3':
                return 3;
            case '4':
                return 4;
            case '5':
                return 5;
            case '6':
                return 6;
            case '7':
                return 7;
            case '8':
                return 8;
            case '9':
                return 9;
            case 'A':
            case 'a':
                return 10;
            case 'B':
            case 'b':
                return 11;
            case 'C':
            case 'c':
                return 12;
            case 'D':
            case 'd':
                return 13;
            case 'E':
            case 'e':
                return 14;
            case 'F':
            case 'f':
                return 15;
            default:
                throw new IllegalArgumentException("Error decoding byte");
        }
    }

    /**
     * A 4-digit hex result.
     */
    @SuppressWarnings("CharUsedInArithmeticContext")
    public static
    void hex4(final char c, final StringBuilder sb) {
        sb.append(HEX_CHARS[(c & 0xF000) >> 12]);
        sb.append(HEX_CHARS[(c & 0x0F00) >> 8]);
        sb.append(HEX_CHARS[(c & 0x00F0) >> 4]);
        sb.append(HEX_CHARS[c & 0x000F]);
    }

    /**
     * Returns a string representation of the byte array as a series of
     * hexadecimal characters.
     *
     * @param bytes byte array to convert
     * @return a string representation of the byte array as a series of
     * hexadecimal characters
     */
    public static
    String toHexString(byte[] bytes) {
        char[] hexString = new char[2 * bytes.length];
        int j = 0;

        for (int i = 0; i < bytes.length; i++) {
            hexString[j++] = HEX_CHARS[(bytes[i] & 0xF0) >> 4];
            hexString[j++] = HEX_CHARS[bytes[i] & 0x0F];
        }

        return new String(hexString);
    }

    /**
     * from netty 4.1, apache 2.0, https://netty.io
     */
    public static byte hexToByte(CharSequence s, int pos) {
        int hi = hexCharToInt(s.charAt(pos));
        int lo = hexCharToInt(s.charAt(pos + 1));
        if (hi == -1 || lo == -1) {
            throw new IllegalArgumentException(String.format(
                    "invalid hex byte '%s' at index %d of '%s'", s.subSequence(pos, pos + 2), pos, s));
        }
        return (byte) ((hi << 4) + lo);
    }

    /**
     * Decodes a string with <a href="http://en.wikipedia.org/wiki/Hex_dump">hex dump</a>
     *
     * @param hex a {@link CharSequence} which contains the hex dump
     */
    public static byte[] hexToBytes(CharSequence hex) {
        return hexToBytes(hex, 0, hex.length());
    }

    /**
     * Decodes part of a string with <a href="http://en.wikipedia.org/wiki/Hex_dump">hex dump</a>
     *
     * from netty 4.1, apache 2.0, https://netty.io
     *
     * @param hexDump a {@link CharSequence} which contains the hex dump
     * @param fromIndex start of hex dump in {@code hexDump}
     * @param length hex string length
     */
    public static byte[] hexToBytes(CharSequence hexDump, int fromIndex, int length) {
        if (length < 0 || (length & 1) != 0) {
            throw new IllegalArgumentException("length: " + length);
        }

        if (length == 0) {
            return new byte[0];
        }

        byte[] bytes = new byte[length >>> 1];
        for (int i = 0; i < length; i += 2) {
            bytes[i >>> 1] = hexToByte(hexDump, fromIndex + i);
        }
        return bytes;
    }



    /**
     * XOR two byte arrays together, and save result in originalArray
     *
     * @param originalArray this is the base of the XOR operation.
     * @param keyArray      this is XOR'd into the original array, repeats if necessary.
     */
    @SuppressWarnings("NumericCastThatLosesPrecision")
    public static
    void xorArrays(byte[] originalArray, byte[] keyArray) {
        int keyIndex = 0;
        int keyLength = keyArray.length;

        for (int i = 0; i < originalArray.length; i++) {
            //XOR the data and start over if necessary
            originalArray[i] = (byte) (originalArray[i] ^ keyArray[keyIndex++ % keyLength]);
        }
    }



    public static
    byte[] encodeStringArray(List<String> array) {
        int length = 0;
        for (String s : array) {
            byte[] bytes = s.getBytes();
            length += bytes.length;
        }

        if (length == 0) {
            return new byte[0];
        }

        byte[] bytes = new byte[length + array.size()];

        length = 0;
        for (String s : array) {
            byte[] sBytes = s.getBytes();
            System.arraycopy(sBytes, 0, bytes, length, sBytes.length);
            length += sBytes.length;
            bytes[length++] = (byte) 0x01;
        }

        return bytes;
    }

    public static
    ArrayList<String> decodeStringArray(byte[] bytes) {
        int length = bytes.length;
        int position = 0;
        byte token = (byte) 0x01;
        ArrayList<String> list = new ArrayList<String>(0);

        int last = 0;
        while (last + position < length) {
            byte b = bytes[last + position++];
            if (b == token) {
                byte[] xx = new byte[position - 1];
                System.arraycopy(bytes, last, xx, 0, position - 1);
                list.add(new String(xx));
                last += position;
                position = 0;
            }

        }

        return list;
    }

    public static
    String printArrayRaw(final byte[] bytes) {
        return printArrayRaw(bytes, 0);
    }

    public static
    String printArrayRaw(final byte[] bytes, final int lineLength) {
        if (lineLength > 0) {
            int length = bytes.length;
            int comma = length - 1;

            StringBuilder builder = new StringBuilder(length + length / lineLength);
            for (int i = 0; i < length; i++) {
                builder.append(bytes[i]);
                if (i < comma) {
                    builder.append(",");
                }
                if (i > 0 && i % lineLength == 0) {
                    builder.append(OS.LINE_SEPARATOR);
                }
            }

            return builder.toString();

        }
        else {
            int length = bytes.length;
            int comma = length - 1;

            StringBuilder builder = new StringBuilder(length + length);
            for (int i = 0; i < length; i++) {
                builder.append(bytes[i]);
                if (i < comma) {
                    builder.append(",");
                }
            }

            return builder.toString();
        }
    }

    public static
    void printArray(byte[] bytes) {
        printArray(bytes, bytes.length, true);
    }

    public static
    void printArray(byte[] bytes, int length, boolean includeByteCount) {
        printArray(bytes, 0, length, includeByteCount, 40, null);
    }

    public static
    void printArray(byte[] bytes, int inputOffset, int length, boolean includeByteCount) {
        printArray(bytes, inputOffset, length, includeByteCount, 40, null);
    }

    public static
    void printArray(byte[] bytes, int inputOffset, int length, boolean includeByteCount, int lineLength, String header) {
        int comma = length - 1;

        int builderLength = length + comma + 2;
        if (includeByteCount) {
            builderLength += 7 + Integer.toString(length)
                                        .length();
        }
        if (lineLength > 0) {
            builderLength += length / lineLength;
        }
        if (header != null) {
            builderLength += header.length() + 2;
        }

        StringBuilder builder = new StringBuilder(builderLength);

        if (header != null) {
            builder.append(header)
                   .append(OS.LINE_SEPARATOR);
        }

        if (includeByteCount) {
            builder.append("Bytes: ").append(length).append(OS.LINE_SEPARATOR);
        }

        builder.append("{");

        for (int i = inputOffset; i < length; i++) {
            builder.append(bytes[i]);
            if (i < comma) {
                builder.append(",");
            }
            if (i > inputOffset && lineLength > 0 && i % lineLength == 0) {
                builder.append(OS.LINE_SEPARATOR);
            }
        }

        builder.append("}");
        System.err.println(builder.toString());
    }

    /**
     * Raises an exception, but bypasses the compiler checks for the checked exception. This uses type erasure to work
     */
    public static void throwException(Throwable t) {
        Sys.<RuntimeException>throwException0(t);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwException0(Throwable t) throws E {
        throw (E) t;
    }

    private
    Sys() {
    }
}
