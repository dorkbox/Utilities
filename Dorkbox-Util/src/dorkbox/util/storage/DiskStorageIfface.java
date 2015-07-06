package dorkbox.util.storage;

import dorkbox.util.bytes.ByteArrayWrapper;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public
interface DiskStorageIfface {
    /**
     * Returns the number of objects in the database.
     */
    int size();

    /**
     * Checks if there is a object corresponding to the given key.
     */
    boolean contains(String key);

    /**
     * Reads a object using the default (blank) key, and casts it to the expected class
     */
    <T> T get();

    /**
     * Reads a object using the specific key, and casts it to the expected class
     */
    <T> T get(String key);

    /**
     * Reads a object using the specific key, and casts it to the expected class
     */
    <T> T get(byte[] key);

    /**
     * Reads a object using the specific key, and casts it to the expected class
     */
    <T> T get(ByteArrayWrapper key);

    /**
     * Uses the DEFAULT key ("") to return saved data.
     * <p/>
     * This will check to see if there is an associated key for that data, if not - it will use data as the default
     *
     * @param data The data that will hold the copy of the data from disk
     */
    <T> T load(T data) throws IOException;

    /**
     * Returns the saved data for the specified key.
     *
     * @param data If there is no object in the DB with the specified key, this value will be the default (and will be saved to the db)
     */
    <T> T load(String key, T data) throws IOException;

    /**
     * Returns the saved data for the specified key.
     *
     * @param data If there is no object in the DB with the specified key, this value will be the default (and will be saved to the db)
     */
    <T> T load(byte[] key, T data) throws IOException;

    /**
     * Returns the saved data for the specified key.
     *
     * @param data If there is no object in the DB with the specified key, this value will be the default (and will be saved to the db)
     */
    @SuppressWarnings("unchecked")
    <T> T load(ByteArrayWrapper key, T data) throws IOException;

    /**
     * Saves the given data to storage with the associated key.
     * <p/>
     * Also will update existing data. If the new contents do not fit in the original space, then the update is handled by
     * deleting the old data and adding the new.
     */
    void put(String key, Object data);

    /**
     * Saves the given data to storage with the associated key.
     * <p/>
     * Also will update existing data. If the new contents do not fit in the original space, then the update is handled by
     * deleting the old data and adding the new.
     */
    void put(byte[] key, Object data);

    /**
     * Saves the given data to storage with the associated key.
     * <p/>
     * Also will update existing data. If the new contents do not fit in the original space, then the update is handled by
     * deleting the old data and adding the new.
     */
    void put(ByteArrayWrapper key, Object data);

    /**
     * Adds the given object to the storage using a default (blank) key, OR -- if it has been registered, using it's registered key
     * <p/>
     * Also will update existing data. If the new contents do not fit in the original space, then the update is handled by
     * deleting the old data and adding the new.
     */
    void put(Object data);

    /**
     * Deletes an object from storage. To ALSO remove from the cache, use unRegister(key)
     *
     * @return true if the delete was successful. False if there were problems deleting the data.
     */
    boolean delete(String key);

    /**
     * @return the file that backs this storage
     */
    File getFile();

    /**
     * Gets the backing file size.
     *
     * @return -1 if there was an error
     */
    long getFileSize();

    /**
     * @return true if there are objects queued to be written?
     */
    boolean hasWriteWaiting();

    /**
     * @return the delay in milliseconds this will wait after the last action to flush the data to the disk
     */
    long getSaveDelay();

    /**
     * @param milliSeconds milliseconds to wait
     */
    void setSaveDelay(long milliSeconds);

    /**
     * @return the version of data stored in the database
     */
    int getVersion();

    /**
     * Sets the version of data stored in the database
     */
    void setVersion(int version);

    /**
     * Save the storage to disk, immediately.
     * <p/>
     * This will save the ALL of the pending save actions to the file
     */
    void commit();

    /**
     * Save the storage to disk, immediately.
     * <p/>
     * This will save the ALL of the pending save actions to the file
     */
    void commit(String key, Object object);

    /**
     * Save the storage to disk, immediately.
     * <p/>
     * This will save the ALL of the pending save actions to the file
     */
    void commit(byte[] key, Object object);

    /**
     * Save the storage to disk, immediately.
     * <p/>
     * This will save the ALL of the pending save actions to the file
     */
    void commit(ByteArrayWrapper key, Object object);
}
