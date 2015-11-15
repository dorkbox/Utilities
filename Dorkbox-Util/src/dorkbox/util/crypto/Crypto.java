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
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * http://en.wikipedia.org/wiki/NSA_Suite_B http://www.nsa.gov/ia/programs/suiteb_cryptography/
 * <p/>
 * NSA Suite B
 * <p/>
 * TOP-SECRET LEVEL AES256/GCM ECC with 384-bit prime curve (FIPS PUB 186-3), and SHA-384
 * <p/>
 * SECRET LEVEL AES 128 ECDH and ECDSA using the 256-bit prime (FIPS PUB 186-3), and SHA-256. RSA with 2048 can be used for DH key
 * negotiation
 * <p/>
 * WARNING! Note that this call is INCOMPATIBLE with GWT, so we have EXCLUDED IT from gwt, and created a CryptoGwt class in the web-client
 * project which only has the necessary crypto utility methods that are 1) Necessary 2) Compatible with GWT
 * <p/>
 * <p/>
 * To determine if we have hardware accelerated AES java -XX:+PrintFlagsFinal -version | grep UseAES
 */
@SuppressWarnings("unused")
public final
class Crypto {


    public static
    void addProvider() {
        // make sure we only add it once (in case it's added elsewhere...)
        Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (provider == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private
    Crypto() {
    }


    public static final
    class Util {

        // CUSTOM_HEADER USE
        private static final byte[] CUSTOM_HEADER = new byte[] {(byte) -2, (byte) -54, (byte) -54, (byte) -98};

        /**
         * Return the hash of the file or NULL if file is invalid
         *
         * @param logger
         *                 may be null, if no log output is necessary
         */
        public static
        byte[] hashFile(File file, Digest digest, Logger logger) {
            return hashFile(file, digest, 0L, logger);
        }

        /**
         * Return the hash of the file or NULL if file is invalid
         *
         * @param logger
         *                 may be null, if no log output is necessary
         */
        public static
        byte[] hashFile(File file, Digest digest, long lengthFromEnd, Logger logger) {
            return hashFile(file, digest, lengthFromEnd, null, logger);
        }

        /**
         * Return the hash of the file or NULL if the file is invalid. ALSO includes the hash of the 'extraData' if specified.
         *
         * @param logger
         *                 may be null, if no log output is necessary
         */
        public static
        byte[] hashFile(File file, Digest digest, long lengthFromEnd, byte[] extraData, Logger logger) {
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

                    int readBytes;
                    digest.reset();

                    while (size > 0) {
                        //noinspection NumericCastThatLosesPrecision
                        int maxToRead = (int) Math.min(bufferSize, size);
                        readBytes = inputStream.read(buffer, 0, maxToRead);
                        size -= readBytes;

                        if (readBytes == 0) {
                            //wtf. finally still gets called.
                            return null;
                        }

                        digest.update(buffer, 0, readBytes);
                    }
                } catch (Exception e) {
                    if (logger != null) {
                        logger.error("Error hashing file: {}", file.getAbsolutePath(), e);
                    }
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

        /**
         * Specifically, to return the hash of the ALL files/directories inside the jar, minus the action specified (LGPL) files.
         */
        public static
        byte[] hashJarContentsExcludeAction(File jarDestFilename, Digest digest, int action) throws IOException {
            JarFile jarDestFile = new JarFile(jarDestFilename);

            try {
                Enumeration<JarEntry> jarElements = jarDestFile.entries();

                boolean okToHash;
                boolean hasAction;
                byte[] buffer = new byte[2048];
                int read;
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
            int read;
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
         * @param password
         *                 will be filled with '*'
         * @param salt
         *                 should be a RANDOM number, at least 256bits (32 bytes) in size.
         * @param iterationCount
         *                 should be a lot, like 10,000
         *
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
         * @param password
         *                 The password that you want to mix
         * @param salt
         *                 should be a RANDOM number, at least 256bits (32 bytes) in size.
         * @param iterationCount
         *                 should be a lot, like 10,000
         *
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
                //noinspection NumericCastThatLosesPrecision
                passwordBytes[2 * i] = (byte) (((int) password[i] & 0xFF00) >> 8);
                //noinspection NumericCastThatLosesPrecision
                passwordBytes[2 * i + 1] = (byte) ((int) password[i] & 0x00FF);
            }

            // asterisk out the password
            Arrays.fill(password, '*');

            return passwordBytes;
        }


        private
        Util() {
        }
    }
}
