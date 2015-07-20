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
 *
 * Copyright (c) 2000 - 2015 The Legion of the Bouncy Castle Inc.
 * (http://www.bouncycastle.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package dorkbox.util.crypto.bouncycastle;

import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Pack;

public abstract class GCMUtil_ByteBuf
{
    static byte[] oneAsBytes()
    {
        byte[] tmp = new byte[16];
        tmp[0] = (byte)0x80;
        return tmp;
    }

    static int[] oneAsInts()
    {
        int[] tmp = new int[4];
        tmp[0] = 0x80000000;
        return tmp;
    }

    static int[] asInts(byte[] bs)
    {
        int[] us = new int[4];
        us[0] = Pack.bigEndianToInt(bs, 0);
        us[1] = Pack.bigEndianToInt(bs, 4);
        us[2] = Pack.bigEndianToInt(bs, 8);
        us[3] = Pack.bigEndianToInt(bs, 12);
        return us;
    }

    static void multiply(byte[] block, byte[] val)
    {
        byte[] tmp = Arrays.clone(block);
        byte[] c = new byte[16];

        for (int i = 0; i < 16; ++i)
        {
            byte bits = val[i];
            for (int j = 7; j >= 0; --j)
            {
                if ((bits & 1 << j) != 0)
                {
                    xor(c, tmp);
                }

                boolean lsb = (tmp[15] & 1) != 0;
                shiftRight(tmp);
                if (lsb)
                {
                    // R = new byte[]{ 0xe1, ... };
//                    GCMUtil.xor(v, R);
                    tmp[0] ^= (byte)0xe1;
                }
            }
        }

        System.arraycopy(c, 0, block, 0, 16);
    }

    // P is the value with only bit i=1 set
    static void multiplyP(int[] x)
    {
        boolean lsb = (x[3] & 1) != 0;
        shiftRight(x);
        if (lsb)
        {
            // R = new int[]{ 0xe1000000, 0, 0, 0 };
//            xor(v, R);
            x[0] ^= 0xe1000000;
        }
    }

    static void multiplyP8(int[] x)
    {
//        for (int i = 8; i != 0; --i)
//        {
//            multiplyP(x);
//        }

        int lsw = x[3];
        shiftRightN(x, 8);
        for (int i = 7; i >= 0; --i)
        {
            if ((lsw & 1 << i) != 0)
            {
                x[0] ^= 0xe1000000 >>> 7 - i;
            }
        }
    }

    static void shiftRight(byte[] block)
    {
        int i = 0;
        int bit = 0;
        for (;;)
        {
            int b = block[i] & 0xff;
            block[i] = (byte) (b >>> 1 | bit);
            if (++i == 16)
            {
                break;
            }
            bit = (b & 1) << 7;
        }
    }

    static void shiftRight(int[] block)
    {
        int i = 0;
        int bit = 0;
        for (;;)
        {
            int b = block[i];
            block[i] = b >>> 1 | bit;
            if (++i == 4)
            {
                break;
            }
            bit = b << 31;
        }
    }

    static void shiftRightN(int[] block, int n)
    {
        int i = 0;
        int bits = 0;
        for (;;)
        {
            int b = block[i];
            block[i] = b >>> n | bits;
            if (++i == 4)
            {
                break;
            }
            bits = b << 32 - n;
        }
    }

    static void xor(byte[] block, byte[] val)
    {
        for (int i = 15; i >= 0; --i)
        {
            block[i] ^= val[i];
        }
    }

    static void xor(int[] block, int[] val)
    {
        for (int i = 3; i >= 0; --i)
        {
            block[i] ^= val[i];
        }
    }
}
