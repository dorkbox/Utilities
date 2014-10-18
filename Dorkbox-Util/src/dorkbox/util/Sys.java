package dorkbox.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.JarURLConnection;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.crypto.digests.SHA256Digest;

import com.esotericsoftware.kryo.util.Util;

import dorkbox.urlHandler.BoxURLConnection;

public class Sys {
    public static final int javaVersion = getJavaVersion();
    public static final boolean isAndroid = Util.isAndroid;

    public static final sun.misc.Unsafe unsafe = getUNSAFE();

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

    private static int getJavaVersion() {
        String fullJavaVersion = System.getProperty("java.version");

        // Converts a java version string, such as "1.7u45", and converts it into 7
        char versionChar;
        if (fullJavaVersion.startsWith("1.")) {
            versionChar = fullJavaVersion.charAt(2);
        } else {
            versionChar = fullJavaVersion.charAt(0);
        }

        switch (versionChar) {
            case '4': return 4;
            case '5': return 5;
            case '6': return 6;
            case '7': return 7;
            case '8': return 8;
            case '9': return 9;
            default: return -1;
        }
    }

    private static sun.misc.Unsafe getUNSAFE() {
        try {
            final PrivilegedExceptionAction<sun.misc.Unsafe> action = new PrivilegedExceptionAction<sun.misc.Unsafe>() {
                @Override
                public sun.misc.Unsafe run() throws Exception {
                    Class<sun.misc.Unsafe> unsafeClass = sun.misc.Unsafe.class;
                    Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    Object unsafeObject = theUnsafe.get(null);
                    if (unsafeClass.isInstance(unsafeObject)) {
                        return unsafeClass.cast(unsafeObject);
                    }

                    throw new NoSuchFieldError("the Unsafe");
                }
            };

            return AccessController.doPrivileged(action);
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to load unsafe", e);
        }
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

    /**
     * FROM: https://www.cqse.eu/en/blog/string-replace-performance/
     *
     * Replaces all occurrences of keys of the given map in the given string
     * with the associated value in that map.
     *
     * This method is semantically the same as calling
     * {@link String#replace(CharSequence, CharSequence)} for each of the
     * entries in the map, but may be significantly faster for many replacements
     * performed on a short string, since
     * {@link String#replace(CharSequence, CharSequence)} uses regular
     * expressions internally and results in many String object allocations when
     * applied iteratively.
     *
     * The order in which replacements are applied depends on the order of the
     * map's entry set.
     */
    public static String replaceStringFast(String string, Map<String, String> replacements) {
        StringBuilder sb = new StringBuilder(string);
        for (Entry<String, String> entry : replacements.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            int start = sb.indexOf(key, 0);
            while (start > -1) {
                int end = start + key.length();
                int nextSearchStart = start + value.length();
                sb.replace(start, end, value);
                start = sb.indexOf(key, nextSearchStart);
            }
        }

        return sb.toString();
    }

    /**
     * Quickly finds a char in a string.
     *
     * @return index if it's there, -1 if not there
     */
    public static int searchStringFast(String string, char c) {
        int length = string.length();
        for (int i=0;i<length;i++) {
            if (string.charAt(i) == c) {
                return i;
            }
        }

        return -1;
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

    public static final byte[] copyBytes(byte[] src) {
        return copyBytes(src, 0);
    }

    public static final byte[] copyBytes(byte[] src, int position) {
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
            bytes[2*i] = (byte) (text[i] >> 8);
            bytes[2*i+1] = (byte) text[i];
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
        printArray(bytes, length, includeByteCount, 40);
    }

    public static void printArray(byte[] bytes, int length, boolean includeByteCount, int lineLength) {
        if (includeByteCount) {
            System.err.println("Bytes: " + length);
        }

        int comma = length-1;

        StringBuilder builder;
        if (lineLength > 0) {
            builder = new StringBuilder(length + comma + length/lineLength + 2);
        } else {
            builder = new StringBuilder(length + comma + 2);
        }
        builder.append("{");

        for (int i = 0; i < length; i++) {
            builder.append(bytes[i]);
            if (i < comma) {
                builder.append(",");
            }
            if (i > 0 && lineLength > 0 && i%lineLength == 0) {
                builder.append(OS.LINE_SEPARATOR);
            }
        }

        builder.append("}");
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

        String noDotsPackageName = packageName;
        boolean isEmpty = true;
        if (packageName != null && !packageName.isEmpty()) {
            noDotsPackageName = packageName.replace('.', '/');
            isEmpty = false;
        } else {
            noDotsPackageName = ""; // cannot be null!
        }

        // look for all annotated classes in the projects package.
        try {
            LinkedList<Class<?>> annotatedClasses = new LinkedList<Class<?>>();

            URL url;
            Enumeration<URL> resources = classLoader.getResources(noDotsPackageName);

            // lengthy, but it will traverse how we want.
            while (resources.hasMoreElements()) {
                url = resources.nextElement();
                if (url.getProtocol().equals("file")) {
                    File file = new File(url.getFile());
                    if (!isEmpty) {
                        String relativeToDir = FileUtil.getParentRelativeToDir(file, noDotsPackageName);
                        if (relativeToDir != null) {
                            findAnnotatedClassesRecursive(classLoader, noDotsPackageName, annotation, file, relativeToDir, annotatedClasses);
                        }
                    } else {
                        findAnnotatedClassesRecursive(classLoader, noDotsPackageName, annotation, file, file.getAbsolutePath(), annotatedClasses);
                    }
                } else {
                    findModulesInJar(classLoader, noDotsPackageName, annotation, url, annotatedClasses);
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

                    if (!packageName.isEmpty()) {
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

            Enumeration<JarEntry> entries = jarFile.entries();

            // read all the jar entries.
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                String name = jarEntry.getName();

                if (name.startsWith(searchLocation) && // make sure it's at least the correct package
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

            if (name.startsWith(searchLocation) && // make sure it's at least the correct package
                isValid(name)) {

                String classPath = name.substring(0, name.lastIndexOf('.'));
                classPath = classPath.replace('/', '.');

                Class<?> clazz = Class.forName(classPath, false, classLoader);
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

        if (name.charAt(length-1) == '/') { // remove directories from the search.)
            return false;
        }

        // ALSO, cannot use classes such as "ServerBloah$4.class".
        int newLength = length-6;
        for (int i=0;i<newLength;i++) {
            if (name.charAt(i) == '$') {
                return false;
            }
        }

        return name.charAt(length-6) == '.' &&
               name.charAt(length-5) == 'c' &&
               name.charAt(length-4) == 'l' &&
               name.charAt(length-3) == 'a' &&
               name.charAt(length-2) == 's' &&
               name.charAt(length-1) == 's'; // make sure it's a class file
    }

    /**
     * Returns an <code>InetAddress</code> object encapsulating what is most likely the machine's LAN IP address.
     * <p/>
     * This method is intended for use as a replacement of JDK method <code>InetAddress.getLocalHost</code>, because
     * that method is ambiguous on Linux systems. Linux systems enumerate the loopback network interface the same
     * way as regular LAN network interfaces, but the JDK <code>InetAddress.getLocalHost</code> method does not
     * specify the algorithm used to select the address returned under such circumstances, and will often return the
     * loopback address, which is not valid for network communication. Details
     * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4665037">here</a>.
     * <p/>
     * This method will scan all IP addresses on all network interfaces on the host machine to determine the IP address
     * most likely to be the machine's LAN address. If the machine has multiple IP addresses, this method will prefer
     * a site-local IP address (e.g. 192.168.x.x or 10.10.x.x, usually IPv4) if the machine has one (and will return the
     * first site-local address if the machine has more than one), but if the machine does not hold a site-local
     * address, this method will return simply the first non-loopback address found (IPv4 or IPv6).
     * <p/>
     * If this method cannot find a non-loopback address using this selection algorithm, it will fall back to
     * calling and returning the result of JDK method <code>InetAddress.getLocalHost</code>.
     * <p/>
     *
     * @throws UnknownHostException If the LAN address of the machine cannot be found.
     *
     * From: https://issues.apache.org/jira/browse/JCS-40
     */
    public static InetAddress getLocalHostLanAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            // Iterate all NICs (network interface cards)...
            for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
                NetworkInterface iface = ifaces.nextElement();
                // Iterate all IP addresses assigned to each card...
                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {

                        if (inetAddr.isSiteLocalAddress()) {
                            // Found non-loopback site-local address. Return it immediately...
                            return inetAddr;
                        }
                        else if (candidateAddress == null) {
                            // Found non-loopback address, but not necessarily site-local.
                            // Store it as a candidate to be returned if site-local address is not subsequently found...
                            candidateAddress = inetAddr;
                            // Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
                            // only the first. For subsequent iterations, candidate will be non-null.
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                // We did not find a site-local address, but we found some other non-loopback address.
                // Server might have a non-site-local address assigned to its NIC (or it might be running
                // IPv6 which deprecates the "site-local" concept).
                // Return this non-loopback candidate address...
                return candidateAddress;
            }
            // At this point, we did not find a non-loopback address.
            // Fall back to returning whatever InetAddress.getLocalHost() returns...
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        }
        catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }


    /**
     * This will retrieve your IP address via an HTTP server.
     * <p>
     * <b>NOTE: Use DnsClient.getPublicIp() instead. It's much faster and more reliable as it uses DNS.</b>
     *
     * @return the public IP address if found, or null if it didn't find it
     */
    @Deprecated
    public static String getPublicIpViaHttp() {
        // method 1: use DNS servers
        // dig +short myip.opendns.com @resolver1.opendns.com

        // method 2: use public http servers
        final String websites[] = {
                "http://ip.dorkbox.com/",
                "http://ip.javalauncher.com/",
                "http://checkip.dyndns.com/",
                "http://checkip.dyn.com/",
                "http://curlmyip.com/",
                "http://tnx.nl/ip",
                "http://ipecho.net/plain",
                "http://icanhazip.com/",
                "http://ip.appspot.com/",
            };

        // loop, since they won't always work.
        for (int i=0;i<websites.length;i++) {
            try {
                URL autoIP = new URL(websites[i]);
                BufferedReader in = new BufferedReader(new InputStreamReader(autoIP.openStream()));
                String response = in.readLine().trim();
                in.close();

                Pattern pattern = Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b");
                Matcher matcher = pattern.matcher(response);
                if (matcher.find()) {
                    String IP = matcher.group().trim();
                    return IP;
                }
            } catch (Exception e){
            }
        }

        return null;
    }



}
