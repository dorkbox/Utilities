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
 */
package dorkbox.util.crypto;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * AES crypto functions
 */
@SuppressWarnings({"unused", "Duplicates"})
public final
class CryptoAES {
    private static final int ivSize = 16;

    /**
     * AES encrypts data with a specified key.
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return empty byte[] if error
     */
    public static
    byte[] encryptWithIV(GCMBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, byte[] data, Logger logger) {
        byte[] encryptAES = encrypt(aesEngine, aesKey, aesIV, data, logger);

        int length = encryptAES.length;

        byte[] out = new byte[length + ivSize];
        System.arraycopy(aesIV, 0, out, 0, ivSize);
        System.arraycopy(encryptAES, 0, out, ivSize, length);

        return out;
    }

    /**
     * <b>CONVENIENCE METHOD ONLY - DO NOT USE UNLESS YOU HAVE TO</b>
     * <p/>
     * Use GCM instead, as it's an authenticated cipher (and "regular" AES is not). This prevents tampering with the blocks of encrypted
     * data.
     * <p/>
     * AES encrypts data with a specified key.
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return empty byte[] if error
     */
    @Deprecated
    public static
    byte[] encryptWithIV(BufferedBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, byte[] data, Logger logger) {

        byte[] encryptAES = encrypt(aesEngine, aesKey, aesIV, data, logger);

        int length = encryptAES.length;

        byte[] out = new byte[length + ivSize];
        System.arraycopy(aesIV, 0, out, 0, ivSize);
        System.arraycopy(encryptAES, 0, out, ivSize, length);

        return out;
    }

    /**
     * AES encrypts data with a specified key.
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return true if successful
     */
    public static
    boolean encryptStreamWithIV(GCMBlockCipher aesEngine,
                                byte[] aesKey,
                                byte[] aesIV,
                                InputStream in,
                                OutputStream out,
                                Logger logger) {

        try {
            out.write(aesIV);
        } catch (IOException e) {
            if (logger != null) {
                logger.error("Unable to perform AES cipher.", e);
            }
            return false;
        }

        return encryptStream(aesEngine, aesKey, aesIV, in, out, logger);
    }

    /**
     * <b>CONVENIENCE METHOD ONLY - DO NOT USE UNLESS YOU HAVE TO</b>
     * <p/>
     * Use GCM instead, as it's an authenticated cipher (and "regular" AES is not). This prevents tampering with the blocks of encrypted
     * data.
     * <p/>
     * AES encrypts data with a specified key.
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return true if successful
     */
    @Deprecated
    public static
    boolean encryptStreamWithIV(BufferedBlockCipher aesEngine,
                                byte[] aesKey,
                                byte[] aesIV,
                                InputStream in,
                                OutputStream out,
                                Logger logger) {

        try {
            out.write(aesIV);
        } catch (IOException e) {
            if (logger != null) {
                logger.error("Unable to perform AES cipher.", e);
            }
            return false;
        }

        return encryptStream(aesEngine, aesKey, aesIV, in, out, logger);
    }

    /**
     * AES encrypts data with a specified key.
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return empty byte[] if error
     */
    public static
    byte[] encrypt(GCMBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, byte[] data, Logger logger) {
        int length = data.length;

        CipherParameters aesIVAndKey = new ParametersWithIV(new KeyParameter(aesKey), aesIV);
        aesEngine.init(true, aesIVAndKey);

        int minSize = aesEngine.getOutputSize(length);
        byte[] outBuf = new byte[minSize];

        int actualLength = aesEngine.processBytes(data, 0, length, outBuf, 0);

        try {
            actualLength += aesEngine.doFinal(outBuf, actualLength);
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Unable to perform AES cipher.", e);
            }
            return new byte[0];
        }

        if (outBuf.length == actualLength) {
            return outBuf;
        }
        else {
            byte[] result = new byte[actualLength];
            System.arraycopy(outBuf, 0, result, 0, result.length);
            return result;
        }
    }

    /**
     * AES encrypts data with a specified key.
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return length of encrypted data, -1 if there was an error.
     */
    public static
    int encrypt(dorkbox.util.crypto.bouncycastle.GCMBlockCipher_ByteBuf aesEngine,
                CipherParameters aesIVAndKey,
                io.netty.buffer.ByteBuf inBuffer,
                io.netty.buffer.ByteBuf outBuffer,
                int length,
                Logger logger) {

        aesEngine.reset();
        aesEngine.init(true, aesIVAndKey);

        length = aesEngine.processBytes(inBuffer, outBuffer, length);

        try {
            length += aesEngine.doFinal(outBuffer);
        } catch (Exception e) {
            if (logger != null) {
                logger.debug("Unable to perform AES cipher.", e);
            }
            return -1;
        }

        // specify where the encrypted data is at
        outBuffer.readerIndex(0);
        outBuffer.writerIndex(length);

        return length;
    }

    /**
     * <b>CONVENIENCE METHOD ONLY - DO NOT USE UNLESS YOU HAVE TO</b>
     * <p/>
     * Use GCM instead, as it's an authenticated cipher (and "regular" AES is not). This prevents tampering with the blocks of encrypted
     * data.
     * <p/>
     * AES encrypts data with a specified key.
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return empty byte[] if error
     */
    @Deprecated
    public static
    byte[] encrypt(BufferedBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, byte[] data, Logger logger) {
        int length = data.length;

        CipherParameters aesIVAndKey = new ParametersWithIV(new KeyParameter(aesKey), aesIV);
        aesEngine.init(true, aesIVAndKey);

        int minSize = aesEngine.getOutputSize(length);
        byte[] outBuf = new byte[minSize];

        int actualLength = aesEngine.processBytes(data, 0, length, outBuf, 0);

        try {
            actualLength += aesEngine.doFinal(outBuf, actualLength);
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Unable to perform AES cipher.", e);
            }
            return new byte[0];
        }

        if (outBuf.length == actualLength) {
            return outBuf;
        }
        else {
            byte[] result = new byte[actualLength];
            System.arraycopy(outBuf, 0, result, 0, result.length);
            return result;
        }
    }

    /**
     * <b>CONVENIENCE METHOD ONLY - DO NOT USE UNLESS YOU HAVE TO</b>
     * <p/>
     * Use GCM instead, as it's an authenticated cipher (and "regular" AES is not). This prevents tampering with the blocks of encrypted
     * data.
     * <p/>
     * AES encrypt from one stream to another.
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return true if successful
     */
    @Deprecated
    public static
    boolean encryptStream(BufferedBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, InputStream in, OutputStream out, Logger logger) {
        byte[] buf = new byte[ivSize];
        byte[] outbuf = new byte[512];

        CipherParameters aesIVAndKey = new ParametersWithIV(new KeyParameter(aesKey), aesIV);
        aesEngine.init(true, aesIVAndKey);

        try {
            int bytesRead;
            int bytesProcessed;

            while ((bytesRead = in.read(buf)) >= 0) {
                bytesProcessed = aesEngine.processBytes(buf, 0, bytesRead, outbuf, 0);
                out.write(outbuf, 0, bytesProcessed);
            }

            bytesProcessed = aesEngine.doFinal(outbuf, 0);

            out.write(outbuf, 0, bytesProcessed);
            out.flush();
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Unable to perform AES cipher.", e);
            }
            return false;
        }

        return true;
    }

    /**
     * AES encrypt from one stream to another.
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return true if successful
     */
    public static
    boolean encryptStream(GCMBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, InputStream in, OutputStream out, Logger logger) {

        byte[] buf = new byte[ivSize];
        byte[] outbuf = new byte[512];

        CipherParameters aesIVAndKey = new ParametersWithIV(new KeyParameter(aesKey), aesIV);
        aesEngine.init(true, aesIVAndKey);

        try {
            int bytesRead;
            int bytesProcessed;

            while ((bytesRead = in.read(buf)) >= 0) {
                bytesProcessed = aesEngine.processBytes(buf, 0, bytesRead, outbuf, 0);
                out.write(outbuf, 0, bytesProcessed);
            }

            bytesProcessed = aesEngine.doFinal(outbuf, 0);

            out.write(outbuf, 0, bytesProcessed);
            out.flush();
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Unable to perform AES cipher.", e);
            }
            return false;
        }

        return true;
    }

    /**
     * AES decrypt (if the aes IV is included in the data)
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return empty byte[] if error
     */
    public static
    byte[] decryptWithIV(GCMBlockCipher aesEngine, byte[] aesKey, byte[] data, Logger logger) {
        byte[] aesIV = new byte[ivSize];
        System.arraycopy(data, 0, aesIV, 0, ivSize);

        byte[] in = new byte[data.length - ivSize];
        System.arraycopy(data, ivSize, in, 0, in.length);

        return decrypt(aesEngine, aesKey, aesIV, in, logger);
    }

    /**
     * <b>CONVENIENCE METHOD ONLY - DO NOT USE UNLESS YOU HAVE TO</b>
     * <p/>
     * Use GCM instead, as it's an authenticated cipher (and "regular" AES is not). This prevents tampering with the blocks of encrypted
     * data.
     * <p/>
     * AES decrypt (if the aes IV is included in the data)
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return empty byte[] if error
     */
    @Deprecated
    public static
    byte[] decryptWithIV(BufferedBlockCipher aesEngine, byte[] aesKey, byte[] data, Logger logger) {
        byte[] aesIV = new byte[ivSize];
        System.arraycopy(data, 0, aesIV, 0, ivSize);

        byte[] in = new byte[data.length - ivSize];
        System.arraycopy(data, ivSize, in, 0, in.length);

        return decrypt(aesEngine, aesKey, aesIV, in, logger);
    }

    /**
     * AES decrypt (if the aes IV is included in the data)
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return true if successful
     */
    public static
    boolean decryptStreamWithIV(GCMBlockCipher aesEngine, byte[] aesKey, InputStream in, OutputStream out, Logger logger) {
        byte[] aesIV = new byte[ivSize];
        try {
            in.read(aesIV, 0, ivSize);
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Unable to perform AES cipher.", e);
            }
            return false;
        }

        return decryptStream(aesEngine, aesKey, aesIV, in, out, logger);
    }

    /**
     * <b>CONVENIENCE METHOD ONLY - DO NOT USE UNLESS YOU HAVE TO</b>
     * <p/>
     * Use GCM instead, as it's an authenticated cipher (and "regular" AES is not). This prevents tampering with the blocks of encrypted
     * data.
     * <p/>
     * AES decrypt (if the aes IV is included in the data)
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return true if successful
     */
    @Deprecated
    public static
    boolean decryptStreamWithIV(BufferedBlockCipher aesEngine, byte[] aesKey, InputStream in, OutputStream out, Logger logger) {
        byte[] aesIV = new byte[ivSize];
        try {
            in.read(aesIV, 0, ivSize);
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Unable to perform AES cipher.", e);
            }
            return false;
        }

        return decryptStream(aesEngine, aesKey, aesIV, in, out, logger);
    }

    /**
     * AES decrypt (if we already know the aes IV -- and it's NOT included in the data)
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return empty byte[] if error
     */
    public static
    byte[] decrypt(GCMBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, byte[] data, Logger logger) {
        int length = data.length;

        CipherParameters aesIVAndKey = new ParametersWithIV(new KeyParameter(aesKey), aesIV);
        aesEngine.init(false, aesIVAndKey);

        int minSize = aesEngine.getOutputSize(length);
        byte[] outBuf = new byte[minSize];

        int actualLength = aesEngine.processBytes(data, 0, length, outBuf, 0);

        try {
            actualLength += aesEngine.doFinal(outBuf, actualLength);
        } catch (Exception e) {
            if (logger != null) {
                logger.debug("Unable to perform AES cipher.", e);
            }
            return new byte[0];
        }
        if (outBuf.length == actualLength) {
            return outBuf;
        }
        else {
            byte[] result = new byte[actualLength];
            System.arraycopy(outBuf, 0, result, 0, result.length);
            return result;
        }
    }

    /**
     * AES decrypt (if we already know the aes IV -- and it's NOT included in the data)
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return length of decrypted data, -1 if there was an error.
     */
    public static
    int decrypt(dorkbox.util.crypto.bouncycastle.GCMBlockCipher_ByteBuf aesEngine,
                ParametersWithIV aesIVAndKey,
                io.netty.buffer.ByteBuf bufferWithData,
                io.netty.buffer.ByteBuf bufferTempData,
                int length,
                Logger logger) {

        aesEngine.reset();
        aesEngine.init(false, aesIVAndKey);

        // ignore the start position
        // we also do NOT want to have the same start position for the altBuffer, since it could then grow larger than the buffer capacity.
        length = aesEngine.processBytes(bufferWithData, bufferTempData, length);

        try {
            length += aesEngine.doFinal(bufferTempData);
        } catch (Exception e) {
            if (logger != null) {
                logger.debug("Unable to perform AES cipher.", e);
            }
            return -1;
        }

        bufferTempData.readerIndex(0);
        bufferTempData.writerIndex(length);

        return length;
    }

    /**
     * <b>CONVENIENCE METHOD ONLY - DO NOT USE UNLESS YOU HAVE TO</b>
     * <p/>
     * Use GCM instead, as it's an authenticated cipher (and "regular" AES is not). This prevents tampering with the blocks of encrypted
     * data.
     * <p/>
     * AES decrypt (if we already know the aes IV -- and it's NOT included in the data)
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return empty byte[] if error
     */
    @Deprecated
    public static
    byte[] decrypt(BufferedBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, byte[] data, Logger logger) {

        int length = data.length;

        CipherParameters aesIVAndKey = new ParametersWithIV(new KeyParameter(aesKey), aesIV);
        aesEngine.init(false, aesIVAndKey);

        int minSize = aesEngine.getOutputSize(length);
        byte[] outBuf = new byte[minSize];

        int actualLength = aesEngine.processBytes(data, 0, length, outBuf, 0);

        try {
            actualLength += aesEngine.doFinal(outBuf, actualLength);
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Unable to perform AES cipher.", e);
            }
            return new byte[0];
        }

        if (outBuf.length == actualLength) {
            return outBuf;
        }
        else {
            byte[] result = new byte[actualLength];
            System.arraycopy(outBuf, 0, result, 0, result.length);
            return result;
        }
    }

    /**
     * AES decrypt from one stream to another.
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return true if successful
     */
    public static
    boolean decryptStream(GCMBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, InputStream in, OutputStream out, Logger logger) {
        byte[] buf = new byte[ivSize];
        byte[] outbuf = new byte[512];

        CipherParameters aesIVAndKey = new ParametersWithIV(new KeyParameter(aesKey), aesIV);
        aesEngine.init(false, aesIVAndKey);

        try {
            int bytesRead;
            int bytesProcessed;

            while ((bytesRead = in.read(buf)) >= 0) {
                bytesProcessed = aesEngine.processBytes(buf, 0, bytesRead, outbuf, 0);
                out.write(outbuf, 0, bytesProcessed);
            }

            bytesProcessed = aesEngine.doFinal(outbuf, 0);

            out.write(outbuf, 0, bytesProcessed);
            out.flush();
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Unable to perform AES cipher.", e);
            }
            return false;
        }

        return true;
    }

    /**
     * <b>CONVENIENCE METHOD ONLY - DO NOT USE UNLESS YOU HAVE TO</b>
     * <p/>
     * Use GCM instead, as it's an authenticated cipher (and "regular" AES is not). This prevents tampering with the blocks of encrypted
     * data.
     * <p/>
     * AES decrypt from one stream to another.
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return true if successful
     */
    @Deprecated
    public static
    boolean decryptStream(BufferedBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, InputStream in, OutputStream out, Logger logger) {
        byte[] buf = new byte[ivSize];
        byte[] outbuf = new byte[512];

        CipherParameters aesIVAndKey = new ParametersWithIV(new KeyParameter(aesKey), aesIV);
        aesEngine.init(false, aesIVAndKey);

        try {
            int bytesRead;
            int bytesProcessed;

            while ((bytesRead = in.read(buf)) >= 0) {
                bytesProcessed = aesEngine.processBytes(buf, 0, bytesRead, outbuf, 0);
                out.write(outbuf, 0, bytesProcessed);
            }

            bytesProcessed = aesEngine.doFinal(outbuf, 0);

            out.write(outbuf, 0, bytesProcessed);
            out.flush();
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Unable to perform AES cipher.", e);
            }
            return false;
        }

        return true;
    }

    private
    CryptoAES() {
    }
}
