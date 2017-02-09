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
