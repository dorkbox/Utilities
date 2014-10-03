package dorkbox.util.objectPool;


public interface PoolableObject<T> {
    /**
    * called when a new instance is created
    */
    public T create();

    /**
    * invoked on every instance that is borrowed from the pool
    */
    public void activate(T t);

    /**
    * invoked on every instance that is returned to the pool
    */
    public void passivate(T t);
}
