package dorkbox.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * File related utilities.
 *
 *
 * Contains code from FilenameUtils.java (normalize + dependencies) - Apache 2.0 License
 * http://commons.apache.org/proper/commons-io/
 * Copyright 2013 ASF
 * Authors: Kevin A. Burton, Scott Sanders, Daniel Rall, Christoph.Reck,
 *          Peter Donald, Jeff Turner, Matthew Hawthorne, Martin Cooper,
 *          Jeremias Maerki, Stephen Colebourne
 */
public class FileUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    /**
     * The Unix separator character.
     */
    private static final char UNIX_SEPARATOR = '/';

    /**
     * The Windows separator character.
     */
    private static final char WINDOWS_SEPARATOR = '\\';

    /**
     * The system separator character.
     */
    private static final char SYSTEM_SEPARATOR = File.separatorChar;

    /**
     * The separator character that is the opposite of the system separator.
     */
    private static final char OTHER_SEPARATOR;
    static {
        if (OS.isWindows()) {
            OTHER_SEPARATOR = UNIX_SEPARATOR;
        } else {
            OTHER_SEPARATOR = WINDOWS_SEPARATOR;
        }
    }


    public static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    public static byte[] ZIP_HEADER = { 'P', 'K', 0x3, 0x4 };

    /**
     * Renames a file. Windows has all sorts of problems which are worked around.
     *
     * @return true if successful, false otherwise
     */
    public static boolean renameTo(File source, File dest) {
        // if we're on a civilized operating system we may be able to simple
        // rename it
        if (source.renameTo(dest)) {
            return true;
        }

        // fall back to trying to rename the old file out of the way, rename the
        // new file into
        // place and then delete the old file
        if (dest.exists()) {
            File temp = new File(dest.getPath() + "_old");
            if (temp.exists()) {
                if (!temp.delete()) {
                    logger.warn("Failed to delete old intermediate file {}.", temp);
                    // the subsequent code will probably fail
                }
            }
            if (dest.renameTo(temp)) {
                if (source.renameTo(dest)) {
                    if (temp.delete()) {
                        logger.warn("Failed to delete intermediate file {}.", temp);
                    }
                    return true;
                }
            }
        }

        // as a last resort, try copying the old data over the new
        FileInputStream fin = null;
        FileOutputStream fout = null;
        try {
            fin = new FileInputStream(source);
            fout = new FileOutputStream(dest);
            Sys.copyStream(fin, fout);
            if (!source.delete()) {
                logger.warn("Failed to delete {} after brute force copy to {}.", source, dest);
            }
            return true;

        } catch (IOException ioe) {
            logger.warn("Failed to copy {} to {}.", source, dest, ioe);
            return false;

        } finally {
            Sys.close(fin);
            Sys.close(fout);
        }
    }

    /**
     * Reads the contents of the supplied input stream into a list of lines.
     * Closes the reader on successful or failed completion.
     */
    public static List<String> readLines(Reader in) throws IOException {
        List<String> lines = new ArrayList<String>();
        try {
            BufferedReader bin = new BufferedReader(in);
            for (String line = null; (line = bin.readLine()) != null; lines.add(line)) {}
        } finally {
            Sys.close(in);
        }
        return lines;
    }

    /**
     * Copies a files from one location to another.  Overwriting any existing file at the destination.
     */
    public static File copyFile(String in, String out) throws IOException {
        return copyFile(new File(in), new File(out));
    }

    /**
     * Copies a files from one location to another. Overwriting any existing file at the destination.
     */
    public static File copyFile(File in, File out) throws IOException {
        if (in == null) {
            throw new IllegalArgumentException("in cannot be null.");
        }
        if (out == null) {
            throw new IllegalArgumentException("out cannot be null.");
        }


        String normalizedIn = normalize(in.getAbsolutePath());
        String normalizedout = normalize(out.getAbsolutePath());

        // if out doesn't exist, then create it.
        File parentOut = out.getParentFile();
        if (!parentOut.canWrite()) {
            parentOut.mkdirs();
        }

        Logger logger2 = logger;
        if (logger2.isTraceEnabled()) {
            logger2.trace("Copying file: {}  -->  {}", in, out);
        }

        FileChannel sourceChannel = null;
        FileChannel destinationChannel = null;
        try {
            sourceChannel = new FileInputStream(normalizedIn).getChannel();
            destinationChannel = new FileOutputStream(normalizedout).getChannel();
            sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        } finally {
            try {
                if (sourceChannel != null) {
                    sourceChannel.close();
                }
            } catch (Exception ignored) {
            }
            try {
                if (destinationChannel != null) {
                    destinationChannel.close();
                }
            } catch (Exception ignored) {
            }
        }

        out.setLastModified(in.lastModified());

        return out;
    }

    /**
     * Copies the contents of file two onto the END of file one.
     */
    @SuppressWarnings("resource")
    public static File concatFiles(File one, File two) {
        if (one == null) {
            throw new IllegalArgumentException("one cannot be null.");
        }
        if (two == null) {
            throw new IllegalArgumentException("two cannot be null.");
        }

        String normalizedOne = normalize(one.getAbsolutePath());
        String normalizedTwo = normalize(two.getAbsolutePath());

        Logger logger2 = logger;
        if (logger2.isTraceEnabled()) {
            logger2.trace("Cocating file: {}  -->  {}", one, two);
        }

        FileChannel channelOne = null;
        FileChannel channelTwo = null;
        try {
            // open it in append mode
            channelOne = new FileOutputStream(normalizedOne, true).getChannel();
            channelTwo = new FileInputStream(normalizedTwo).getChannel();

            long size = two.length();
            while (size > 0) {
                size -= channelOne.transferFrom(channelTwo, 0, size);
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        } finally {
            try {
                if (channelOne != null) {
                    channelOne.close();
                }
            } catch (Exception ignored) {
            }
            try {
                if (channelTwo != null) {
                    channelTwo.close();
                }
            } catch (Exception ignored) {
            }
        }

        one.setLastModified(System.currentTimeMillis());

        return one;
    }


    /**
     * Moves a file, overwriting any existing file at the destination.
     */
    public static File moveFile(String in, String out) {
        if (in == null || in.isEmpty()) {
            throw new IllegalArgumentException("in cannot be null.");
        }
        if (out == null || out.isEmpty()) {
            throw new IllegalArgumentException("out cannot be null.");
        }

        return moveFile(new File(in), new File(out));
    }

    /**
     * Moves a file, overwriting any existing file at the destination.
     */
    public static File moveFile(File in, File out) {
        if (in == null) {
            throw new IllegalArgumentException("in cannot be null.");
        }
        if (out == null) {
            throw new IllegalArgumentException("out cannot be null.");
        }

        System.err.println("\t\t: Moving file");
        System.err.println("\t\t:   " + in.getAbsolutePath());
        System.err.println("\t\t:     " + out.getAbsolutePath());

        if (out.canRead()) {
            out.delete();
        }

        boolean renameSuccess = renameTo(in, out);
        if (!renameSuccess) {
            throw new RuntimeException("Unable to move file: '" + in.getAbsolutePath() + "' -> '" + out.getAbsolutePath() + "'");
        }
        return out;
    }

    /**
     * Copies a directory from one location to another
     */
    public static void copyDirectory(String src, String dest, String... dirNamesToIgnore) throws IOException {
        copyDirectory(new File(src), new File(dest), dirNamesToIgnore);
    }


    /**
     * Copies a directory from one location to another
     */
    public static void copyDirectory(File src, File dest, String... dirNamesToIgnore) throws IOException {
        if (dirNamesToIgnore.length > 0) {
            String name = src.getName();
            for (String ignore : dirNamesToIgnore) {
                if (name.equals(ignore)) {
                    return;
                }
            }
        }


        if (src.isDirectory()) {
            // if directory not exists, create it
            if (!dest.exists()) {
                dest.mkdir();
                Logger logger2 = logger;
                if (logger2.isTraceEnabled()) {
                    logger2.trace("Directory copied from  {}  -->  {}", src, dest);
                }
            }

            // list all the directory contents
            String files[] = src.list();

            for (String file : files) {
                // construct the src and dest file structure
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);

                // recursive copy
                copyDirectory(srcFile, destFile, dirNamesToIgnore);
            }
        } else {
            // if file, then copy it
            copyFile(src, dest);
        }
    }

    /**
     * Safely moves a directory from one location to another (by copying it first, then deleting the original).
     */
    public static void moveDirectory(String src, String dest, String... dirNamesToIgnore) throws IOException {
        moveDirectory(new File(src), new File(dest), dirNamesToIgnore);
    }

    /**
     * Safely moves a directory from one location to another (by copying it first, then deleting the original).
     */
    public static void moveDirectory(File src, File dest, String... dirNamesToIgnore) throws IOException {
        if (dirNamesToIgnore.length > 0) {
            String name = src.getName();
            for (String ignore : dirNamesToIgnore) {
                if (name.equals(ignore)) {
                    return;
                }
            }
        }


        if (src.isDirectory()) {
            // if directory not exists, create it
            if (!dest.exists()) {
                dest.mkdir();
                Logger logger2 = logger;
                if (logger2.isTraceEnabled()) {
                    logger2.trace("Directory copied from  {}  -->  {}", src, dest);
                }
            }

            // list all the directory contents
            String files[] = src.list();

            for (String file : files) {
                // construct the src and dest file structure
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);

                // recursive copy
                moveDirectory(srcFile, destFile, dirNamesToIgnore);
            }
        } else {
            // if file, then copy it
            moveFile(src, dest);
        }
    }

    /**
     * Deletes a file or directory and all files and sub-directories under it.
     */
    public static boolean delete(String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException("fileName cannot be null.");
        }

        return delete(new File(fileName));
    }

    /**
     * Deletes a file or directory and all files and sub-directories under it.
     */
    public static boolean delete(File file) {
        Logger logger2 = logger;
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0, n = files.length; i < n; i++) {
                if (files[i].isDirectory()) {
                    delete(files[i].getAbsolutePath());
                } else {
                    if (logger2.isTraceEnabled()) {
                        logger2.trace("Deleting file: {}", files[i]);
                    }
                    files[i].delete();
                }
            }
        }
        if (logger2.isTraceEnabled()) {
            logger2.trace("Deleting file: {}", file);
        }

        return file.delete();
    }


    /**
     * Creates the directories in the specified location.
     */
    public static String mkdir(File location) {
        if (location == null) {
            throw new IllegalArgumentException("fileDir cannot be null.");
        }

        String path = location.getAbsolutePath();
        if (location.mkdirs()) {
            Logger logger2 = logger;
            if (logger2.isTraceEnabled()) {
                logger2.trace("Created directory: {}", path);
            }
        }

        return path;
    }

    /**
     * Creates the directories in the specified location.
     */
    public static String mkdir(String location) {
        if (location == null) {
            throw new IllegalArgumentException("path cannot be null.");
        }

        return mkdir(new File(location));
    }


    /**
     * Creates a temp file
     */
    public static File tempFile(String fileName) throws IOException {
        if (fileName == null) {
            throw new IllegalArgumentException("fileName cannot be null");
        }

        return File.createTempFile(fileName, null).getAbsoluteFile();
    }

    /**
     * Creates a temp directory
     */
    public static String tempDirectory(String directoryName) throws IOException {
        if (directoryName == null) {
            throw new IllegalArgumentException("directoryName cannot be null");
        }

        File file = File.createTempFile(directoryName, null);
        if (!file.delete()) {
            throw new IOException("Unable to delete temp file: " + file);
        }

        if (!file.mkdir()) {
            throw new IOException("Unable to create temp directory: " + file);
        }

        return file.getAbsolutePath();
    }

    /**
     *  @return true if the inputStream is a zip/jar stream. DOES NOT CLOSE THE STREAM
     */
    public static boolean isZipStream(InputStream in) {
        if (!in.markSupported()) {
            in = new BufferedInputStream(in);
        }
        boolean isZip = true;
        try {
            in.mark(ZIP_HEADER.length);
            for (int i = 0; i < ZIP_HEADER.length; i++) {
                if (ZIP_HEADER[i] != (byte) in.read()) {
                    isZip = false;
                    break;
                }
            }
            in.reset();
        } catch (Exception e) {
            isZip = false;
        }

        return isZip;
    }

    /**
     * @return true if the named file is a zip/jar file
     */
    public static boolean isZipFile(String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException("fileName cannot be null");
        }

        return isZipFile(new File(fileName));
    }

    /**
     * @return true if the file is a zip/jar file
     */
    public static boolean isZipFile(File file) {
        boolean isZip = true;
        byte[] buffer = new byte[ZIP_HEADER.length];

        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            raf.readFully(buffer);
            for (int i = 0; i < ZIP_HEADER.length; i++) {
                if (buffer[i] != ZIP_HEADER[i]) {
                    isZip = false;
                    break;
                }
            }
        } catch (Exception e) {
            isZip = false;
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return isZip;
    }


    /**
     * Unzips a ZIP file
     *
     * @return The path to the output directory.
     */
    public static void unzip(String zipFile, String outputDir) throws IOException {
        unzipJar(zipFile, outputDir, true);
    }

    /**
     * Unzips a ZIP file
     *
     * @return The path to the output directory.
     */
    public static void unzip(File zipFile, File outputDir) throws IOException {
        unzipJar(zipFile, outputDir, true);
    }

    /**
     * Unzips a ZIP file. Will close the input stream.
     *
     * @return The path to the output directory.
     */
    public static void unzip(ZipInputStream inputStream, String outputDir) throws IOException {
        if (outputDir == null) {
            throw new IllegalArgumentException("outputDir cannot be null.");
        }

        unzip(inputStream, new File(outputDir));
    }

    /**
     * Unzips a ZIP file. Will close the input stream.
     *
     * @return The path to the output directory.
     */
    public static void unzip(ZipInputStream inputStream, File outputDir) throws IOException {
        unzipJar(inputStream, outputDir, true);
    }

    /**
     * Unzips a ZIP file
     *
     * @return The path to the output directory.
     */
    public static void unzipJar(String zipFile, String outputDir, boolean extractManifest) throws IOException {
        if (zipFile == null) {
            throw new IllegalArgumentException("zipFile cannot be null.");
        }
        if (outputDir == null) {
            throw new IllegalArgumentException("outputDir cannot be null.");
        }

        unjarzip0(new File(zipFile), new File(outputDir), extractManifest);
    }

    /**
     * Unzips a ZIP file
     *
     * @return The path to the output directory.
     */
    public static void unzipJar(File zipFile, File outputDir, boolean extractManifest) throws IOException {
        if (zipFile == null) {
            throw new IllegalArgumentException("zipFile cannot be null.");
        }
        if (outputDir == null) {
            throw new IllegalArgumentException("outputDir cannot be null.");
        }

        unjarzip0(zipFile, outputDir, extractManifest);
    }

    /**
     * Unzips a ZIP file. Will close the input stream.
     *
     * @return The path to the output directory.
     */
    public static void unzipJar(ZipInputStream inputStream, File outputDir, boolean extractManifest) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream cannot be null.");
        }
        if (outputDir == null) {
            throw new IllegalArgumentException("outputDir cannot be null.");
        }

        unjarzip1(inputStream, outputDir, extractManifest);
    }

    /**
     * Unzips a ZIP or JAR file (and handles the manifest if requested)
     */
    private static void unjarzip0(File zipFile, File outputDir, boolean extractManifest) throws IOException {
        if (zipFile == null) {
            throw new IllegalArgumentException("zipFile cannot be null.");
        }
        if (outputDir == null) {
            throw new IllegalArgumentException("outputDir cannot be null.");
        }

        long fileLength = zipFile.length();
        if (fileLength > Integer.MAX_VALUE - 1) {
            throw new RuntimeException("Source filesize is too large!");
        }

        ZipInputStream inputStream = new ZipInputStream(new FileInputStream(zipFile));

        unjarzip1(inputStream, outputDir, extractManifest);
    }

    /**
     * Unzips a ZIP file
     *
     * @return The path to the output directory.
     */
    private static void unjarzip1(ZipInputStream inputStream, File outputDir, boolean extractManifest) throws FileNotFoundException, IOException {
        try {
            ZipEntry entry = null;
            while ((entry = inputStream.getNextEntry()) != null) {
                String name = entry.getName();

                if (!extractManifest && name.startsWith("META-INF/")) {
                    continue;
                }

                File file = new File(outputDir, name);
                if (entry.isDirectory()) {
                    mkdir(file.getPath());
                    continue;
                }
                mkdir(file.getParent());


                FileOutputStream output = new FileOutputStream(file);
                try {
                    Sys.copyStream(inputStream, output);
                } finally {
                    Sys.close(output);
                }
            }
        } finally {
            Sys.close(inputStream);
        }
    }


    /**
     * Parses the specified root directory for <b>ALL</b> files that are in it. All of the sub-directories are searched as well.
     * <p>
     * <i>This is different, in that it returns ALL FILES, instead of ones that just match a specific extension.</i>
     * @return the list of all files in the root+sub-dirs.
     */
    public static List<File> parseDir(File rootDirectory) {
        return parseDir(rootDirectory, (String) null);
    }

    /**
     * Parses the specified root directory for files that end in the extension to match. All of the sub-directories are searched as well.
     * @return the list of all files in the root+sub-dirs that match the given extension.
     */
    public static List<File> parseDir(File rootDirectory, String... extensionsToMatch) {
        List<File> jarList = new LinkedList<File>();
        LinkedList<File> directories = new LinkedList<File>();

        if (rootDirectory.isDirectory()) {
            directories.add(rootDirectory);

            while (directories.peek() != null) {
                File dir = directories.poll();
                File[] listFiles = dir.listFiles();
                if (listFiles != null) {
                    for (File file : listFiles) {
                        if (file.isDirectory()) {
                            directories.add(file);
                        } else {
                            if (extensionsToMatch == null || extensionsToMatch.length == 0 || extensionsToMatch[0] == null) {
                                jarList.add(file);
                            } else {
                                for (String e : extensionsToMatch) {
                                    if (file.getAbsolutePath().endsWith(e)) {
                                        jarList.add(file);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            System.err.println("Cannot search directory children if the dir is a file name: " + rootDirectory.getAbsolutePath());
        }


        return jarList;
    }

    /**
     * Gets the relative path of a file to a specific directory in it's hierarchy.
     *
     * For example: getChildRelativeToDir("/a/b/c/d/e.bah", "c") -> "d/e.bah"
     */
    public static String getChildRelativeToDir(String fileName, String dirInHeirarchy) {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("fileName cannot be null.");
        }

        return getChildRelativeToDir(new File(fileName), dirInHeirarchy);
    }

    /**
     * Gets the relative path of a file to a specific directory in it's hierarchy.
     *
     * For example: getChildRelativeToDir("/a/b/c/d/e.bah", "c") -> "d/e.bah"
     * @return null if it cannot be found
     */
    public static String getChildRelativeToDir(File file, String dirInHeirarchy) {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null.");
        }

        if (dirInHeirarchy == null || dirInHeirarchy.isEmpty()) {
            throw new IllegalArgumentException("dirInHeirarchy cannot be null.");
        }

        String[] split = dirInHeirarchy.split(File.separator);
        int splitIndex = split.length-1;

        String absolutePath = file.getAbsolutePath();

        File parent = file;
        String parentName;

        if (splitIndex == 0) {
            // match on ONE dir
            while (parent != null) {
                parentName = parent.getName();

                if (parentName.equals(dirInHeirarchy)) {
                    parentName = parent.getAbsolutePath();

                    return absolutePath.substring(parentName.length() + 1);
                }
                parent = parent.getParentFile();
            }
        }
        else {
            // match on MANY dir. They must be "in-order"
            boolean matched = false;
            while (parent != null) {
                parentName = parent.getName();

                if (matched) {
                    if (parentName.equals(split[splitIndex])) {
                        splitIndex--;
                        if (splitIndex < 0) {
                            parent = parent.getParentFile();
                            parentName = parent.getAbsolutePath();
                            return absolutePath.substring(parentName.length() + 1);
                        }
                    } else {
                        // because it has to be "in-order", if it doesn't match, we immediately abort
                        return null;
                    }
                } else {
                    if (parentName.equals(split[splitIndex])) {
                        matched = true;
                        splitIndex--;
                    }
                }

                parent = parent.getParentFile();
            }
        }

        return null;
    }

    /**
     * Gets the PARENT relative path of a file to a specific directory in it's hierarchy.
     *
     * For example: getParentRelativeToDir("/a/b/c/d/e.bah", "c") -> "/a/b"
     */
    public static String getParentRelativeToDir(String fileName, String dirInHeirarchy) {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("fileName cannot be null.");
        }

        return getParentRelativeToDir(new File(fileName), dirInHeirarchy);
    }

    /**
     * Gets the relative path of a file to a specific directory in it's hierarchy.
     *
     * For example: getParentRelativeToDir("/a/b/c/d/e.bah", "c") -> "/a/b"
     * @return null if it cannot be found
     */
    public static String getParentRelativeToDir(File file, String dirInHeirarchy) {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null.");
        }

        if (dirInHeirarchy == null || dirInHeirarchy.isEmpty()) {
            throw new IllegalArgumentException("dirInHeirarchy cannot be null.");
        }

        String[] split = dirInHeirarchy.split(File.separator);
        int splitIndex = split.length-1;

        File parent = file;
        String parentName;

        if (splitIndex == 0) {
            // match on ONE dir
            while (parent != null) {
                parentName = parent.getName();

                if (parentName.equals(dirInHeirarchy)) {
                    parent = parent.getParentFile();
                    parentName = parent.getAbsolutePath();
                    return parentName;
                }
                parent = parent.getParentFile();
            }
        }
        else {
            // match on MANY dir. They must be "in-order"
            boolean matched = false;
            while (parent != null) {
                parentName = parent.getName();

                if (matched) {
                    if (parentName.equals(split[splitIndex])) {
                        splitIndex--;
                        if (splitIndex < 0) {
                            parent = parent.getParentFile();
                            parentName = parent.getAbsolutePath();
                            return parentName;
                        }
                    } else {
                        // because it has to be "in-order", if it doesn't match, we immediately abort
                        return null;
                    }
                } else {
                    if (parentName.equals(split[splitIndex])) {
                        matched = true;
                        splitIndex--;
                    }
                }

                parent = parent.getParentFile();
            }
        }

        return null;
    }

    /**
     * Extracts a file from a zip into a TEMP file, if possible. The TEMP file is deleted upon JVM exit.
     *
     * @throws IOException
     * @return the location of the extracted file, or NULL if the file cannot be extracted or doesn't exist.
     */
    public static String extractFromZip(String zipFile, String fileToExtract) throws IOException {
        if (zipFile == null) {
            throw new IllegalArgumentException("file cannot be null.");
        }

        if (fileToExtract == null) {
            throw new IllegalArgumentException("fileToExtract cannot be null.");
        }

        ZipInputStream inputStrem = new ZipInputStream(new FileInputStream(zipFile));
        try {
            while (true) {
                ZipEntry entry = inputStrem.getNextEntry();
                if (entry == null) {
                    break;
                }

                String name = entry.getName();
                if (entry.isDirectory()) {
                    continue;
                }

                if (name.equals(fileToExtract)) {
                    File tempFile = FileUtil.tempFile(name);
                    tempFile.deleteOnExit();

                    FileOutputStream output = new FileOutputStream(tempFile);
                    try {
                        Sys.copyStream(inputStrem, output);
                    } finally {
                        output.close();
                    }

                    return tempFile.getAbsolutePath();
                }
            }
        } finally {
            inputStrem.close();
        }

        return null;
    }


  //-----------------------------------------------------------------------
    /**
     * Normalizes a path, removing double and single dot path steps.
     * <p>
     * This method normalizes a path to a standard format.
     * The input may contain separators in either Unix or Windows format.
     * The output will contain separators in the format of the system.
     * <p>
     * A trailing slash will be retained.
     * A double slash will be merged to a single slash (but UNC names are handled).
     * A single dot path segment will be removed.
     * A double dot will cause that path segment and the one before to be removed.
     * If the double dot has no parent path segment to work with, {@code null}
     * is returned.
     * <p>
     * The output will be the same on both Unix and Windows except
     * for the separator character.
     * <pre>
     * /foo//               -->   /foo/
     * /foo/./              -->   /foo/
     * /foo/../bar          -->   /bar
     * /foo/../bar/         -->   /bar/
     * /foo/../bar/../baz   -->   /baz
     * //foo//./bar         -->   /foo/bar
     * /../                 -->   null
     * ../foo               -->   null
     * foo/bar/..           -->   foo/
     * foo/../../bar        -->   null
     * foo/../bar           -->   bar
     * //server/foo/../bar  -->   //server/bar
     * //server/../bar      -->   null
     * C:\foo\..\bar        -->   C:\bar
     * C:\..\bar            -->   null
     * ~/foo/../bar/        -->   ~/bar/
     * ~/../bar             -->   null
     * </pre>
     * (Note the file separator returned will be correct for Windows/Unix)
     *
     * @param filename  the filename to normalize, null returns null
     * @return the normalized filename, or null if invalid
     */
    public static String normalize(String filename) {
        return doNormalize(filename, SYSTEM_SEPARATOR, true);
    }

    /**
     * Normalizes a path, removing double and single dot path steps.
     * <p>
     * This method normalizes a path to a standard format.
     * The input may contain separators in either Unix or Windows format.
     * The output will contain separators in the format of the system.
     * <p>
     * A trailing slash will be retained.
     * A double slash will be merged to a single slash (but UNC names are handled).
     * A single dot path segment will be removed.
     * A double dot will cause that path segment and the one before to be removed.
     * If the double dot has no parent path segment to work with, {@code null}
     * is returned.
     * <p>
     * The output will be the same on both Unix and Windows except
     * for the separator character.
     * <pre>
     * /foo//               -->   /foo/
     * /foo/./              -->   /foo/
     * /foo/../bar          -->   /bar
     * /foo/../bar/         -->   /bar/
     * /foo/../bar/../baz   -->   /baz
     * //foo//./bar         -->   /foo/bar
     * /../                 -->   null
     * ../foo               -->   null
     * foo/bar/..           -->   foo/
     * foo/../../bar        -->   null
     * foo/../bar           -->   bar
     * //server/foo/../bar  -->   //server/bar
     * //server/../bar      -->   null
     * C:\foo\..\bar        -->   C:\bar
     * C:\..\bar            -->   null
     * ~/foo/../bar/        -->   ~/bar/
     * ~/../bar             -->   null
     * </pre>
     * (Note the file separator returned will be correct for Windows/Unix)
     *
     * @param file  the file to normalize, null returns null
     * @return the normalized file, or null if invalid
     */
    public static File normalize(File file) {
        if (file == null) {
            return null;
        }

        String asString = doNormalize(file.getAbsolutePath(), SYSTEM_SEPARATOR, true);
        if (asString == null) {
            return null;
        }

        return new File(asString);
    }


    /**
     * Normalizes a path, removing double and single dot path steps.
     * <p>
     * This method normalizes a path to a standard format.
     * The input may contain separators in either Unix or Windows format.
     * The output will contain separators in the format specified.
     * <p>
     * A trailing slash will be retained.
     * A double slash will be merged to a single slash (but UNC names are handled).
     * A single dot path segment will be removed.
     * A double dot will cause that path segment and the one before to be removed.
     * If the double dot has no parent path segment to work with, {@code null}
     * is returned.
     * <p>
     * The output will be the same on both Unix and Windows except
     * for the separator character.
     * <pre>
     * /foo//               -->   /foo/
     * /foo/./              -->   /foo/
     * /foo/../bar          -->   /bar
     * /foo/../bar/         -->   /bar/
     * /foo/../bar/../baz   -->   /baz
     * //foo//./bar         -->   /foo/bar
     * /../                 -->   null
     * ../foo               -->   null
     * foo/bar/..           -->   foo/
     * foo/../../bar        -->   null
     * foo/../bar           -->   bar
     * //server/foo/../bar  -->   //server/bar
     * //server/../bar      -->   null
     * C:\foo\..\bar        -->   C:\bar
     * C:\..\bar            -->   null
     * ~/foo/../bar/        -->   ~/bar/
     * ~/../bar             -->   null
     * </pre>
     * The output will be the same on both Unix and Windows including
     * the separator character.
     *
     * @param filename  the filename to normalize, null returns null
     * @param unixSeparator {@code true} if a unix separator should
     * be used or {@code false} if a windows separator should be used.
     * @return the normalized filename, or null if invalid
     * @since 2.0
     */
    public static String normalize(String filename, boolean unixSeparator) {
        char separator = unixSeparator ? UNIX_SEPARATOR : WINDOWS_SEPARATOR;
        return doNormalize(filename, separator, true);
    }

    //-----------------------------------------------------------------------
    /**
     * Normalizes a path, removing double and single dot path steps,
     * and removing any final directory separator.
     * <p>
     * This method normalizes a path to a standard format.
     * The input may contain separators in either Unix or Windows format.
     * The output will contain separators in the format of the system.
     * <p>
     * A trailing slash will be removed.
     * A double slash will be merged to a single slash (but UNC names are handled).
     * A single dot path segment will be removed.
     * A double dot will cause that path segment and the one before to be removed.
     * If the double dot has no parent path segment to work with, {@code null}
     * is returned.
     * <p>
     * The output will be the same on both Unix and Windows except
     * for the separator character.
     * <pre>
     * /foo//               -->   /foo
     * /foo/./              -->   /foo
     * /foo/../bar          -->   /bar
     * /foo/../bar/         -->   /bar
     * /foo/../bar/../baz   -->   /baz
     * //foo//./bar         -->   /foo/bar
     * /../                 -->   null
     * ../foo               -->   null
     * foo/bar/..           -->   foo
     * foo/../../bar        -->   null
     * foo/../bar           -->   bar
     * //server/foo/../bar  -->   //server/bar
     * //server/../bar      -->   null
     * C:\foo\..\bar        -->   C:\bar
     * C:\..\bar            -->   null
     * ~/foo/../bar/        -->   ~/bar
     * ~/../bar             -->   null
     * </pre>
     * (Note the file separator returned will be correct for Windows/Unix)
     *
     * @param filename  the filename to normalize, null returns null
     * @return the normalized filename, or null if invalid
     */
    public static String normalizeNoEndSeparator(String filename) {
        return doNormalize(filename, SYSTEM_SEPARATOR, false);
    }

    /**
     * Normalizes a path, removing double and single dot path steps,
     * and removing any final directory separator.
     * <p>
     * This method normalizes a path to a standard format.
     * The input may contain separators in either Unix or Windows format.
     * The output will contain separators in the format specified.
     * <p>
     * A trailing slash will be removed.
     * A double slash will be merged to a single slash (but UNC names are handled).
     * A single dot path segment will be removed.
     * A double dot will cause that path segment and the one before to be removed.
     * If the double dot has no parent path segment to work with, {@code null}
     * is returned.
     * <p>
     * The output will be the same on both Unix and Windows including
     * the separator character.
     * <pre>
     * /foo//               -->   /foo
     * /foo/./              -->   /foo
     * /foo/../bar          -->   /bar
     * /foo/../bar/         -->   /bar
     * /foo/../bar/../baz   -->   /baz
     * //foo//./bar         -->   /foo/bar
     * /../                 -->   null
     * ../foo               -->   null
     * foo/bar/..           -->   foo
     * foo/../../bar        -->   null
     * foo/../bar           -->   bar
     * //server/foo/../bar  -->   //server/bar
     * //server/../bar      -->   null
     * C:\foo\..\bar        -->   C:\bar
     * C:\..\bar            -->   null
     * ~/foo/../bar/        -->   ~/bar
     * ~/../bar             -->   null
     * </pre>
     *
     * @param filename  the filename to normalize, null returns null
     * @param unixSeparator {@code true} if a unix separator should
     * be used or {@code false} if a windows separtor should be used.
     * @return the normalized filename, or null if invalid
     * @since 2.0
     */
    public static String normalizeNoEndSeparator(String filename, boolean unixSeparator) {
         char separator = unixSeparator ? UNIX_SEPARATOR : WINDOWS_SEPARATOR;
        return doNormalize(filename, separator, false);
    }

    /**
     * Internal method to perform the normalization.
     *
     * @param filename  the filename
     * @param separator The separator character to use
     * @param keepSeparator  true to keep the final separator
     * @return the normalized filename
     */
    private static String doNormalize(String filename, char separator, boolean keepSeparator) {
        if (filename == null) {
            return null;
        }
        int size = filename.length();
        if (size == 0) {
            return filename;
        }
        int prefix = getPrefixLength(filename);
        if (prefix < 0) {
            return null;
        }

        char[] array = new char[size + 2];  // +1 for possible extra slash, +2 for arraycopy
        filename.getChars(0, filename.length(), array, 0);

        // fix separators throughout
        char otherSeparator = separator == SYSTEM_SEPARATOR ? OTHER_SEPARATOR : SYSTEM_SEPARATOR;
        for (int i = 0; i < array.length; i++) {
            if (array[i] == otherSeparator) {
                array[i] = separator;
            }
        }

        // add extra separator on the end to simplify code below
        boolean lastIsDirectory = true;
        if (array[size - 1] != separator) {
            array[size++] = separator;
            lastIsDirectory = false;
        }

        // adjoining slashes
        for (int i = prefix + 1; i < size; i++) {
            if (array[i] == separator && array[i - 1] == separator) {
                System.arraycopy(array, i, array, i - 1, size - i);
                size--;
                i--;
            }
        }

        // dot slash
        for (int i = prefix + 1; i < size; i++) {
            if (array[i] == separator && array[i - 1] == '.' &&
                    (i == prefix + 1 || array[i - 2] == separator)) {
                if (i == size - 1) {
                    lastIsDirectory = true;
                }
                System.arraycopy(array, i + 1, array, i - 1, size - i);
                size -=2;
                i--;
            }
        }

        // double dot slash
        outer:
        for (int i = prefix + 2; i < size; i++) {
            if (array[i] == separator && array[i - 1] == '.' && array[i - 2] == '.' &&
                    (i == prefix + 2 || array[i - 3] == separator)) {
                if (i == prefix + 2) {
                    return null;
                }
                if (i == size - 1) {
                    lastIsDirectory = true;
                }
                int j;
                for (j = i - 4 ; j >= prefix; j--) {
                    if (array[j] == separator) {
                        // remove b/../ from a/b/../c
                        System.arraycopy(array, i + 1, array, j + 1, size - i);
                        size -= i - j;
                        i = j + 1;
                        continue outer;
                    }
                }
                // remove a/../ from a/../c
                System.arraycopy(array, i + 1, array, prefix, size - i);
                size -= i + 1 - prefix;
                i = prefix + 1;
            }
        }

        if (size <= 0) {  // should never be less than 0
            return "";
        }
        if (size <= prefix) {  // should never be less than prefix
            return new String(array, 0, size);
        }
        if (lastIsDirectory && keepSeparator) {
            return new String(array, 0, size);  // keep trailing separator
        }
        return new String(array, 0, size - 1);  // lose trailing separator
    }

    //-----------------------------------------------------------------------
    /**
     * Returns the length of the filename prefix, such as <code>C:/</code> or <code>~/</code>.
     * <p>
     * This method will handle a file in either Unix or Windows format.
     * <p>
     * The prefix length includes the first slash in the full filename
     * if applicable. Thus, it is possible that the length returned is greater
     * than the length of the input string.
     * <pre>
     * Windows:
     * a\b\c.txt           --> ""          --> relative
     * \a\b\c.txt          --> "\"         --> current drive absolute
     * C:a\b\c.txt         --> "C:"        --> drive relative
     * C:\a\b\c.txt        --> "C:\"       --> absolute
     * \\server\a\b\c.txt  --> "\\server\" --> UNC
     *
     * Unix:
     * a/b/c.txt           --> ""          --> relative
     * /a/b/c.txt          --> "/"         --> absolute
     * ~/a/b/c.txt         --> "~/"        --> current user
     * ~                   --> "~/"        --> current user (slash added)
     * ~user/a/b/c.txt     --> "~user/"    --> named user
     * ~user               --> "~user/"    --> named user (slash added)
     * </pre>
     * <p>
     * The output will be the same irrespective of the machine that the code is running on.
     * ie. both Unix and Windows prefixes are matched regardless.
     *
     * @param filename  the filename to find the prefix in, null returns -1
     * @return the length of the prefix, -1 if invalid or null
     */
    public static int getPrefixLength(String filename) {
        if (filename == null) {
            return -1;
        }
        int len = filename.length();
        if (len == 0) {
            return 0;
        }
        char ch0 = filename.charAt(0);
        if (ch0 == ':') {
            return -1;
        }
        if (len == 1) {
            if (ch0 == '~') {
                return 2;  // return a length greater than the input
            }
            return isSeparator(ch0) ? 1 : 0;
        } else {
            if (ch0 == '~') {
                int posUnix = filename.indexOf(UNIX_SEPARATOR, 1);
                int posWin = filename.indexOf(WINDOWS_SEPARATOR, 1);
                if (posUnix == -1 && posWin == -1) {
                    return len + 1;  // return a length greater than the input
                }
                posUnix = posUnix == -1 ? posWin : posUnix;
                posWin = posWin == -1 ? posUnix : posWin;
                return Math.min(posUnix, posWin) + 1;
            }
            char ch1 = filename.charAt(1);
            if (ch1 == ':') {
                ch0 = Character.toUpperCase(ch0);
                if (ch0 >= 'A' && ch0 <= 'Z') {
                    if (len == 2 || isSeparator(filename.charAt(2)) == false) {
                        return 2;
                    }
                    return 3;
                }
                return -1;

            } else if (isSeparator(ch0) && isSeparator(ch1)) {
                int posUnix = filename.indexOf(UNIX_SEPARATOR, 2);
                int posWin = filename.indexOf(WINDOWS_SEPARATOR, 2);
                if (posUnix == -1 && posWin == -1 || posUnix == 2 || posWin == 2) {
                    return -1;
                }
                posUnix = posUnix == -1 ? posWin : posUnix;
                posWin = posWin == -1 ? posUnix : posWin;
                return Math.min(posUnix, posWin) + 1;
            } else {
                return isSeparator(ch0) ? 1 : 0;
            }
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Checks if the character is a separator.
     *
     * @param ch  the character to check
     * @return true if it is a separator character
     */
    private static boolean isSeparator(char ch) {
        return ch == UNIX_SEPARATOR || ch == WINDOWS_SEPARATOR;
    }

    /**
     * Gets the extension of a file
     */
    public static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot > -1) {
            return fileName.substring(dot + 1);
        } else {
            return null;
        }
    }
}
