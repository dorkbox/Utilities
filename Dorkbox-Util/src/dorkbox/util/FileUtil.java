package dorkbox.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
 */
public class FileUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

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


        String normalizedIn = FilenameUtils.normalize(in.getAbsolutePath());
        String normalizedout = FilenameUtils.normalize(out.getAbsolutePath());

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
    public static File concatFiles(File one, File two) throws IOException {
        if (one == null) {
            throw new IllegalArgumentException("one cannot be null.");
        }
        if (two == null) {
            throw new IllegalArgumentException("two cannot be null.");
        }

        String normalizedOne = FilenameUtils.normalize(one.getAbsolutePath());
        String normalizedTwo = FilenameUtils.normalize(two.getAbsolutePath());

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
    public static File moveFile(String in, String out) throws IOException {
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
    public static File moveFile(File in, File out) throws IOException {
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


        ZipInputStream inputStrem = new ZipInputStream(new FileInputStream(zipFile));
        try {
            while (true) {
                ZipEntry entry = inputStrem.getNextEntry();
                if (entry == null) {
                    break;
                }

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
                    Sys.copyStream(inputStrem, output);
                } finally {
                    output.close();
                }
            }
        } finally {
            inputStrem.close();
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
}
