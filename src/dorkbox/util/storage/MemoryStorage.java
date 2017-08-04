/*
 * Copyright 2014 dorkbox, llc
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
package dorkbox.util.storage;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage that is in memory only (and is not persisted to disk)
 */
class MemoryStorage implements Storage {
    private final ConcurrentHashMap<StorageKey, Object> storage;
    private int version;


    MemoryStorage() {
        this.storage = new ConcurrentHashMap<StorageKey, Object>();
    }


    /**
     * Returns the number of objects in the database.
     */
    @Override
    public
    int size() {
        return storage.size();
    }

    /**
     * Checks if there is a object corresponding to the given key.
     */
    @Override
    public
    boolean contains(final StorageKey key) {
        return storage.containsKey(key);
    }

    /**
     * Reads a object using the specific key, and casts it to the expected class
     */
    @SuppressWarnings("unchecked")
    @Override
    public
    <T> T get(final StorageKey key) {
        return (T) storage.get(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public
    <T> T get(final StorageKey key, final T data) {
        final Object o = storage.get(key);
        if (o == null) {
            storage.put(key, data);
            return data;
        }
        return (T) o;
    }

    /**
     * Saves the given data to storage with the associated key.
     * <p/>
     * Also will update existing data. If the new contents do not fit in the original space, then the update is handled by
     * deleting the old data and adding the new.
     */
    @Override
    public
    void put(final StorageKey key, final Object object) {
        storage.put(key, object);
    }

    /**
     * Deletes an object from storage.
     *
     * @return true if the delete was successful. False if there were problems deleting the data.
     */
    @Override
    public
    boolean delete(final StorageKey key) {
        storage.remove(key);
        return true;
    }

    /**
     * @return null. There is no file that backs this storage
     */
    @Override
    public
    File getFile() {
        return null;
    }

    /**
     * Gets the backing file size.
     *
     * @return 0. There is no file that backs this storage
     */
    @Override
    public
    long getFileSize() {
        return 0;
    }

    /**
     * @return false. Writes to in-memory storage are immediate.
     */
    @Override
    public
    boolean hasWriteWaiting() {
        return false;
    }

    /**
     * @return 0. There is no file that backs this storage
     */
    @Override
    public
    long getSaveDelay() {
        return 0;
    }


    /**
     * There is no file that backs this storage, so saves/writes are immediate
     */
    @Override
    public
    void setSaveDelay(final long milliSeconds) {
        // no-op
    }

    /**
     * @return the version of data stored in the database
     */
    @Override
    public synchronized
    int getVersion() {
        return version;
    }

    /**
     * Sets the version of data stored in the database
     */
    @Override
    public synchronized
    void setVersion(final int version) {
        this.version = version;
    }

    /**
     * There is no file that backs this storage, so writes are immediate and saves do nothgin
     */
    @Override
    public
    void save() {
        // no-op
    }

    /**
     * In-memory storage systems do not have a backing file, so there is nothing to close
     */
    public
    void close() {
        StorageSystem.close(this);
    }
}
