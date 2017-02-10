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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;

import dorkbox.util.IO;
import dorkbox.util.OS;

/**
 * PGP crypto related methods
 */
public final
class CryptoPGP {
    private static final BcPGPDigestCalculatorProvider digestCalculatorProvider = new BcPGPDigestCalculatorProvider();
    private static final BcKeyFingerprintCalculator fingerprintCalculator = new BcKeyFingerprintCalculator();


//    https://github.com/weiliatgithub/bouncycastle-gpg-exampleC
//    https://gist.github.com/turingbirds/3df43f1920a98010667a
//    http://sloanseaman.com/wordpress/2012/05/13/revisited-pgp-encryptiondecryption-in-java/
//    http://bouncycastle-pgp-cookbook.blogspot.de/

    /**
     * Sign a message using our private PGP key file, this matches gpg -ab "hello.txt"
     *
     * @param privateKeyInputStream
     *                 this is an armored key file, not a binary stream
     * @param userId
     *                 this is the userID to get out of the private key
     * @param password
     *                 this is the password to unlock the private key
     * @param messageAsUtf8Bytes
     *                 this is the message, in bytes, to sign
     */
    public static
    byte[] signGpgCompatible(InputStream privateKeyInputStream, String userId, char[] password, byte[] messageAsUtf8Bytes)
                    throws PGPException {

        // the signature type (in gpg terms), is "sigclass". gpg is BINARY_DOC (0x00)
        return sign(privateKeyInputStream,
                    userId,
                    password,
                    new ByteArrayInputStream(messageAsUtf8Bytes),
                    PGPSignature.BINARY_DOCUMENT,
                    false,
                    true,
                    false,
                    false,
                    false);
    }

    /**
     * Sign a message using our private PGP key file, this matches gpg -ab "hello.txt"
     *
     * @param privateKeyInputStream
     *                 this is an armored key file, not a binary stream
     * @param userId
     *                 this is the userID to get out of the private key
     * @param password
     *                 this is the password to unlock the private key
     * @param message
     *                 this is the message to sign
     */
    public static
    byte[] signGpgCompatible(InputStream privateKeyInputStream, String userId, char[] password, InputStream message)
                    throws PGPException {

        // the signature type (in gpg terms), is "sigclass". gpg is BINARY_DOC (0x00)
        return sign(privateKeyInputStream,
                    userId,
                    password,
                    message,
                    PGPSignature.BINARY_DOCUMENT,
                    false,
                    true,
                    false,
                    false,
                    false);
    }

    /**
     * Sign a message using our private PGP key file, this matches gpg -ab "hello.txt". This will save the signature of the passed-in
     * file to file name + .asc
     *
     * @param privateKeyInputStream
     *                 this is an armored key file, not a binary stream
     * @param userId
     *                 this is the userID to get out of the private key
     * @param password
     *                 this is the password to unlock the private key
     * @param file
     *                 this is the file to sign
     */
    public static
    void signGpgCompatible(InputStream privateKeyInputStream, String userId, char[] password, File file)
                    throws PGPException {

        // the signature type (in gpg terms), is "sigclass". gpg is BINARY_DOC (0x00)
        final byte[] sign = sign(privateKeyInputStream,
                                 userId,
                                 password,
                                 file,
                                 PGPSignature.BINARY_DOCUMENT,
                                 false,
                                 true,
                                 false,
                                 false,
                                 false);

        FileOutputStream fileOutputStream1 = null;
        try {
            fileOutputStream1 = new FileOutputStream(new File(file.getAbsolutePath() + ".asc"));
            fileOutputStream1.write(sign);
            fileOutputStream1.flush();
        } catch (FileNotFoundException e) {
            throw new PGPException("Unable to save signature to file " + file.getAbsolutePath() + ".asc", e);
        } catch (IOException e) {
            throw new PGPException("Unable to save signature to file " + file.getAbsolutePath() + ".asc", e);
        } finally {
            IO.close(fileOutputStream1);
        }
    }

    /**
     * Sign a message using our private PGP key file, with a variety of options
     */
    @SuppressWarnings("Duplicates")
    public static
    byte[] sign(InputStream privateKeyInputStream,
                String userId,
                char[] password,
                InputStream message,
                int signatureType,
                boolean compressSignature,
                boolean asciiArmoredOutput,
                boolean includeDataInSignature,
                boolean generateUserIdSubPacket,
                boolean generateOnePassVersion) throws PGPException {

        List<PGPSecretKey> secretKeys = getSecretKeys(privateKeyInputStream, userId);
        PGPSignatureGenerator signature = createSignature(secretKeys, password, signatureType, generateUserIdSubPacket);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStream outputStream = byteArrayOutputStream;
        if (asciiArmoredOutput) {
            outputStream = new ArmoredOutputStream(byteArrayOutputStream);
        }

        PGPCompressedDataGenerator compressedDataGenerator = null;
        BCPGOutputStream bcOutputStream;

        if (compressSignature) {
            compressedDataGenerator = new PGPCompressedDataGenerator(PGPCompressedData.ZLIB);
            try {
                bcOutputStream = new BCPGOutputStream(compressedDataGenerator.open(outputStream));
            } catch (IOException e) {
                throw new PGPException("Unable to open compression stream in the signature", e);
            }
        }
        else {
            bcOutputStream = new BCPGOutputStream(outputStream);
        }

        if (generateOnePassVersion) {
            try {
                signature.generateOnePassVersion(false)
                         .encode(bcOutputStream);
            } catch (IOException e) {
                throw new PGPException("Unable to generate OnePass signature header", e);
            }
        }

        PGPLiteralDataGenerator literalDataGenerator = null;
        OutputStream literalDataOutput = null;

        if (includeDataInSignature) {
            literalDataGenerator = new PGPLiteralDataGenerator();
            try {
                literalDataOutput = literalDataGenerator.open(bcOutputStream,
                                                              PGPLiteralData.BINARY,
                                                              "_CONSOLE",
                                                              message.available(),
                                                              new Date());
            } catch (IOException e1) {
                throw new PGPException("Unable to generate Literal Data signature header", e1);
            }
        }

        try {
            byte[] buffer = new byte[4096];
            int read;

            // update bytes in the streams
            if (literalDataOutput != null) {
                while ((read = message.read(buffer)) > 0) {
                    literalDataOutput.write(buffer, 0, read);
                    signature.update(buffer, 0, read);
                }
                literalDataOutput.flush();
            } else {

                while ((read = message.read(buffer)) > 0) {
                    signature.update(buffer, 0, read);
                }
            }

            // close generators and update signature
            if (literalDataGenerator != null) {
                literalDataGenerator.close();
            }

            signature.generate()
                     .encode(bcOutputStream);


            if (compressedDataGenerator != null) {
                compressedDataGenerator.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IO.close(bcOutputStream);
            IO.close(outputStream);
            IO.close(literalDataOutput);
        }

        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Sign a message using our private PGP key file, with a variety of options
     */
    @SuppressWarnings("Duplicates")
    public static
    byte[] sign(InputStream privateKeyInputStream,
                String userId,
                char[] password,
                File fileMessage,
                int signatureType,
                boolean compressSignature,
                boolean asciiArmoredOutput,
                boolean includeDataInSignature,
                boolean generateUserIdSubPacket,
                boolean generateOnePassVersion) throws PGPException {

        List<PGPSecretKey> secretKeys = getSecretKeys(privateKeyInputStream, userId);
        PGPSignatureGenerator signature = createSignature(secretKeys, password, signatureType, generateUserIdSubPacket);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStream outputStream = byteArrayOutputStream;
        if (asciiArmoredOutput) {
            outputStream = new ArmoredOutputStream(byteArrayOutputStream);
        }

        PGPCompressedDataGenerator compressedDataGenerator = null;
        BCPGOutputStream bcOutputStream;

        if (compressSignature) {
            compressedDataGenerator = new PGPCompressedDataGenerator(PGPCompressedData.ZLIB);
            try {
                bcOutputStream = new BCPGOutputStream(compressedDataGenerator.open(outputStream));
            } catch (IOException e) {
                throw new PGPException("Unable to open compression stream in the signature", e);
            }
        }
        else {
            bcOutputStream = new BCPGOutputStream(outputStream);
        }

        if (generateOnePassVersion) {
            try {
                signature.generateOnePassVersion(false)
                         .encode(bcOutputStream);
            } catch (IOException e) {
                throw new PGPException("Unable to generate OnePass signature header", e);
            }
        }

        PGPLiteralDataGenerator literalDataGenerator = null;
        OutputStream literalDataOutput = null;

        if (includeDataInSignature) {
            literalDataGenerator = new PGPLiteralDataGenerator();
            try {
                literalDataOutput = literalDataGenerator.open(bcOutputStream,
                                                              PGPLiteralData.BINARY,
                                                              fileMessage);
            } catch (IOException e1) {
                throw new PGPException("Unable to generate Literal Data signature header", e1);
            }
        }

        try {
            final FileInputStream fileInputStream = new FileInputStream(fileMessage);

            byte[] buffer = new byte[4096];
            int read;

            // update bytes in the streams
            if (literalDataOutput != null) {
                while ((read = fileInputStream.read(buffer)) > 0) {
                    literalDataOutput.write(buffer, 0, read);
                    signature.update(buffer, 0, read);
                }
                literalDataOutput.flush();
            } else {

                while ((read = fileInputStream.read(buffer)) > 0) {
                    signature.update(buffer, 0, read);
                }
            }

            // close generators and update signature
            if (literalDataGenerator != null) {
                literalDataGenerator.close();
            }

            signature.generate()
                     .encode(bcOutputStream);


            if (compressedDataGenerator != null) {
                compressedDataGenerator.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IO.close(bcOutputStream);
            IO.close(outputStream);
            IO.close(literalDataOutput);
        }

        return byteArrayOutputStream.toByteArray();
    }


    /**
     * Find private gpg key in InputStream, also closes the input stream
     *
     * @param inputStream
     *                 the inputStream that contains the private (secret) key
     * @param userId
     *                 the user id
     *
     * @return the PGP secret key
     */
    public static
    List<PGPSecretKey> getSecretKeys(InputStream inputStream, String userId) throws PGPException {
        // iterate over every private key in the key ring
        PGPSecretKeyRingCollection secretKeyRings;
        try {
            secretKeyRings = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(inputStream), fingerprintCalculator);
        } catch (IOException e) {
            throw new PGPException("No private key found in stream!", e);
        } finally {
            IO.close(inputStream);
        }

        // look for the key ring that is used to authenticate our reporting facilities
        Iterator<PGPSecretKeyRing> secretKeys = secretKeyRings.getKeyRings(userId);
        List<PGPSecretKey> pgpSecretKeys = new ArrayList<PGPSecretKey>();

        // iterate over every private key in the ring
        while (secretKeys.hasNext()) {
            PGPSecretKeyRing secretKeyRing = secretKeys.next();
            PGPSecretKey tmpKey = secretKeyRing.getSecretKey();

            if (tmpKey != null) {
                pgpSecretKeys.add(tmpKey);
            }
        }

        if (!pgpSecretKeys.isEmpty()) {
            return pgpSecretKeys;
        }

        throw new PGPException("No private key found in stream!");
    }

    /**
     * Creates the signature that will be used to PGP sign data
     *
     * @param secretKeys
     *                 these are the secret keys
     * @param password
     *                 this is the password to unlock the secret key
     *
     * @return the signature used to sign data
     *
     * @throws PGPException
     */
    private static
    PGPSignatureGenerator createSignature(List<PGPSecretKey> secretKeys,
                                          char[] password,
                                          int signatureType,
                                          boolean generateUserIdSubPacket) throws PGPException {

        PGPSecretKey secretKey = null;
        for (int i = 0; i < secretKeys.size(); i++) {
            secretKey = secretKeys.get(i);

            // we ONLY want the signing master key
            if (!secretKey.isSigningKey() || !secretKey.isMasterKey()) {
                secretKey = null;
            }
        }

        if (secretKey == null) {
            throw new PGPException("Secret key is not the signing master key");
        }

//            System.err.println("Signing key = " + tmpKey.isSigningKey() +", Master key = " + tmpKey.isMasterKey() + ", UserId = " +
//                               userId );

        if (password == null) {
            password = new char[0];
        }

        PBESecretKeyDecryptor build = new BcPBESecretKeyDecryptorBuilder(digestCalculatorProvider).build(password);

        SecureRandom random = new SecureRandom();
        BcPGPContentSignerBuilder bcPGPContentSignerBuilder = new BcPGPContentSignerBuilder(secretKey.getPublicKey()
                                                                                                     .getAlgorithm(),
                                                                                            PGPUtil.SHA1).setSecureRandom(random);

        PGPSignatureGenerator signature = new PGPSignatureGenerator(bcPGPContentSignerBuilder);
        signature.init(signatureType, secretKey.extractPrivateKey(build));

        Iterator userIds = secretKey.getPublicKey()
                                    .getUserIDs();

        // use the first userId that matches
        if (userIds.hasNext()) {
            if (generateUserIdSubPacket) {
                PGPSignatureSubpacketGenerator subpacketGenerator = new PGPSignatureSubpacketGenerator();
                subpacketGenerator.setSignerUserID(false, (String) userIds.next());
                signature.setHashedSubpackets(subpacketGenerator.generate());
            }
            else {
                signature.setHashedSubpackets(null);
            }

            return signature;
        }
        else {
            throw new PGPException("Did not find specified userId");
        }
    }










    /**
     * Decode a PGP public key block and return the keyring it represents.
     */
    public static
    PGPPublicKeyRing getKeyring(InputStream keyBlockStream) throws IOException {

        BcKeyFingerprintCalculator keyfp = new BcKeyFingerprintCalculator();

        // PGPUtil.getDecoderStream() will detect ASCII-armor automatically and decode it,
        // the PGPObject factory then knows how to read all the data in the encoded stream
        PGPObjectFactory factory = new PGPObjectFactory(PGPUtil.getDecoderStream(keyBlockStream), keyfp);

        // these files should really just have one object in them, and that object should be a PGPPublicKeyRing.
        Object o = factory.nextObject();
        if (o instanceof PGPPublicKeyRing) {
            return (PGPPublicKeyRing) o;
        }
        throw new IllegalArgumentException("Input stream does not contain a PGP Public Key");
    }

    /**
     * Get the first encryption key from the given keyring.
     */
    public static
    PGPPublicKey getEncryptionKey(PGPPublicKeyRing keyRing) {
        if (keyRing == null) {
            return null;
        }

        // iterate over the keys on the ring, look for one which is suitable for encryption.
        Iterator keys = keyRing.getPublicKeys();
        PGPPublicKey key;
        while (keys.hasNext()) {
            key = (PGPPublicKey) keys.next();
            if (key.isEncryptionKey()) {
                return key;
            }
        }

        return null;
    }

    /**
     * Get the first decryption key from the given keyring.
     */
    public
    PGPSecretKey getDecryptionKey(PGPSecretKeyRing keyRing) {
        if (keyRing == null) {
            return null;
        }

        // iterate over the keys on the ring, look for one which is suitable for encryption.
        Iterator keys = keyRing.getSecretKeys();
        PGPSecretKey key;
        while (keys.hasNext()) {
            key = (PGPSecretKey) keys.next();
            if (key.isMasterKey()) {
                return key;
            }
        }

        return null;
    }


    /**
     * Encrypt plaintext message using public key from publickeyFile.
     *
     * @param message
     *                 the message
     *
     * @return the string
     */
    private
    String encrypt(InputStream publicKeyInputStream, String message) throws PGPException, IOException, NoSuchProviderException {
        // find the PGP key in the file
        PGPPublicKey publicKey = findPublicGPGKey(publicKeyInputStream);

        if (publicKey == null) {
            System.err.println("Did not find public GPG key");
            return null;
        }


        // Encode the string into bytes using utf-8
        byte[] utf8Bytes = message.getBytes(OS.UTF_8);

        ByteArrayOutputStream compressedOutput = new ByteArrayOutputStream();

        // compress bytes with zip
        PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator();

        // the reason why we compress here is GPG not being able to decrypt our message input but if we do not compress.
        // I guess pkzip compression also encodes only to GPG-friendly characters.
        PGPCompressedDataGenerator compressedDataGenerator = new PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP);
        try {
            OutputStream literalDataOutput = literalDataGenerator.open(compressedOutput,
                                                                       PGPLiteralData.BINARY,
                                                                       "_CONSOLE",
                                                                       utf8Bytes.length,
                                                                       new Date());
            // update bytes in the stream
            literalDataOutput.write(utf8Bytes);
        } catch (IOException e) {
            // catch but close the streams in finally
            throw e;
        } finally {
            compressedDataGenerator.close();
            IO.close(compressedOutput);
        }

        SecureRandom random = new SecureRandom();

        // now we have zip-compressed bytes
        byte[] compressedBytes = compressedOutput.toByteArray();

        BcPGPDataEncryptorBuilder bcPGPDataEncryptorBuilder = new BcPGPDataEncryptorBuilder(PGPEncryptedData.CAST5)
                        .setWithIntegrityPacket(true)
                        .setSecureRandom(random);

        PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(bcPGPDataEncryptorBuilder);

        // use public key to encrypt data

        BcPublicKeyKeyEncryptionMethodGenerator encKeyGen = new BcPublicKeyKeyEncryptionMethodGenerator(publicKey)
                        .setSecureRandom(random);

        encryptedDataGenerator.addMethod(encKeyGen);

        // literalDataOutput --> compressedOutput --> ArmoredOutputStream --> ByteArrayOutputStream
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ArmoredOutputStream armoredOut = new ArmoredOutputStream(byteArrayOutputStream);
        OutputStream encryptedOutput = null;
        try {
            encryptedOutput = encryptedDataGenerator.open(armoredOut, compressedBytes.length);
            encryptedOutput.write(compressedBytes);
        } catch (IOException e) {
            throw e;
        } catch (PGPException e) {
            throw e;
        } finally {
            IO.close(encryptedOutput);
            IO.close(armoredOut);
        }
        String encrypted = new String(byteArrayOutputStream.toByteArray());

        System.err.println("Message: " + message);
        System.err.println("Encrypted: " + encrypted);

        return encrypted;
    }

    /**
     * Find public gpg key in InputStream.
     *
     * @param inputStream
     *                 the input stream
     *
     * @return the PGP public key
     */
    private static
    PGPPublicKey findPublicGPGKey(InputStream inputStream) throws IOException, PGPException {

        // get all key rings in the input stream
        PGPPublicKeyRingCollection publicKeyRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(inputStream), fingerprintCalculator);

        System.err.println("key ring size: " + publicKeyRingCollection.size());

        Iterator<PGPPublicKeyRing> keyRingIter = publicKeyRingCollection.getKeyRings();

        // iterate over keyrings
        while (keyRingIter.hasNext()) {
            PGPPublicKeyRing keyRing = keyRingIter.next();
            Iterator<PGPPublicKey> keyIter = keyRing.getPublicKeys();
            // iterate over public keys in the key ring
            while (keyIter.hasNext()) {
                PGPPublicKey tmpKey = keyIter.next();

                if (tmpKey == null) {
                    break;
                }

                Iterator<String> userIDs = tmpKey.getUserIDs();
                ArrayList<String> strings = new ArrayList<String>();
                while (userIDs.hasNext()) {
                    String next = userIDs.next();
                    strings.add(next);
                }

                System.err.println(
                                "Encryption key = " + tmpKey.isEncryptionKey() + ", Master key = " + tmpKey.isMasterKey() + ", UserId = " +
                                strings);

                // we need a master encryption key
                if (tmpKey.isEncryptionKey() && tmpKey.isMasterKey()) {
                    return tmpKey;
                }
            }
        }
        throw new PGPException("No public key found!");
    }



    private static
    void verify(final InputStream publicKeyInputStream, final byte[] signature) throws Exception {
        PGPPublicKey publicKey = findPublicGPGKey(publicKeyInputStream);

        String text = new String(signature);

        Pattern regex = Pattern.compile(
                        "-----BEGIN PGP SIGNED MESSAGE-----\\r?\\n.*?\\r?\\n\\r?\\n(.*)\\r?\\n(-----BEGIN PGP SIGNATURE-----\\r?\\n.*-----END PGP SIGNATURE-----)",
                        Pattern.CANON_EQ | Pattern.DOTALL);
        Matcher regexMatcher = regex.matcher(text);
        if (regexMatcher.find()) {
            String dataText = regexMatcher.group(1);
            String signText = regexMatcher.group(2);

            ByteArrayInputStream dataIn = new ByteArrayInputStream(dataText.getBytes("UTF8"));
            ByteArrayInputStream signIn = new ByteArrayInputStream(signText.getBytes("UTF8"));


            InputStream signIn2 = PGPUtil.getDecoderStream(signIn);

            PGPObjectFactory pgpFact = new PGPObjectFactory(signIn2, new BcKeyFingerprintCalculator());
            PGPSignatureList p3 = null;

            Object o;

            try {
                o = pgpFact.nextObject();
                if (o == null) {
                    throw new Exception();
                }
            } catch (Exception ex) {
                throw new Exception("Invalid input data");
            }

            if (o instanceof PGPCompressedData) {
                PGPCompressedData c1 = (PGPCompressedData) o;

                pgpFact = new PGPObjectFactory(c1.getDataStream(), new BcKeyFingerprintCalculator());

                p3 = (PGPSignatureList) pgpFact.nextObject();
            }
            else {
                p3 = (PGPSignatureList) o;
            }


//            PGPSignature sig = p3.get(0);
//            PGPPublicKey key = KeyRing.getPublicKeyByID(sig.getKeyID());
//
//            if (key == null)
//                throw new Exception("Cannot find key 0x" + Integer.toHexString((int) sig.getKeyID()).toUpperCase() + " in the pubring");
//
//            sig.initVerify(key, "BC");
//
//            while ((ch = dataIn.read()) >= 0) {
//                sig.update((byte) ch); //TODO migliorabile con byte[]
//            }
//
//            if (sig.verify())
//                return new PrintablePGPPublicKey(key).toString();
//            else
//                return null;

//            return verifyFile(dataIn, signIn);

        }
    }


    private
    CryptoPGP() {
    }

    public static
    void main(String[] args) throws Exception {
        InputStream privateKeyInputStream = new FileInputStream(new File("/home/user/dorkbox/sonatype_private.key"));

        byte[] textBytes = "hello".getBytes(OS.UTF_8);

        byte[] bytes = CryptoPGP.signGpgCompatible(privateKeyInputStream, "Dorkbox <sonatype@dorkbox.com>", new char[0], textBytes);

//        String s = new String(hello);
//        String s1 = s.replaceAll("\n", "\r\n");
//        byte[] bytes = s1.getBytes(OS.UTF_8);

//
//        String signed = new String(bytes);
//
//        System.err.println("Message: " + new String(messageAsUtf8Bytes));
//        System.err.println("Signature: " + signed);
//
//        return bytes;

//        String s2 = new String(bytes);


//        InputStream publicKeyInputStream = new FileInputStream(new File("/home/user/dorkbox/sonatype_public.key"));
//        cryptoPGP.verify(publicKeyInputStream, hello);


        FileOutputStream fileOutputStream = new FileOutputStream(new File("/home/user/dorkbox/hello2.txt"));
        fileOutputStream.write(textBytes);
        fileOutputStream.flush();
        IO.close(fileOutputStream);


        FileOutputStream fileOutputStream1 = new FileOutputStream(new File("/home/user/dorkbox/hello2.txt.asc"));
        fileOutputStream1.write(bytes);
        fileOutputStream1.flush();
        IO.close(fileOutputStream1);
    }
}
