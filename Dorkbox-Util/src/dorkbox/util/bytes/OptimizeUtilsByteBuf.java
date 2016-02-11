/*
 * Copyright 2014 dorkbox, llc
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
  *
 * Copyright (c) 2008, Nathan Sweet
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package dorkbox.util.bytes;

import io.netty.buffer.ByteBuf;

public
class OptimizeUtilsByteBuf {

    // int

    /**
     * FROM KRYO
     * <p>
     * Returns the number of bytes that would be written with {@link #writeInt(ByteBuf, int, boolean)}.
     *
     * @param optimizePositive
     *                 true if you want to optimize the number of bytes needed to write the length value
     */
    public static
    int intLength(int value, boolean optimizePositive) {
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
     * FROM KRYO
     * <p>
     * look at buffer, and see if we can read the length of the int off of it. (from the reader index)
     *
     * @return 0 if we could not read anything, >0 for the number of bytes for the int on the buffer
     */
    public static
    int canReadInt(ByteBuf buffer) {
        int startIndex = buffer.readerIndex();
        try {
            int remaining = buffer.readableBytes();
            for (int offset = 0, count = 1; offset < 32 && remaining > 0; offset += 7, remaining--, count++) {
                int b = buffer.readByte();
                if ((b & 0x80) == 0) {
                    return count;
                }
            }
            return 0;
        } finally {
            buffer.readerIndex(startIndex);
        }
    }

    /**
     * FROM KRYO
     * <p>
     * Reads an int from the buffer that was optimized.
     */
    public static
    int readInt(ByteBuf buffer, boolean optimizePositive) {
        int b = buffer.readByte();
        int result = b & 0x7F;
        if ((b & 0x80) != 0) {
            b = buffer.readByte();
            result |= (b & 0x7F) << 7;
            if ((b & 0x80) != 0) {
                b = buffer.readByte();
                result |= (b & 0x7F) << 14;
                if ((b & 0x80) != 0) {
                    b = buffer.readByte();
                    result |= (b & 0x7F) << 21;
                    if ((b & 0x80) != 0) {
                        b = buffer.readByte();
                        result |= (b & 0x7F) << 28;
                    }
                }
            }
        }
        return optimizePositive ? result : result >>> 1 ^ -(result & 1);
    }

    /**
     * FROM KRYO
     * <p>
     * Writes the specified int to the buffer using 1 to 5 bytes, depending on the size of the number.
     *
     * @param optimizePositive
     *                 true if you want to optimize the number of bytes needed to write the length value
     *
     * @return the number of bytes written.
     */
    public static
    int writeInt(ByteBuf buffer, int value, boolean optimizePositive) {
        if (!optimizePositive) {
            value = value << 1 ^ value >> 31;
        }
        if (value >>> 7 == 0) {
            buffer.writeByte((byte) value);
            return 1;
        }
        if (value >>> 14 == 0) {
            buffer.writeByte((byte) (value & 0x7F | 0x80));
            buffer.writeByte((byte) (value >>> 7));
            return 2;
        }
        if (value >>> 21 == 0) {
            buffer.writeByte((byte) (value & 0x7F | 0x80));
            buffer.writeByte((byte) (value >>> 7 | 0x80));
            buffer.writeByte((byte) (value >>> 14));
            return 3;
        }
        if (value >>> 28 == 0) {
            buffer.writeByte((byte) (value & 0x7F | 0x80));
            buffer.writeByte((byte) (value >>> 7 | 0x80));
            buffer.writeByte((byte) (value >>> 14 | 0x80));
            buffer.writeByte((byte) (value >>> 21));
            return 4;
        }
        buffer.writeByte((byte) (value & 0x7F | 0x80));
        buffer.writeByte((byte) (value >>> 7 | 0x80));
        buffer.writeByte((byte) (value >>> 14 | 0x80));
        buffer.writeByte((byte) (value >>> 21 | 0x80));
        buffer.writeByte((byte) (value >>> 28));
        return 5;
    }

    // long

    /**
     * Returns the number of bytes that would be written with {@link #writeLong(long, boolean)}.
     *
     * @param optimizePositive
     *                 true if you want to optimize the number of bytes needed to write the length value
     */
    public static
    int longLength(long value, boolean optimizePositive) {
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

    /**
     * FROM KRYO
     * <p>
     * Reads a 1-9 byte long.
     *
     * @param optimizePositive
     *                 true if you want to optimize the number of bytes needed to write the length value
     */
    public static
    long readLong(ByteBuf buffer, boolean optimizePositive) {
        int b = buffer.readByte();
        long result = b & 0x7F;
        if ((b & 0x80) != 0) {
            b = buffer.readByte();
            result |= (b & 0x7F) << 7;
            if ((b & 0x80) != 0) {
                b = buffer.readByte();
                result |= (b & 0x7F) << 14;
                if ((b & 0x80) != 0) {
                    b = buffer.readByte();
                    result |= (b & 0x7F) << 21;
                    if ((b & 0x80) != 0) {
                        b = buffer.readByte();
                        result |= (long) (b & 0x7F) << 28;
                        if ((b & 0x80) != 0) {
                            b = buffer.readByte();
                            result |= (long) (b & 0x7F) << 35;
                            if ((b & 0x80) != 0) {
                                b = buffer.readByte();
                                result |= (long) (b & 0x7F) << 42;
                                if ((b & 0x80) != 0) {
                                    b = buffer.readByte();
                                    result |= (long) (b & 0x7F) << 49;
                                    if ((b & 0x80) != 0) {
                                        b = buffer.readByte();
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

    /**
     * FROM KRYO
     * <p>
     * Writes a 1-9 byte long.
     *
     * @param optimizePositive
     *                 If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be inefficient (9
     *                 bytes).
     *
     * @return the number of bytes written.
     */
    public static
    int writeLong(ByteBuf buffer, long value, boolean optimizePositive) {
        if (!optimizePositive) {
            value = value << 1 ^ value >> 63;
        }
        if (value >>> 7 == 0) {
            buffer.writeByte((byte) value);
            return 1;
        }
        if (value >>> 14 == 0) {
            buffer.writeByte((byte) (value & 0x7F | 0x80));
            buffer.writeByte((byte) (value >>> 7));
            return 2;
        }
        if (value >>> 21 == 0) {
            buffer.writeByte((byte) (value & 0x7F | 0x80));
            buffer.writeByte((byte) (value >>> 7 | 0x80));
            buffer.writeByte((byte) (value >>> 14));
            return 3;
        }
        if (value >>> 28 == 0) {
            buffer.writeByte((byte) (value & 0x7F | 0x80));
            buffer.writeByte((byte) (value >>> 7 | 0x80));
            buffer.writeByte((byte) (value >>> 14 | 0x80));
            buffer.writeByte((byte) (value >>> 21));
            return 4;
        }
        if (value >>> 35 == 0) {
            buffer.writeByte((byte) (value & 0x7F | 0x80));
            buffer.writeByte((byte) (value >>> 7 | 0x80));
            buffer.writeByte((byte) (value >>> 14 | 0x80));
            buffer.writeByte((byte) (value >>> 21 | 0x80));
            buffer.writeByte((byte) (value >>> 28));
            return 5;
        }
        if (value >>> 42 == 0) {
            buffer.writeByte((byte) (value & 0x7F | 0x80));
            buffer.writeByte((byte) (value >>> 7 | 0x80));
            buffer.writeByte((byte) (value >>> 14 | 0x80));
            buffer.writeByte((byte) (value >>> 21 | 0x80));
            buffer.writeByte((byte) (value >>> 28 | 0x80));
            buffer.writeByte((byte) (value >>> 35));
            return 6;
        }
        if (value >>> 49 == 0) {
            buffer.writeByte((byte) (value & 0x7F | 0x80));
            buffer.writeByte((byte) (value >>> 7 | 0x80));
            buffer.writeByte((byte) (value >>> 14 | 0x80));
            buffer.writeByte((byte) (value >>> 21 | 0x80));
            buffer.writeByte((byte) (value >>> 28 | 0x80));
            buffer.writeByte((byte) (value >>> 35 | 0x80));
            buffer.writeByte((byte) (value >>> 42));
            return 7;
        }
        if (value >>> 56 == 0) {
            buffer.writeByte((byte) (value & 0x7F | 0x80));
            buffer.writeByte((byte) (value >>> 7 | 0x80));
            buffer.writeByte((byte) (value >>> 14 | 0x80));
            buffer.writeByte((byte) (value >>> 21 | 0x80));
            buffer.writeByte((byte) (value >>> 28 | 0x80));
            buffer.writeByte((byte) (value >>> 35 | 0x80));
            buffer.writeByte((byte) (value >>> 42 | 0x80));
            buffer.writeByte((byte) (value >>> 49));
            return 8;
        }
        buffer.writeByte((byte) (value & 0x7F | 0x80));
        buffer.writeByte((byte) (value >>> 7 | 0x80));
        buffer.writeByte((byte) (value >>> 14 | 0x80));
        buffer.writeByte((byte) (value >>> 21 | 0x80));
        buffer.writeByte((byte) (value >>> 28 | 0x80));
        buffer.writeByte((byte) (value >>> 35 | 0x80));
        buffer.writeByte((byte) (value >>> 42 | 0x80));
        buffer.writeByte((byte) (value >>> 49 | 0x80));
        buffer.writeByte((byte) (value >>> 56));
        return 9;
    }

    /**
     * FROM KRYO
     * <p>
     * look at buffer, and see if we can read the length of the long off of it (from the reader index).
     *
     * @return 0 if we could not read anything, >0 for the number of bytes for the long on the buffer
     */
    public static
    int canReadLong(ByteBuf buffer) {
        int position = buffer.readerIndex();
        try {
            int remaining = buffer.readableBytes();
            for (int offset = 0, count = 1; offset < 64 && remaining > 0; offset += 7, remaining--, count++) {
                int b = buffer.readByte();
                if ((b & 0x80) == 0) {
                    return count;
                }
            }
            return 0;
        } finally {
            buffer.readerIndex(position);
        }
    }
}
