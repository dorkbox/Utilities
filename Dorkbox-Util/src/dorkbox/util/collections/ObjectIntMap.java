/*******************************************************************************
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
package dorkbox.util.collections;

import com.esotericsoftware.kryo.util.ObjectMap;
import dorkbox.util.MathUtils;

/** An unordered map where the values are ints. This implementation is a cuckoo hash map using 3 hashes, random walking, and a
 * small stash for problematic keys. Null keys are not allowed. No allocation is done except when growing the table size. <br>
 * <br>
 * This map performs very fast get, containsKey, and remove (typically O(1), worst case O(log(n))). Put may be a bit slower,
 * depending on hash collisions. Load factors greater than 0.91 greatly increase the chances the map will have to rehash to the
 * next higher POT size.
 * @author Nathan Sweet */
public class ObjectIntMap<K> {
    @SuppressWarnings("unused")
    private static final int PRIME1 = 0xbe1f14b1;
    private static final int PRIME2 = 0xb4b82e39;
    private static final int PRIME3 = 0xced1c241;

    public int size;

    K[] keyTable;
    int[] valueTable;
    int capacity, stashSize;

    private float loadFactor;
    private int hashShift, mask, threshold;
    private int stashCapacity;
    private int pushIterations;

    /** Creates a new map with an initial capacity of 32 and a load factor of 0.8. This map will hold 25 items before growing the
    * backing table. */
    public ObjectIntMap () {
        this(32, 0.8f);
    }

    /** Creates a new map with a load factor of 0.8. This map will hold initialCapacity * 0.8 items before growing the backing
    * table. */
    public ObjectIntMap (int initialCapacity) {
        this(initialCapacity, 0.8f);
    }

    /** Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity * loadFactor items
    * before growing the backing table. */
    @SuppressWarnings("unchecked")
    public ObjectIntMap (int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be >= 0: " + initialCapacity);
        }
        if (initialCapacity > 1 << 30) {
            throw new IllegalArgumentException("initialCapacity is too large: " + initialCapacity);
        }
        this.capacity = ObjectMap.nextPowerOfTwo(initialCapacity);

        if (loadFactor <= 0) {
            throw new IllegalArgumentException("loadFactor must be > 0: " + loadFactor);
        }
        this.loadFactor = loadFactor;

        this.threshold = (int)(this.capacity * loadFactor);
        this.mask = this.capacity - 1;
        this.hashShift = 31 - Integer.numberOfTrailingZeros(this.capacity);
        this.stashCapacity = Math.max(3, (int)Math.ceil(Math.log(this.capacity)) * 2);
        this.pushIterations = Math.max(Math.min(this.capacity, 8), (int)Math.sqrt(this.capacity) / 8);

        this.keyTable = (K[])new Object[this.capacity + this.stashCapacity];
        this.valueTable = new int[this.keyTable.length];
    }

    /** Creates a new map identical to the specified map. */
    public ObjectIntMap (ObjectIntMap<? extends K> map) {
        this(map.capacity, map.loadFactor);
        this.stashSize = map.stashSize;
        System.arraycopy(map.keyTable, 0, this.keyTable, 0, map.keyTable.length);
        System.arraycopy(map.valueTable, 0, this.valueTable, 0, map.valueTable.length);
        this.size = map.size;
    }

    public void put (K key, int value) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null.");
        }
        K[] keyTable = this.keyTable;

        // Check for existing keys.
        int hashCode = key.hashCode();
        int index1 = hashCode & this.mask;
        K key1 = keyTable[index1];
        if (key.equals(key1)) {
            this.valueTable[index1] = value;
            return;
        }

        int index2 = hash2(hashCode);
        K key2 = keyTable[index2];
        if (key.equals(key2)) {
            this.valueTable[index2] = value;
            return;
        }

        int index3 = hash3(hashCode);
        K key3 = keyTable[index3];
        if (key.equals(key3)) {
            this.valueTable[index3] = value;
            return;
        }

        // Update key in the stash.
        for (int i = this.capacity, n = i + this.stashSize; i < n; i++) {
            if (key.equals(keyTable[i])) {
                this.valueTable[i] = value;
                return;
            }
        }

        // Check for empty buckets.
        if (key1 == null) {
            keyTable[index1] = key;
            this.valueTable[index1] = value;
            if (this.size++ >= this.threshold) {
                resize(this.capacity << 1);
            }
            return;
        }

        if (key2 == null) {
            keyTable[index2] = key;
            this.valueTable[index2] = value;
            if (this.size++ >= this.threshold) {
                resize(this.capacity << 1);
            }
            return;
        }

        if (key3 == null) {
            keyTable[index3] = key;
            this.valueTable[index3] = value;
            if (this.size++ >= this.threshold) {
                resize(this.capacity << 1);
            }
            return;
        }

        push(key, value, index1, key1, index2, key2, index3, key3);
    }

    /** Skips checks for existing keys. */
    private void putResize (K key, int value) {
        // Check for empty buckets.
        int hashCode = key.hashCode();
        int index1 = hashCode & this.mask;
        K key1 = this.keyTable[index1];
        if (key1 == null) {
            this.keyTable[index1] = key;
            this.valueTable[index1] = value;
            if (this.size++ >= this.threshold) {
                resize(this.capacity << 1);
            }
            return;
        }

        int index2 = hash2(hashCode);
        K key2 = this.keyTable[index2];
        if (key2 == null) {
            this.keyTable[index2] = key;
            this.valueTable[index2] = value;
            if (this.size++ >= this.threshold) {
                resize(this.capacity << 1);
            }
            return;
        }

        int index3 = hash3(hashCode);
        K key3 = this.keyTable[index3];
        if (key3 == null) {
            this.keyTable[index3] = key;
            this.valueTable[index3] = value;
            if (this.size++ >= this.threshold) {
                resize(this.capacity << 1);
            }
            return;
        }

        push(key, value, index1, key1, index2, key2, index3, key3);
    }

    private void push (K insertKey, int insertValue, int index1, K key1, int index2, K key2, int index3, K key3) {
        K[] keyTable = this.keyTable;
        int[] valueTable = this.valueTable;
        int mask = this.mask;

        // Push keys until an empty bucket is found.
        K evictedKey;
        int evictedValue;
        int i = 0, pushIterations = this.pushIterations;
        do {
            // Replace the key and value for one of the hashes.
            switch (MathUtils.randomInt(2)) {
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
            int hashCode = evictedKey.hashCode();
            index1 = hashCode & mask;
            key1 = keyTable[index1];
            if (key1 == null) {
                keyTable[index1] = evictedKey;
                valueTable[index1] = evictedValue;
                if (this.size++ >= this.threshold) {
                    resize(this.capacity << 1);
                }
                return;
            }

            index2 = hash2(hashCode);
            key2 = keyTable[index2];
            if (key2 == null) {
                keyTable[index2] = evictedKey;
                valueTable[index2] = evictedValue;
                if (this.size++ >= this.threshold) {
                    resize(this.capacity << 1);
                }
                return;
            }

            index3 = hash3(hashCode);
            key3 = keyTable[index3];
            if (key3 == null) {
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

    private void putStash (K key, int value) {
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

    /** @param defaultValue Returned if the key was not associated with a value. */
    public int get (K key, int defaultValue) {
        int hashCode = key.hashCode();
        int index = hashCode & this.mask;
        if (!key.equals(this.keyTable[index])) {
            index = hash2(hashCode);
            if (!key.equals(this.keyTable[index])) {
                index = hash3(hashCode);
                if (!key.equals(this.keyTable[index])) {
                    return getStash(key, defaultValue);
                }
            }
        }
        return this.valueTable[index];
    }

    private int getStash (K key, int defaultValue) {
        K[] keyTable = this.keyTable;
        for (int i = this.capacity, n = i + this.stashSize; i < n; i++) {
            if (key.equals(keyTable[i])) {
                return this.valueTable[i];
            }
        }
        return defaultValue;
    }

    /** Returns the key's current value and increments the stored value. If the key is not in the map, defaultValue + increment is
    * put into the map. */
    public int getAndIncrement (K key, int defaultValue, int increment) {
        int hashCode = key.hashCode();
        int index = hashCode & this.mask;
        if (!key.equals(this.keyTable[index])) {
            index = hash2(hashCode);
            if (!key.equals(this.keyTable[index])) {
                index = hash3(hashCode);
                if (!key.equals(this.keyTable[index])) {
                    return getAndIncrementStash(key, defaultValue, increment);
                }
            }
        }
        int value = this.valueTable[index];
        this.valueTable[index] = value + increment;
        return value;
    }

    private int getAndIncrementStash (K key, int defaultValue, int increment) {
        K[] keyTable = this.keyTable;
        for (int i = this.capacity, n = i + this.stashSize; i < n; i++) {
            if (key.equals(keyTable[i])) {
                int value = this.valueTable[i];
                this.valueTable[i] = value + increment;
                return value;
            }
        }
        put(key, defaultValue + increment);
        return defaultValue;
    }

    public int remove (K key, int defaultValue) {
        int hashCode = key.hashCode();
        int index = hashCode & this.mask;
        if (key.equals(this.keyTable[index])) {
            this.keyTable[index] = null;
            int oldValue = this.valueTable[index];
            this.size--;
            return oldValue;
        }

        index = hash2(hashCode);
        if (key.equals(this.keyTable[index])) {
            this.keyTable[index] = null;
            int oldValue = this.valueTable[index];
            this.size--;
            return oldValue;
        }

        index = hash3(hashCode);
        if (key.equals(this.keyTable[index])) {
            this.keyTable[index] = null;
            int oldValue = this.valueTable[index];
            this.size--;
            return oldValue;
        }

        return removeStash(key, defaultValue);
    }

    int removeStash (K key, int defaultValue) {
        K[] keyTable = this.keyTable;
        for (int i = this.capacity, n = i + this.stashSize; i < n; i++) {
            if (key.equals(keyTable[i])) {
                int oldValue = this.valueTable[i];
                removeStashIndex(i);
                this.size--;
                return oldValue;
            }
        }
        return defaultValue;
    }

    void removeStashIndex (int index) {
        // If the removed location was not last, move the last tuple to the removed location.
        this.stashSize--;
        int lastIndex = this.capacity + this.stashSize;
        if (index < lastIndex) {
            this.keyTable[index] = this.keyTable[lastIndex];
            this.valueTable[index] = this.valueTable[lastIndex];
        }
    }

    /** Reduces the size of the backing arrays to be the specified capacity or less. If the capacity is already less, nothing is
    * done. If the map contains more items than the specified capacity, the next highest power of two capacity is used instead. */
    public void shrink (int maximumCapacity) {
        if (maximumCapacity < 0) {
            throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
        }
        if (this.size > maximumCapacity) {
            maximumCapacity = this.size;
        }
        if (this.capacity <= maximumCapacity) {
            return;
        }
        maximumCapacity = ObjectMap.nextPowerOfTwo(maximumCapacity);
        resize(maximumCapacity);
    }

    /** Clears the map and reduces the size of the backing arrays to be the specified capacity if they are larger. */
    public void clear (int maximumCapacity) {
        if (this.capacity <= maximumCapacity) {
            clear();
            return;
        }
        this.size = 0;
        resize(maximumCapacity);
    }

    public void clear () {
        K[] keyTable = this.keyTable;
        for (int i = this.capacity + this.stashSize; i-- > 0;) {
            keyTable[i] = null;
        }
        this.size = 0;
        this.stashSize = 0;
    }

    /** Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may be
    * an expensive operation. */
    public boolean containsValue (int value) {
        int[] valueTable = this.valueTable;
        for (int i = this.capacity + this.stashSize; i-- > 0;) {
            if (keyTable[i] != null && valueTable[i] == value) {
                return true;
            }
        }
        return false;
    }

    public boolean containsKey (K key) {
        int hashCode = key.hashCode();
        int index = hashCode & this.mask;
        if (!key.equals(this.keyTable[index])) {
            index = hash2(hashCode);
            if (!key.equals(this.keyTable[index])) {
                index = hash3(hashCode);
                if (!key.equals(this.keyTable[index])) {
                    return containsKeyStash(key);
                }
            }
        }
        return true;
    }

    private boolean containsKeyStash (K key) {
        K[] keyTable = this.keyTable;
        for (int i = this.capacity, n = i + this.stashSize; i < n; i++) {
            if (key.equals(keyTable[i])) {
                return true;
            }
        }
        return false;
    }

    /** Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
    * every value, which may be an expensive operation. */
    public K findKey (int value) {
        int[] valueTable = this.valueTable;
        for (int i = this.capacity + this.stashSize; i-- > 0;) {
            if (keyTable[i] != null && valueTable[i] == value) {
                return this.keyTable[i];
            }
        }
        return null;
    }

    /** Increases the size of the backing array to acommodate the specified number of additional items. Useful before adding many
    * items to avoid multiple backing array resizes. */
    public void ensureCapacity (int additionalCapacity) {
        int sizeNeeded = this.size + additionalCapacity;
        if (sizeNeeded >= this.threshold) {
            resize(ObjectMap.nextPowerOfTwo((int)(sizeNeeded / this.loadFactor)));
        }
    }

    @SuppressWarnings("unchecked")
    private void resize (int newSize) {
        int oldEndIndex = this.capacity + this.stashSize;

        this.capacity = newSize;
        this.threshold = (int)(newSize * this.loadFactor);
        this.mask = newSize - 1;
        this.hashShift = 31 - Integer.numberOfTrailingZeros(newSize);
        this.stashCapacity = Math.max(3, (int)Math.ceil(Math.log(newSize)) * 2);
        this.pushIterations = Math.max(Math.min(newSize, 8), (int)Math.sqrt(newSize) / 8);

        K[] oldKeyTable = this.keyTable;
        int[] oldValueTable = this.valueTable;

        this.keyTable = (K[])new Object[newSize + this.stashCapacity];
        this.valueTable = new int[newSize + this.stashCapacity];

        int oldSize = this.size;
        this.size = 0;
        this.stashSize = 0;
        if (oldSize > 0) {
            for (int i = 0; i < oldEndIndex; i++) {
                K key = oldKeyTable[i];
                if (key != null) {
                    putResize(key, oldValueTable[i]);
                }
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
            return "{}";
        }
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('{');
        K[] keyTable = this.keyTable;
        int[] valueTable = this.valueTable;
        int i = keyTable.length;
        while (i-- > 0) {
            K key = keyTable[i];
            if (key == null) {
                continue;
            }
            buffer.append(key);
            buffer.append('=');
            buffer.append(valueTable[i]);
            break;
        }
        while (i-- > 0) {
            K key = keyTable[i];
            if (key == null) {
                continue;
            }
            buffer.append(", ");
            buffer.append(key);
            buffer.append('=');
            buffer.append(valueTable[i]);
        }
        buffer.append('}');
        return buffer.toString();
    }
}
