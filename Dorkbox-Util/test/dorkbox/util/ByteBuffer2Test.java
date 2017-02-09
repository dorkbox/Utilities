/*
 * Copyright 2015 dorkbox, llc
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

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import dorkbox.util.bytes.ByteBuffer2;

public class ByteBuffer2Test {


    static public void assertArrayEquals(Object object1, Object object2) {
        Assert.assertEquals(arrayToList(object1), arrayToList(object2));
    }

    static public Object arrayToList(Object array) {
        if (array == null || !array.getClass().isArray()) {
            return array;
        }
        ArrayList<Object> list = new ArrayList<Object>(Array.getLength(array));
        for (int i = 0, n = Array.getLength(array); i < n; i++) {
            list.add(arrayToList(Array.get(array, i)));
        }
        return list;
    }

    @Test
    public void testWriteBytes() {
        ByteBuffer2 buffer = new ByteBuffer2(512);
        buffer.writeBytes(new byte[] {11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26});
        buffer.writeBytes(new byte[] {31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46});
        buffer.writeByte(51);
        buffer.writeBytes(new byte[] {52,53,54,55,56,57,58});
        buffer.writeByte(61);
        buffer.writeByte(62);
        buffer.writeByte(63);
        buffer.writeByte(64);
        buffer.writeByte(65);

        assertArrayEquals(new byte[] {11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,31,32,33,34,35,36,37,38,39,40,41,42,
                43,44,45,46,51,52,53,54,55,56,57,58,61,62,63,64,65}, buffer.toBytes());
    }

    @Test
    public void testStrings() {
        runStringTest(new ByteBuffer2(4096));
        runStringTest(new ByteBuffer2(897));

        ByteBuffer2 write = new ByteBuffer2(21);
        String value = "abcdef\u00E1\u00E9\u00ED\u00F3\u00FA\u1234";
        write.writeString(value);
        ByteBuffer2 read = new ByteBuffer2(write.toBytes());
        assertArrayEquals(value, read.readString());

        runStringTest(127);
        runStringTest(256);
        runStringTest(1024 * 1023);
        runStringTest(1024 * 1024);
        runStringTest(1024 * 1025);
        runStringTest(1024 * 1026);
        runStringTest(1024 * 1024 * 2);
    }

    public void runStringTest(ByteBuffer2 write) {
        String value1 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ\rabcdefghijklmnopqrstuvwxyz\n1234567890\t\"!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*";
        String value2 = "abcdef\u00E1\u00E9\u00ED\u00F3\u00FA\u1234";

        write.writeString("");
        write.writeString("1");
        write.writeString("22");
        write.writeString("uno");
        write.writeString("dos");
        write.writeString("tres");
        write.writeString(null);
        write.writeString(value1);
        write.writeString(value2);
        for (int i = 0; i < 127; i++) {
            write.writeString(String.valueOf((char) i));
        }
        for (int i = 0; i < 127; i++) {
            write.writeString(String.valueOf((char) i) + "abc");
        }

        ByteBuffer2 read = new ByteBuffer2(write.toBytes());
        Assert.assertEquals("", read.readString());
         Assert.assertEquals("1", read.readString());
         Assert.assertEquals("22", read.readString());
         Assert.assertEquals("uno", read.readString());
         Assert.assertEquals("dos", read.readString());
         Assert.assertEquals("tres", read.readString());
         Assert.assertEquals(null, read.readString());
         Assert.assertEquals(value1, read.readString());
         Assert.assertEquals(value2, read.readString());
        for (int i = 0; i < 127; i++) {
             Assert.assertEquals(String.valueOf((char) i), read.readString());
        }
        for (int i = 0; i < 127; i++) {
             Assert.assertEquals(String.valueOf((char) i) + "abc", read.readString());
        }

        read.rewind();

         Assert.assertEquals("", read.readStringBuilder().toString());
         Assert.assertEquals("1", read.readStringBuilder().toString());
         Assert.assertEquals("22", read.readStringBuilder().toString());
         Assert.assertEquals("uno", read.readStringBuilder().toString());
         Assert.assertEquals("dos", read.readStringBuilder().toString());
         Assert.assertEquals("tres", read.readStringBuilder().toString());
         Assert.assertEquals(null, read.readStringBuilder());
         Assert.assertEquals(value1, read.readStringBuilder().toString());
         Assert.assertEquals(value2, read.readStringBuilder().toString());
        for (int i = 0; i < 127; i++) {
             Assert.assertEquals(String.valueOf((char) i), read.readStringBuilder().toString());
        }
        for (int i = 0; i < 127; i++) {
             Assert.assertEquals(String.valueOf((char) i) + "abc", read.readStringBuilder().toString());
        }
    }

    public void runStringTest(int length) {
        ByteBuffer2 write = new ByteBuffer2(1024, -1);

        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < length; i++) {
            buffer.append((char) i);
        }

        String value = buffer.toString();
        write.writeString(value);
        write.writeString(value);

        ByteBuffer2 read = new ByteBuffer2(write.toBytes());
        Assert.assertEquals(value, read.readString());
        Assert.assertEquals(value, read.readStringBuilder().toString());

        write.clear();
        write.writeString(buffer);
        write.writeString(buffer);
        read = new ByteBuffer2(write.toBytes());
        Assert.assertEquals(value, read.readStringBuilder().toString());
        Assert.assertEquals(value, read.readString());

        if (length <= 127) {
            write.clear();
            write.writeAscii(value);
            write.writeAscii(value);
            read = new ByteBuffer2(write.toBytes());
            Assert.assertEquals(value, read.readStringBuilder().toString());
            Assert.assertEquals(value, read.readString());
        }
    }

    @Test
    public void testCanReadInt() {
        ByteBuffer2 write = new ByteBuffer2();

        ByteBuffer2 read = new ByteBuffer2(write.toBytes());
        Assert.assertEquals(false, read.canReadInt());

        write = new ByteBuffer2(4);
        write.writeInt(400, true);

        read = new ByteBuffer2(write.toBytes());
        Assert.assertEquals(true, read.canReadInt());
        read.setPosition(read.capacity());
        Assert.assertEquals(false, read.canReadInt());
    }

    @Test
    public void testInts() {
        runIntTest(new ByteBuffer2(4096));
    }

    private void runIntTest(ByteBuffer2 write) {
        write.writeInt(0);
        write.writeInt(63);
        write.writeInt(64);
        write.writeInt(127);
        write.writeInt(128);
        write.writeInt(8192);
        write.writeInt(16384);
        write.writeInt(2097151);
        write.writeInt(1048575);
        write.writeInt(134217727);
        write.writeInt(268435455);
        write.writeInt(134217728);
        write.writeInt(268435456);
        write.writeInt(-2097151);
        write.writeInt(-1048575);
        write.writeInt(-134217727);
        write.writeInt(-268435455);
        write.writeInt(-134217728);
        write.writeInt(-268435456);
        Assert.assertEquals(1, write.writeInt(0, true));
        Assert.assertEquals(1, write.writeInt(0, false));
        Assert.assertEquals(1, write.writeInt(63, true));
        Assert.assertEquals(1, write.writeInt(63, false));
        Assert.assertEquals(1, write.writeInt(64, true));
        Assert.assertEquals(2, write.writeInt(64, false));
        Assert.assertEquals(1, write.writeInt(127, true));
        Assert.assertEquals(2, write.writeInt(127, false));
        Assert.assertEquals(2, write.writeInt(128, true));
        Assert.assertEquals(2, write.writeInt(128, false));
        Assert.assertEquals(2, write.writeInt(8191, true));
        Assert.assertEquals(2, write.writeInt(8191, false));
        Assert.assertEquals(2, write.writeInt(8192, true));
        Assert.assertEquals(3, write.writeInt(8192, false));
        Assert.assertEquals(2, write.writeInt(16383, true));
        Assert.assertEquals(3, write.writeInt(16383, false));
        Assert.assertEquals(3, write.writeInt(16384, true));
        Assert.assertEquals(3, write.writeInt(16384, false));
        Assert.assertEquals(3, write.writeInt(2097151, true));
        Assert.assertEquals(4, write.writeInt(2097151, false));
        Assert.assertEquals(3, write.writeInt(1048575, true));
        Assert.assertEquals(3, write.writeInt(1048575, false));
        Assert.assertEquals(4, write.writeInt(134217727, true));
        Assert.assertEquals(4, write.writeInt(134217727, false));
        Assert.assertEquals(4, write.writeInt(268435455, true));
        Assert.assertEquals(5, write.writeInt(268435455, false));
        Assert.assertEquals(4, write.writeInt(134217728, true));
        Assert.assertEquals(5, write.writeInt(134217728, false));
        Assert.assertEquals(5, write.writeInt(268435456, true));
        Assert.assertEquals(5, write.writeInt(268435456, false));
        Assert.assertEquals(1, write.writeInt(-64, false));
        Assert.assertEquals(5, write.writeInt(-64, true));
        Assert.assertEquals(2, write.writeInt(-65, false));
        Assert.assertEquals(5, write.writeInt(-65, true));
        Assert.assertEquals(2, write.writeInt(-8192, false));
        Assert.assertEquals(5, write.writeInt(-8192, true));
        Assert.assertEquals(3, write.writeInt(-1048576, false));
        Assert.assertEquals(5, write.writeInt(-1048576, true));
        Assert.assertEquals(4, write.writeInt(-134217728, false));
        Assert.assertEquals(5, write.writeInt(-134217728, true));
        Assert.assertEquals(5, write.writeInt(-134217729, false));
        Assert.assertEquals(5, write.writeInt(-134217729, true));

        ByteBuffer2 read = new ByteBuffer2(write.toBytes());
        Assert.assertEquals(0, read.readInt());
        Assert.assertEquals(63, read.readInt());
        Assert.assertEquals(64, read.readInt());
        Assert.assertEquals(127, read.readInt());
        Assert.assertEquals(128, read.readInt());
        Assert.assertEquals(8192, read.readInt());
        Assert.assertEquals(16384, read.readInt());
        Assert.assertEquals(2097151, read.readInt());
        Assert.assertEquals(1048575, read.readInt());
        Assert.assertEquals(134217727, read.readInt());
        Assert.assertEquals(268435455, read.readInt());
        Assert.assertEquals(134217728, read.readInt());
        Assert.assertEquals(268435456, read.readInt());
        Assert.assertEquals(-2097151, read.readInt());
        Assert.assertEquals(-1048575, read.readInt());
        Assert.assertEquals(-134217727, read.readInt());
        Assert.assertEquals(-268435455, read.readInt());
        Assert.assertEquals(-134217728, read.readInt());
        Assert.assertEquals(-268435456, read.readInt());
        Assert.assertEquals(true, read.canReadInt());
        Assert.assertEquals(true, read.canReadInt());
        Assert.assertEquals(true, read.canReadInt());
        Assert.assertEquals(0, read.readInt(true));
        Assert.assertEquals(0, read.readInt(false));
        Assert.assertEquals(63, read.readInt(true));
        Assert.assertEquals(63, read.readInt(false));
        Assert.assertEquals(64, read.readInt(true));
        Assert.assertEquals(64, read.readInt(false));
        Assert.assertEquals(127, read.readInt(true));
        Assert.assertEquals(127, read.readInt(false));
        Assert.assertEquals(128, read.readInt(true));
        Assert.assertEquals(128, read.readInt(false));
        Assert.assertEquals(8191, read.readInt(true));
        Assert.assertEquals(8191, read.readInt(false));
        Assert.assertEquals(8192, read.readInt(true));
        Assert.assertEquals(8192, read.readInt(false));
        Assert.assertEquals(16383, read.readInt(true));
        Assert.assertEquals(16383, read.readInt(false));
        Assert.assertEquals(16384, read.readInt(true));
        Assert.assertEquals(16384, read.readInt(false));
        Assert.assertEquals(2097151, read.readInt(true));
        Assert.assertEquals(2097151, read.readInt(false));
        Assert.assertEquals(1048575, read.readInt(true));
        Assert.assertEquals(1048575, read.readInt(false));
        Assert.assertEquals(134217727, read.readInt(true));
        Assert.assertEquals(134217727, read.readInt(false));
        Assert.assertEquals(268435455, read.readInt(true));
        Assert.assertEquals(268435455, read.readInt(false));
        Assert.assertEquals(134217728, read.readInt(true));
        Assert.assertEquals(134217728, read.readInt(false));
        Assert.assertEquals(268435456, read.readInt(true));
        Assert.assertEquals(268435456, read.readInt(false));
        Assert.assertEquals(-64, read.readInt(false));
        Assert.assertEquals(-64, read.readInt(true));
        Assert.assertEquals(-65, read.readInt(false));
        Assert.assertEquals(-65, read.readInt(true));
        Assert.assertEquals(-8192, read.readInt(false));
        Assert.assertEquals(-8192, read.readInt(true));
        Assert.assertEquals(-1048576, read.readInt(false));
        Assert.assertEquals(-1048576, read.readInt(true));
        Assert.assertEquals(-134217728, read.readInt(false));
        Assert.assertEquals(-134217728, read.readInt(true));
        Assert.assertEquals(-134217729, read.readInt(false));
        Assert.assertEquals(-134217729, read.readInt(true));
        Assert.assertEquals(false, read.canReadInt());

        Random random = new Random();
        for (int i = 0; i < 10000; i++) {
            int value = random.nextInt();
            write.clear();
            write.writeInt(value);
            write.writeInt(value, true);
            write.writeInt(value, false);
            read.setBuffer(write.toBytes());
            Assert.assertEquals(value, read.readInt());
            Assert.assertEquals(value, read.readInt(true));
            Assert.assertEquals(value, read.readInt(false));
        }
    }

    @Test
    public void testLongs() {
        runLongTest(new ByteBuffer2(4096));
    }

    private void runLongTest(ByteBuffer2 write) {
        write.writeLong(0);
        write.writeLong(63);
        write.writeLong(64);
        write.writeLong(127);
        write.writeLong(128);
        write.writeLong(8192);
        write.writeLong(16384);
        write.writeLong(2097151);
        write.writeLong(1048575);
        write.writeLong(134217727);
        write.writeLong(268435455);
        write.writeLong(134217728);
        write.writeLong(268435456);
        write.writeLong(-2097151);
        write.writeLong(-1048575);
        write.writeLong(-134217727);
        write.writeLong(-268435455);
        write.writeLong(-134217728);
        write.writeLong(-268435456);
        Assert.assertEquals(1, write.writeLong(0, true));
        Assert.assertEquals(1, write.writeLong(0, false));
        Assert.assertEquals(1, write.writeLong(63, true));
        Assert.assertEquals(1, write.writeLong(63, false));
        Assert.assertEquals(1, write.writeLong(64, true));
        Assert.assertEquals(2, write.writeLong(64, false));
        Assert.assertEquals(1, write.writeLong(127, true));
        Assert.assertEquals(2, write.writeLong(127, false));
        Assert.assertEquals(2, write.writeLong(128, true));
        Assert.assertEquals(2, write.writeLong(128, false));
        Assert.assertEquals(2, write.writeLong(8191, true));
        Assert.assertEquals(2, write.writeLong(8191, false));
        Assert.assertEquals(2, write.writeLong(8192, true));
        Assert.assertEquals(3, write.writeLong(8192, false));
        Assert.assertEquals(2, write.writeLong(16383, true));
        Assert.assertEquals(3, write.writeLong(16383, false));
        Assert.assertEquals(3, write.writeLong(16384, true));
        Assert.assertEquals(3, write.writeLong(16384, false));
        Assert.assertEquals(3, write.writeLong(2097151, true));
        Assert.assertEquals(4, write.writeLong(2097151, false));
        Assert.assertEquals(3, write.writeLong(1048575, true));
        Assert.assertEquals(3, write.writeLong(1048575, false));
        Assert.assertEquals(4, write.writeLong(134217727, true));
        Assert.assertEquals(4, write.writeLong(134217727, false));
        Assert.assertEquals(4, write.writeLong(268435455l, true));
        Assert.assertEquals(5, write.writeLong(268435455l, false));
        Assert.assertEquals(4, write.writeLong(134217728l, true));
        Assert.assertEquals(5, write.writeLong(134217728l, false));
        Assert.assertEquals(5, write.writeLong(268435456l, true));
        Assert.assertEquals(5, write.writeLong(268435456l, false));
        Assert.assertEquals(1, write.writeLong(-64, false));
        Assert.assertEquals(9, write.writeLong(-64, true));
        Assert.assertEquals(2, write.writeLong(-65, false));
        Assert.assertEquals(9, write.writeLong(-65, true));
        Assert.assertEquals(2, write.writeLong(-8192, false));
        Assert.assertEquals(9, write.writeLong(-8192, true));
        Assert.assertEquals(3, write.writeLong(-1048576, false));
        Assert.assertEquals(9, write.writeLong(-1048576, true));
        Assert.assertEquals(4, write.writeLong(-134217728, false));
        Assert.assertEquals(9, write.writeLong(-134217728, true));
        Assert.assertEquals(5, write.writeLong(-134217729, false));
        Assert.assertEquals(9, write.writeLong(-134217729, true));

        ByteBuffer2 read = new ByteBuffer2(write.toBytes());
        Assert.assertEquals(0, read.readLong());
        Assert.assertEquals(63, read.readLong());
        Assert.assertEquals(64, read.readLong());
        Assert.assertEquals(127, read.readLong());
        Assert.assertEquals(128, read.readLong());
        Assert.assertEquals(8192, read.readLong());
        Assert.assertEquals(16384, read.readLong());
        Assert.assertEquals(2097151, read.readLong());
        Assert.assertEquals(1048575, read.readLong());
        Assert.assertEquals(134217727, read.readLong());
        Assert.assertEquals(268435455, read.readLong());
        Assert.assertEquals(134217728, read.readLong());
        Assert.assertEquals(268435456, read.readLong());
        Assert.assertEquals(-2097151, read.readLong());
        Assert.assertEquals(-1048575, read.readLong());
        Assert.assertEquals(-134217727, read.readLong());
        Assert.assertEquals(-268435455, read.readLong());
        Assert.assertEquals(-134217728, read.readLong());
        Assert.assertEquals(-268435456, read.readLong());
        Assert.assertEquals(0, read.readLong(true));
        Assert.assertEquals(0, read.readLong(false));
        Assert.assertEquals(63, read.readLong(true));
        Assert.assertEquals(63, read.readLong(false));
        Assert.assertEquals(64, read.readLong(true));
        Assert.assertEquals(64, read.readLong(false));
        Assert.assertEquals(127, read.readLong(true));
        Assert.assertEquals(127, read.readLong(false));
        Assert.assertEquals(128, read.readLong(true));
        Assert.assertEquals(128, read.readLong(false));
        Assert.assertEquals(8191, read.readLong(true));
        Assert.assertEquals(8191, read.readLong(false));
        Assert.assertEquals(8192, read.readLong(true));
        Assert.assertEquals(8192, read.readLong(false));
        Assert.assertEquals(16383, read.readLong(true));
        Assert.assertEquals(16383, read.readLong(false));
        Assert.assertEquals(16384, read.readLong(true));
        Assert.assertEquals(16384, read.readLong(false));
        Assert.assertEquals(2097151, read.readLong(true));
        Assert.assertEquals(2097151, read.readLong(false));
        Assert.assertEquals(1048575, read.readLong(true));
        Assert.assertEquals(1048575, read.readLong(false));
        Assert.assertEquals(134217727, read.readLong(true));
        Assert.assertEquals(134217727, read.readLong(false));
        Assert.assertEquals(268435455, read.readLong(true));
        Assert.assertEquals(268435455, read.readLong(false));
        Assert.assertEquals(134217728, read.readLong(true));
        Assert.assertEquals(134217728, read.readLong(false));
        Assert.assertEquals(268435456, read.readLong(true));
        Assert.assertEquals(268435456, read.readLong(false));
        Assert.assertEquals(-64, read.readLong(false));
        Assert.assertEquals(-64, read.readLong(true));
        Assert.assertEquals(-65, read.readLong(false));
        Assert.assertEquals(-65, read.readLong(true));
        Assert.assertEquals(-8192, read.readLong(false));
        Assert.assertEquals(-8192, read.readLong(true));
        Assert.assertEquals(-1048576, read.readLong(false));
        Assert.assertEquals(-1048576, read.readLong(true));
        Assert.assertEquals(-134217728, read.readLong(false));
        Assert.assertEquals(-134217728, read.readLong(true));
        Assert.assertEquals(-134217729, read.readLong(false));
        Assert.assertEquals(-134217729, read.readLong(true));

        Random random = new Random();
        for (int i = 0; i < 10000; i++) {
            long value = random.nextLong();
            write.clear();
            write.writeLong(value);
            write.writeLong(value, true);
            write.writeLong(value, false);
            read.setBuffer(write.toBytes());
            Assert.assertEquals(value, read.readLong());
            Assert.assertEquals(value, read.readLong(true));
            Assert.assertEquals(value, read.readLong(false));
        }
    }

    @Test
    public void testShorts() {
        runShortTest(new ByteBuffer2(4096));
    }

    private void runShortTest(ByteBuffer2 write) {
        write.writeShort(0);
        write.writeShort(63);
        write.writeShort(64);
        write.writeShort(127);
        write.writeShort(128);
        write.writeShort(8192);
        write.writeShort(16384);
        write.writeShort(32767);
        write.writeShort(-63);
        write.writeShort(-64);
        write.writeShort(-127);
        write.writeShort(-128);
        write.writeShort(-8192);
        write.writeShort(-16384);
        write.writeShort(-32768);

        ByteBuffer2 read = new ByteBuffer2(write.toBytes());
        Assert.assertEquals(0, read.readShort());
        Assert.assertEquals(63, read.readShort());
        Assert.assertEquals(64, read.readShort());
        Assert.assertEquals(127, read.readShort());
        Assert.assertEquals(128, read.readShort());
        Assert.assertEquals(8192, read.readShort());
        Assert.assertEquals(16384, read.readShort());
        Assert.assertEquals(32767, read.readShort());
        Assert.assertEquals(-63, read.readShort());
        Assert.assertEquals(-64, read.readShort());
        Assert.assertEquals(-127, read.readShort());
        Assert.assertEquals(-128, read.readShort());
        Assert.assertEquals(-8192, read.readShort());
        Assert.assertEquals(-16384, read.readShort());
        Assert.assertEquals(-32768, read.readShort());
    }

    @Test
    public void testFloats() {
        runFloatTest(new ByteBuffer2(4096));
    }

    private void runFloatTest(ByteBuffer2 write) {
        write.writeFloat(0);
        write.writeFloat(63);
        write.writeFloat(64);
        write.writeFloat(127);
        write.writeFloat(128);
        write.writeFloat(8192);
        write.writeFloat(16384);
        write.writeFloat(32767);
        write.writeFloat(-63);
        write.writeFloat(-64);
        write.writeFloat(-127);
        write.writeFloat(-128);
        write.writeFloat(-8192);
        write.writeFloat(-16384);
        write.writeFloat(-32768);
        Assert.assertEquals(1, write.writeFloat(0, 1000, true));
        Assert.assertEquals(1, write.writeFloat(0, 1000, false));
        Assert.assertEquals(3, write.writeFloat(63, 1000, true));
        Assert.assertEquals(3, write.writeFloat(63, 1000, false));
        Assert.assertEquals(3, write.writeFloat(64, 1000, true));
        Assert.assertEquals(3, write.writeFloat(64, 1000, false));
        Assert.assertEquals(3, write.writeFloat(127, 1000, true));
        Assert.assertEquals(3, write.writeFloat(127, 1000, false));
        Assert.assertEquals(3, write.writeFloat(128, 1000, true));
        Assert.assertEquals(3, write.writeFloat(128, 1000, false));
        Assert.assertEquals(4, write.writeFloat(8191, 1000, true));
        Assert.assertEquals(4, write.writeFloat(8191, 1000, false));
        Assert.assertEquals(4, write.writeFloat(8192, 1000, true));
        Assert.assertEquals(4, write.writeFloat(8192, 1000, false));
        Assert.assertEquals(4, write.writeFloat(16383, 1000, true));
        Assert.assertEquals(4, write.writeFloat(16383, 1000, false));
        Assert.assertEquals(4, write.writeFloat(16384, 1000, true));
        Assert.assertEquals(4, write.writeFloat(16384, 1000, false));
        Assert.assertEquals(4, write.writeFloat(32767, 1000, true));
        Assert.assertEquals(4, write.writeFloat(32767, 1000, false));
        Assert.assertEquals(3, write.writeFloat(-64, 1000, false));
        Assert.assertEquals(5, write.writeFloat(-64, 1000, true));
        Assert.assertEquals(3, write.writeFloat(-65, 1000, false));
        Assert.assertEquals(5, write.writeFloat(-65, 1000, true));
        Assert.assertEquals(4, write.writeFloat(-8192, 1000, false));
        Assert.assertEquals(5, write.writeFloat(-8192, 1000, true));

        float delta = 0.00000001f;
        ByteBuffer2 read = new ByteBuffer2(write.toBytes());
        Assert.assertEquals(read.readFloat(), 0f, delta);
        Assert.assertEquals(read.readFloat(), 63f, delta);
        Assert.assertEquals(read.readFloat(), 64f, delta);
        Assert.assertEquals(read.readFloat(), 127f, delta);
        Assert.assertEquals(read.readFloat(), 128f, delta);
        Assert.assertEquals(read.readFloat(), 8192f, delta);
        Assert.assertEquals(read.readFloat(), 16384f, delta);
        Assert.assertEquals(read.readFloat(), 32767f, delta);
        Assert.assertEquals(read.readFloat(), -63f, delta);
        Assert.assertEquals(read.readFloat(), -64f, delta);
        Assert.assertEquals(read.readFloat(), -127f, delta);
        Assert.assertEquals(read.readFloat(), -128f, delta);
        Assert.assertEquals(read.readFloat(), -8192f, delta);
        Assert.assertEquals(read.readFloat(), -16384f, delta);
        Assert.assertEquals(read.readFloat(), -32768f, delta);
        Assert.assertEquals(read.readFloat(1000, true), 0f, delta);
        Assert.assertEquals(read.readFloat(1000, false), 0f, delta);
        Assert.assertEquals(read.readFloat(1000, true), 63f, delta);
        Assert.assertEquals(read.readFloat(1000, false), 63f, delta);
        Assert.assertEquals(read.readFloat(1000, true), 64f, delta);
        Assert.assertEquals(read.readFloat(1000, false), 64f, delta);
        Assert.assertEquals(read.readFloat(1000, true), 127f, delta);
        Assert.assertEquals(read.readFloat(1000, false), 127f, delta);
        Assert.assertEquals(read.readFloat(1000, true), 128f, delta);
        Assert.assertEquals(read.readFloat(1000, false), 128f, delta);
        Assert.assertEquals(read.readFloat(1000, true), 8191f, delta);
        Assert.assertEquals(read.readFloat(1000, false), 8191f, delta);
        Assert.assertEquals(read.readFloat(1000, true), 8192f, delta);
        Assert.assertEquals(read.readFloat(1000, false), 8192f, delta);
        Assert.assertEquals(read.readFloat(1000, true), 16383f, delta);
        Assert.assertEquals(read.readFloat(1000, false), 16383f, delta);
        Assert.assertEquals(read.readFloat(1000, true), 16384f, delta);
        Assert.assertEquals(read.readFloat(1000, false), 16384f, delta);
        Assert.assertEquals(read.readFloat(1000, true), 32767f, delta);
        Assert.assertEquals(read.readFloat(1000, false), 32767f, delta);
        Assert.assertEquals(read.readFloat(1000, false), -64f, delta);
        Assert.assertEquals(read.readFloat(1000, true), -64f, delta);
        Assert.assertEquals(read.readFloat(1000, false), -65f, delta);
        Assert.assertEquals(read.readFloat(1000, true), -65f, delta);
        Assert.assertEquals(read.readFloat(1000, false), -8192f, delta);
        Assert.assertEquals(read.readFloat(1000, true), -8192f, delta);
    }

    @Test
    public void testDoubles()   {
        runDoubleTest(new ByteBuffer2(4096));
    }

    private void runDoubleTest(ByteBuffer2 write)  {
        write.writeDouble(0);
        write.writeDouble(63);
        write.writeDouble(64);
        write.writeDouble(127);
        write.writeDouble(128);
        write.writeDouble(8192);
        write.writeDouble(16384);
        write.writeDouble(32767);
        write.writeDouble(-63);
        write.writeDouble(-64);
        write.writeDouble(-127);
        write.writeDouble(-128);
        write.writeDouble(-8192);
        write.writeDouble(-16384);
        write.writeDouble(-32768);
        Assert.assertEquals(1, write.writeDouble(0, 1000, true));
        Assert.assertEquals(1, write.writeDouble(0, 1000, false));
        Assert.assertEquals(3, write.writeDouble(63, 1000, true));
        Assert.assertEquals(3, write.writeDouble(63, 1000, false));
        Assert.assertEquals(3, write.writeDouble(64, 1000, true));
        Assert.assertEquals(3, write.writeDouble(64, 1000, false));
        Assert.assertEquals(3, write.writeDouble(127, 1000, true));
        Assert.assertEquals(3, write.writeDouble(127, 1000, false));
        Assert.assertEquals(3, write.writeDouble(128, 1000, true));
        Assert.assertEquals(3, write.writeDouble(128, 1000, false));
        Assert.assertEquals(4, write.writeDouble(8191, 1000, true));
        Assert.assertEquals(4, write.writeDouble(8191, 1000, false));
        Assert.assertEquals(4, write.writeDouble(8192, 1000, true));
        Assert.assertEquals(4, write.writeDouble(8192, 1000, false));
        Assert.assertEquals(4, write.writeDouble(16383, 1000, true));
        Assert.assertEquals(4, write.writeDouble(16383, 1000, false));
        Assert.assertEquals(4, write.writeDouble(16384, 1000, true));
        Assert.assertEquals(4, write.writeDouble(16384, 1000, false));
        Assert.assertEquals(4, write.writeDouble(32767, 1000, true));
        Assert.assertEquals(4, write.writeDouble(32767, 1000, false));
        Assert.assertEquals(3, write.writeDouble(-64, 1000, false));
        Assert.assertEquals(9, write.writeDouble(-64, 1000, true));
        Assert.assertEquals(3, write.writeDouble(-65, 1000, false));
        Assert.assertEquals(9, write.writeDouble(-65, 1000, true));
        Assert.assertEquals(4, write.writeDouble(-8192, 1000, false));
        Assert.assertEquals(9, write.writeDouble(-8192, 1000, true));
        write.writeDouble(1.23456d);

        double delta = 0.00000001D;
        ByteBuffer2 read = new ByteBuffer2(write.toBytes());
        Assert.assertEquals(read.readDouble(), 0d, delta);
        Assert.assertEquals(read.readDouble(), 63d, delta);
        Assert.assertEquals(read.readDouble(), 64d, delta);
        Assert.assertEquals(read.readDouble(), 127d, delta);
        Assert.assertEquals(read.readDouble(), 128d, delta);
        Assert.assertEquals(read.readDouble(), 8192d, delta);
        Assert.assertEquals(read.readDouble(), 16384d, delta);
        Assert.assertEquals(read.readDouble(), 32767d, delta);
        Assert.assertEquals(read.readDouble(), -63d, delta);
        Assert.assertEquals(read.readDouble(), -64d, delta);
        Assert.assertEquals(read.readDouble(), -127d, delta);
        Assert.assertEquals(read.readDouble(), -128d, delta);
        Assert.assertEquals(read.readDouble(), -8192d, delta);
        Assert.assertEquals(read.readDouble(), -16384d, delta);
        Assert.assertEquals(read.readDouble(), -32768d, delta);
        Assert.assertEquals(read.readDouble(1000, true), 0d, delta);
        Assert.assertEquals(read.readDouble(1000, false), 0d, delta);
        Assert.assertEquals(read.readDouble(1000, true), 63d, delta);
        Assert.assertEquals(read.readDouble(1000, false), 63d, delta);
        Assert.assertEquals(read.readDouble(1000, true), 64d, delta);
        Assert.assertEquals(read.readDouble(1000, false), 64d, delta);
        Assert.assertEquals(read.readDouble(1000, true), 127d, delta);
        Assert.assertEquals(read.readDouble(1000, false), 127d, delta);
        Assert.assertEquals(read.readDouble(1000, true), 128d, delta);
        Assert.assertEquals(read.readDouble(1000, false), 128d, delta);
        Assert.assertEquals(read.readDouble(1000, true), 8191d, delta);
        Assert.assertEquals(read.readDouble(1000, false), 8191d, delta);
        Assert.assertEquals(read.readDouble(1000, true), 8192d, delta);
        Assert.assertEquals(read.readDouble(1000, false), 8192d, delta);
        Assert.assertEquals(read.readDouble(1000, true), 16383d, delta);
        Assert.assertEquals(read.readDouble(1000, false), 16383d, delta);
        Assert.assertEquals(read.readDouble(1000, true), 16384d, delta);
        Assert.assertEquals(read.readDouble(1000, false), 16384d, delta);
        Assert.assertEquals(read.readDouble(1000, true), 32767d, delta);
        Assert.assertEquals(read.readDouble(1000, false), 32767d, delta);
        Assert.assertEquals(read.readDouble(1000, false), -64d, delta);
        Assert.assertEquals(read.readDouble(1000, true), -64d, delta);
        Assert.assertEquals(read.readDouble(1000, false), -65d, delta);
        Assert.assertEquals(read.readDouble(1000, true), -65d, delta);
        Assert.assertEquals(read.readDouble(1000, false), -8192d, delta);
        Assert.assertEquals(read.readDouble(1000, true), -8192d, delta);
        Assert.assertEquals(1.23456d, read.readDouble(), delta);
    }

    @Test
    public void testBooleans() {
        runBooleanTest(new ByteBuffer2(4096));
    }

    private void runBooleanTest(ByteBuffer2 write) {
        for (int i = 0; i < 100; i++) {
            write.writeBoolean(true);
            write.writeBoolean(false);
        }

        ByteBuffer2 read = new ByteBuffer2(write.toBytes());
        for (int i = 0; i < 100; i++) {
            Assert.assertEquals(true, read.readBoolean());
            Assert.assertEquals(false, read.readBoolean());
        }
    }

    @Test
    public void testChars() {
        runCharTest(new ByteBuffer2(4096));
    }

    private void runCharTest(ByteBuffer2 write) {
        write.writeChar((char) 0);
        write.writeChar((char) 63);
        write.writeChar((char) 64);
        write.writeChar((char) 127);
        write.writeChar((char) 128);
        write.writeChar((char) 8192);
        write.writeChar((char) 16384);
        write.writeChar((char) 32767);
        write.writeChar((char) 65535);

        ByteBuffer2 read = new ByteBuffer2(write.toBytes());
        Assert.assertEquals(0, read.readChar());
        Assert.assertEquals(63, read.readChar());
        Assert.assertEquals(64, read.readChar());
        Assert.assertEquals(127, read.readChar());
        Assert.assertEquals(128, read.readChar());
        Assert.assertEquals(8192, read.readChar());
        Assert.assertEquals(16384, read.readChar());
        Assert.assertEquals(32767, read.readChar());
        Assert.assertEquals(65535, read.readChar());
    }

    @Test
    public void testInputWithOffset() throws Exception {
        final byte[] buf = new byte[30];
        final ByteBuffer2 in = new ByteBuffer2(buf);
        in.skip(20);
        Assert.assertEquals(10, in.remaining());
    }

    @Test
    public void testSmallBuffers() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(1024);

        ByteBuffer2 testOutput = new ByteBuffer2(buf.array());
        testOutput.writeBytes(new byte[512]);
        testOutput.writeBytes(new byte[512]);

        ByteBuffer2 testInputs = new ByteBuffer2();
        buf.flip();

        testInputs.setBuffer(buf.array());
        byte[] toRead = new byte[512];
        testInputs.readBytes(toRead);

        testInputs.readBytes(toRead);
    }
}
