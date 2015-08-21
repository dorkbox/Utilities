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

import org.bouncycastle.crypto.digests.SHA256Digest;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@SuppressWarnings("unused")
public final
class Sys {
    public static final boolean isAndroid = getIsAndroid();

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

    private static
    boolean getIsAndroid() {
        try {
            Class.forName("android.os.Process");
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }


    public static
    void eraseString(String string) {
//      You can change the value of the inner char[] using reflection.
//
//      You must be careful to either change it with an array of the same length,
//      or to also update the count field.
//
//      If you want to be able to use it as an entry in a set or as a value in map,
//      you will need to recalculate the hash code and set the value of the hashCode field.

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
     * Replaces all occurrences of keys of the given map in the given string
     * with the associated value in that map.
     * <p/>
     * This method is semantically the same as calling
     * {@link String#replace(CharSequence, CharSequence)} for each of the
     * entries in the map, but may be significantly faster for many replacements
     * performed on a short string, since
     * {@link String#replace(CharSequence, CharSequence)} uses regular
     * expressions internally and results in many String object allocations when
     * applied iteratively.
     * <p/>
     * The order in which replacements are applied depends on the order of the
     * map's entry set.
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
            return String.format("%2.2fTB", (float) size / TERABYTE);
        }
        if (size > GIGABYTE) {
            return String.format("%2.2fGB", (float) size / GIGABYTE);
        }
        if (size > MEGABYTE) {
            return String.format("%2.2fMB", (float) size / MEGABYTE);
        }
        if (size > KILOBYTE) {
            return String.format("%2.2fKB", (float) size / KILOBYTE);
        }

        return String.valueOf(size) + "B";
    }

    /**
     * Convenient close for a stream.
     */
    public static
    void close(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException ioe) {
                System.err.println("Error closing the input stream:" + inputStream);
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Convenient close for a stream.
     */
    public static
    void close(OutputStream outputStream) {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException ioe) {
                System.err.println("Error closing the output stream:" + outputStream);
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Convenient close for a Reader.
     */
    public static
    void close(Reader inputReader) {
        if (inputReader != null) {
            try {
                inputReader.close();
            } catch (IOException ioe) {
                System.err.println("Error closing input reader: " + inputReader);
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Convenient close for a Writer.
     */
    public static
    void close(Writer outputWriter) {
        if (outputWriter != null) {
            try {
                outputWriter.close();
            } catch (IOException ioe) {
                System.err.println("Error closing output writer: " + outputWriter);
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Copy the contents of the input stream to the output stream.
     * <p/>
     * DOES NOT CLOSE THE STEAMS!
     */
    public static
    <T extends OutputStream> T copyStream(InputStream inputStream, T outputStream) throws IOException {
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, read);
        }

        return outputStream;
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
     * gets the SHA256 hash + SALT of the specified username, as UTF-16
     */
    public static
    byte[] getSha256WithSalt(String username, byte[] saltBytes) {
        if (username == null) {
            return null;
        }

        byte[] charToBytes = Sys.charToBytes(username.toCharArray());
        byte[] userNameWithSalt = Sys.concatBytes(charToBytes, saltBytes);


        SHA256Digest sha256 = new SHA256Digest();
        byte[] usernameHashBytes = new byte[sha256.getDigestSize()];
        sha256.update(userNameWithSalt, 0, userNameWithSalt.length);
        sha256.doFinal(usernameHashBytes, 0);

        return usernameHashBytes;
    }

    /**
     * gets the SHA256 hash of the specified string, as UTF-16
     */
    public static
    byte[] getSha256(String string) {
        byte[] charToBytes = Sys.charToBytes(string.toCharArray());

        SHA256Digest sha256 = new SHA256Digest();
        byte[] usernameHashBytes = new byte[sha256.getDigestSize()];
        sha256.update(charToBytes, 0, charToBytes.length);
        sha256.doFinal(usernameHashBytes, 0);

        return usernameHashBytes;
    }

    /**
     * gets the SHA256 hash of the specified byte array
     */
    public static
    byte[] getSha256(byte[] bytes) {

        SHA256Digest sha256 = new SHA256Digest();
        byte[] hashBytes = new byte[sha256.getDigestSize()];
        sha256.update(bytes, 0, bytes.length);
        sha256.doFinal(hashBytes, 0);

        return hashBytes;
    }

    /**
     * this saves the char array in UTF-16 format of bytes
     */
    public static
    byte[] charToBytes(char[] text) {
        // NOTE: this saves the char array in UTF-16 format of bytes.
        byte[] bytes = new byte[text.length * 2];
        for (int i = 0; i < text.length; i++) {
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

            bytes[i] = (byte) intValue;
        }

        return bytes;
    }

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
    int[] bytesToInts(byte[] bytes) {
        int length = bytes.length;
        int[] ints = new int[length];

        for (int i = 0; i < length; i++) {
            ints[i] = bytes[i] & 0xFF;
        }

        return ints;
    }

    public static
    String bytesToHex(byte[] bytes) {
        return bytesToHex(bytes, false);
    }

    public static
    String bytesToHex(byte[] bytes, boolean padding) {
        if (padding) {
            char[] hexString = new char[3 * bytes.length];
            int j = 0;

            for (int i = 0; i < bytes.length; i++) {
                hexString[j++] = HEX_CHARS[(bytes[i] & 0xF0) >> 4];
                hexString[j++] = HEX_CHARS[bytes[i] & 0x0F];
                hexString[j++] = ' ';
            }

            return new String(hexString);
        }
        else {
            char[] hexString = new char[2 * bytes.length];
            int j = 0;

            for (int i = 0; i < bytes.length; i++) {
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
     * A 4-digit hex result.
     */
    public static
    void hex4(char c, StringBuilder sb) {
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
     * XOR two byte arrays together, and save result in originalArray
     *
     * @param originalArray this is the base of the XOR operation.
     * @param keyArray      this is XOR'd into the original array, repeats if necessary.
     */
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
        printArray(bytes, length, includeByteCount, 40);
    }

    public static
    void printArray(byte[] bytes, int length, boolean includeByteCount, int lineLength) {
        if (includeByteCount) {
            System.err.println("Bytes: " + length);
        }

        int comma = length - 1;

        StringBuilder builder;
        if (lineLength > 0) {
            builder = new StringBuilder(length + comma + length / lineLength + 2);
        }
        else {
            builder = new StringBuilder(length + comma + 2);
        }
        builder.append("{");

        for (int i = 0; i < length; i++) {
            builder.append(bytes[i]);
            if (i < comma) {
                builder.append(",");
            }
            if (i > 0 && lineLength > 0 && i % lineLength == 0) {
                builder.append(OS.LINE_SEPARATOR);
            }
        }

        builder.append("}");
        System.err.println(builder.toString());
    }

    private
    Sys() {
    }
}
