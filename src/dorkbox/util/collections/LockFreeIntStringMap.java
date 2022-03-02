package dorkbox.util.collections;

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
 *
 * This is an unordered map that uses int keys. This implementation is a cuckoo hash map using 3 hashes, random walking, and a small stash
 * for problematic keys. Null values are allowed. No allocation is done except when growing the table size. <br>
 * <br>
 * This map performs very fast get, containsKey, and remove (typically O(1), worst case O(log(n))). Put may be a bit slower,
 * depending on hash collisions. Load factors greater than 0.91 greatly increase the chances the map will have to rehash to the
 * next higher POT size.
 */
@SuppressWarnings("unchecked")
public
class LockFreeIntStringMap<V> {
    private static final AtomicReferenceFieldUpdater<LockFreeIntStringMap, IntMap> mapREF = AtomicReferenceFieldUpdater.newUpdater(
            LockFreeIntStringMap.class,
            IntMap.class,
            "map");

    private volatile IntMap<V> map;


    public LockFreeIntStringMap() {
        this.map = new IntMap<>();
    }

    /**
     * Constructs an empty <tt>IntMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param initialCapacity the initial capacity.
     *
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public
    LockFreeIntStringMap(int initialCapacity) {
        map = new IntMap<>(initialCapacity);
    }

    /**
     * Constructs an empty <tt>IntMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param initialCapacity the initial capacity
     * @param loadFactor the load factor
     *
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    public
    LockFreeIntStringMap(int initialCapacity, float loadFactor) {
        this.map = new IntMap(initialCapacity, loadFactor);
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
    boolean containsKey(final int key) {
        // use the SWP to get a lock-free get of the value
        return mapREF.get(this)
                     .containsKey(key);
    }

    public
    boolean containsKey(final String key) {
        // use the SWP to get a lock-free get of the value
        return mapREF.get(this)
                     .containsKey(key.hashCode());
    }

    /**
     * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may be
     * an expensive operation.
     *
     * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
     *         {@link #equals(Object)}.
     */
    public
    boolean containsValue(final Object value, boolean identity) {
        // use the SWP to get a lock-free get of the value
        return mapREF.get(this)
                     .containsValue(value, identity);
    }

    public
    V get(final int key) {
        // use the SWP to get a lock-free get of the value
        return (V) mapREF.get(this)
                         .get(key);
    }

    public
    V get(final String key) {
        // use the SWP to get a lock-free get of the value
        return (V) mapREF.get(this)
                         .get(key.hashCode());
    }

    public synchronized
    V put(final int key, final V value) {
        return map.put(key, value);
    }

    public synchronized
    V put(final String key, final V value) {
        return map.put(key.hashCode(), value);
    }

    public synchronized
    V remove(final int key) {
        return map.remove(key);
    }

    public synchronized
    V remove(final String key) {
        return map.remove(key.hashCode());
    }

    public synchronized
    void putAll(final IntMap<V> map) {
        this.map.putAll(map);
    }

    /**
     * DO NOT MODIFY THE MAP VIA THIS (unless you synchronize around it!) It will result in unknown object visibility!
     *
     * Returns an iterator for the keys in the map. Remove is supported. Note that the same iterator instance is returned each
     * time this method is called. Use the {@link IntMap.Entries} constructor for nested or multi-threaded iteration.
     */
    public
    IntMap.Keys keys() {
        return mapREF.get(this)
                     .keys();
    }

    /**
     * DO NOT MODIFY THE MAP VIA THIS (unless you synchronize around it!) It will result in unknown object visibility!
     *
     * Returns an iterator for the values in the map. Remove is supported. Note that the same iterator instance is returned each
     * time this method is called. Use the {@link IntMap.Entries} constructor for nested or multi-threaded iteration.
     */
    public
    IntMap.Values<V> values() {
        return mapREF.get(this)
                     .values();
    }

    /**
     * DO NOT MODIFY THE MAP VIA THIS (unless you synchronize around it!) It will result in unknown object visibility!
     *
     * Returns an iterator for the entries in the map. Remove is supported. Note that the same iterator instance is returned each
     * time this method is called. Use the {@link IntMap.Entries} constructor for nested or multi-threaded iteration.
     */
    public
    IntMap.Entries<V> entries() {
        return mapREF.get(this)
                     .entries();
    }

    public synchronized
    void clear() {
        map.clear();
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
