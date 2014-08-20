package dorkbox.util.bytes;

import java.nio.BufferUnderflowException;

/**
 * Cleanroom implementation of a self-growing bytebuffer
 */
public class ByteBuffer2 {

    private byte[] bytes;

    private int position = 0;
    private int mark = -1;
    private int limit = 0;

    public static ByteBuffer2 wrap(byte[] buffer) {
        return new ByteBuffer2(buffer);
    }

    public static ByteBuffer2 allocate(int capacity) {
        ByteBuffer2 byteBuffer2 = new ByteBuffer2(new byte[capacity]);
        byteBuffer2.clear();
        return byteBuffer2;
    }

    public ByteBuffer2() {
        this(0);
    }

    public ByteBuffer2(int size) {
        this(new byte[size]);
    }

    public ByteBuffer2(byte[] bytes) {
        this.bytes = bytes;
        clear();
        position = bytes.length;
    }

    public byte getByte() {
        if (position > limit) {
            throw new BufferUnderflowException();
        }

        return bytes[position++];
    }

    public byte getByte(int i) {
        if (i > limit) {
            throw new BufferUnderflowException();
        }

        return bytes[i];
    }

    public void getBytes(byte[] buffer) {
        getBytes(buffer, 0, buffer.length);
    }

    public void getBytes(byte[] buffer, int length) {
        getBytes(buffer, 0, length);
    }

    public void getBytes(byte[] buffer, int offset, int length) {
        if (position + length - offset > limit) {
            throw new BufferUnderflowException();
        }

        System.arraycopy(bytes, position, buffer, 0, length-offset);
        position += length-offset;
    }

    /**
     * MUST call checkBuffer before calling this!
     *
     *  NOT PROTECTED
     */
    private final void _put(byte b) {
        bytes[position++] = b;
    }

    /** NOT PROTECTED! */
    private final void checkBuffer(int threshold) {
        if (bytes.length < threshold) {
            byte[] t = new byte[threshold];
            // grow at back of array
            System.arraycopy(bytes, 0, t, 0, bytes.length);
            limit = t.length;

            bytes = t;
        }
    }

    public final synchronized void put(ByteBuffer2 buffer) {
        putBytes(buffer.array(), buffer.position, buffer.limit);
        buffer.position = buffer.limit;
    }

    public final synchronized ByteBuffer2 putBytes(byte[] src) {
        return putBytes(src, 0, src.length);
    }

    public final synchronized ByteBuffer2 putBytes(byte[] src, int offset, int length) {
        checkBuffer(position + length - offset);

        System.arraycopy(src, offset, bytes, position, length);
        position += length;

        return this;
    }

    public final synchronized ByteBuffer2 putByte(byte b) {
        checkBuffer(position + 1);

        _put(b);
        return this;
    }

    public final synchronized void putByte(int position, byte b) {
        this.position = position;
        putByte(b);
    }

    public final synchronized void putChar(char c) {
        checkBuffer(position + 2);

        putBytes(BigEndian.Char_.toBytes(c));
    }

    public final synchronized char getChar() {
        return BigEndian.Char_.fromBytes(getByte(), getByte());
    }

    public final synchronized ByteBuffer2 putShort(short x) {
        checkBuffer(position + 2);

        putBytes(BigEndian.Short_.toBytes(x));

        return this;
    }

    public final synchronized short getShort() {
        return BigEndian.Short_.fromBytes(getByte(), getByte());
    }

    public final synchronized void putInt(int x) {
        checkBuffer(position + 4);

        putBytes(BigEndian.Int_.toBytes(x));
    }

    public final synchronized int getInt() {
        byte b3 = getByte();
        byte b2 = getByte();
        byte b1 = getByte();
        return BigEndian.Int_.fromBytes(getByte(), b1, b2, b3);
    }

    public final synchronized void putLong(long x) {
        checkBuffer(position + 8);

        putBytes(BigEndian.Long_.toBytes(x));
    }

    public final synchronized long getLong() {
        return BigEndian.Long_.fromBytes(getByte(), getByte(), getByte(), getByte(), getByte(), getByte(), getByte(), getByte());
    }

    /**
     * Returns the backing array of this buffer
     */
    public byte[] array() {
        return bytes;
    }

    /**
     * Returns a copy of the backing array of this buffer
     */
    public final synchronized byte[] arrayCopy() {
        int length = bytes.length - position;

        byte[] b = new byte[length];
        System.arraycopy(bytes, position, b, 0, length);
        return b;
    }

    /**
     * Returns this buffer's position.
     */
    public int position() {
        return position;
    }

    /**
     * Sets this buffer's position.
     */
    public final synchronized ByteBuffer2 position(int position) {
        if (position > bytes.length || position < 0) {
            throw new IllegalArgumentException();
        }

        this.position = position;
        if (mark > position) {
            mark = -1;
        }

        return this;
    }

    /**
     * Returns the number of elements between the current position and the
     * limit.
     */
    public final synchronized int remaining() {
        return limit - position;
    }

    /**
     * Tells whether there are any elements between the current position and
     * the limit.
     */
    public final synchronized boolean hasRemaining() {
        return position < limit;
    }

    /**
     * Sets this buffer's limit.
     */
    public final synchronized void limit(int limit) {
        this.limit = limit;
        if (position > limit) {
            position = limit;
        }
        if (mark > limit) {
            mark = -1;
        }
    }

    /**
     * Returns this buffer's limit.
     */
    public int limit() {
        return limit;
    }

    /**
     * Returns this buffer's capacity.
     */
    public int capacity() {
        return bytes.length;
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
    public final synchronized void compact() {
        mark = -1;
        System.arraycopy(bytes, position, bytes, 0, remaining());

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
    public final synchronized void flip() {
        limit = position;
        position = 0;
        mark = -1;
    }

    /**
     * Clears this buffer.  The position is set to zero, the limit is set to
     * the capacity, and the mark is discarded.
     */
    public final synchronized void clear() {
        position = 0;
        limit = capacity();
        mark = -1;
    }

    /**
     * Rewinds this buffer.  The position is set to zero and the mark is
     * discarded.
     */
    public final synchronized void rewind() {
        position = 0;
        mark = -1;
    }

    /**
     * Sets this buffer's mark at its position.
     */
    public final synchronized void mark() {
        mark = position;
    }

    /**
     * Resets this buffer's position to the previously-marked position.
     *
     * <p> Invoking this method neither changes nor discards the mark's
     * value. </p>
     */
    public void reset() {
        position = mark;
    }
}