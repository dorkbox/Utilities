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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAParams;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.BERSequence;
import org.bouncycastle.asn1.BERSet;
import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.cms.SignerIdentifier;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.DSAParameter;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.DefaultCMSSignatureAlgorithmNameGenerator;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.DSAParameters;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.dsa.BCDSAPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.dsa.DSAUtil;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.RSAUtil;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.JCEECPublicKey;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcContentSignerBuilder;
import org.bouncycastle.operator.bc.BcDSAContentSignerBuilder;
import org.bouncycastle.operator.bc.BcDSAContentVerifierProviderBuilder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.bc.BcRSAContentVerifierProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dorkbox.util.crypto.signers.BcECDSAContentSignerBuilder;
import dorkbox.util.crypto.signers.BcECDSAContentVerifierProviderBuilder;

public class CryptoX509 {

    private static final Logger logger = LoggerFactory.getLogger(CryptoX509.class);

    public static void addProvider() {
        // make sure we only add it once (in case it's added elsewhere...)
        Provider provider = Security.getProvider("BC");
        if (provider == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static class Util {

        /**
         * @return true if saving the x509 certificate to a PEM format file was successful
         */
        public static boolean convertToPemFile(X509Certificate x509cert, String fileName) {
            boolean failed = false;
            Writer output = null;

            try {
                String lineSeparator = "\r\n";

                String cert_begin = "-----BEGIN CERTIFICATE-----";
                String cert_end =   "-----END CERTIFICATE-----";

                byte[] derCert = Base64.getMimeEncoder().encode(x509cert.getEncoded());
                char[] encodeToChar = new char[derCert.length];

                for (int i = 0; i < derCert.length; i++) {
                    encodeToChar[i] = (char) derCert[i];
                }


                int newLineCount = encodeToChar.length/64;

                int length = encodeToChar.length;

                output = new BufferedWriter(new FileWriter(fileName, false),
                                              cert_begin.length() + cert_end.length() + length + newLineCount + 3);

                output.write(cert_begin);
                output.write(lineSeparator);

                int copyCount = 64;
                for (int i=0;i<length;i+=64) {
                    if (i+64 > length) {
                        copyCount = length - i;
                    }

                    output.write(encodeToChar, i, copyCount);
                    output.write(lineSeparator);
                }
                output.write(cert_end);
                output.write(lineSeparator);
            } catch (Exception e) {
                logger.error("Error during conversion.", e);
                failed = true;
            } finally {
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        logger.error("Error closing resource.", e);
                    }
                }
            }

            return !failed;
        }

        public static String convertToPem(X509Certificate x509cert) throws CertificateEncodingException {
            String lineSeparator = "\r\n";

            String cert_begin = "-----BEGIN CERTIFICATE-----";
            String cert_end =   "-----END CERTIFICATE-----";

            byte[] derCert = Base64.getMimeEncoder().encode(x509cert.getEncoded());
            char[] encodeToChar = new char[derCert.length];

            for (int i = 0; i < derCert.length; i++) {
                encodeToChar[i] = (char) derCert[i];
            }

            int newLineCount = encodeToChar.length/64;

            int length = encodeToChar.length;
            int lastIndex = 0;
            StringBuilder sb = new StringBuilder(cert_begin.length() + cert_end.length() + length + newLineCount + 2);

            sb.append(cert_begin);
            sb.append(lineSeparator);
            for (int i=64;i<length;i+=64) {
                sb.append(encodeToChar, lastIndex, i);
                sb.append(lineSeparator);
                lastIndex = i;
            }
            sb.append(cert_end);

            return sb.toString();
           }

        public static String getDigestNameFromCert(X509CertificateHolder x509CertificateHolder) {
            return Util.getDigestNameFromSigAlgId(x509CertificateHolder.getSignatureAlgorithm().getAlgorithm());
        }

        public static String getDigestNameFromSigAlgId(ASN1ObjectIdentifier algorithm) {
            String digest = null;
            try {
                // have to use reflection in order to access the DIGEST method used by the key.
                DefaultCMSSignatureAlgorithmNameGenerator defaultCMSSignatureAlgorithmNameGenerator = new DefaultCMSSignatureAlgorithmNameGenerator();
                Method declaredMethod = DefaultCMSSignatureAlgorithmNameGenerator.class.getDeclaredMethod("getDigestAlgName",
                                                                                                          ASN1ObjectIdentifier.class);
                declaredMethod.setAccessible(true);
                digest = (String) declaredMethod.invoke(defaultCMSSignatureAlgorithmNameGenerator, algorithm);
            } catch (Throwable t) {
                throw new RuntimeException("Weird error using reflection to get the digest name: " + algorithm.getId() + t.getMessage());
            }

            if (algorithm.getId().equals(digest)) {
                throw new RuntimeException("Unable to get digest name from algorithm ID: " + algorithm.getId());
            }

            return digest;
        }


//        @SuppressWarnings("rawtypes")
//        public static void verify(JarFile jf, X509Certificate[] trustedCaCerts) throws IOException, CertificateException {
//            Vector<JarEntry> entriesVec = new Vector<JarEntry>();
//
//            // Ensure there is a manifest file
//            Manifest man = jf.getManifest();
//            if (man == null) {
//                throw new SecurityException("The JAR is not signed");
//            }
//
//            // Ensure all the entries' signatures verify correctly
//            byte[] buffer = new byte[8192];
//            Enumeration entries = jf.entries();
//
//            while (entries.hasMoreElements()) {
//                JarEntry je = (JarEntry) entries.nextElement();
//                entriesVec.addElement(je);
//                InputStream is = jf.getInputStream(je);
//                @SuppressWarnings("unused")
//                int n;
//                while ((n = is.read(buffer, 0, buffer.length)) != -1) {
//                    // we just read. this will throw a SecurityException
//                    // if  a signature/digest check fails.
//                }
//                is.close();
//            }
//            jf.close();
//
//            // Get the list of signer certificates
//            Enumeration e = entriesVec.elements();
//            while (e.hasMoreElements()) {
//                JarEntry je = (JarEntry) e.nextElement();
//
//                if (je.isDirectory()) {
//                    continue;
//                }
//                // Every file must be signed - except
//                // files in META-INF
//                Certificate[] certs = je.getCertificates();
//                if (certs == null || certs.length == 0) {
//                    if (!je.getName().startsWith("META-INF")) {
//                        throw new SecurityException("The JCE framework has unsigned class files.");
//                    }
//                } else {
//                    // Check whether the file
//                    // is signed as expected.
//                    // The framework may be signed by
//                    // multiple signers. At least one of
//                    // the signers must be a trusted signer.
//
//                    // First, determine the roots of the certificate chains
//                    X509Certificate[] chainRoots = getChainRoots(certs);
//                    boolean signedAsExpected = false;
//
//                    for (int i = 0; i < chainRoots.length; i++) {
//                        if (isTrusted(chainRoots[i], trustedCaCerts)) {
//                            signedAsExpected = true;
//                            break;
//                        }
//                    }
//
//                    if (!signedAsExpected) {
//                        throw new SecurityException("The JAR is not signed by a trusted signer");
//                    }
//                }
//            }
//        }

        public static boolean isTrusted(X509Certificate cert, X509Certificate[] trustedCaCerts) {
            // Return true iff either of the following is true:
            // 1) the cert is in the trustedCaCerts.
            // 2) the cert is issued by a trusted CA.

            // Check whether the cert is in the trustedCaCerts
            for (int i = 0; i < trustedCaCerts.length; i++) {
                // If the cert has the same SubjectDN
                // as a trusted CA, check whether
                // the two certs are the same.
                if (cert.getSubjectDN().equals(trustedCaCerts[i].getSubjectDN())) {
                    if (cert.equals(trustedCaCerts[i])) {
                        return true;
                    }
                }
            }

            // Check whether the cert is issued by a trusted CA.
            // Signature verification is expensive. So we check
            // whether the cert is issued
            // by one of the trusted CAs if the above loop failed.
            for (int i = 0; i < trustedCaCerts.length; i++) {
                // If the issuer of the cert has the same name as
                // a trusted CA, check whether that trusted CA
                // actually issued the cert.
                if (cert.getIssuerDN().equals(trustedCaCerts[i].getSubjectDN())) {
                    try {
                        cert.verify(trustedCaCerts[i].getPublicKey());
                        return true;
                    } catch (Exception e) {
                        // Do nothing.
                    }
                }
            }

            return false;
        }

//        public static X509Certificate[] getChainRoots(Certificate[] certs) {
//            Vector<X509Certificate> result = new Vector<X509Certificate>(3);
//            // choose a Vector size that seems reasonable
//            for (int i = 0; i < certs.length - 1; i++) {
//                if (!((X509Certificate) certs[i + 1]).getSubjectDN().equals(
//                        ((X509Certificate) certs[i]).getIssuerDN())) {
//                    // We've reached the end of a chain
//                    result.addElement((X509Certificate) certs[i]);
//                }
//            }
//
//            // The final entry in the certs array is always
//            // a "root" certificate
//            result.addElement((X509Certificate) certs[certs.length - 1]);
//            X509Certificate[] ret = new X509Certificate[result.size()];
//            result.copyInto(ret);
//
//            return ret;
//        }
    }


    public static class DSA {
        static {
            addProvider();
        }

        /**
         * Creates a X509 certificate holder object. <p>
         *
         * Look at BCStyle for a list of all valid X500 Names.
         */
        public static X509CertificateHolder createCertHolder(Date startDate, Date expiryDate,
                                                             X500Name issuerName, X500Name subjectName, BigInteger serialNumber,
                                                             DSAPrivateKeyParameters privateKey, DSAPublicKeyParameters publicKey) {

            String signatureAlgorithm = "SHA1withDSA";


            AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(signatureAlgorithm);
            AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);


            SubjectPublicKeyInfo subjectPublicKeyInfo;
            DSAParameters parameters = publicKey.getParameters();
            try {
                byte[] encoded = new SubjectPublicKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_dsa,
                                                                                  new DSAParameter(parameters.getP(),
                                                                                                   parameters.getQ(),
                                                                                                   parameters.getG())),
                                                          new ASN1Integer(publicKey.getY())).getEncoded(ASN1Encoding.DER);

                ASN1Sequence seq = (ASN1Sequence)ASN1Primitive.fromByteArray(encoded);
                subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(seq);
            } catch (IOException e) {
                logger.error("Error during DSA.", e);
                return null;
            }

            X509v3CertificateBuilder v3CertBuilder = new X509v3CertificateBuilder(issuerName,
                                                                                  serialNumber, startDate, expiryDate,
                                                                                  subjectName, subjectPublicKeyInfo);

            BcDSAContentSignerBuilder contentSignerBuilder = new BcDSAContentSignerBuilder(sigAlgId, digAlgId);

            ContentSigner build;
            try {
                build = contentSignerBuilder.build(privateKey);
            } catch (OperatorCreationException e) {
                logger.error("Error creating certificate.", e);
                return null;
            }

            return v3CertBuilder.build(build);
        }


        /**
         * Verifies that the certificate is legitimate.
         * <p>
         * MUST have BouncyCastle provider loaded by the security manager!
         * <p>
         * @return true if it was a valid cert.
         */
        public static boolean validate(X509CertificateHolder x509CertificateHolder) {
            try {

                // this is unique in that it verifies that the certificate is a LEGIT certificate, but not necessarily
                //  valid during this time period.
                ContentVerifierProvider contentVerifierProvider = new BcDSAContentVerifierProviderBuilder(
                      new DefaultDigestAlgorithmIdentifierFinder()).build(x509CertificateHolder);

                boolean signatureValid = x509CertificateHolder.isSignatureValid(contentVerifierProvider);

                if (!signatureValid) {
                    return false;
                }


                CertificateFactory certificateFactory = new CertificateFactory();
                java.security.cert.Certificate certificate = certificateFactory.engineGenerateCertificate(new ByteArrayInputStream(x509CertificateHolder.getEncoded()));
                // Note: this requires the BC provider to be loaded!
                if (certificate == null || certificate.getPublicKey() == null) {
                    return false;
                }

                // TODO: when validating the certificate, it is important to use a date from somewhere other than the host computer! (maybe use google? or something...)
                // this will validate the DATES of the certificate, to make sure the cert is valid during the correct time period.

                // Verify the TIME/DATE of the certificate
                ((X509Certificate) certificate).checkValidity(new Date());

                // if we get here, it means that our cert is LEGIT and VALID.
                return true;

            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        /**
         * Verifies the given x509 based signature against the OPTIONAL original public key. If not specified, then
         * the public key from the signature is used.
         * <p>
         * MUST have BouncyCastle provider loaded by the security manager!
         * <p>
         * @return true if the signature was valid.
         */
        public static boolean verifySignature(byte[] signatureBytes, DSAPublicKeyParameters optionalOriginalPublicKey) {
            ASN1InputStream asn1InputStream = null;
            try {
                asn1InputStream = new ASN1InputStream(new ByteArrayInputStream(signatureBytes));
                ASN1Primitive signatureASN = asn1InputStream.readObject();

                BERSequence seq = (BERSequence) ASN1Sequence.getInstance(signatureASN);
                ContentInfo contentInfo = ContentInfo.getInstance(seq);

                // Extract certificates
                SignedData newSignedData = SignedData.getInstance(contentInfo.getContent());

                InputStream newSigIn = new ByteArrayInputStream(newSignedData.getCertificates().parser().readObject().toASN1Primitive().getEncoded());

                org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory certificateFactory = new org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory();
                java.security.cert.Certificate engineGenerateCert = certificateFactory.engineGenerateCertificate(newSigIn);

                BCDSAPublicKey publicKey2 = (BCDSAPublicKey) engineGenerateCert.getPublicKey();

                if (optionalOriginalPublicKey != null) {
                    DSAParams params = publicKey2.getParams();
                    DSAParameters parameters = optionalOriginalPublicKey.getParameters();

                    if (!publicKey2.getY().equals(optionalOriginalPublicKey.getY()) ||
                        !params.getP().equals(parameters.getP()) ||
                        !params.getQ().equals(parameters.getQ()) ||
                        !params.getG().equals(parameters.getG())) {

                        return false;
                    }
                }

                // throws exception if it fails
                engineGenerateCert.verify(publicKey2);

                return true;
            } catch (Throwable t) {
                return false;
            } finally {
                if (asn1InputStream != null) {
                    try {
                        asn1InputStream.close();
                    } catch (IOException e) {
                        logger.error("Error closing stream during DSA.", e);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public static class RSA {
        static {
            addProvider();
        }

//        public static class CertificateAuthority {
//            public static X509Certificate generateCert(KeyFactory factory, Date startDate, Date expiryDate,
//                                                         String  issuer, String subject, String friendlyName,
//                                                         RSAKeyParameters publicKey, RSAPrivateCrtKeyParameters privateKey) throws InvalidKeySpecException, IOException, InvalidKeyException, OperatorCreationException {
//
//                return CryptoX509.RSA.generateCert(factory, startDate, expiryDate, new X500Name(issuer), new X500Name(subject), friendlyName, publicKey, privateKey, null);
//            }
//
//            public static X509Certificate generateCert(KeyFactory factory, Date startDate, Date expiryDate,
//                                                         X509Principal issuer, String subject, String friendlyName,
//                                                         RSAKeyParameters publicKey, RSAPrivateCrtKeyParameters privateKey) throws InvalidKeySpecException, InvalidKeyException, IOException, OperatorCreationException {
//
//                return CryptoX509.RSA.generateCert(factory, startDate, expiryDate, X500Name.getInstance(issuer), new X500Name(subject), friendlyName, publicKey, privateKey, null);
//            }
//        }
//
//
//        public static class IntermediateAuthority {
//            public static X509Certificate generateCert(KeyFactory factory, Date startDate, Date expiryDate,
//                                                         String  issuer, String subject, String friendlyName,
//                                                         RSAKeyParameters publicKey, RSAPrivateCrtKeyParameters privateKey,
//                                                         X509Certificate caCertificate) throws InvalidKeySpecException, IOException, InvalidKeyException, OperatorCreationException {
//
//                return CryptoX509.RSA.generateCert(factory, startDate, expiryDate, new X500Name(issuer), new X500Name(subject), friendlyName, publicKey, privateKey, caCertificate);
//            }
//
//            public static X509Certificate generateCert(KeyFactory factory, Date startDate, Date expiryDate,
//                                                           X509Principal issuer, String subject, String friendlyName,
//                                                           RSAKeyParameters publicKey, RSAPrivateCrtKeyParameters privateKey,
//                                                           X509Certificate caCertificate) throws InvalidKeySpecException, InvalidKeyException, IOException, OperatorCreationException {
//
//                return CryptoX509.RSA.generateCert(factory, startDate, expiryDate, X500Name.getInstance(issuer), new X500Name(subject), friendlyName, publicKey, privateKey, caCertificate);
//            }
//        }
//
        public static class CertificateAuthrority {
            public static X509Certificate generateCert(KeyFactory factory, Date startDate, Date endDate,
                                                         String subject, String friendlyName,
                                                         RSAKeyParameters publicKey, RSAPrivateCrtKeyParameters privateKey) {

                String signatureAlgorithm = "SHA1withRSA";

                AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(signatureAlgorithm); // specify it's RSA
                AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId); // specify SHA

                try {
                    // JCE format needed for the certificate - because getEncoded() is necessary...
                    PublicKey jcePublicKey = convertToJCE(factory, publicKey);
//                PrivateKey jcePrivateKey = convertToJCE(factory, publicKey, privateKey);

                    SubjectPublicKeyInfo subjectPublicKeyInfo = createSubjectPublicKey(jcePublicKey);
                    X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(new X500Name(subject), BigInteger.valueOf(System.currentTimeMillis()),
                                                                                           startDate, endDate, new X500Name(subject),
                                                                                           subjectPublicKeyInfo);

                    //
                    // extensions
                    //
                    JcaX509ExtensionUtils jcaX509ExtensionUtils = new JcaX509ExtensionUtils(); // SHA1
                    SubjectKeyIdentifier createSubjectKeyIdentifier = jcaX509ExtensionUtils.createSubjectKeyIdentifier(subjectPublicKeyInfo);

                    certBuilder.addExtension(Extension.subjectKeyIdentifier,
                                              false,
                                              createSubjectKeyIdentifier);

                    certBuilder.addExtension(Extension.basicConstraints,
                                             true,
                                             new BasicConstraints(1));


                    ContentSigner hashSigner = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(privateKey);
                    X509CertificateHolder certHolder = certBuilder.build(hashSigner);

                    java.security.cert.Certificate certificate = new CertificateFactory().engineGenerateCertificate(
                            new ByteArrayInputStream(certHolder.getEncoded()));

                    if (!(certificate instanceof X509Certificate)) {
                        logger.error("Error generating certificate, it's the wrong type.");
                        return null;
                    }

                    certificate.verify(jcePublicKey);


                    if (certificate instanceof PKCS12BagAttributeCarrier) {
                        PKCS12BagAttributeCarrier bagAttr = (PKCS12BagAttributeCarrier) certificate;

                        //
                        // this is actually optional - but if you want to have control
                        // over setting the friendly name this is the way to do it...
                        //
                        bagAttr.setBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_friendlyName,
                                                new DERBMPString(friendlyName));
                    }

                    return (X509Certificate) certificate;
                } catch (Exception e) {
                    logger.error("Error generating certificate.", e);
                    return null;
                }
            }
        }

        public static class SelfSigned {
            public static X509Certificate generateCert(KeyFactory factory, Date startDate, Date endDate,
                                                         String subject, String friendlyName,
                                                         RSAKeyParameters publicKey, RSAPrivateCrtKeyParameters privateKey) {

                String signatureAlgorithm = "SHA1withRSA";

                AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(signatureAlgorithm); // specify it's RSA
                AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId); // specify SHA

                try {
                    // JCE format needed for the certificate - because getEncoded() is necessary...
                    PublicKey jcePublicKey = convertToJCE(factory, publicKey);
//                PrivateKey jcePrivateKey = convertToJCE(factory, publicKey, privateKey);

                    SubjectPublicKeyInfo subjectPublicKeyInfo = createSubjectPublicKey(jcePublicKey);
                    X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(new X500Name(subject), BigInteger.valueOf(System.currentTimeMillis()),
                                                                                        startDate, endDate, new X500Name(subject),
                                                                                        subjectPublicKeyInfo);

                    //
                    // extensions
                    //
                    JcaX509ExtensionUtils jcaX509ExtensionUtils = new JcaX509ExtensionUtils(); // SHA1
                    SubjectKeyIdentifier createSubjectKeyIdentifier = jcaX509ExtensionUtils.createSubjectKeyIdentifier(subjectPublicKeyInfo);

                    certBuilder.addExtension(Extension.subjectKeyIdentifier,
                                             false,
                                             createSubjectKeyIdentifier);

                    certBuilder.addExtension(Extension.basicConstraints,
                                             true,
                                             new BasicConstraints(false));


                    ContentSigner hashSigner = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(privateKey);
                    X509CertificateHolder certHolder = certBuilder.build(hashSigner);

                    java.security.cert.Certificate certificate = new CertificateFactory().engineGenerateCertificate(new ByteArrayInputStream(
                            certHolder.getEncoded()));

                    if (!(certificate instanceof X509Certificate)) {
                        logger.error("Error generating certificate, it's the wrong type.");
                        return null;
                    }

                    certificate.verify(jcePublicKey);


                    if (certificate instanceof PKCS12BagAttributeCarrier) {
                        PKCS12BagAttributeCarrier bagAttr = (PKCS12BagAttributeCarrier) certificate;

                        //
                        // this is actually optional - but if you want to have control
                        // over setting the friendly name this is the way to do it...
                        //
                        bagAttr.setBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_friendlyName, new DERBMPString(friendlyName));
                    }


                    return (X509Certificate) certificate;
                } catch (Exception e) {
                    logger.error("Error generating certificate.", e);
                    return null;
                }
            }
        }


        /**
         * Generate a cert that is signed by a CA cert.
         */
        public static
        X509Certificate generateCert(KeyFactory factory,
                                     Date startDate,
                                     Date expiryDate,
                                     X509Certificate issuerCert,
                                     String subject,
                                     String friendlyName,
                                     RSAKeyParameters publicKey,
                                     RSAPrivateCrtKeyParameters signingCaKey)
                        throws InvalidKeySpecException, InvalidKeyException, IOException, OperatorCreationException, CertificateException,
                               NoSuchAlgorithmException, NoSuchProviderException, SignatureException {

            return CryptoX509.RSA.generateCert(factory,
                                               startDate,
                                               expiryDate,
                                               X500Name.getInstance(PrincipalUtil.getSubjectX509Principal(issuerCert)),
                                               new X500Name(subject),
                                               friendlyName,
                                               publicKey,
                                               issuerCert,
                                               signingCaKey);
        }


        /**
         * Generate a cert that is self signed.
         */
        public static
        X509Certificate generateCert(KeyFactory factory,
                                     Date startDate,
                                     Date expiryDate,
                                     String subject,
                                     String friendlyName,
                                     RSAKeyParameters publicKey,
                                     RSAPrivateCrtKeyParameters privateKey)
                        throws InvalidKeySpecException, InvalidKeyException, IOException, OperatorCreationException, CertificateException,
                               NoSuchAlgorithmException, NoSuchProviderException, SignatureException {

            return CryptoX509.RSA.generateCert(factory,
                                               startDate,
                                               expiryDate,
                                               new X500Name(subject),
                                               new X500Name(subject),
                                               friendlyName,
                                               publicKey,
                                               null,
                                               privateKey);
        }



        private static X509Certificate generateCert(KeyFactory factory, Date startDate, Date expiryDate,
                                                     X500Name issuer, X500Name subject, String friendlyName,
                                                     RSAKeyParameters certPublicKey,
                                                     X509Certificate signingCertificate, RSAPrivateCrtKeyParameters signingPrivateKey)
                        throws InvalidKeySpecException, IOException, InvalidKeyException, OperatorCreationException, CertificateException,
                               NoSuchAlgorithmException, NoSuchProviderException, SignatureException {


            String signatureAlgorithm = "SHA1withRSA";

            AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(signatureAlgorithm); // specify it's RSA
            AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId); // specify SHA

            // JCE format needed for the certificate - because getEncoded() is necessary...
            PublicKey jcePublicKey = convertToJCE(factory, certPublicKey);
//            PrivateKey jcePrivateKey = convertToJCE(factory, publicKey, privateKey);




            SubjectPublicKeyInfo subjectPublicKeyInfo = createSubjectPublicKey(jcePublicKey);
            X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(issuer, BigInteger.valueOf(System.currentTimeMillis()),
                                                                                   startDate, expiryDate, subject,
                                                                                   subjectPublicKeyInfo);



            //
            // extensions
            //
            JcaX509ExtensionUtils jcaX509ExtensionUtils = new JcaX509ExtensionUtils(); // SHA1
            SubjectKeyIdentifier createSubjectKeyIdentifier = jcaX509ExtensionUtils.createSubjectKeyIdentifier(subjectPublicKeyInfo);

            certBuilder.addExtension(Extension.subjectKeyIdentifier,
                                      false,
                                      createSubjectKeyIdentifier);

            if (signingCertificate != null) {
                AuthorityKeyIdentifier createAuthorityKeyIdentifier = jcaX509ExtensionUtils.createAuthorityKeyIdentifier(signingCertificate.getPublicKey());
                certBuilder.addExtension(Extension.authorityKeyIdentifier,
                                          false,
                                          createAuthorityKeyIdentifier);
//                new AuthorityKeyIdentifierStructure(signingCertificate));
            }

            certBuilder.addExtension(Extension.basicConstraints,
                                     true,
                                     new BasicConstraints(false));



            ContentSigner signer = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(signingPrivateKey);
            X509CertificateHolder certHolder = certBuilder.build(signer);

            java.security.cert.Certificate certificate = new CertificateFactory().engineGenerateCertificate(new ByteArrayInputStream(
                    certHolder.getEncoded()));

            if (!(certificate instanceof X509Certificate)) {
                logger.error("Error generating certificate, it's the wrong type.");
                return null;
            }

            if (signingCertificate != null) {
                certificate.verify(signingCertificate.getPublicKey());
            } else {
                certificate.verify(jcePublicKey);
            }

            if (certificate instanceof PKCS12BagAttributeCarrier) {
                PKCS12BagAttributeCarrier bagAttr = (PKCS12BagAttributeCarrier) certificate;

                //
                // this is actually optional - but if you want to have control
                // over setting the friendly name this is the way to do it...
                //
                bagAttr.setBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_friendlyName, new DERBMPString(friendlyName));

                if (signingCertificate != null) {
                    bagAttr.setBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_localKeyId, subjectPublicKeyInfo);
                }
            }

            return (X509Certificate) certificate;


//            //// subject name table.
//            //Hashtable<ASN1ObjectIdentifier, String> attrs = new Hashtable<ASN1ObjectIdentifier, String>();
//            //Vector<ASN1ObjectIdentifier>            order = new Vector<ASN1ObjectIdentifier>();
//            //
//            //attrs.put(BCStyle.C, "US");
//            //attrs.put(BCStyle.O, "Dorkbox");
//            //attrs.put(BCStyle.OU, "Dorkbox Certificate Authority");
//            //attrs.put(BCStyle.EmailAddress, "admin@dorkbox.com");
//            //
//            //order.addElement(BCStyle.C);
//            //order.addElement(BCStyle.O);
//            //order.addElement(BCStyle.OU);
//            //order.addElement(BCStyle.EmailAddress);
//            //
//            //X509Principal issuer = new X509Principal(order, attrs);
//            // MASTER CERT
//
//            //// signers name
//            //String  issuer = "C=US, O=dorkbox llc, OU=Dorkbox Certificate Authority";
//            //
//            //// subjects name - the same as we are self signed.
//            //String  subject = "C=US, O=dorkbox llc, OU=Dorkbox Certificate Authority";
        }

        private static SubjectPublicKeyInfo createSubjectPublicKey(PublicKey jcePublicKey) throws IOException {
            ASN1InputStream asn1InputStream = null;
            try {
                asn1InputStream = new ASN1InputStream(new ByteArrayInputStream(jcePublicKey.getEncoded()));
                return SubjectPublicKeyInfo.getInstance(asn1InputStream.readObject());
            } finally {
                if (asn1InputStream != null) {
                    asn1InputStream.close();
                }
            }
        }


        public static PublicKey convertToJCE(RSAKeyParameters publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return convertToJCE(keyFactory, publicKey);
        }

        public static PublicKey convertToJCE(KeyFactory keyFactory, RSAKeyParameters publicKey) throws InvalidKeySpecException {
            return keyFactory.generatePublic(new RSAPublicKeySpec(publicKey.getModulus(), publicKey.getExponent()));
        }

        public static RSAKeyParameters convertToBC(PublicKey publicKey) {
            RSAPublicKey pubKey = RSAPublicKey.getInstance(publicKey);
            return new RSAKeyParameters(false, pubKey.getModulus(), pubKey.getPublicExponent());
        }

        public static PrivateKey convertToJCE(RSAKeyParameters publicKey, RSAPrivateCrtKeyParameters privateKey) throws InvalidKeySpecException, NoSuchAlgorithmException {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return convertToJCE(keyFactory, publicKey, privateKey);
        }

        public static PrivateKey convertToJCE(KeyFactory keyFactory, RSAKeyParameters publicKey, RSAPrivateCrtKeyParameters privateKey) throws InvalidKeySpecException {
            return keyFactory.generatePrivate(new RSAPrivateCrtKeySpec(publicKey.getModulus(), publicKey.getExponent(),
                                                                       privateKey.getExponent(), privateKey.getP(), privateKey.getQ(),
                                                                       privateKey.getDP(), privateKey.getDQ(), privateKey.getQInv()));
        }

        /**
         * Creates a X509 certificate holder object. <p>
         *
         * Look at BCStyle for a list of all valid X500 Names.
         */
        public static X509CertificateHolder createCertHolder(Date startDate, Date expiryDate,
                                                             X500Name issuerName, X500Name subjectName, BigInteger serialNumber,
                                                             RSAPrivateCrtKeyParameters privateKey, RSAKeyParameters publicKey) {

            String signatureAlgorithm = "SHA256withRSA";


            AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(signatureAlgorithm);
            AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);


            SubjectPublicKeyInfo subjectPublicKeyInfo;

            try {
                // JCE format needed for the certificate - because getEncoded() is necessary...
                PublicKey jcePublicKey = convertToJCE(publicKey);
//            PrivateKey jcePrivateKey = convertToJCE(factory, publicKey, privateKey);

                subjectPublicKeyInfo = createSubjectPublicKey(jcePublicKey);
            } catch (Exception e) {
                logger.error("Unable to create RSA keyA.", e);
                return null;
            }


            try {
                X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(issuerName, serialNumber,
                                                                                    startDate, expiryDate, subjectName,
                                                                                    subjectPublicKeyInfo);
                //
                // extensions
                //
                JcaX509ExtensionUtils jcaX509ExtensionUtils = new JcaX509ExtensionUtils(); // SHA1
                SubjectKeyIdentifier createSubjectKeyIdentifier = jcaX509ExtensionUtils.createSubjectKeyIdentifier(subjectPublicKeyInfo);

                certBuilder.addExtension(Extension.subjectKeyIdentifier,
                                         false,
                                         createSubjectKeyIdentifier);

                certBuilder.addExtension(Extension.basicConstraints,
                                         true,
                                         new BasicConstraints(false));


                ContentSigner hashSigner = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(privateKey);

                return certBuilder.build(hashSigner);
            } catch (Exception e) {
                logger.error("Error generating certificate.", e);
                return null;
            }
        }


        /**
         * Verifies that the certificate is legitimate.
         * <p>
         * MUST have BouncyCastle provider loaded by the security manager!
         * <p>
         * @return true if it was a valid cert.
         */
        public static boolean validate(X509CertificateHolder x509CertificateHolder) {
            try {

                // this is unique in that it verifies that the certificate is a LEGIT certificate, but not necessarily
                //  valid during this time period.
                ContentVerifierProvider contentVerifierProvider = new BcRSAContentVerifierProviderBuilder(new DefaultDigestAlgorithmIdentifierFinder()).build(x509CertificateHolder);

                boolean signatureValid = x509CertificateHolder.isSignatureValid(contentVerifierProvider);

                if (!signatureValid) {
                    return false;
                }

                java.security.cert.Certificate certificate = new CertificateFactory().engineGenerateCertificate(
                        new ByteArrayInputStream(x509CertificateHolder.getEncoded()));

                // Note: this requires the BC provider to be loaded!
                if (certificate == null || certificate.getPublicKey() == null) {
                    return false;
                }

                if (!(certificate instanceof X509Certificate)) {
                    return false;
                }

                // TODO: when validating the certificate, it is important to use a date from somewhere other than the host computer! (maybe use google? or something...)
                // this will validate the DATES of the certificate, to make sure the cert is valid during the correct time period.

                // Verify the TIME/DATE of the certificate
                ((X509Certificate) certificate).checkValidity(new Date());

                // if we get here, it means that our cert is LEGIT and VALID.
                return true;

            } catch (Throwable t) {
                logger.error("Error validating certificate.", t);
                return false;
            }
        }

        /**
         * Verifies the given x509 based signature against the OPTIONAL original public key. If not specified, then
         * the public key from the signature is used.
         * <p>
         * MUST have BouncyCastle provider loaded by the security manager!
         * <p>
         * @return true if the signature was valid.
         */
        public static boolean verifySignature(byte[] signatureBytes, RSAKeyParameters publicKey) {

            ASN1InputStream asn1InputStream = null;
            try {
                asn1InputStream = new ASN1InputStream(new ByteArrayInputStream(signatureBytes));
                ASN1Primitive signatureASN = asn1InputStream.readObject();
                BERSequence seq = (BERSequence) ASN1Sequence.getInstance(signatureASN);
                ContentInfo contentInfo = ContentInfo.getInstance(seq);

                // Extract certificates
                SignedData newSignedData = SignedData.getInstance(contentInfo.getContent());

                InputStream newSigIn = new ByteArrayInputStream(newSignedData.getCertificates().parser().readObject().toASN1Primitive().getEncoded());

                org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory certFactory = new org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory();
                java.security.cert.Certificate certificate = certFactory.engineGenerateCertificate(newSigIn);

                BCRSAPublicKey publicKey2 = (BCRSAPublicKey) certificate.getPublicKey();

                if (publicKey != null) {
                    if (!publicKey.getModulus().equals(publicKey2.getModulus()) ||
                        !publicKey.getExponent().equals(publicKey2.getPublicExponent())) {

                        return false;
                    }
                }

                // throws exception if it fails.
                certificate.verify(publicKey2);

                return true;
            } catch (Throwable t) {
                logger.error("Error validating certificate.", t);
                return false;
            } finally {
                if (asn1InputStream != null) {
                    try {
                        asn1InputStream.close();
                    } catch (IOException e) {
                        logger.error("Error closing stream during RSA.", e);
                    }
                }
            }
        }
    }

    public static class ECDSA {
        static {
            // make sure we only add it once (in case it's added elsewhere...)
            Provider provider = Security.getProvider("BC");
            if (provider == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
        }

        /**
         * Creates a X509 certificate holder object.
         */
        public static X509CertificateHolder createCertHolder(String digestName,
                                                   Date startDate, Date expiryDate,
                                                   X500Name issuerName, X500Name subjectName,
                                                   BigInteger serialNumber,
                                                   ECPrivateKeyParameters privateKey,
                                                   ECPublicKeyParameters publicKey) {

            String signatureAlgorithm = digestName + "withECDSA";


            // we WANT the ECparameterSpec to be null, so it's created from the public key
            JCEECPublicKey pubKey = new JCEECPublicKey("EC", publicKey, (ECParameterSpec) null);

            AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(signatureAlgorithm);
            AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

            SubjectPublicKeyInfo subjectPublicKeyInfo;

            try {
                byte[] encoded = pubKey.getEncoded();
                ASN1Sequence seq = (ASN1Sequence)ASN1Primitive.fromByteArray(encoded);
                subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(seq);
            } catch (IOException e) {
                logger.error("Unable to perform DSA.", e);
                return null;
            }

            X509v3CertificateBuilder v3CertBuilder = new X509v3CertificateBuilder(issuerName,
                                                                                  serialNumber, startDate, expiryDate,
                                                                                  subjectName, subjectPublicKeyInfo);

            BcECDSAContentSignerBuilder contentSignerBuilder = new BcECDSAContentSignerBuilder(sigAlgId, digAlgId);

            ContentSigner build;
            try {
                build = contentSignerBuilder.build(privateKey);
            } catch (OperatorCreationException e) {
                logger.error("Error creating certificate.", e);
                return null;
            }

            return v3CertBuilder.build(build);
        }

        /**
         * Verifies that the certificate is legitimate.
         * <p>
         * MUST have BouncyCastle provider loaded by the security manager!
         * <p>
         * @return true if it was a valid cert.
         */
        public static boolean validate(X509CertificateHolder x509CertificateHolder) {
            try {

                // this is unique in that it verifies that the certificate is a LEGIT certificate, but not necessarily
                //  valid during this time period.
                ContentVerifierProvider contentVerifierProvider = new BcECDSAContentVerifierProviderBuilder(
                      new DefaultDigestAlgorithmIdentifierFinder()).build(x509CertificateHolder);

                boolean signatureValid = x509CertificateHolder.isSignatureValid(contentVerifierProvider);

                if (!signatureValid) {
                    return false;
                }

                CertificateFactory certFactory = new CertificateFactory();
                java.security.cert.Certificate certificate = certFactory.engineGenerateCertificate(new ByteArrayInputStream(x509CertificateHolder.getEncoded()));

                // Note: this requires the BC provider to be loaded!
                if (certificate == null || certificate.getPublicKey() == null) {
                    return false;
                }

                // TODO: when validating the certificate, it is important to use a date from somewhere other than the host computer! (maybe use google? or something...)
                // this will validate the DATES of the certificate, to make sure the cert is valid during the correct time period.

                // Verify the TIME/DATE of the certificate
                ((X509Certificate) certificate).checkValidity(new Date());

                // if we get here, it means that our cert is LEGIT and VALID.
                return true;
            } catch (Throwable t) {
                logger.error("Error validating certificate.", t);
                return false;
            }

        }

        /**
         * Verifies the given x509 based signature against the OPTIONAL original public key. If not specified, then
         * the public key from the signature is used.
         * <p>
         * MUST have BouncyCastle provider loaded by the security manager!
         * <p>
         * @return true if the signature was valid.
         */
        public static boolean verifySignature(byte[] signatureBytes, ECPublicKeyParameters optionalOriginalPublicKey) {
            ASN1InputStream asn1InputStream = null;
            try {
                asn1InputStream = new ASN1InputStream(new ByteArrayInputStream(signatureBytes));
                ASN1Primitive signatureASN = asn1InputStream.readObject();
                BERSequence seq = (BERSequence) ASN1Sequence.getInstance(signatureASN);
                ContentInfo contentInfo = ContentInfo.getInstance(seq);

                // Extract certificates
                SignedData newSignedData = SignedData.getInstance(contentInfo.getContent());

                InputStream newSigIn = new ByteArrayInputStream(newSignedData.getCertificates().parser().readObject().toASN1Primitive().getEncoded());

                org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory certificateFactory = new org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory();
                java.security.cert.Certificate certificate = certificateFactory.engineGenerateCertificate(newSigIn);

                PublicKey publicKey2 = certificate.getPublicKey();

                if (optionalOriginalPublicKey != null) {
                    ECDomainParameters parameters = optionalOriginalPublicKey.getParameters();
                    ECParameterSpec ecParameterSpec = new ECParameterSpec(parameters.getCurve(), parameters.getG(), parameters.getN(), parameters.getH());
                    BCECPublicKey origPublicKey = new BCECPublicKey("EC", optionalOriginalPublicKey, ecParameterSpec, null);

                    boolean equals = origPublicKey.equals(publicKey2);
                    if (!equals) {
                        return false;
                    }

                    publicKey2 = origPublicKey;
                }

                // throws an exception if not valid!
                certificate.verify(publicKey2);

                return true;
            } catch (Throwable t) {
                logger.error("Error validating certificate.", t);
                return false;
            } finally {
                if (asn1InputStream != null) {
                    try {
                        asn1InputStream.close();
                    } catch (IOException e) {
                        logger.error("Error during ECDSA.", e);
                    }
                }
            }
        }
    }


    /**
     * Creates a NEW signature block that contains the pkcs7 (minus content, which is the .SF file)
     * signature of the .SF file.
     *
     * It contains the hash of the data, and the verification signature.
     */
    public static byte[] createSignature(byte[] signatureSourceData,
                                         X509CertificateHolder x509CertificateHolder, AsymmetricKeyParameter privateKey) {

        try {
            CMSTypedData content = new CMSProcessableByteArray(signatureSourceData);

            ASN1ObjectIdentifier contentTypeOID = new ASN1ObjectIdentifier(content.getContentType().getId());
            ASN1EncodableVector  digestAlgs = new ASN1EncodableVector();
            ASN1EncodableVector  signerInfos = new ASN1EncodableVector();

            AlgorithmIdentifier sigAlgId = x509CertificateHolder.getSignatureAlgorithm();
            AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);

            // use the bouncy-castle lightweight API to generate a hash of the signature source data (usually the signature file bytes)
            BcContentSignerBuilder contentSignerBuilder;
            AlgorithmIdentifier digEncryptionAlgorithm;


            if (privateKey instanceof ECPrivateKeyParameters) {
                contentSignerBuilder = new BcECDSAContentSignerBuilder(sigAlgId, digAlgId);
                digEncryptionAlgorithm = new AlgorithmIdentifier(DSAUtil.dsaOids[0], null); // 1.2.840.10040.4.1  // DSA hashID
            } else if (privateKey instanceof DSAPrivateKeyParameters) {
                contentSignerBuilder = new BcDSAContentSignerBuilder(sigAlgId, digAlgId);
                digEncryptionAlgorithm = new AlgorithmIdentifier(DSAUtil.dsaOids[0], null); // 1.2.840.10040.4.1  // DSA hashID
            } else if (privateKey instanceof RSAPrivateCrtKeyParameters) {
                contentSignerBuilder = new BcRSAContentSignerBuilder(sigAlgId, digAlgId);
                digEncryptionAlgorithm = new AlgorithmIdentifier(RSAUtil.rsaOids[0], null); // 1.2.840.113549.1.1.1 // RSA hashID
            } else {
                throw new RuntimeException("Invalid signature type. Only ECDSA, DSA, RSA supported.");
            }

            ContentSigner hashSigner = contentSignerBuilder.build(privateKey);
            OutputStream outputStream = hashSigner.getOutputStream();
            outputStream.write(signatureSourceData, 0, signatureSourceData.length);
            outputStream.flush();
            byte[] sigBytes = hashSigner.getSignature();


            SignerIdentifier sigId = new SignerIdentifier(new IssuerAndSerialNumber(x509CertificateHolder.toASN1Structure()));

            SignerInfo inf = new SignerInfo(sigId, digAlgId, null, digEncryptionAlgorithm, new DEROctetString(sigBytes), (ASN1Set) null);

            digestAlgs.add(inf.getDigestAlgorithm());
            signerInfos.add(inf);


            ASN1EncodableVector certs = new ASN1EncodableVector();
            certs.add(x509CertificateHolder.toASN1Structure());


            ContentInfo encInfo = new ContentInfo(contentTypeOID, null);
            SignedData  sd = new SignedData(
                                     new DERSet(digestAlgs),
                                     encInfo,
                                     new BERSet(certs),
                                     null,
                                     new DERSet(signerInfos)
                                     );


            ContentInfo contentInfo = new ContentInfo(CMSObjectIdentifiers.signedData, sd);
            CMSSignedData cmsSignedData2 = new CMSSignedData(content, contentInfo);

            return cmsSignedData2.getEncoded();
        } catch (Throwable t) {
            logger.error("Error signing data.", t);
            throw new RuntimeException("Error trying to sign data. " + t.getMessage());
        }
    }

    /**
     * Load a key and certificate from a Java KeyStore, and convert the key to a bouncy-castle key.
     *
     * Code is present but commented out, as it was a PITA to figure it out, as documentation is lacking....
     */
    public static void loadKeystore(String keystoreLocation, String alias, char[] passwd, char[] keypasswd) {
//            FileInputStream fileIn = new FileInputStream(keystoreLocation);
//          KeyStore keyStore = KeyStore.getInstance("JKS");
//          keyStore.load(fileIn, passwd);
//          java.security.cert.Certificate[] chain = keyStore.getCertificateChain(alias);
//          X509Certificate certChain[] = new X509Certificate[chain.length];
//
//          CertificateFactory cf = CertificateFactory.getInstance("X.509");
//          for (int count = 0; count < chain.length; count++) {
//              ByteArrayInputStream certIn = new ByteArrayInputStream(chain[0].getEncoded());
//              X509Certificate cert = (X509Certificate) cf.generateCertificate(certIn);
//              certChain[count] = cert;
//          }
//
//          Key key = keyStore.getKey(alias, keypasswd);
//          KeyFactory keyFactory = KeyFactory.getInstance(key.getAlgorithm());
//          KeySpec keySpec;
//          if (key instanceof DSAPrivateKey) {
//              keySpec = keyFactory.getKeySpec(key, DSAPrivateKeySpec.class);
//          } else {
//              //keySpec = keyFactory.getKeySpec(key, RSAPrivateKeySpec.class);
//              throw new RuntimeException("Only able to support DSA algorithm!");
//          }
//
//          DSAPrivateKey privateKey = (DSAPrivateKey) keyFactory.generatePrivate(keySpec);

        // convert private key to bouncycastle specific
//          DSAParams params = privateKey.getParams();
//          DSAPrivateKeyParameters wimpyPrivKey = new DSAPrivateKeyParameters(privateKey.getX(), new DSAParameters(params.getP(), params.getQ(), params.getG()));
//          X509CertificateHolder x509CertificateHolder = new X509CertificateHolder(certChain[0].getEncoded());
//

//            fileIn.close(); // close JKS
    }
}
