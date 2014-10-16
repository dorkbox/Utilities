package dorkbox.util.bytes;

import java.nio.BufferUnderflowException;

/**
 * Cleanroom implementation of a self-growing bytebuffer. NOT SYNCHRONIZED!
 */
public class ByteBuffer2Fast {

    private byte[] bytes;

    private int position = 0;
    private int mark = -1;
    private int limit = 0;

    public static ByteBuffer2Fast wrap(byte[] buffer) {
        return new ByteBuffer2Fast(buffer);
    }

    public static ByteBuffer2Fast allocate(int capacity) {
        ByteBuffer2Fast byteBuffer2 = new ByteBuffer2Fast(new byte[capacity]);
        byteBuffer2.clear();
        return byteBuffer2;
    }

    public ByteBuffer2Fast() {
        this(0);
    }

    public ByteBuffer2Fast(int size) {
        this(new byte[size]);
    }

    public ByteBuffer2Fast(byte[] bytes) {
        this.bytes = bytes;
        clear();
        this.position = bytes.length;
    }

    public byte getByte() {
        if (this.position > this.limit) {
            throw new BufferUnderflowException();
        }

        return this.bytes[this.position++];
    }

    public byte getByte(int i) {
        if (i > this.limit) {
            throw new BufferUnderflowException();
        }

        return this.bytes[i];
    }

    public void getBytes(byte[] buffer) {
        getBytes(buffer, 0, buffer.length);
    }

    public void getBytes(byte[] buffer, int length) {
        getBytes(buffer, 0, length);
    }

    public void getBytes(byte[] buffer, int offset, int length) {
        if (this.position + length - offset > this.limit) {
            throw new BufferUnderflowException();
        }

        System.arraycopy(this.bytes, this.position, buffer, 0, length-offset);
        this.position += length-offset;
    }

    /**
     * MUST call checkBuffer before calling this!
     *
     *  NOT PROTECTED
     */
    private final void _put(byte b) {
        this.bytes[this.position++] = b;
    }

    /** NOT PROTECTED! */
    private final void checkBuffer(int threshold) {
        if (this.bytes.length < threshold) {
            byte[] t = new byte[threshold];
            // grow at back of array
            System.arraycopy(this.bytes, 0, t, 0, this.bytes.length);
            this.limit = t.length;

            this.bytes = t;
        }
    }

    public final void put(ByteBuffer2Fast buffer) {
        putBytes(buffer.array(), buffer.position, buffer.limit);
        buffer.position = buffer.limit;
    }

    public final ByteBuffer2Fast putBytes(byte[] src) {
        return putBytes(src, 0, src.length);
    }

    public final ByteBuffer2Fast putBytes(byte[] src, int offset, int length) {
        checkBuffer(this.position + length - offset);

        System.arraycopy(src, offset, this.bytes, this.position, length);
        this.position += length;

        return this;
    }

    public final ByteBuffer2Fast putByte(byte b) {
        checkBuffer(this.position + 1);

        _put(b);
        return this;
    }

    public final void putByte(int position, byte b) {
        this.position = position;
        putByte(b);
    }

    public final void putChar(char c) {
        checkBuffer(this.position + 2);

        putBytes(BigEndian.Char_.toBytes(c));
    }

    public final char getChar() {
        return BigEndian.Char_.fromBytes(getByte(), getByte());
    }

    public final char getChar(int i) {
        return BigEndian.Char_.fromBytes(getByte(i++), getByte(i));
    }

    public void getChars(int srcStart, int srcLength, char[] dest, int destStart) {
        for (int i=srcStart;i<srcLength;i+=2) {
            char c = BigEndian.Char_.fromBytes(getByte(i), getByte(i+1));
            dest[destStart++] = c;
        }
    }

    public final ByteBuffer2Fast putShort(short x) {
        checkBuffer(this.position + 2);

        putBytes(BigEndian.Short_.toBytes(x));

        return this;
    }

    public final short getShort() {
        return BigEndian.Short_.fromBytes(getByte(), getByte());
    }

    public final short getShort(int i) {
        return BigEndian.Short_.fromBytes(getByte(i++), getByte(i));
    }

    public final void putInt(int x) {
        checkBuffer(this.position + 4);

        putBytes(BigEndian.Int_.toBytes(x));
    }

    public final int getInt() {
        return BigEndian.Int_.fromBytes(getByte(), getByte(), getByte(), getByte());
    }

    public final int getInt(int i) {
        return BigEndian.Int_.fromBytes(getByte(i++), getByte(i++), getByte(i++), getByte(i++));
    }

    public final void putLong(long x) {
        checkBuffer(this.position + 8);

        putBytes(BigEndian.Long_.toBytes(x));
    }

    public final long getLong() {
        return BigEndian.Long_.fromBytes(getByte(), getByte(), getByte(), getByte(), getByte(), getByte(), getByte(), getByte());
    }

    public final long getLong(int i) {
        return BigEndian.Long_.fromBytes(getByte(i++), getByte(i++), getByte(i++), getByte(i++), getByte(i++), getByte(i++), getByte(i++), getByte(i++));
    }

    /**
     * Returns the backing array of this buffer
     */
    public byte[] array() {
        return this.bytes;
    }

    /**
     * Returns a copy of the backing array of this buffer
     */
    public final byte[] arrayCopy() {
        int length = this.bytes.length - this.position;

        byte[] b = new byte[length];
        System.arraycopy(this.bytes, this.position, b, 0, length);
        return b;
    }

    /**
     * Returns this buffer's position.
     */
    public int position() {
        return this.position;
    }

    /**
     * Sets this buffer's position.
     */
    public final ByteBuffer2Fast position(int position) {
        if (position > this.bytes.length || position < 0) {
            throw new IllegalArgumentException();
        }

        this.position = position;
        if (this.mark > position) {
            this.mark = -1;
        }

        return this;
    }

    /**
     * Returns the number of elements between the current position and the
     * limit.
     */
    public final int remaining() {
        return this.limit - this.position;
    }

    /**
     * Tells whether there are any elements between the current position and
     * the limit.
     */
    public final boolean hasRemaining() {
        return this.position < this.limit;
    }

    /**
     * Sets this buffer's limit.
     */
    public final void limit(int limit) {
        this.limit = limit;
        if (this.position > limit) {
            this.position = limit;
        }
        if (this.mark > limit) {
            this.mark = -1;
        }
    }

    /**
     * Returns this buffer's limit.
     */
    public int limit() {
        return this.limit;
    }

    /**
     * Returns this buffer's capacity.
     */
    public int capacity() {
        return this.bytes.length;
    }

    /**
     *  The bytes between the buffer's current position and its limit, if any, are copied to the beginning of the buffer.
     *  That is, the byte at index p = position() is copied to index zero, the byte at index p + 1 is copied to index one,
     *  and so forth until the byte at index limit() - 1 is copied to index n = limit() - 1 - p. The buffer's position is
     *  then set to n+1 and its limit is set to its capacity. The mark, if defined, is discarded.
     *
     *  The buffer's position is set to the number of bytes copied, rather than to zero, so that an invocation of this method
     *  can be followed immediately by an invocation of another relative put method.
     */
    public final void compact() {
        this.mark = -1;
        System.arraycopy(this.bytes, this.position, this.bytes, 0, remaining());

        position(remaining());
        limit(capacity());
    }

    /**
     * Readies the buffer for reading.
     *
     * Flips this buffer.  The limit is set to the current position and then
     * the position is set to zero.  If the mark is defined then it is
     * discarded.
     */
    public final void flip() {
        this.limit = this.position;
        this.position = 0;
        this.mark = -1;
    }

    /**
     * Clears this buffer.  The position is set to zero, the limit is set to
     * the capacity, and the mark is discarded.
     */
    public final void clear() {
        this.position = 0;
        this.limit = capacity();
        this.mark = -1;
    }

    /**
     * Rewinds this buffer.  The position is set to zero and the mark is
     * discarded.
     */
    public final void rewind() {
        this.position = 0;
        this.mark = -1;
    }

    /**
     * Sets this buffer's mark at its position.
     */
    public final void mark() {
        this.mark = this.position;
    }

    /**
     * Resets this buffer's position to the previously-marked position.
     *
     * <p> Invoking this method neither changes nor discards the mark's
     * value. </p>
     */
    public void reset() {
        this.position = this.mark;
    }
}