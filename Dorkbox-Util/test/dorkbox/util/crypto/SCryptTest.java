
package dorkbox.util.crypto;



import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.junit.Test;

import dorkbox.util.Sys;


public class SCryptTest {

    @Test
    public void SCrypt() throws IOException, GeneralSecurityException {

        byte[] P, S;
        int N, r, p, dkLen;
        String DK;

        // empty key & salt test missing because unsupported by JCE

        P = "password".getBytes("UTF-8");
        S = "NaCl".getBytes("UTF-8");
        N = 1024;
        r = 8;
        p = 16;
        dkLen = 64;
        DK = "FDBABE1C9D3472007856E7190D01E9FE7C6AD7CBC8237830E77376634B3731622EAF30D92E22A3886FF109279D9830DAC727AFB94A83EE6D8360CBDFA2CC0640";

        assertEquals(DK, Sys.bytesToHex(CryptoSCrypt.encrypt(P, S, N, r, p, dkLen)));


        P = "pleaseletmein".getBytes("UTF-8");
        S = "SodiumChloride".getBytes("UTF-8");
        N = 16384;
        r = 8;
        p = 1;
        dkLen = 64;
        DK = "7023BDCB3AFD7348461C06CD81FD38EBFDA8FBBA904F8E3EA9B543F6545DA1F2D5432955613F0FCF62D49705242A9AF9E61E85DC0D651E40DFCF017B45575887";

        assertEquals(DK, Sys.bytesToHex(CryptoSCrypt.encrypt(P, S, N, r, p, dkLen)));


        P = "pleaseletmein".getBytes("UTF-8");
        S = "SodiumChloride".getBytes("UTF-8");
        N = 1048576;
        r = 8;
        p = 1;
        dkLen = 64;
        DK = "2101CB9B6A511AAEADDBBE09CF70F881EC568D574A2FFD4DABE5EE9820ADAA478E56FD8F4BA5D09FFA1C6D927C40F4C337304049E8A952FBCBF45C6FA77A41A4";

        assertEquals(DK, Sys.bytesToHex(CryptoSCrypt.encrypt(P, S, N, r, p, dkLen)));
    }
}
