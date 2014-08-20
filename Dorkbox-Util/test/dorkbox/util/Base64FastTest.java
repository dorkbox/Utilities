package dorkbox.util;


import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

public class Base64FastTest {

    @Test
    public void base64Test() throws IOException {
        byte[] randomData = new byte[1000000];
        new Random().nextBytes(randomData);

        byte[] enc = Base64Fast.encodeToByte(randomData, true);
        byte[] dec = Base64Fast.decode(enc);

        if (!Arrays.equals(randomData, dec)) {
            fail("base64 test failed");
        }

        randomData = null;
    }
}