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
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import com.esotericsoftware.kryo.Serializer;

import dorkbox.os.OS;
import dorkbox.util.FileUtil;
import dorkbox.util.serialization.SerializationManager;

public
class StorageSystem {
    private static final Map<File, Storage> storages = new HashMap<File, Storage>(1);

    // Make sure that the timer is run on shutdown. A HARD shutdown will just POW! kill it, a "nice" shutdown will run the hook
    private static final Thread shutdownHook = new Thread(new Runnable() {
        @Override
        public
        void run() {
            StorageSystem.shutdown();
        }
    });

    static {
        // add a shutdown hook to make sure that we properly flush/shutdown storage.
        Runtime.getRuntime()
               .addShutdownHook(shutdownHook);
    }


    /**
     * Creates a persistent, on-disk storage system. Writes to disk are queued, so it is recommended to NOT edit/change an object after
     * it has been put into storage, or whenever it does changes, make sure to put it back into storage (to update the saved record)
     */
    public static
    DiskBuilder Disk() {
        return new DiskBuilder();
    }

    /**
     * Creates an in-memory only storage system
     */
    public static
    MemoryBuilder Memory() {
        return new MemoryBuilder();
    }

    /**
     * Closes the specified storage system based on the file used
     */
    public static
    void close(final File file) {
        synchronized (storages) {
            Storage storage = storages.get(file);
            if (storage != null) {
                if (storage instanceof DiskStorage) {
                    final DiskStorage diskStorage = (DiskStorage) storage;
                    boolean isLastOne = diskStorage.decrementReference();
                    if (isLastOne) {
                        diskStorage.closeFully();
                        storages.remove(file);
                    }
                }
            }
        }
    }

    /**
     * Closes the specified storage system
     */
    public static
    void close(final Storage storage) {
        synchronized (storages) {
            File file = storage.getFile();
            close(file);
        }
    }

    /**
     * Saves and closes all open storage systems
     */
    public static
    void shutdown() {
        synchronized (storages) {
            Collection<Storage> values = storages.values();
            for (Storage storage : values) {
                if (storage instanceof DiskStorage) {
                    final DiskStorage diskStorage = (DiskStorage) storage;
                    //noinspection StatementWithEmptyBody
                    while (!diskStorage.decrementReference()) {
                    }
                    diskStorage.closeFully();
                }
            }
            storages.clear();
        }
    }

    /**
     * Closes (if in use) and deletes the specified storage file.
     * <p>
     * The file is checked to see if it is in use by the storage system first, and closes if so.
     */
    public static
    void delete(File file) {
        synchronized (storages) {
            Storage remove = storages.remove(file);
            if (remove instanceof DiskStorage) {
                ((DiskStorage) remove).closeFully();
            }
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    /**
     * Closes (if in use) and deletes the specified storage.
     */
    public static
    void delete(Storage storage) {
        File file = storage.getFile();
        delete(file);
    }

    /**
     * Creates a persistent, on-disk storage system. Writes to disk are queued, so it is recommended to NOT edit/change an object after
     * it has been put into storage, or whenever it does changes, make sure to put it back into storage (to update the saved record)
     */
    @SuppressWarnings("unused")
    public static
    class DiskBuilder implements StorageBuilder {
        public File file;
        public SerializationManager<?> serializationManager = new DefaultStorageSerializationManager(); // default
        public boolean readOnly = false;
        public Logger logger = null;
        public long saveDelayInMilliseconds = 3000L; // default

        /**
         * Specify the file to write to on disk when saving objects
         */
        public
        DiskBuilder file(File file) {
            this.file = FileUtil.normalize(file);
            return this;
        }

        /**
         * Specify the file to write to on disk when saving objects
         */
        public
        DiskBuilder file(String file) {
            this.file = FileUtil.normalize(file);
            return this;
        }

        /**
         * Specify the serialization manager to use. This is what serializes the files (which are then saved to disk)
         */
        public
        DiskBuilder serializer(SerializationManager<?> serializationManager) {
            this.serializationManager = serializationManager;
            return this;
        }

        /**
         * Mark this storage system as read only
         */
        public
        DiskBuilder readOnly() {
            this.readOnly = true;
            return this;
        }

        /**
         * Mark this storage system as read only
         */
        public
        DiskBuilder setSaveDelay(long saveDelayInMilliseconds) {
            this.saveDelayInMilliseconds = saveDelayInMilliseconds;
            return this;
        }

        /**
         * Assigns a logger to use for the storage system. If null, then only errors will be logged to the error console.
         */
        public
        DiskBuilder logger(final Logger logger) {
            this.logger = logger;
            return this;
        }

        /**
         * Assigns a No Operation (NOP) logger which will ignore everything. This is not recommended for normal use, as it will also
         * suppress serialization errors.
         */
        public
        DiskBuilder noLogger() {
            this.logger = NOPLogger.NOP_LOGGER;
            return this;
        }

        @Override
        public
        <T> StorageBuilder register(final Class<T> clazz) {
            this.serializationManager.register(clazz);
            return this;
        }

        @Override
        public
        <T> StorageBuilder register(final Class<T> clazz, final int id) {
            this.serializationManager.register(clazz, id);
            return this;
        }

        @Override
        public
        <T> StorageBuilder register(final Class<T> clazz, final Serializer<T> serializer) {
            this.serializationManager.register(clazz, serializer);
            return this;
        }

        @Override
        public
        <T> StorageBuilder register(final Class<T> clazz, final Serializer<T> serializer, final int id) {
            this.serializationManager.register(clazz, serializer, id);
            return this;
        }

        /**
         * Makes the storage system
         */
        @Override
        public
        Storage build() {
            if (this.file == null) {
                throw new IllegalArgumentException("file cannot be null!");
            }

            // if we load from a NEW storage at the same location as an ALREADY EXISTING storage,
            // without saving the existing storage first --- whoops!
            synchronized (storages) {
                Storage storage = storages.get(this.file);

                if (storage != null) {
                    if (storage instanceof DiskStorage) {
                        boolean waiting = storage.hasWriteWaiting();
                        // we want this storage to be in a fresh state
                        if (waiting) {
                            storage.save();
                        }
                        ((DiskStorage) storage).increaseReference();
                    }
                    else {
                        throw new RuntimeException("Unable to change storage types for: " + this.file);
                    }
                }
                else {
                    try {
                        storage = new DiskStorage(this.file, this.serializationManager, this.readOnly, this.saveDelayInMilliseconds, this.logger);
                        storages.put(this.file, storage);
                    } catch (IOException e) {
                        String message = e.getMessage();
                        int index = message.indexOf(OS.LINE_SEPARATOR);
                        if (index > -1) {
                            message = message.substring(0, index);
                        }
                        if (logger != null) {
                            logger.error("Unable to open storage file at {}. {}", this.file, message);
                        }
                        else {
                            System.err.print("Unable to open storage file at " + this.file + ". " + message);
                        }
                    }
                }

                return storage;
            }
        }
    }


    /**
     * Creates an in-memory only storage system. This storage system DOES NOT care about serializing data, so `register` has no effect.
     */
    public static
    class MemoryBuilder implements StorageBuilder {

        /**
         * Builds the storage system
         */
        @Override
        public
        Storage build() {
            return new MemoryStorage();
        }

        @Override
        public
        <T> StorageBuilder register(final Class<T> clazz) {
            return this;
        }

        @Override
        public
        <T> StorageBuilder register(final Class<T> clazz, final int id) {
            return this;
        }

        @Override
        public
        <T> StorageBuilder register(final Class<T> clazz, final Serializer<T> serializer) {
            return this;
        }

        @Override
        public
        <T> StorageBuilder register(final Class<T> clazz, final Serializer<T> serializer, final int id) {
            return this;
        }
    }
}
