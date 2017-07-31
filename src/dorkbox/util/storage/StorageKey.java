package dorkbox.util.storage;

import dorkbox.util.HashUtil;
import dorkbox.util.bytes.ByteArrayWrapper;

/**
 * Make a ByteArrayWrapper that is really a SHA256 hash of the bytes.
 */
public
class StorageKey extends ByteArrayWrapper {
    public
    StorageKey(String key) {
        super(HashUtil.getSha256(key), false);
    }

    public
    StorageKey(byte[] key) {
        super(key, false);
    }
}
