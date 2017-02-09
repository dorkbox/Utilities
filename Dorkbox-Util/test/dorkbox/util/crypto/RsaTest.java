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
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.encodings.OAEPEncoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.signers.PSSSigner;
import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import dorkbox.util.serialization.RsaPrivateKeySerializer;
import dorkbox.util.serialization.RsaPublicKeySerializer;


public class RsaTest {
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());
    private static String entropySeed = "asdjhaffasttjjhgpx600gn,-356268909087s0dfgkjh255124515hasdg87";

    @SuppressWarnings("deprecation")
    @Test
    public void Rsa() {
        byte[] bytes = "hello, my name is inigo montoya".getBytes();

        AsymmetricCipherKeyPair key = CryptoRSA.generateKeyPair(new SecureRandom(entropySeed.getBytes()), 1024);

        RSAKeyParameters public1 = (RSAKeyParameters) key.getPublic();
        RSAPrivateCrtKeyParameters private1 = (RSAPrivateCrtKeyParameters) key.getPrivate();


        RSAEngine engine = new RSAEngine();
        SHA1Digest digest = new SHA1Digest();
        OAEPEncoding rsaEngine = new OAEPEncoding(engine, digest);

        // test encrypt/decrypt
        byte[] encryptRSA = CryptoRSA.encrypt(rsaEngine, public1, bytes, logger);
        byte[] decryptRSA = CryptoRSA.decrypt(rsaEngine, private1, encryptRSA, logger);

        if (Arrays.equals(bytes, encryptRSA)) {
            fail("bytes should not be equal");
        }

        if (!Arrays.equals(bytes, decryptRSA)) {
            fail("bytes not equal");
        }

        // test signing/verification
        PSSSigner signer = new PSSSigner(engine, digest, digest.getDigestSize());

        byte[] signatureRSA = CryptoRSA.sign(signer, private1, bytes, logger);
        boolean verify = CryptoRSA.verify(signer, public1, signatureRSA, bytes);

        if (!verify) {
            fail("failed signature verification");
        }
    }


    @SuppressWarnings("deprecation")
    @Test
    public void RsaSerialization () throws IOException {
        RSAKeyPairGenerator keyGen = new RSAKeyPairGenerator();
        RSAKeyGenerationParameters params = new RSAKeyGenerationParameters(new BigInteger("65537"), // public exponent
                                                                           new SecureRandom(entropySeed.getBytes()), //pnrg
                                                                           1024, // key length
                                                                           8);  //the number of iterations of the Miller-Rabin primality test.
        keyGen.init(params);


        AsymmetricCipherKeyPair key = keyGen.generateKeyPair();

        RSAKeyParameters public1 = (RSAKeyParameters) key.getPublic();
        RSAPrivateCrtKeyParameters private1 = (RSAPrivateCrtKeyParameters) key.getPrivate();


        Kryo kryo = new Kryo();
        kryo.register(RSAKeyParameters.class, new RsaPublicKeySerializer());
        kryo.register(RSAPrivateCrtKeyParameters.class, new RsaPrivateKeySerializer());

        // Test output to stream, large buffer.
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        Output output = new Output(outStream, 4096);
        kryo.writeClassAndObject(output, public1);
        output.flush();

        // Test input from stream, large buffer.
        Input input = new Input(new ByteArrayInputStream(outStream.toByteArray()), 4096);
        RSAKeyParameters public2 = (RSAKeyParameters) kryo.readClassAndObject(input);


        if (!CryptoRSA.compare(public1, public2)) {
            fail("public keys not equal");
        }


        // Test output to stream, large buffer.
        outStream = new ByteArrayOutputStream();
        output = new Output(outStream, 4096);
        kryo.writeClassAndObject(output, private1);
        output.flush();

        // Test input from stream, large buffer.
        input = new Input(new ByteArrayInputStream(outStream.toByteArray()), 4096);
        RSAPrivateCrtKeyParameters private2 = (RSAPrivateCrtKeyParameters) kryo.readClassAndObject(input);


        if (!CryptoRSA.compare(private1, private2)) {
            fail("private keys not equal");
        }
    }
}
