package dorkbox.util;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.OptionalDataException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import dorkbox.util.storage.Storage;

/**
 * Simple test class for the RecordsFile example. To run the test, set you CLASSPATH and then type
 * "java hamner.dbtest.TestRecords"
 */
public class StorageTest {

    private static final String TEST_DB = "sampleFile.records";

    static void log(String s) {
        System.err.println(s);
    }

    @Before
    public void deleteDB() {
        Storage.delete(new File(TEST_DB));
    }

    @After
    public void delete2DB() {
        Storage.delete(new File(TEST_DB));
    }


    @Test
    public void testCreateDB() throws IOException {
        Storage storage = Storage.open(TEST_DB);

        int numberOfRecords1 = storage.size();
        long size1 = storage.getFileSize();

        Assert.assertEquals("count is not correct", numberOfRecords1, 0);
        Assert.assertEquals("size is not correct", size1, 208L);  // NOTE this will change based on the data size added!

        Storage.close(storage);

        storage = Storage.open(TEST_DB);
        int numberOfRecords2 = storage.size();
        long size2 = storage.getFileSize();

        Assert.assertEquals("Record count is not the same", numberOfRecords1, numberOfRecords2);
        Assert.assertEquals("size is not the same", size1, size2);

        Storage.close(storage);
    }


  @Test
  public void testAddAsOne() throws IOException, ClassNotFoundException {
      int total = 100;

      try {
          Storage storage = Storage.open(TEST_DB);
          for (int i=0;i<total;i++) {
              add(storage, i);
          }

          Storage.close(storage);
          storage = Storage.open(TEST_DB);
          for (int i=0;i<total;i++) {
              String record1Data = createData(i);
              String readRecord = readRecord(storage, i);

              Assert.assertEquals("Object is not the same", record1Data, readRecord);
          }

          Storage.close(storage);
      } catch (Exception e) {
          e.printStackTrace();
          fail("Error!");
      }
  }

    @Test
    public void testAddNoKeyRecords() throws IOException, ClassNotFoundException {
        int total = 100;

        try {
            Storage storage = Storage.open(TEST_DB);
            for (int i=0;i<total;i++) {
                log("adding record " + i + "...");
                String addRecord = createData(i);
                storage.save(addRecord);

                log("reading record " + i + "...");
                String readData = storage.get();

                Assert.assertEquals("Object is not the same", addRecord, readData);
            }
            Storage.close(storage);

            storage = Storage.open(TEST_DB);

            String dataCheck = createData(total-1);
            log("reading record " + (total-1) + "...");
            String readData = storage.get();

            Assert.assertEquals("Object is not the same", dataCheck, readData);

            int numberOfRecords1 = storage.size();
            long size1 = storage.getFileSize();

            Assert.assertEquals("count is not correct", numberOfRecords1, 1);
            Assert.assertEquals("size is not correct", size1, 235L); // NOTE this will change based on the data size added!

            Storage.close(storage);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error!");
        }
    }

    @Test
    public void testAddRecords_DelaySaveA() throws IOException, ClassNotFoundException {
        int total = 100;

        try {
            Storage storage = Storage.open(TEST_DB);
            for (int i=0;i<total;i++) {
                add(storage, i);
            }

            synchronized (Thread.currentThread()) {
                Thread.currentThread().wait(storage.getSaveDelay() + 1000L);
            }

            for (int i=0;i<total;i++) {
                String record1Data = createData(i);
                String readRecord = readRecord(storage, i);

                Assert.assertEquals("Object is not the same", record1Data, readRecord);
            }

            Storage.close(storage);

            storage = Storage.open(TEST_DB);
            for (int i=0;i<total;i++) {
                String dataCheck = createData(i);
                String readRecord = readRecord(storage, i);

                Assert.assertEquals("Object is not the same", dataCheck, readRecord);
            }

            Storage.close(storage);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error!");
        }
    }

    @Test
    public void testAddRecords_DelaySaveB() throws IOException, ClassNotFoundException {
        int total = 100;

        try {
            Storage storage = Storage.open(TEST_DB);
            for (int i=0;i<total;i++) {
                add(storage, i);
            }

            for (int i=0;i<total;i++) {
                String record1Data = createData(i);
                String readRecord = readRecord(storage, i);

                Assert.assertEquals("Object is not the same", record1Data, readRecord);
            }

            Storage.close(storage);

            storage = Storage.open(TEST_DB);
            for (int i=0;i<total;i++) {
                String dataCheck = createData(i);
                String readRecord = readRecord(storage, i);

                Assert.assertEquals("Object is not the same", dataCheck, readRecord);
            }

            Storage.close(storage);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error!");
        }
    }

    @Test
    public void testLoadRecords() throws IOException, ClassNotFoundException {
        int total = 100;

        try {
            Storage storage = Storage.open(TEST_DB);
            for (int i=0;i<total;i++) {
                String addRecord = add(storage, i);
                String readRecord = readRecord(storage, i);

                Assert.assertEquals("Object is not the same", addRecord, readRecord);
            }
            Storage.close(storage);

            storage = Storage.open(TEST_DB);
            for (int i=0;i<total;i++) {
                String dataCheck = createData(i);
                String readRecord = readRecord(storage, i);

                Assert.assertEquals("Object is not the same", dataCheck, readRecord);
            }

            // now test loading data
            Data data = new Data();
            String createKey = createKey(63);
            makeData(data);

            storage.save(createKey, data);

            Data data2 = new Data();
            storage.load(createKey, data2);
            Assert.assertEquals("Object is not the same", data, data2);

            Storage.close(storage);
            storage = Storage.open(TEST_DB);

            data2 = new Data();
            storage.load(createKey, data2);
            Assert.assertEquals("Object is not the same", data, data2);

            Storage.close(storage);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error!");
        }
    }


    @Test
    public void testAddRecordsDelete1Record() throws IOException, ClassNotFoundException {
        int total = 100;

        try {
            Storage storage = Storage.open(TEST_DB);
            for (int i=0;i<total;i++) {
                String addRecord = add(storage, i);
                String readRecord = readRecord(storage, i);

                Assert.assertEquals("Object is not the same", addRecord, readRecord);
            }
            Storage.close(storage);

            storage = Storage.open(TEST_DB);
            for (int i=0;i<total;i++) {
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
                fail("record NOT successfully deleted.");
            }

            // now we add 3 back
            String addRecord = add(storage, 3);
            dataCheck = createData(3);

            Assert.assertEquals("Object is not the same", dataCheck, addRecord);

            Storage.close(storage);

            storage = Storage.open(TEST_DB);

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
            fail("Error!");
        }
    }

    @Test
    public void testUpdateRecords() throws IOException, ClassNotFoundException {
        int total = 100;

        try {
            Storage storage = Storage.open(TEST_DB);
            for (int i=0;i<total;i++) {
                String addRecord = add(storage, i);
                String readRecord = readRecord(storage, i);

                Assert.assertEquals("Object is not the same", addRecord, readRecord);
            }
            Storage.close(storage);

            storage = Storage.open(TEST_DB);

            String updateRecord = updateRecord(storage, 3, createData(3) + "new");
            String readRecord = readRecord(storage, 3);
            Assert.assertEquals("Object is not the same", updateRecord, readRecord);

            Storage.close(storage);
            storage = Storage.open(TEST_DB);

            readRecord = readRecord(storage, 3);
            Assert.assertEquals("Object is not the same", updateRecord, readRecord);

            updateRecord = updateRecord(storage, 3, createData(3));

            Storage.close(storage);
            storage = Storage.open(TEST_DB);

            readRecord = readRecord(storage, 3);
            Assert.assertEquals("Object is not the same", updateRecord, readRecord);

            Storage.close(storage);
            storage = Storage.open(TEST_DB);

            updateRecord = updateRecord(storage, 0, createData(0) + "new");
            readRecord = readRecord(storage, 0);
            Assert.assertEquals("Object is not the same", updateRecord, readRecord);

            Storage.close(storage);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error!");
        }
    }


    @Test
    public void testSaveAllRecords() throws IOException, ClassNotFoundException {
        int total = 100;

        try {
            Storage storage = Storage.open(TEST_DB);
            for (int i=0;i<total;i++) {
                Data data = new Data();
                makeData(data);
                String createKey = createKey(i);

                storage.register(createKey, data);
            }
            Storage.close(storage);

            Data data = new Data();
            makeData(data);

            storage = Storage.open(TEST_DB);
            for (int i=0;i<total;i++) {
                String createKey = createKey(i);

                Data data2 = new Data();
                storage.load(createKey, data2);
                Assert.assertEquals("Object is not the same", data, data2);
            }
            Storage.close(storage);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error!");
        }
    }





    private String createData(int number) {
        return number + " data for record # " + number;
    }

    public String add(Storage storage, int number) throws IOException {
        String record1Data = createData(number);
        String record1Key = createKey(number);

        log("adding record " + number + "...");
        storage.save(record1Key, record1Data);
        return record1Data;
    }

    public String readRecord(Storage storage, int number) throws OptionalDataException, ClassNotFoundException, IOException {
        String record1Key = createKey(number);

        log("reading record " + number + "...");

        String readData = storage.get(record1Key);
        log("\trecord " + number + " data: '" + readData + "'");
        return readData;
    }

    public void deleteRecord(Storage storage, int nNumber) throws OptionalDataException, ClassNotFoundException, IOException {
        String record1Key = createKey(nNumber);

        log("deleting record " + nNumber + "...");
        storage.delete(record1Key);
    }

    private String updateRecord(Storage storage, int number, String newData) throws IOException {
        String record1Key = createKey(number);

        log("updating record " + number + "...");
        storage.save(record1Key, newData);

        return newData;
    }

    private String createKey(int number) {
        return "foo" + number;
    }


    // from kryo unit test.
    private void makeData(Data data) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            buffer.append('a');
        }
        data.string = buffer.toString();

        data.strings = new String[] {"ab012", "", null, "!@#$", "�����"};
        data.ints = new int[] {-1234567, 1234567, -1, 0, 1, Integer.MAX_VALUE, Integer.MIN_VALUE};
        data.shorts = new short[] {-12345, 12345, -1, 0, 1, Short.MAX_VALUE, Short.MIN_VALUE};
        data.floats = new float[] {0, -0, 1, -1, 123456, -123456, 0.1f, 0.2f, -0.3f, (float)Math.PI, Float.MAX_VALUE,
            Float.MIN_VALUE};
        data.doubles = new double[] {0, -0, 1, -1, 123456, -123456, 0.1d, 0.2d, -0.3d, Math.PI, Double.MAX_VALUE, Double.MIN_VALUE};
        data.longs = new long[] {0, -0, 1, -1, 123456, -123456, 99999999999l, -99999999999l, Long.MAX_VALUE, Long.MIN_VALUE};
        data.bytes = new byte[] {-123, 123, -1, 0, 1, Byte.MAX_VALUE, Byte.MIN_VALUE};
        data.chars = new char[] {32345, 12345, 0, 1, 63, Character.MAX_VALUE, Character.MIN_VALUE};
        data.booleans = new boolean[] {true, false};
        data.Ints = new Integer[] {-1234567, 1234567, -1, 0, 1, Integer.MAX_VALUE, Integer.MIN_VALUE};
        data.Shorts = new Short[] {-12345, 12345, -1, 0, 1, Short.MAX_VALUE, Short.MIN_VALUE};
        data.Floats = new Float[] {0f, -0f, 1f, -1f, 123456f, -123456f, 0.1f, 0.2f, -0.3f, (float)Math.PI, Float.MAX_VALUE,
            Float.MIN_VALUE};
        data.Doubles = new Double[] {0d, -0d, 1d, -1d, 123456d, -123456d, 0.1d, 0.2d, -0.3d, Math.PI, Double.MAX_VALUE,
            Double.MIN_VALUE};
        data.Longs = new Long[] {0l, -0l, 1l, -1l, 123456l, -123456l, 99999999999l, -99999999999l, Long.MAX_VALUE, Long.MIN_VALUE};
        data.Bytes = new Byte[] {-123, 123, -1, 0, 1, Byte.MAX_VALUE, Byte.MIN_VALUE};
        data.Chars = new Character[] {32345, 12345, 0, 1, 63, Character.MAX_VALUE, Character.MIN_VALUE};
        data.Booleans = new Boolean[] {true, false};
    }

    public static class Data {
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

        public Data() {
        }

        @Override
        public int hashCode () {
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
        public boolean equals (Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Data other = (Data)obj;
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
            } else if (!this.string.equals(other.string)) {
                return false;
            }
            if (!Arrays.equals(this.strings, other.strings)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString () {
            return "Data";
        }
    }
}
