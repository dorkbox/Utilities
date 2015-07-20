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
 * This is intel/amd/arm arch!
 * <p/>
 * arm is technically bi-endian
 * <p/>
 * Network byte order IS big endian, as is Java.
 */
@SuppressWarnings("ALL")
public
class LittleEndian {
    // the following are ALL in Little-Endian (byte[0] is least significant)


    /**
     * CHAR to and from bytes
     */
    public static final
    class Char_ {
        @SuppressWarnings("fallthrough")
        public static
        char from(byte[] bytes, int offset, int byteNum) {
            char number = 0;

            switch (byteNum) {
                case 2:
                    number |= (bytes[offset + 1] & 0xFF) << 8;
                case 1:
                    number |= (bytes[offset + 0] & 0xFF) << 0;
            }

            return number;
        }

        @SuppressWarnings("fallthrough")
        public static
        char from(byte[] bytes) {
            char number = 0;

            switch (bytes.length) {
                case 2:
                    number |= (bytes[1] & 0xFF) << 8;
                case 1:
                    number |= (bytes[0] & 0xFF) << 0;
            }

            return number;
        }

        public static
        char from(byte b0, byte b1) {
            return (char) ((b1 & 0xFF) << 8 | (b0 & 0xFF) << 0);
        }

        public static
        byte[] toBytes(char x) {
            return new byte[] {(byte) (x >> 0), (byte) (x >> 8)};
        }

        public static
        char from(ByteBuffer buff) {
            return from(buff.get(), buff.get());
        }

        public static
        char from(InputStream inputStream) throws IOException {
            byte[] b = new byte[2];
            if (inputStream.read(b) != 2) {
                throw new EOFException();
            }

            return from(b[0], b[1]);
        }

        private
        Char_() {
        }
    }


    /**
     * UNSIGNED CHAR to and from bytes
     */
    public static final
    class UChar_ {
        @SuppressWarnings("fallthrough")
        public static
        UShort from(byte[] bytes, int offset, int bytenum) {
            char number = 0;

            switch (bytenum) {
                case 2:
                    number |= (bytes[offset + 1] & 0xFF) << 8;
                case 1:
                    number |= (bytes[offset + 0] & 0xFF) << 0;
            }

            return UShort.valueOf(number);
        }

        @SuppressWarnings("fallthrough")
        public static
        UShort from(byte[] bytes) {
            short number = 0;

            switch (bytes.length) {
                case 2:
                    number |= (bytes[1] & 0xFF) << 8;
                case 1:
                    number |= (bytes[0] & 0xFF) << 0;
            }

            return UShort.valueOf(number);
        }

        public static
        UShort from(byte b0, byte b1) {
            return UShort.valueOf((short) ((b1 & 0xFF) << 8) | (b0 & 0xFF) << 0);
        }

        public static
        byte[] toBytes(UShort x) {
            int num = x.intValue();

            return new byte[] {(byte) (num & 0x00FF >> 0), (byte) ((num & 0xFF00) >> 8)};
        }

        public static
        UShort from(ByteBuffer buff) {
            return from(buff.get(), buff.get());
        }

        public static
        UShort from(InputStream inputStream) throws IOException {
            byte[] b = new byte[2];
            if (inputStream.read(b) != 2) {
                throw new EOFException();
            }

            return from(b[0], b[1]);
        }

        private
        UChar_() {
        }
    }


    /**
     * SHORT to and from bytes
     */
    public static final
    class Short_ {
        @SuppressWarnings("fallthrough")
        public static
        short from(byte[] bytes, int offset, int bytenum) {
            short number = 0;

            switch (bytenum) {
                case 2:
                    number |= (bytes[offset + 1] & 0xFF) << 8;
                case 1:
                    number |= (bytes[offset + 0] & 0xFF) << 0;
            }

            return number;
        }

        @SuppressWarnings("fallthrough")
        public static
        short from(byte[] bytes) {
            short number = 0;

            switch (bytes.length) {
                case 2:
                    number |= (bytes[1] & 0xFF) << 8;
                case 1:
                    number |= (bytes[0] & 0xFF) << 0;
            }

            return number;
        }

        public static
        short from(byte b0, byte b1) {
            return (short) ((b1 & 0xFF) << 8 | (b0 & 0xFF) << 0);
        }

        public static
        byte[] toBytes(short x) {
            return new byte[] {(byte) (x >> 0), (byte) (x >> 8)};
        }

        public static
        short from(ByteBuffer buff) {
            return from(buff.get(), buff.get());
        }

        public static
        short from(InputStream inputStream) throws IOException {
            byte[] b = new byte[2];
            if (inputStream.read(b) != 2) {
                throw new EOFException();
            }

            return from(b[0], b[1]);
        }

        private
        Short_() {
        }
    }


    /**
     * UNSIGNED SHORT to and from bytes
     */
    public static final
    class UShort_ {
        @SuppressWarnings("fallthrough")
        public static
        UShort from(byte[] bytes, int offset, int bytenum) {
            short number = 0;

            switch (bytenum) {
                case 2:
                    number |= (bytes[offset + 1] & 0xFF) << 8;
                case 1:
                    number |= (bytes[offset + 0] & 0xFF) << 0;
            }

            return UShort.valueOf(number);
        }

        @SuppressWarnings("fallthrough")
        public static
        UShort from(byte[] bytes) {
            short number = 0;

            switch (bytes.length) {
                case 2:
                    number |= (bytes[1] & 0xFF) << 8;
                case 1:
                    number |= (bytes[0] & 0xFF) << 0;
            }

            return UShort.valueOf(number);
        }

        public static
        UShort from(byte b0, byte b1) {
            return UShort.valueOf((short) ((b1 & 0xFF) << 8 | (b0 & 0xFF) << 0));
        }

        public static
        byte[] toBytes(UShort x) {
            int num = x.intValue();

            return new byte[] {(byte) (num & 0x00FF >> 0), (byte) ((num & 0xFF00) >> 8)};
        }

        public static
        UShort from(ByteBuffer buff) {
            return from(buff.get(), buff.get());
        }

        public static
        UShort from(InputStream inputStream) throws IOException {
            byte[] b = new byte[2];
            if (inputStream.read(b) != 2) {
                throw new EOFException();
            }

            return from(b[0], b[1]);
        }

        private
        UShort_() {
        }
    }


    /**
     * INT to and from bytes
     */
    public static final
    class Int_ {
        @SuppressWarnings("fallthrough")
        public static
        int from(byte[] bytes, int offset, int bytenum) {
            int number = 0;

            switch (bytenum) {
                case 4:
                    number |= (bytes[offset + 3] & 0xFF) << 24;
                case 3:
                    number |= (bytes[offset + 2] & 0xFF) << 16;
                case 2:
                    number |= (bytes[offset + 1] & 0xFF) << 8;
                case 1:
                    number |= (bytes[offset + 0] & 0xFF) << 0;
            }

            return number;
        }

        @SuppressWarnings("fallthrough")
        public static
        int from(byte[] bytes) {
            int number = 0;

            switch (bytes.length) {
                case 4:
                    number |= (bytes[3] & 0xFF) << 24;
                case 3:
                    number |= (bytes[2] & 0xFF) << 16;
                case 2:
                    number |= (bytes[1] & 0xFF) << 8;
                case 1:
                    number |= (bytes[0] & 0xFF) << 0;
            }

            return number;
        }

        public static
        int from(byte b0, byte b1, byte b2, byte b3) {
            return (b3 & 0xFF) << 24 |
                   (b2 & 0xFF) << 16 |
                   (b1 & 0xFF) << 8 |
                   (b0 & 0xFF) << 0;
        }

        public static
        byte[] toBytes(int x) {
            return new byte[] {(byte) (x >> 0), (byte) (x >> 8), (byte) (x >> 16), (byte) (x >> 24)};
        }

        public static
        int from(ByteBuffer buff) {
            return from(buff.get(), buff.get(), buff.get(), buff.get());
        }

        public static
        int from(InputStream inputStream) throws IOException {
            byte[] b = new byte[4];
            if (inputStream.read(b) != 4) {
                throw new EOFException();
            }

            return from(b[0], b[1], b[2], b[3]);
        }

        private
        Int_() {
        }
    }


    /**
     * UNSIGNED INT to and from bytes
     */
    public static final
    class UInt_ {
        @SuppressWarnings("fallthrough")
        public static
        UInteger from(byte[] bytes, int offset, int bytenum) {
            int number = 0;

            switch (bytenum) {
                case 4:
                    number |= (bytes[offset + 3] & 0xFF) << 24;
                case 3:
                    number |= (bytes[offset + 2] & 0xFF) << 16;
                case 2:
                    number |= (bytes[offset + 1] & 0xFF) << 8;
                case 1:
                    number |= (bytes[offset + 0] & 0xFF) << 0;
            }

            return UInteger.valueOf(number);
        }

        @SuppressWarnings("fallthrough")
        public static
        UInteger from(byte[] bytes) {
            int number = 0;

            switch (bytes.length) {
                case 4:
                    number |= (bytes[3] & 0xFF) << 24;
                case 3:
                    number |= (bytes[2] & 0xFF) << 16;
                case 2:
                    number |= (bytes[1] & 0xFF) << 8;
                case 1:
                    number |= (bytes[0] & 0xFF) << 0;
            }

            return UInteger.valueOf(number);
        }

        public static
        UInteger from(byte b0, byte b1, byte b2, byte b3) {
            int number = (b3 & 0xFF) << 24 |
                         (b2 & 0xFF) << 16 |
                         (b1 & 0xFF) << 8 |
                         (b0 & 0xFF) << 0;

            return UInteger.valueOf(number);
        }

        public static
        byte[] toBytes(UInteger x) {
            long num = x.longValue();

            return new byte[] {(byte) (num & 0x000000FFL >> 0), (byte) ((num & 0x0000FF00L) >> 8), (byte) ((num & 0x00FF0000L) >> 16),
                               (byte) ((num & 0xFF000000L) >> 24)};
        }

        public static
        UInteger from(ByteBuffer buff) {
            return from(buff.get(), buff.get(), buff.get(), buff.get());
        }

        public static
        UInteger from(InputStream inputStream) throws IOException {
            byte[] b = new byte[4];
            if (inputStream.read(b) != 4) {
                throw new EOFException();
            }

            return from(b[0], b[1], b[2], b[3]);
        }

        private
        UInt_() {
        }
    }


    /**
     * LONG to and from bytes
     */
    public static final
    class Long_ {
        @SuppressWarnings("fallthrough")
        public static
        long from(byte[] bytes, int offset, int bytenum) {
            long number = 0;

            switch (bytenum) {
                case 8:
                    number |= (long) (bytes[offset + 7] & 0xFF) << 56;
                case 7:
                    number |= (long) (bytes[offset + 6] & 0xFF) << 48;
                case 6:
                    number |= (long) (bytes[offset + 5] & 0xFF) << 40;
                case 5:
                    number |= (long) (bytes[offset + 4] & 0xFF) << 32;
                case 4:
                    number |= (long) (bytes[offset + 3] & 0xFF) << 24;
                case 3:
                    number |= (long) (bytes[offset + 2] & 0xFF) << 16;
                case 2:
                    number |= (long) (bytes[offset + 1] & 0xFF) << 8;
                case 1:
                    number |= (long) (bytes[offset + 0] & 0xFF) << 0;
            }

            return number;
        }

        @SuppressWarnings("fallthrough")
        public static
        long from(byte[] bytes) {
            long number = 0L;

            switch (bytes.length) {
                case 8:
                    number |= (long) (bytes[7] & 0xFF) << 56;
                case 7:
                    number |= (long) (bytes[6] & 0xFF) << 48;
                case 6:
                    number |= (long) (bytes[5] & 0xFF) << 40;
                case 5:
                    number |= (long) (bytes[4] & 0xFF) << 32;
                case 4:
                    number |= (long) (bytes[3] & 0xFF) << 24;
                case 3:
                    number |= (long) (bytes[2] & 0xFF) << 16;
                case 2:
                    number |= (long) (bytes[1] & 0xFF) << 8;
                case 1:
                    number |= (long) (bytes[0] & 0xFF) << 0;
            }

            return number;
        }

        public static
        long from(byte b0, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7) {
            return (long) (b7 & 0xFF) << 56 |
                   (long) (b6 & 0xFF) << 48 |
                   (long) (b5 & 0xFF) << 40 |
                   (long) (b4 & 0xFF) << 32 |
                   (long) (b3 & 0xFF) << 24 |
                   (long) (b2 & 0xFF) << 16 |
                   (long) (b1 & 0xFF) << 8 |
                   (long) (b0 & 0xFF) << 0;
        }

        public static
        byte[] toBytes(long x) {
            return new byte[] {(byte) (x >> 0), (byte) (x >> 8), (byte) (x >> 16), (byte) (x >> 24), (byte) (x >> 32), (byte) (x >> 40),
                               (byte) (x >> 48), (byte) (x >> 56),};
        }

        public static
        long from(ByteBuffer buff) {
            return from(buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get());
        }

        public static
        long from(InputStream inputStream) throws IOException {
            byte[] b = new byte[8];
            if (inputStream.read(b) != 8) {
                throw new EOFException();
            }

            return from(b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7]);
        }

        private
        Long_() {
        }
    }


    /**
     * UNSIGNED LONG to and from bytes
     */
    public static final
    class ULong_ {
        @SuppressWarnings("fallthrough")
        public static
        ULong from(byte[] bytes, int offset, int bytenum) {
            long number = 0;

            switch (bytenum) {
                case 8:
                    number |= (long) (bytes[offset + 7] & 0xFF) << 56;
                case 7:
                    number |= (long) (bytes[offset + 6] & 0xFF) << 48;
                case 6:
                    number |= (long) (bytes[offset + 5] & 0xFF) << 40;
                case 5:
                    number |= (long) (bytes[offset + 4] & 0xFF) << 32;
                case 4:
                    number |= (long) (bytes[offset + 3] & 0xFF) << 24;
                case 3:
                    number |= (long) (bytes[offset + 2] & 0xFF) << 16;
                case 2:
                    number |= (long) (bytes[offset + 1] & 0xFF) << 8;
                case 1:
                    number |= (long) (bytes[offset + 0] & 0xFF) << 0;
            }

            return ULong.valueOf(number);
        }

        public static
        ULong from(byte[] bytes) {
            BigInteger ulong = new BigInteger(1, bytes);
            return ULong.valueOf(ulong);
        }

        public static
        ULong from(byte b0, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7) {
            byte[] bytes = new byte[] {b7, b6, b5, b4, b3, b2, b1, b0};
            BigInteger ulong = new BigInteger(1, bytes);
            return ULong.valueOf(ulong);
        }

        public static
        byte[] toBytes(ULong x) {
            byte[] bytes = new byte[8];
            int offset = 0;

            byte temp_byte[] = x.toBigInteger()
                                .toByteArray();
            int array_count = temp_byte.length - 1;

            for (int i = 7; i >= 0; i--) {
                if (array_count >= 0) {
                    bytes[offset] = temp_byte[array_count];
                }
                else {
                    bytes[offset] = (byte) 00;
                }

                offset++;
                array_count--;
            }

            return bytes;
        }

        public static
        ULong from(ByteBuffer buff) {
            return from(buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get(), buff.get());
        }

        public static
        ULong from(InputStream inputStream) throws IOException {
            byte[] b = new byte[8];
            if (inputStream.read(b) != 8) {
                throw new EOFException();
            }

            return from(b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7]);
        }

        private
        ULong_() {
        }
    }
}

