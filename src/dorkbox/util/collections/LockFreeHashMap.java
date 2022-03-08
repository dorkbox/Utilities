/*
 * Copyright 2015 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.util.collections;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
public final
class LockFreeHashMap<K, V> implements Map<K, V>, Cloneable, Serializable {
    // Recommended for best performance while adhering to the "single writer principle". Must be static-final
    private static final AtomicReferenceFieldUpdater<LockFreeHashMap, HashMap> mapREF = AtomicReferenceFieldUpdater.newUpdater(
            LockFreeHashMap.class,
            HashMap.class,
            "hashMap");

    private volatile HashMap<K, V> hashMap;

    // synchronized is used here to ensure the "single writer principle", and make sure that ONLY one thread at a time can enter this
    // section. Because of this, we can have unlimited reader threads all going at the same time, without contention (which is our
    // use-case 99% of the time)

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public
    LockFreeHashMap() {
        hashMap = new HashMap<K, V>();
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
    LockFreeHashMap(int initialCapacity) {
        hashMap = new HashMap<K, V>(initialCapacity);
    }

    /**
     * Constructs a new <tt>HashMap</tt> with the same mappings as the
     * specified <tt>Map</tt>.  The <tt>HashMap</tt> is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified <tt>Map</tt>.
     *
     * @param map the map whose mappings are to be placed in this map
     *
     * @throws NullPointerException if the specified map is null
     */
    public
    LockFreeHashMap(Map<K, V> map) {
        this.hashMap = new HashMap<K, V>(map);
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
    LockFreeHashMap(int initialCapacity, float loadFactor) {
        this.hashMap = new HashMap<K, V>(initialCapacity, loadFactor);
    }

    @SuppressWarnings("unchecked")
    public
    Map<K, V> getMap() {
        // use the SWP to get a lock-free get of the map. It's values are only valid at the moment this method is called.
        return Collections.unmodifiableMap(mapREF.get(this));
    }

    @Override
    public
    int size() {
        // use the SWP to get a lock-free get of the value
        return mapREF.get(this)
                     .size();
    }

    @Override
    public
    boolean isEmpty() {
        // use the SWP to get a lock-free get of the value
        return mapREF.get(this)
                     .isEmpty();
    }

    @Override
    public
    boolean containsKey(final Object key) {
        // use the SWP to get a lock-free get of the value
        return mapREF.get(this)
                     .containsKey(key);
    }

    @Override
    public
    boolean containsValue(final Object value) {
        // use the SWP to get a lock-free get of the value
        return mapREF.get(this)
                     .containsValue(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public
    V get(final Object key) {
        // use the SWP to get a lock-free get of the value
        return (V) mapREF.get(this)
                         .get(key);
    }

    @Override
    public synchronized
    V put(final K key, final V value) {
        return hashMap.put(key, value);
    }

    @Override
    public synchronized
    V remove(final Object key) {
        return hashMap.remove(key);
    }

    @SuppressWarnings("Java8CollectionRemoveIf")
    public synchronized
    void removeAllValues(final V value) {
        for (Iterator<Entry<K, V>> iterator = hashMap.entrySet().iterator(); iterator.hasNext(); ) {
            final Map.Entry<K, V> kvEntry = iterator.next();
            if (kvEntry.getValue().equals(value)) {
                iterator.remove();
            }
        }
    }

    @Override
    public synchronized
    void putAll(final Map<? extends K, ? extends V> map) {
        this.hashMap.putAll(map);
    }

    /**
     * This uses equals to update values. At first glance, this seems like a waste (since if it's equal, why update it?). This is because
     * the ONLY location this is used (in the Database, for updating all DeviceUser in the map), equals compares ONLY the DB ID. In only
     * this situation, this makes sense (since anything with the same DB ID, we should replace/update the value)
     */
    public synchronized
    void updateAllWithValue(final V value) {
        for (Map.Entry<K, V> entry : hashMap.entrySet()) {
            if (value.equals(entry.getValue())) {
                // get's all device IDs that have this user assigned, and reassign the value
                entry.setValue(value);
            }
        }
    }

    public synchronized
    void replaceAll(Map<K,V> hashMap) {
        this.hashMap.clear();
        this.hashMap.putAll(hashMap);
    }

    @Override
    public synchronized
    void clear() {
        hashMap.clear();
    }

    @Override
    public
    Set<K> keySet() {
        return getMap().keySet();
    }

    @Override
    public
    Collection<V> values() {
        return getMap().values();
    }

    @Override
    public
    Set<Entry<K, V>> entrySet() {
        return getMap().entrySet();
    }

    @Override
    public
    boolean equals(final Object o) {
        return mapREF.get(this)
                     .equals(o);
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

    @SuppressWarnings("unchecked")
    public
    Collection<K> keys() {
        // use the SWP to get a lock-free get of the value
        return mapREF.get(this).keySet();
    }

    @SuppressWarnings("unchecked")
    public
    Map<K,V> elements() {
        // use the SWP to get a lock-free get of the value
        return mapREF.get(this);
    }

    @SuppressWarnings("unchecked")
    public
    HashMap<K, V> backingMap() {
        // use the SWP to get a lock-free get of the value
        return mapREF.get(this);
    }
}
