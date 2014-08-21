package dorkbox.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Nothing spectacular about this storage -- it allows for persistent storage of objects to disk.
 */
public class Storage {
    // TODO: add snappy compression to storage objects??

    private static final Logger logger = LoggerFactory.getLogger(Storage.class);

    private static Map<File, Storage> storages = new HashMap<File, Storage>(1);

    private final File file;

    private long milliSeconds = 3000L;
    private DelayTimer timer;
    private WeakReference<?> objectReference;

    private Kryo kryo;

    @SuppressWarnings({"rawtypes","unchecked"})
    public static Storage load(File file, Object loadIntoObject) {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null!");
        }

        if (loadIntoObject == null) {
            throw new IllegalArgumentException("loadIntoObject cannot be null!");
        }


        // if we load from a NEW storage at the same location as an ALREADY EXISTING storage,
        // without saving the existing storage first --- whoops!
        synchronized (storages) {
            Storage storage = storages.get(file);
            if (storage != null) {
                boolean waiting = storage.timer.isWaiting();
                if (waiting) {
                    storage.saveNow();
                }

                // why load it from disk again? just copy out the values!
                synchronized (storage) {
                    // have to load from disk!
                    Object source = storage.load(file, loadIntoObject.getClass());

                    Object orig = storage.objectReference.get();
                    if (orig != null) {
                        if (orig != loadIntoObject) {
                            storage.objectReference = new WeakReference(loadIntoObject);
                        }

                    } else {
                        // whoopie - the old one got GC'd! (for whatever reason, it can be legit)
                        storage.objectReference = new WeakReference(loadIntoObject);
                    }

                    if (source != null) {
                        copyFields(source, loadIntoObject);
                    }
                }
            } else {
                // this will load it from disk again, if necessary
                storage = new Storage(file, loadIntoObject);
                storages.put(file, storage);

                // have to load from disk!
                Object source = storage.load(file, loadIntoObject.getClass());
                if (source != null) {
                    copyFields(source, loadIntoObject);
                }
            }
            return storage;
        }
    }


    /**
     * Also loads the saved object into the passed-in object. This is sorta slow (nothing is cached for speed!)
     *
     * If the saved object has more fields than the loadIntoObject, only the fields in loadIntoObject will be
     * populated. If the loadIntoObject has more fields than the saved object, then the loadIntoObject will not
     * have those fields changed.
     */
    @SuppressWarnings({"rawtypes","unchecked"})
    private Storage(File file, Object loadIntoObject) {
        this.file = file.getAbsoluteFile();
        File parentFile = this.file.getParentFile();
        if (parentFile != null) {
            parentFile.mkdirs();
        }

        this.kryo = new Kryo();
        this.kryo.setRegistrationRequired(false);

        this.objectReference = new WeakReference(loadIntoObject);
        this.timer = new DelayTimer("Storage Writer", new DelayTimer.Callback() {
            @Override
            public void execute() {
                save0();
            }
        });
    }

    /**
     * Loads the saved object into the passed-in object. This is sorta slow (nothing is cached for speed!)
     *
     * If the saved object has more fields than the loadIntoObject, only the fields in loadIntoObject will be
     * populated. If the loadIntoObject has more fields than the saved object, then the loadIntoObject will not
     * have those fields changed.
     */
    public final void load(Object loadIntoObject) {
        if (loadIntoObject == null) {
            throw new IllegalArgumentException("loadIntoObject cannot be null!");
        }


        // if we load from a NEW storage at the same location as an ALREADY EXISTING storage,
        // without saving the existing storage first --- whoops!
        synchronized (storages) {
            File file2 = this.file;

            Storage storage = storages.get(file2);
            Object source = null;
            if (storage != null) {
                boolean waiting = storage.timer.isWaiting();
                if (waiting) {
                    storage.saveNow();
                }

                // why load it from disk again? just copy out the values!
                source = storage.objectReference.get();
                if (source == null) {
                    // have to load from disk!
                    source = load(file2, loadIntoObject.getClass());
                }
            }

            if (source != null) {
                copyFields(source, loadIntoObject);
            }
        }
    }

    /**
     * @param delay milliseconds to wait
     */
    public final void setSaveDelay(long milliSeconds) {
        this.milliSeconds = milliSeconds;
    }

    /**
     * Immediately save the storage to disk
     */
    public final synchronized void saveNow() {
        this.timer.delay(0L);
    }

    /**
     * Save the storage to disk, once xxxx milli-seconds have passed.
     * This is to help prevent thrashing the disk, or wearing it out on multiple, rapid, changes.
     */
    public final synchronized void save() {
        this.timer.delay(this.milliSeconds);
    }

    private synchronized void save0() {
        Object object = Storage.this.objectReference.get();

        if (object == null) {
            Storage.logger.error("Object has been erased and is no longer available to save!");
            return;
        }

        Class<? extends Object> class1 = object.getClass();

        RandomAccessFile raf = null;
        Output output = null;
        try {
            raf = new RandomAccessFile(this.file, "rw");
            OutputStream outputStream = new DeflaterOutputStream(new FileOutputStream(raf.getFD()));
            output = new Output(outputStream, 1024); // write 1024 at a time

            this.kryo.writeObject(output, object);
            output.flush();

            load(this.file, class1);

        } catch (Exception e) {
            Storage.logger.error("Error saving the data!", e);
        } finally {
            if (output != null) {
                output.close();
            }
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized <T> T load(File file, Class<? extends Object> clazz) {
        if (file.length() == 0) {
            return null;
        }

        RandomAccessFile raf = null;
        Input input = null;
        try {
            raf = new RandomAccessFile(file, "r");
            input = new Input(new InflaterInputStream(new FileInputStream(raf.getFD())), 1024); // read 1024 at a time

            Object readObject = this.kryo.readObject(input, clazz);
            return (T) readObject;
        } catch (Exception e) {
            logger.error("Error reading from '{}'! Perhaps the file is corrupt?", file.getAbsolutePath());
            return null;
        } finally {
            if (input != null) {
                input.close();
            }
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.file == null ? 0 : this.file.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Storage other = (Storage) obj;
        if (this.file == null) {
            if (other.file != null) {
                return false;
            }
        } else if (!this.file.equals(other.file)) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return "Storage [" + this.file + "]";
    }


    private static void copyFields(Object source, Object dest) {
        Class<? extends Object> sourceClass = source.getClass();
        Field[] destFields = dest.getClass().getDeclaredFields();

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
    }

    public static void shutdown() {
        synchronized(storages) {
            storages.clear();
        }
    }
}
