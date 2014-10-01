package dorkbox.util.objectPool;

import java.util.concurrent.atomic.AtomicInteger;

public class Holder<T> {
    private T value;

    static final int FREE = 0;
    static final int USED = 1;

    AtomicInteger state = new AtomicInteger(FREE);


    public Holder(T value) {
        this.value = value;
    }

    public T getValue() {
        return this.value;
    }
}
