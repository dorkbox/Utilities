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
