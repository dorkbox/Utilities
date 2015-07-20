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

import io.netty.buffer.ByteBuf;
import org.bouncycastle.util.Pack;

public class Tables8kGCMMultiplier_ByteBuf
{
    private final int[][][] M = new int[32][16][];

    public void init(byte[] H)
    {
        this.M[0][0] = new int[4];
        this.M[1][0] = new int[4];
        this.M[1][8] = GCMUtil_ByteBuf.asInts(H);

        for (int j = 4; j >= 1; j >>= 1)
        {
            int[] tmp = new int[4];
            System.arraycopy(this.M[1][j + j], 0, tmp, 0, 4);

            GCMUtil_ByteBuf.multiplyP(tmp);
            this.M[1][j] = tmp;
        }

        {
            int[] tmp = new int[4];
            System.arraycopy(this.M[1][1], 0, tmp, 0, 4);

            GCMUtil_ByteBuf.multiplyP(tmp);
            this.M[0][8] = tmp;
        }

        for (int j = 4; j >= 1; j >>= 1)
        {
            int[] tmp = new int[4];
            System.arraycopy(this.M[0][j + j], 0, tmp, 0, 4);

            GCMUtil_ByteBuf.multiplyP(tmp);
            this.M[0][j] = tmp;
        }

        int i = 0;
        for (;;)
        {
            for (int j = 2; j < 16; j += j)
            {
                for (int k = 1; k < j; ++k)
                {
                    int[] tmp = new int[4];
                    System.arraycopy(this.M[i][j], 0, tmp, 0, 4);

                    GCMUtil_ByteBuf.xor(tmp, this.M[i][k]);
                    this.M[i][j + k] = tmp;
                }
            }

            if (++i == 32)
            {
                return;
            }

            if (i > 1)
            {
                this.M[i][0] = new int[4];
                for(int j = 8; j > 0; j >>= 1)
                {
                  int[] tmp = new int[4];
                  System.arraycopy(this.M[i - 2][j], 0, tmp, 0, 4);

                  GCMUtil_ByteBuf.multiplyP8(tmp);
                  this.M[i][j] = tmp;
                }
            }
        }
    }

    public void multiplyH(byte[] x)
    {
//      assert x.Length == 16;

        int[] z = new int[4];
        for (int i = 15; i >= 0; --i)
        {
//            GCMUtil.xor(z, M[i + i][x[i] & 0x0f]);
            int[] m = this.M[i + i][x[i] & 0x0f];
            z[0] ^= m[0];
            z[1] ^= m[1];
            z[2] ^= m[2];
            z[3] ^= m[3];
//            GCMUtil.xor(z, M[i + i + 1][(x[i] & 0xf0) >>> 4]);
            m = this.M[i + i + 1][(x[i] & 0xf0) >>> 4];
            z[0] ^= m[0];
            z[1] ^= m[1];
            z[2] ^= m[2];
            z[3] ^= m[3];
        }

        Pack.intToBigEndian(z, x, 0);
    }

    public void multiplyH(ByteBuf x)
    {
//      assert x.Length == 16;

        int[] z = new int[4];
        for (int i = 15; i >= 0; --i)
        {
//            GCMUtil.xor(z, M[i + i][x[i] & 0x0f]);
            int[] m = this.M[i + i][x.getByte(i) & 0x0f];
            z[0] ^= m[0];
            z[1] ^= m[1];
            z[2] ^= m[2];
            z[3] ^= m[3];
//            GCMUtil.xor(z, M[i + i + 1][(x[i] & 0xf0) >>> 4]);
            m = this.M[i + i + 1][(x.getByte(i) & 0xf0) >>> 4];
            z[0] ^= m[0];
            z[1] ^= m[1];
            z[2] ^= m[2];
            z[3] ^= m[3];
        }

        intToBigEndian(z, x, 0);
    }

    public static void intToBigEndian(int[] ns, ByteBuf bs, int off) {
        for (int i = 0; i < ns.length; ++i) {
            intToBigEndian(ns[i], bs, off);
            off += 4;
        }
    }

    public static void intToBigEndian(int n, ByteBuf bs, int off) {
        bs.setByte(off, (byte) (n >>> 24));
        bs.setByte(++off, (byte) (n >>> 16));
        bs.setByte(++off, (byte) (n >>> 8));
        bs.setByte(++off, (byte) n);
    }
}
