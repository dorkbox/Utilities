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

import java.security.SecureRandom;
import java.util.Random;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;


// See: https://stackoverflow.com/questions/25992131/slow-aes-gcm-encryption-and-decryption-with-java-8u20
// java8 performance is 3 MB/s. BC is ~43 MB/s
public
class PerformanceTest {
    private static  org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PerformanceTest.class);
    private static String entropySeed = "asdjhasdkljalksdfhlaks4356268909087s0dfgkjh255124515hasdg87";

    public static
    void main(String[] args) throws Exception {
        final int max = 5;
        for (int i = 0; i < max; i++) {
            System.out.println("Warming up " + (i+1) + " of " + max);
            new PerformanceTest(true);
        }
        new PerformanceTest(false);
    }

    PerformanceTest(boolean isWarmup) {
        final byte[] bytes = new byte[64 * 1024];
        byte[] encrypted = null;
        final byte[] aesKey = new byte[32];
        final byte[] aesIV = new byte[12];

        final Random random = new SecureRandom(entropySeed.getBytes());
        random.nextBytes(bytes);
        random.nextBytes(aesKey);
        random.nextBytes(aesIV);

        int length = bytes.length;

        if (!isWarmup) {
            System.out.println("Benchmarking AES-256 GCM encryption");
        }

        long javaEncryptInputBytes = 0;
        long javaEncryptStartTime = System.currentTimeMillis();

        // convert to bouncycastle
        GCMBlockCipher aesEngine = new GCMBlockCipher(new AESFastEngine());
        CipherParameters aesIVAndKey = new ParametersWithIV(new KeyParameter(aesKey), aesIV);

        long encryptInitTime = 0L;
        long encryptUpdate1Time = 0L;
        long encryptDoFinalTime = 0L;

        while (System.currentTimeMillis() - javaEncryptStartTime < 10000) {
            random.nextBytes(aesIV);

            long n1 = System.nanoTime();

            aesEngine.reset();
            aesEngine.init(true, aesIVAndKey);

            if (encrypted == null) {
                int minSize = aesEngine.getOutputSize(length);
                encrypted = new byte[minSize];
            }

            long n2 = System.nanoTime();
            int actualLength = aesEngine.processBytes(bytes, 0, length, encrypted, 0);

            long n3 = System.nanoTime();
            try {
                actualLength += aesEngine.doFinal(encrypted, actualLength);
            } catch (Exception e) {
                logger.error("Unable to perform AES cipher.", e);
            }

            if (encrypted.length != actualLength) {
                byte[] result = new byte[actualLength];
                System.arraycopy(encrypted, 0, result, 0, result.length);
                encrypted = result;
            }

            long n4 = System.nanoTime();

            javaEncryptInputBytes += actualLength;

            encryptInitTime = n2 - n1;
            encryptUpdate1Time = n3 - n2;
            encryptDoFinalTime = n4 - n3;
        }

        long javaEncryptEndTime = System.currentTimeMillis();

        if (!isWarmup) {
            System.out.println("Time init (ns): " + encryptInitTime);
            System.out.println("Time update (ns): " + encryptUpdate1Time);
            System.out.println("Time do final (ns): " + encryptDoFinalTime);
            System.out.println("Java calculated at " +
                               (javaEncryptInputBytes / 1024 / 1024 / ((javaEncryptEndTime - javaEncryptStartTime) / 1000)) + " MB/s");

            System.out.println("Benchmarking AES-256 GCM decryption");
        }


        long javaDecryptInputBytes = 0;
        long javaDecryptStartTime = System.currentTimeMillis();

        length = encrypted.length;

        long decryptInitTime = 0L;
        long decryptUpdate1Time = 0L;
        long decryptDoFinalTime = 0L;

        while (System.currentTimeMillis() - javaDecryptStartTime < 10000) {
            long n1 = System.nanoTime();

            aesEngine.reset();
            aesEngine.init(false, aesIVAndKey);

            long n2 = System.nanoTime();

            int actualLength = aesEngine.processBytes(encrypted, 0, length, bytes, 0);

            long n3 = System.nanoTime();

            try {
                actualLength += aesEngine.doFinal(bytes, actualLength);
            } catch (Exception e) {
                logger.debug("Unable to perform AES cipher.", e);
            }


            long n4 = System.nanoTime();

            javaDecryptInputBytes += actualLength;

            decryptInitTime += n2 - n1;
            decryptUpdate1Time += n3 - n2;
            decryptDoFinalTime += n4 - n3;
        }
        long javaDecryptEndTime = System.currentTimeMillis();

        if (!isWarmup) {
            System.out.println("Time init (ns): " + decryptInitTime);
            System.out.println("Time update 1 (ns): " + decryptUpdate1Time);
            System.out.println("Time do final (ns): " + decryptDoFinalTime);
            System.out.println("Total bytes processed: " + javaDecryptInputBytes);
            System.out.println("Java calculated at " +
                               (javaDecryptInputBytes / 1024 / 1024 / ((javaDecryptEndTime - javaDecryptStartTime) / 1000)) + " MB/s");
        }
    }
}
