package dorkbox.util.storage;

import dorkbox.util.SerializationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public
class MakeStorage {
    private static final Logger logger = LoggerFactory.getLogger(DiskStorage.class);
    @SuppressWarnings("SpellCheckingInspection")
    private static final Map<File, DiskStorageIfface> storages = new HashMap<File, DiskStorageIfface>(1);

    public static
    DiskMaker Disk() {
        return new DiskMaker();
    }

    public static
    MemoryMaker Memory() {
        return new MemoryMaker();
    }

    /**
     * Closes the storage.
     */
    public static
    void close(File file) {
        synchronized (storages) {
            DiskStorageIfface storage = storages.get(file);
            if (storage != null) {
                if (storage instanceof DiskStorage) {
                    final DiskStorage diskStorage = (DiskStorage) storage;
                    boolean isLastOne = diskStorage.decrementReference();
                    if (isLastOne) {
                        diskStorage.close();
                        storages.remove(file);
                    }
                }
            }
        }
    }

    /**
     * Closes the storage.
     */
    public static
    void close(DiskStorageIfface _storage) {
        synchronized (storages) {
            File file = _storage.getFile();
            DiskStorageIfface storage = storages.get(file);
            if (storage != null) {
                if (storage instanceof DiskStorage) {
                    final DiskStorage diskStorage = (DiskStorage) storage;
                    boolean isLastOne = diskStorage.decrementReference();
                    if (isLastOne) {
                        diskStorage.close();
                        storages.remove(file);
                    }
                }
            }
        }
    }

    public static
    void shutdown() {
        synchronized (storages) {
            Collection<DiskStorageIfface> values = storages.values();
            for (DiskStorageIfface storage : values) {
                if (storage instanceof DiskStorage) {
                    //noinspection StatementWithEmptyBody
                    final DiskStorage diskStorage = (DiskStorage) storage;
                    while (!diskStorage.decrementReference()) {
                    }
                    diskStorage.close();
                }
            }
            storages.clear();
        }
    }

    public static
    void delete(File file) {
        synchronized (storages) {
            DiskStorageIfface remove = storages.remove(file);
            if (remove != null && remove instanceof DiskStorage) {
                ((DiskStorage) remove).close();
            }
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    public static
    void delete(DiskStorageIfface storage) {
        File file = storage.getFile();
        delete(file);
    }


    public static
    class DiskMaker {
        private File file;
        private SerializationManager serializationManager;

        public
        DiskMaker file(File file) {
            this.file = file;
            return this;
        }

        public
        DiskMaker serializer(SerializationManager serializationManager) {
            this.serializationManager = serializationManager;
            return this;
        }

        public
        DiskStorageIfface make() {
            if (file == null) {
                throw new IllegalArgumentException("file cannot be null!");
            }

            // if we load from a NEW storage at the same location as an ALREADY EXISTING storage,
            // without saving the existing storage first --- whoops!
            synchronized (storages) {
                DiskStorageIfface storage = storages.get(file);

                if (storage != null) {
                    if (storage instanceof DiskStorage) {
                        boolean waiting = storage.hasWriteWaiting();
                        // we want this storage to be in a fresh state
                        if (waiting) {
                            storage.commit();
                        }
                        ((DiskStorage) storage).increaseReference();
                    }
                    else {
                        throw new RuntimeException("Unable to change storage types for: " + file);
                    }
                }
                else {
                    try {
                        storage = new DiskStorage(file, serializationManager);
                        storages.put(file, storage);
                    } catch (IOException e) {
                        logger.error("Unable to open storage", e);
                    }
                }

                return storage;
            }
        }
    }


    public static
    class MemoryMaker {
        private SerializationManager serializationManager;

        public
        MemoryMaker serializer(SerializationManager serializationManager) {
            this.serializationManager = serializationManager;
            return this;
        }

        MemoryStorage make() {
            return null;
        }
    }
}
