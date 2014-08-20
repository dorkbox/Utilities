package dorkbox.util.bytes;

import java.nio.ByteBuffer;

public class BigEndian {
    // the following are ALL in Big-Endian (big is to the left, first byte is most significant, unsigned bytes)

    /** SHORT to and from bytes */
    public static class Short_ {
        public static final short fromBytes(byte[] bytes) {
            return fromBytes(bytes[0], bytes[1]);
        }

        public static final short fromBytes(byte b0, byte b1) {
            return (short) ((b0 & 0xFF) << 8 |
                            (b1 & 0xFF) << 0);
        }


        public static final byte[] toBytes(short x) {
            return new byte[] {(byte) (x >> 8),
                               (byte) (x >> 0)
                              };
        }

        public static final int fromBytes(ByteBuffer buff) {
            return fromBytes(buff.get(), buff.get());
        }
    }

    /** CHAR to and from bytes */
    public static class Char_ {
        public static final char fromBytes(byte[] bytes) {
            return fromBytes(bytes[0], bytes[1]);
        }

        public static final char fromBytes(byte b0, byte b1) {
            return (char) ((b0 & 0xFF) << 8 |
                           (b1 & 0xFF) << 0);
        }


        public static final byte[] toBytes(char x) {
            return new byte[] {(byte) (x >> 8),
                               (byte) (x >> 0)
                              };
        }

        public static final int fromBytes(ByteBuffer buff) {
            return fromBytes(buff.get(), buff.get());
        }
    }


    /** INT to and from bytes */
    public static class Int_ {
        public static final int fromBytes(byte[] bytes) {
            return fromBytes(bytes[0], bytes[1], bytes[2], bytes[3]);
        }

        public static final int fromBytes(byte b0, byte b1, byte b2, byte b3) {
            return (b0 & 0xFF) << 24 |
                   (b1 & 0xFF) << 16 |
                   (b2 & 0xFF) <<  8 |
                   (b3 & 0xFF) <<  0;
        }

        public static int fromBytes(byte b0, byte b1) {
            return (b0 & 0xFF) << 24 |
                   (b1 & 0xFF) << 16;
        }

        public static final byte[] toBytes(int x) {
            return new byte[] {(byte) (x >> 24),
                               (byte) (x >> 16),
                               (byte) (x >>  8),
                               (byte) (x >>  0)
                              } ;
        }

        public static final int fromBytes(ByteBuffer buff) {
            return fromBytes(buff.get(), buff.get(), buff.get(), buff.get());
        }
    }

    /** LONG to and from bytes */
    public static class Long_ {
        public static final long fromBytes(byte[] bytes) {
            return fromBytes(bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7]);
        }

        public static final long fromBytes(byte b0, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7) {
             return  (long) (b0 & 0xFF) << 56 |
                     (long) (b1 & 0xFF) << 48 |
                     (long) (b2 & 0xFF) << 40 |
                     (long) (b3 & 0xFF) << 32 |
                     (long) (b4 & 0xFF) << 24 |
                     (long) (b5 & 0xFF) << 16 |
                     (long) (b6 & 0xFF) <<  8 |
                     (long) (b7 & 0xFF) <<  0;
        }

        public static final byte[] toBytes (long x) {
            return new byte[] {(byte) (x >> 56),
                               (byte) (x >> 48),
                               (byte) (x >> 40),
                               (byte) (x >> 32),
                               (byte) (x >> 24),
                               (byte) (x >> 16),
                               (byte) (x >>  8),
                               (byte) (x >>  0),
                              };
        }

        public static final long fromBytes(ByteBuffer buff) {
            return fromBytes(buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get());
        }
    }
}

