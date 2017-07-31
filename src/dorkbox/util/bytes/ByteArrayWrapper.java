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
package dorkbox.util.bytes;

import java.util.Arrays;

/**
 * Necessary to provide equals and hashcode methods on a byte arrays, if they are to be used as keys in a map/set/etc
 */
public
class ByteArrayWrapper {
    private byte[] data;
    private Integer hashCode;

    private
    ByteArrayWrapper() {
        // this is necessary for kryo
    }

    /**
     * Permits the re-use of a byte array.
     *
     * @param copyBytes if TRUE, then the byteArray is copied. if FALSE, the byte array is used as-is.
     *                  Using FALSE IS DANGEROUS!!!! If the underlying byte array is modified, this changes as well.
     */
    public
    ByteArrayWrapper(byte[] data, boolean copyBytes) {
        if (data == null) {
            throw new NullPointerException();
        }
        int length = data.length;

        if (copyBytes) {
            this.data = new byte[length];
            // copy so it's immutable as a key.
            System.arraycopy(data, 0, this.data, 0, length);
        }
        else {
            this.data = data;
        }
    }

    /**
     * Makes a safe copy of the byte array, so that changes to the original do not affect the wrapper.
     * Side affect is additional memory is used.
     */
    public static
    ByteArrayWrapper copy(byte[] data) {
        if (data == null) {
            return null;
        }

        return new ByteArrayWrapper(data, true);
    }

    /**
     * Does not make a copy of the data, so changes to the original will also affect the wrapper.
     * Side affect is no extra memory is needed.
     */
    public static
    ByteArrayWrapper wrap(byte[] data) {
        if (data == null) {
            return null;
        }
        return new ByteArrayWrapper(data, false);
    }

    public
    byte[] getBytes() {
        return this.data;
    }

    @Override
    public
    int hashCode() {
        // might be null for a thread because it's stale. who cares, get the value again
        Integer hashCode = this.hashCode;
        if (hashCode == null) {
            hashCode = Arrays.hashCode(this.data);
            this.hashCode = hashCode;
        }
        return hashCode;
    }

    @Override
    public
    boolean equals(Object other) {
        if (!(other instanceof ByteArrayWrapper)) {
            return false;
        }

        // CANNOT be null, so we don't have to null check!
        return Arrays.equals(this.data, ((ByteArrayWrapper) other).data);
    }

    @Override
    public
    String toString() {
        return "ByteArrayWrapper " + java.util.Arrays.toString(this.data);
    }
}
