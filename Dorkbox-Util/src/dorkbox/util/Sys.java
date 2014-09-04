package dorkbox.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bouncycastle.crypto.digests.SHA256Digest;

import dorkbox.urlHandler.BoxURLConnection;

public class Sys {
    public static final int     KILOBYTE = 1024;
    public static final int     MEGABYTE = 1024 * KILOBYTE;
    public static final int     GIGABYTE = 1024 * MEGABYTE;
    public static final long    TERABYTE = 1024L * GIGABYTE;

    public static char[] HEX_CHARS = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static final char[] convertStringToChars(String string) {
        char[] charArray = string.toCharArray();

        eraseString(string);

        return charArray;
    }


    public static final void eraseString(String string) {
//      You can change the value of the inner char[] using reflection.
//
//      You must be careful to either change it with an array of the same length,
//      or to also update the count field.
//
//      If you want to be able to use it as an entry in a set or as a value in map,
//      you will need to recalculate the hash code and set the value of the hashCode field.

        try {
          Field valueField = String.class.getDeclaredField("value");
          valueField.setAccessible(true);
          char[] chars  = (char[]) valueField.get(string);
          Arrays.fill(chars, '*');  // asterisk it out in case of GC not picking up the old char array.

          valueField.set(string, new char[0]); // replace it.

          // set count to 0
          Field countField = String.class.getDeclaredField("count");
          countField.setAccessible(true);
          countField.set(string, 0);

          // set hash to 0
          Field hashField = String.class.getDeclaredField("hash");
          hashField.setAccessible(true);
          hashField.set(string, 0);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static String getSizePretty(final long size) {
        if (size > TERABYTE) {
            return String.format("%2.2fTB", (float) size / TERABYTE);
        }
        if (size > GIGABYTE) {
            return String.format("%2.2fGB", (float) size / GIGABYTE);
        }
        if (size > MEGABYTE) {
            return String.format("%2.2fMB", (float) size / MEGABYTE);
        }
        if (size > KILOBYTE) {
            return String.format("%2.2fKB", (float) size / KILOBYTE);
        }

        return String.valueOf(size) + "B";
      }

    /**
     * Convenient close for a stream.
     */
    public static void close(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException ioe) {
                System.err.println("Error closing the input stream:" + inputStream);
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Convenient close for a stream.
     */
    public static void close(OutputStream outputStream) {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException ioe) {
                System.err.println("Error closing the output stream:" + outputStream);
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Convenient close for a Reader.
     */
    public static void close(Reader inputReader) {
        if (inputReader != null) {
            try {
                inputReader.close();
            } catch (IOException ioe) {
                System.err.println("Error closing input reader: " + inputReader);
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Convenient close for a Writer.
     */
    public static void close(Writer outputWriter) {
        if (outputWriter != null) {
            try {
                outputWriter.close();
            } catch (IOException ioe) {
                System.err.println("Error closing output writer: " + outputWriter);
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Copy the contents of the input stream to the output stream.
     * <p>
     * DOES NOT CLOSE THE STEAMS!
     */
    public static <T extends OutputStream> T copyStream(InputStream inputStream, T outputStream) throws IOException {
        byte[] buffer = new byte[4096];
        int read = 0;
        while ((read = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, read);
        }

        return outputStream;
    }

    /**
     * Convert the contents of the input stream to a byte array.
     */
    public static byte[] getBytesFromStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);

        byte[] buffer = new byte[4096];
        int read = 0;
        while ((read = inputStream.read(buffer)) > 0) {
            baos.write(buffer, 0, read);
        }
        baos.flush();
        inputStream.close();

        return baos.toByteArray();
    }

    public static final byte[] arrayCloneBytes(byte[] src) {
        return arrayCloneBytes(src, 0);
    }

    public static final byte[] arrayCloneBytes(byte[] src, int position) {
        int length = src.length - position;

        byte[] b = new byte[length];
        System.arraycopy(src, position, b, 0, length);
        return b;
    }

    public static final byte[] concatBytes(byte[]... arrayBytes) {
        int length = 0;
        for (byte[] bytes : arrayBytes) {
            length += bytes.length;
        }

        byte[] concatBytes = new byte[length];

        length = 0;
        for (byte[] bytes : arrayBytes) {
            System.arraycopy(bytes, 0, concatBytes, length, bytes.length);
            length += bytes.length;
        }

        return concatBytes;
    }

    /** gets the SHA256 hash + SALT of the specified username, as UTF-16 */
    public static final byte[] getSha256WithSalt(String username,  byte[] saltBytes) {
        if (username == null) {
            return null;
        }

        byte[] charToBytes = Sys.charToBytes(username.toCharArray());
        byte[] userNameWithSalt = Sys.concatBytes(charToBytes, saltBytes);


        SHA256Digest sha256 = new SHA256Digest();
        byte[] usernameHashBytes = new byte[sha256.getDigestSize()];
        sha256.update(userNameWithSalt, 0, userNameWithSalt.length);
        sha256.doFinal(usernameHashBytes, 0);

        return usernameHashBytes;
    }

    /** gets the SHA256 hash of the specified string, as UTF-16 */
    public static final byte[] getSha256(String string) {
        byte[] charToBytes = Sys.charToBytes(string.toCharArray());

        SHA256Digest sha256 = new SHA256Digest();
        byte[] usernameHashBytes = new byte[sha256.getDigestSize()];
        sha256.update(charToBytes, 0, charToBytes.length);
        sha256.doFinal(usernameHashBytes, 0);

        return usernameHashBytes;
    }

    /** gets the SHA256 hash of the specified byte array */
    public static final byte[] getSha256(byte[] bytes) {

        SHA256Digest sha256 = new SHA256Digest();
        byte[] hashBytes = new byte[sha256.getDigestSize()];
        sha256.update(bytes, 0, bytes.length);
        sha256.doFinal(hashBytes, 0);

        return hashBytes;
    }

    /** this saves the char array in UTF-16 format of bytes */
    public static final byte[] charToBytes(char[] text) {
        // NOTE: this saves the char array in UTF-16 format of bytes.
        byte[] bytes = new byte[text.length*2];
        for(int i=0; i<text.length; i++) {
            bytes[2*i] = (byte) ((text[i] & 0xFF00)>>8);
            bytes[2*i+1] = (byte) (text[i] & 0x00FF);
        }

        return bytes;
    }


    public static final byte[] intsToBytes(int[] ints) {
        int length = ints.length;
        byte[] bytes = new byte[length];

        for (int i = 0; i < length; i++) {
            int intValue = ints[i];
            if (intValue < 0 || intValue > 255) {
                System.err.println("WARNING: int at index " + i + "(" + intValue + ") was not a valid byte value (0-255)");
                return new byte[length];
            }

            bytes[i] = (byte)intValue;
        }

        return bytes;
    }

    public static final int[] bytesToInts(byte[] bytes) {
        int length = bytes.length;
        int[] ints = new int[length];

        for (int i = 0; i < length; i++) {
            ints[i] = bytes[i] & 0xFF;
        }

        return ints;
    }

    public static final String bytesToHex(byte[] bytes) {
        return bytesToHex(bytes, false);
    }

    public static final String bytesToHex(byte[] bytes, boolean padding) {
        if (padding) {
            char[] hexString = new char[3 * bytes.length];
            int j = 0;

            for (int i = 0; i < bytes.length; i++) {
                hexString[j++] = HEX_CHARS[(bytes[i] & 0xF0) >> 4];
                hexString[j++] = HEX_CHARS[bytes[i] & 0x0F];
                hexString[j++] = ' ';
            }

            return new String(hexString);
        } else {
            char[] hexString = new char[2 * bytes.length];
            int j = 0;

            for (int i = 0; i < bytes.length; i++) {
                hexString[j++] = HEX_CHARS[(bytes[i] & 0xF0) >> 4];
                hexString[j++] = HEX_CHARS[bytes[i] & 0x0F];
            }

            return new String(hexString);
        }
    }

    /**
     * Converts an ASCII character representing a hexadecimal
     * value into its integer equivalent.
     */
    public static final int hexByteToInt(byte b) {
        switch (b) {
            case '0' :
                return 0;
            case '1' :
                return 1;
            case '2' :
                return 2;
            case '3' :
                return 3;
            case '4' :
                return 4;
            case '5' :
                return 5;
            case '6' :
                return 6;
            case '7' :
                return 7;
            case '8' :
                return 8;
            case '9' :
                return 9;
            case 'A' :
            case 'a' :
                return 10;
            case 'B' :
            case 'b' :
                return 11;
            case 'C' :
            case 'c' :
                return 12;
            case 'D' :
            case 'd' :
                return 13;
            case 'E' :
            case 'e' :
                return 14;
            case 'F' :
            case 'f' :
                return 15;
            default :
                throw new IllegalArgumentException("Error decoding byte");
        }
    }

    /**
     * A 4-digit hex result.
     */
    public static final void hex4(char c, StringBuilder sb) {
        sb.append(HEX_CHARS[(c & 0xF000) >> 12]);
        sb.append(HEX_CHARS[(c & 0x0F00) >> 8]);
        sb.append(HEX_CHARS[(c & 0x00F0) >> 4]);
        sb.append(HEX_CHARS[c & 0x000F]);
    }

    /**
     * Returns a string representation of the byte array as a series of
     * hexadecimal characters.
     *
     * @param bytes
     *            byte array to convert
     * @return a string representation of the byte array as a series of
     *         hexadecimal characters
     */
    public static final String toHexString(byte[] bytes) {
        char[] hexString = new char[2 * bytes.length];
        int j = 0;

        for (int i = 0; i < bytes.length; i++) {
            hexString[j++] = HEX_CHARS[(bytes[i] & 0xF0) >> 4];
            hexString[j++] = HEX_CHARS[bytes[i] & 0x0F];
        }

        return new String(hexString);
    }

    /**
     * XOR two byte arrays together, and save result in originalArray
     *
     * @param originalArray this is the base of the XOR operation.
     * @param keyArray this is XOR'd into the original array, repeats if necessary.
     */
    public static void xorArrays(byte[] originalArray, byte[] keyArray) {
        int keyIndex = 0;
        int keyLength = keyArray.length;

        for (int i=0;i<originalArray.length;i++) {
            //XOR the data and start over if necessary
            originalArray[i] = (byte) (originalArray[i] ^ keyArray[keyIndex++ % keyLength]);
        }
    }



    public static final byte[] encodeStringArray(List<String> array) {
        int length = 0;
        for (String s : array) {
            byte[] bytes = s.getBytes();
            if (bytes != null) {
                length += bytes.length;
            }
        }

        if (length == 0) {
            return new byte[0];
        }

        byte[] bytes = new byte[length+array.size()];

        length = 0;
        for (String s : array) {
            byte[] sBytes = s.getBytes();
            System.arraycopy(sBytes, 0, bytes, length, sBytes.length);
            length += sBytes.length;
            bytes[length++] = (byte) 0x01;
        }

        return bytes;
    }

    public static final ArrayList<String> decodeStringArray(byte[] bytes) {
        int length = bytes.length;
        int position = 0;
        byte token = (byte) 0x01;
        ArrayList<String> list = new ArrayList<String>(0);

        int last = 0;
        while (last+position < length) {
            byte b = bytes[last+position++];
            if (b == token ) {
                byte[] xx = new byte[position-1];
                System.arraycopy(bytes, last, xx, 0, position-1);
                list.add(new String(xx));
                last += position;
                position = 0;
            }

        }

        return list;
    }

    public static String printArrayRaw(byte[] bytes) {
        return printArrayRaw(bytes, 0);
    }

    public static String printArrayRaw(byte[] bytes, int lineLength) {
        if (lineLength > 0) {
            int mod = lineLength;
            int length = bytes.length;
            int comma = length-1;

            StringBuilder builder = new StringBuilder(length + length/mod);
            for (int i = 0; i < length; i++) {
                builder.append(bytes[i]);
                if (i < comma) {
                    builder.append(",");
                }
                if (i > 0 && i%mod == 0) {
                    builder.append(OS.LINE_SEPARATOR);
                }
            }

            return builder.toString();

        } else {
            int length = bytes.length;
            int comma = length-1;

            StringBuilder builder = new StringBuilder(length + length);
            for (int i = 0; i < length; i++) {
                builder.append(bytes[i]);
                if (i < comma) {
                    builder.append(",");
                }
            }

            return builder.toString();
        }
    }

    public static void printArray(byte[] bytes) {
        printArray(bytes, bytes.length, true);
    }

    public static void printArray(byte[] bytes, int length, boolean includeByteCount) {
        if (includeByteCount) {
            System.err.println("Bytes: " + length);
        }

        int mod = 40;
        int comma = length-1;

        StringBuilder builder = new StringBuilder(length + length/mod);
        for (int i = 0; i < length; i++) {
            builder.append(bytes[i]);
            if (i < comma) {
                builder.append(",");
            }
            if (i > 0 && i%mod == 0) {
                builder.append(OS.LINE_SEPARATOR);
            }
        }

        System.err.println(builder.toString());
    }

    /**
     * Finds a list of classes that are annotated with the specified annotation.
     */
    public static final List<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotation) {
        return findAnnotatedClasses("", annotation);
    }

    /**
     * Finds a list of classes in the specific package that are annotated with the specified annotation.
     */
    public static final List<Class<?>> findAnnotatedClasses(String packageName, Class<? extends Annotation> annotation) {
        // find ALL ServerLoader classes and use reflection to load them.
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        if (packageName != null && !packageName.isEmpty()) {
            packageName = packageName.replace('.', '/');
        } else {
            packageName = ""; // cannot be null!
        }

        // look for all annotated classes in the projects package.
        try {
            LinkedList<Class<?>> annotatedClasses = new LinkedList<Class<?>>();

            URL url;
            Enumeration<URL> resources = classLoader.getResources(packageName);

            // lengthy, but it will traverse how we want.
            while (resources.hasMoreElements()) {
                url = resources.nextElement();
                if (url.getProtocol().equals("file")) {
                    File file = new File(url.getFile());
                    findAnnotatedClassesRecursive(classLoader, packageName, annotation, file, file.getAbsolutePath(), annotatedClasses);
                } else {
                    findModulesInJar(classLoader, packageName, annotation, url, annotatedClasses);
                }
            }

            return annotatedClasses;
        } catch (Exception e) {
            System.err.println("Problem finding annotated classes for: " + annotation.getSimpleName());
            System.exit(-1);
        }

        return null;
    }

    private static final void findAnnotatedClassesRecursive(ClassLoader classLoader, String packageName,
                                             Class<? extends Annotation> annotation, File directory,
                                             String rootPath,
                                             List<Class<?>> annotatedClasses) throws ClassNotFoundException {

        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                String absolutePath = file.getAbsolutePath();
                String fileName = file.getName();

                if (file.isDirectory()) {
                    findAnnotatedClassesRecursive(classLoader, packageName , annotation, file, rootPath, annotatedClasses);
                }
                else if (isValid(fileName)) {
                    String classPath = absolutePath.substring(rootPath.length() + 1, absolutePath.length() - 6);

                    if (packageName.isEmpty()) {
                        if (!classPath.startsWith(packageName)) {
                            return;
                        }
                    }

                    String toDots = classPath.replaceAll(File.separator, ".");

                    Class<?> clazz = Class.forName(toDots, false, classLoader);
                    if (clazz.getAnnotation(annotation) != null) {
                        annotatedClasses.add(clazz);
                    }
                }
            }
        }
    }


    private static final void findModulesInJar(ClassLoader classLoader, String searchLocation,
            Class<? extends Annotation> annotation, URL resource, List<Class<?>> annotatedClasses)
                    throws IOException, ClassNotFoundException {

        URLConnection connection = resource.openConnection();

        // Regular JAR
        if (connection instanceof JarURLConnection) {
            JarURLConnection jarURLConnection = (JarURLConnection) connection;

            JarFile jarFile = jarURLConnection.getJarFile();
            String fileResource = jarURLConnection.getEntryName();

            Enumeration<JarEntry> entries = jarFile.entries();

            // read all the jar entries.
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                String name = jarEntry.getName();

                if (name.startsWith(fileResource) && // make sure it's at least the correct package
                        isValid(name)) {

                    String classPath = name.replace(File.separatorChar, '.').substring(0, name.lastIndexOf("."));

                    String toDots = classPath.replaceAll(File.separator, ".");

                    Class<?> clazz = Class.forName(toDots, false, classLoader);
                    if (clazz.getAnnotation(annotation) != null) {
                        annotatedClasses.add(clazz);
                    }
                }
            }
            jarFile.close();

        }
        // Files inside of box deployment
        else if (connection instanceof BoxURLConnection) {
            BoxURLConnection hiveJarURLConnection = (BoxURLConnection) connection;

            // class files will not have an entry name, which is reserved for resources only
            String name = hiveJarURLConnection.getResourceName();

            if (isValid(name)) {
                String classPath = name.substring(0, name.lastIndexOf('.'));
                classPath = classPath.replace('/', '.');

                String toDots = classPath.replaceAll(File.separator, ".");

                Class<?> clazz = Class.forName(toDots, false, classLoader);
                if (clazz.getAnnotation(annotation) != null) {
                    annotatedClasses.add(clazz);
                }
            }
        }
        else {
            return;
        }
    }


    /**
     * remove directories from the search. make sure it's a class file shortcut so we don't load ALL .class files!
     *
     **/
    private static boolean isValid(String name) {

        if (name == null) {
            return false;
        }

        int length = name.length();
        boolean isValid = length > 6 &&
                          name.charAt(length-1) != '/' && // remove directories from the search.
                          name.charAt(length-6) == '.' &&
                          name.charAt(length-5) == 'c' &&
                          name.charAt(length-4) == 'l' &&
                          name.charAt(length-3) == 'a' &&
                          name.charAt(length-2) == 's' &&
                          name.charAt(length-1) == 's'; // make sure it's a class file

        return isValid;
    }
}
