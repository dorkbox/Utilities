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
package dorkbox.util;

import org.bouncycastle.crypto.digests.SHA256Digest;

/**
 * Bouncycastle hashes!
 */
public
class HashUtil {
    /**
     * gets the SHA256 hash + SALT of the specified username, as UTF-16
     */
    public static
    byte[] getSha256WithSalt(String username, byte[] saltBytes) {
        if (username == null) {
            return null;
        }

        byte[] charToBytes = Sys.charToBytes(username.toCharArray());
        byte[] userNameWithSalt = Sys.concatBytes(charToBytes, saltBytes);


        SHA256Digest sha256 = new SHA256Digest();
        byte[] usernameHashBytes = new byte[sha256.getDigestSize()];
        sha256.update(userNameWithSalt, 0, userNameWithSalt.length);
        sha256.doFinal(usernameHashBytes, 0);

        return usernameHashBytes;
    }

    /**
     * gets the SHA256 hash of the specified string, as UTF-16
     */
    public static
    byte[] getSha256(String string) {
        byte[] charToBytes = Sys.charToBytes(string.toCharArray());

        SHA256Digest sha256 = new SHA256Digest();
        byte[] usernameHashBytes = new byte[sha256.getDigestSize()];
        sha256.update(charToBytes, 0, charToBytes.length);
        sha256.doFinal(usernameHashBytes, 0);

        return usernameHashBytes;
    }

    /**
     * gets the SHA256 hash of the specified byte array
     */
    public static
    byte[] getSha256(byte[] bytes) {

        SHA256Digest sha256 = new SHA256Digest();
        byte[] hashBytes = new byte[sha256.getDigestSize()];
        sha256.update(bytes, 0, bytes.length);
        sha256.doFinal(hashBytes, 0);

        return hashBytes;
    }
}
