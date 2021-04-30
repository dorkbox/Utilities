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
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import dorkbox.util.collections.IntMap.Entries;
import dorkbox.util.collections.IntMap.Keys;

/**
 * A bimap (or "bidirectional map") is a map that preserves the uniqueness of its values as well as that of its keys. This constraint
 * enables bimaps to support an "inverse view", which is another bimap containing the same entries as this bimap but with reversed keys and values.
 *
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
class LockFreeObjectIntBiMap<V> {
    // Recommended for best performance while adhering to the "single writer principle". Must be static-final
    private static final AtomicReferenceFieldUpdater<LockFreeObjectIntBiMap, ObjectIntMap> forwardREF = AtomicReferenceFieldUpdater.newUpdater(
            LockFreeObjectIntBiMap.class,
            ObjectIntMap.class,
            "forwardHashMap");

    private static final AtomicReferenceFieldUpdater<LockFreeObjectIntBiMap, IntMap> reverseREF = AtomicReferenceFieldUpdater.newUpdater(
            LockFreeObjectIntBiMap.class,
            IntMap.class,
            "reverseHashMap");

    private volatile ObjectIntMap<V> forwardHashMap;
    private volatile IntMap<V> reverseHashMap;

    private final int defaultReturnValue;
    private final LockFreeIntBiMap<V> inverse;

    // synchronized is used here to ensure the "single writer principle", and make sure that ONLY one thread at a time can enter this
    // section. Because of this, we can have unlimited reader threads all going at the same time, without contention (which is our
    // use-case 99% of the time)

    /**
     * Creates a new bimap using @{link Integer#MIN_VALUE}.
     */
    public
    LockFreeObjectIntBiMap() {
        this(Integer.MIN_VALUE);
    }

    /**
     * The default return value is used for various get/put operations on the IntMap/ObjectIntMap.
     *
     * @param defaultReturnValue value used for various get/put operations on the IntMap/ObjectIntMap.
     */
    public
    LockFreeObjectIntBiMap(int defaultReturnValue) {
        this(new ObjectIntMap<V>(), new IntMap<V>(), defaultReturnValue);
    }

    /**
     * The default return value is used for various get/put operations on the IntMap/ObjectIntMap.
     *
     * @param defaultReturnValue value used for various get/put operations on the IntMap/ObjectIntMap.
     */
    LockFreeObjectIntBiMap(ObjectIntMap<V> forwardHashMap, IntMap<V> reverseHashMap, int defaultReturnValue) {
        this.forwardHashMap = forwardHashMap;
        this.reverseHashMap = reverseHashMap;
        this.defaultReturnValue = defaultReturnValue;

        this.inverse = new LockFreeIntBiMap<V>(reverseHashMap, forwardHashMap, defaultReturnValue, this);
    }

    LockFreeObjectIntBiMap(final ObjectIntMap<V> forwardHashMap,
                           final IntMap<V> reverseHashMap,
                           final int defaultReturnValue,
                           final LockFreeIntBiMap<V> inverse) {

        this.forwardHashMap = forwardHashMap;
        this.reverseHashMap = reverseHashMap;
        this.defaultReturnValue = defaultReturnValue;
        this.inverse = inverse;
    }

    /**
     * Removes all of the mappings from this bimap.
     *
     * The bimap will be empty after this call returns.
     */
    public synchronized
    void clear() {
        forwardHashMap.clear();
        reverseHashMap.clear();
    }

    /**
     * @return the inverse view of this bimap, which maps each of this bimap's values to its associated key.
     */
    public
    LockFreeIntBiMap<V> inverse() {
        return inverse;
    }

    /**
     * Associates the specified value with the specified key in this bimap.
     * If the bimap previously contained a mapping for the key, the old
     * value is replaced. If the given value is already bound to a different
     * key in this bimap, the bimap will remain unmodified. To avoid throwing
     * an exception, call {@link #putForce(Object, int)} instead.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     *
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     *
     * @throws IllegalArgumentException if the given value is already bound to a different key in this bimap. The bimap will remain
     * unmodified in this event. To avoid this exception, call {@link #putForce(Object, int)} instead.
     */
    public synchronized
    int put(final V key, final int value) throws IllegalArgumentException {
        int prevForwardValue = this.forwardHashMap.get(key, defaultReturnValue);
        this.forwardHashMap.put(key, value);
        if (prevForwardValue != defaultReturnValue) {
            reverseHashMap.remove(prevForwardValue);
        }

        V prevReverseValue = this.reverseHashMap.put(value, key);
        if (prevReverseValue != null) {
            // put the old value back
            if (prevForwardValue != defaultReturnValue) {
                this.forwardHashMap.put(key, prevForwardValue);
            }
            else {
                this.forwardHashMap.remove(key, defaultReturnValue);
            }

            this.reverseHashMap.put(value, prevReverseValue);

            throw new IllegalArgumentException("Value already exists. Keys and values must both be unique!");
        }

        return prevForwardValue;
    }

    /**
     * Associates the specified value with the specified key in this bimap.
     * If the bimap previously contained a mapping for the key, the old
     * value is replaced. This is an alternate form of {@link #put(Object, int)}
     * that will silently ignore duplicates
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     *
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    public synchronized
    int putForce(final V key, final int value) {
        int prevForwardValue = this.forwardHashMap.get(key, defaultReturnValue);
        this.forwardHashMap.put(key, value);
        if (prevForwardValue != defaultReturnValue) {
            reverseHashMap.remove(prevForwardValue);
        }


        V prevReverseValue = this.reverseHashMap.get(value);
        this.reverseHashMap.put(value, key);

        if (prevReverseValue != null) {
            forwardHashMap.remove(prevReverseValue, defaultReturnValue);
        }

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
     *
     * @throws IllegalArgumentException if the given value is already bound to a different key in this bimap. The bimap will remain
     * unmodified in this event. To avoid this exception, call {@link #putAllForce(Map)} instead.
     */
    public synchronized
    void putAll(final Map<V, Integer> hashMap) throws IllegalArgumentException {
        LockFreeObjectIntBiMap<V> biMap = new LockFreeObjectIntBiMap<V>();

        try {
            for (Map.Entry<V, Integer> entry : hashMap.entrySet()) {
                V key = entry.getKey();
                Integer value = entry.getValue();

                biMap.put(key, value);

                // we have to verify that the keys/values between the bimaps are unique
                if (this.forwardHashMap.containsKey(key)) {
                    throw new IllegalArgumentException("Key already exists. Keys and values must both be unique!");
                }

                if (this.reverseHashMap.containsKey(value)) {
                    throw new IllegalArgumentException("Value already exists. Keys and values must both be unique!");
                }
            }
        } catch (IllegalArgumentException e) {
            // do nothing if there is an exception
            throw e;
        }

        // we have checked to make sure that the bimap is unique, AND have checked that we don't already have any of the key/values in ourselves
        this.forwardHashMap.putAll(biMap.forwardHashMap);
        this.reverseHashMap.putAll(biMap.reverseHashMap);
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for
     * any of the keys currently in the specified map. This is an alternate
     * form of {@link #putAll(Map)} putAll(K, V) that will silently
     * ignore duplicates
     *
     * @param hashMap mappings to be stored in this map
     *
     * @throws NullPointerException if the specified map is null
     */
    public synchronized
    void putAllForce(final Map<V, Integer> hashMap) {
        for (Map.Entry<V, Integer> entry : hashMap.entrySet()) {
            V key = entry.getKey();
            Integer value = entry.getValue();

            putForce(key, value);
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
        int value = forwardHashMap.remove(key, defaultReturnValue);
        if (value != defaultReturnValue) {
            reverseHashMap.remove(value);
        }
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
        return forwardREF.get(this).get(key, defaultReturnValue);
    }

    /**
     * Returns an iterator for the keys in the map. Remove is supported. Note that the same iterator instance is returned each
     * time this method is called. Use the {@link Entries} constructor for nested or multithreaded iteration.
     */
    @SuppressWarnings("unchecked")
    public
    Iterator<V> keys() {
        // the ObjectIntMap doesn't have iterators, but the IntMap does
        return inverse.values();
    }

    /**
     * Returns an iterator for the values in the map. Remove is supported. Note that the same iterator instance is returned each
     * time this method is called. Use the {@link Entries} constructor for nested or multithreaded iteration.
     */
    @SuppressWarnings("unchecked")
    public
    Keys values() {
        // the ObjectIntMap doesn't have iterators, but the IntMap does
        return inverse.keys();
    }

    /**
     * Returns <tt>true</tt> if this bimap contains no key-value mappings.
     *
     * @return <tt>true</tt> if this bimap contains no key-value mappings
     */
    public
    boolean isEmpty() {
        // use the SWP to get a lock-free get of the value
        return forwardREF.get(this)
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
        return forwardREF.get(this)
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
        int result = forwardREF.get(this).hashCode();
        result = 31 * result + reverseREF.get(this).hashCode();
        result = 31 * result + defaultReturnValue;
        return result;
    }

    @Override
    public
    String toString() {
        StringBuilder builder = new StringBuilder("LockFreeObjectIntBiMap {");

        Iterator<V> keys = keys();
        Keys values = values();

        while (keys.hasNext()) {
            builder.append(keys.next());
            builder.append(" (")
                   .append(values.next())
                   .append("), ");
        }

        int length = builder.length();
        if (length > 1) {
            // delete the ', '
            builder.delete(length - 2, length);
        }

        builder.append('}');

        return builder.toString();
    }
}
