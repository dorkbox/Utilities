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
import org.bouncycastle.crypto.BasicAgreement;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.agreement.ECDHCBasicAgreement;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.engines.IESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.IESParameters;
import org.bouncycastle.crypto.params.IESWithCipherParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import dorkbox.util.serialization.EccPrivateKeySerializer;
import dorkbox.util.serialization.EccPublicKeySerializer;
import dorkbox.util.serialization.IesParametersSerializer;
import dorkbox.util.serialization.IesWithCipherParametersSerializer;


public class EccTest {

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());
    private static String entropySeed = "asdjhaffasttjasdasdgfgaerym0698768.,./8909087s0dfgkjgb49bmngrSGDSG#";

    @Test
    public void EccStreamMode() throws IOException {
        SecureRandom secureRandom = new SecureRandom();

        AsymmetricCipherKeyPair key1 = CryptoECC.generateKeyPair(CryptoECC.default_curve, secureRandom);
        AsymmetricCipherKeyPair key2 = CryptoECC.generateKeyPair(CryptoECC.default_curve, secureRandom);

        IESParameters cipherParams = CryptoECC.generateSharedParameters(secureRandom);

        IESEngine encrypt = CryptoECC.createEngine();
        IESEngine decrypt = CryptoECC.createEngine();


        // note: we want an ecc key that is AT LEAST 512 bits! (which is equal to AES 256)
        // using 521 bits from curve.
        CipherParameters private1 = key1.getPrivate();
        CipherParameters public1 = key1.getPublic();

        CipherParameters private2 = key2.getPrivate();
        CipherParameters public2 = key2.getPublic();

        byte[] message = Hex.decode(
                        "123456784358754934597967249867359283792374987692348750276509765091834790abcdef123456784358754934597967249867359283792374987692348750276509765091834790abcdef123456784358754934597967249867359283792374987692348750276509765091834790abcdef");


        // test stream mode
        byte[] encrypted = CryptoECC.encrypt(encrypt, private1, public2, cipherParams, message, logger);
        byte[] plaintext = CryptoECC.decrypt(decrypt, private2, public1, cipherParams, encrypted, logger);

        if (Arrays.equals(encrypted, message)) {
            fail("stream cipher test failed");
        }

        if (!Arrays.equals(plaintext, message)) {
            fail("stream cipher test failed");
        }
    }

    @Test
    public void EccAesMode() throws IOException {
        // test AES encrypt mode
        SecureRandom secureRandom = new SecureRandom();

        AsymmetricCipherKeyPair key1 = CryptoECC.generateKeyPair(CryptoECC.default_curve, secureRandom);
        AsymmetricCipherKeyPair key2 = CryptoECC.generateKeyPair(CryptoECC.default_curve, secureRandom);


        PaddedBufferedBlockCipher aesEngine1 = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));
        PaddedBufferedBlockCipher aesEngine2 = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESFastEngine()));

        IESWithCipherParameters cipherParams = CryptoECC.generateSharedParametersWithCipher(secureRandom);


        IESEngine encrypt = CryptoECC.createEngine(aesEngine1);
        IESEngine decrypt = CryptoECC.createEngine(aesEngine2);


        // note: we want an ecc key that is AT LEAST 512 bits! (which is equal to AES 256)
        // using 521 bits from curve.
        CipherParameters private1 = key1.getPrivate();
        CipherParameters public1 = key1.getPublic();

        CipherParameters private2 = key2.getPrivate();
        CipherParameters public2 = key2.getPublic();

        byte[] message = Hex.decode("123456784358754934597967249867359283792374987692348750276509765091834790abcdef123456784358754934597967249867359283792374987692348750276509765091834790abcdef123456784358754934597967249867359283792374987692348750276509765091834790abcdef");

        // test stream mode
        byte[] encrypted = CryptoECC.encrypt(encrypt, private1, public2, cipherParams, message, logger);
        byte[] plaintext = CryptoECC.decrypt(decrypt, private2, public1, cipherParams, encrypted, logger);

        if (Arrays.equals(encrypted, message)) {
            fail("stream cipher test failed");
        }

        if (!Arrays.equals(plaintext, message)) {
            fail("stream cipher test failed");
        }
    }

    @Test
    public void Ecdh() throws IOException {
        // test DH key exchange
        SecureRandom secureRandom = new SecureRandom();

        AsymmetricCipherKeyPair key1 = CryptoECC.generateKeyPair(CryptoECC.default_curve, secureRandom);
        AsymmetricCipherKeyPair key2 = CryptoECC.generateKeyPair(CryptoECC.default_curve, secureRandom);

        BasicAgreement e1 = new ECDHCBasicAgreement();
        BasicAgreement e2 = new ECDHCBasicAgreement();

        e1.init(key1.getPrivate());
        e2.init(key2.getPrivate());

        BigInteger   k1 = e1.calculateAgreement(key2.getPublic());
        BigInteger   k2 = e2.calculateAgreement(key1.getPublic());

        if (!k1.equals(k2)) {
            fail("ECDHC cipher test failed");
        }
    }

    @Test
    public void EccDsa() throws IOException {
        SecureRandom secureRandom = new SecureRandom();

        AsymmetricCipherKeyPair key1 = CryptoECC.generateKeyPair(CryptoECC.default_curve, secureRandom);

        ParametersWithRandom param = new ParametersWithRandom(key1.getPrivate(), new SecureRandom());

        ECDSASigner ecdsa = new ECDSASigner();

        ecdsa.init(true, param);

        byte[] message = new BigInteger("345234598734987394672039478602934578").toByteArray();
        BigInteger[] sig = ecdsa.generateSignature(message);


        ecdsa.init(false, key1.getPublic());

        if (!ecdsa.verifySignature(message, sig[0], sig[1])) {
            fail("ECDSA signature fails");
        }
    }

    @Test
    public void EccSerialization() {
        SecureRandom secureRandom = new SecureRandom();

        AsymmetricCipherKeyPair key1 = CryptoECC.generateKeyPair(CryptoECC.default_curve, secureRandom);

        IESParameters cipherAParams = CryptoECC.generateSharedParameters(secureRandom);
        IESWithCipherParameters cipherBParams = CryptoECC.generateSharedParametersWithCipher(secureRandom);


        // note: we want an ecc key that is AT LEAST 512 bits! (which is equal to AES 256)
        // using 521 bits from curve.
        ECPrivateKeyParameters private1 = (ECPrivateKeyParameters) key1.getPrivate();
        ECPublicKeyParameters public1 = (ECPublicKeyParameters) key1.getPublic();


        Kryo kryo = new Kryo();
        kryo.register(IESParameters.class, new IesParametersSerializer());
        kryo.register(IESWithCipherParameters.class, new IesWithCipherParametersSerializer());
        kryo.register(ECPublicKeyParameters.class, new EccPublicKeySerializer());
        kryo.register(ECPrivateKeyParameters.class, new EccPrivateKeySerializer());



        // Test output to stream, large buffer.
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        Output output = new Output(outStream, 4096);
        kryo.writeClassAndObject(output, cipherAParams);
        output.flush();

        // Test input from stream, large buffer.
        Input input = new Input(new ByteArrayInputStream(outStream.toByteArray()), 4096);
        IESParameters cipherAParams2 = (IESParameters) kryo.readClassAndObject(input);


        if (!CryptoECC.compare(cipherAParams, cipherAParams2)) {
            fail("cipher parameters not equal");
        }

        // Test output to stream, large buffer.
        outStream = new ByteArrayOutputStream();
        output = new Output(outStream, 4096);
        kryo.writeClassAndObject(output, cipherBParams);
        output.flush();

        // Test input from stream, large buffer.
        input = new Input(new ByteArrayInputStream(outStream.toByteArray()), 4096);
        IESWithCipherParameters cipherBParams2 = (IESWithCipherParameters) kryo.readClassAndObject(input);

        if (!CryptoECC.compare(cipherBParams, cipherBParams2)) {
            fail("cipher parameters not equal");
        }


        // Test output to stream, large buffer.
        outStream = new ByteArrayOutputStream();
        output = new Output(outStream, 4096);
        kryo.writeClassAndObject(output, private1);
        output.flush();

        // Test input from stream, large buffer.
        input = new Input(new ByteArrayInputStream(outStream.toByteArray()), 4096);
        ECPrivateKeyParameters private2 = (ECPrivateKeyParameters) kryo.readClassAndObject(input);

        if (!CryptoECC.compare(private1, private2)) {
            fail("private keys not equal");
        }


        // Test output to stream, large buffer.
        outStream = new ByteArrayOutputStream();
        output = new Output(outStream, 4096);
        kryo.writeClassAndObject(output, public1);
        output.flush();

        // Test input from stream, large buffer.
        input = new Input(new ByteArrayInputStream(outStream.toByteArray()), 4096);
        ECPublicKeyParameters public2 = (ECPublicKeyParameters) kryo.readClassAndObject(input);

        if (!CryptoECC.compare(public1, public2)) {
            fail("public keys not equal");
        }
    }


    @Test
    public void EccJceSerialization() throws IOException {
        AsymmetricCipherKeyPair generateKeyPair = CryptoECC.generateKeyPair(CryptoECC.default_curve, new SecureRandom());
        ECPrivateKeyParameters privateKey = (ECPrivateKeyParameters) generateKeyPair.getPrivate();
        ECPublicKeyParameters publicKey = (ECPublicKeyParameters) generateKeyPair.getPublic();


        BCECPublicKey bcecPublicKey = new BCECPublicKey("EC", publicKey, (ECParameterSpec) null, BouncyCastleProvider.CONFIGURATION);
        byte[] publicBytes = bcecPublicKey.getEncoded();



        // relies on the BC public key.
        BCECPrivateKey bcecPrivateKey = new BCECPrivateKey("EC", privateKey, bcecPublicKey, (ECParameterSpec) null, BouncyCastleProvider.CONFIGURATION);
        byte[] privateBytes = bcecPrivateKey.getEncoded();



        ECPublicKeyParameters publicKey2 = (ECPublicKeyParameters) PublicKeyFactory.createKey(publicBytes);
        ECPrivateKeyParameters privateKey2 = (ECPrivateKeyParameters) PrivateKeyFactory.createKey(privateBytes);



        // test via signing
        byte[] bytes = "hello, my name is inigo montoya".getBytes();


        BigInteger[] signature = CryptoECC.generateSignature("SHA384", privateKey, new SecureRandom(entropySeed.getBytes()), bytes);

        boolean verify1 = CryptoECC.verifySignature("SHA384", publicKey, bytes, signature);

        if (!verify1) {
            fail("failed signature verification");
        }

        boolean verify2 = CryptoECC.verifySignature("SHA384", publicKey2, bytes, signature);

        if (!verify2) {
            fail("failed signature verification");
        }



        // now reverse who signs what.
        BigInteger[] signatureB = CryptoECC.generateSignature("SHA384", privateKey2, new SecureRandom(entropySeed.getBytes()), bytes);

        boolean verifyB1 = CryptoECC.verifySignature("SHA384", publicKey, bytes, signatureB);

        if (!verifyB1) {
            fail("failed signature verification");
        }

        boolean verifyB2 = CryptoECC.verifySignature("SHA384", publicKey2, bytes, signatureB);

        if (!verifyB2) {
            fail("failed signature verification");
        }
    }
}
