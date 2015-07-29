
package dorkbox.util.crypto;

import dorkbox.util.crypto.bouncycastle.GCMBlockCipher_ByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.junit.Test;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;

import static org.junit.Assert.fail;

public class AesByteBufTest {

    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());
    private static String entropySeed = "asdjhasdkljalksdfhlaks4356268909087s0dfgkjh255124515hasdg87";

    private  String text = "hello, my name is inigo montoya. hello, my name is inigo montoya. hello, my name is inigo montoya." +
            "hello, my name is inigo montoya. hello, my name is inigo montoya. hello, my name is inigo montoya." +
            "hello, my name is inigo montoya. hello, my name is inigo montoya. hello, my name is inigo montoya." +
            "hello, my name is inigo montoya. hello, my name is inigo montoya. hello, my name is inigo montoya." +
            "hello, my name is inigo montoya. hello, my name is inigo montoya. hello, my name is inigo montoya." +
            "hello, my name is inigo montoya. hello, my name is inigo montoya. hello, my name is inigo montoya." +
            "hello, my name is inigo montoya. hello, my name is inigo montoya. hello, my name is inigo montoya." +
            "hello, my name is inigo montoya. hello, my name is inigo montoya. hello, my name is inigo montoya." +
            "hello, my name is inigo montoya. hello, my name is inigo montoya. hello, my name is inigo montoya." +
            "hello, my name is inigo montoya. hello, my name is inigo montoya. hello, my name is inigo montoya." +
            "hello, my name is inigo montoya. hello, my name is inigo montoya. hello, my name is inigo montoya." +
            "hello, my name is inigo montoya. hello, my name is inigo montoya. hello, my name is inigo montoya." +
            "hello, my name is inigo montoya. hello, my name is inigo montoya. hello, my name is inigo montoya." +
            "hello, my name is inigo montoya. hello, my name is inigo montoya. hello, my name is inigo montoya." +
            "hello, my name is inigo montoya. hello, my name is inigo montoya. hello, my name is inigo montoya." +
            "hello, my name is inigo montoya. hello, my name is inigo montoya. hello, my name is inigo montoya." +
            "hello, my name is inigo montoya. hello, my name is inigo montoya. hello, my name is inigo montoya.";

    // test input smaller than block size
    private byte[] bytes = "hello!".getBytes();

    // test input larger than block size
    private byte[] bytesLarge = text.getBytes();

    @Test
    public void AesGcmEncryptBothA() throws IOException {

        final byte[] SOURCE = bytesLarge;

        SecureRandom rand = new SecureRandom(entropySeed.getBytes());

        final GCMBlockCipher_ByteBuf aesEngine1 = new GCMBlockCipher_ByteBuf(new AESFastEngine());
        final GCMBlockCipher aesEngine2 = new GCMBlockCipher(new AESFastEngine());

        final byte[] key = new byte[32];
        final byte[] iv = new byte[16];

        // note: the IV needs to be VERY unique!
        rand.nextBytes(key);  // 256bit key (32 bytes)
        rand.nextBytes(iv); // 128bit block size (16 bytes)

        final ParametersWithIV aesIVAndKey = new ParametersWithIV(new KeyParameter(key), iv);

        ByteBuf source = Unpooled.wrappedBuffer(SOURCE);
        int length = SOURCE.length;
        ByteBuf encryptAES = Unpooled.buffer(1024);
        int encryptLength = Crypto.AES.encrypt(aesEngine1, aesIVAndKey, source, encryptAES, length, logger);

        byte[] encrypt = new byte[encryptLength];
        System.arraycopy(encryptAES.array(), 0, encrypt, 0, encryptLength);

        byte[] encrypt2 = Crypto.AES.encrypt(aesEngine2, key, iv, SOURCE, logger);


        if (Arrays.equals(SOURCE, encrypt)) {
            fail("bytes should not be equal");
        }

        if (!Arrays.equals(encrypt, encrypt2)) {
            fail("bytes not equal");
        }
    }

    @Test
    public void AesGcmEncryptBothB() throws IOException {
        final byte[] SOURCE = bytes;

        SecureRandom rand = new SecureRandom(entropySeed.getBytes());

        final GCMBlockCipher_ByteBuf aesEngine1 = new GCMBlockCipher_ByteBuf(new AESFastEngine());
        final GCMBlockCipher aesEngine2 = new GCMBlockCipher(new AESFastEngine());

        final byte[] key = new byte[32];
        final byte[] iv = new byte[16];

        // note: the IV needs to be VERY unique!
        rand.nextBytes(key);  // 256bit key (32 bytes)
        rand.nextBytes(iv); // 128bit block size (16 bytes)

        final ParametersWithIV aesIVAndKey = new ParametersWithIV(new KeyParameter(key), iv);

        ByteBuf source = Unpooled.wrappedBuffer(SOURCE);
        int length = SOURCE.length;
        ByteBuf encryptAES = Unpooled.buffer(1024);
        int encryptLength = Crypto.AES.encrypt(aesEngine1, aesIVAndKey, source, encryptAES, length, logger);

        byte[] encrypt = new byte[encryptLength];
        System.arraycopy(encryptAES.array(), 0, encrypt, 0, encryptLength);


        byte[] encrypt2 = Crypto.AES.encrypt(aesEngine2, key, iv, SOURCE, logger);


        if (Arrays.equals(SOURCE, encrypt)) {
            fail("bytes should not be equal");
        }

        if (!Arrays.equals(encrypt, encrypt2)) {
            fail("bytes not equal");
        }
    }

    @Test
    public void AesGcmEncryptBufOnly() throws IOException {

        SecureRandom rand = new SecureRandom(entropySeed.getBytes());

        GCMBlockCipher_ByteBuf aesEngine1 = new GCMBlockCipher_ByteBuf(new AESFastEngine());
        GCMBlockCipher aesEngine2 = new GCMBlockCipher(new AESFastEngine());

        byte[] key = new byte[32];
        byte[] iv = new byte[16];

        // note: the IV needs to be VERY unique!
        rand.nextBytes(key);  // 256bit key (32 bytes)
        rand.nextBytes(iv); // 128bit block size (16 bytes)

        ParametersWithIV aesIVAndKey = new ParametersWithIV(new KeyParameter(key), iv);
        ByteBuf source = Unpooled.wrappedBuffer(bytes);
        int length = bytes.length;
        ByteBuf encryptAES = Unpooled.buffer(1024);
        int encryptLength = Crypto.AES.encrypt(aesEngine1, aesIVAndKey, source, encryptAES, length, logger);

        byte[] encrypt = new byte[encryptLength];
        System.arraycopy(encryptAES.array(), 0, encrypt, 0, encryptLength);


        byte[] decrypt = Crypto.AES.decrypt(aesEngine2, key, iv, encrypt, logger);


        if (Arrays.equals(bytes, encrypt)) {
            fail("bytes should not be equal");
        }

        if (!Arrays.equals(bytes, decrypt)) {
            fail("bytes not equal");
        }
    }

    @Test
    public void AesGcmDecryptBothA() throws IOException {

        final byte[] SOURCE = bytesLarge;

        SecureRandom rand = new SecureRandom(entropySeed.getBytes());

        final GCMBlockCipher aesEngine1 = new GCMBlockCipher(new AESFastEngine());
        final GCMBlockCipher_ByteBuf aesEngine2 = new GCMBlockCipher_ByteBuf(new AESFastEngine());

        final byte[] key = new byte[32];
        final byte[] iv = new byte[16];

        // note: the IV needs to be VERY unique!
        rand.nextBytes(key);  // 256bit key (32 bytes)
        rand.nextBytes(iv); // 128bit block size (16 bytes)

        final byte[] encrypt = Crypto.AES.encrypt(aesEngine1, key, iv, SOURCE, logger);
        final ByteBuf encryptAES = Unpooled.wrappedBuffer(encrypt);
        final int length = encrypt.length;

        byte[] decrypt1 = Crypto.AES.decrypt(aesEngine1, key, iv, encrypt, logger);


        ParametersWithIV aesIVAndKey = new ParametersWithIV(new KeyParameter(key), iv);
        ByteBuf decryptAES = Unpooled.buffer(1024);
        int decryptLength = Crypto.AES.decrypt(aesEngine2, aesIVAndKey, encryptAES, decryptAES, length, logger);
        byte[] decrypt2 = new byte[decryptLength];
        System.arraycopy(decryptAES.array(), 0, decrypt2, 0, decryptLength);


        if (Arrays.equals(SOURCE, encrypt)) {
            fail("bytes should not be equal");
        }

        if (!Arrays.equals(decrypt1, decrypt2)) {
            fail("bytes not equal");
        }

        if (!Arrays.equals(SOURCE, decrypt1)) {
            fail("bytes not equal");
        }
    }

    @Test
    public void AesGcmDecryptBothB() throws IOException {

        byte[] SOURCE = bytes;

        SecureRandom rand = new SecureRandom(entropySeed.getBytes());

        final GCMBlockCipher aesEngine1 = new GCMBlockCipher(new AESFastEngine());
        final GCMBlockCipher_ByteBuf aesEngine2 = new GCMBlockCipher_ByteBuf(new AESFastEngine());

        final byte[] key = new byte[32];
        final byte[] iv = new byte[16];

        // note: the IV needs to be VERY unique!
        rand.nextBytes(key);  // 256bit key (32 bytes)
        rand.nextBytes(iv); // 128bit block size (16 bytes)

        final byte[] encrypt = Crypto.AES.encrypt(aesEngine1, key, iv, SOURCE, logger);
        final ByteBuf encryptAES = Unpooled.wrappedBuffer(encrypt);
        final int length = encrypt.length;


        byte[] decrypt1 = Crypto.AES.decrypt(aesEngine1, key, iv, encrypt, logger);


        ParametersWithIV aesIVAndKey = new ParametersWithIV(new KeyParameter(key), iv);
        ByteBuf decryptAES = Unpooled.buffer(1024);
        int decryptLength = Crypto.AES.decrypt(aesEngine2, aesIVAndKey, encryptAES, decryptAES, length, logger);
        byte[] decrypt2 = new byte[decryptLength];
        System.arraycopy(decryptAES.array(), 0, decrypt2, 0, decryptLength);


        if (Arrays.equals(SOURCE, encrypt)) {
            fail("bytes should not be equal");
        }

        if (!Arrays.equals(decrypt1, decrypt2)) {
            fail("bytes not equal");
        }

        if (!Arrays.equals(SOURCE, decrypt1)) {
            fail("bytes not equal");
        }
    }

    @Test
    public void AesGcmDecryptBufOnlyA() throws IOException {
        byte[] SOURCE = bytesLarge;

        SecureRandom rand = new SecureRandom(entropySeed.getBytes());

        GCMBlockCipher aesEngine1 = new GCMBlockCipher(new AESFastEngine());
        GCMBlockCipher_ByteBuf aesEngine2 = new GCMBlockCipher_ByteBuf(new AESFastEngine());

        byte[] key = new byte[32];
        byte[] iv = new byte[16];

        // note: the IV needs to be VERY unique!
        rand.nextBytes(key);  // 256bit key (32 bytes)
        rand.nextBytes(iv); // 128bit block size (16 bytes)


        byte[] encrypt = Crypto.AES.encrypt(aesEngine1, key, iv, SOURCE, logger);
        ByteBuf encryptAES = Unpooled.wrappedBuffer(encrypt);
        int length = encrypt.length;


        ParametersWithIV aesIVAndKey = new ParametersWithIV(new KeyParameter(key), iv);
        ByteBuf decryptAES = Unpooled.buffer(1024);
        int decryptLength = Crypto.AES.decrypt(aesEngine2, aesIVAndKey, encryptAES, decryptAES, length, logger);
        byte[] decrypt = new byte[decryptLength];
        System.arraycopy(decryptAES.array(), 0, decrypt, 0, decryptLength);

        if (Arrays.equals(SOURCE, encrypt)) {
            fail("bytes should not be equal");
        }

        if (!Arrays.equals(SOURCE, decrypt)) {
            fail("bytes not equal");
        }
    }

    @Test
    public void AesGcmDecryptBufOnlyB() throws IOException {
        byte[] SOURCE = bytes;

        SecureRandom rand = new SecureRandom(entropySeed.getBytes());

        GCMBlockCipher aesEngine1 = new GCMBlockCipher(new AESFastEngine());
        GCMBlockCipher_ByteBuf aesEngine2 = new GCMBlockCipher_ByteBuf(new AESFastEngine());

        byte[] key = new byte[32];
        byte[] iv = new byte[16];

        // note: the IV needs to be VERY unique!
        rand.nextBytes(key);  // 256bit key (32 bytes)
        rand.nextBytes(iv); // 128bit block size (16 bytes)


        byte[] encrypt = Crypto.AES.encrypt(aesEngine1, key, iv, SOURCE, logger);
        ByteBuf encryptAES = Unpooled.wrappedBuffer(encrypt);
        int length = encrypt.length;


        ParametersWithIV aesIVAndKey = new ParametersWithIV(new KeyParameter(key), iv);
        ByteBuf decryptAES = Unpooled.buffer(1024);
        int decryptLength = Crypto.AES.decrypt(aesEngine2, aesIVAndKey, encryptAES, decryptAES, length, logger);
        byte[] decrypt = new byte[decryptLength];
        System.arraycopy(decryptAES.array(), 0, decrypt, 0, decryptLength);

        if (Arrays.equals(SOURCE, encrypt)) {
            fail("bytes should not be equal");
        }

        if (!Arrays.equals(SOURCE, decrypt)) {
            fail("bytes not equal");
        }
    }
}
