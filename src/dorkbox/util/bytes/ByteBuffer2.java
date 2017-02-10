/*
 * Copyright (c) 2008, Nathan Sweet
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *     * Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Modified by dorkbox, llc
 */
package dorkbox.util.bytes;

import java.nio.BufferUnderflowException;
import java.util.Arrays;

/**
 * A self-growing byte array wrapper.
 *
 * Utility methods are provided for efficiently writing primitive types and strings.
 *
 * Encoding of integers: BIG_ENDIAN is used for storing fixed native size integer values LITTLE_ENDIAN is used for a variable
 * length encoding of integer values
 *
 * @author Nathan Sweet <misc@n4te.com>
 */
public class ByteBuffer2 {
    private int capacity;  // exactly how many bytes have been allocated
    private int maxCapacity;  // how large we can grow


    private int position;  // current pointer to the point where data is read/written

    private byte[] bytes; // the backing buffer
    private char[] chars = new char[32]; // small buffer for reading strings

    /**
     * Creates an uninitialized object. {@link #setBuffer(byte[], int)} must be called before the object is used.
     */
    public ByteBuffer2() {
    }

    /**
     * Creates a new object for writing to a byte array.
     *
     * @param bufferSize
     *            The initial and maximum size of the buffer. An exception is thrown if this size is exceeded.
     */
    public ByteBuffer2(int bufferSize) {
        this(bufferSize, bufferSize);
    }

    /**
     * Creates a new object for writing to a byte array.
     *
     * @param bufferSize
     *            The initial size of the buffer.
     * @param maxBufferSize
     *            The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown. Can be -1
     *            for no maximum.
     */
    public ByteBuffer2(int bufferSize, int maxBufferSize) {
        if (maxBufferSize < -1) {
            throw new IllegalArgumentException("maxBufferSize cannot be < -1: " + maxBufferSize);
        }

        this.capacity = bufferSize;
        this.maxCapacity = maxBufferSize == -1 ? Integer.MAX_VALUE : maxBufferSize;
        this.bytes = new byte[bufferSize];
    }

    /**
     * Creates a new object for writing to a byte array.
     *
     * @see #setBuffer(byte[])
     */
    public ByteBuffer2(byte[] buffer) {
        this(buffer, buffer.length);
    }

    /**
     * Creates a new object for writing to a byte array.
     *
     * @see #setBuffer(byte[], int)
     */
    public ByteBuffer2(byte[] buffer, int maxBufferSize) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer cannot be null.");
        }
        setBuffer(buffer, maxBufferSize);
    }

    /**
     * Sets the buffer that will be written to. {@link #setBuffer(byte[], int)} is called with the specified buffer's
     * length as the maxBufferSize.
     */
    public void setBuffer(byte[] buffer) {
        setBuffer(buffer, buffer.length);
    }

    /**
     * Sets the buffer that will be written to. The position and total are reset, discarding any buffered bytes.
     *
     * @param maxBufferSize
     *            The buffer is doubled as needed until it exceeds maxBufferSize and an exception is thrown.
     */
    public void setBuffer(byte[] buffer, int maxBufferSize) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer cannot be null.");
        }

        if (maxBufferSize < -1) {
            throw new IllegalArgumentException("maxBufferSize cannot be < -1: " + maxBufferSize);
        }

        this.bytes = buffer;
        this.maxCapacity = maxBufferSize == -1 ? Integer.MAX_VALUE : maxBufferSize;
        this.capacity = buffer.length;
        this.position = 0;
    }

    /**
     * Returns the buffer. The bytes between zero and {@link #position()} are the data that has been written.
     */
    public byte[] getBuffer() {
        return this.bytes;
    }

    /**
     * Returns a new byte array containing the bytes currently in the buffer between zero and {@link #position()}.
     */
    public byte[] toBytes() {
        int position2 = this.position;
        byte[] newBuffer = new byte[position2];

        if (position2 > 0) {
            System.arraycopy(this.bytes, 0, newBuffer, 0, position2);
        }
        return newBuffer;
    }

    /**
     * Returns the remaining read/write bytes available before the end of the buffer
     */
    public int remaining() {
        return this.capacity - this.position;
    }

    /**
     * Returns the size of the backing byte buffer
     */
    public int capacity() {
        return this.capacity;
    }

    /**
     * Returns the current position in the buffer. This is the number of bytes that have not been flushed.
     */
    public int position() {
        return this.position;
    }

    /**
     * Sets the current position in the buffer.
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Sets the position to zero.
     */
    public void clear() {
        this.position = 0;
    }

    /**
     * Sets the position to zero.
     */
    public void rewind() {
        this.position = 0;
    }

    /**
     * Sets the position to zero, and write 0 to all bytes in the buffer
     */
    public void clearSecure() {
        this.position = 0;
        byte[] buffer = this.bytes;

        for (int i=0;i<this.capacity;i++) {
            buffer[i] = 0;
        }
    }

    /**
     * Discards the specified number of bytes.
     */
    public void skip(int count) {
        int skipCount = Math.min(this.capacity - this.position, count);

        while (true) {
            this.position += skipCount;
            count -= skipCount;
            if (count == 0) {
                break;
            }

            skipCount = Math.min(count, this.capacity);
            require(skipCount);
        }
    }

    /**
     * @return true if the buffer has been resized.
     */
    private boolean require(int required) {
        if (this.capacity - this.position >= required) {
            return false;
        }
        if (required > this.maxCapacity) {
            throw new RuntimeException("Buffer overflow. Max capacity: " + this.maxCapacity + ", required: " + required);
        }

        while (this.capacity - this.position < required) {
            if (this.capacity == this.maxCapacity) {
                throw new RuntimeException("Buffer overflow. Available: " + (this.capacity - this.position)
                        + ", required: " + required);
            }

            // Grow buffer.
            if (this.capacity == 0) {
                this.capacity = 1;
            }
            this.capacity = Math.min((int)(this.capacity * 1.6D), this.maxCapacity);
            if (this.capacity < 0) {
                this.capacity = this.maxCapacity;
            }
            byte[] newBuffer = new byte[this.capacity];
            System.arraycopy(this.bytes, 0, newBuffer, 0, this.position);
            this.bytes = newBuffer;
        }

        return true;
    }

    // byte

    /**
     * Writes a byte.
     */
    public void writeByte(byte value) {
        if (this.position == this.capacity) {
            require(1);
        }
        this.bytes[this.position++] = value;
    }

    /**
     * Writes a byte.
     */
    public void writeByte(int value) {
        if (this.position == this.capacity) {
            require(1);
        }
        this.bytes[this.position++] = (byte) value;
    }

    /**
     * Writes the bytes. Note the byte[] length is not written.
     */
    public void writeBytes(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes cannot be null.");
        }
        writeBytes(bytes, 0, bytes.length);
    }

    /**
     * Writes the bytes. Note the byte[] length is not written.
     */
    public void writeBytes(byte[] bytes, int offset, int count) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes cannot be null.");
        }

        int copyCount = Math.min(this.capacity - this.position, count);
        while (true) {
            System.arraycopy(bytes, offset, this.bytes, this.position, copyCount);
            this.position += copyCount;
            count -= copyCount;
            if (count == 0) {
                return;
            }
            offset += copyCount;
            copyCount = Math.min(this.capacity, count);
            require(copyCount);
        }
    }

    /**
     * Reads a single byte.
     */
    public byte readByte() {
        return this.bytes[this.position++];
    }

    /**
     * Reads a byte as an int from 0 to 255.
     */
    public int readByteUnsigned() {
        return this.bytes[this.position++] & 0xFF;
    }

    /**
     * Reads a single byte, does not advance the position
     */
    public byte readByte(int position) {
        return this.bytes[position];
    }

    /**
     * Reads a byte as an int from 0 to 255, does not advance the position
     */
    public int readByteUnsigned(int position) {
        return this.bytes[position] & 0xFF;
    }

    /**
     * Reads the specified number of bytes into a new byte[].
     */
    public byte[] readBytes(int length) {
        byte[] bytes = new byte[length];
        readBytes(bytes, 0, length);
        return bytes;
    }

    /**
     * Reads bytes.length bytes and writes them to the specified byte[], starting at index 0.
     */
    public void readBytes(byte[] bytes) {
        readBytes(bytes, 0, bytes.length);
    }

    /**
     * Reads count bytes and writes them to the specified byte[], starting at offset in target byte array.
     */
    public void readBytes(byte[] bytes, int offset, int count) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes cannot be null.");
        }

        System.arraycopy(this.bytes, this.position, bytes, offset, count);
        this.position += count;
    }

    // int

    /**
     * Writes a 4 byte int. Uses BIG_ENDIAN byte order.
     */
    public void writeInt(int value) {
        require(4);

        byte[] buffer = this.bytes;
        buffer[this.position++] = (byte) (value >> 24);
        buffer[this.position++] = (byte) (value >> 16);
        buffer[this.position++] = (byte) (value >> 8);
        buffer[this.position++] = (byte) value;
    }

    /**
     * Writes a 1-5 byte int. This stream may consider such a variable length encoding request as a hint. It is not
     * guaranteed that a variable length encoding will be really used. The stream may decide to use native-sized integer
     * representation for efficiency reasons.
     *
     * @param optimizePositive
     *            If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
     *            inefficient (5 bytes).
     */
    public int writeInt(int value, boolean optimizePositive) {
        return writeVarInt(value, optimizePositive);
    }

    /**
     * Writes a 1-5 byte int. It is guaranteed that a varible length encoding will be used.
     *
     * @param optimizePositive
     *            If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
     *            inefficient (5 bytes).
     */
    public int writeVarInt(int value, boolean optimizePositive) {
        if (!optimizePositive) {
            value = value << 1 ^ value >> 31;
        }

        if (value >>> 7 == 0) {
            require(1);
            this.bytes[this.position++] = (byte) value;
            return 1;
        }
        if (value >>> 14 == 0) {
            require(2);
            byte[] buffer = this.bytes;
            buffer[this.position++] = (byte) (value & 0x7F | 0x80);
            buffer[this.position++] = (byte) (value >>> 7);
            return 2;
        }
        if (value >>> 21 == 0) {
            require(3);
            byte[] buffer = this.bytes;
            buffer[this.position++] = (byte) (value & 0x7F | 0x80);
            buffer[this.position++] = (byte) (value >>> 7 | 0x80);
            buffer[this.position++] = (byte) (value >>> 14);
            return 3;
        }
        if (value >>> 28 == 0) {
            require(4);
            byte[] buffer = this.bytes;
            buffer[this.position++] = (byte) (value & 0x7F | 0x80);
            buffer[this.position++] = (byte) (value >>> 7 | 0x80);
            buffer[this.position++] = (byte) (value >>> 14 | 0x80);
            buffer[this.position++] = (byte) (value >>> 21);
            return 4;
        }

        require(5);
        byte[] buffer = this.bytes;
        buffer[this.position++] = (byte) (value & 0x7F | 0x80);
        buffer[this.position++] = (byte) (value >>> 7 | 0x80);
        buffer[this.position++] = (byte) (value >>> 14 | 0x80);
        buffer[this.position++] = (byte) (value >>> 21 | 0x80);
        buffer[this.position++] = (byte) (value >>> 28);
        return 5;
    }

    /**
     * Reads a 4 byte int.
     */
    public int readInt() {
        byte[] buffer = this.bytes;
        int position = this.position;

        int value = (buffer[position] & 0xFF) << 24
                | (buffer[position + 1] & 0xFF) << 16
                | (buffer[position + 2] & 0xFF) << 8
                | buffer[position + 3] & 0xFF;

        this.position = position + 4;
        return value;
    }

    /**
     * Reads a 4 byte int, does not advance the position
     */
    public int readInt(int position) {
        byte[] buffer = this.bytes;
        int value = (buffer[position] & 0xFF) << 24
                | (buffer[position + 1] & 0xFF) << 16
                | (buffer[position + 2] & 0xFF) << 8
                | buffer[position + 3] & 0xFF;

        this.position = position + 4;
        return value;
    }

    /**
     * Reads a 1-5 byte int. This stream may consider such a variable length encoding request as a hint. It is not
     * guaranteed that a variable length encoding will be really used. The stream may decide to use native-sized integer
     * representation for efficiency reasons.
     */
    public int readInt(boolean optimizePositive) {
        return readVarInt(optimizePositive);
    }

    /**
     * Reads a 1-5 byte int. This stream may consider such a variable length encoding request as a hint. It is not
     * guaranteed that a variable length encoding will be really used. The stream may decide to use native-sized integer
     * representation for efficiency reasons.
     * <p>
     * does not advance the position
     */
    public int readInt(int position, boolean optimizePositive) {
        int pos = this.position;
        this.position = position;
        int value = readVarInt(optimizePositive);
        this.position = pos;
        return value;
    }

    /**
     * Reads a 1-5 byte int. It is guaranteed that a variable length encoding will be used.
     */
    private int readVarInt(boolean optimizePositive) {
        byte[] buffer = this.bytes;

        if (this.capacity - this.position < 5) {
            return readInt_slow(optimizePositive);
        }

        int b = buffer[this.position++];
        int result = b & 0x7F;
        if ((b & 0x80) != 0) {
            b = buffer[this.position++];
            result |= (b & 0x7F) << 7;
            if ((b & 0x80) != 0) {
                b = buffer[this.position++];
                result |= (b & 0x7F) << 14;
                if ((b & 0x80) != 0) {
                    b = buffer[this.position++];
                    result |= (b & 0x7F) << 21;
                    if ((b & 0x80) != 0) {
                        b = buffer[this.position++];
                        result |= (b & 0x7F) << 28;
                    }
                }
            }
        }
        return optimizePositive ? result : result >>> 1 ^ -(result & 1);
    }

    private int readInt_slow(boolean optimizePositive) {
        byte[] buffer = this.bytes;

        // The buffer is guaranteed to have at least 1 byte.
        int b = buffer[this.position++];
        int result = b & 0x7F;
        if ((b & 0x80) != 0) {
            b = buffer[this.position++];
            result |= (b & 0x7F) << 7;
            if ((b & 0x80) != 0) {
                b = buffer[this.position++];
                result |= (b & 0x7F) << 14;
                if ((b & 0x80) != 0) {
                    b = buffer[this.position++];
                    result |= (b & 0x7F) << 21;
                    if ((b & 0x80) != 0) {
                        b = buffer[this.position++];
                        result |= (b & 0x7F) << 28;
                    }
                }
            }
        }
        return optimizePositive ? result : result >>> 1 ^ -(result & 1);
    }

    /**
     * Returns true if enough bytes are available to read an int with {@link #readInt(boolean)}.
     */
    public boolean canReadInt() {
        if (this.capacity - this.position >= 5) {
            return true;
        }

        if (this.position + 1 > this.capacity) {
            return false;
        }

        byte[] buffer = this.bytes;

        int p = this.position;
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if enough bytes are available to read an int with {@link #readInt(boolean)}.
     */
    public boolean canReadInt(int position) {
        if (this.capacity - position >= 5) {
            return true;
        }

        if (position + 1 > this.capacity) {
            return false;
        }

        byte[] buffer = this.bytes;

        int p = position;
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        return true;
    }

   // string

    /**
     * Writes the length and string, or null. Short strings are checked and if ASCII they are written more efficiently,
     * else they are written as UTF8. If a string is known to be ASCII, {@link ByteBuffer2#writeAscii(String)} may be used. The
     * string can be read using {@link ByteBuffer2#readString()} or {@link ByteBuffer2#readStringBuilder()}.
     *
     * @param value
     *            May be null.
     */
    @SuppressWarnings("deprecation")
    public void writeString(String value)   {
        if (value == null) {
            writeByte(0x80); // 0 means null, bit 8 means UTF8.
            return;
        }

        int charCount = value.length();
        if (charCount == 0) {
            writeByte(1 | 0x80); // 1 means empty string, bit 8 means UTF8.
            return;
        }

        // Detect ASCII.
        boolean ascii = false;
        if (charCount > 1 && charCount < 64) {
            ascii = true;
            for (int i = 0; i < charCount; i++) {
                int c = value.charAt(i);
                if (c > 127) {
                    ascii = false;
                    break;
                }
            }
        }

        if (ascii) {
            if (this.capacity - this.position < charCount) {
                writeAscii_slow(value, charCount);
            } else {
                value.getBytes(0, charCount, this.bytes, this.position);
                this.position += charCount;
            }
            this.bytes[this.position - 1] |= 0x80;
        } else {
            writeUtf8Length(charCount + 1);
            int charIndex = 0;
            if (this.capacity - this.position >= charCount) {
                // Try to write 8 bit chars.
                byte[] buffer = this.bytes;
                int position = this.position;
                for (; charIndex < charCount; charIndex++) {
                    int c = value.charAt(charIndex);
                    if (c > 127) {
                        break;
                    }
                    buffer[position++] = (byte) c;
                }
                this.position = position;
            }

            if (charIndex < charCount) {
                writeString_slow(value, charCount, charIndex);
            }
        }
    }

    /**
     * Writes the length and CharSequence as UTF8, or null. The string can be read using {@link ByteBuffer2#readString()} or
     * {@link ByteBuffer2#readStringBuilder()}.
     *
     * @param value
     *            May be null.
     */
    public void writeString(CharSequence value) {
        if (value == null) {
            writeByte(0x80); // 0 means null, bit 8 means UTF8.
            return;
        }

        int charCount = value.length();
        if (charCount == 0) {
            writeByte(1 | 0x80); // 1 means empty string, bit 8 means UTF8.
            return;
        }

        writeUtf8Length(charCount + 1);
        int charIndex = 0;
        if (this.capacity - this.position >= charCount) {
            // Try to write 8 bit chars.
            byte[] buffer = this.bytes;
            int position = this.position;

            for (; charIndex < charCount; charIndex++) {
                int c = value.charAt(charIndex);
                if (c > 127) {
                    break;
                }
                buffer[position++] = (byte) c;
            }
            this.position = position;
        }

        if (charIndex < charCount) {
            writeString_slow(value, charCount, charIndex);
        }
    }

    /**
     * Writes a string that is known to contain only ASCII characters. Non-ASCII strings passed to this method will be
     * corrupted. Each byte is a 7 bit character with the remaining byte denoting if another character is available.
     * This is slightly more efficient than {@link ByteBuffer2#writeString(String)}. The string can be read using
     * {@link ByteBuffer2#readString()} or {@link ByteBuffer2#readStringBuilder()}.
     *
     * @param value
     *            May be null.
     */
    @SuppressWarnings("deprecation")
    public void writeAscii(String value) {
        if (value == null) {
            writeByte(0x80); // 0 means null, bit 8 means UTF8.
            return;
        }

        int charCount = value.length();
        switch (charCount) {
            case 0 :
                writeByte(1 | 0x80); // 1 is string length + 1, bit 8 means UTF8.
                return;
            case 1 :
                writeByte(2 | 0x80); // 2 is string length + 1, bit 8 means UTF8.
                writeByte(value.charAt(0));
                return;
        }

        if (this.capacity - this.position < charCount) {
            writeAscii_slow(value, charCount);
        } else {
            value.getBytes(0, charCount, this.bytes, this.position);
            this.position += charCount;
        }

        this.bytes[this.position - 1] |= 0x80; // Bit 8 means end of ASCII.
    }

    /**
     * Writes the length of a string, which is a variable length encoded int except the first byte uses bit 8 to denote
     * UTF8 and bit 7 to denote if another byte is present.
     */
    private void writeUtf8Length(int value) {

        if (value >>> 6 == 0) {
            require(1);
            this.bytes[this.position++] = (byte) (value | 0x80); // Set bit 8.
        } else if (value >>> 13 == 0) {
            require(2);
            byte[] buffer = this.bytes;
            buffer[this.position++] = (byte) (value | 0x40 | 0x80); // Set bit 7 and 8.
            buffer[this.position++] = (byte) (value >>> 6);
        } else if (value >>> 20 == 0) {
            require(3);
            byte[] buffer = this.bytes;
            buffer[this.position++] = (byte) (value | 0x40 | 0x80); // Set bit 7 and 8.
            buffer[this.position++] = (byte) (value >>> 6 | 0x80); // Set bit 8.
            buffer[this.position++] = (byte) (value >>> 13);
        } else if (value >>> 27 == 0) {
            require(4);
            byte[] buffer = this.bytes;
            buffer[this.position++] = (byte) (value | 0x40 | 0x80); // Set bit 7 and 8.
            buffer[this.position++] = (byte) (value >>> 6 | 0x80); // Set bit 8.
            buffer[this.position++] = (byte) (value >>> 13 | 0x80); // Set bit 8.
            buffer[this.position++] = (byte) (value >>> 20);
        } else {
            require(5);
            byte[] buffer = this.bytes;
            buffer[this.position++] = (byte) (value | 0x40 | 0x80); // Set bit 7 and 8.
            buffer[this.position++] = (byte) (value >>> 6 | 0x80); // Set bit 8.
            buffer[this.position++] = (byte) (value >>> 13 | 0x80); // Set bit 8.
            buffer[this.position++] = (byte) (value >>> 20 | 0x80); // Set bit 8.
            buffer[this.position++] = (byte) (value >>> 27);
        }
    }

    private void writeString_slow(CharSequence value, int charCount, int charIndex) {
        byte[] buffer = this.bytes;
        for (; charIndex < charCount; charIndex++) {
            if (this.position == this.capacity) {
                require(Math.min(this.capacity, charCount - charIndex));
                buffer = this.bytes;
            }

            int c = value.charAt(charIndex);
            if (c <= 0x007F) {
                this.bytes[this.position++] = (byte) c;
            } else if (c > 0x07FF) {
                buffer[this.position++] = (byte) (0xE0 | c >> 12 & 0x0F);
                require(2);

                buffer = this.bytes;
                buffer[this.position++] = (byte) (0x80 | c >> 6 & 0x3F);
                buffer[this.position++] = (byte) (0x80 | c & 0x3F);
            } else {
                buffer[this.position++] = (byte) (0xC0 | c >> 6 & 0x1F);
                require(1);
                buffer = this.bytes;
                buffer[this.position++] = (byte) (0x80 | c & 0x3F);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void writeAscii_slow(String value, int charCount) {
        byte[] buffer = this.bytes;
        int charIndex = 0;
        int charsToWrite = Math.min(charCount, this.capacity - this.position);

        while (charIndex < charCount) {
            value.getBytes(charIndex, charIndex + charsToWrite, buffer, this.position);
            charIndex += charsToWrite;
            this.position += charsToWrite;
            charsToWrite = Math.min(charCount - charIndex, this.capacity);
            if (require(charsToWrite)) {
                buffer = this.bytes;
            }
        }
    }

    /**
     * Reads the length and string of UTF8 characters, or null. This can read strings written by
     * {@link ByteBuffer2#writeString(String)} , {@link ByteBuffer2#writeString(CharSequence)}, and
     * {@link ByteBuffer2#writeAscii(String)}.
     *
     * @return May be null.
     */
    public String readString() {
        int available = this.capacity - this.position;

        int b = this.bytes[this.position++];
        if ((b & 0x80) == 0) {
            return readAscii(); // ASCII.
        }

        // Null, empty, or UTF8.
        int charCount = available >= 5 ? readUtf8Length(b) : readUtf8Length_slow(b);
        switch (charCount) {
            case 0 :
                return null;
            case 1 :
                return "";
        }
        charCount--;

        if (this.chars.length < charCount) {
            this.chars = new char[charCount];
        }

        if (available < charCount) {
            throw new BufferUnderflowException();
        }

        readUtf8(charCount);
        return new String(this.chars, 0, charCount);
    }

    private int readUtf8Length(int b) {
        int result = b & 0x3F; // Mask all but first 6 bits.
        if ((b & 0x40) != 0) { // Bit 7 means another byte, bit 8 means UTF8.
            byte[] buffer = this.bytes;

            b = buffer[this.position++];
            result |= (b & 0x7F) << 6;
            if ((b & 0x80) != 0) {
                b = buffer[this.position++];
                result |= (b & 0x7F) << 13;
                if ((b & 0x80) != 0) {
                    b = buffer[this.position++];
                    result |= (b & 0x7F) << 20;
                    if ((b & 0x80) != 0) {
                        b = buffer[this.position++];
                        result |= (b & 0x7F) << 27;
                    }
                }
            }
        }
        return result;
    }

    private int readUtf8Length_slow(int b) {
        int result = b & 0x3F; // Mask all but first 6 bits.
        if ((b & 0x40) != 0) { // Bit 7 means another byte, bit 8 means UTF8.
            byte[] buffer = this.bytes;

            b = buffer[this.position++];
            result |= (b & 0x7F) << 6;
            if ((b & 0x80) != 0) {
                b = buffer[this.position++];
                result |= (b & 0x7F) << 13;
                if ((b & 0x80) != 0) {
                    b = buffer[this.position++];
                    result |= (b & 0x7F) << 20;
                    if ((b & 0x80) != 0) {
                        b = buffer[this.position++];
                        result |= (b & 0x7F) << 27;
                    }
                }
            }
        }
        return result;
    }

    private void readUtf8(int charCount) {
        byte[] buffer = this.bytes;
        int position = this.position;
        char[] chars = this.chars;

        // Try to read 7 bit ASCII chars.
        int charIndex = 0;
        int spaceAvailable = this.capacity - this.position;
        int count = Math.min(spaceAvailable, charCount);

        int b;
        while (charIndex < count) {
            b = buffer[position++];
            if (b < 0) {
                position--;
                break;
            }
            chars[charIndex++] = (char) b;
        }
        this.position = position;

        // If buffer didn't hold all chars or any were not ASCII, use slow path for remainder.
        if (charIndex < charCount) {
            readUtf8_slow(charCount, charIndex);
        }
    }

    private void readUtf8_slow(int charCount, int charIndex) {
        char[] chars = this.chars;
        byte[] buffer = this.bytes;

        while (charIndex < charCount) {
            int b = buffer[this.position++] & 0xFF;
            switch (b >> 4) {
                case 0 :
                case 1 :
                case 2 :
                case 3 :
                case 4 :
                case 5 :
                case 6 :
                case 7 :
                    chars[charIndex] = (char) b;
                    break;
                case 12 :
                case 13 :
                    chars[charIndex] = (char) ((b & 0x1F) << 6 | buffer[this.position++] & 0x3F);
                    break;
                case 14 :
                    chars[charIndex] = (char) ((b & 0x0F) << 12 | (buffer[this.position++] & 0x3F) << 6 | buffer[this.position++] & 0x3F);
                    break;
            }
            charIndex++;
        }
    }

    private String readAscii() {
        byte[] buffer = this.bytes;
        int end = this.position;
        int start = end - 1;
        int limit = this.capacity;
        int b;

        do {
            if (end == limit) {
                return readAscii_slow();
            }
            b = buffer[end++];
        } while ((b & 0x80) == 0);

        buffer[end - 1] &= 0x7F; // Mask end of ascii bit.

        @SuppressWarnings("deprecation")
        String value = new String(buffer, 0, start, end - start);
        buffer[end - 1] |= 0x80;

        this.position = end;
        return value;
    }

    private String readAscii_slow() {
        this.position--; // Re-read the first byte.

        // Copy chars currently in buffer.
        int charCount = this.capacity - this.position;
        if (charCount > this.chars.length) {
            this.chars = new char[charCount * 2];
        }

        char[] chars = this.chars;
        byte[] buffer = this.bytes;
        for (int i = this.position, ii = 0, n = this.capacity; i < n; i++, ii++) {
            chars[ii] = (char) buffer[i];
        }
        this.position = this.capacity;

        // Copy additional chars one by one.
        while (true) {
            int b = buffer[this.position++];
            if (charCount == chars.length) {
                char[] newChars = new char[charCount * 2];
                System.arraycopy(chars, 0, newChars, 0, charCount);
                chars = newChars;
                this.chars = newChars;
            }
            if ((b & 0x80) == 0x80) {
                chars[charCount++] = (char) (b & 0x7F);
                break;
            }
            chars[charCount++] = (char) b;
        }

        return new String(chars, 0, charCount);
    }

    /**
     * Reads the length and string of UTF8 characters, or null. This can read strings written by
     * {@link ByteBuffer2#writeString(String)} , {@link ByteBuffer2#writeString(CharSequence)}, and
     * {@link ByteBuffer2#writeAscii(String)}.
     *
     * @return May be null.
     */
    public StringBuilder readStringBuilder() {
        int available = this.capacity - this.position;
        int b = this.bytes[this.position++];
        if ((b & 0x80) == 0) {
            return new StringBuilder(readAscii()); // ASCII.
        }

        // Null, empty, or UTF8.
        int charCount = available >= 5 ? readUtf8Length(b) : readUtf8Length_slow(b);
        switch (charCount) {
            case 0 :
                return null;
            case 1 :
                return new StringBuilder("");
        }
        charCount--;
        if (this.chars.length < charCount) {
            this.chars = new char[charCount];
        }

        readUtf8(charCount);
        StringBuilder builder = new StringBuilder(charCount);
        builder.append(this.chars, 0, charCount);

        return builder;
    }


   // float

    /**
     * Writes a 4 byte float.
     */
    public void writeFloat(float value) {
        writeInt(Float.floatToIntBits(value));
    }

    /**
     * Writes a 1-5 byte float with reduced precision.
     *
     * @param optimizePositive
     *            If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
     *            inefficient (5 bytes).
     */
    public int writeFloat(float value, float precision, boolean optimizePositive) {
        return writeInt((int) (value * precision), optimizePositive);
    }

    /**
     * Reads a 4 byte float.
     */
    public float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    /**
     * Reads a 1-5 byte float with reduced precision.
     */
    public float readFloat(float precision, boolean optimizePositive) {
        return readInt(optimizePositive) / precision;
    }

    /**
     * Reads a 4 byte float, does not advance the position
     */
    public float readFloat(int position) {
        return Float.intBitsToFloat(readInt(position));
    }

    /**
     * Reads a 1-5 byte float with reduced precision, does not advance the position
     */
    public float readFloat(int position, float precision, boolean optimizePositive) {
        return readInt(position, optimizePositive) / precision;
    }

    // short

    /**
     * Writes a 2 byte short. Uses BIG_ENDIAN byte order.
     */
    public void writeShort(int value) {
        require(2);
        byte[] buffer = this.bytes;
        buffer[this.position++] = (byte) (value >>> 8);
        buffer[this.position++] = (byte) value;
    }

    /**
     * Reads a 2 byte short.
     */
    public short readShort() {
        byte[] buffer = this.bytes;
        return (short) ((buffer[this.position++] & 0xFF) << 8 | buffer[this.position++] & 0xFF);
    }

    /**
     * Reads a 2 byte short as an int from 0 to 65535.
     */
    public int readShortUnsigned() {
        byte[] buffer = this.bytes;
        return (buffer[this.position++] & 0xFF) << 8 | buffer[this.position++] & 0xFF;
    }

    /**
     * Reads a 2 byte short, does not advance the position
     */
    public short readShort(int position) {
        byte[] buffer = this.bytes;
        return (short) ((buffer[position++] & 0xFF) << 8 | buffer[position] & 0xFF);
    }

    /**
     * Reads a 2 byte short as an int from 0 to 65535, does not advance the position
     */
    public int readShortUnsigned(int position) {
        byte[] buffer = this.bytes;
        return (buffer[position++] & 0xFF) << 8 | buffer[position] & 0xFF;
    }

   // long

    /**
     * Writes an 8 byte long. Uses BIG_ENDIAN byte order.
     */
    public void writeLong(long value) {
        require(8);

        byte[] buffer = this.bytes;
        buffer[this.position++] = (byte) (value >>> 56);
        buffer[this.position++] = (byte) (value >>> 48);
        buffer[this.position++] = (byte) (value >>> 40);
        buffer[this.position++] = (byte) (value >>> 32);
        buffer[this.position++] = (byte) (value >>> 24);
        buffer[this.position++] = (byte) (value >>> 16);
        buffer[this.position++] = (byte) (value >>> 8);
        buffer[this.position++] = (byte) value;
    }

    /**
     * Writes a 1-9 byte long. This stream may consider such a variable length encoding request as a hint. It is not
     * guaranteed that a variable length encoding will be really used. The stream may decide to use native-sized integer
     * representation for efficiency reasons.
     *
     * @param optimizePositive
     *            If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
     *            inefficient (9 bytes).
     */
    public int writeLong(long value, boolean optimizePositive) {
        return writeVarLong(value, optimizePositive);
    }

    /**
     * Writes a 1-9 byte long. It is guaranteed that a varible length encoding will be used.
     *
     * @param optimizePositive
     *            If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
     *            inefficient (9 bytes).
     */
    public int writeVarLong(long value, boolean optimizePositive) {
        if (!optimizePositive) {
            value = value << 1 ^ value >> 63;
        }
        if (value >>> 7 == 0) {
            require(1);
            this.bytes[this.position++] = (byte) value;
            return 1;
        }
        if (value >>> 14 == 0) {
            require(2);
            byte[] buffer = this.bytes;
            buffer[this.position++] = (byte) (value & 0x7F | 0x80);
            buffer[this.position++] = (byte) (value >>> 7);
            return 2;
        }
        if (value >>> 21 == 0) {
            require(3);
            byte[] buffer = this.bytes;
            buffer[this.position++] = (byte) (value & 0x7F | 0x80);
            buffer[this.position++] = (byte) (value >>> 7 | 0x80);
            buffer[this.position++] = (byte) (value >>> 14);
            return 3;
        }
        if (value >>> 28 == 0) {
            require(4);
            byte[] buffer = this.bytes;
            buffer[this.position++] = (byte) (value & 0x7F | 0x80);
            buffer[this.position++] = (byte) (value >>> 7 | 0x80);
            buffer[this.position++] = (byte) (value >>> 14 | 0x80);
            buffer[this.position++] = (byte) (value >>> 21);
            return 4;
        }
        if (value >>> 35 == 0) {
            require(5);
            byte[] buffer = this.bytes;
            buffer[this.position++] = (byte) (value & 0x7F | 0x80);
            buffer[this.position++] = (byte) (value >>> 7 | 0x80);
            buffer[this.position++] = (byte) (value >>> 14 | 0x80);
            buffer[this.position++] = (byte) (value >>> 21 | 0x80);
            buffer[this.position++] = (byte) (value >>> 28);
            return 5;
        }
        if (value >>> 42 == 0) {
            require(6);
            byte[] buffer = this.bytes;
            buffer[this.position++] = (byte) (value & 0x7F | 0x80);
            buffer[this.position++] = (byte) (value >>> 7 | 0x80);
            buffer[this.position++] = (byte) (value >>> 14 | 0x80);
            buffer[this.position++] = (byte) (value >>> 21 | 0x80);
            buffer[this.position++] = (byte) (value >>> 28 | 0x80);
            buffer[this.position++] = (byte) (value >>> 35);
            return 6;
        }
        if (value >>> 49 == 0) {
            require(7);
            byte[] buffer = this.bytes;
            buffer[this.position++] = (byte) (value & 0x7F | 0x80);
            buffer[this.position++] = (byte) (value >>> 7 | 0x80);
            buffer[this.position++] = (byte) (value >>> 14 | 0x80);
            buffer[this.position++] = (byte) (value >>> 21 | 0x80);
            buffer[this.position++] = (byte) (value >>> 28 | 0x80);
            buffer[this.position++] = (byte) (value >>> 35 | 0x80);
            buffer[this.position++] = (byte) (value >>> 42);
            return 7;
        }
        if (value >>> 56 == 0) {
            require(8);
            byte[] buffer = this.bytes;
            buffer[this.position++] = (byte) (value & 0x7F | 0x80);
            buffer[this.position++] = (byte) (value >>> 7 | 0x80);
            buffer[this.position++] = (byte) (value >>> 14 | 0x80);
            buffer[this.position++] = (byte) (value >>> 21 | 0x80);
            buffer[this.position++] = (byte) (value >>> 28 | 0x80);
            buffer[this.position++] = (byte) (value >>> 35 | 0x80);
            buffer[this.position++] = (byte) (value >>> 42 | 0x80);
            buffer[this.position++] = (byte) (value >>> 49);
            return 8;
        }

        require(9);
        byte[] buffer = this.bytes;
        buffer[this.position++] = (byte) (value & 0x7F | 0x80);
        buffer[this.position++] = (byte) (value >>> 7 | 0x80);
        buffer[this.position++] = (byte) (value >>> 14 | 0x80);
        buffer[this.position++] = (byte) (value >>> 21 | 0x80);
        buffer[this.position++] = (byte) (value >>> 28 | 0x80);
        buffer[this.position++] = (byte) (value >>> 35 | 0x80);
        buffer[this.position++] = (byte) (value >>> 42 | 0x80);
        buffer[this.position++] = (byte) (value >>> 49 | 0x80);
        buffer[this.position++] = (byte) (value >>> 56);
        return 9;
    }

    /**
     * Returns true if enough bytes are available to read a long with {@link #readLong(boolean)}.
     */
    public boolean canReadLong() {
        if (this.capacity - this.position >= 9) {
            return true;
        }

        if (this.position + 1 > this.capacity) {
            return false;
        }

        byte[] buffer = this.bytes;
        int p = this.position;
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if enough bytes are available to read a long with {@link #readLong(boolean)}.
     */
    public boolean canReadLong(int position) {
        if (this.capacity - position >= 9) {
            return true;
        }

        if (position + 1 > this.capacity) {
            return false;
        }

        byte[] buffer = this.bytes;
        int p = position;
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        if ((buffer[p++] & 0x80) == 0) {
            return true;
        }
        if (p == this.capacity) {
            return false;
        }
        return true;
    }

    /**
     * Reads an 8 byte long.
     */
    public long readLong() {
        byte[] buffer = this.bytes;

        return (long) buffer[this.position++] << 56
                | (long) (buffer[this.position++] & 0xFF) << 48
                | (long) (buffer[this.position++] & 0xFF) << 40
                | (long) (buffer[this.position++] & 0xFF) << 32
                | (long) (buffer[this.position++] & 0xFF) << 24
                | (buffer[this.position++] & 0xFF) << 16
                | (buffer[this.position++] & 0xFF) << 8
                | buffer[this.position++] & 0xFF;

    }

    /**
     * Reads a 1-9 byte long. This stream may consider such a variable length encoding request as a hint. It is not
     * guaranteed that a variable length encoding will be really used. The stream may decide to use native-sized integer
     * representation for efficiency reasons.
     */
    public long readLong(boolean optimizePositive) {
        return readVarLong(optimizePositive);
    }

    /**
     * Reads an 8 byte long, does not advance the position
     */
    public long readLong(int position) {
        byte[] buffer = this.bytes;

        return (long) buffer[position++] << 56
                | (long) (buffer[position++] & 0xFF) << 48
                | (long) (buffer[position++] & 0xFF) << 40
                | (long) (buffer[position++] & 0xFF) << 32
                | (long) (buffer[position++] & 0xFF) << 24
                | (buffer[position++] & 0xFF) << 16
                | (buffer[position++] & 0xFF) << 8
                | buffer[position++] & 0xFF;

    }

    /**
     * Reads a 1-9 byte long. This stream may consider such a variable length encoding request as a hint. It is not
     * guaranteed that a variable length encoding will be really used. The stream may decide to use native-sized integer
     * representation for efficiency reasons.
     * <p>
     * does not advance the position
     */
    public long readLong(int position, boolean optimizePositive) {
        int pos = this.position;
        this.position = position;
        long value = readVarLong(optimizePositive);
        this.position = pos;
        return value;
    }

    /**
     * Reads a 1-9 byte long. It is guaranteed that a varible length encoding will be used.
     */
    private long readVarLong(boolean optimizePositive) {
        if (this.capacity - this.position < 9) {
            return readLong_slow(optimizePositive);
        }

        int b = this.bytes[this.position++];
        long result = b & 0x7F;
        if ((b & 0x80) != 0) {
            byte[] buffer = this.bytes;
            b = buffer[this.position++];
            result |= (b & 0x7F) << 7;
            if ((b & 0x80) != 0) {
                b = buffer[this.position++];
                result |= (b & 0x7F) << 14;
                if ((b & 0x80) != 0) {
                    b = buffer[this.position++];
                    result |= (b & 0x7F) << 21;
                    if ((b & 0x80) != 0) {
                        b = buffer[this.position++];
                        result |= (long) (b & 0x7F) << 28;
                        if ((b & 0x80) != 0) {
                            b = buffer[this.position++];
                            result |= (long) (b & 0x7F) << 35;
                            if ((b & 0x80) != 0) {
                                b = buffer[this.position++];
                                result |= (long) (b & 0x7F) << 42;
                                if ((b & 0x80) != 0) {
                                    b = buffer[this.position++];
                                    result |= (long) (b & 0x7F) << 49;
                                    if ((b & 0x80) != 0) {
                                        b = buffer[this.position++];
                                        result |= (long) b << 56;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!optimizePositive) {
            result = result >>> 1 ^ -(result & 1);
        }

        return result;
    }

    private long readLong_slow(boolean optimizePositive) {
        // The buffer is guaranteed to have at least 1 byte.
        byte[] buffer = this.bytes;
        int b = buffer[this.position++];

        long result = b & 0x7F;
        if ((b & 0x80) != 0) {
            b = buffer[this.position++];
            result |= (b & 0x7F) << 7;
            if ((b & 0x80) != 0) {
                b = buffer[this.position++];
                result |= (b & 0x7F) << 14;
                if ((b & 0x80) != 0) {
                    b = buffer[this.position++];
                    result |= (b & 0x7F) << 21;
                    if ((b & 0x80) != 0) {
                        b = buffer[this.position++];
                        result |= (long) (b & 0x7F) << 28;
                        if ((b & 0x80) != 0) {
                            b = buffer[this.position++];
                            result |= (long) (b & 0x7F) << 35;
                            if ((b & 0x80) != 0) {
                                b = buffer[this.position++];
                                result |= (long) (b & 0x7F) << 42;
                                if ((b & 0x80) != 0) {
                                    b = buffer[this.position++];
                                    result |= (long) (b & 0x7F) << 49;
                                    if ((b & 0x80) != 0) {
                                        b = buffer[this.position++];
                                        result |= (long) b << 56;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!optimizePositive) {
            result = result >>> 1 ^ -(result & 1);
        }
        return result;
    }

   // boolean

    /**
     * Writes a 1 byte boolean.
     */
    public void writeBoolean(boolean value) {
        require(1);
        this.bytes[this.position++] = (byte) (value ? 1 : 0);
    }

    /**
     * Reads a 1 byte boolean.
     */
    public boolean readBoolean ()   {
       return this.bytes[this.position++] == 1;
    }

    /**
     * Reads a 1 byte boolean, does not advance the position
     */
    public boolean readBoolean (int position)   {
       return this.bytes[position] == 1;
    }

    // char

    /**
     * Writes a 2 byte char. Uses BIG_ENDIAN byte order.
     */
    public void writeChar(char value) {
        require(2);
        byte[] buffer = this.bytes;
        buffer[this.position++] = (byte) (value >>> 8);
        buffer[this.position++] = (byte) value;
    }

    /**
     * Reads a 2 byte char.
     */
    public char readChar() {
        byte[] buffer = this.bytes;
        return (char) ((buffer[this.position++] & 0xFF) << 8 | buffer[this.position++] & 0xFF);
    }

    /**
     * Reads a 2 byte char, does not advance the position
     */
    public char readChar(int position) {
        byte[] buffer = this.bytes;
        return (char) ((buffer[position++] & 0xFF) << 8 | buffer[position++] & 0xFF);
    }

    // double

    /**
     * Writes an 8 byte double.
     */
    public void writeDouble(double value) {
        writeLong(Double.doubleToLongBits(value));
    }

    /**
     * Writes a 1-9 byte double with reduced precision
     *
     * @param optimizePositive
     *            If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
     *            inefficient (9 bytes).
     */
    public int writeDouble(double value, double precision, boolean optimizePositive) {
        return writeLong((long) (value * precision), optimizePositive);
    }


    /**
     * Reads an 8 bytes double.
     */
    public double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Reads a 1-9 byte double with reduced precision.
     */
    public double readDouble(double precision, boolean optimizePositive) {
        return readLong(optimizePositive) / precision;
    }

    /**
     * Reads an 8 bytes double, does not advance the position
     */
    public double readDouble(int position) {
        return Double.longBitsToDouble(readLong(position));
    }

    /**
     * Reads a 1-9 byte double with reduced precision, does not advance the position
     */
    public double readDouble(int position, double precision, boolean optimizePositive) {
        return readLong(position, optimizePositive) / precision;
    }





    /**
     * Returns the number of bytes that would be written with {@link #writeInt(int, boolean)}.
     */
    public static int intLength(int value, boolean optimizePositive) {
        if (!optimizePositive) {
            value = value << 1 ^ value >> 31;
        }
        if (value >>> 7 == 0) {
            return 1;
        }
        if (value >>> 14 == 0) {
            return 2;
        }
        if (value >>> 21 == 0) {
            return 3;
        }
        if (value >>> 28 == 0) {
            return 4;
        }
        return 5;
    }

    /**
     * Returns the number of bytes that would be written with {@link #writeLong(long, boolean)}.
     */
   public static int longLength(long value, boolean optimizePositive) {
        if (!optimizePositive) {
            value = value << 1 ^ value >> 63;
        }
        if (value >>> 7 == 0) {
            return 1;
        }
        if (value >>> 14 == 0) {
            return 2;
        }
        if (value >>> 21 == 0) {
            return 3;
        }
        if (value >>> 28 == 0) {
            return 4;
        }
        if (value >>> 35 == 0) {
            return 5;
        }
        if (value >>> 42 == 0) {
            return 6;
        }
        if (value >>> 49 == 0) {
            return 7;
        }
        if (value >>> 56 == 0) {
            return 8;
        }
        return 9;
    }

   // Methods implementing bulk operations on arrays of primitive types

    /**
     * Bulk output of an int array.
     */
    public void writeInts(int[] object, boolean optimizePositive) {
        for (int i = 0, n = object.length; i < n; i++) {
            writeInt(object[i], optimizePositive);
        }
    }

    /**
     * Bulk input of an int array.
     */
    public int[] readInts(int length, boolean optimizePositive) {
        int[] array = new int[length];
        for (int i = 0; i < length; i++) {
            array[i] = readInt(optimizePositive);
        }
        return array;
    }

    /**
     * Bulk output of an long array.
     */
    public void writeLongs(long[] object, boolean optimizePositive) {
        for (int i = 0, n = object.length; i < n; i++) {
            writeLong(object[i], optimizePositive);
        }
    }

    /**
     * Bulk input of a long array.
     */
    public long[] readLongs(int length, boolean optimizePositive) {
        long[] array = new long[length];
        for (int i = 0; i < length; i++) {
            array[i] = readLong(optimizePositive);
        }
        return array;
    }

    /**
     * Bulk output of an int array.
     */
    public void writeInts(int[] object) {
        for (int i = 0, n = object.length; i < n; i++) {
            writeInt(object[i]);
        }
    }

    /**
     * Bulk input of an int array.
     */
    public int[] readInts(int length) {
        int[] array = new int[length];
        for (int i = 0; i < length; i++) {
            array[i] = readInt();
        }
        return array;
    }

    /**
     * Bulk output of an long array.
     */
    public void writeLongs(long[] object) {
        for (int i = 0, n = object.length; i < n; i++) {
            writeLong(object[i]);
        }
    }

    /**
     * Bulk input of a long array.
     */
    public long[] readLongs(int length) {
        long[] array = new long[length];
        for (int i = 0; i < length; i++) {
            array[i] = readLong();
        }
        return array;
    }

    /**
     * Bulk output of a float array.
     */
    public void writeFloats(float[] object) {
        for (int i = 0, n = object.length; i < n; i++) {
            writeFloat(object[i]);
        }
    }

    /**
     * Bulk input of a float array.
     */
    public float[] readFloats(int length) {
        float[] array = new float[length];
        for (int i = 0; i < length; i++) {
            array[i] = readFloat();
        }
        return array;
    }

    /**
     * Bulk output of a short array.
     */
    public void writeShorts(short[] object) {
        for (int i = 0, n = object.length; i < n; i++) {
            writeShort(object[i]);
        }
    }

    /**
     * Bulk input of a short array.
     */
    public short[] readShorts(int length) {
        short[] array = new short[length];
        for (int i = 0; i < length; i++) {
            array[i] = readShort();
        }
        return array;
    }

    /**
     * Bulk output of a char array.
     */
    public void writeChars(char[] object) {
        for (int i = 0, n = object.length; i < n; i++) {
            writeChar(object[i]);
        }
    }

    /**
     * Bulk input of a char array.
     */
    public char[] readChars(int length) {
        char[] array = new char[length];
        for (int i = 0; i < length; i++) {
            array[i] = readChar();
        }
        return array;
    }

    /**
     * Bulk output of a double array.
     */
    public void writeDoubles(double[] object) {
        for (int i = 0, n = object.length; i < n; i++) {
            writeDouble(object[i]);
        }
    }

    /**
     * Bulk input of a double array
     */
    public double[] readDoubles(int length) {
        double[] array = new double[length];
        for (int i = 0; i < length; i++) {
            array[i] = readDouble();
        }
        return array;
    }


    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ByteBuffer2)) {
            return false;
        }

        // CANNOT be null, so we don't have to null check!
        return Arrays.equals(this.bytes, ((ByteBuffer2) other).bytes);
    }

    @Override
    public int hashCode() {
        // might be null for a thread because it's stale. who cares, get the value again
        return Arrays.hashCode(this.bytes);
    }

    @Override
    public String toString() {
        return "ByteBuffer2 " + java.util.Arrays.toString(this.bytes);
    }
}
