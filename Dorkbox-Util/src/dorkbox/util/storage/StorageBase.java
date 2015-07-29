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

import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Output;
import dorkbox.util.SerializationManager;
import dorkbox.util.bytes.ByteArrayWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.channels.FileLock;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;


// a note on file locking between c and java
// http://panks-dev.blogspot.de/2008/04/linux-file-locks-java-and-others.html
// Also, file locks on linux are ADVISORY. if an app doesn't care about locks, then it can do stuff -- even if locked by another app


@SuppressWarnings("unused")
public
class StorageBase {
    private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());


    // File pointer to the data start pointer header.
    private static final long VERSION_HEADER_LOCATION = 0;

    // File pointer to the num records header.
    private static final long NUM_RECORDS_HEADER_LOCATION = 4;

    // File pointer to the data start pointer header.
    private static final long DATA_START_HEADER_LOCATION = 8;

    // Total length in bytes of the global database headers.
    static final int FILE_HEADERS_REGION_LENGTH = 16;


    // The in-memory index (for efficiency, all of the record info is cached in memory).
    private final Map<ByteArrayWrapper, Metadata> memoryIndex;

    // determines how much the index will grow by
    private final Float weight;

    // The keys are weak! When they go, the map entry is removed!
    private final ReentrantLock referenceLock = new ReentrantLock();


    private final File baseFile;
    private final RandomAccessFile file;


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

    private final Deflater deflater;
    private final DeflaterOutputStream outputStream;

    private final Inflater inflater;
    private final InflaterInputStream inputStream;

    /**
     * Creates or opens a new database file.
     */
    StorageBase(File filePath, SerializationManager serializationManager) throws IOException {
        this.serializationManager = serializationManager;

        this.logger.info("Opening storage file: '{}'", filePath.getAbsolutePath());

        this.baseFile = filePath;

        File parentFile = this.baseFile.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                throw new IOException("Unable to create dirs for: " + filePath);
            }
        }

        this.file = new RandomAccessFile(this.baseFile, "rw");

        if (this.file.length() > FILE_HEADERS_REGION_LENGTH) {
            this.file.seek(VERSION_HEADER_LOCATION);
            this.databaseVersion = this.file.readInt();
            this.numberOfRecords = this.file.readInt();
            this.dataPosition = this.file.readLong();
        }
        else {
            setVersionNumber(this.file, 0);

            // always start off with 4 records
            setRecordCount(this.file, 4);

            long indexPointer = Metadata.getMetaDataPointer(4);
            setDataPosition(this.file, indexPointer);
            // have to make sure we can read header info (even if it's blank)
            this.file.setLength(indexPointer);
        }

        if (this.file.length() < this.dataPosition) {
            this.logger.error("Corrupted storage file!");
            throw new IllegalArgumentException("Unable to parse header information from storage. Maybe it's corrupted?");
        }

        //noinspection AutoBoxing
        this.logger.info("Storage version: {}", this.databaseVersion);

        this.deflater = new Deflater(7, true);
        this.outputStream = new DeflaterOutputStream(new FileOutputStream(this.file.getFD()), this.deflater, 65536, true);

        this.inflater = new Inflater(true);
        this.inputStream = new InflaterInputStream(new FileInputStream(this.file.getFD()), this.inflater, 65536);

        //noinspection AutoBoxing
        this.weight = .5F;
        this.memoryIndex = new ConcurrentHashMap<ByteArrayWrapper, Metadata>(this.numberOfRecords);

        Metadata meta;
        for (int index = 0; index < this.numberOfRecords; index++) {
            meta = Metadata.readHeader(this.file, index);
            if (meta == null) {
                // because we guarantee that empty metadata are AWLAYS at the end of the section, if we get a null one, break!
                break;
            }
            this.memoryIndex.put(meta.key, meta);
        }

        if (this.memoryIndex.size() != this.numberOfRecords) {
            setRecordCount(this.file, this.memoryIndex.size());
            this.logger.warn("Mismatch record count in storage, auto-correcting size.");
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
    boolean contains(ByteArrayWrapper key) {
        // protected by lock

        // check to see if it's in the pending ops
        return this.memoryIndex.containsKey(key);
    }

    /**
     * @return an object for a specified key ONLY FROM THE REFERENCE CACHE
     */
    final
    <T> T getCached(ByteArrayWrapper key) {
        // protected by lock

        Metadata meta = this.memoryIndex.get(key);
        if (meta == null) {
            return null;
        }

        // now stuff it into our reference cache so subsequent lookups are fast!
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
    <T> T get(ByteArrayWrapper key) {
        // NOT protected by lock

        Metadata meta = this.memoryIndex.get(key);
        if (meta == null) {
            return null;
        }

        // now get it from our reference cache so subsequent lookups are fast!
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
            // else, we have to load it from disk
            this.inflater.reset();
            this.file.seek(meta.dataPointer);

            T readRecordData = Metadata.readData(this.serializationManager, this.inputStream);

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
        } catch (KryoException e) {
            this.logger.error("Error while getting data from disk. Ignoring previous value.");
            return null;
        } catch (Exception e) {
            this.logger.error("Error while getting data from disk", e);
            return null;
        }
    }

    /**
     * Deletes a record
     *
     * @return true if the delete was successful. False if there were problems deleting the data.
     */
    final
    boolean delete(ByteArrayWrapper key) {
        // pending ops flushed (protected by lock)
        // not protected by lock
        Metadata delRec = this.memoryIndex.get(key);

        try {
            deleteRecordData(delRec, delRec.dataCapacity);
            deleteRecordIndex(key, delRec);
            return true;
        } catch (IOException e) {
            this.logger.error("Error while deleting data from disk", e);
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
        try {
            this.file.getFD()
                     .sync();
            this.file.close();
            this.memoryIndex.clear();

            this.inputStream.close();
        } catch (IOException e) {
            this.logger.error("Error while closing the file", e);
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
            return this.file.length();
        } catch (IOException e) {
            this.logger.error("Error getting file size for {}", this.baseFile.getAbsolutePath(), e);
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
    void save0(ByteArrayWrapper key, Object object, DeflaterOutputStream fileOutputStream, Deflater deflater) {
        deflater.reset();

        Metadata metaData = this.memoryIndex.get(key);
        int currentRecordCount = this.numberOfRecords;

        if (metaData != null) {
            // now we have to UPDATE instead of add!
            try {
                if (currentRecordCount == 1) {
                    // if we are the ONLY one, then we can do things differently.
                    // just dump the data again to disk.
                    FileLock lock = this.file.getChannel()
                                             .lock(this.dataPosition,
                                                   Long.MAX_VALUE - this.dataPosition,
                                                   false); // don't know how big it is, so max value it
                    this.file.seek(this.dataPosition); // this is the end of the file, we know this ahead-of-time
                    Metadata.writeDataFast(this.serializationManager, object, file, fileOutputStream);
                    // have to re-specify the capacity and size
                    int sizeOfWrittenData = (int) (this.file.length() - this.dataPosition);

                    metaData.dataCapacity = sizeOfWrittenData;
                    metaData.dataCount = sizeOfWrittenData;
                    lock.release();
                }
                else {
                    // this is comparatively slow, since we serialize it first to get the size, then we put it in the file.
                    ByteArrayOutputStream dataStream = getDataAsByteArray(this.serializationManager, this.logger, object, deflater);

                    int size = dataStream.size();
                    if (size > metaData.dataCapacity) {
                        deleteRecordData(metaData, size);
                        // stuff this record to the end of the file, since it won't fit in it's current location
                        metaData.dataPointer = this.file.length();
                        // have to make sure that the CAPACITY of the new one is the SIZE of the new data!
                        // and since it is going to the END of the file, we do that.
                        metaData.dataCapacity = size;
                        metaData.dataCount = 0;
                    }

                    // TODO: should check to see if the data is different. IF SO, then we write, otherwise nothing!

                    metaData.writeData(dataStream, this.file);
                }

                metaData.writeDataInfo(this.file);
            } catch (IOException e) {
                this.logger.error("Error while saving data to disk", e);
            }
        }
        else {
            try {
                // try to move the read head in order
                setRecordCount(this.file, currentRecordCount + 1);

                // This will make sure that there is room to write a new record. This is zero indexed.
                // this will skip around if moves occur
                ensureIndexCapacity(this.file);

                // append record to end of file
                long length = this.file.length();
                metaData = new Metadata(key, currentRecordCount, length);
                metaData.writeMetaDataInfo(this.file);

                // add new entry to the index
                this.memoryIndex.put(key, metaData);
                setRecordCount(this.file, currentRecordCount + 1);

                // save out the data. Because we KNOW that we are writing this to the end of the file,
                // there are some tricks we can use.

                FileLock lock = this.file.getChannel()
                                         .lock(length, Long.MAX_VALUE - length, false); // don't know how big it is, so max value it
                this.file.seek(length); // this is the end of the file, we know this ahead-of-time
                Metadata.writeDataFast(this.serializationManager, object, file, fileOutputStream);
                lock.release();

                metaData.dataCount = deflater.getTotalOut();
                metaData.dataCapacity = metaData.dataCount;
                // have to save it.
                metaData.writeDataInfo(this.file);
            } catch (IOException e) {
                this.logger.error("Error while writing data to disk", e);
                return;
            }
        }

        // put the object in the reference cache so we can read/get it later on
        metaData.objectReferenceCache = new WeakReference<Object>(object);
    }


    private static
    ByteArrayOutputStream getDataAsByteArray(SerializationManager serializationManager, Logger logger, Object data, Deflater deflater) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStream outputStream = new DeflaterOutputStream(byteArrayOutputStream, deflater);
        Output output = new Output(outputStream, 1024); // write 1024 at a time
        serializationManager.writeFullClassAndObject(logger, output, data);
        output.flush();

        outputStream.flush();
        outputStream.close();

        return byteArrayOutputStream;
    }

    void doActionThings(Map<ByteArrayWrapper, Object> actions) {
        DeflaterOutputStream outputStream2 = this.outputStream;
        Deflater deflater2 = this.deflater;

        // actions is thrown away after this invocation. GC can pick it up.
        // we are only interested in the LAST action that happened for some data.
        // items to be "autosaved" are automatically injected into "actions".
        for (Entry<ByteArrayWrapper, Object> entry : actions.entrySet()) {
            Object object = entry.getValue();
            ByteArrayWrapper key = entry.getKey();

            // our action list is for explicitly saving objects (but not necessarily "registering" them to be auto-saved
            save0(key, object, outputStream2, deflater2);
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
        //noinspection AutoUnboxing
        return numberOfRecords + 1 + (int) (numberOfRecords * this.weight);
    }


    private
    void deleteRecordData(Metadata deletedRecord, int sizeOfDataToAdd) throws IOException {
        if (this.file.length() == deletedRecord.dataPointer + deletedRecord.dataCapacity) {
            // shrink file since this is the last record in the file
            FileLock lock = this.file.getChannel()
                                     .lock(deletedRecord.dataPointer, Long.MAX_VALUE - deletedRecord.dataPointer, false);
            this.file.setLength(deletedRecord.dataPointer);
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
                    setDataPosition(this.file, newEndOfDataPointer);
                }
                else {
                    // option two is to grow the RECORD section, and put the data at the end of the file
                    setDataPosition(this.file, endOfDataPointer);
                }
            }
            else {
                Metadata previous = index_getMetaDataFromData(deletedRecord.dataPointer - 1);
                if (previous != null) {
                    // append space of deleted record onto previous record
                    previous.dataCapacity += deletedRecord.dataCapacity;
                    previous.writeDataInfo(this.file);
                }
                else {
                    // because there is no "previous", that means we MIGHT be the FIRST record
                    // well, we're not the first record. which one is RIGHT before us?
                    // it should be "previous", so something fucked up
                    this.logger.error("Trying to delete an object, and it's in a weird state");
                }
            }
        }
    }

    private
    void deleteRecordIndex(ByteArrayWrapper key, Metadata deleteRecord) throws IOException {
        int currentNumRecords = this.memoryIndex.size();

        if (deleteRecord.indexPosition != currentNumRecords - 1) {
            Metadata last = Metadata.readHeader(this.file, currentNumRecords - 1);
            assert last != null;

            last.move(this.file, deleteRecord.indexPosition);
        }

        this.memoryIndex.remove(key);

        setRecordCount(this.file, currentNumRecords - 1);
    }


    /**
     * Writes the number of records header to the file.
     */
    private
    void setVersionNumber(RandomAccessFile file, int versionNumber) throws IOException {
        this.databaseVersion = versionNumber;

        FileLock lock = this.file.getChannel()
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
        this.numberOfRecords = numberOfRecords;

        FileLock lock = this.file.getChannel()
                                 .lock(NUM_RECORDS_HEADER_LOCATION, 4, false);
        file.seek(NUM_RECORDS_HEADER_LOCATION);
        file.writeInt(numberOfRecords);
        lock.release();
    }

    /**
     * Writes the data start position to the file.
     */
    private
    void setDataPosition(RandomAccessFile file, long dataPositionPointer) throws IOException {
        this.dataPosition = dataPositionPointer;

        FileLock lock = this.file.getChannel()
                                 .lock(DATA_START_HEADER_LOCATION, 8, false);
        file.seek(DATA_START_HEADER_LOCATION);
        file.writeLong(dataPositionPointer);
        lock.release();
    }

    int getVersion() {
        return this.databaseVersion;
    }

    void setVersion(int versionNumber) {
        try {
            setVersionNumber(this.file, versionNumber);
        } catch (IOException e) {
            this.logger.error("Unable to set the version number", e);
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
        int numberOfRecords = this.numberOfRecords; // because we are zero indexed, this is ALSO the index where the record will START
        long endIndexPointer = Metadata.getMetaDataPointer(
                        numberOfRecords + 1); // +1 because this is where that index will END (the start of the NEXT one)

        // just set the data position to the end of the file, since we don't have any data yet.
        if (endIndexPointer > file.length() && numberOfRecords == 0) {
            file.setLength(endIndexPointer);
            setDataPosition(file, endIndexPointer);
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
        endIndexPointer = Math.max(endIndexPointer, file.length());

        // we know that the start of the NEW data position has to be here.
        setDataPosition(file, endIndexPointer);

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
