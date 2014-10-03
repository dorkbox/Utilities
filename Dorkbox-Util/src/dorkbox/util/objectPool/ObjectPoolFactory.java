package dorkbox.util.objectPool;

import dorkbox.util.Sys;

public class ObjectPoolFactory<T> {

    private ObjectPoolFactory() {
    }

    public static <T> ObjectPool<T> create(PoolableObject<T> poolableObject, int size) {
        if (Sys.isAndroid) {
            // unfortunately, unsafe is not available in android
            SlowObjectPool<T> slowObjectPool = new SlowObjectPool<T>(poolableObject, size);
            return slowObjectPool;
        } else {
            // here we use FAST (via UNSAFE) one!
            FastObjectPool<T> fastObjectPool = new FastObjectPool<T>(poolableObject, size);
            return fastObjectPool;
        }
    }
}