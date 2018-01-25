/*
 * Copyright 2018 dorkbox, llc
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

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
public final
class LockFreeBiMap<K, V> {
    // Recommended for best performance while adhering to the "single writer principle". Must be static-final
    private static final AtomicReferenceFieldUpdater<LockFreeBiMap, HashMap> forwardREF =
                    AtomicReferenceFieldUpdater.newUpdater(LockFreeBiMap.class,
                                                           HashMap.class,
                                                           "forwardHashMap");

    private static final AtomicReferenceFieldUpdater<LockFreeBiMap, HashMap> reverseREF =
                    AtomicReferenceFieldUpdater.newUpdater(LockFreeBiMap.class,
                                                           HashMap.class,
                                                           "reverseHashMap");
    
    private volatile HashMap<K, V> forwardHashMap = new HashMap<K, V>();
    private volatile HashMap<V, K> reverseHashMap = new HashMap<V, K>();

    // synchronized is used here to ensure the "single writer principle", and make sure that ONLY one thread at a time can enter this
    // section. Because of this, we can have unlimited reader threads all going at the same time, without contention (which is our
    // use-case 99% of the time)

    public
    LockFreeBiMap() {
    }

    /**
     * Removes all of the mappings from this bimap.
     * The bimap will be empty after this call returns.
     */
    public synchronized
    void clear() {
        forwardHashMap.clear();
        reverseHashMap.clear();
    }

    /**
     * Replaces all of the mappings from the specified map to this bimap.
     * These mappings will replace any mappings that this map had for
     * any of the keys currently in the specified map.
     *
     * @param hashMap mappings to be stored in this map
     *
     * @throws NullPointerException if the specified map is null
     *
     * @throws IllegalArgumentException if a given value in the map is already bound to a different key in this bimap. The bimap will remain
     *         unmodified in this event. To avoid this exception, call {@link #replaceAllForce(Map)} replaceAllForce(map) instead.
     */
    public synchronized
    void replaceAll(final Map<K, V> hashMap) throws IllegalArgumentException {
        if (hashMap == null) {
            throw new NullPointerException("hashMap");
        }

        LockFreeBiMap<K, V> biMap = new LockFreeBiMap<K, V>();

        try {
            biMap.putAll(hashMap);
        } catch (IllegalArgumentException e) {
            // do nothing if there is an exception
            throw e;
        }

        // only if there are no problems with the creation of the new bimap.
        this.forwardHashMap.clear();
        this.reverseHashMap.clear();

        this.forwardHashMap.putAll(biMap.forwardHashMap);
        this.reverseHashMap.putAll(biMap.reverseHashMap);
    }

    /**
     * Replaces all of the mappings from the specified map to this bimap.
     * These mappings will replace any mappings that this map had for
     * any of the keys currently in the specified map. This is an alternate
     * form of {@link #replaceAll(Map)} replaceAll(K, V) that will silently
     * ignore duplicates
     *
     * @param hashMap mappings to be stored in this map
     *
     * @throws NullPointerException if the specified map is null
     */
    public synchronized
    void replaceAllForce(final Map<K, V> hashMap) {
        if (hashMap == null) {
            throw new NullPointerException("hashMap");
        }

        // only if there are no problems with the creation of the new bimap.
        this.forwardHashMap.clear();
        this.reverseHashMap.clear();

        putAllForce(hashMap);
    }

    /**
     * Associates the specified value with the specified key in this bimap.
     * If the bimap previously contained a mapping for the key, the old
     * value is replaced. If the given value is already bound to a different
     * key in this bimap, the bimap will remain unmodified. To avoid throwing
     * an exception, call {@link #putForce(Object, Object)} putForce(K, V) instead.
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
     * unmodified in this event. To avoid this exception, call {@link #putForce(Object, Object)}  putForce(K, V) instead.
     */
    public synchronized
    V put(final K key, final V value) throws IllegalArgumentException {
        V prevForwardValue = this.forwardHashMap.put(key, value);
        if (prevForwardValue != null) {
            reverseHashMap.remove(prevForwardValue);
        }

        K prevReverseValue = this.reverseHashMap.put(value, key);
        if (prevReverseValue != null) {
            // put the old value back
            this.forwardHashMap.remove(key);
            this.reverseHashMap.put(value, prevReverseValue);

            throw new IllegalArgumentException("Value already exists. Keys and values must both be unique!");
        }

        return prevForwardValue;
    }

    /**
     * Associates the specified value with the specified key in this bimap.
     * If the bimap previously contained a mapping for the key, the old
     * value is replaced. This is an alternate form of {@link #put(Object, Object)} put(K, V)
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
    V putForce(final K key, final V value) {
        V prevForwardValue = this.forwardHashMap.put(key, value);
        if (prevForwardValue != null) {
            reverseHashMap.remove(prevForwardValue);
        }

        K prevReverseValue = this.reverseHashMap.put(value, key);
        if (prevReverseValue != null) {
            forwardHashMap.remove(prevReverseValue);
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
     * unmodified in this event. To avoid this exception, call {@link #putAllForce(Map)} putAllForce(K, V) instead.
     */
    public synchronized
    void putAll(final Map<K, V> hashMap) throws IllegalArgumentException {
        LockFreeBiMap<K, V> biMap = new LockFreeBiMap<K, V>();

        try {
            for (Map.Entry<K, V> entry : hashMap.entrySet()) {
                K key = entry.getKey();
                V value = entry.getValue();

                biMap.put(key, value);

                // we have to verify that the keys/values between the bimaps are unique
                if (this.forwardHashMap.containsKey(key)) {
                    throw new IllegalArgumentException("Key already exists. Keys and values must both be unique!");
                }

                if (this.reverseHashMap.containsValue(value)) {
                    throw new IllegalArgumentException("Value already exists. Keys and values must both be unique!");
                }
            }
        } catch (IllegalArgumentException e) {
            // do nothing if there is an exception
            throw e;
        }

        // only if there are no problems with the creation of the new bimap AND the uniqueness constrain is guaranteed
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
    void putAllForce(final Map<K, V> hashMap) {
        for (Map.Entry<K, V> entry : hashMap.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();

            putForce(key, value);
        }
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param key key whose mapping is to be removed from the map
     *
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    public synchronized
    V remove(final K key) {
        V value = forwardHashMap.remove(key);
        reverseHashMap.remove(value);
        return value;
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param key value whose presence in this map is to be tested
     *
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value
     */
    public
    boolean containsValue(final K key) {
        // use the SWP to get a lock-free get of the value
        return forwardREF.get(this).containsValue(key);
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested
     *
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value
     */
    public
    boolean containsReverseValue(final V value) {
        // use the SWP to get a lock-free get of the value
        return reverseREF.get(this).containsValue(value);
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     * <p>
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
     * key.equals(k))}, then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     * <p>
     * <p>A return value of {@code null} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link HashMap#containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @see #put(Object, Object)
     */
    @SuppressWarnings("unchecked")
    public
    V get(final K key) {
        // use the SWP to get a lock-free get of the value
        return (V) forwardREF.get(this).get(key);
    }

    /**
     * Returns the reverse key to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     * <p>
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
     * key.equals(k))}, then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     * <p>
     * <p>A return value of {@code null} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link HashMap#containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @see #put(Object, Object)
     */
    @SuppressWarnings("unchecked")
    public
    K getReverse(final V key) {
        // use the SWP to get a lock-free get of the value
        return (K) reverseREF.get(this).get(key);
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own <tt>remove</tt> operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
     * support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a view of the values contained in this map
     */
    @SuppressWarnings("unchecked")
    public
    Collection<V> values() {
        // use the SWP to get a lock-free get of the value
        return forwardREF.get(this).values();
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
                         .isEmpty();
    }


    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own <tt>remove</tt> operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
     * support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a view of the values contained in this map
     */
    @SuppressWarnings("unchecked")
    public
    Collection<K> reverseValues() {
        // use the SWP to get a lock-free get of the value
        return reverseREF.get(this).values();
    }
}
