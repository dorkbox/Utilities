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

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * An implementation of the <a href="http://www.tarsnap.com/scrypt/scrypt.pdf"/>scrypt</a> key derivation function.
 */
public final
class CryptoSCrypt {
    /**
     * Hash the supplied plaintext password and generate output using default parameters
     * <p/>
     * The password chars are no longer valid after this call
     *
     * @param password
     *                 Password.
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
     * @param password
     *                 Password.
     * @param salt
     *                 Salt parameter
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
     * @param password
     *                 Password.
     * @param N
     *                 CPU cost parameter.
     * @param r
     *                 Memory cost parameter.
     * @param p
     *                 Parallelization parameter.
     *
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
     * @param password
     *                 Password.
     * @param salt
     *                 Salt parameter
     * @param N
     *                 CPU cost parameter.
     * @param r
     *                 Memory cost parameter.
     * @param p
     *                 Parallelization parameter.
     * @param dkLen
     *                 Intended length of the derived key.
     *
     * @return The hashed password.
     */
    public static
    String encrypt(char[] password, byte[] salt, int N, int r, int p, int dkLen) {
        // Note: this saves the char array in UTF-16 format of bytes.
        // can't use password after this as it's been changed to '*'
        byte[] passwordBytes = Crypto.charToBytesPassword_UTF16(password);

        byte[] derived = encrypt(passwordBytes, salt, N, r, p, dkLen);

        String params = Integer.toString(log2(N) << 16 | r << 8 | p, 16);

        @SuppressWarnings("StringBufferReplaceableByString")
        StringBuilder sb = new StringBuilder((salt.length + derived.length) * 2);
        sb.append("$s0$")
          .append(params)
          .append('$');
        sb.append(dorkbox.util.Base64Fast.encodeToString(salt, false))
          .append('$');
        sb.append(dorkbox.util.Base64Fast.encodeToString(derived, false));

        return sb.toString();
    }

    /**
     * Compare the supplied plaintext password to a hashed password.
     *
     * @param password
     *                 Plaintext password.
     * @param hashed
     *                 scrypt hashed password.
     *
     * @return true if password matches hashed value.
     */
    public static
    boolean verify(char[] password, String hashed) {
        // Note: this saves the char array in UTF-16 format of bytes.
        // can't use password after this as it's been changed to '*'
        byte[] passwordBytes = Crypto.charToBytesPassword_UTF16(password);

        String[] parts = hashed.split("\\$");

        if (parts.length != 5 || !parts[1].equals("s0")) {
            throw new IllegalArgumentException("Invalid hashed value");
        }

        int params = Integer.parseInt(parts[2], 16);
        byte[] salt = dorkbox.util.Base64Fast.decodeFast(parts[3]);
        byte[] derived0 = dorkbox.util.Base64Fast.decodeFast(parts[4]);

        //noinspection NumericCastThatLosesPrecision
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
     * @param password
     *                 Password.
     * @param salt
     *                 Salt.
     * @param N
     *                 CPU cost parameter.
     * @param r
     *                 Memory cost parameter.
     * @param p
     *                 Parallelization parameter.
     * @param dkLen
     *                 Intended length of the derived key.
     *
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

    private
    CryptoSCrypt() {
    }
}
