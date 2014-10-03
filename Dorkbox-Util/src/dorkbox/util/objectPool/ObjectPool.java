package dorkbox.util.objectPool;

public interface ObjectPool<T> {
    /**
    * Takes an object from the pool
    */
    public ObjectPoolHolder<T> take();

    /**
    * Return object to the pool
    */
    public void release(ObjectPoolHolder<T> object) throws InterruptedException;
}
