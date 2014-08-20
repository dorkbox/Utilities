package dorkbox.util.crypto;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.junit.Test;


public class x509Test {

    private static String entropySeed = "asdjhaffasdgfaasttjjhgpx600gn,-356268909087s0dfg4-42kjh255124515hasdg87";

    @Test
    public void EcdsaCertificate() throws IOException {
        // create the certificate
        Calendar expiry = Calendar.getInstance();
        expiry.add(Calendar.DAY_OF_YEAR, 360);

        Date startDate = new Date();              // time from which certificate is valid
        Date expiryDate = expiry.getTime();       // time after which certificate is not valid
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());     // serial number for certificate


        AsymmetricCipherKeyPair generateKeyPair = Crypto.ECC.generateKeyPair(Crypto.ECC.p521_curve, new SecureRandom());  // key name from Crypto class
        ECPrivateKeyParameters privateKey = (ECPrivateKeyParameters) generateKeyPair.getPrivate();
        ECPublicKeyParameters publicKey = (ECPublicKeyParameters) generateKeyPair.getPublic();



        X509CertificateHolder ECDSAx509Certificate = CryptoX509.ECDSA.createCertHolder("SHA384",
                                                                                        startDate, expiryDate,
                                                                                        new X500Name("CN=Test"), new X500Name("CN=Test"), serialNumber,
                                                                                        privateKey, publicKey);
        // make sure it's a valid cert.
        if (ECDSAx509Certificate != null) {
            boolean valid = CryptoX509.ECDSA.validate(ECDSAx509Certificate);

            if (!valid) {
                fail("Unable to verify a x509 certificate.");
            }
        } else {
            fail("Unable to create a x509 certificate.");
        }

        // now sign something, then verify the signature.
        byte[] data = "My keyboard is awesome".getBytes();
        byte[] signatureBlock = CryptoX509.createSignature(data, ECDSAx509Certificate, privateKey);

        boolean verifySignature = CryptoX509.ECDSA.verifySignature(signatureBlock, publicKey);

        if (!verifySignature) {
            fail("Unable to verify a x509 certificate signature.");
        }
    }

    @Test
    public void DsaCertificate() throws IOException {
        // create the certificate
        Calendar expiry = Calendar.getInstance();
        expiry.add(Calendar.DAY_OF_YEAR, 360);

        Date startDate = new Date();              // time from which certificate is valid
        Date expiryDate = expiry.getTime();       // time after which certificate is not valid
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());     // serial number for certificate


        @SuppressWarnings("deprecation")
        AsymmetricCipherKeyPair generateKeyPair = Crypto.DSA.generateKeyPair(new SecureRandom(entropySeed.getBytes()), 1024);


        DSAPrivateKeyParameters privateKey = (DSAPrivateKeyParameters) generateKeyPair.getPrivate();
        DSAPublicKeyParameters publicKey = (DSAPublicKeyParameters) generateKeyPair.getPublic();



        X509CertificateHolder DSAx509Certificate = CryptoX509.DSA.createCertHolder(startDate, expiryDate,
                                                                                   new X500Name("CN=Test"), new X500Name("CN=Test"), serialNumber,
                                                                                   privateKey, publicKey);
        // make sure it's a valid cert.
        if (DSAx509Certificate != null) {
            boolean valid = CryptoX509.DSA.validate(DSAx509Certificate);

            if (!valid) {
                fail("Unable to verify a x509 certificate.");
            }
        } else {
            fail("Unable to create a x509 certificate.");
        }

        // now sign something, then verify the signature.
        byte[] data = "My keyboard is awesome".getBytes();
        byte[] signatureBlock = CryptoX509.createSignature(data, DSAx509Certificate, privateKey);

        boolean verifySignature = CryptoX509.DSA.verifySignature(signatureBlock, publicKey);

        if (!verifySignature) {
            fail("Unable to verify a x509 certificate signature.");
        }
    }

    @Test
    public void RsaCertificate() throws IOException {
        // create the certificate
        Calendar expiry = Calendar.getInstance();
        expiry.add(Calendar.DAY_OF_YEAR, 360);

        Date startDate = new Date();              // time from which certificate is valid
        Date expiryDate = expiry.getTime();       // time after which certificate is not valid
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());     // serial number for certificate

        @SuppressWarnings("deprecation")
        AsymmetricCipherKeyPair generateKeyPair = Crypto.RSA.generateKeyPair(new SecureRandom(entropySeed.getBytes()), 1024);
        RSAPrivateCrtKeyParameters privateKey = (RSAPrivateCrtKeyParameters) generateKeyPair.getPrivate();
        RSAKeyParameters publicKey = (RSAKeyParameters) generateKeyPair.getPublic();


        X509CertificateHolder RSAx509Certificate = CryptoX509.RSA.createCertHolder(startDate, expiryDate,
                                                                                   new X500Name("CN=Test"), new X500Name("CN=Test"), serialNumber,
                                                                                   privateKey, publicKey);
        // make sure it's a valid cert.
        if (RSAx509Certificate != null) {
            boolean valid = CryptoX509.RSA.validate(RSAx509Certificate);

            if (!valid) {
                fail("Unable to verify a x509 certificate.");
            }
        } else {
            fail("Unable to create a x509 certificate.");
        }

        // now sign something, then verify the signature.
        byte[] data = "My keyboard is awesome".getBytes();
        byte[] signatureBlock = CryptoX509.createSignature(data, RSAx509Certificate, privateKey);

        boolean verifySignature = CryptoX509.RSA.verifySignature(signatureBlock, publicKey);

        if (!verifySignature) {
            fail("Unable to verify a x509 certificate signature.");
        }
    }
}
