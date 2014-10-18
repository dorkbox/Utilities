package dorkbox.util.bytes;

import io.netty.buffer.ByteBuf;

public class OptimizeUtilsByteBuf {

    private static final OptimizeUtilsByteBuf instance = new OptimizeUtilsByteBuf();

    public static OptimizeUtilsByteBuf get() {
        return instance;
    }

    // int

    /**
     * FROM KRYO
     *
     * Returns the number of bytes that would be written with {@link #writeInt(int, boolean)}.
     *
     * @param optimizePositive
     *            true if you want to optimize the number of bytes needed to write the length value
     */
    public final int intLength(int value, boolean optimizePositive) {
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
     *
     * look at buffer, and see if we can read the length of the int off of it. (from the reader index)
     *
     * @return 0 if we could not read anything, >0 for the number of bytes for the int on the buffer
     */
    public final int canReadInt(ByteBuf buffer) {
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
     *
     * Reads an int from the buffer that was optimized.
     */
    public final int readInt(ByteBuf buffer, boolean optimizePositive) {
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
     *
     * Writes the specified int to the buffer using 1 to 5 bytes, depending on the size of the number.
     *
     * @param optimizePositive
     *            true if you want to optimize the number of bytes needed to write the length value
     * @return the number of bytes written.
     */
    public final int writeInt(ByteBuf buffer, int value, boolean optimizePositive) {
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
     *            true if you want to optimize the number of bytes needed to write the length value
     */
    public final int longLength(long value, boolean optimizePositive) {
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
     *
     * Reads a 1-9 byte long.
     *
     * @param optimizePositive
     *            true if you want to optimize the number of bytes needed to write the length value
     */
    public final long readLong(ByteBuf buffer, boolean optimizePositive) {
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
     *
     * Writes a 1-9 byte long.
     *
     * @param optimizePositive
     *            If true, small positive numbers will be more efficient (1 byte) and small negative numbers will be
     *            inefficient (9 bytes).
     * @return the number of bytes written.
     */
    public final int writeLong(ByteBuf buffer, long value, boolean optimizePositive) {
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
     *
     * look at buffer, and see if we can read the length of the long off of it (from the reader index).
     *
     * @return 0 if we could not read anything, >0 for the number of bytes for the long on the buffer
     */
    public final int canReadLong(ByteBuf buffer) {
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
