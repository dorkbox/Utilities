package dorkbox.util.bytes;

import java.util.Arrays;

/**
 * Necessary to provide equals and hashcode methods on a byte arrays, if they are to be used as keys in a map/set/etc
 */
public final class ByteArrayWrapper {
    private final byte[] data;
    private Integer hashCode;

    /**
     * Makes a safe copy of the byte array, so that changes to the original do not affect the wrapper.
     * Side affect is additional memory is used.
     */
    public static ByteArrayWrapper copy(byte[] data) {
        return new ByteArrayWrapper(data, true);
    }


    /**
     * Does not make a copy of the data, so changes to the original will also affect the wrapper.
     * Side affect is no extra memory is needed.
     */
    public static ByteArrayWrapper wrap(byte[] data) {
        return new ByteArrayWrapper(data, false);
    }

    /**
     * Permits the re-use of a byte array.
     * @param copyBytes if TRUE, then the byteArray is copies. if FALSE, the byte array is uses as-is.
     *                    Using FALSE IS DANGEROUS!!!! If the underlying byte array is modified, this changes as well.
     */
    private ByteArrayWrapper(byte[] data, boolean copyBytes) {
        if (data == null) {
            throw new NullPointerException();
        }
        int length = data.length;

        if (copyBytes) {
            this.data = new byte[length];
            // copy so it's immutable as a key.
            System.arraycopy(data, 0, this.data, 0, length);
        } else {
            this.data = data;
        }
    }

    public byte[] getBytes() {
        return this.data;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ByteArrayWrapper)) {
            return false;
        }

        // CANNOT be null, so we don't have to null check!
        return Arrays.equals(this.data, ((ByteArrayWrapper) other).data);
    }

    @Override
    public int hashCode() {
        // might be null for a thread because it's stale. who cares, get the value again
        Integer hashCode = this.hashCode;
        if (hashCode == null) {
            hashCode = Arrays.hashCode(this.data);
            this.hashCode = hashCode;
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return "ByteArrayWrapper " + java.util.Arrays.toString(this.data);
    }
}