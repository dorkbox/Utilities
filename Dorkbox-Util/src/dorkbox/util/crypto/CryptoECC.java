package dorkbox.util.crypto;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.agreement.ECDHCBasicAgreement;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.engines.IESEngine;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.generators.KDF2BytesGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.IESParameters;
import org.bouncycastle.crypto.params.IESWithCipherParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jcajce.provider.util.DigestFactory;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;
import org.slf4j.Logger;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * ECC crypto functions
 */
public final
class CryptoECC {
    public static final String p521_curve = "secp521r1";
    public static final int macSize = 512;
    // more info about ECC from: http://www.johannes-bauer.com/compsci/ecc/?menuid=4
    // http://stackoverflow.com/questions/7419183/problems-implementing-ecdh-on-android-using-bouncycastle
    // http://tools.ietf.org/html/draft-jivsov-openpgp-ecc-06#page-4
    // http://www.nsa.gov/ia/programs/suiteb_cryptography/
    // https://github.com/nelenkov/ecdh-kx/blob/master/src/org/nick/ecdhkx/Crypto.java
    // http://nelenkov.blogspot.com/2011/12/using-ecdh-on-android.html
    // http://www.secg.org/collateral/sec1_final.pdf
    static final String ECC_NAME = "EC";

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
        return new IESEngine(new ECDHCBasicAgreement(),
                             new KDF2BytesGenerator(new SHA384Digest()),
                             new HMac(new SHA512Digest()),
                             aesEngine);
    }

    /**
     * These parameters are shared between the two parties. These are a NONCE (use ONCE number!!)
     */
    public static
    IESParameters generateSharedParameters(SecureRandom secureRandom) {

        int macSize = CryptoECC.macSize; // must be the MAC size

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
        int macSize = CryptoECC.macSize; // must be the MAC size

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
        ECKeyGenerationParameters ecParams = new ECKeyGenerationParameters(new ECDomainParameters(eccSpec.getCurve(),
                                                                                                  eccSpec.getG(),
                                                                                                  eccSpec.getN()), secureRandom);

        ECKeyPairGenerator ecKeyGen = new ECKeyPairGenerator();
        ecKeyGen.init(ecParams);

        return ecKeyGen.generateKeyPair();
    }

    /**
     * ECC encrypts data with a specified key.
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return empty byte[] if error
     */
    public static
    byte[] encrypt(IESEngine eccEngine,
                   CipherParameters private1,
                   CipherParameters public2,
                   IESParameters cipherParams,
                   byte[] message,
                   Logger logger) {

        eccEngine.init(true, private1, public2, cipherParams);

        //noinspection Duplicates
        try {
            return eccEngine.processBlock(message, 0, message.length);
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Unable to perform ECC cipher.", e);
            }
            return new byte[0];
        }
    }

    /**
     * ECC decrypt data with a specified key.
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return empty byte[] if error
     */
    public static
    byte[] decrypt(IESEngine eccEngine,
                   CipherParameters private2,
                   CipherParameters public1,
                   IESParameters cipherParams,
                   byte[] encrypted,
                   Logger logger) {

        eccEngine.init(false, private2, public1, cipherParams);

        //noinspection Duplicates
        try {
            return eccEngine.processBlock(encrypted, 0, encrypted.length);
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Unable to perform ECC cipher.", e);
            }
            return new byte[0];
        }
    }

    public static
    boolean compare(ECPrivateKeyParameters privateA, ECPrivateKeyParameters privateB) {
        ECDomainParameters parametersA = privateA.getParameters();
        ECDomainParameters parametersB = privateB.getParameters();

        // is it the same curve?
        boolean equals = parametersA.getCurve()
                                    .equals(parametersB.getCurve());
        if (!equals) {
            return false;
        }

        equals = parametersA.getG()
                            .equals(parametersB.getG());
        if (!equals) {
            return false;
        }


        equals = parametersA.getH()
                            .equals(parametersB.getH());
        if (!equals) {
            return false;
        }

        equals = parametersA.getN()
                            .equals(parametersB.getN());
        if (!equals) {
            return false;
        }

        equals = privateA.getD()
                         .equals(privateB.getD());

        return equals;
    }

    /**
     * @return true if publicA and publicB are NOT NULL, and are both equal to eachother
     */
    @SuppressWarnings({"RedundantIfStatement", "SpellCheckingInspection"})
    public static
    boolean compare(ECPublicKeyParameters publicA, ECPublicKeyParameters publicB) {
        if (publicA == null || publicB == null) {
            return false;
        }


        ECDomainParameters parametersA = publicA.getParameters();
        ECDomainParameters parametersB = publicB.getParameters();

        // is it the same curve?
        boolean equals = parametersA.getCurve()
                                    .equals(parametersB.getCurve());
        if (!equals) {
            return false;
        }

        equals = parametersA.getG()
                            .equals(parametersB.getG());
        if (!equals) {
            return false;
        }


        equals = parametersA.getH()
                            .equals(parametersB.getH());
        if (!equals) {
            return false;
        }

        equals = parametersA.getN()
                            .equals(parametersB.getN());
        if (!equals) {
            return false;
        }


        ECPoint normalizeA = publicA.getQ()
                                    .normalize();
        ECPoint normalizeB = publicB.getQ()
                                    .normalize();


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

    @SuppressWarnings("RedundantIfStatement")
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
     * @param signature
     *                 is the {r,s} signature array.
     *
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
     * @param signature
     *                 is the {r,s} signature array.
     *
     * @return true if the signature is valid
     */
    public static
    boolean verifySignatureHash(ECPublicKeyParameters publicKey, byte[] hash, BigInteger[] signature) {

        ECDSASigner ecdsa = new ECDSASigner();
        ecdsa.init(false, publicKey);


        return ecdsa.verifySignature(hash, signature[0], signature[1]);
    }

    private
    CryptoECC() {
    }
}
