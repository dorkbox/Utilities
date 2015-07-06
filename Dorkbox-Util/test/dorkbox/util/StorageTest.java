package dorkbox.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import dorkbox.util.storage.DiskStorageIfface;
import dorkbox.util.storage.MakeStorage;
import io.netty.buffer.ByteBuf;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public
class StorageTest {

    private static final File TEST_DB = new File("sampleFile.records");
    private static final SerializationManager manager = new SerializationManager() {
        Kryo kryo = new Kryo();

        @Override
        public
        boolean setReferences(final boolean references) {
            kryo.setReferences(references);
            return false;
        }

        @Override
        public
        void setRegistrationRequired(final boolean registrationRequired) {
            kryo.setRegistrationRequired(registrationRequired);
        }

        @Override
        public
        void register(final Class<?> clazz) {
            kryo.register(clazz);
        }

        @Override
        public
        void register(final Class<?> clazz, final Serializer<?> serializer) {
            kryo.register(clazz, serializer);
        }

        @Override
        public
        Registration register(final Class<?> type, final Serializer<?> serializer, final int id) {
            kryo.register(type, serializer, id);
            return null;
        }

        @Override
        public
        void write(final ByteBuf buffer, final Object message) {
            final Output output = new Output();
            writeClassAndObject(output, message);
            buffer.writeBytes(output.getBuffer());
        }

        @Override
        public
        Object read(final ByteBuf buffer, final int length) {
            final Input input = new Input();
            buffer.readBytes(input.getBuffer());

            final Object o = readClassAndObject(input);
            buffer.skipBytes(input.position());

            return o;
        }

        @Override
        public
        void writeClassAndObject(final Output output, final Object value) {
            kryo.writeClassAndObject(output, value);
        }

        @Override
        public
        Object readClassAndObject(final Input input) {
            return kryo.readClassAndObject(input);
        }
    };

    static
    void log(String s) {
        System.err.println(s);
    }

    @Before
    public
    void deleteDB() {
        MakeStorage.delete(TEST_DB);
    }

    @After
    public
    void delete2DB() {
        MakeStorage.delete(TEST_DB);
    }


    @Test
    public
    void testCreateDB() throws IOException {
        DiskStorageIfface storage = MakeStorage.Disk()
                                         .file(TEST_DB)
                                         .serializer(manager)
                                         .make();

        int numberOfRecords1 = storage.size();
        long size1 = storage.getFileSize();

        Assert.assertEquals("count is not correct", numberOfRecords1, 0);
        Assert.assertEquals("size is not correct", size1, 208L);  // NOTE this will change based on the data size added!

        MakeStorage.close(storage);

        storage = MakeStorage.Disk()
                             .file(TEST_DB)
                             .serializer(manager)
                             .make();
        int numberOfRecords2 = storage.size();
        long size2 = storage.getFileSize();

        Assert.assertEquals("Record count is not the same", numberOfRecords1, numberOfRecords2);
        Assert.assertEquals("size is not the same", size1, size2);

        MakeStorage.close(storage);
    }


    @Test
    public
    void testAddAsOne() throws IOException, ClassNotFoundException {
        int total = 100;

        try {
            DiskStorageIfface storage = MakeStorage.Disk()
                                             .file(TEST_DB)
                                             .serializer(manager)
                                             .make();
            for (int i = 0; i < total; i++) {
                add(storage, i);
            }

            MakeStorage.close(storage);
            storage = MakeStorage.Disk()
                                 .file(TEST_DB)
                                 .serializer(manager)
                                 .make();
            for (int i = 0; i < total; i++) {
                String record1Data = createData(i);
                String readRecord = readRecord(storage, i);

                Assert.assertEquals("Object is not the same", record1Data, readRecord);
            }

            MakeStorage.close(storage);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Error!");
        }
    }

    @Test
    public
    void testAddNoKeyRecords() throws IOException, ClassNotFoundException {
        int total = 100;

        try {
            DiskStorageIfface storage = MakeStorage.Disk()
                                             .file(TEST_DB)
                                             .serializer(manager)
                                             .make();
            for (int i = 0; i < total; i++) {
                log("adding record " + i + "...");
                String addRecord = createData(i);
                storage.put(addRecord);

                log("reading record " + i + "...");
                String readData = storage.get();

                Assert.assertEquals("Object is not the same", addRecord, readData);
            }
            MakeStorage.close(storage);

            storage = MakeStorage.Disk()
                                 .file(TEST_DB)
                                 .serializer(manager)
                                 .make();

            String dataCheck = createData(total - 1);
            log("reading record " + (total - 1) + "...");
            String readData = storage.get();

            Assert.assertEquals("Object is not the same", dataCheck, readData);

            int numberOfRecords1 = storage.size();
            long size1 = storage.getFileSize();

            Assert.assertEquals("count is not correct", numberOfRecords1, 1);
            Assert.assertEquals("size is not correct", size1, 235L); // NOTE this will change based on the data size added!

            MakeStorage.close(storage);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Error!");
        }
    }

    @Test
    public
    void testAddRecords_DelaySaveA() throws IOException, ClassNotFoundException {
        int total = 100;

        try {
            DiskStorageIfface storage = MakeStorage.Disk()
                                             .file(TEST_DB)
                                             .serializer(manager)
                                             .make();
            for (int i = 0; i < total; i++) {
                add(storage, i);
            }

            synchronized (Thread.currentThread()) {
                Thread.currentThread()
                      .wait(storage.getSaveDelay() + 1000L);
            }

            for (int i = 0; i < total; i++) {
                String record1Data = createData(i);
                String readRecord = readRecord(storage, i);

                Assert.assertEquals("Object is not the same", record1Data, readRecord);
            }

            MakeStorage.close(storage);

            storage = MakeStorage.Disk()
                                 .file(TEST_DB)
                                 .serializer(manager)
                                 .make();
            for (int i = 0; i < total; i++) {
                String dataCheck = createData(i);
                String readRecord = readRecord(storage, i);

                Assert.assertEquals("Object is not the same", dataCheck, readRecord);
            }

            MakeStorage.close(storage);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Error!");
        }
    }

    @Test
    public
    void testAddRecords_DelaySaveB() throws IOException, ClassNotFoundException {
        int total = 100;

        try {
            DiskStorageIfface storage = MakeStorage.Disk()
                                             .file(TEST_DB)
                                             .serializer(manager)
                                             .make();
            for (int i = 0; i < total; i++) {
                add(storage, i);
            }

            for (int i = 0; i < total; i++) {
                String record1Data = createData(i);
                String readRecord = readRecord(storage, i);

                Assert.assertEquals("Object is not the same", record1Data, readRecord);
            }

            MakeStorage.close(storage);

            storage = MakeStorage.Disk()
                                 .file(TEST_DB)
                                 .serializer(manager)
                                 .make();
            for (int i = 0; i < total; i++) {
                String dataCheck = createData(i);
                String readRecord = readRecord(storage, i);

                Assert.assertEquals("Object is not the same", dataCheck, readRecord);
            }

            MakeStorage.close(storage);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Error!");
        }
    }

    @Test
    public
    void testLoadRecords() throws IOException, ClassNotFoundException {
        int total = 100;

        try {
            DiskStorageIfface storage = MakeStorage.Disk()
                                             .file(TEST_DB)
                                             .serializer(manager)
                                             .make();
            for (int i = 0; i < total; i++) {
                String addRecord = add(storage, i);
                String readRecord = readRecord(storage, i);

                Assert.assertEquals("Object is not the same", addRecord, readRecord);
            }
            MakeStorage.close(storage);

            storage = MakeStorage.Disk()
                                 .file(TEST_DB)
                                 .serializer(manager)
                                 .make();
            for (int i = 0; i < total; i++) {
                String dataCheck = createData(i);
                String readRecord = readRecord(storage, i);

                Assert.assertEquals("Object is not the same", dataCheck, readRecord);
            }

            // now test loading data
            Data data = new Data();
            String createKey = createKey(63);
            makeData(data);

            storage.put(createKey, data);

            Data data2 = new Data();
            storage.load(createKey, data2);
            Assert.assertEquals("Object is not the same", data, data2);

            MakeStorage.close(storage);
            storage = MakeStorage.Disk()
                                 .file(TEST_DB)
                                 .serializer(manager)
                                 .make();

            data2 = new Data();
            storage.load(createKey, data2);
            Assert.assertEquals("Object is not the same", data, data2);

            MakeStorage.close(storage);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Error!");
        }
    }


    @Test
    public
    void testAddRecordsDelete1Record() throws IOException, ClassNotFoundException {
        int total = 100;

        try {
            DiskStorageIfface storage = MakeStorage.Disk()
                                             .file(TEST_DB)
                                             .serializer(manager)
                                             .make();
            for (int i = 0; i < total; i++) {
                String addRecord = add(storage, i);
                String readRecord = readRecord(storage, i);

                Assert.assertEquals("Object is not the same", addRecord, readRecord);
            }
            MakeStorage.close(storage);

            storage = MakeStorage.Disk()
                                 .file(TEST_DB)
                                 .serializer(manager)
                                 .make();
            for (int i = 0; i < total; i++) {
                String dataCheck = createData(i);
                String readRecord = readRecord(storage, i);

                Assert.assertEquals("Object is not the same", dataCheck, readRecord);
            }

            // make sure now that we can delete one of the records.
            deleteRecord(storage, 3);

            String readRecord = readRecord(storage, 9);
            String dataCheck = createData(9);

            Assert.assertEquals("Object is not the same", dataCheck, readRecord);


            if (storage.contains(createKey(3))) {
                Assert.fail("record NOT successfully deleted.");
            }

            // now we add 3 back
            String addRecord = add(storage, 3);
            dataCheck = createData(3);

            Assert.assertEquals("Object is not the same", dataCheck, addRecord);

            MakeStorage.close(storage);

            storage = MakeStorage.Disk()
                                 .file(TEST_DB)
                                 .serializer(manager)
                                 .make();

            // check 9 again
            readRecord = readRecord(storage, 9);
            dataCheck = createData(9);
            Assert.assertEquals("Object is not the same", dataCheck, readRecord);

            // check 3 again
            readRecord = readRecord(storage, 3);
            dataCheck = createData(3);
            Assert.assertEquals("Object is not the same", dataCheck, readRecord);

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Error!");
        }
    }

    @Test
    public
    void testUpdateRecords() throws IOException, ClassNotFoundException {
        int total = 100;

        try {
            DiskStorageIfface storage = MakeStorage.Disk()
                                             .file(TEST_DB)
                                             .serializer(manager)
                                             .make();
            for (int i = 0; i < total; i++) {
                String addRecord = add(storage, i);
                String readRecord = readRecord(storage, i);

                Assert.assertEquals("Object is not the same", addRecord, readRecord);
            }
            MakeStorage.close(storage);

            storage = MakeStorage.Disk()
                                 .file(TEST_DB)
                                 .serializer(manager)
                                 .make();

            String updateRecord = updateRecord(storage, 3, createData(3) + "new");
            String readRecord = readRecord(storage, 3);
            Assert.assertEquals("Object is not the same", updateRecord, readRecord);

            MakeStorage.close(storage);
            storage = MakeStorage.Disk()
                                 .file(TEST_DB)
                                 .serializer(manager)
                                 .make();

            readRecord = readRecord(storage, 3);
            Assert.assertEquals("Object is not the same", updateRecord, readRecord);

            updateRecord = updateRecord(storage, 3, createData(3));

            MakeStorage.close(storage);
            storage = MakeStorage.Disk()
                                 .file(TEST_DB)
                                 .serializer(manager)
                                 .make();

            readRecord = readRecord(storage, 3);
            Assert.assertEquals("Object is not the same", updateRecord, readRecord);

            MakeStorage.close(storage);
            storage = MakeStorage.Disk()
                                 .file(TEST_DB)
                                 .serializer(manager)
                                 .make();

            updateRecord = updateRecord(storage, 0, createData(0) + "new");
            readRecord = readRecord(storage, 0);
            Assert.assertEquals("Object is not the same", updateRecord, readRecord);

            MakeStorage.close(storage);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Error!");
        }
    }


    @Test
    public
    void testSaveAllRecords() throws IOException, ClassNotFoundException {
        int total = 100;

        try {
            DiskStorageIfface storage = MakeStorage.Disk()
                                             .file(TEST_DB)
                                             .serializer(manager)
                                             .make();
            for (int i = 0; i < total; i++) {
                Data data = new Data();
                makeData(data);
                String createKey = createKey(i);

                storage.put(createKey, data);
            }
            MakeStorage.close(storage);

            Data data = new Data();
            makeData(data);

            storage = MakeStorage.Disk()
                                 .file(TEST_DB)
                                 .serializer(manager)
                                 .make();
            for (int i = 0; i < total; i++) {
                String createKey = createKey(i);

                Data data2 = new Data();
                storage.load(createKey, data2);
                Assert.assertEquals("Object is not the same", data, data2);
            }
            MakeStorage.close(storage);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Error!");
        }
    }



    private static
    String createData(int number) {
        return number + " data for record # " + number;
    }

    public static
    String add(DiskStorageIfface storage, int number) throws IOException {
        String record1Data = createData(number);
        String record1Key = createKey(number);

        log("adding record " + number + "...");
        storage.put(record1Key, record1Data);
        return record1Data;
    }

    public static
    String readRecord(DiskStorageIfface storage, int number) throws ClassNotFoundException, IOException {
        String record1Key = createKey(number);

        log("reading record " + number + "...");

        String readData = storage.get(record1Key);
        log("\trecord " + number + " data: '" + readData + "'");
        return readData;
    }

    public static
    void deleteRecord(DiskStorageIfface storage, int nNumber) throws ClassNotFoundException, IOException {
        String record1Key = createKey(nNumber);

        log("deleting record " + nNumber + "...");
        storage.delete(record1Key);
    }

    private static
    String updateRecord(DiskStorageIfface storage, int number, String newData) throws IOException {
        String record1Key = createKey(number);

        log("updating record " + number + "...");
        storage.put(record1Key, newData);

        return newData;
    }

    private static
    String createKey(int number) {
        return "foo" + number;
    }


    // from kryo unit test.
    private static
    void makeData(Data data) {
        StringBuilder buffer = new StringBuilder(128);
        for (int i = 0; i < 3; i++) {
            buffer.append('a');
        }
        data.string = buffer.toString();

        data.strings = new String[] {"ab012", "", null, "!@#$", "�����"};
        data.ints = new int[] {-1234567, 1234567, -1, 0, 1, Integer.MAX_VALUE, Integer.MIN_VALUE};
        data.shorts = new short[] {-12345, 12345, -1, 0, 1, Short.MAX_VALUE, Short.MIN_VALUE};
        data.floats = new float[] {0, -0, 1, -1, 123456, -123456, 0.1f, 0.2f, -0.3f, (float) Math.PI, Float.MAX_VALUE, Float.MIN_VALUE};
        data.doubles = new double[] {0, -0, 1, -1, 123456, -123456, 0.1d, 0.2d, -0.3d, Math.PI, Double.MAX_VALUE, Double.MIN_VALUE};
        data.longs = new long[] {0, -0, 1, -1, 123456, -123456, 99999999999l, -99999999999l, Long.MAX_VALUE, Long.MIN_VALUE};
        data.bytes = new byte[] {-123, 123, -1, 0, 1, Byte.MAX_VALUE, Byte.MIN_VALUE};
        data.chars = new char[] {32345, 12345, 0, 1, 63, Character.MAX_VALUE, Character.MIN_VALUE};
        data.booleans = new boolean[] {true, false};
        data.Ints = new Integer[] {-1234567, 1234567, -1, 0, 1, Integer.MAX_VALUE, Integer.MIN_VALUE};
        data.Shorts = new Short[] {-12345, 12345, -1, 0, 1, Short.MAX_VALUE, Short.MIN_VALUE};
        data.Floats = new Float[] {0f, -0f, 1f, -1f, 123456f, -123456f, 0.1f, 0.2f, -0.3f, (float) Math.PI, Float.MAX_VALUE,
                                   Float.MIN_VALUE};
        data.Doubles = new Double[] {0d, -0d, 1d, -1d, 123456d, -123456d, 0.1d, 0.2d, -0.3d, Math.PI, Double.MAX_VALUE, Double.MIN_VALUE};
        data.Longs = new Long[] {0l, -0l, 1l, -1l, 123456l, -123456l, 99999999999l, -99999999999l, Long.MAX_VALUE, Long.MIN_VALUE};
        data.Bytes = new Byte[] {-123, 123, -1, 0, 1, Byte.MAX_VALUE, Byte.MIN_VALUE};
        data.Chars = new Character[] {32345, 12345, 0, 1, 63, Character.MAX_VALUE, Character.MIN_VALUE};
        data.Booleans = new Boolean[] {true, false};
    }

    public static
    class Data {
        public String string;
        public String[] strings;
        public int[] ints;
        public short[] shorts;
        public float[] floats;
        public double[] doubles;
        public long[] longs;
        public byte[] bytes;
        public char[] chars;
        public boolean[] booleans;
        public Integer[] Ints;
        public Short[] Shorts;
        public Float[] Floats;
        public Double[] Doubles;
        public Long[] Longs;
        public Byte[] Bytes;
        public Character[] Chars;
        public Boolean[] Booleans;

        public
        Data() {
        }

        @Override
        public
        int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(this.Booleans);
            result = prime * result + Arrays.hashCode(this.Bytes);
            result = prime * result + Arrays.hashCode(this.Chars);
            result = prime * result + Arrays.hashCode(this.Doubles);
            result = prime * result + Arrays.hashCode(this.Floats);
            result = prime * result + Arrays.hashCode(this.Ints);
            result = prime * result + Arrays.hashCode(this.Longs);
            result = prime * result + Arrays.hashCode(this.Shorts);
            result = prime * result + Arrays.hashCode(this.booleans);
            result = prime * result + Arrays.hashCode(this.bytes);
            result = prime * result + Arrays.hashCode(this.chars);
            result = prime * result + Arrays.hashCode(this.doubles);
            result = prime * result + Arrays.hashCode(this.floats);
            result = prime * result + Arrays.hashCode(this.ints);
            result = prime * result + Arrays.hashCode(this.longs);
            result = prime * result + Arrays.hashCode(this.shorts);
            result = prime * result + (this.string == null ? 0 : this.string.hashCode());
            result = prime * result + Arrays.hashCode(this.strings);
            return result;
        }

        @Override
        public
        boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Data other = (Data) obj;
            if (!Arrays.equals(this.Booleans, other.Booleans)) {
                return false;
            }
            if (!Arrays.equals(this.Bytes, other.Bytes)) {
                return false;
            }
            if (!Arrays.equals(this.Chars, other.Chars)) {
                return false;
            }
            if (!Arrays.equals(this.Doubles, other.Doubles)) {
                return false;
            }
            if (!Arrays.equals(this.Floats, other.Floats)) {
                return false;
            }
            if (!Arrays.equals(this.Ints, other.Ints)) {
                return false;
            }
            if (!Arrays.equals(this.Longs, other.Longs)) {
                return false;
            }
            if (!Arrays.equals(this.Shorts, other.Shorts)) {
                return false;
            }
            if (!Arrays.equals(this.booleans, other.booleans)) {
                return false;
            }
            if (!Arrays.equals(this.bytes, other.bytes)) {
                return false;
            }
            if (!Arrays.equals(this.chars, other.chars)) {
                return false;
            }
            if (!Arrays.equals(this.doubles, other.doubles)) {
                return false;
            }
            if (!Arrays.equals(this.floats, other.floats)) {
                return false;
            }
            if (!Arrays.equals(this.ints, other.ints)) {
                return false;
            }
            if (!Arrays.equals(this.longs, other.longs)) {
                return false;
            }
            if (!Arrays.equals(this.shorts, other.shorts)) {
                return false;
            }
            if (this.string == null) {
                if (other.string != null) {
                    return false;
                }
            }
            else if (!this.string.equals(other.string)) {
                return false;
            }
            if (!Arrays.equals(this.strings, other.strings)) {
                return false;
            }
            return true;
        }

        @Override
        public
        String toString() {
            return "Data";
        }
    }
}
