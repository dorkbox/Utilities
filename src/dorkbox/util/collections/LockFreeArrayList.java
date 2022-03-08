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

import java.util.ArrayList;
import java.util.Collection;
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
public final
class LockFreeArrayList<E> {
    // Recommended for best performance while adhering to the "single writer principle". Must be static-final
    private static final AtomicReferenceFieldUpdater<LockFreeArrayList, ArrayList> listRef =
            AtomicReferenceFieldUpdater.newUpdater(LockFreeArrayList.class,
                                                   ArrayList.class, "arrayList");
    private volatile ArrayList<E> arrayList = new ArrayList<>();


    public
    LockFreeArrayList(){}


    public
    LockFreeArrayList(Collection<E> elements) {
        arrayList.addAll(elements);
    }

    // synchronized is used here to ensure the "single writer principle", and make sure that ONLY one thread at a time can enter this
    // section. Because of this, we can have unlimited reader threads all going at the same time, without contention (which is our
    // use-case 99% of the time)
    public synchronized
    void clear() {
        arrayList.clear();
    }

    // synchronized is used here to ensure the "single writer principle", and make sure that ONLY one thread at a time can enter this
    // section. Because of this, we can have unlimited reader threads all going at the same time, without contention (which is our
    // use-case 99% of the time)
    public synchronized
    boolean add(final E element) {
        return arrayList.add(element);
    }

    // synchronized is used here to ensure the "single writer principle", and make sure that ONLY one thread at a time can enter this
    // section. Because of this, we can have unlimited reader threads all going at the same time, without contention (which is our
    // use-case 99% of the time)
    public synchronized
    void addAll(final Collection<E> elements) {
        arrayList.addAll(elements);
    }

    public synchronized
    E get(int index) {
        return arrayList.get(index);
    }

    // synchronized is used here to ensure the "single writer principle", and make sure that ONLY one thread at a time can enter this
    // section. Because of this, we can have unlimited reader threads all going at the same time, without contention (which is our
    // use-case 99% of the time)
    public synchronized
    boolean remove(final E element) {
        return arrayList.remove(element);
    }

    public
    int size() {
        return listRef.get(this).size();
    }

    // lock-free get
    public
    boolean contains(final E element) {
        // use the SWP to get the value
        return listRef.get(this).contains(element);
    }

    // lock-free get
    @SuppressWarnings("unchecked")
    public
    ArrayList<E> elements() {
        // use the SWP to get the value
        return listRef.get(this);
    }
}
