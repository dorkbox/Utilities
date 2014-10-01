package dorkbox.util.objectPool;

public interface PoolFactory<T> {
    public T create();
}