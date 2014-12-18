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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * This is (mostly) motorola, and is "network byte order".
 * This is also the default for Java.
 * <p>
 * arm is technically bi-endian
 */
public class BigEndian {
    // the following are ALL in Bit-Endian (byte[0] is most significant)
    // TODO: switch these to big endian. these are a copy of little endian

    /** CHAR to and from bytes */
    public static class Char_ {
        @SuppressWarnings("fallthrough")
        public static char from(byte[] bytes, int offset, int bytenum) {
            char number = 0;

            switch (bytenum) {
                case 2: number |= (bytes[offset+0] & 0xFF) <<  8;
                case 1: number |= (bytes[offset+1] & 0xFF) <<  0;
            }

            return number;
        }

        @SuppressWarnings("fallthrough")
        public static char from(byte[] bytes) {
            char number = 0;

            switch (bytes.length) {
                case 2: number |= (bytes[0] & 0xFF) <<  8;
                case 1: number |= (bytes[1] & 0xFF) <<  0;
            }

            return number;
        }

        public static char from(byte b0, byte b1) {
            return (char) ((b0 & 0xFF) << 8 |
                           (b1 & 0xFF) << 0);
        }

        public static byte[] toBytes(char x) {
            return new byte[] {(byte) (x >> 8),
                               (byte) (x >> 0)
                              };
        }

        public static char from(ByteBuffer buff) {
            return from(buff.get(), buff.get());
        }

        public static char fromStream(InputStream inputStream) throws IOException {
            byte[] b = new byte[2];
            if (inputStream.read(b) != 2) {
                throw new EOFException();
            }

            return from(b[0], b[1]);
        }
    }

    /** UNSIGNED CHAR to and from bytes */
    public static class UChar_ {
        @SuppressWarnings("fallthrough")
        public static UShort from(byte[] bytes, int offset, int bytenum) {
            char number = 0;

            switch (bytenum) {
                case 2: number |= (bytes[offset+0] & 0xFF) <<  8;
                case 1: number |= (bytes[offset+1] & 0xFF) <<  0;
            }

            return UShort.valueOf(number);
        }

        @SuppressWarnings("fallthrough")
        public static UShort from(byte[] bytes) {
            short number = 0;

            switch (bytes.length) {
                case 2: number |= (bytes[0] & 0xFF) <<  8;
                case 1: number |= (bytes[1] & 0xFF) <<  0;
            }

            return UShort.valueOf(number);
        }

        public static UShort from(byte b0, byte b1) {
            return UShort.valueOf((short)
                            ((b0 & 0xFF) << 8) |
                             (b1 & 0xFF) << 0) ;
        }


        public static byte[] toBytes(UShort x) {
            int num = x.intValue();

            return new byte[] {(byte) ((num & 0xFF00) >> 8),
                               (byte)  (num & 0x00FF  >> 0),
            };
        }

        public static UShort from(ByteBuffer buff) {
            return from(buff.get(), buff.get());
        }

        public static UShort fromStream(InputStream inputStream) throws IOException {
            byte[] b = new byte[2];
            if (inputStream.read(b) != 2) {
                throw new EOFException();
            }

            return from(b[0], b[1]);
        }
    }

    /** SHORT to and from bytes */
    public static class Short_ {
        @SuppressWarnings("fallthrough")
        public static short from(byte[] bytes, int offset, int bytenum) {
            short number = 0;

            switch (bytenum) {
                case 2: number |= (bytes[offset+0] & 0xFF) <<  8;
                case 1: number |= (bytes[offset+1] & 0xFF) <<  0;
            }

            return number;
        }

        @SuppressWarnings("fallthrough")
        public static short from(byte[] bytes) {
            short number = 0;

            switch (bytes.length) {
                case 2: number |= (bytes[0] & 0xFF) <<  8;
                case 1: number |= (bytes[1] & 0xFF) <<  0;
            }

            return number;
        }

        public static short from(byte b0, byte b1) {
            return (short) ((b0 & 0xFF) << 8 |
                            (b1 & 0xFF) << 0);
        }


        public static byte[] toBytes(short x) {
            return new byte[] {(byte) (x >> 8),
                               (byte) (x >> 0)
                              };
        }

        public static short from(ByteBuffer buff) {
            return from(buff.get(), buff.get());
        }

        public static short fromStream(InputStream inputStream) throws IOException {
            byte[] b = new byte[2];
            if (inputStream.read(b) != 2) {
                throw new EOFException();
            }

            return from(b[0], b[1]);
        }
    }

    /** UNSIGNED SHORT to and from bytes */
    public static class UShort_ {
        @SuppressWarnings("fallthrough")
        public static UShort from(byte[] bytes, int offset, int bytenum) {
            char number = 0;

            switch (bytenum) {
                case 2: number |= (bytes[offset+0] & 0xFF) <<  8;
                case 1: number |= (bytes[offset+1] & 0xFF) <<  0;
            }

            return UShort.valueOf(number);
        }

        @SuppressWarnings("fallthrough")
        public static UShort from(byte[] bytes) {
            short number = 0;

            switch (bytes.length) {
                case 2: number |= (bytes[0] & 0xFF) <<  8;
                case 1: number |= (bytes[1] & 0xFF) <<  0;
            }

            return UShort.valueOf(number);
        }

        public static UShort from(byte b0, byte b1) {
            return UShort.valueOf((short)
                            ((b0 & 0xFF) << 8) |
                             (b1 & 0xFF) << 0) ;
        }


        public static byte[] toBytes(UShort x) {
            int num = x.intValue();

            return new byte[] {(byte) ((num & 0xFF00) >> 8),
                               (byte)  (num & 0x00FF  >> 0),
            };
        }

        public static UShort from(ByteBuffer buff) {
            return from(buff.get(), buff.get());
        }

        public static UShort fromStream(InputStream inputStream) throws IOException {
            byte[] b = new byte[2];
            if (inputStream.read(b) != 2) {
                throw new EOFException();
            }

            return from(b[0], b[1]);
        }
    }

    /** INT to and from bytes */
    public static class Int_ {
        @SuppressWarnings("fallthrough")
        public static int from(byte[] bytes, int offset, int bytenum) {
            int number = 0;

            switch (bytenum) {
                case 4: number |= (bytes[offset+0] & 0xFF) << 24;
                case 3: number |= (bytes[offset+1] & 0xFF) << 16;
                case 2: number |= (bytes[offset+2] & 0xFF) <<  8;
                case 1: number |= (bytes[offset+3] & 0xFF) <<  0;
            }

            return number;
        }

        @SuppressWarnings("fallthrough")
        public static int from(byte[] bytes) {
            int number = 0;

            switch (bytes.length) {
                case 4: number |= (bytes[0] & 0xFF) << 24;
                case 3: number |= (bytes[1] & 0xFF) << 16;
                case 2: number |= (bytes[2] & 0xFF) <<  8;
                case 1: number |= (bytes[3] & 0xFF) <<  0;
            }

            return number;
        }

        public static int from(byte b0, byte b1, byte b2, byte b3) {
            return (b0 & 0xFF) << 24 |
                   (b1 & 0xFF) << 16 |
                   (b2 & 0xFF) <<  8 |
                   (b3 & 0xFF) <<  0;
        }

        public static byte[] toBytes(int x) {
            return new byte[] {(byte) (x >> 24),
                               (byte) (x >> 16),
                               (byte) (x >>  8),
                               (byte) (x >>  0)
                              } ;
        }

        public static int from(ByteBuffer buff) {
            return from(buff.get(), buff.get(), buff.get(), buff.get());
        }

        public static int fromStream(InputStream inputStream) throws IOException {
            byte[] b = new byte[4];
            if (inputStream.read(b) != 4) {
                throw new EOFException();
            }

            return from(b[0], b[1], b[2], b[3]);
        }
    }

    /** UNSIGNED INT to and from bytes */
    public static class UInt_ {
        @SuppressWarnings("fallthrough")
        public static UInteger from(byte[] bytes, int offset, int bytenum) {
            int number = 0;

            switch (bytenum) {
                case 4: number |= (bytes[offset+0] & 0xFF) << 24;
                case 3: number |= (bytes[offset+1] & 0xFF) << 16;
                case 2: number |= (bytes[offset+2] & 0xFF) <<  8;
                case 1: number |= (bytes[offset+3] & 0xFF) <<  0;
            }

            return UInteger.valueOf(number);
        }

        @SuppressWarnings("fallthrough")
        public static UInteger from(byte[] bytes) {
            int number = 0;

            switch (bytes.length) {
                case 4: number |= (bytes[0] & 0xFF) << 24;
                case 3: number |= (bytes[1] & 0xFF) << 16;
                case 2: number |= (bytes[2] & 0xFF) <<  8;
                case 1: number |= (bytes[3] & 0xFF) <<  0;
            }

            return UInteger.valueOf(number);
        }

        public static UInteger from(byte b0, byte b1, byte b2, byte b3) {
            int number = (b0 & 0xFF) << 24 |
                         (b1 & 0xFF) << 16 |
                         (b2 & 0xFF) <<  8 |
                         (b3 & 0xFF) <<  0;

            return UInteger.valueOf(number);
        }

        public static byte[] toBytes(UInteger x) {
            long num = x.longValue();

            return new byte[] {(byte) ((num & 0xFF000000L) >> 24),
                               (byte) ((num & 0x00FF0000L) >> 16),
                               (byte) ((num & 0x0000FF00L) >> 8),
                               (byte)  (num & 0x000000FFL  >> 0)
            };
        }

        public static UInteger from(ByteBuffer buff) {
            return from(buff.get(), buff.get(), buff.get(), buff.get());
        }

        public static UInteger fromStream(InputStream inputStream) throws IOException {
            byte[] b = new byte[4];
            if (inputStream.read(b) != 4) {
                throw new EOFException();
            }

            return from(b[0], b[1], b[2], b[3]);
        }
    }

    /** LONG to and from bytes */
    public static class Long_ {
        @SuppressWarnings("fallthrough")
        public static long from(byte[] bytes, int offset, int bytenum) {
            long number = 0;

            switch (bytenum) {
                case 8: number |= (long) (bytes[offset+0] & 0xFF) << 56;
                case 7: number |= (long) (bytes[offset+1] & 0xFF) << 48;
                case 6: number |= (long) (bytes[offset+2] & 0xFF) << 40;
                case 5: number |= (long) (bytes[offset+3] & 0xFF) << 32;
                case 4: number |= (long) (bytes[offset+4] & 0xFF) << 24;
                case 3: number |= (long) (bytes[offset+5] & 0xFF) << 16;
                case 2: number |= (long) (bytes[offset+6] & 0xFF) <<  8;
                case 1: number |= (long) (bytes[offset+7] & 0xFF) <<  0;
            }

            return number;
        }

        @SuppressWarnings("fallthrough")
        public static long from(byte[] bytes) {
            long number = 0L;

            switch (bytes.length) {
                case 8: number |= (long) (bytes[0] & 0xFF) << 56;
                case 7: number |= (long) (bytes[1] & 0xFF) << 48;
                case 6: number |= (long) (bytes[2] & 0xFF) << 40;
                case 5: number |= (long) (bytes[3] & 0xFF) << 32;
                case 4: number |= (long) (bytes[4] & 0xFF) << 24;
                case 3: number |= (long) (bytes[5] & 0xFF) << 16;
                case 2: number |= (long) (bytes[6] & 0xFF) <<  8;
                case 1: number |= (long) (bytes[7] & 0xFF) <<  0;
            }

            return number;
        }

        public static long from(byte b0, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7) {
            return (long) (b0 & 0xFF) << 56 |
                   (long) (b1 & 0xFF) << 48 |
                   (long) (b2 & 0xFF) << 40 |
                   (long) (b3 & 0xFF) << 32 |
                   (long) (b4 & 0xFF) << 24 |
                   (long) (b5 & 0xFF) << 16 |
                   (long) (b6 & 0xFF) <<  8 |
                   (long) (b7 & 0xFF) <<  0;
        }

        public static byte[] toBytes (long x) {
            return new byte[] {(byte) (x >> 56),
                               (byte) (x >> 48),
                               (byte) (x >> 40),
                               (byte) (x >> 32),
                               (byte) (x >> 24),
                               (byte) (x >> 16),
                               (byte) (x >>  8),
                               (byte) (x >>  0)
                              };
        }

        public static long from(ByteBuffer buff) {
            return from(buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get());
        }

        public static long fromStream(InputStream inputStream) throws IOException {
            byte[] b = new byte[8];
            if (inputStream.read(b) != 8) {
                throw new EOFException();
            }

            return from(b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7]);
        }
    }

    /** UNSIGNED LONG to and from bytes */
    public static class ULong_ {
        @SuppressWarnings("fallthrough")
        public static ULong from(byte[] bytes, int offset, int bytenum) {
            long number = 0;

            switch (bytenum) {
                case 8: number |= (long) (bytes[offset+0] & 0xFF) << 56;
                case 7: number |= (long) (bytes[offset+1] & 0xFF) << 48;
                case 6: number |= (long) (bytes[offset+2] & 0xFF) << 40;
                case 5: number |= (long) (bytes[offset+3] & 0xFF) << 32;
                case 4: number |= (long) (bytes[offset+4] & 0xFF) << 24;
                case 3: number |= (long) (bytes[offset+5] & 0xFF) << 16;
                case 2: number |= (long) (bytes[offset+6] & 0xFF) <<  8;
                case 1: number |= (long) (bytes[offset+7] & 0xFF) <<  0;
            }

            return ULong.valueOf(number);
        }

        public static ULong from(byte[] bytes) {
            BigInteger ulong = new BigInteger(1, bytes);
            return ULong.valueOf(ulong);
        }

        public static ULong from(byte b0, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7) {
            byte[] bytes = new byte[] {b0, b1, b2, b3, b4, b5, b6, b7};
            BigInteger ulong = new BigInteger(1, bytes);
            return ULong.valueOf(ulong);
        }

        public static byte[] toBytes (ULong x) {
            return x.toBigInteger().toByteArray();
        }

        public static ULong from(ByteBuffer buff) {
            return from(buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get());
        }

        public static ULong from(InputStream inputStream) throws IOException {
            byte[] b = new byte[8];
            if (inputStream.read(b) != 8) {
                throw new EOFException();
            }

            return from(b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7]);
        }
    }
}

