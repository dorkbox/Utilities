package dorkbox.util.bytes;

import dorkbox.util.objectPool.PoolableObject;

public class ByteBuffer2Poolable implements PoolableObject<ByteBuffer2> {
    @Override
    public ByteBuffer2 create() {
        return new ByteBuffer2(8, -1);
    }

    @Override
    public void activate(ByteBuffer2 object) {
        object.clear();
    }

    @Override
    public void passivate(ByteBuffer2 object) {
    }
}

