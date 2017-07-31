/*
 * Copyright 2014 dorkbox, llc
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
