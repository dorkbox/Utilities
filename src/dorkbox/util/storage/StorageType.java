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

import dorkbox.util.FileUtil;
import dorkbox.util.SerializationManager;

public
class StorageType {
    @SuppressWarnings("SpellCheckingInspection")
    private static final Map<File, Storage> storages = new HashMap<File, Storage>(1);

    // Make sure that the timer is run on shutdown. A HARD shutdown will just POW! kill it, a "nice" shutdown will run the hook
    private static Thread shutdownHook = new Thread(new Runnable() {
        @Override
        public
        void run() {
            StorageType.shutdown();
        }
    });

    static {
        // add a shutdown hook to make sure that we properly flush/shutdown storage.
        Runtime.getRuntime()
               .addShutdownHook(shutdownHook);
    }

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
            Storage storage = storages.get(file);
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
    void close(Storage _storage) {
        synchronized (storages) {
            File file = _storage.getFile();
            close(file);
        }
    }

    public static
    void shutdown() {
        synchronized (storages) {
            Collection<Storage> values = storages.values();
            for (Storage storage : values) {
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
            Storage remove = storages.remove(file);
            if (remove != null && remove instanceof DiskStorage) {
                ((DiskStorage) remove).close();
            }
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    public static
    void delete(Storage storage) {
        File file = storage.getFile();
        delete(file);
    }


    public static
    class DiskMaker {
        private File file;
        private SerializationManager serializationManager;
        private boolean readOnly = false;
        private Logger logger = null;

        public
        DiskMaker file(File file) {
            this.file = FileUtil.normalize(file);
            return this;
        }

        public
        DiskMaker file(String file) {
            this.file = FileUtil.normalize(file);
            return this;
        }

        public
        DiskMaker serializer(SerializationManager serializationManager) {
            this.serializationManager = serializationManager;
            return this;
        }

        public
        Storage make() {
            if (this.file == null) {
                throw new IllegalArgumentException("file cannot be null!");
            }

            if (this.serializationManager == null) {
                throw new IllegalArgumentException("serializer cannot be null!");
            }

            if (this.logger == null) {
                this.logger = NOPLogger.NOP_LOGGER;
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
                        storage = new DiskStorage(this.file, this.serializationManager, this.readOnly, this.logger);
                        storages.put(this.file, storage);
                    } catch (IOException e) {
                        logger.error("Unable to open storage", e);
                    }
                }

                return storage;
            }
        }

        public
        DiskMaker readOnly() {
            this.readOnly = true;
            return this;
        }

        public
        DiskMaker logger(final Logger logger) {
            this.logger = logger;
            return this;
        }
    }


    public static
    class MemoryMaker {
        public
        MemoryStorage make() throws IOException {
            return new MemoryStorage();
        }
    }
}