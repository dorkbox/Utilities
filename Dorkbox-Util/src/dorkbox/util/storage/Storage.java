package dorkbox.util.storage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dorkbox.util.DelayTimer;
import dorkbox.util.OS;
import dorkbox.util.bytes.ByteArrayWrapper;

/**
 * Nothing spectacular about this storage -- it allows for persistent storage of objects to disk.
 */
public class Storage {
    private static final Logger logger = LoggerFactory.getLogger(Storage.class);

    private static Map<File, Storage> storages = new HashMap<File, Storage>(1);

    public static Storage open(String file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file cannot be null or empty!");
        }
        return open(new File(file));
    }

    /**
     * Two types of storage.
     * Raw) save/load a single object to disk (better for really large files)
     * Normal) save/load key/value objects to disk (better for multiple types of data in a single file)
     */
    public static Storage open(File file) {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null!");
        }

        // if we load from a NEW storage at the same location as an ALREADY EXISTING storage,
        // without saving the existing storage first --- whoops!
        synchronized (storages) {
            Storage storage = storages.get(file);

            if (storage != null) {
                boolean waiting = storage.hasWriteWaiting();
                // we want this storage to be in a fresh state
                if (waiting) {
                    storage.saveNow();
                }
                storage.increaseReference();
            } else {
                try {
                    storage = new Storage(file);
                    storages.put(file, storage);
                } catch (IOException e) {
                    logger.error("Unable to open storage", e);
                }
            }

            return storage;
        }
    }


    /**
     * Closes the storage.
     */
    public static void close(File file) {
        synchronized (storages) {
            Storage storage = storages.get(file);
            if (storage != null) {
                boolean isLastOne = storage.decrementReference();
                if (isLastOne) {
                    storage.close();
                    storages.remove(file);
                }
            }
        }
    }

    /**
     * Closes the storage.
     */
    public static void close(Storage _storage) {
        synchronized (storages) {
            File file = _storage.getFile();
            Storage storage = storages.get(file);
            if (storage != null) {
                boolean isLastOne = storage.decrementReference();
                if (isLastOne) {
                    storage.close();
                    storages.remove(file);
                }
            }
        }
    }

    public static void shutdown() {
        synchronized(storages) {
            Collection<Storage> values = storages.values();
            for (Storage storage : values) {
                while (!storage.decrementReference()) {
                }
                storage.close();
            }
            storages.clear();
        }
    }

    public static void delete(File file) {
        synchronized(storages) {
            Storage remove = storages.remove(file);
            if (remove != null) {
                remove.close();
            }
            file.delete();
        }
    }

    public static void delete(Storage storage) {
        File file = storage.getFile();
        delete(file);
    }


    private static void copyFields(Object source, Object dest) {
        Class<? extends Object> sourceClass = source.getClass();
        Class<? extends Object> destClass = dest.getClass();

        if (sourceClass != destClass) {
            throw new IllegalArgumentException("Source and Dest objects are not of the same class!");
        }

        // have to walk up the object hierarchy.
        while (destClass != Object.class) {
            Field[] destFields = destClass.getDeclaredFields();

            for (Field destField : destFields) {
                String name = destField.getName();
                try {
                    Field sourceField = sourceClass.getDeclaredField(name);
                    destField.setAccessible(true);
                    sourceField.setAccessible(true);

                    Object sourceObj = sourceField.get(source);

                    if (sourceObj instanceof Map) {
                        Object destObj = destField.get(dest);
                        if (destObj == null) {
                            destField.set(dest, sourceObj);
                        } else if (destObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<Object, Object> sourceMap = (Map<Object, Object>) sourceObj;
                            @SuppressWarnings("unchecked")
                            Map<Object, Object> destMap = (Map<Object, Object>) destObj;

                            destMap.clear();
                            Iterator<?> entries = sourceMap.entrySet().iterator();
                            while (entries.hasNext()) {
                                Map.Entry<?, ?> entry = (Map.Entry<?, ?>)entries.next();
                                Object key = entry.getKey();
                                Object value = entry.getValue();
                                destMap.put(key, value);
                            }

                        } else {
                            logger.error("Incompatible field type! '{}'", name);
                        }
                    } else {
                        destField.set(dest, sourceObj);
                    }
                } catch (Exception e) {
                    logger.error("Unable to copy field: {}", name, e);
                }
            }

            destClass = destClass.getSuperclass();
            sourceClass = sourceClass.getSuperclass();
        }
    }

    /**
     * @return true if all of the fields in the two objects are the same.
     *
     *       NOTE: This is SLIGHTLY different than .equals(), in that there doesn't have to
     *             be an EXPLICIT .equals() method in the object
     */
    private static boolean compareFields(Object source, Object dest) {
        Class<? extends Object> sourceClass = source.getClass();
        Class<? extends Object> destClass = dest.getClass();

        if (sourceClass != destClass) {
            throw new IllegalArgumentException("Source and Dest objects are not of the same class!");
        }

        // have to walk up the object hierarchy.
        while (destClass != Object.class) {
            Field[] destFields = destClass.getDeclaredFields();

            for (Field destField : destFields) {
                String name = destField.getName();
                try {
                    Field sourceField = sourceClass.getDeclaredField(name);
                    destField.setAccessible(true);
                    sourceField.setAccessible(true);

                    Object sourceObj = sourceField.get(source);
                    Object destObj = destField.get(dest);

                    if (sourceObj == null) {
                        if (destObj == null) {
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        if (destObj == null) {
                            return false;
                        } else {
                            return destObj.equals(sourceObj);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Unable to copy field: {}", name, e);
                    return false;
                }
            }

            destClass = destClass.getSuperclass();
            sourceClass = sourceClass.getSuperclass();
        }

        return true;
    }









    private volatile long milliSeconds = 3000L;
    private volatile DelayTimer timer;

    private final ByteArrayWrapper defaultKey;
    private final StorageBase storage;

    private AtomicInteger references = new AtomicInteger(1);

    private final ReentrantLock actionLock = new ReentrantLock();
    private volatile Map<ByteArrayWrapper, Object> actionMap = new ConcurrentHashMap<ByteArrayWrapper, Object>();

    private AtomicBoolean isOpen = new AtomicBoolean(false);



    /**
     * Creates or opens a new database file.
     */
    private Storage(File storageFile) throws IOException {
        this.storage = new StorageBase(storageFile);
        this.defaultKey = wrap("");

        this.timer = new DelayTimer("Storage Writer", false, new DelayTimer.Callback() {
            @Override
            public void execute() {
                Map<ByteArrayWrapper, Object> actions = Storage.this.actionMap;

                ReentrantLock actionLock2 = Storage.this.actionLock;

                try {
                    actionLock2.lock();

                    // do a fast swap on the actionMap.
                    Storage.this.actionMap = new ConcurrentHashMap<ByteArrayWrapper, Object>();
                } finally {
                    actionLock2.unlock();
                }

                Storage.this.storage.doActionThings(actions);
            }
        });

        this.isOpen.set(true);
    }

    /**
     * Returns the number of objects in the database.
     * <p>
     * SLOW because this must save all data to disk first!
     */
    public final int size() {
        if (!this.isOpen.get()) {
            throw new RuntimeException("Unable to act on closed storage");
        }

        // flush actions
        // timer action runs on THIS thread, not timer thread
        this.timer.delay(0L);

        return this.storage.size();
    }

    /**
     * Checks if there is a object corresponding to the given key.
     */
    public final boolean contains(String key) {
        if (!this.isOpen.get()) {
            throw new RuntimeException("Unable to act on closed storage");
        }

        return this.storage.contains(wrap(key));
    }

    /**
     * Reads a object using the default (blank) key
     */
    public final <T> T get() {
        return get0(this.defaultKey);
    }

    /**
     * Reads a object using the specific key.
     */
    public final <T> T get(String key) {
        ByteArrayWrapper wrap = wrap(key);
        return get0(wrap);
    }

    /**
     * Copies the saved data (from) into the passed-in data.  This just assigns all of the values from one to the other.
     * <p>
     * This will check to see if there is an associated key for that data, if not - it will use the default (blank)
     *
     * @param data The data that will hold the copy of the data from disk
     */
    public void load(Object data) {
        Object source = get();

        if (source != null) {
            Storage.copyFields(source, data);
        }
    }

    /**
     * Copies the saved data (from) into the passed-in data.  This just assigns all of the values from one to the other.
     * <p>
     * The key/data will be associated for the lifetime of the object.
     *
     * @param object The object that will hold the copy of the data from disk (but not change the reference) once this is completed
     */
    public void load(String key, Object object) {
        ByteArrayWrapper wrap = wrap(key);

        Object source = get0(wrap);
        if (source != null) {
            Storage.copyFields(source, object);
        }
    }

    /**
     * Reads a object from pending or from storage
     */
    private final <T> T get0(ByteArrayWrapper key) {
        if (!this.isOpen.get()) {
            throw new RuntimeException("Unable to act on closed storage");
        }

        // if the object in is pending, we get it from there
        try {
            this.actionLock.lock();

            Object object = this.actionMap.get(key);

            if (object != null) {
                @SuppressWarnings("unchecked")
                T returnObject =  (T) object;
                return returnObject;
            }
        } finally {
            this.actionLock.unlock();
        }

        // not found, so we have to go find it on disk
        return this.storage.get(key);
    }

    /**
     * Save the storage to disk, once xxxx milli-seconds have passed.
     * This is to help prevent thrashing the disk, or wearing it out on multiple, rapid, changes.
     * <p>
     * This will save the ALL of the pending save actions to the file
     */
    public final void saveNow() {
        if (!this.isOpen.get()) {
            throw new RuntimeException("Unable to act on closed storage");
        }

        // timer action runs on THIS thread, not timer thread
        this.timer.delay(0L);
    }

    /**
     * Saves the given data to storage with the associated key.
     * <p>
     * Also will update existing data. If the new contents do not fit in the original space, then the update is handled by
     * deleting the old data and adding the new.
     */
    public final void save(String key, Object object) {
        if (!this.isOpen.get()) {
            throw new RuntimeException("Unable to act on closed storage");
        }

        ByteArrayWrapper wrap = wrap(key);
        action(wrap, object);

        // timer action runs on TIMER thread, not this thread
        this.timer.delay(this.milliSeconds);
    }

    /**
     * Saves the given object to storage with the associated key.
     * <p>
     * Also will update existing data. If the new contents do not fit in the original space, then the update is handled by
     * deleting the old data and adding the new.
     */
    public final void saveNow(String key, Object object) {
        if (!this.isOpen.get()) {
            throw new RuntimeException("Unable to act on closed storage");
        }

        ByteArrayWrapper wrap = wrap(key);
        action(wrap, object);

        // timer action runs on THIS thread, not timer thread
        this.timer.delay(0L);
    }

    /**
     * Adds the given object to the storage using a default (blank) key, OR -- if it has been registered, using it's registered key
     * <p>
     * Also will update existing data. If the new contents do not fit in the original space, then the update is handled by
     * deleting the old data and adding the new.
     */
    public final void save(Object object) {
        if (!this.isOpen.get()) {
            throw new RuntimeException("Unable to act on closed storage");
        }

        action(this.defaultKey, object);

        // timer action runs on TIMER thread, not this thread
        this.timer.delay(this.milliSeconds);
    }

    /**
     * Adds the given object to the storage using a default (blank) key
     * <p>
     * Also will update existing data. If the new contents do not fit in the original space, then the update is handled by
     * deleting the old data and adding the new.
     */
    public final void saveNow(Object object) {
        if (!this.isOpen.get()) {
            throw new RuntimeException("Unable to act on closed storage");
        }

        action(this.defaultKey, object);

        // timer action runs on THIS thread, not timer thread
        this.timer.delay(0L);
    }

    /**
     * Deletes an object from storage. To ALSO remove from the cache, use unRegister(key)
     *
     * @return true if the delete was successful. False if there were problems deleting the data.
     */
    public final boolean delete(String key) {
        if (!this.isOpen.get()) {
            throw new RuntimeException("Unable to act on closed storage");
        }

        ByteArrayWrapper wrap = wrap(key);

        // timer action runs on THIS thread, not timer thread
        this.timer.delay(0L);

        return this.storage.delete(wrap);
    }

    /**
     * Closes the database and file.
     */
    private final void close() {
        // timer action runs on THIS thread, not timer thread
        this.timer.delay(0L);

        // have to "close" it after we run the timer!
        this.isOpen.set(false);

        this.storage.close();
    }

    /**
     * @return the file that backs this storage
     */
    public final File getFile() {
        return this.storage.getFile();
    }

    /**
     * Gets the backing file size.
     *
     * @return -1 if there was an error
     */
    public final long getFileSize() {
        // timer action runs on THIS thread, not timer thread
        this.timer.delay(0L);

        return this.storage.getFileSize();
    }


    /**
     * @return true if there are objects queued to be written?
     */
    public final boolean hasWriteWaiting() {
        if (!this.isOpen.get()) {
            throw new RuntimeException("Unable to act on closed storage");
        }

        return this.timer.isWaiting();
    }

    /**
     * @param delay milliseconds to wait
     */
    public final void setSaveDelay(long milliSeconds) {
        if (!this.isOpen.get()) {
            throw new RuntimeException("Unable to act on closed storage");
        }

        this.milliSeconds = milliSeconds;
    }

    /**
     * @return the delay in milliseconds this will wait after the last action to flush the data to the disk
     */
    public final long getSaveDelay() {
        return this.milliSeconds;
    }

    /**
     * @return the version of data stored in the database
     */
    public final int getVersion() {
        return this.storage.getVersion();
    }

    /**
     * Sets the version of data stored in the database
     */
    public final void setVersion(int version) {
        this.storage.setVersion(version);
    }


    private final ByteArrayWrapper wrap(String key) {
        byte[] bytes = key.getBytes(OS.UTF_8);

        SHA256Digest digest = new SHA256Digest();
        digest.update(bytes, 0, bytes.length);
        byte[] hashBytes = new byte[digest.getDigestSize()];
        digest.doFinal(hashBytes, 0);
        ByteArrayWrapper wrap = ByteArrayWrapper.wrap(hashBytes);

        return wrap;
    }

    private void action(ByteArrayWrapper key, Object object) {
        try {
            this.actionLock.lock();

            // push action to map
            this.actionMap.put(key, object);
        } finally {
            this.actionLock.unlock();
        }
    }

    private final void increaseReference() {
        this.references.incrementAndGet();
    }

    /** return true when this is the last reference */
    private final boolean decrementReference() {
        return this.references.decrementAndGet() == 0;
    }
}
