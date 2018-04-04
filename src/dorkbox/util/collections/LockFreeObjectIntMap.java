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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * This class uses the "single-writer-principle" for lock-free publication.
 *
 * Since there are only 2 methods to guarantee that modifications can only be called one-at-a-time (either it is only called by
 * one thread, or only one thread can access it at a time) -- we chose the 2nd option -- and use 'synchronized' to make sure that only
 * one thread can access this modification methods at a time. Getting or checking the presence of values can then happen in a lock-free
 * manner.
 *
 * According to my benchmarks, this is approximately 25% faster than ConcurrentHashMap for (all types of) reads, and a lot slower for
 * contended writes.
 *
 * This data structure is for many-read/few-write scenarios
 */
public
class LockFreeObjectIntMap<V> {
    // Recommended for best performance while adhering to the "single writer principle". Must be static-final
    private static final AtomicReferenceFieldUpdater<LockFreeObjectIntMap, ObjectIntMap> mapREF = AtomicReferenceFieldUpdater.newUpdater(
            LockFreeObjectIntMap.class,
            ObjectIntMap.class,
            "map");

    private volatile ObjectIntMap<V> map;

    private final int defaultReturnValue;

    // synchronized is used here to ensure the "single writer principle", and make sure that ONLY one thread at a time can enter this
    // section. Because of this, we can have unlimited reader threads all going at the same time, without contention (which is our
    // use-case 99% of the time)

    /**
     * Creates a new map using @{link Integer#MIN_VALUE}.
     */
    public
    LockFreeObjectIntMap() {
        this(Integer.MIN_VALUE);
    }

    /**
     * The default return value is used for various get/put operations on the ObjectIntMap.
     *
     * @param defaultReturnValue value used for various get/put operations on the ObjectIntMap.
     */
    public
    LockFreeObjectIntMap(int defaultReturnValue) {
        this(new ObjectIntMap<V>(), defaultReturnValue);
    }

    /**
     * The default return value is used for various get/put operations on the ObjectIntMap.
     *
     * @param defaultReturnValue value used for various get/put operations on the ObjectIntMap.
     */
    LockFreeObjectIntMap(ObjectIntMap<V> forwardHashMap, int defaultReturnValue) {
        this.map = forwardHashMap;
        this.defaultReturnValue = defaultReturnValue;
    }

    /**
     * Removes all of the mappings from this map.
     *
     * The map will be empty after this call returns.
     */
    public synchronized
    void clear() {
        map.clear();
    }

    public synchronized
    int put(final V key, final int value) {
        int prevForwardValue = this.map.get(key, defaultReturnValue);
        this.map.put(key, value);

        return prevForwardValue;
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for
     * any of the keys currently in the specified map.
     *
     * @param hashMap mappings to be stored in this map
     *
     * @throws NullPointerException if the specified map is null
     */
    public synchronized
    void putAll(final Map<V, Integer> hashMap) throws IllegalArgumentException {
        try {
            ObjectIntMap<V> map = this.map;
            for (Map.Entry<V, Integer> entry : hashMap.entrySet()) {
                V key = entry.getKey();
                Integer value = entry.getValue();

                map.put(key, value);
            }
        } catch (IllegalArgumentException e) {
            // do nothing if there is an exception
            throw e;
        }
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param key key whose mapping is to be removed from the map
     *
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>defaultReturnValue</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>defaultReturnValue</tt> return can also indicate that the map
     *         previously associated <tt>defaultReturnValue</tt> with <tt>key</tt>.)
     */
    public synchronized
    int remove(final V key) {
        int value = map.remove(key, defaultReturnValue);
        return value;
    }


    /**
     * Returns the value to which the specified key is mapped,
     * or {@code defaultReturnValue} if this map contains no mapping for the key.
     * <p>
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
     * key.equals(k))}, then this method returns {@code v}; otherwise
     * it returns {@code defaultReturnValue}.  (There can be at most one such mapping.)
     * <p>
     * <p>A return value of {@code defaultReturnValue} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link HashMap#containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @see #put(Object, int)
     */
    @SuppressWarnings("unchecked")
    public
    int get(final V key) {
        // use the SWP to get a lock-free get of the value
        return mapREF.get(this).get(key, defaultReturnValue);
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    public
    boolean isEmpty() {
        // use the SWP to get a lock-free get of the value
        return mapREF.get(this)
                         .size == 0;
    }

    /**
     * Returns the number of key-value mappings in this map.  If the
     * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of key-value mappings in this map
     */
    public
    int size() {
        // use the SWP to get a lock-free get of the value
        return mapREF.get(this)
                         .size;
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
        return mapREF.get(this).hashCode();
    }

    @Override
    public
    String toString() {
        return mapREF.get(this)
                     .toString();
    }
}
