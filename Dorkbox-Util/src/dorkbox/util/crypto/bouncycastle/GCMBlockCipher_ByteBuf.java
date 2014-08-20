package dorkbox.util.crypto.bouncycastle;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Pack;

/**
 * Implements the Galois/Counter mode (GCM) detailed in
 * NIST Special Publication 800-38D.
 */
public class GCMBlockCipher_ByteBuf
    implements AEADBlockCipher
{
    private static final int BLOCK_SIZE = 16;
    private static final byte[] ZEROES = new byte[BLOCK_SIZE];

    // not final due to a compiler bug
    private BlockCipher   cipher;
    private Tables8kGCMMultiplier_ByteBuf multiplier;

    // These fields are set by init and not modified by processing
    private boolean             forEncryption;
    private int                 macSize;
    private byte[]              nonce;
    private byte[]              A;
    private byte[]              H;
    private ByteBuf             initS;
    private byte[]              J0;

    // These fields are modified during processing
    private byte[]      bufBlock;
    private byte[]      macBlock;
    private ByteBuf     S;
    private byte[]      counter;
    private int         bufOff;
    private long        totalLength;

    public GCMBlockCipher_ByteBuf(BlockCipher c)
    {
        if (c.getBlockSize() != BLOCK_SIZE)
        {
            throw new IllegalArgumentException(
                "cipher required with a block size of " + BLOCK_SIZE + ".");
        }

        this.cipher = c;
        this.multiplier = new Tables8kGCMMultiplier_ByteBuf();
    }

    @Override
    public BlockCipher getUnderlyingCipher()
    {
        return this.cipher;
    }

    @Override
    public String getAlgorithmName()
    {
        return this.cipher.getAlgorithmName() + "/GCM";
    }

    @Override
    public void init(boolean forEncryption, CipherParameters params)
        throws IllegalArgumentException
    {
        this.forEncryption = forEncryption;
        this.macBlock = null;

        KeyParameter        keyParam;

        if (params instanceof AEADParameters)
        {
            AEADParameters param = (AEADParameters)params;

            this.nonce = param.getNonce();
            this.A = param.getAssociatedText();

            int macSizeBits = param.getMacSize();
            if (macSizeBits < 96 || macSizeBits > 128 || macSizeBits % 8 != 0)
            {
                throw new IllegalArgumentException("Invalid value for MAC size: " + macSizeBits);
            }

            this.macSize = macSizeBits / 8;
            keyParam = param.getKey();
        }
        else if (params instanceof ParametersWithIV)
        {
            ParametersWithIV param = (ParametersWithIV)params;

            this.nonce = param.getIV();
            this.A = null;
            this.macSize = 16;
            keyParam = (KeyParameter)param.getParameters();
        }
        else
        {
            throw new IllegalArgumentException("invalid parameters passed to GCM");
        }

        int bufLength = forEncryption ? BLOCK_SIZE : BLOCK_SIZE + this.macSize;
        this.bufBlock = new byte[bufLength];

        if (this.nonce == null || this.nonce.length < 1)
        {
            throw new IllegalArgumentException("IV must be at least 1 byte");
        }

        if (this.A == null)
        {
            // Avoid lots of null checks
            this.A = new byte[0];
        }

        // Cipher always used in forward mode
        // if keyParam is null we're reusing the last key.
        if (keyParam != null)
        {
            this.cipher.init(true, keyParam);
        }

        // TODO This should be configurable by init parameters
        // (but must be 16 if nonce length not 12) (BLOCK_SIZE?)
//        this.tagLength = 16;

        this.H = new byte[BLOCK_SIZE];
        this.cipher.processBlock(ZEROES, 0, this.H, 0);
        this.multiplier.init(this.H);

        this.initS = Unpooled.wrappedBuffer(gHASH(this.A));

        if (this.nonce.length == 12)
        {
            this.J0 = new byte[16];
            System.arraycopy(this.nonce, 0, this.J0, 0, this.nonce.length);
            this.J0[15] = 0x01;
        }
        else
        {
            this.J0 = gHASH(this.nonce);
            byte[] X = new byte[16];
            packLength((long)this.nonce.length * 8, X, 8);
            xor(this.J0, X);
            this.multiplier.multiplyH(this.J0);
        }

        this.S = this.initS.copy();
        this.counter = Arrays.clone(this.J0);
        this.bufOff = 0;
        this.totalLength = 0;
    }

    @Override
    public byte[] getMac() {
        return Arrays.clone(this.macBlock);
    }

    @Override
    public int getOutputSize(int len) {
        if (this.forEncryption) {
            return len + this.bufOff + this.macSize;
        }

        return len + this.bufOff - this.macSize;
    }

    @Override
    public int getUpdateOutputSize(int len) {
        return (len + this.bufOff) / BLOCK_SIZE * BLOCK_SIZE;
    }

    // MODIFIED
    @Override
    public int processByte(byte in, byte[] out, int outOff)
        throws DataLengthException
    {
        // DO NOTHING
        return 0;
    }

 // MODIFIED
    public int processBytes(ByteBuf in, ByteBuf out, int len)
        throws DataLengthException
    {
        out.clear();

        int didRead = 0;
        int resultLen = 0;

        while (didRead < len) {
            int buffOffRead = this.bufOff + len - didRead;

            if (buffOffRead >= this.bufBlock.length) {
                int amtToRead = this.bufBlock.length - this.bufOff;
                didRead += amtToRead;

                in.readBytes(this.bufBlock, this.bufOff, amtToRead);

                gCTRBlock(this.bufBlock, BLOCK_SIZE, out);
                if (!this.forEncryption) {
                    System.arraycopy(this.bufBlock, BLOCK_SIZE, this.bufBlock, 0, this.macSize);
                }
                resultLen += BLOCK_SIZE;
                this.bufOff = this.bufBlock.length - BLOCK_SIZE;
            } else {
                int read = len - didRead;
                in.readBytes(this.bufBlock, this.bufOff, read);
                this.bufOff += read;
                break;
            }
        }

        return resultLen;
    }

    public int doFinal(ByteBuf out) throws IllegalStateException, InvalidCipherTextException {
        int extra = this.bufOff;
        if (!this.forEncryption) {
            if (extra < this.macSize) {
                throw new InvalidCipherTextException("data too short");
            }
            extra -= this.macSize;
        }

        if (extra > 0) {
            byte[] tmp = new byte[BLOCK_SIZE];
            System.arraycopy(this.bufBlock, 0, tmp, 0, extra);
            gCTRBlock(tmp, extra, out);
        }

        // Final gHASH
        byte[] X = new byte[16];
        packLength((long) this.A.length * 8, X, 0);
        packLength(this.totalLength * 8, X, 8);

        xor(this.S, X);
        this.multiplier.multiplyH(this.S);

        // TODO Fix this if tagLength becomes configurable
        // T = MSBt(GCTRk(J0,S))
        byte[] tag = new byte[BLOCK_SIZE];
        this.cipher.processBlock(this.J0, 0, tag, 0);
        xor(tag, this.S);

        int resultLen = extra;

        // We place into macBlock our calculated value for T
        this.macBlock = new byte[this.macSize];
        System.arraycopy(tag, 0, this.macBlock, 0, this.macSize);

        if (this.forEncryption) {
            // Append T to the message
            out.writeBytes(this.macBlock, 0, this.macSize);
            resultLen += this.macSize;
        } else {
            // Retrieve the T value from the message and compare to calculated
            // one
            byte[] msgMac = new byte[this.macSize];
            System.arraycopy(this.bufBlock, extra, msgMac, 0, this.macSize);
            if (!Arrays.constantTimeAreEqual(this.macBlock, msgMac)) {
                throw new InvalidCipherTextException("mac check in GCM failed");
            }
        }

        reset(false);

        return resultLen;
    }

    @Override
    public void reset() {
        reset(true);
    }

    private void reset(boolean clearMac) {
        this.S = this.initS != null ? this.initS.copy() : Unpooled.buffer();
        this.counter = Arrays.clone(this.J0);
        this.bufOff = 0;
        this.totalLength = 0;

        if (this.bufBlock != null) {
            Arrays.fill(this.bufBlock, (byte) 0);
        }

        if (clearMac) {
            this.macBlock = null;
        }

        this.cipher.reset();
    }

    // MODIFIED
    private void gCTRBlock(byte[] buf, int bufCount, ByteBuf out) {
        // inc(counter);
        for (int i = 15; i >= 12; --i) {
            byte b = (byte) (this.counter[i] + 1 & 0xff);
            this.counter[i] = b;

            if (b != 0) {
                break;
            }
        }

        byte[] tmp = new byte[BLOCK_SIZE];
        this.cipher.processBlock(this.counter, 0, tmp, 0);

        byte[] hashBytes;
        if (this.forEncryption) {
            System.arraycopy(ZEROES, bufCount, tmp, bufCount, BLOCK_SIZE - bufCount);
            hashBytes = tmp;
        } else {
            hashBytes = buf;
        }

        for (int i = bufCount - 1; i >= 0; --i) {
            tmp[i] ^= buf[i];
        }
        out.writeBytes(tmp, 0, bufCount);

        // gHASHBlock(hashBytes);
        xor(this.S, hashBytes);
        this.multiplier.multiplyH(this.S);

        this.totalLength += bufCount;
    }

    private byte[] gHASH(byte[] b)
    {
        byte[] Y = new byte[16];

        for (int pos = 0; pos < b.length; pos += 16)
        {
            byte[] X = new byte[16];
            int num = Math.min(b.length - pos, 16);
            System.arraycopy(b, pos, X, 0, num);
            xor(Y, X);
            this.multiplier.multiplyH(Y);
        }

        return Y;
    }

//    private void gHASHBlock(byte[] block)
//    {
//        xor(S, block);
//        multiplier.multiplyH(S);
//    }

//    private static void inc(byte[] block)
//    {
//        for (int i = 15; i >= 12; --i)
//        {
//            byte b = (byte)((block[i] + 1) & 0xff);
//            block[i] = b;
//
//            if (b != 0)
//            {
//                break;
//            }
//        }
//    }

    private static void xor(ByteBuf block, byte[] val) {
        for (int i = 15; i >= 0; --i) {
            byte b = (byte) (block.getByte(i) ^ val[i]);
            block.setByte(i, b);
        }
    }

    private static void xor(byte[] block, ByteBuf val) {
        for (int i = 15; i >= 0; --i) {
            block[i] ^= val.getByte(i);
        }
    }

    private static void xor(byte[] block, byte[] val) {
        for (int i = 15; i >= 0; --i) {
            block[i] ^= val[i];
        }
    }

    private static void packLength(long count, byte[] bs, int off) {
        Pack.intToBigEndian((int)(count >>> 32), bs, off);
        Pack.intToBigEndian((int)count, bs, off + 4);
    }

    // MODIFIED
    @Override
    public int processBytes(byte[] in, int inOff, int len, byte[] out, int outOff) throws DataLengthException {
        // DO NOTHING
        return 0;
    }

    // MODIFIED
    @Override
    public int doFinal(byte[] out, int outOff) throws IllegalStateException, InvalidCipherTextException {
        // DO NOTHING
        return 0;
    }

    @Override
    public void processAADByte(byte arg0) {

    }

    @Override
    public void processAADBytes(byte[] arg0, int arg1, int arg2) {

    }
}
