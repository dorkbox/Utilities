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


import dorkbox.util.OS;
import dorkbox.util.bytes.LittleEndian;
import org.bouncycastle.crypto.*;
import org.bouncycastle.crypto.agreement.ECDHCBasicAgreement;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.engines.IESEngine;
import org.bouncycastle.crypto.generators.*;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.crypto.signers.DSASigner;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.PSSSigner;
import org.bouncycastle.jcajce.provider.util.DigestFactory;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * http://en.wikipedia.org/wiki/NSA_Suite_B
 * http://www.nsa.gov/ia/programs/suiteb_cryptography/
 * <p/>
 * NSA Suite B
 * <p/>
 * TOP-SECRET LEVEL
 * AES256/GCM
 * ECC with 384-bit prime curve (FIPS PUB 186-3), and SHA-384
 * <p/>
 * SECRET LEVEL
 * AES 128
 * ECDH and ECDSA using the 256-bit prime (FIPS PUB 186-3), and SHA-256. RSA with 2048 can be used for DH key negotiation
 * <p/>
 * WARNING!
 * Note that this call is INCOMPATIBLE with GWT, so we have EXCLUDED IT from gwt, and created a CryptoGwt class in the web-client project
 * which only has the necessary crypto utility methods that are
 * 1) Necessary
 * 2) Compatible with GWT
 * <p/>
 * <p/>
 * To determine if we have hardware accelerated AES
 * java -XX:+PrintFlagsFinal -version | grep UseAES
 */
public final
class Crypto {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Crypto.class);

    private
    Crypto() {
    }

    public static
    void addProvider() {
        // make sure we only add it once (in case it's added elsewhere...)
        Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (provider == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static final
    class Util {

        private
        Util() {
        }

        /**
         * Return the hash of the file or NULL if file is invalid
         */
        public static
        byte[] hashFile(File file, Digest digest) {
            return hashFile(file, digest, 0L);
        }

        /**
         * Return the hash of the file or NULL if file is invalid
         */
        public static
        byte[] hashFile(File file, Digest digest, long lengthFromEnd) {
            return hashFile(file, digest, lengthFromEnd, null);
        }

        /**
         * Return the hash of the file or NULL if the file is invalid. ALSO includes the hash of the 'extraData' if specified.
         */
        public static
        byte[] hashFile(File file, Digest digest, long lengthFromEnd, byte[] extraData) {
            if (file.isFile() && file.canRead()) {
                InputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(file);
                    long size = file.length();

                    if (lengthFromEnd > 0 && lengthFromEnd < size) {
                        size -= lengthFromEnd;
                    }

                    int bufferSize = 4096;
                    byte[] buffer = new byte[bufferSize];

                    int readBytes = 0;
                    digest.reset();

                    while (size > 0) {
                        int maxToRead = (int) Math.min(bufferSize, size);
                        readBytes = inputStream.read(buffer, 0, maxToRead);
                        size -= readBytes;

                        if (readBytes == 0) {
                            //wtf. finally still gets called.
                            return null;
                        }

                        digest.update(buffer, 0, readBytes);
                    }
                } catch (IOException e) {
                    logger.error("Error hashing file: {}", file.getAbsolutePath(), e);
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (extraData != null) {
                    digest.update(extraData, 0, extraData.length);
                }

                byte[] digestBytes = new byte[digest.getDigestSize()];

                digest.doFinal(digestBytes, 0);
                return digestBytes;

            }
            else {
                return null;
            }
        }

        // CUSTOM_HEADER USE
        private static final byte[] CUSTOM_HEADER = new byte[] {-2, -54, -54, -98};

        /**
         * Specifically, to return the hash of the ALL files/directories inside the jar, minus the action specified (LGPL) files.
         */
        public static
        byte[] hashJarContentsExcludeAction(File jarDestFilename, Digest digest, int action) throws IOException {
            JarFile jarDestFile = new JarFile(jarDestFilename);

            try {
                Enumeration<JarEntry> jarElements = jarDestFile.entries();

                boolean okToHash = false;
                boolean hasAction = false;
                byte[] buffer = new byte[2048];
                int read = 0;
                digest.reset();

                while (jarElements.hasMoreElements()) {
                    JarEntry jarEntry = jarElements.nextElement();
                    String name = jarEntry.getName();
                    okToHash = !jarEntry.isDirectory();

                    if (!okToHash) {
                        continue;
                    }

                    // data with NO extra data will NOT BE HASHED
                    // data that matches our action bitmask WILL NOT BE HASHED

                    okToHash = false;
                    hasAction = false;

                    byte[] extraData = jarEntry.getExtra();
                    if (extraData == null || extraData.length == 0) {
                        okToHash = false;
                    }
                    else if (extraData.length >= 4) {
                        for (int i = 0; i < CUSTOM_HEADER.length; i++) {
                            if (extraData[i] != CUSTOM_HEADER[i]) {
                                throw new RuntimeException("Unexpected extra data in zip assigned. Aborting");
                            }
                        }

                        // this means we matched our header
                        if (extraData[4] > 0) {
                            hasAction = true;

                            // we have an ACTION describing how it was compressed, etc
                            int fileAction = LittleEndian.Int_.from(new byte[] {extraData[5], extraData[6], extraData[7], extraData[8]});

                            if ((fileAction & action) != action) {
                                okToHash = true;
                            }
                        }
                        else {
                            okToHash = true;
                        }
                    }
                    else {
                        throw new RuntimeException("Unexpected extra data in zip assigned. Aborting");
                    }

                    // skips hashing lgpl files. (technically, whatever our action bitmask is...)
                    // we want to hash everything BY DEFAULT. we ALSO want to hash the NAME, LOAD ACTION TYPE, and the contents
                    if (okToHash) {
                        // System.err.println("HASHING: " + name);
                        // hash the file name
                        byte[] bytes = name.getBytes(OS.US_ASCII);
                        digest.update(bytes, 0, bytes.length);

                        if (hasAction) {
                            // hash the action - since we don't want to permit anyone to change this after we sign the file
                            digest.update(extraData, 5, 4);
                        }

                        // hash the contents
                        InputStream inputStream = jarDestFile.getInputStream(jarEntry);
                        while ((read = inputStream.read(buffer)) > 0) {
                            digest.update(buffer, 0, read);
                        }
                        inputStream.close();
                    }
                    //else {
                    //    System.err.println("Skipping: " + name);
                    //}
                }
            } catch (Exception e) {
                throw new RuntimeException("Unexpected extra data in zip assigned. Aborting");
            } finally {
                jarDestFile.close();
            }

            byte[] digestBytes = new byte[digest.getDigestSize()];

            digest.doFinal(digestBytes, 0);
            return digestBytes;
        }

        /**
         * Hash an input stream, based on the specified digest
         */
        public static
        byte[] hashStream(Digest digest, InputStream inputStream) throws IOException {

            byte[] buffer = new byte[2048];
            int read = 0;
            digest.reset();


            while ((read = inputStream.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            inputStream.close();

            byte[] digestBytes = new byte[digest.getDigestSize()];

            digest.doFinal(digestBytes, 0);
            return digestBytes;
        }

        /**
         * Secure way to generate an AES key based on a password. Will '*' out the passed-in password
         *
         * @param password       will be filled with '*'
         * @param salt           should be a RANDOM number, at least 256bits (32 bytes) in size.
         * @param iterationCount should be a lot, like 10,000
         * @return the secure key to use
         */
        public static
        byte[] PBKDF2(char[] password, byte[] salt, int iterationCount) {
            // will also zero out the password.
            byte[] charToBytes = Crypto.Util.charToBytesPassword(password);

            return PBKDF2(charToBytes, salt, iterationCount);
        }

        /**
         * Secure way to generate an AES key based on a password.
         *
         * @param password       The password that you want to mix
         * @param salt           should be a RANDOM number, at least 256bits (32 bytes) in size.
         * @param iterationCount should be a lot, like 10,000
         * @return the secure key to use
         */
        public static
        byte[] PBKDF2(byte[] password, byte[] salt, int iterationCount) {
            SHA256Digest digest = new SHA256Digest();
            PBEParametersGenerator pGen = new PKCS5S2ParametersGenerator(digest);
            pGen.init(password, salt, iterationCount);

            KeyParameter key = (KeyParameter) pGen.generateDerivedMacParameters(digest.getDigestSize() * 8); // *8 for bit length.

            // zero out the password.
            Arrays.fill(password, (byte) 0);

            return key.getKey();
        }


        /**
         * this saves the char array in UTF-16 format of bytes and BLANKS out the password char array.
         */
        public static
        byte[] charToBytesPassword(char[] password) {
            // note: this saves the char array in UTF-16 format of bytes.
            byte[] passwordBytes = new byte[password.length * 2];
            for (int i = 0; i < password.length; i++) {
                passwordBytes[2 * i] = (byte) ((password[i] & 0xFF00) >> 8);
                passwordBytes[2 * i + 1] = (byte) (password[i] & 0x00FF);
            }

            // asterisk out the password
            Arrays.fill(password, '*');

            return passwordBytes;
        }
    }


    public static final
    class AES {
        private static final int ivSize = 16;

        private
        AES() {
        }

        /**
         * AES encrypts data with a specified key.
         *
         * @return empty byte[] if error
         */
        public static
        byte[] encryptWithIV(GCMBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, byte[] data) {
            byte[] encryptAES = encrypt(aesEngine, aesKey, aesIV, data);

            int length = encryptAES.length;

            byte[] out = new byte[length + ivSize];
            System.arraycopy(aesIV, 0, out, 0, ivSize);
            System.arraycopy(encryptAES, 0, out, ivSize, length);

            return out;
        }

        /**
         * <b>CONVENIENCE METHOD ONLY - DO NOT USE UNLESS YOU HAVE TO</b>
         * <p/>
         * Use GCM instead, as it's an authenticated cipher (and "regular" AES is not). This prevents tampering with the blocks of encrypted data.
         * <p/>
         * AES encrypts data with a specified key.
         *
         * @return empty byte[] if error
         */
        @Deprecated
        public static
        byte[] encryptWithIV(BufferedBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, byte[] data) {

            byte[] encryptAES = encrypt(aesEngine, aesKey, aesIV, data);

            int length = encryptAES.length;

            byte[] out = new byte[length + ivSize];
            System.arraycopy(aesIV, 0, out, 0, ivSize);
            System.arraycopy(encryptAES, 0, out, ivSize, length);

            return out;
        }

        /**
         * AES encrypts data with a specified key.
         *
         * @return true if successful
         */
        public static
        boolean encryptStreamWithIV(GCMBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, InputStream in, OutputStream out) {

            try {
                out.write(aesIV);
            } catch (IOException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            }

            return encryptStream(aesEngine, aesKey, aesIV, in, out);
        }

        /**
         * <b>CONVENIENCE METHOD ONLY - DO NOT USE UNLESS YOU HAVE TO</b>
         * <p/>
         * Use GCM instead, as it's an authenticated cipher (and "regular" AES is not). This prevents tampering with the blocks of encrypted data.
         * <p/>
         * AES encrypts data with a specified key.
         *
         * @return true if successful
         */
        @Deprecated
        public static
        boolean encryptStreamWithIV(BufferedBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, InputStream in, OutputStream out) {

            try {
                out.write(aesIV);
            } catch (IOException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            }

            return encryptStream(aesEngine, aesKey, aesIV, in, out);
        }


        /**
         * AES encrypts data with a specified key.
         *
         * @return empty byte[] if error
         */
        public static
        byte[] encrypt(GCMBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, byte[] data) {
            int length = data.length;

            CipherParameters aesIVAndKey = new ParametersWithIV(new KeyParameter(aesKey), aesIV);
            aesEngine.init(true, aesIVAndKey);

            int minSize = aesEngine.getOutputSize(length);
            byte[] outBuf = new byte[minSize];

            int actualLength = aesEngine.processBytes(data, 0, length, outBuf, 0);

            try {
                actualLength += aesEngine.doFinal(outBuf, actualLength);
            } catch (DataLengthException e) {
                logger.error("Unable to perform AES cipher.", e);
                return new byte[0];
            } catch (IllegalStateException e) {
                logger.error("Unable to perform AES cipher.", e);
                return new byte[0];
            } catch (InvalidCipherTextException e) {
                logger.error("Unable to perform AES cipher.", e);
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
         * @return length of encrypted data, -1 if there was an error.
         */
        public static
        int encrypt(dorkbox.util.crypto.bouncycastle.GCMBlockCipher_ByteBuf aesEngine, CipherParameters aesIVAndKey,
                    io.netty.buffer.ByteBuf inBuffer, io.netty.buffer.ByteBuf outBuffer, int length) {

            aesEngine.reset();
            aesEngine.init(true, aesIVAndKey);

            length = aesEngine.processBytes(inBuffer, outBuffer, length);

            try {
                length += aesEngine.doFinal(outBuffer);
            } catch (DataLengthException e) {
                logger.debug("Unable to perform AES cipher.", e);
                return -1;
            } catch (IllegalStateException e) {
                logger.debug("Unable to perform AES cipher.", e);
                return -1;
            } catch (InvalidCipherTextException e) {
                logger.debug("Unable to perform AES cipher.", e);
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
         * Use GCM instead, as it's an authenticated cipher (and "regular" AES is not). This prevents tampering with the blocks of encrypted data.
         * <p/>
         * AES encrypts data with a specified key.
         *
         * @return empty byte[] if error
         */
        @Deprecated
        public static
        byte[] encrypt(BufferedBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, byte[] data) {
            int length = data.length;

            CipherParameters aesIVAndKey = new ParametersWithIV(new KeyParameter(aesKey), aesIV);
            aesEngine.init(true, aesIVAndKey);

            int minSize = aesEngine.getOutputSize(length);
            byte[] outBuf = new byte[minSize];

            int actualLength = aesEngine.processBytes(data, 0, length, outBuf, 0);

            try {
                actualLength += aesEngine.doFinal(outBuf, actualLength);
            } catch (DataLengthException e) {
                logger.error("Unable to perform AES cipher.", e);
                return new byte[0];
            } catch (IllegalStateException e) {
                logger.error("Unable to perform AES cipher.", e);
                return new byte[0];
            } catch (InvalidCipherTextException e) {
                logger.error("Unable to perform AES cipher.", e);
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
         * Use GCM instead, as it's an authenticated cipher (and "regular" AES is not). This prevents tampering with the blocks of encrypted data.
         * <p/>
         * AES encrypt from one stream to another.
         *
         * @return true if successful
         */
        @Deprecated
        public static
        boolean encryptStream(BufferedBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, InputStream in, OutputStream out) {
            byte[] buf = new byte[ivSize];
            byte[] outbuf = new byte[512];

            CipherParameters aesIVAndKey = new ParametersWithIV(new KeyParameter(aesKey), aesIV);
            aesEngine.init(true, aesIVAndKey);

            try {
                int bytesRead = 0;
                int bytesProcessed = 0;

                while ((bytesRead = in.read(buf)) >= 0) {
                    bytesProcessed = aesEngine.processBytes(buf, 0, bytesRead, outbuf, 0);
                    out.write(outbuf, 0, bytesProcessed);
                }

                bytesProcessed = aesEngine.doFinal(outbuf, 0);

                out.write(outbuf, 0, bytesProcessed);
                out.flush();
            } catch (IOException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            } catch (DataLengthException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            } catch (IllegalStateException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            } catch (InvalidCipherTextException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            }

            return true;
        }

        /**
         * AES encrypt from one stream to another.
         *
         * @return true if successful
         */
        public static
        boolean encryptStream(GCMBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, InputStream in, OutputStream out) {

            byte[] buf = new byte[ivSize];
            byte[] outbuf = new byte[512];

            CipherParameters aesIVAndKey = new ParametersWithIV(new KeyParameter(aesKey), aesIV);
            aesEngine.init(true, aesIVAndKey);

            try {
                int bytesRead = 0;
                int bytesProcessed = 0;

                while ((bytesRead = in.read(buf)) >= 0) {
                    bytesProcessed = aesEngine.processBytes(buf, 0, bytesRead, outbuf, 0);
                    out.write(outbuf, 0, bytesProcessed);
                }

                bytesProcessed = aesEngine.doFinal(outbuf, 0);

                out.write(outbuf, 0, bytesProcessed);
                out.flush();
            } catch (IOException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            } catch (DataLengthException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            } catch (IllegalStateException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            } catch (InvalidCipherTextException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            }

            return true;
        }

        /**
         * AES decrypt (if the aes IV is included in the data)
         *
         * @return empty byte[] if error
         */
        public static
        byte[] decryptWithIV(GCMBlockCipher aesEngine, byte[] aesKey, byte[] data) {
            byte[] aesIV = new byte[ivSize];
            System.arraycopy(data, 0, aesIV, 0, ivSize);

            byte[] in = new byte[data.length - ivSize];
            System.arraycopy(data, ivSize, in, 0, in.length);

            return decrypt(aesEngine, aesKey, aesIV, in);
        }

        /**
         * <b>CONVENIENCE METHOD ONLY - DO NOT USE UNLESS YOU HAVE TO</b>
         * <p/>
         * Use GCM instead, as it's an authenticated cipher (and "regular" AES is not). This prevents tampering with the blocks of encrypted data.
         * <p/>
         * AES decrypt (if the aes IV is included in the data)
         *
         * @return empty byte[] if error
         */
        @Deprecated
        public static
        byte[] decryptWithIV(BufferedBlockCipher aesEngine, byte[] aesKey, byte[] data) {
            byte[] aesIV = new byte[ivSize];
            System.arraycopy(data, 0, aesIV, 0, ivSize);

            byte[] in = new byte[data.length - ivSize];
            System.arraycopy(data, ivSize, in, 0, in.length);

            return decrypt(aesEngine, aesKey, aesIV, in);
        }

        /**
         * AES decrypt (if the aes IV is included in the data)
         *
         * @return true if successful
         */
        public static
        boolean decryptStreamWithIV(GCMBlockCipher aesEngine, byte[] aesKey, InputStream in, OutputStream out) {
            byte[] aesIV = new byte[ivSize];
            try {
                in.read(aesIV, 0, ivSize);
            } catch (IOException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            }

            return decryptStream(aesEngine, aesKey, aesIV, in, out);
        }

        /**
         * <b>CONVENIENCE METHOD ONLY - DO NOT USE UNLESS YOU HAVE TO</b>
         * <p/>
         * Use GCM instead, as it's an authenticated cipher (and "regular" AES is not). This prevents tampering with the blocks of encrypted data.
         * <p/>
         * AES decrypt (if the aes IV is included in the data)
         *
         * @return true if successful
         */
        @Deprecated
        public static
        boolean decryptStreamWithIV(BufferedBlockCipher aesEngine, byte[] aesKey, InputStream in, OutputStream out) {
            byte[] aesIV = new byte[ivSize];
            try {
                in.read(aesIV, 0, ivSize);
            } catch (IOException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            }

            return decryptStream(aesEngine, aesKey, aesIV, in, out);
        }

        /**
         * AES decrypt (if we already know the aes IV -- and it's NOT included in the data)
         *
         * @return empty byte[] if error
         */
        public static
        byte[] decrypt(GCMBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, byte[] data) {
            int length = data.length;

            CipherParameters aesIVAndKey = new ParametersWithIV(new KeyParameter(aesKey), aesIV);
            aesEngine.init(false, aesIVAndKey);

            int minSize = aesEngine.getOutputSize(length);
            byte[] outBuf = new byte[minSize];

            int actualLength = aesEngine.processBytes(data, 0, length, outBuf, 0);

            try {
                actualLength += aesEngine.doFinal(outBuf, actualLength);
            } catch (DataLengthException e) {
                logger.debug("Unable to perform AES cipher.", e);
                return new byte[0];
            } catch (IllegalStateException e) {
                logger.debug("Unable to perform AES cipher.", e);
                return new byte[0];
            } catch (InvalidCipherTextException e) {
                logger.debug("Unable to perform AES cipher.", e);
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
         * @return length of decrypted data, -1 if there was an error.
         */
        public static
        int decrypt(dorkbox.util.crypto.bouncycastle.GCMBlockCipher_ByteBuf aesEngine, ParametersWithIV aesIVAndKey,
                    io.netty.buffer.ByteBuf bufferWithData, io.netty.buffer.ByteBuf bufferTempData, int length) {

            aesEngine.reset();
            aesEngine.init(false, aesIVAndKey);

            // ignore the start position
            // we also do NOT want to have the same start position for the altBuffer, since it could then grow larger than the buffer capacity.
            length = aesEngine.processBytes(bufferWithData, bufferTempData, length);

            try {
                length += aesEngine.doFinal(bufferTempData);
            } catch (DataLengthException e) {
                logger.debug("Unable to perform AES cipher.", e);
                return -1;
            } catch (IllegalStateException e) {
                logger.debug("Unable to perform AES cipher.", e);
                return -1;
            } catch (InvalidCipherTextException e) {
                logger.debug("Unable to perform AES cipher.", e);
                return -1;
            }

            bufferTempData.readerIndex(0);
            bufferTempData.writerIndex(length);

            return length;
        }

        /**
         * <b>CONVENIENCE METHOD ONLY - DO NOT USE UNLESS YOU HAVE TO</b>
         * <p/>
         * Use GCM instead, as it's an authenticated cipher (and "regular" AES is not). This prevents tampering with the blocks of encrypted data.
         * <p/>
         * AES decrypt (if we already know the aes IV -- and it's NOT included in the data)
         *
         * @return empty byte[] if error
         */
        @Deprecated
        public static
        byte[] decrypt(BufferedBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, byte[] data) {

            int length = data.length;

            CipherParameters aesIVAndKey = new ParametersWithIV(new KeyParameter(aesKey), aesIV);
            aesEngine.init(false, aesIVAndKey);

            int minSize = aesEngine.getOutputSize(length);
            byte[] outBuf = new byte[minSize];

            int actualLength = aesEngine.processBytes(data, 0, length, outBuf, 0);

            try {
                actualLength += aesEngine.doFinal(outBuf, actualLength);
            } catch (DataLengthException e) {
                logger.error("Unable to perform AES cipher.", e);
                return new byte[0];
            } catch (IllegalStateException e) {
                logger.error("Unable to perform AES cipher.", e);
                return new byte[0];
            } catch (InvalidCipherTextException e) {
                logger.error("Unable to perform AES cipher.", e);
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
         * @return true if successful
         */
        public static
        boolean decryptStream(GCMBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, InputStream in, OutputStream out) {
            byte[] buf = new byte[ivSize];
            byte[] outbuf = new byte[512];

            CipherParameters aesIVAndKey = new ParametersWithIV(new KeyParameter(aesKey), aesIV);
            aesEngine.init(false, aesIVAndKey);

            try {
                int bytesRead = 0;
                int bytesProcessed = 0;

                while ((bytesRead = in.read(buf)) >= 0) {
                    bytesProcessed = aesEngine.processBytes(buf, 0, bytesRead, outbuf, 0);
                    out.write(outbuf, 0, bytesProcessed);
                }

                bytesProcessed = aesEngine.doFinal(outbuf, 0);

                out.write(outbuf, 0, bytesProcessed);
                out.flush();
            } catch (IOException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            } catch (DataLengthException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            } catch (IllegalStateException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            } catch (InvalidCipherTextException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            }

            return true;
        }

        /**
         * <b>CONVENIENCE METHOD ONLY - DO NOT USE UNLESS YOU HAVE TO</b>
         * <p/>
         * Use GCM instead, as it's an authenticated cipher (and "regular" AES is not). This prevents tampering with the blocks of encrypted data.
         * <p/>
         * AES decrypt from one stream to another.
         *
         * @return true if successful
         */
        @Deprecated
        public static
        boolean decryptStream(BufferedBlockCipher aesEngine, byte[] aesKey, byte[] aesIV, InputStream in, OutputStream out) {
            byte[] buf = new byte[ivSize];
            byte[] outbuf = new byte[512];

            CipherParameters aesIVAndKey = new ParametersWithIV(new KeyParameter(aesKey), aesIV);
            aesEngine.init(false, aesIVAndKey);

            try {
                int bytesRead = 0;
                int bytesProcessed = 0;

                while ((bytesRead = in.read(buf)) >= 0) {
                    bytesProcessed = aesEngine.processBytes(buf, 0, bytesRead, outbuf, 0);
                    out.write(outbuf, 0, bytesProcessed);
                }

                bytesProcessed = aesEngine.doFinal(outbuf, 0);

                out.write(outbuf, 0, bytesProcessed);
                out.flush();
            } catch (IOException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            } catch (DataLengthException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            } catch (IllegalStateException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            } catch (InvalidCipherTextException e) {
                logger.error("Unable to perform AES cipher.", e);
                return false;
            }

            return true;
        }
    }



    // Note: this is here just for keeping track of how this is done. This should NOT be used, and instead use ECC crypto.
    @Deprecated
    public static final
    class RSA {

        private
        RSA() {
        }

        public static
        AsymmetricCipherKeyPair generateKeyPair(SecureRandom secureRandom, int keyLength) {
            RSAKeyPairGenerator keyGen = new RSAKeyPairGenerator();
            RSAKeyGenerationParameters params = new RSAKeyGenerationParameters(new BigInteger("65537"), // public exponent
                                                                               secureRandom, //pnrg
                                                                               keyLength, // key length
                                                                               8);  //the number of iterations of the Miller-Rabin primality test.
            keyGen.init(params);
            return keyGen.generateKeyPair();
        }


        /**
         * RSA encrypt using public key A, and sign data with private key B.
         * <p/>
         * byte[0][] = encrypted data
         * byte[1][] = signature
         *
         * @return empty byte[][] if error
         */
        public static
        byte[][] encryptAndSign(AsymmetricBlockCipher rsaEngine, Digest digest, RSAKeyParameters rsaPublicKeyA,
                                RSAPrivateCrtKeyParameters rsaPrivateKeyB, byte[] bytes) {
            if (bytes.length == 0) {
                return new byte[0][0];
            }

            byte[] encryptBytes = encrypt(rsaEngine, rsaPublicKeyA, bytes);

            if (encryptBytes.length == 0) {
                return new byte[0][0];
            }

            // now sign it.
            PSSSigner signer = new PSSSigner(rsaEngine, digest, digest.getDigestSize());

            byte[] signatureRSA = Crypto.RSA.sign(signer, rsaPrivateKeyB, encryptBytes);

            if (signatureRSA.length == 0) {
                return new byte[0][0];
            }

            byte[][] total = new byte[2][];
            total[0] = encryptBytes;
            total[1] = signatureRSA;


            return total;
        }

        /**
         * RSA verify data with public key B, and decrypt using private key A.
         *
         * @return empty byte[] if error
         */
        public static
        byte[] decryptAndVerify(AsymmetricBlockCipher rsaEngine, Digest digest, RSAKeyParameters rsaPublicKeyA,
                                RSAPrivateCrtKeyParameters rsaPrivateKeyB, byte[] encryptedData, byte[] signature) {
            if (encryptedData.length == 0 || signature.length == 0) {
                return new byte[0];
            }

            // verify encrypted data.
            PSSSigner signer = new PSSSigner(rsaEngine, digest, digest.getDigestSize());

            boolean verify = verify(signer, rsaPublicKeyA, signature, encryptedData);
            if (!verify) {
                return new byte[0];
            }

            return decrypt(rsaEngine, rsaPrivateKeyB, encryptedData);

        }

        /**
         * RSA encrypts data with a specified key.
         *
         * @return empty byte[] if error
         */
        public static
        byte[] encrypt(AsymmetricBlockCipher rsaEngine, RSAKeyParameters rsaPublicKey, byte[] bytes) {
            rsaEngine.init(true, rsaPublicKey);

            try {
                int inputBlockSize = rsaEngine.getInputBlockSize();
                if (inputBlockSize < bytes.length) {
                    int outSize = rsaEngine.getOutputBlockSize();
                    int realsize = (int) Math.round(bytes.length / (outSize * 1.0D) + .5);
                    ByteBuffer buffer = ByteBuffer.allocateDirect(outSize * realsize);

                    int position = 0;

                    while (position < bytes.length) {
                        int size = Math.min(inputBlockSize, bytes.length - position);

                        byte[] block = rsaEngine.processBlock(bytes, position, size);
                        buffer.put(block, 0, block.length);

                        position += size;
                    }


                    return buffer.array();

                }
                else {
                    return rsaEngine.processBlock(bytes, 0, bytes.length);
                }
            } catch (InvalidCipherTextException e) {
                logger.error("Unable to perform RSA cipher.", e);
                return new byte[0];
            }
        }

        /**
         * RSA decrypt data with a specified key.
         *
         * @return empty byte[] if error
         */
        public static
        byte[] decrypt(AsymmetricBlockCipher rsaEngine, RSAPrivateCrtKeyParameters rsaPrivateKey, byte[] bytes) {
            rsaEngine.init(false, rsaPrivateKey);

            try {
                int inputBlockSize = rsaEngine.getInputBlockSize();
                if (inputBlockSize < bytes.length) {
                    int outSize = rsaEngine.getOutputBlockSize();
                    int realsize = (int) Math.round(bytes.length / (outSize * 1.0D) + .5);
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream(outSize * realsize);

                    int position = 0;

                    while (position < bytes.length) {
                        int size = Math.min(inputBlockSize, bytes.length - position);

                        byte[] block = rsaEngine.processBlock(bytes, position, size);
                        buffer.write(block, 0, block.length);

                        position += size;
                    }


                    return buffer.toByteArray();
                }
                else {
                    return rsaEngine.processBlock(bytes, 0, bytes.length);
                }
            } catch (InvalidCipherTextException e) {
                logger.error("Unable to perform RSA cipher.", e);
                return new byte[0];
            }
        }

        /**
         * RSA sign data with a specified key.
         *
         * @return empty byte[] if error
         */
        public static
        byte[] sign(PSSSigner signer, RSAPrivateCrtKeyParameters rsaPrivateKey, byte[] mesg) {
            signer.init(true, rsaPrivateKey);
            signer.update(mesg, 0, mesg.length);

            try {
                return signer.generateSignature();
            } catch (Exception e) {
                logger.error("Unable to perform RSA cipher.", e);
                return new byte[0];
            }
        }

        /**
         * RSA verify data with a specified key.
         */
        public static
        boolean verify(PSSSigner signer, RSAKeyParameters rsaPublicKey, byte[] sig, byte[] mesg) {
            signer.init(false, rsaPublicKey);
            signer.update(mesg, 0, mesg.length);

            return signer.verifySignature(sig);
        }

        public static
        boolean compare(RSAKeyParameters publicA, RSAKeyParameters publicB) {
            if (!publicA.getExponent().equals(publicB.getExponent())) {
                return false;
            }
            if (!publicA.getModulus().equals(publicB.getModulus())) {
                return false;
            }

            return true;
        }

        public static
        boolean compare(RSAPrivateCrtKeyParameters private1, RSAPrivateCrtKeyParameters private2) {
            if (!private1.getModulus().equals(private2.getModulus())) {
                return false;
            }
            if (!private1.getExponent().equals(private2.getExponent())) {
                return false;
            }
            if (!private1.getDP().equals(private2.getDP())) {
                return false;
            }
            if (!private1.getDQ().equals(private2.getDQ())) {
                return false;
            }
            if (!private1.getP().equals(private2.getP())) {
                return false;
            }
            if (!private1.getPublicExponent().equals(private2.getPublicExponent())) {
                return false;
            }
            if (!private1.getQ().equals(private2.getQ())) {
                return false;
            }
            if (!private1.getQInv().equals(private2.getQInv())) {
                return false;
            }

            return true;
        }
    }



    // Note: this is here just for keeping track of how this is done. This should NOT be used, and instead use ECC crypto.
    @Deprecated
    public static final
    class DSA {
        private
        DSA() {
        }

        /**
         * Generates the DSA key (using RSA and SHA1)
         * <p/>
         * Note: this is here just for keeping track of how this is done. This should NOT be used, and instead use ECC crypto.
         */
        public static
        AsymmetricCipherKeyPair generateKeyPair(SecureRandom secureRandom, int keyLength) {
            DSAKeyPairGenerator keyGen = new DSAKeyPairGenerator();

            DSAParametersGenerator dsaParametersGenerator = new DSAParametersGenerator();
            dsaParametersGenerator.init(keyLength, 20, secureRandom);
            DSAParameters generateParameters = dsaParametersGenerator.generateParameters();

            DSAKeyGenerationParameters params = new DSAKeyGenerationParameters(secureRandom, generateParameters);
            keyGen.init(params);
            return keyGen.generateKeyPair();
        }

        /**
         * The message will have the SHA1 hash calculated and used for the signature.
         * <p/>
         * Note: this is here just for keeping track of how this is done. This should NOT be used, and instead use ECC crypto.
         * <p/>
         * The returned signature is the {r,s} signature array.
         */
        public static
        BigInteger[] generateSignature(DSAPrivateKeyParameters privateKey, SecureRandom secureRandom, byte[] message) {
            ParametersWithRandom param = new ParametersWithRandom(privateKey, secureRandom);

            DSASigner dsa = new DSASigner();

            dsa.init(true, param);


            SHA1Digest sha1Digest = new SHA1Digest();
            byte[] checksum = new byte[sha1Digest.getDigestSize()];

            sha1Digest.update(message, 0, message.length);
            sha1Digest.doFinal(checksum, 0);


            BigInteger[] signature = dsa.generateSignature(checksum);
            return signature;
        }

        /**
         * The message will have the SHA1 hash calculated and used for the signature.
         * <p/>
         * Note: this is here just for keeping track of how this is done. This should NOT be used, and instead use ECC crypto.
         *
         * @param signature is the {r,s} signature array.
         * @return true if the signature is valid
         */
        public static
        boolean verifySignature(DSAPublicKeyParameters publicKey, byte[] message, BigInteger[] signature) {
            SHA1Digest sha1Digest = new SHA1Digest();
            byte[] checksum = new byte[sha1Digest.getDigestSize()];

            sha1Digest.update(message, 0, message.length);
            sha1Digest.doFinal(checksum, 0);


            DSASigner dsa = new DSASigner();

            dsa.init(false, publicKey);

            return dsa.verifySignature(checksum, signature[0], signature[1]);
        }
    }



    public static final
    class ECC {
        static final String ECC_NAME = "EC";
        public static final String p521_curve = "secp521r1";

        // more info about ECC from: http://www.johannes-bauer.com/compsci/ecc/?menuid=4
        // http://stackoverflow.com/questions/7419183/problems-implementing-ecdh-on-android-using-bouncycastle
        // http://tools.ietf.org/html/draft-jivsov-openpgp-ecc-06#page-4
        // http://www.nsa.gov/ia/programs/suiteb_cryptography/
        // https://github.com/nelenkov/ecdh-kx/blob/master/src/org/nick/ecdhkx/Crypto.java
        // http://nelenkov.blogspot.com/2011/12/using-ecdh-on-android.html
        // http://www.secg.org/collateral/sec1_final.pdf

        public static final int macSize = 512;

        private
        ECC() {
        }

        /**
         * Uses SHA512
         */
        public static
        IESEngine createEngine() {
            return new IESEngine(new ECDHCBasicAgreement(), new KDF2BytesGenerator(new SHA384Digest()), new HMac(new SHA512Digest()));
        }

        /**
         * Uses SHA512
         */
        public static
        IESEngine createEngine(PaddedBufferedBlockCipher aesEngine) {
            return new IESEngine(new ECDHCBasicAgreement(), new KDF2BytesGenerator(new SHA384Digest()), new HMac(new SHA512Digest()),
                                 aesEngine);
        }

        /**
         * These parameters are shared between the two parties. These are a NONCE (use ONCE number!!)
         */
        public static
        IESParameters generateSharedParameters(SecureRandom secureRandom) {

            int macSize = Crypto.ECC.macSize; // must be the MAC size

            // MUST be random EACH TIME encrypt/sign happens!
            byte[] derivation = new byte[macSize / 8];
            byte[] encoding = new byte[macSize / 8];

            secureRandom.nextBytes(derivation);
            secureRandom.nextBytes(encoding);

            return new IESParameters(derivation, encoding, macSize);
        }

        /**
         * AES-256 ONLY!
         */
        public static
        IESWithCipherParameters generateSharedParametersWithCipher(SecureRandom secureRandom) {
            int macSize = Crypto.ECC.macSize; // must be the MAC size

            byte[] derivation = new byte[macSize / 8]; // MUST be random EACH TIME encrypt/sign happens!
            byte[] encoding = new byte[macSize / 8];

            secureRandom.nextBytes(derivation);
            secureRandom.nextBytes(encoding);

            return new IESWithCipherParameters(derivation, encoding, macSize, 256);
        }


        public static
        AsymmetricCipherKeyPair generateKeyPair(String eccCurveName, SecureRandom secureRandom) {
            ECParameterSpec eccSpec = ECNamedCurveTable.getParameterSpec(eccCurveName);

            return generateKeyPair(eccSpec, secureRandom);
        }

        public static
        AsymmetricCipherKeyPair generateKeyPair(ECParameterSpec eccSpec, SecureRandom secureRandom) {
            ECKeyGenerationParameters ecParams = new ECKeyGenerationParameters(new ECDomainParameters(eccSpec.getCurve(), eccSpec.getG(),
                                                                                                      eccSpec.getN()), secureRandom);

            ECKeyPairGenerator ecKeyGen = new ECKeyPairGenerator();
            ecKeyGen.init(ecParams);

            return ecKeyGen.generateKeyPair();
        }

        /**
         * ECC encrypts data with a specified key.
         *
         * @return empty byte[] if error
         */
        public static
        byte[] encrypt(IESEngine eccEngine, CipherParameters private1, CipherParameters public2, IESParameters cipherParams,
                       byte[] message) {

            eccEngine.init(true, private1, public2, cipherParams);

            try {
                return eccEngine.processBlock(message, 0, message.length);
            } catch (InvalidCipherTextException e) {
                logger.error("Unable to perform ECC cipher.", e);
                return new byte[0];
            }
        }

        /**
         * ECC decrypt data with a specified key.
         *
         * @return empty byte[] if error
         */
        public static
        byte[] decrypt(IESEngine eccEngine, CipherParameters private2, CipherParameters public1, IESParameters cipherParams,
                       byte[] encrypted) {

            eccEngine.init(false, private2, public1, cipherParams);

            try {
                return eccEngine.processBlock(encrypted, 0, encrypted.length);
            } catch (InvalidCipherTextException e) {
                logger.error("Unable to perform ECC cipher.", e);
                return new byte[0];
            }
        }

        public static
        boolean compare(ECPrivateKeyParameters privateA, ECPrivateKeyParameters privateB) {
            ECDomainParameters parametersA = privateA.getParameters();
            ECDomainParameters parametersB = privateB.getParameters();

            // is it the same curve?
            boolean equals = parametersA.getCurve().equals(parametersB.getCurve());
            if (!equals) {
                return false;
            }

            equals = parametersA.getG().equals(parametersB.getG());
            if (!equals) {
                return false;
            }


            equals = parametersA.getH().equals(parametersB.getH());
            if (!equals) {
                return false;
            }

            equals = parametersA.getN().equals(parametersB.getN());
            if (!equals) {
                return false;
            }

            equals = privateA.getD().equals(privateB.getD());

            return equals;
        }

        /**
         * @return true if publicA and publicB are NOT NULL, and are both equal to eachother
         */
        public static
        boolean compare(ECPublicKeyParameters publicA, ECPublicKeyParameters publicB) {
            if (publicA == null || publicB == null) {
                return false;
            }


            ECDomainParameters parametersA = publicA.getParameters();
            ECDomainParameters parametersB = publicB.getParameters();

            // is it the same curve?
            boolean equals = parametersA.getCurve().equals(parametersB.getCurve());
            if (!equals) {
                return false;
            }

            equals = parametersA.getG().equals(parametersB.getG());
            if (!equals) {
                return false;
            }


            equals = parametersA.getH().equals(parametersB.getH());
            if (!equals) {
                return false;
            }

            equals = parametersA.getN().equals(parametersB.getN());
            if (!equals) {
                return false;
            }


            ECPoint normalizeA = publicA.getQ().normalize();
            ECPoint normalizeB = publicB.getQ().normalize();


            ECFieldElement xCoordA = normalizeA.getXCoord();
            ECFieldElement xCoordB = normalizeB.getXCoord();

            equals = xCoordA.equals(xCoordB);
            if (!equals) {
                return false;
            }

            ECFieldElement yCoordA = normalizeA.getYCoord();
            ECFieldElement yCoordB = normalizeB.getYCoord();

            equals = yCoordA.equals(yCoordB);
            if (!equals) {
                return false;
            }

            return true;
        }

        public static
        boolean compare(IESParameters cipherAParams, IESParameters cipherBParams) {
            if (!Arrays.equals(cipherAParams.getDerivationV(), cipherBParams.getDerivationV())) {
                return false;
            }
            if (!Arrays.equals(cipherAParams.getEncodingV(), cipherBParams.getEncodingV())) {
                return false;
            }

            if (cipherAParams.getMacKeySize() != cipherBParams.getMacKeySize()) {
                return false;
            }
            return true;
        }

        public static
        boolean compare(IESWithCipherParameters cipherAParams, IESWithCipherParameters cipherBParams) {
            if (cipherAParams.getCipherKeySize() != cipherBParams.getCipherKeySize()) {
                return false;
            }

            // only need to cast one side.
            return compare((IESParameters) cipherAParams, cipherBParams);
        }


        /**
         * The message will have the (digestName) hash calculated and used for the signature.
         * <p/>
         * The returned signature is the {r,s} signature array.
         */
        public static
        BigInteger[] generateSignature(String digestName, ECPrivateKeyParameters privateKey, SecureRandom secureRandom, byte[] bytes) {

            Digest digest = DigestFactory.getDigest(digestName);

            byte[] checksum = new byte[digest.getDigestSize()];

            digest.update(bytes, 0, bytes.length);
            digest.doFinal(checksum, 0);

            return generateSignatureForHash(privateKey, secureRandom, checksum);
        }

        /**
         * The message will use the bytes AS THE HASHED VALUE to calculate the signature.
         * <p/>
         * The returned signature is the {r,s} signature array.
         */
        public static
        BigInteger[] generateSignatureForHash(ECPrivateKeyParameters privateKey, SecureRandom secureRandom, byte[] hashBytes) {

            ParametersWithRandom param = new ParametersWithRandom(privateKey, secureRandom);

            ECDSASigner ecdsa = new ECDSASigner();
            ecdsa.init(true, param);

            return ecdsa.generateSignature(hashBytes);
        }

        /**
         * The message will have the (digestName) hash calculated and used for the signature.
         *
         * @param signature is the {r,s} signature array.
         * @return true if the signature is valid
         */
        public static
        boolean verifySignature(String digestName, ECPublicKeyParameters publicKey, byte[] message, BigInteger[] signature) {

            Digest digest = DigestFactory.getDigest(digestName);

            byte[] checksum = new byte[digest.getDigestSize()];

            digest.update(message, 0, message.length);
            digest.doFinal(checksum, 0);


            return verifySignatureHash(publicKey, checksum, signature);
        }

        /**
         * The provided hash will be used in the signature verification.
         *
         * @param signature is the {r,s} signature array.
         * @return true if the signature is valid
         */
        public static
        boolean verifySignatureHash(ECPublicKeyParameters publicKey, byte[] hash, BigInteger[] signature) {

            ECDSASigner ecdsa = new ECDSASigner();
            ecdsa.init(false, publicKey);


            return ecdsa.verifySignature(hash, signature[0], signature[1]);
        }
    }



    /**
     * An implementation of the <a href="http://www.tarsnap.com/scrypt/scrypt.pdf"/>scrypt</a>
     * key derivation function.
     */
    public static final
    class SCrypt {

        private
        SCrypt() {
        }

        /**
         * Hash the supplied plaintext password and generate output using default parameters
         * <p/>
         * The password chars are no longer valid after this call
         *
         * @param password Password.
         */
        public static
        String encrypt(char[] password) {
            return encrypt(password, 16384, 32, 1);
        }

        /**
         * Hash the supplied plaintext password and generate output using default parameters
         * <p/>
         * The password chars are no longer valid after this call
         *
         * @param password Password.
         * @param salt     Salt parameter
         */
        public static
        String encrypt(char[] password, byte[] salt) {
            return encrypt(password, salt, 16384, 32, 1, 64);
        }

        /**
         * Hash the supplied plaintext password and generate output.
         * <p/>
         * The password chars are no longer valid after this call
         *
         * @param password Password.
         * @param N        CPU cost parameter.
         * @param r        Memory cost parameter.
         * @param p        Parallelization parameter.
         * @return The hashed password.
         */
        public static
        String encrypt(char[] password, int N, int r, int p) {
            SecureRandom secureRandom = new SecureRandom();
            byte[] salt = new byte[32];
            secureRandom.nextBytes(salt);

            return encrypt(password, salt, N, r, p, 64);
        }

        /**
         * Hash the supplied plaintext password and generate output.
         * <p/>
         * The password chars are no longer valid after this call
         *
         * @param password Password.
         * @param salt     Salt parameter
         * @param N        CPU cost parameter.
         * @param r        Memory cost parameter.
         * @param p        Parallelization parameter.
         * @param dkLen    Intended length of the derived key.
         * @return The hashed password.
         */
        public static
        String encrypt(char[] password, byte[] salt, int N, int r, int p, int dkLen) {
            // Note: this saves the char array in UTF-16 format of bytes.
            // can't use password after this as it's been changed to '*'
            byte[] passwordBytes = Crypto.Util.charToBytesPassword(password);

            byte[] derived = encrypt(passwordBytes, salt, N, r, p, dkLen);

            String params = Integer.toString(log2(N) << 16 | r << 8 | p, 16);

            StringBuilder sb = new StringBuilder((salt.length + derived.length) * 2);
            sb.append("$s0$").append(params).append('$');
            sb.append(dorkbox.util.Base64Fast.encodeToString(salt, false)).append('$');
            sb.append(dorkbox.util.Base64Fast.encodeToString(derived, false));

            return sb.toString();
        }

        /**
         * Compare the supplied plaintext password to a hashed password.
         *
         * @param password Plaintext password.
         * @param hashed   scrypt hashed password.
         * @return true if password matches hashed value.
         */
        public static
        boolean verify(char[] password, String hashed) {
            // Note: this saves the char array in UTF-16 format of bytes.
            // can't use password after this as it's been changed to '*'
            byte[] passwordBytes = Crypto.Util.charToBytesPassword(password);

            String[] parts = hashed.split("\\$");

            if (parts.length != 5 || !parts[1].equals("s0")) {
                throw new IllegalArgumentException("Invalid hashed value");
            }

            int params = Integer.parseInt(parts[2], 16);
            byte[] salt = dorkbox.util.Base64Fast.decodeFast(parts[3]);
            byte[] derived0 = dorkbox.util.Base64Fast.decodeFast(parts[4]);

            int N = (int) Math.pow(2, params >> 16 & 0xFF);
            int r = params >> 8 & 0xFF;
            int p = params & 0xFF;

            int length = derived0.length;
            if (length == 0) {
                return false;
            }

            byte[] derived1 = encrypt(passwordBytes, salt, N, r, p, length);

            if (length != derived1.length) {
                return false;
            }

            int result = 0;
            for (int i = 0; i < length; i++) {
                result |= derived0[i] ^ derived1[i];
            }

            return result == 0;
        }

        private static
        int log2(int n) {
            int log = 0;
            if ((n & 0xFFFF0000) != 0) {
                n >>>= 16;
                log = 16;
            }
            if (n >= 256) {
                n >>>= 8;
                log += 8;
            }
            if (n >= 16) {
                n >>>= 4;
                log += 4;
            }
            if (n >= 4) {
                n >>>= 2;
                log += 2;
            }
            return log + (n >>> 1);
        }


        /**
         * Pure Java implementation of the <a href="http://www.tarsnap.com/scrypt/scrypt.pdf"/>scrypt KDF</a>.
         *
         * @param password Password.
         * @param salt     Salt.
         * @param N        CPU cost parameter.
         * @param r        Memory cost parameter.
         * @param p        Parallelization parameter.
         * @param dkLen    Intended length of the derived key.
         * @return The derived key.
         */
        public static
        byte[] encrypt(byte[] password, byte[] salt, int N, int r, int p, int dkLen) {
            if (N == 0 || (N & N - 1) != 0) {
                throw new IllegalArgumentException("N must be > 0 and a power of 2");
            }

            if (N > Integer.MAX_VALUE / 128 / r) {
                throw new IllegalArgumentException("Parameter N is too large");
            }
            if (r > Integer.MAX_VALUE / 128 / p) {
                throw new IllegalArgumentException("Parameter r is too large");
            }

            try {
                return org.bouncycastle.crypto.generators.SCrypt.generate(password, salt, N, r, p, dkLen);
            } finally {
                // now zero out the bytes in password.
                Arrays.fill(password, (byte) 0);
            }
        }
    }
}
