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
 */
package dorkbox.util.bytes;

import java.nio.ByteBuffer;

/**
 * This is intel/amd/arm arch!
 *
 * arm is technically bi-endian
 */
public class LittleEndian {
    // the following are ALL in Little-Endian (big is to the right, first byte is least significant, unsigned bytes)

    /** CHAR to and from bytes */
    public static class Char_ {
        @SuppressWarnings("fallthrough")
        public static final char fromBytes(byte[] bytes) {
            char number = 0;

            switch (bytes.length) {
                case 2: number += (bytes[1] & 0xFF) <<  8;
                case 1: number += (bytes[0] & 0xFF) <<  0;
            }

            return number;
        }

        public static final char fromBytes(byte b0, byte b1) {
            return (char) ((b1 & 0xFF) << 8 |
                           (b0 & 0xFF) << 0);
        }

        public static final byte[] toBytes(char x) {
            return new byte[] {(byte) (x >> 0),
                               (byte) (x >> 8)
                              };
        }

        public static final int fromBytes(ByteBuffer buff) {
            return fromBytes(buff.get(), buff.get());
        }
    }

    /** CHAR to and from bytes */
    public static class UChar_ {
        @SuppressWarnings("fallthrough")
        public static final char fromBytes(byte[] bytes) {
            char number = 0;

            switch (bytes.length) {
                case 2: number += (bytes[1] & 0xFF) <<  8;
                case 1: number += (bytes[0] & 0xFF) <<  0;
            }

            return number;
        }

        public static final char fromBytes(byte b0, byte b1) {
            return (char) ((b1 & 0xFF) << 8 |
                           (b0 & 0xFF) << 0);
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

    /** SHORT to and from bytes */
    public static class Short_ {
        @SuppressWarnings("fallthrough")
        public static final short fromBytes(byte[] bytes) {
            short number = 0;

            switch (bytes.length) {
                case 2: number += (bytes[1] & 0xFF) <<  8;
                case 1: number += (bytes[0] & 0xFF) <<  0;
            }

            return number;
        }

        public static final short fromBytes(byte b0, byte b1) {
            return (short) ((b1 & 0xFF) << 8 |
                            (b0 & 0xFF) << 0);
        }


        public static final byte[] toBytes(short x) {
            return new byte[] {(byte) (x >> 0),
                               (byte) (x >> 8)
                              };
        }

        public static final int fromBytes(ByteBuffer buff) {
            return fromBytes(buff.get(), buff.get());
        }
    }

    /** SHORT to and from bytes */
    public static class UShort_ {
        @SuppressWarnings("fallthrough")
        public static final short fromBytes(byte[] bytes) {
            short number = 0;

            switch (bytes.length) {
                case 2: number += (bytes[1] & 0xFF) <<  8;
                case 1: number += (bytes[0] & 0xFF) <<  0;
            }

            return number;
        }

        @SuppressWarnings("fallthrough")
        public static short fromBytes(byte[] bytes, int offset, int bytenum) {
            short number = 0;

            switch (bytenum) {
                case 2: number += (bytes[offset+1] & 0xFF) <<  8;
                case 1: number += (bytes[offset+0] & 0xFF) <<  0;
            }

            return number;
        }

        public static final short fromBytes(byte b0, byte b1) {
            return (short) ((b1 & 0xFF) << 8 |
                            (b0 & 0xFF) << 0);
        }


        public static final byte[] toBytes(short x) {
            return new byte[] {(byte) (x >> 0),
                               (byte) (x >> 8)
            };
        }

        public static final int fromBytes(ByteBuffer buff) {
            return fromBytes(buff.get(), buff.get());
        }
    }

    /** INT to and from bytes */
    public static class Int_ {
        @SuppressWarnings("fallthrough")
        public static final int fromBytes(byte[] bytes) {
            int number = 0;

            switch (bytes.length) {
                case 4: number += (bytes[3] & 0xFF) << 24;
                case 3: number += (bytes[2] & 0xFF) << 16;
                case 2: number += (bytes[1] & 0xFF) <<  8;
                case 1: number += (bytes[0] & 0xFF) <<  0;
            }

            return number;
        }

        public static final int fromBytes(byte b0, byte b1, byte b2, byte b3) {
            return (b3 & 0xFF) << 24 |
                   (b2 & 0xFF) << 16 |
                   (b1 & 0xFF) <<  8 |
                   (b0 & 0xFF) <<  0;
        }

        public static final byte[] toBytes(int x) {
            return new byte[] {(byte) (x >>  0),
                               (byte) (x >>  8),
                               (byte) (x >> 16),
                               (byte) (x >> 24)
                              } ;
        }

        public static final int fromBytes(ByteBuffer buff) {
            return fromBytes(buff.get(), buff.get(), buff.get(), buff.get());
        }
    }

    /** INT to and from bytes */
    public static class UInt_ {
        @SuppressWarnings("fallthrough")
        public static final int fromBytes(byte[] bytes) {
            int number = 0;

            switch (bytes.length) {
                case 4: number += (bytes[3] & 0xFF) << 24;
                case 3: number += (bytes[2] & 0xFF) << 16;
                case 2: number += (bytes[1] & 0xFF) <<  8;
                case 1: number += (bytes[0] & 0xFF) <<  0;
            }

            return number;
        }

        @SuppressWarnings("fallthrough")
        public static int fromBytes(byte[] bytes, int offset, int bytenum) {
            int number = 0;

            switch (bytenum) {
                case 4: number += (bytes[offset+3] & 0xFF) << 24;
                case 3: number += (bytes[offset+2] & 0xFF) << 16;
                case 2: number += (bytes[offset+1] & 0xFF) <<  8;
                case 1: number += (bytes[offset+0] & 0xFF) <<  0;
            }

            return number;
        }

        public static final int fromBytes(byte b0, byte b1, byte b2, byte b3) {
            return (b3 & 0xFF) << 24 |
                   (b2 & 0xFF) << 16 |
                   (b1 & 0xFF) <<  8 |
                   (b0 & 0xFF) <<  0;
        }

        public static final byte[] toBytes(int x) {
            return new byte[] {(byte) (x >>  0),
                               (byte) (x >>  8),
                               (byte) (x >> 16),
                               (byte) (x >> 24)
            } ;
        }

        public static final int fromBytes(ByteBuffer buff) {
            return fromBytes(buff.get(), buff.get(), buff.get(), buff.get());
        }
    }

    /** LONG to and from bytes */
    public static class Long_ {

        @SuppressWarnings("fallthrough")
        public static final long fromBytes(byte[] bytes) {
            long number = 0L;

            switch (bytes.length) {
                case 8: number += (long) (bytes[7] & 0xFF) << 56;
                case 7: number += (long) (bytes[6] & 0xFF) << 48;
                case 6: number += (long) (bytes[5] & 0xFF) << 40;
                case 5: number += (long) (bytes[4] & 0xFF) << 32;
                case 4: number += (long) (bytes[3] & 0xFF) << 24;
                case 3: number += (long) (bytes[2] & 0xFF) << 16;
                case 2: number += (long) (bytes[1] & 0xFF) <<  8;
                case 1: number += (long) (bytes[0] & 0xFF) <<  0;
            }

            return number;
        }

        public static final long fromBytes(byte b0, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7) {
            return (long) (b7 & 0xFF) << 56 |
                   (long) (b6 & 0xFF) << 48 |
                   (long) (b5 & 0xFF) << 40 |
                   (long) (b4 & 0xFF) << 32 |
                   (long) (b3 & 0xFF) << 24 |
                   (long) (b2 & 0xFF) << 16 |
                   (long) (b1 & 0xFF) <<  8 |
                   (long) (b0 & 0xFF) <<  0;
        }

        public static final byte[] toBytes (long x) {
            return new byte[] {(byte) (x >>  0),
                               (byte) (x >>  8),
                               (byte) (x >> 16),
                               (byte) (x >> 24),
                               (byte) (x >> 32),
                               (byte) (x >> 40),
                               (byte) (x >> 48),
                               (byte) (x >> 56),
                              };
        }

        public static final long fromBytes(ByteBuffer buff) {
            return fromBytes(buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get());
        }
    }

    /** LONG to and from bytes */
    public static class ULong_ {
        @SuppressWarnings("fallthrough")
        public static int fromBytes(byte[] bytes, int offset, int bytenum) {
            int number = 0;

            switch (bytenum) {
                case 8: number += (long) (bytes[offset+7] & 0xFF) << 56;
                case 7: number += (long) (bytes[offset+6] & 0xFF) << 48;
                case 6: number += (long) (bytes[offset+5] & 0xFF) << 40;
                case 5: number += (long) (bytes[offset+4] & 0xFF) << 32;
                case 4: number += (long) (bytes[offset+3] & 0xFF) << 24;
                case 3: number += (long) (bytes[offset+2] & 0xFF) << 16;
                case 2: number += (long) (bytes[offset+1] & 0xFF) <<  8;
                case 1: number += (long) (bytes[offset+0] & 0xFF) <<  0;
            }

            return number;
        }

        @SuppressWarnings("fallthrough")
        public static final long fromBytes(byte[] bytes) {
            long number = 0L;

            switch (bytes.length) {
                case 8: number += (long) (bytes[7] & 0xFF) << 56;
                case 7: number += (long) (bytes[6] & 0xFF) << 48;
                case 6: number += (long) (bytes[5] & 0xFF) << 40;
                case 5: number += (long) (bytes[4] & 0xFF) << 32;
                case 4: number += (long) (bytes[3] & 0xFF) << 24;
                case 3: number += (long) (bytes[2] & 0xFF) << 16;
                case 2: number += (long) (bytes[1] & 0xFF) <<  8;
                case 1: number += (long) (bytes[0] & 0xFF) <<  0;
            }

            return number;
        }

        public static final long fromBytes(byte b0, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7) {
            return (long) (b7 & 0xFF) << 56 |
                   (long) (b6 & 0xFF) << 48 |
                   (long) (b5 & 0xFF) << 40 |
                   (long) (b4 & 0xFF) << 32 |
                   (long) (b3 & 0xFF) << 24 |
                   (long) (b2 & 0xFF) << 16 |
                   (long) (b1 & 0xFF) <<  8 |
                   (long) (b0 & 0xFF) <<  0;
        }

        public static final byte[] toBytes (long x) {
            return new byte[] {(byte) (x >>  0),
                    (byte) (x >>  8),
                    (byte) (x >> 16),
                    (byte) (x >> 24),
                    (byte) (x >> 32),
                    (byte) (x >> 40),
                    (byte) (x >> 48),
                    (byte) (x >> 56),
            };
        }

        public static final long fromBytes(ByteBuffer buff) {
            return fromBytes(buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get());
        }
    }
}

