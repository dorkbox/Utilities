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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.channels.Channels;
import java.nio.channels.FileLock;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import dorkbox.util.OS;
import dorkbox.util.SerializationManager;


// a note on file locking between c and java
// http://panks-dev.blogspot.de/2008/04/linux-file-locks-java-and-others.html
// Also, file locks on linux are ADVISORY. if an app doesn't care about locks, then it can do stuff -- even if locked by another app


class StorageBase {
    private final Logger logger;


    // File pointer to the data start pointer header.
    private static final long VERSION_HEADER_LOCATION = 0;

    // File pointer to the num records header.
    private static final long NUM_RECORDS_HEADER_LOCATION = 4;

    // File pointer to the data start pointer header.
    private static final long DATA_START_HEADER_LOCATION = 8;

    // Total length in bytes of the global database headers.
    static final int FILE_HEADERS_REGION_LENGTH = 16;


    // The in-memory index (for efficiency, all of the record info is cached in memory).
    private final Map<StorageKey, Metadata> memoryIndex;

    // determines how much the index will grow by
    private final Float weight;

    // The keys are weak! When they go, the map entry is removed!
    private final ReentrantLock referenceLock = new ReentrantLock();


    // file/raf that are used
    private final File baseFile;
    private final RandomAccessFile randomAccessFile;


    /**
     * Version number of database (4 bytes).
     */
    private int databaseVersion = 0;

    /**
     * Number of records (4 bytes).
     */
    private int numberOfRecords;

    /**
     * File pointer to the first byte of the record data (8 bytes).
     */
    private long dataPosition;


    // save references to these, so they don't have to be created/destroyed any time there is I/O
    private final SerializationManager serializationManager;

    private final Output output;
    private final Input input;

    // input/output write buffer size before flushing to/from the file
    public static final int BUFFER_SIZE = 1024;


    /**
     * Creates or opens a new database file.
     */
    StorageBase(final File filePath, final SerializationManager serializationManager, final Logger logger) throws IOException {
        this.serializationManager = serializationManager;
        this.logger = logger;

        if (logger != null) {
            logger.info("Opening storage file: '{}'", filePath.getAbsolutePath());
        }

        this.baseFile = filePath;

        boolean newStorage = !filePath.exists();

        if (newStorage) {
            File parentFile = this.baseFile.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                if (!parentFile.mkdirs()) {
                    throw new IOException("Unable to create dirs for: " + filePath);
                }
            }
        }

        this.randomAccessFile = new RandomAccessFile(this.baseFile, "rw");


        if (newStorage || this.randomAccessFile.length() <= FILE_HEADERS_REGION_LENGTH) {
            setVersion(this.randomAccessFile, 0);
            setRecordCount(this.randomAccessFile, 0);

            // pad the metadata with 21 records, so there is about 1k of padding before the data starts
            long indexPointer = Metadata.getMetaDataPointer(21);
            setDataStartPosition(indexPointer);
            // have to make sure we can read header info (even if it's blank)
            this.randomAccessFile.setLength(indexPointer);
        }
        else {
            this.randomAccessFile.seek(VERSION_HEADER_LOCATION);
            this.databaseVersion = this.randomAccessFile.readInt();
            this.numberOfRecords = this.randomAccessFile.readInt();
            this.dataPosition = this.randomAccessFile.readLong();

            if (this.randomAccessFile.length() < this.dataPosition) {
                if (logger != null) {
                    logger.error("Corrupted storage file!");
                }
                throw new IllegalArgumentException("Unable to parse header information from storage. Maybe it's corrupted?");
            }
        }

        //noinspection AutoBoxing
        if (logger != null) {
            logger.info("Storage version: {}", this.databaseVersion);
        }


        // If we want to use compression (no need really, since this file is small already),
        // then we have to make sure it's sync'd on flush AND have actually call outputStream.flush().
        final InputStream inputStream = Channels.newInputStream(randomAccessFile.getChannel());
        final OutputStream outputStream = Channels.newOutputStream(randomAccessFile.getChannel());

        // read/write 1024 bytes at a time
        output = new Output(outputStream, BUFFER_SIZE);
        input = new Input(inputStream, BUFFER_SIZE);


        //noinspection AutoBoxing
        this.weight = 0.5F;
        this.memoryIndex = new ConcurrentHashMap<StorageKey, Metadata>(this.numberOfRecords);

        if (!newStorage) {
            Metadata meta;
            for (int index = 0; index < this.numberOfRecords; index++) {
                meta = Metadata.readHeader(this.randomAccessFile, index);
                if (meta == null) {
                    // because we guarantee that empty metadata are ALWAYS at the end of the section, if we get a null one, break!
                    break;
                }
                this.memoryIndex.put(meta.key, meta);
            }

            if (this.memoryIndex.size() != (this.numberOfRecords)) {
                setRecordCount(this.randomAccessFile, this.memoryIndex.size());
                if (logger != null) {
                   logger.warn("Mismatch record count in storage, auto-correcting size.");
                }
            }
        }
    }

    /**
     * Returns the current number of records in the database.
     */
    final
    int size() {
        // wrapper flushes first (protected by lock)
        // not protected by lock
        return this.memoryIndex.size();
    }

    /**
     * Checks if there is a record belonging to the given key.
     */
    final
    boolean contains(StorageKey key) {
        // protected by lock

        // check to see if it's in the pending ops
        return this.memoryIndex.containsKey(key);
    }

    /**
     * @return an object for a specified key ONLY FROM THE REFERENCE CACHE
     */
    final
    <T> T getCached(StorageKey key) {
        // protected by lock

        Metadata meta = this.memoryIndex.get(key);
        if (meta == null) {
            return null;
        }

        // now stuff it into our reference cache so subsequent lookups are fast!
        //noinspection Duplicates
        try {
            this.referenceLock.lock();

            // if we have registered it, get it!
            WeakReference<Object> ref = meta.objectReferenceCache;

            if (ref != null) {
                @SuppressWarnings("unchecked")
                T referenceObject = (T) ref.get();
                return referenceObject;
            }
        } finally {
            this.referenceLock.unlock();
        }

        return null;
    }

    /**
     * @return an object for a specified key form referenceCache FIRST, then from DISK
     */
    final
    <T> T get(StorageKey key) throws IOException {
        // NOT protected by lock

        Metadata meta = this.memoryIndex.get(key);
        if (meta == null) {
            return null;
        }

        // now get it from our reference cache so subsequent lookups are fast!
        //noinspection Duplicates
        try {
            this.referenceLock.lock();

            // if we have registered it, get it!
            WeakReference<Object> ref = meta.objectReferenceCache;

            if (ref != null) {
                @SuppressWarnings("unchecked")
                T referenceObject = (T) ref.get();
                return referenceObject;
            }
        } finally {
            this.referenceLock.unlock();
        }


        try {
//            System.err.println("--Reading data from: " + meta.dataPointer);

            // else, we have to load it from disk
            this.randomAccessFile.seek(meta.dataPointer);

            T readRecordData = Metadata.readData(this.serializationManager, this.input);

            if (readRecordData != null) {
                // now stuff it into our reference cache for future lookups!
                try {
                    this.referenceLock.lock();

                    meta.objectReferenceCache = new WeakReference<Object>(readRecordData);
                } finally {
                    this.referenceLock.unlock();
                }
            }

            return readRecordData;
        } catch (Exception e) {
            if (e instanceof KryoException && e.getMessage().contains("(missing no-arg constructor)")) {
                throw new IOException("Cannot get data from disk: " + e.getMessage().substring(0, e.getMessage().indexOf(OS.LINE_SEPARATOR)));
            } else {
                throw new IOException("Cannot get data from disk", e);
            }
        }
    }

    /**
     * Deletes a record
     *
     * @return true if the delete was successful. False if there were problems deleting the data.
     */
    final
    boolean delete(StorageKey key) {
        // pending ops flushed (protected by lock)
        // not protected by lock
        Metadata delRec = this.memoryIndex.get(key);

        try {
            deleteRecordData(delRec, delRec.dataCapacity);
            deleteRecordIndex(key, delRec);
            return true;
        } catch (IOException e) {
            if (this.logger != null) {
                this.logger.error("Error while deleting data from disk", e);
            } else {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Closes the database and file.
     */
    final
    void close() {
        // pending ops flushed (protected by lock)
        // not protected by lock

        if (this.logger != null) {
            this.logger.info("Closing storage file: '{}'", this.baseFile.getAbsolutePath());
        }

        try {
            this.randomAccessFile.getFD()
                                 .sync();
            this.input.close();
            this.randomAccessFile.close();
            this.memoryIndex.clear();

        } catch (IOException e) {
            if (this.logger != null) {
                this.logger.error("Error while closing the file", e);
            } else {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets the backing file size.
     *
     * @return -1 if there was an error
     */
    long getFileSize() {
        // protected by actionLock
        try {
            return this.randomAccessFile.length();
        } catch (IOException e) {
            if (this.logger != null) {
                this.logger.error("Error getting file size for {}", this.baseFile.getAbsolutePath(), e);
            } else {
                e.printStackTrace();
            }
            return -1L;
        }
    }

    /**
     * @return the file that backs this storage
     */
    final
    File getFile() {
        return this.baseFile;
    }


    /**
     * Saves the given data to storage.
     * <p/>
     * Also will update existing data. If the new contents do not fit in the original space, then the update is handled by
     * deleting the old data and adding the new.
     * <p/>
     * Will also save the object in a cache.
     */
    private
    void save0(StorageKey key, Object object) {
        Metadata metaData = this.memoryIndex.get(key);
        int currentRecordCount = this.numberOfRecords;

        if (metaData != null) {
            // now we have to UPDATE instead of add!
            try {
                if (currentRecordCount == 1) {
                    // if we are the ONLY one, then we can do things differently.
                    // just dump the data again to disk.
                    FileLock lock = this.randomAccessFile.getChannel()
                                                         .lock(this.dataPosition,
                                                   Long.MAX_VALUE - this.dataPosition,
                                                   false); // don't know how big it is, so max value it

                    this.randomAccessFile.seek(this.dataPosition); // this is the end of the file, we know this ahead-of-time
                    Metadata.writeData(this.serializationManager, object, this.output);
                    // have to re-specify the capacity and size
                    //noinspection NumericCastThatLosesPrecision
                    int sizeOfWrittenData = (int) (this.randomAccessFile.length() - this.dataPosition);

                    metaData.dataCapacity = sizeOfWrittenData;
                    metaData.dataCount = sizeOfWrittenData;

                    lock.release();
                }
                else {
                    // this is comparatively slow, since we serialize it first to get the size, then we put it in the file.
                    ByteArrayOutputStream dataStream = getDataAsByteArray(this.serializationManager, this.logger, object);

                    int size = dataStream.size();
                    if (size > metaData.dataCapacity) {
                        deleteRecordData(metaData, size);
                        // stuff this record to the end of the file, since it won't fit in it's current location
                        metaData.dataPointer = this.randomAccessFile.length();
                        // have to make sure that the CAPACITY of the new one is the SIZE of the new data!
                        // and since it is going to the END of the file, we do that.
                        metaData.dataCapacity = size;
                        metaData.dataCount = 0;
                    }

                    // TODO: should check to see if the data is different. IF SO, then we write, otherwise nothing!

                    metaData.writeDataRaw(dataStream, this.randomAccessFile);
                }

                metaData.writeDataInfo(this.randomAccessFile);
            } catch (IOException e) {
                if (this.logger != null) {
                    this.logger.error("Error while saving data to disk", e);
                } else {
                    e.printStackTrace();
                }
            }
        }
        else {
            // metadata == null...
            try {
                // set the number of records that this storage has
                setRecordCount(this.randomAccessFile, currentRecordCount + 1);

                // This will make sure that there is room to write a new record. This is zero indexed.
                // this will skip around if moves occur
                ensureIndexCapacity(this.randomAccessFile);

                // append record to end of file
                long length = this.randomAccessFile.length();

//                System.err.println("--Writing data to: " + length);

                metaData = new Metadata(key, currentRecordCount, length);
                metaData.writeMetaDataInfo(this.randomAccessFile);

                // add new entry to the index
                this.memoryIndex.put(key, metaData);

                // save out the data. Because we KNOW that we are writing this to the end of the file,
                // there are some tricks we can use.

                // don't know how big it is, so max value it
                FileLock lock = this.randomAccessFile.getChannel()
                                                     .lock(0, Long.MAX_VALUE, false);

                // this is the end of the file, we know this ahead-of-time
                this.randomAccessFile.seek(length);

                int total = Metadata.writeData(this.serializationManager, object, this.output);
                lock.release();

                metaData.dataCount = metaData.dataCapacity = total;
                // have to save it.
                metaData.writeDataInfo(this.randomAccessFile);
            } catch (IOException e) {
                if (this.logger != null) {
                    this.logger.error("Error while writing data to disk", e);
                } else {
                    e.printStackTrace();
                }
                return;
            }
        }

        // put the object in the reference cache so we can read/get it later on
        metaData.objectReferenceCache = new WeakReference<Object>(object);
    }


    private static
    ByteArrayOutputStream getDataAsByteArray(SerializationManager serializationManager, Logger logger, Object data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Output output = new Output(outputStream, 1024); // write 1024 at a time

        serializationManager.writeFullClassAndObject(logger, output, data);
        output.flush();

        outputStream.flush();
        outputStream.close();

        return outputStream;
    }

    void doActionThings(Map<StorageKey, Object> actions) {

        // actions is thrown away after this invocation. GC can pick it up.
        // we are only interested in the LAST action that happened for some data.
        // items to be "autosaved" are automatically injected into "actions".
        final Set<Entry<StorageKey, Object>> entries = actions.entrySet();
        for (Entry<StorageKey, Object> entry : entries) {
            Object object = entry.getValue();
            StorageKey key = entry.getKey();

            // our action list is for explicitly saving objects (but not necessarily "registering" them to be auto-saved
            save0(key, object);
        }
    }


    /////////////////////
    /////////////////////
    // private/index only methods
    /////////////////////
    /////////////////////


    /**
     * "intelligent" move strategy.
     * <p/>
     * we should increase by some weight (ie: .5) would increase the number of allocated
     * record headers by 50%, instead of just incrementing them by one at a time.
     */
    private
    int getWeightedNewRecordCount(int numberOfRecords) {
        //noinspection AutoUnboxing,NumericCastThatLosesPrecision
        return numberOfRecords + 1 + (int) (numberOfRecords * this.weight);
    }


    private
    void deleteRecordData(Metadata deletedRecord, int sizeOfDataToAdd) throws IOException {
        if (this.randomAccessFile.length() == deletedRecord.dataPointer + deletedRecord.dataCapacity) {
            // shrink file since this is the last record in the file
            FileLock lock = this.randomAccessFile.getChannel()
                                                 .lock(deletedRecord.dataPointer, Long.MAX_VALUE - deletedRecord.dataPointer, false);
            this.randomAccessFile.setLength(deletedRecord.dataPointer);
            lock.release();
        }
        else {
            // we MIGHT be the FIRST record
            Metadata first = index_getMetaDataFromData(this.dataPosition);
            if (first == deletedRecord) {
                // the record to delete is the FIRST (of many) in the file.
                // the FASTEST way to delete is to grow the number of allowed records!
                // Another option is to move the #2 data to the first data, but then there is the same gap after #2.

                int numberOfRecords = this.numberOfRecords;

                // "intelligent" move strategy.
                int newNumberOfRecords = getWeightedNewRecordCount(numberOfRecords);
                long endIndexPointer = Metadata.getMetaDataPointer(newNumberOfRecords);

                long endOfDataPointer = deletedRecord.dataPointer + deletedRecord.dataCapacity;
                long newEndOfDataPointer = endOfDataPointer - sizeOfDataToAdd;

                if (endIndexPointer < this.dataPosition && endIndexPointer <= newEndOfDataPointer) {
                    // one option is to shrink the RECORD section to fit the new data
                    setDataStartPosition(newEndOfDataPointer);
                }
                else {
                    // option two is to grow the RECORD section, and put the data at the end of the file
                    setDataStartPosition(endOfDataPointer);
                }
            }
            else {
                Metadata previous = index_getMetaDataFromData(deletedRecord.dataPointer - 1);
                if (previous != null) {
                    // append space of deleted record onto previous record
                    previous.dataCapacity += deletedRecord.dataCapacity;
                    previous.writeDataInfo(this.randomAccessFile);
                }
                else {
                    // because there is no "previous", that means we MIGHT be the FIRST record
                    // well, we're not the first record. which one is RIGHT before us?
                    // it should be "previous", so something messed up
                    if (this.logger != null) {
                        this.logger.error("Trying to delete an object, and it's in a weird state");
                    } else {
                        System.err.println("Trying to delete an object, and it's in a weird state");
                    }
                }
            }
        }
    }

    private
    void deleteRecordIndex(StorageKey key, Metadata deleteRecord) throws IOException {
        int currentNumRecords = this.memoryIndex.size();

        if (deleteRecord.indexPosition != currentNumRecords - 1) {
            Metadata last = Metadata.readHeader(this.randomAccessFile, currentNumRecords - 1);
            assert last != null;

            last.moveRecord(this.randomAccessFile, deleteRecord.indexPosition);
        }

        this.memoryIndex.remove(key);

        setRecordCount(this.randomAccessFile, currentNumRecords - 1);
    }


    /**
     * Writes the number of records header to the file.
     */
    private
    void setVersion(RandomAccessFile file, int versionNumber) throws IOException {
        this.databaseVersion = versionNumber;

        FileLock lock = this.randomAccessFile.getChannel()
                                             .lock(VERSION_HEADER_LOCATION, 4, false);
        file.seek(VERSION_HEADER_LOCATION);
        file.writeInt(versionNumber);

        lock.release();
    }

    /**
     * Writes the number of records header to the file.
     */
    private
    void setRecordCount(RandomAccessFile file, int numberOfRecords) throws IOException {
        if (this.numberOfRecords != numberOfRecords) {
            this.numberOfRecords = numberOfRecords;

//            System.err.println("Set recordCount: " + numberOfRecords);

            FileLock lock = this.randomAccessFile.getChannel()
                                                 .lock(NUM_RECORDS_HEADER_LOCATION, 4, false);
            file.seek(NUM_RECORDS_HEADER_LOCATION);
            file.writeInt(numberOfRecords);

            lock.release();
        }
    }

    /**
     * Writes the data start position to the file.
     */
    private
    void setDataStartPosition(long dataPositionPointer) throws IOException {
        FileLock lock = this.randomAccessFile.getChannel()
                                             .lock(DATA_START_HEADER_LOCATION, 8, false);

//        System.err.println("Setting data position: " + dataPositionPointer);
        dataPosition = dataPositionPointer;

        randomAccessFile.seek(DATA_START_HEADER_LOCATION);
        randomAccessFile.writeLong(dataPositionPointer);

        lock.release();
    }

    int getVersion() {
        return this.databaseVersion;
    }

    void setVersion(int versionNumber) {
        try {
            setVersion(this.randomAccessFile, versionNumber);
        } catch (IOException e) {
            if (this.logger != null) {
                this.logger.error("Unable to set the version number", e);
            } else {
                e.printStackTrace();
            }
        }
    }


    /**
     * Returns the record to which the target file pointer belongs - meaning the specified location in the file is part
     * of the record data of the RecordHeader which is returned. Returns null if the location is not part of a record.
     * (O(n) mem accesses)
     */
    private
    Metadata index_getMetaDataFromData(long targetFp) {
        Iterator<Metadata> iterator = this.memoryIndex.values()
                                                      .iterator();

        //noinspection WhileLoopReplaceableByForEach
        while (iterator.hasNext()) {
            Metadata next = iterator.next();
            if (targetFp >= next.dataPointer && targetFp < next.dataPointer + next.dataCapacity) {
                return next;
            }
        }

        return null;
    }


    /**
     * Ensure index capacity. This operation makes sure the INDEX REGION is large enough to accommodate additional entries.
     */
    private
    void ensureIndexCapacity(RandomAccessFile file) throws IOException {
        // because we are zero indexed, this is ALSO the index where the record will START
        int numberOfRecords = this.numberOfRecords;

        // +1 because this is where that index will END (the start of the NEXT one)
        long endIndexPointer = Metadata.getMetaDataPointer(numberOfRecords + 1);

        // just set the data position to the end of the file, since we don't have any data yet.
        if (endIndexPointer > file.length() && numberOfRecords == 0) {
            file.setLength(endIndexPointer);
            setDataStartPosition(endIndexPointer);
            return;
        }

        // now we have to check, is there room for just 1 more entry?
        long readDataPosition = this.dataPosition;
        if (endIndexPointer < readDataPosition) {
            // we have room for this entry.
            return;
        }


        // otherwise, we have to grow our index.
        Metadata first;
        // "intelligent" move strategy.
        int newNumberOfRecords = getWeightedNewRecordCount(numberOfRecords);
        endIndexPointer = Metadata.getMetaDataPointer(newNumberOfRecords);


        // sometimes the endIndexPointer is in the middle of data, so we cannot move a record to where
        // data already exists, we have to move it to the end. Since we GUARANTEE that there is never "free space" at the
        // end of a file, this is ok
        if (endIndexPointer > file.length()) {
            // make sure we adjust the file size
            file.setLength(endIndexPointer);
        }
        else {
            endIndexPointer = file.length();
        }

        // we know that the start of the NEW data position has to be here.
        setDataStartPosition(endIndexPointer);


        long writeDataPosition = endIndexPointer;

        // if we only have ONE record left, and we move it to the end, then no reason to keep looking for records.
        while (endIndexPointer > readDataPosition && numberOfRecords > 0) {
            // this is the FIRST record that is in our data section
            first = index_getMetaDataFromData(readDataPosition);
            if (first == null) {
                //nothing is here, so keep checking
                readDataPosition += Metadata.INDEX_ENTRY_LENGTH;
                continue;
            }

//            System.err.println("\nMoving record: " + first.indexPosition + " -> " + writeDataPosition);
            first.moveData(file, writeDataPosition);

            int dataCapacity = first.dataCapacity;
            readDataPosition += dataCapacity;
            writeDataPosition += dataCapacity;
            numberOfRecords--;
        }
    }
}
