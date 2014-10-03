package dorkbox.util.objectPool;

import java.util.concurrent.atomic.AtomicBoolean;

public class ObjectPoolHolder<T> {
    private T value;

    AtomicBoolean state = new AtomicBoolean(true);


    public ObjectPoolHolder(T value) {
        this.value = value;
    }

    public T getValue() {
        return this.value;
    }
}
