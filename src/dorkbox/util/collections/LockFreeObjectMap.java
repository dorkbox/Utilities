/*
 * Copyright 2018 dorkbox, llc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package dorkbox.util.collections;

import static dorkbox.util.collections.ObjectMap.*;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * This class uses the "single-writer-principle" for lock-free publication.
 * <p>
 * Since there are only 2 methods to guarantee that modifications can only be called one-at-a-time (either it is only called by
 * one thread, or only one thread can access it at a time) -- we chose the 2nd option -- and use 'synchronized' to make sure that only
 * one thread can access this modification methods at a time. Getting or checking the presence of values can then happen in a lock-free
 * manner.
 * <p>
 * According to my benchmarks, this is approximately 25% faster than ConcurrentHashMap for (all types of) reads, and a lot slower for
 * contended writes.
 * <p>
 * This data structure is for many-read/few-write scenarios
 */
@SuppressWarnings("unchecked")
public final
class LockFreeObjectMap<K, V> implements Cloneable, Serializable {
    // Recommended for best performance while adhering to the "single writer principle". Must be static-final
    private static final AtomicReferenceFieldUpdater<LockFreeObjectMap, ObjectMap> mapREF = AtomicReferenceFieldUpdater.newUpdater(
            LockFreeObjectMap.class,
            ObjectMap.class,
            "hashMap");

    private volatile ObjectMap<K, V> hashMap;

    // synchronized is used here to ensure the "single writer principle", and make sure that ONLY one thread at a time can enter this
    // section. Because of this, we can have unlimited reader threads all going at the same time, without contention (which is our
    // use-case 99% of the time)

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public
    LockFreeObjectMap() {
        hashMap = new ObjectMap<K, V>();
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param initialCapacity the initial capacity.
     *
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public
    LockFreeObjectMap(int initialCapacity) {
        hashMap = new ObjectMap<K, V>(initialCapacity);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param initialCapacity the initial capacity
     * @param loadFactor the load factor
     *
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    public
    LockFreeObjectMap(int initialCapacity, float loadFactor) {
        this.hashMap = new ObjectMap<K, V>(initialCapacity, loadFactor);
    }

    public
    int size() {
        // use the SWP to get a lock-free get of the value
        return mapREF.get(this)
                     .size;
    }

    public
    boolean isEmpty() {
        // use the SWP to get a lock-free get of the value
        return mapREF.get(this)
                     .size == 0;
    }

    public
    boolean containsKey(final K key) {
        // use the SWP to get a lock-free get of the value
        return mapREF.get(this)
                     .containsKey(key);
    }

    public
    boolean containsValue(final V value, boolean identity) {
        // use the SWP to get a lock-free get of the value
        return mapREF.get(this)
                     .containsValue(value, identity);
    }

    @SuppressWarnings("unchecked")
    public
    V get(final K key) {
        // use the SWP to get a lock-free get of the value
        return (V) mapREF.get(this)
                         .get(key);
    }

    public synchronized
    V put(final K key, final V value) {
        return hashMap.put(key, value);
    }

    public synchronized
    V remove(final K key) {
        return hashMap.remove(key);
    }

    public synchronized
    void putAll(final ObjectMap<K, V> map) {
        this.hashMap.putAll(map);
    }

    public synchronized
    void clear() {
        hashMap.clear();
    }

    /**
     * DO NOT MODIFY THE MAP VIA THIS (unless you synchronize around it!) It will result in unknown object visibility!
     *
     * Returns an iterator for the keys in the map. Remove is supported. Note that the same iterator instance is returned each
     * time this method is called. Use the {@link Entries} constructor for nested or multithreaded iteration.
     */
    public
    Keys keys() {
        return mapREF.get(this)
                         .keys();
    }

    /**
     * DO NOT MODIFY THE MAP VIA THIS (unless you synchronize around it!) It will result in unknown object visibility!
     *
     * Returns an iterator for the values in the map. Remove is supported. Note that the same iterator instance is returned each
     * time this method is called. Use the {@link Entries} constructor for nested or multithreaded iteration.
     */
    public
    Values values() {
        return mapREF.get(this)
                         .values();
    }

    /**
     * DO NOT MODIFY THE MAP VIA THIS (unless you synchronize around it!) It will result in unknown object visibility!
     *
     * Returns an iterator for the entries in the map. Remove is supported. Note that the same iterator instance is returned each
     * time this method is called. Use the {@link Entries} constructor for nested or multithreaded iteration.
     */
    public
    Entries entries() {
        return mapREF.get(this)
                         .entries();
    }

    /**
     * Identity equals only!
     */
    @Override
    public
    boolean equals(final Object o) {
        return this == o;
    }

    @Override
    public
    int hashCode() {
        return mapREF.get(this)
                     .hashCode();
    }

    @Override
    public
    String toString() {
        return mapREF.get(this)
                     .toString();
    }
}
