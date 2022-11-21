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
package dorkbox.util.crypto;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.junit.Test;

public class AesTest {

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());
    private static String entropySeed = "asdjhasdkljalksdfhlaks4356268909087s0dfgkjh255124515hasdg87";

    @Test
    public void AesGcm() throws IOException {
        byte[] bytes = "hello, my name is inigo montoya".getBytes();

        SecureRandom rand = new SecureRandom(entropySeed.getBytes());

        GCMBlockCipher aesEngine = new GCMBlockCipher(new AESEngine());

        byte[] key = new byte[32];
        byte[] iv = new byte[16];

        // note: the IV needs to be VERY unique!
        rand.nextBytes(key);  // 256bit key (32 bytes)
        rand.nextBytes(iv); // 128bit block size (16 bytes)


        byte[] encryptAES = CryptoAES.encrypt(aesEngine, key, iv, bytes, logger);
        byte[] decryptAES = CryptoAES.decrypt(aesEngine, key, iv, encryptAES, logger);

        if (Arrays.equals(bytes, encryptAES)) {
            fail("bytes should not be equal");
        }

        if (!Arrays.equals(bytes, decryptAES)) {
            fail("bytes not equal");
        }
    }

    // Note: this is still tested, but DO NOT USE BLOCK MODE as it does NOT provide authentication. GCM does.
    @SuppressWarnings("deprecation")
    @Test
    public void AesBlock() throws IOException {
        byte[] bytes = "hello, my name is inigo montoya".getBytes();

        SecureRandom rand = new SecureRandom(entropySeed.getBytes());

        PaddedBufferedBlockCipher aesEngine = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));

        byte[] key = new byte[32];
        byte[] iv = new byte[16];

        // note: the IV needs to be VERY unique!
        rand.nextBytes(key);  // 256bit key
        rand.nextBytes(iv); // 16bit block size


        byte[] encryptAES = CryptoAES.encrypt(aesEngine, key, iv, bytes, logger);
        byte[] decryptAES = CryptoAES.decrypt(aesEngine, key, iv, encryptAES, logger);

        if (Arrays.equals(bytes, encryptAES)) {
            fail("bytes should not be equal");
        }

        if (!Arrays.equals(bytes, decryptAES)) {
            fail("bytes not equal");
        }
    }

    @Test
    public void AesGcmStream() throws IOException {
        byte[] originalBytes = "hello, my name is inigo montoya".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(originalBytes);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        SecureRandom rand = new SecureRandom(entropySeed.getBytes());

        GCMBlockCipher aesEngine = new GCMBlockCipher(new AESEngine());

        byte[] key = new byte[32];
        byte[] iv = new byte[16];

        // note: the IV needs to be VERY unique!
        rand.nextBytes(key);  // 256bit key
        rand.nextBytes(iv); // 128bit block size


        boolean success = CryptoAES.encryptStream(aesEngine, key, iv, inputStream, outputStream, logger);

        if (!success) {
            fail("crypto was not successful");
        }

        byte[] encryptBytes = outputStream.toByteArray();

        inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        outputStream = new ByteArrayOutputStream();

        success = CryptoAES.decryptStream(aesEngine, key, iv, inputStream, outputStream, logger);

        if (!success) {
            fail("crypto was not successful");
        }

        byte[] decryptBytes = outputStream.toByteArray();

        if (Arrays.equals(originalBytes, encryptBytes)) {
            fail("bytes should not be equal");
        }

        if (!Arrays.equals(originalBytes, decryptBytes)) {
            fail("bytes not equal");
        }
    }

    // Note: this is still tested, but DO NOT USE BLOCK MODE as it does NOT provide authentication. GCM does.
    @SuppressWarnings("deprecation")
    @Test
    public void AesBlockStream() throws IOException {
        byte[] originalBytes = "hello, my name is inigo montoya".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(originalBytes);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        SecureRandom rand = new SecureRandom(entropySeed.getBytes());

        PaddedBufferedBlockCipher aesEngine = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));

        byte[] key = new byte[32];
        byte[] iv = new byte[16];

        // note: the IV needs to be VERY unique!
        rand.nextBytes(key);  // 256bit key
        rand.nextBytes(iv); // 128bit block size


        boolean success = CryptoAES.encryptStream(aesEngine, key, iv, inputStream, outputStream, logger);

        if (!success) {
            fail("crypto was not successful");
        }

        byte[] encryptBytes = outputStream.toByteArray();

        inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        outputStream = new ByteArrayOutputStream();

        success = CryptoAES.decryptStream(aesEngine, key, iv, inputStream, outputStream, logger);


        if (!success) {
            fail("crypto was not successful");
        }

        byte[] decryptBytes = outputStream.toByteArray();

        if (Arrays.equals(originalBytes, encryptBytes)) {
            fail("bytes should not be equal");
        }

        if (!Arrays.equals(originalBytes, decryptBytes)) {
            fail("bytes not equal");
        }
    }

    @Test
    public void AesWithIVGcm() throws IOException {
        byte[] bytes = "hello, my name is inigo montoya".getBytes();

        SecureRandom rand = new SecureRandom(entropySeed.getBytes());

        GCMBlockCipher aesEngine = new GCMBlockCipher(new AESEngine());

        byte[] key = new byte[32]; // 256bit key
        byte[] iv = new byte[aesEngine.getUnderlyingCipher().getBlockSize()];

        // note: the IV needs to be VERY unique!
        rand.nextBytes(key);
        rand.nextBytes(iv);


        byte[] encryptAES = CryptoAES.encryptWithIV(aesEngine, key, iv, bytes, logger);
        byte[] decryptAES = CryptoAES.decryptWithIV(aesEngine, key, encryptAES, logger);

        if (Arrays.equals(bytes, encryptAES)) {
            fail("bytes should not be equal");
        }

        if (!Arrays.equals(bytes, decryptAES)) {
            fail("bytes not equal");
        }
    }

    // Note: this is still tested, but DO NOT USE BLOCK MODE as it does NOT provide authentication. GCM does.
    @SuppressWarnings("deprecation")
    @Test
    public void AesWithIVBlock() throws IOException {
        byte[] bytes = "hello, my name is inigo montoya".getBytes();

        SecureRandom rand = new SecureRandom(entropySeed.getBytes());

        PaddedBufferedBlockCipher aesEngine = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));

        byte[] key = new byte[32]; // 256bit key
        byte[] iv = new byte[aesEngine.getUnderlyingCipher().getBlockSize()];

        // note: the IV needs to be VERY unique!
        rand.nextBytes(key);
        rand.nextBytes(iv);


        byte[] encryptAES = CryptoAES.encryptWithIV(aesEngine, key, iv, bytes, logger);
        byte[] decryptAES = CryptoAES.decryptWithIV(aesEngine, key, encryptAES, logger);

        if (Arrays.equals(bytes, encryptAES)) {
            fail("bytes should not be equal");
        }

        if (!Arrays.equals(bytes, decryptAES)) {
            fail("bytes not equal");
        }
    }

    @Test
    public void AesWithIVGcmStream() throws IOException {
        byte[] originalBytes = "hello, my name is inigo montoya".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(originalBytes);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        SecureRandom rand = new SecureRandom(entropySeed.getBytes());

        GCMBlockCipher aesEngine = new GCMBlockCipher(new AESEngine());

        byte[] key = new byte[32];
        byte[] iv = new byte[16];

        // note: the IV needs to be VERY unique!
        rand.nextBytes(key);  // 256bit key
        rand.nextBytes(iv); // 128bit block size


        boolean success = CryptoAES.encryptStreamWithIV(aesEngine, key, iv, inputStream, outputStream, logger);

        if (!success) {
            fail("crypto was not successful");
        }

        byte[] encryptBytes = outputStream.toByteArray();

        inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        outputStream = new ByteArrayOutputStream();

        success = CryptoAES.decryptStreamWithIV(aesEngine, key, inputStream, outputStream, logger);

        if (!success) {
            fail("crypto was not successful");
        }

        byte[] decryptBytes = outputStream.toByteArray();

        if (Arrays.equals(originalBytes, encryptBytes)) {
            fail("bytes should not be equal");
        }

        if (!Arrays.equals(originalBytes, decryptBytes)) {
            fail("bytes not equal");
        }
    }

    // Note: this is still tested, but DO NOT USE BLOCK MODE as it does NOT provide authentication. GCM does.
    @SuppressWarnings("deprecation")
    @Test
    public void AesWithIVBlockStream() throws IOException {
        byte[] originalBytes = "hello, my name is inigo montoya".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(originalBytes);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        SecureRandom rand = new SecureRandom(entropySeed.getBytes());

        PaddedBufferedBlockCipher aesEngine = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));

        byte[] key = new byte[32];
        byte[] iv = new byte[16];

        // note: the IV needs to be VERY unique!
        rand.nextBytes(key);  // 256bit key
        rand.nextBytes(iv); // 128bit block size


        boolean success = CryptoAES.encryptStreamWithIV(aesEngine, key, iv, inputStream, outputStream, logger);

        if (!success) {
            fail("crypto was not successful");
        }

        byte[] encryptBytes = outputStream.toByteArray();

        inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        outputStream = new ByteArrayOutputStream();

        success = CryptoAES.decryptStreamWithIV(aesEngine, key, inputStream, outputStream, logger);


        if (!success) {
            fail("crypto was not successful");
        }

        byte[] decryptBytes = outputStream.toByteArray();

        if (Arrays.equals(originalBytes, encryptBytes)) {
            fail("bytes should not be equal");
        }

        if (!Arrays.equals(originalBytes, decryptBytes)) {
            fail("bytes not equal");
        }
    }
}
