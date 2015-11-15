package dorkbox.util.crypto;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.signers.PSSSigner;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;

/**
 * This is here just for keeping track of how this is done. This should NOT be used, and instead use ECC crypto.
 */
 @SuppressWarnings("unused")
 @Deprecated
public final
class CryptoRSA {
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
     * byte[0][] = encrypted data byte[1][] = signature
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return empty byte[][] if error
     */
    public static
    byte[][] encryptAndSign(AsymmetricBlockCipher rsaEngine,
                            Digest digest,
                            RSAKeyParameters rsaPublicKeyA,
                            RSAPrivateCrtKeyParameters rsaPrivateKeyB,
                            byte[] bytes,
                            Logger logger) {
        if (bytes.length == 0) {
            return new byte[0][0];
        }

        byte[] encryptBytes = encrypt(rsaEngine, rsaPublicKeyA, bytes, logger);

        if (encryptBytes.length == 0) {
            return new byte[0][0];
        }

        // now sign it.
        PSSSigner signer = new PSSSigner(rsaEngine, digest, digest.getDigestSize());

        byte[] signatureRSA = CryptoRSA.sign(signer, rsaPrivateKeyB, encryptBytes, logger);

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
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return empty byte[] if error
     */
    public static
    byte[] decryptAndVerify(AsymmetricBlockCipher rsaEngine,
                            Digest digest,
                            RSAKeyParameters rsaPublicKeyA,
                            RSAPrivateCrtKeyParameters rsaPrivateKeyB,
                            byte[] encryptedData,
                            byte[] signature,
                            Logger logger) {
        if (encryptedData.length == 0 || signature.length == 0) {
            return new byte[0];
        }

        // verify encrypted data.
        PSSSigner signer = new PSSSigner(rsaEngine, digest, digest.getDigestSize());

        boolean verify = verify(signer, rsaPublicKeyA, signature, encryptedData);
        if (!verify) {
            return new byte[0];
        }

        return decrypt(rsaEngine, rsaPrivateKeyB, encryptedData, logger);

    }

    /**
     * RSA encrypts data with a specified key.
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return empty byte[] if error
     */
    public static
    byte[] encrypt(AsymmetricBlockCipher rsaEngine, RSAKeyParameters rsaPublicKey, byte[] bytes, Logger logger) {
        rsaEngine.init(true, rsaPublicKey);

        try {
            int inputBlockSize = rsaEngine.getInputBlockSize();
            if (inputBlockSize < bytes.length) {
                int outSize = rsaEngine.getOutputBlockSize();
                //noinspection NumericCastThatLosesPrecision
                int realsize = (int) Math.round(bytes.length / (outSize * 1.0D) + 0.5);
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
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Unable to perform RSA cipher.", e);
            }
            return new byte[0];
        }
    }

    /**
     * RSA decrypt data with a specified key.
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return empty byte[] if error
     */
    public static
    byte[] decrypt(AsymmetricBlockCipher rsaEngine, RSAPrivateCrtKeyParameters rsaPrivateKey, byte[] bytes, Logger logger) {
        rsaEngine.init(false, rsaPrivateKey);

        try {
            int inputBlockSize = rsaEngine.getInputBlockSize();
            if (inputBlockSize < bytes.length) {
                int outSize = rsaEngine.getOutputBlockSize();
                //noinspection NumericCastThatLosesPrecision
                int realsize = (int) Math.round(bytes.length / (outSize * 1.0D) + 0.5);
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
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Unable to perform RSA cipher.", e);
            }
            return new byte[0];
        }
    }

    /**
     * RSA sign data with a specified key.
     *
     * @param logger
     *                 may be null, if no log output is necessary
     *
     * @return empty byte[] if error
     */
    public static
    byte[] sign(PSSSigner signer, RSAPrivateCrtKeyParameters rsaPrivateKey, byte[] mesg, Logger logger) {
        signer.init(true, rsaPrivateKey);
        signer.update(mesg, 0, mesg.length);

        try {
            return signer.generateSignature();
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Unable to perform RSA cipher.", e);
            }
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

    @SuppressWarnings("RedundantIfStatement")
    public static
    boolean compare(RSAKeyParameters publicA, RSAKeyParameters publicB) {
        if (!publicA.getExponent()
                    .equals(publicB.getExponent())) {
            return false;
        }
        if (!publicA.getModulus()
                    .equals(publicB.getModulus())) {
            return false;
        }

        return true;
    }

    @SuppressWarnings("RedundantIfStatement")
    public static
    boolean compare(RSAPrivateCrtKeyParameters private1, RSAPrivateCrtKeyParameters private2) {
        if (!private1.getModulus()
                     .equals(private2.getModulus())) {
            return false;
        }
        if (!private1.getExponent()
                     .equals(private2.getExponent())) {
            return false;
        }
        if (!private1.getDP()
                     .equals(private2.getDP())) {
            return false;
        }
        if (!private1.getDQ()
                     .equals(private2.getDQ())) {
            return false;
        }
        if (!private1.getP()
                     .equals(private2.getP())) {
            return false;
        }
        if (!private1.getPublicExponent()
                     .equals(private2.getPublicExponent())) {
            return false;
        }
        if (!private1.getQ()
                     .equals(private2.getQ())) {
            return false;
        }
        if (!private1.getQInv()
                     .equals(private2.getQInv())) {
            return false;
        }

        return true;
    }


    private
    CryptoRSA() {
    }
}
