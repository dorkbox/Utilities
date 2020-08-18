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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
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
@SuppressWarnings("unchecked")
public final
class LockFreeSet<E> implements Set<E>, Cloneable, java.io.Serializable {
    // Recommended for best performance while adhering to the "single writer principle". Must be static-final
    private static final AtomicReferenceFieldUpdater<LockFreeSet, Set> setREF = AtomicReferenceFieldUpdater.newUpdater(LockFreeSet.class,
                                                                                                                       Set.class,
                                                                                                                       "hashSet");

    private volatile Set<E> hashSet;

    // synchronized is used here to ensure the "single writer principle", and make sure that ONLY one thread at a time can enter this
    // section. Because of this, we can have unlimited reader threads all going at the same time, without contention (which is our
    // use-case 99% of the time)

    /**
     * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has
     * default initial capacity (16) and load factor (0.75).
     */
    public
    LockFreeSet() {
        hashSet = new HashSet<E>();
    }

    /**
     * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has
     * the specified initial capacity and the specified load factor.
     *
     * @param initialCapacity the initial capacity of the hash map
     * @param loadFactor the load factor of the hash map
     *
     * @throws IllegalArgumentException if the initial capacity is less
     *         than zero, or if the load factor is nonpositive
     */
    public
    LockFreeSet(int initialCapacity, float loadFactor) {
        hashSet = new HashSet<E>(initialCapacity, loadFactor);
    }

    /**
     * Constructs a new, empty set; the backing <tt>HashMap</tt> instance has
     * the specified initial capacity and default load factor (0.75).
     *
     * @param initialCapacity the initial capacity of the hash table
     *
     * @throws IllegalArgumentException if the initial capacity is less
     *         than zero
     */
    public
    LockFreeSet(int initialCapacity) {
        hashSet = new HashSet<E>(initialCapacity);
    }

    /**
     * Constructs a new set containing the elements in the specified
     * collection.  The <tt>HashMap</tt> is created with default load factor
     * (0.75) and an initial capacity sufficient to contain the elements in
     * the specified collection.
     *
     * @param collection the collection whose elements are to be placed into this set
     *
     * @throws NullPointerException if the specified collection is null
     */
    public
    LockFreeSet(final Collection<E> collection) {
        hashSet = new HashSet<E>(collection);
    }


    @SuppressWarnings("unchecked")
    public
    Set<E> elements() {
        // use the SWP to get a lock-free get of the value
        return Collections.unmodifiableSet(setREF.get(this));
    }


    @Override
    public
    int size() {
        return setREF.get(this)
                     .size();
    }

    @Override
    public
    boolean isEmpty() {
        // use the SWP to get a lock-free get of the value
        return setREF.get(this)
                     .isEmpty();
    }

    @Override
    public
    boolean contains(final Object element) {
        // use the SWP to get a lock-free get of the value
        return setREF.get(this)
                     .contains(element);
    }

    @Override
    public
    Iterator<E> iterator() {
        return elements().iterator();
    }

    @Override
    public
    Object[] toArray() {
        return setREF.get(this)
                     .toArray();
    }

    @Override
    public
    <T> T[] toArray(final T[] a) {
        return (T[]) setREF.get(this)
                           .toArray(a);
    }

    @Override
    public synchronized
    boolean add(final E element) {
        return hashSet.add(element);
    }

    @Override
    public synchronized
    boolean remove(final Object element) {
        return hashSet.remove(element);
    }

    @Override
    public
    boolean containsAll(final Collection<?> collection) {
        // use the SWP to get a lock-free get of the value
        return setREF.get(this)
                     .containsAll(collection);
    }

    @Override
    public synchronized
    boolean addAll(final Collection<? extends E> elements) {
        return hashSet.addAll(elements);
    }

    @Override
    public synchronized
    boolean retainAll(final Collection<?> collection) {
        return hashSet.retainAll(collection);
    }

    @Override
    public synchronized
    boolean removeAll(final Collection<?> collection) {
        return hashSet.removeAll(collection);
    }

    @Override
    public synchronized
    void clear() {
        hashSet.clear();
    }

    @Override
    public
    boolean equals(final Object o) {
        return setREF.get(this).equals(o);
    }

    @Override
    public
    int hashCode() {
        return setREF.get(this)
                     .hashCode();
    }

    @Override
    public
    String toString() {
        return setREF.get(this)
                     .toString();
    }
}
