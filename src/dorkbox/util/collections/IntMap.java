/*
 * Copyright 2010 Mario Zechner (contact@badlogicgames.com), Nathan Sweet (admin@esotericsoftware.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// slightly tweaked from libGDX, by dorkbox, llc

package dorkbox.util.collections;


import java.util.Iterator;
import java.util.NoSuchElementException;

import dorkbox.util.MathUtil;

/** An unordered map that uses int keys. This implementation is a cuckoo hash map using 3 hashes, random walking, and a small stash
 * for problematic keys. Null values are allowed. No allocation is done except when growing the table size. <br>
 * <br>
 * This map performs very fast get, containsKey, and remove (typically O(1), worst case O(log(n))). Put may be a bit slower,
 * depending on hash collisions. Load factors greater than 0.91 greatly increase the chances the map will have to rehash to the
 * next higher POT size.
 * @author Nathan Sweet */

@SuppressWarnings({"rawtypes", "unchecked"})
public class IntMap<V> {
    @SuppressWarnings("unused")
    private static final int PRIME1 = 0xbe1f14b1;
    private static final int PRIME2 = 0xb4b82e39;
    private static final int PRIME3 = 0xced1c241;
    private static final int EMPTY = 0;

    public int size;

    int[] keyTable;
    V[] valueTable;
    int capacity, stashSize;
    V zeroValue;
    boolean hasZeroValue;

    private float loadFactor;
    private int hashShift, mask, threshold;
    private int stashCapacity;
    private int pushIterations;

    private Entries entries1, entries2;
    private Values values1, values2;
    private Keys keys1, keys2;

    /** Creates a new map with an initial capacity of 32 and a load factor of 0.8. This map will hold 25 items before growing the
    * backing table. */
    public IntMap () {
        this(32, 0.8f);
    }

    /** Creates a new map with a load factor of 0.8. This map will hold initialCapacity * 0.8 items before growing the backing
    * table. */
    public IntMap (int initialCapacity) {
        this(initialCapacity, 0.8f);
    }

    /** Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity * loadFactor items
    * before growing the backing table. */
    public IntMap (int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be >= 0: " + initialCapacity);
        }
        if (this.capacity > 1 << 30) {
            throw new IllegalArgumentException("initialCapacity is too large: " + initialCapacity);
        }
        this.capacity = MathUtil.nextPowerOfTwo(initialCapacity);

        if (loadFactor <= 0) {
            throw new IllegalArgumentException("loadFactor must be > 0: " + loadFactor);
        }
        this.loadFactor = loadFactor;

        this.threshold = (int)(this.capacity * loadFactor);
        this.mask = this.capacity - 1;
        this.hashShift = 31 - Integer.numberOfTrailingZeros(this.capacity);
        this.stashCapacity = Math.max(3, (int)Math.ceil(Math.log(this.capacity)) * 2);
        this.pushIterations = Math.max(Math.min(this.capacity, 8), (int)Math.sqrt(this.capacity) / 8);

        this.keyTable = new int[this.capacity + this.stashCapacity];
        this.valueTable = (V[])new Object[this.keyTable.length];
    }

    public V put (int key, V value) {
        if (key == 0) {
            V oldValue = this.zeroValue;
            this.zeroValue = value;
            if (!this.hasZeroValue) {
                this.hasZeroValue = true;
                this.size++;
            }
            return oldValue;
        }

        int[] keyTable = this.keyTable;

        // Check for existing keys.
        int index1 = key & this.mask;
        int key1 = keyTable[index1];
        if (key1 == key) {
            V oldValue = this.valueTable[index1];
            this.valueTable[index1] = value;
            return oldValue;
        }

        int index2 = hash2(key);
        int key2 = keyTable[index2];
        if (key2 == key) {
            V oldValue = this.valueTable[index2];
            this.valueTable[index2] = value;
            return oldValue;
        }

        int index3 = hash3(key);
        int key3 = keyTable[index3];
        if (key3 == key) {
            V oldValue = this.valueTable[index3];
            this.valueTable[index3] = value;
            return oldValue;
        }

        // Update key in the stash.
        for (int i = this.capacity, n = i + this.stashSize; i < n; i++) {
            if (keyTable[i] == key) {
                V oldValue = this.valueTable[i];
                this.valueTable[i] = value;
                return oldValue;
            }
        }

        // Check for empty buckets.
        if (key1 == EMPTY) {
            keyTable[index1] = key;
            this.valueTable[index1] = value;
            if (this.size++ >= this.threshold) {
                resize(this.capacity << 1);
            }
            return null;
        }

        if (key2 == EMPTY) {
            keyTable[index2] = key;
            this.valueTable[index2] = value;
            if (this.size++ >= this.threshold) {
                resize(this.capacity << 1);
            }
            return null;
        }

        if (key3 == EMPTY) {
            keyTable[index3] = key;
            this.valueTable[index3] = value;
            if (this.size++ >= this.threshold) {
                resize(this.capacity << 1);
            }
            return null;
        }

        push(key, value, index1, key1, index2, key2, index3, key3);
        return null;
    }

    public void putAll (IntMap<V> map) {
        for (Entry<V> entry : map.entries()) {
            put(entry.key, entry.value);
        }
    }

    /** Skips checks for existing keys. */
    private void putResize (int key, V value) {
        if (key == 0) {
            this.zeroValue = value;
            this.hasZeroValue = true;
            return;
        }

        // Check for empty buckets.
        int index1 = key & this.mask;
        int key1 = this.keyTable[index1];
        if (key1 == EMPTY) {
            this.keyTable[index1] = key;
            this.valueTable[index1] = value;
            if (this.size++ >= this.threshold) {
                resize(this.capacity << 1);
            }
            return;
        }

        int index2 = hash2(key);
        int key2 = this.keyTable[index2];
        if (key2 == EMPTY) {
            this.keyTable[index2] = key;
            this.valueTable[index2] = value;
            if (this.size++ >= this.threshold) {
                resize(this.capacity << 1);
            }
            return;
        }

        int index3 = hash3(key);
        int key3 = this.keyTable[index3];
        if (key3 == EMPTY) {
            this.keyTable[index3] = key;
            this.valueTable[index3] = value;
            if (this.size++ >= this.threshold) {
                resize(this.capacity << 1);
            }
            return;
        }

        push(key, value, index1, key1, index2, key2, index3, key3);
    }

    private void push (int insertKey, V insertValue, int index1, int key1, int index2, int key2, int index3, int key3) {
        int[] keyTable = this.keyTable;

        V[] valueTable = this.valueTable;
        int mask = this.mask;

        // Push keys until an empty bucket is found.
        int evictedKey;
        V evictedValue;
        int i = 0, pushIterations = this.pushIterations;
        do {
            // Replace the key and value for one of the hashes.
            switch (MathUtil.randomInt(2)) {
            case 0:
                evictedKey = key1;
                evictedValue = valueTable[index1];
                keyTable[index1] = insertKey;
                valueTable[index1] = insertValue;
                break;
            case 1:
                evictedKey = key2;
                evictedValue = valueTable[index2];
                keyTable[index2] = insertKey;
                valueTable[index2] = insertValue;
                break;
            default:
                evictedKey = key3;
                evictedValue = valueTable[index3];
                keyTable[index3] = insertKey;
                valueTable[index3] = insertValue;
                break;
            }

            // If the evicted key hashes to an empty bucket, put it there and stop.
            index1 = evictedKey & mask;
            key1 = keyTable[index1];
            if (key1 == EMPTY) {
                keyTable[index1] = evictedKey;
                valueTable[index1] = evictedValue;
                if (this.size++ >= this.threshold) {
                    resize(this.capacity << 1);
                }
                return;
            }

            index2 = hash2(evictedKey);
            key2 = keyTable[index2];
            if (key2 == EMPTY) {
                keyTable[index2] = evictedKey;
                valueTable[index2] = evictedValue;
                if (this.size++ >= this.threshold) {
                    resize(this.capacity << 1);
                }
                return;
            }

            index3 = hash3(evictedKey);
            key3 = keyTable[index3];
            if (key3 == EMPTY) {
                keyTable[index3] = evictedKey;
                valueTable[index3] = evictedValue;
                if (this.size++ >= this.threshold) {
                    resize(this.capacity << 1);
                }
                return;
            }

            if (++i == pushIterations) {
                break;
            }

            insertKey = evictedKey;
            insertValue = evictedValue;
        } while (true);

        putStash(evictedKey, evictedValue);
    }

    private void putStash (int key, V value) {
        if (this.stashSize == this.stashCapacity) {
            // Too many pushes occurred and the stash is full, increase the table size.
            resize(this.capacity << 1);
            put(key, value);
            return;
        }
        // Store key in the stash.
        int index = this.capacity + this.stashSize;
        this.keyTable[index] = key;
        this.valueTable[index] = value;
        this.stashSize++;
        this.size++;
    }

    public V get (int key) {
        if (key == 0) {
            if (!this.hasZeroValue) {
                return null;
            }
            return this.zeroValue;
        }
        int index = key & this.mask;
        if (this.keyTable[index] != key) {
            index = hash2(key);
            if (this.keyTable[index] != key) {
                index = hash3(key);
                if (this.keyTable[index] != key) {
                    return getStash(key, null);
                }
            }
        }
        return this.valueTable[index];
    }

    public V get (int key, V defaultValue) {
        if (key == 0) {
            if (!this.hasZeroValue) {
                return defaultValue;
            }
            return this.zeroValue;
        }
        int index = key & this.mask;
        if (this.keyTable[index] != key) {
            index = hash2(key);
            if (this.keyTable[index] != key) {
                index = hash3(key);
                if (this.keyTable[index] != key) {
                    return getStash(key, defaultValue);
                }
            }
        }
        return this.valueTable[index];
    }

    private V getStash (int key, V defaultValue) {
        int[] keyTable = this.keyTable;
        for (int i = this.capacity, n = i + this.stashSize; i < n; i++) {
            if (keyTable[i] == key) {
                return this.valueTable[i];
            }
        }
        return defaultValue;
    }

    public V remove (int key) {
        if (key == 0) {
            if (!this.hasZeroValue) {
                return null;
            }
            V oldValue = this.zeroValue;
            this.zeroValue = null;
            this.hasZeroValue = false;
            this.size--;
            return oldValue;
        }

        int index = key & this.mask;
        if (this.keyTable[index] == key) {
            this.keyTable[index] = EMPTY;
            V oldValue = this.valueTable[index];
            this.valueTable[index] = null;
            this.size--;
            return oldValue;
        }

        index = hash2(key);
        if (this.keyTable[index] == key) {
            this.keyTable[index] = EMPTY;
            V oldValue = this.valueTable[index];
            this.valueTable[index] = null;
            this.size--;
            return oldValue;
        }

        index = hash3(key);
        if (this.keyTable[index] == key) {
            this.keyTable[index] = EMPTY;
            V oldValue = this.valueTable[index];
            this.valueTable[index] = null;
            this.size--;
            return oldValue;
        }

        return removeStash(key);
    }

    V removeStash (int key) {
        int[] keyTable = this.keyTable;
        for (int i = this.capacity, n = i + this.stashSize; i < n; i++) {
            if (keyTable[i] == key) {
                V oldValue = this.valueTable[i];
                removeStashIndex(i);
                this.size--;
                return oldValue;
            }
        }
        return null;
    }

    void removeStashIndex (int index) {
        // If the removed location was not last, move the last tuple to the removed location.
        this.stashSize--;
        int lastIndex = this.capacity + this.stashSize;
        if (index < lastIndex) {
            this.keyTable[index] = this.keyTable[lastIndex];
            this.valueTable[index] = this.valueTable[lastIndex];
            this.valueTable[lastIndex] = null;
        } else {
            this.valueTable[index] = null;
        }
    }

    public void clear () {
        int[] keyTable = this.keyTable;
        V[] valueTable = this.valueTable;
        for (int i = this.capacity + this.stashSize; i-- > 0;) {
            keyTable[i] = EMPTY;
            valueTable[i] = null;
        }
        this.size = 0;
        this.stashSize = 0;
        this.zeroValue = null;
        this.hasZeroValue = false;
    }

    /** Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may be
    * an expensive operation.
    * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
    *           {@link #equals(Object)}. */
    public boolean containsValue (Object value, boolean identity) {
        V[] valueTable = this.valueTable;
        if (value == null) {
            if (this.hasZeroValue && this.zeroValue == null) {
                return true;
            }
            int[] keyTable = this.keyTable;
            for (int i = this.capacity + this.stashSize; i-- > 0;) {
                if (keyTable[i] != EMPTY && valueTable[i] == null) {
                    return true;
                }
            }
        } else if (identity) {
            if (value == this.zeroValue) {
                return true;
            }
            for (int i = this.capacity + this.stashSize; i-- > 0;) {
                if (valueTable[i] == value) {
                    return true;
                }
            }
        } else {
            if (this.hasZeroValue && value.equals(this.zeroValue)) {
                return true;
            }
            for (int i = this.capacity + this.stashSize; i-- > 0;) {
                if (value.equals(valueTable[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean containsKey (int key) {
        if (key == 0) {
            return this.hasZeroValue;
        }
        int index = key & this.mask;
        if (this.keyTable[index] != key) {
            index = hash2(key);
            if (this.keyTable[index] != key) {
                index = hash3(key);
                if (this.keyTable[index] != key) {
                    return containsKeyStash(key);
                }
            }
        }
        return true;
    }

    private boolean containsKeyStash (int key) {
        int[] keyTable = this.keyTable;
        for (int i = this.capacity, n = i + this.stashSize; i < n; i++) {
            if (keyTable[i] == key) {
                return true;
            }
        }
        return false;
    }

    /** Returns the key for the specified value, or <tt>notFound</tt> if it is not in the map. Note this traverses the entire map
    * and compares every value, which may be an expensive operation.
    * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
    *           {@link #equals(Object)}. */
    public int findKey (Object value, boolean identity, int notFound) {
        V[] valueTable = this.valueTable;
        if (value == null) {
            if (this.hasZeroValue && this.zeroValue == null) {
                return 0;
            }
            int[] keyTable = this.keyTable;
            for (int i = this.capacity + this.stashSize; i-- > 0;) {
                if (keyTable[i] != EMPTY && valueTable[i] == null) {
                    return keyTable[i];
                }
            }
        } else if (identity) {
            if (value == this.zeroValue) {
                return 0;
            }
            for (int i = this.capacity + this.stashSize; i-- > 0;) {
                if (valueTable[i] == value) {
                    return this.keyTable[i];
                }
            }
        } else {
            if (this.hasZeroValue && value.equals(this.zeroValue)) {
                return 0;
            }
            for (int i = this.capacity + this.stashSize; i-- > 0;) {
                if (value.equals(valueTable[i])) {
                    return this.keyTable[i];
                }
            }
        }
        return notFound;
    }

    /** Increases the size of the backing array to acommodate the specified number of additional items. Useful before adding many
    * items to avoid multiple backing array resizes. */
    public void ensureCapacity (int additionalCapacity) {
        int sizeNeeded = this.size + additionalCapacity;
        if (sizeNeeded >= this.threshold) {
            resize(MathUtil.nextPowerOfTwo((int) (sizeNeeded / this.loadFactor)));
        }
    }

    private void resize (int newSize) {
        int oldEndIndex = this.capacity + this.stashSize;

        this.capacity = newSize;
        this.threshold = (int)(newSize * this.loadFactor);
        this.mask = newSize - 1;
        this.hashShift = 31 - Integer.numberOfTrailingZeros(newSize);
        this.stashCapacity = Math.max(3, (int)Math.ceil(Math.log(newSize)) * 2);
        this.pushIterations = Math.max(Math.min(newSize, 8), (int)Math.sqrt(newSize) / 8);

        int[] oldKeyTable = this.keyTable;
        V[] oldValueTable = this.valueTable;

        this.keyTable = new int[newSize + this.stashCapacity];
        this.valueTable = (V[])new Object[newSize + this.stashCapacity];

        this.size = this.hasZeroValue ? 1 : 0;
        this.stashSize = 0;
        for (int i = 0; i < oldEndIndex; i++) {
            int key = oldKeyTable[i];
            if (key != EMPTY) {
                putResize(key, oldValueTable[i]);
            }
        }
    }

    private int hash2 (int h) {
        h *= PRIME2;
        return (h ^ h >>> this.hashShift) & this.mask;
    }

    private int hash3 (int h) {
        h *= PRIME3;
        return (h ^ h >>> this.hashShift) & this.mask;
    }

    @Override
    public String toString () {
        if (this.size == 0) {
            return "[]";
        }
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('[');
        int[] keyTable = this.keyTable;
        V[] valueTable = this.valueTable;
        int i = keyTable.length;
        if (this.hasZeroValue) {
            buffer.append("0=");
            buffer.append(this.zeroValue);
        } else {
            while (i-- > 0) {
                int key = keyTable[i];
                if (key == EMPTY) {
                    continue;
                }
                buffer.append(key);
                buffer.append('=');
                buffer.append(valueTable[i]);
                break;
            }
        }
        while (i-- > 0) {
            int key = keyTable[i];
            if (key == EMPTY) {
                continue;
            }
            buffer.append(", ");
            buffer.append(key);
            buffer.append('=');
            buffer.append(valueTable[i]);
        }
        buffer.append(']');
        return buffer.toString();
    }

    /** Returns an iterator for the entries in the map. Remove is supported. Note that the same iterator instance is returned each
    * time this method is called. Use the {@link Entries} constructor for nested or multithreaded iteration. */
    public Entries<V> entries () {
        if (this.entries1 == null) {
            this.entries1 = new Entries(this);
            this.entries2 = new Entries(this);
        }
        if (!this.entries1.valid) {
            this.entries1.reset();
            this.entries1.valid = true;
            this.entries2.valid = false;
            return this.entries1;
        }
        this.entries2.reset();
        this.entries2.valid = true;
        this.entries1.valid = false;
        return this.entries2;
    }

    /** Returns an iterator for the values in the map. Remove is supported. Note that the same iterator instance is returned each
    * time this method is called. Use the {@link Entries} constructor for nested or multithreaded iteration. */
    public Values<V> values () {
        if (this.values1 == null) {
            this.values1 = new Values(this);
            this.values2 = new Values(this);
        }
        if (!this.values1.valid) {
            this.values1.reset();
            this.values1.valid = true;
            this.values2.valid = false;
            return this.values1;
        }
        this.values2.reset();
        this.values2.valid = true;
        this.values1.valid = false;
        return this.values2;
    }

    /** Returns an iterator for the keys in the map. Remove is supported. Note that the same iterator instance is returned each time
    * this method is called. Use the {@link Entries} constructor for nested or multithreaded iteration. */
    public Keys keys () {
        if (this.keys1 == null) {
            this.keys1 = new Keys(this);
            this.keys2 = new Keys(this);
        }
        if (!this.keys1.valid) {
            this.keys1.reset();
            this.keys1.valid = true;
            this.keys2.valid = false;
            return this.keys1;
        }
        this.keys2.reset();
        this.keys2.valid = true;
        this.keys1.valid = false;
        return this.keys2;
    }

    static public class Entry<V> {
        public int key;
        public V value;

        @Override
        public String toString () {
            return this.key + "=" + this.value;
        }
    }

    static private class MapIterator<V> {
        static final int INDEX_ILLEGAL = -2;
        static final int INDEX_ZERO = -1;

        public boolean hasNext;

        final IntMap<V> map;
        int nextIndex, currentIndex;
        boolean valid = true;

        public MapIterator (IntMap<V> map) {
            this.map = map;
            reset();
        }

        public void reset () {
            this.currentIndex = INDEX_ILLEGAL;
            this.nextIndex = INDEX_ZERO;
            if (this.map.hasZeroValue) {
                this.hasNext = true;
            } else {
                findNextIndex();
            }
        }

        void findNextIndex () {
            this.hasNext = false;
            int[] keyTable = this.map.keyTable;
            for (int n = this.map.capacity + this.map.stashSize; ++this.nextIndex < n;) {
                if (keyTable[this.nextIndex] != EMPTY) {
                    this.hasNext = true;
                    break;
                }
            }
        }

        public void remove () {
            if (this.currentIndex == INDEX_ZERO && this.map.hasZeroValue) {
                this.map.zeroValue = null;
                this.map.hasZeroValue = false;
            } else if (this.currentIndex < 0) {
                throw new IllegalStateException("next must be called before remove.");
            } else if (this.currentIndex >= this.map.capacity) {
                this.map.removeStashIndex(this.currentIndex);
            } else {
                this.map.keyTable[this.currentIndex] = EMPTY;
                this.map.valueTable[this.currentIndex] = null;
            }
            this.currentIndex = INDEX_ILLEGAL;
            this.map.size--;
        }
    }

    static public class Entries<V> extends MapIterator<V> implements Iterable<Entry<V>>, Iterator<Entry<V>> {
        private Entry<V> entry = new Entry();

        public Entries (IntMap map) {
            super(map);
        }

        /** Note the same entry instance is returned each time this method is called. */
        @Override
        public Entry<V> next () {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            }
            if (!this.valid) {
                throw new RuntimeException("#iterator() cannot be used nested.");
            }
            int[] keyTable = this.map.keyTable;
            if (this.nextIndex == INDEX_ZERO) {
                this.entry.key = 0;
                this.entry.value = this.map.zeroValue;
            } else {
                this.entry.key = keyTable[this.nextIndex];
                this.entry.value = this.map.valueTable[this.nextIndex];
            }
            this.currentIndex = this.nextIndex;
            findNextIndex();
            return this.entry;
        }

        @Override
        public boolean hasNext () {
            return this.hasNext;
        }

        @Override
        public Iterator<Entry<V>> iterator () {
            return this;
        }
    }

    static public class Values<V> extends MapIterator<V> implements Iterable<V>, Iterator<V> {
        public Values (IntMap<V> map) {
            super(map);
        }

        @Override
        public boolean hasNext () {
            return this.hasNext;
        }

        @Override
        public V next () {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            }
            if (!this.valid) {
                throw new RuntimeException("#iterator() cannot be used nested.");
            }
            V value;
            if (this.nextIndex == INDEX_ZERO) {
                value = this.map.zeroValue;
            } else {
                value = this.map.valueTable[this.nextIndex];
            }
            this.currentIndex = this.nextIndex;
            findNextIndex();
            return value;
        }

        @Override
        public Iterator<V> iterator () {
            return this;
        }

        /** Returns a new array containing the remaining values. */
//		public Array<V> toArray () {
//			Array array = new Array(true, map.size);
//			while (hasNext) {
//                array.add(next());
//            }
//			return array;
//		}
    }

    static public class Keys extends MapIterator {
        public Keys (IntMap map) {
            super(map);
        }

        public int next () {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            }
            if (!this.valid) {
                throw new RuntimeException("#iterator() cannot be used nested.");
            }
            int key = this.nextIndex == INDEX_ZERO ? 0 : this.map.keyTable[this.nextIndex];
            this.currentIndex = this.nextIndex;
            findNextIndex();
            return key;
        }

        /** Returns a new array containing the remaining keys. */
        public IntArray toArray () {
            IntArray array = new IntArray(true, this.map.size);
            while (this.hasNext) {
                array.add(next());
            }
            return array;
        }
    }
}
