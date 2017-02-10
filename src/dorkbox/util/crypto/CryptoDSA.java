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

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.DSAKeyPairGenerator;
import org.bouncycastle.crypto.generators.DSAParametersGenerator;
import org.bouncycastle.crypto.params.DSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.DSAParameters;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.DSASigner;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * this is here just for keeping track of how this is done. This should correct and working, but should NOT be used, and instead use ECC
 * crypto.
 */
 @Deprecated
public final
class CryptoDSA {
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

        return dsa.generateSignature(checksum);
    }

    /**
     * The message will have the SHA1 hash calculated and used for the signature.
     * <p/>
     * Note: this is here just for keeping track of how this is done. This should NOT be used, and instead use ECC crypto.
     *
     * @param signature
     *                 is the {r,s} signature array.
     *
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


    private
    CryptoDSA() {
    }
}
