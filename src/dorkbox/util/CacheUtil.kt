/*
 * Copyright 2016 dorkbox, llc
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
package dorkbox.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import dorkbox.os.OS;

public
class CacheUtil {

    private static final ThreadLocal<MessageDigest> digestLocal = ThreadLocal.withInitial(()->{
        try {
            return MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to initialize hash algorithm. SHA1 digest doesn't exist?!? (This should not happen");
        }
    });

    private final String tempDir;

    public static
    void clear(String tempDir) {
        new CacheUtil(tempDir).clear();
    }

    public CacheUtil() {
        this("cache");
    }

    public CacheUtil(String tempDir) {
        this.tempDir = tempDir;
    }

    /**
     * Clears ALL saved files in the cache
     */
    public
    void clear() {
        // deletes all of the files (recursively) in the specified location. If the directory is empty (no locked files), then the
        // directory is also deleted.
        FileUtil.delete(new File(OS.INSTANCE.getTEMP_DIR(), tempDir));
    }


    /**
     * Checks to see if the specified file is in the cache. NULL if it is not, otherwise specifies a location on disk.
     * <p>
     * This cache is not persisted across runs.
     */
    public
    File check(final File file) {
        if (file == null) {
            throw new NullPointerException("file");
        }

        // if we already have this fileName, reuse it
        return check(file.getAbsolutePath());
    }

    /**
     * Checks to see if the specified file is in the cache. NULL if it is not, otherwise specifies a location on disk.
     */
    public
    File check(final String fileName) {
        if (fileName == null) {
            throw new NullPointerException("fileName");
        }

        // if we already have this fileName, reuse it
        File newFile = makeCacheFile(fileName);

        // if this file already exists (via HASH), we just reuse what is saved on disk.
        if (newFile.canRead() && newFile.isFile()) {
            return newFile;
        }

        return null;
    }

    /**
     * Checks to see if the specified URL is in the cache. NULL if it is not, otherwise specifies a location on disk.
     */
    public
    File check(final URL fileResource) {
        if (fileResource == null) {
            throw new NullPointerException("fileResource");
        }

        return check(fileResource.getPath());
    }

    /**
     * Checks to see if the specified stream (based on the hash of the input stream) is in the cache. NULL if it is not, otherwise
     * specifies a location on disk.
     */
    public
    File check(final InputStream fileStream) throws IOException {
        if (fileStream == null) {
            throw new NullPointerException("fileStream");
        }

        return check(null, fileStream);
    }

    /**
     * Checks to see if the specified name is in the cache. NULL if it is not, otherwise specifies a location on disk. If the
     * cacheName is NULL, it will use a HASH of the fileStream
     */
    public
    File check(String cacheName, final InputStream fileStream) throws IOException {
        if (fileStream == null) {
            throw new NullPointerException("fileStream");
        }

        if (cacheName == null) {
            cacheName = createNameAsHash(fileStream);
        }

        // if we already have this fileName, reuse it
        File newFile = makeCacheFile(cacheName);

        // if this file already exists (via HASH), we just reuse what is saved on disk.
        if (newFile.canRead() && newFile.isFile()) {
            return newFile;
        }

        return null;
    }













    /**
     * Saves the name of the file in a cache, based on the file's name.
     */
    public
    File save(final File file) throws IOException {
        return save(file.getAbsolutePath(), file);
    }

    /**
     * Saves the name of the file in a cache, based on the specified name. If cacheName is NULL, it will use the file's name.
     */
    public
    File save(String cacheName, final File file) throws IOException {
        if (cacheName == null) {
            cacheName = file.getAbsolutePath();
        }
        return save(cacheName, file.getAbsolutePath());
    }

    /**
     * Saves the name of the file in a cache, based on the specified name.
     */
    public
    File save(final String fileName) throws IOException {
        return save(null, fileName);
    }

    /**
     * Saves the name of the file in a cache, based on name. If cacheName is NULL, it will use the file's name.
     *
     * @return the newly create cache file, or an IOException if there were problems
     */
    public
    File save(String cacheName, final String fileName) throws IOException {
        if (cacheName == null) {
            cacheName = fileName;
        }

        // if we already have this fileName, reuse it
        File newFile = makeCacheFile(cacheName);

        // if this file already exists (via HASH), we just reuse what is saved on disk.
        if (newFile.canRead() && newFile.isFile()) {
            return newFile;
        }


        // is file sitting on drive
        File iconTest = new File(fileName);
        if (iconTest.isFile()) {
            if (!iconTest.canRead()) {
                throw new IOException("File exists but unable to read source file " + fileName);
            }

            // have to copy the resource to the cache
            FileUtil.copyFile(iconTest, newFile);

            return newFile;
        }
        else {
            // suck it out of a URL/Resource (with debugging if necessary)
            final URL systemResource = LocationResolver.getResource(fileName);

            if (systemResource == null) {
                throw new IOException("Unable to load URL resource " + fileName);
            }

            InputStream inStream = systemResource.openStream();

            // saves the file into our temp location, uses HASH of cacheName
            return makeFileViaStream(cacheName, inStream);
        }
    }

    /**
     * Saves the name of the URL in a cache, based on it's path.
     */
    public
    File save(final URL fileResource) throws IOException {
        return save(null, fileResource);
    }

    /**
     * Saves the name of the URL in a cache, based on the specified name. If cacheName is NULL, it will use the URL's path.
     */
    public
    File save(String cacheName, final URL fileResource) throws IOException {
        if (cacheName == null) {
            cacheName = fileResource.getPath();
        }

        // if we already have this fileName, reuse it
        File newFile = makeCacheFile(cacheName);

        // if this file already exists (via HASH), we just reuse what is saved on disk.
        if (newFile.canRead() && newFile.isFile()) {
            return newFile;
        }

        InputStream inStream = fileResource.openStream();

        // saves the file into our temp location, uses HASH of cacheName
        return makeFileViaStream(cacheName, inStream);
    }

    /**
     * This caches the data based on the HASH of the input stream.
     */
    public
    File save(final InputStream fileStream) throws IOException {
        if (fileStream == null) {
            throw new NullPointerException("fileStream");
        }

        return save(null, fileStream);
    }

    /**
     * Saves the name of the file in a cache, based on the cacheName. If the cacheName is NULL, it will use a HASH of the fileStream
     * as the name.
     */
    public
    File save(String cacheName, final InputStream fileStream) throws IOException {
        if (cacheName == null) {
            cacheName = createNameAsHash(fileStream);
        }

        // if we already have this fileName, reuse it
        File newFile = makeCacheFile(cacheName);

        // if this file already exists (via HASH), we just reuse what is saved on disk.
        if (newFile.canRead() && newFile.isFile()) {
            return newFile;
        }

        return makeFileViaStream(cacheName, fileStream);
    }












    /**
     * must be called from synchronized block!
     *
     * @param cacheName needs name+extension for the resource
     * @param resourceStream the resource to copy to a file on disk
     *
     * @return the full path of the resource copied to disk, or NULL if invalid
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private
    File makeFileViaStream(final String cacheName, final InputStream resourceStream) throws IOException {
        if (resourceStream == null) {
            throw new NullPointerException("resourceStream");
        }

        if (cacheName == null) {
            throw new NullPointerException("cacheName");
        }

        File newFile = makeCacheFile(cacheName);

        // if this file already exists (via HASH), we just reuse what is saved on disk.
        if (newFile.canRead() && newFile.isFile()) {
            return newFile.getAbsoluteFile();
        }

        OutputStream outStream = null;
        try {
            int read;
            byte[] buffer = new byte[2048];
            outStream = new FileOutputStream(newFile);

            while ((read = resourceStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, read);
            }
        } catch (IOException e) {
            // Send up exception
            String message = "Unable to copy '" + cacheName + "' to temporary location: '" + newFile.getAbsolutePath() + "'";
            throw new IOException(message, e);
        } finally {
            try {
                resourceStream.close();
            } catch (Exception ignored) {
            }
            try {
                if (outStream != null) {
                    outStream.close();
                }
            } catch (Exception ignored) {
            }
        }

        //get the name of the new file
        return newFile.getAbsoluteFile();
    }

    /**
     * @param cacheName the name of the file to use in the cache. This file name can use invalid file name characters
     *
     * @return the file on disk represented by the file name
     */
    public
    File create(final String cacheName) {
        return makeCacheFile(cacheName);
    }

    // creates the file that will be cached. It may, or may not already exist
    // must be called from synchronized block!
    // never returns null
    private
    File makeCacheFile(final String cacheName) {
        if (cacheName == null) {
            throw new NullPointerException("cacheName");
        }

        File saveDir = new File(OS.INSTANCE.getTEMP_DIR(), tempDir);

        // can be wimpy, only one at a time
        String hash = hashName(cacheName);
        String extension = FileUtil.INSTANCE.getExtension(cacheName);
        if (extension.isEmpty()) {
            extension = "cache";
        }

        File newFile = new File(saveDir, hash + '.' + extension).getAbsoluteFile();
        // make whatever dirs we need to.
        //noinspection ResultOfMethodCallIgnored
        newFile.getParentFile().mkdirs();

        return newFile;
    }

    // hashed name to prevent invalid file names from being used
    private static
    String hashName(final String name) {
        // figure out the fileName
        byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
        MessageDigest digest = digestLocal.get();

        digest.reset();
        digest.update(bytes);

        // convert to alpha-numeric. see https://stackoverflow.com/questions/29183818/why-use-tostring32-and-not-tostring36
        return new BigInteger(1, digest.digest()).toString(32).toUpperCase(Locale.US);
    }

    // this is if we DO NOT have a file name. We hash the resourceStream bytes to base the name on that. The extension will be ".cache"
    public static
    String createNameAsHash(final InputStream resourceStream) throws IOException {
        MessageDigest digest = digestLocal.get();

        digest.reset();

        try {
            // we have to set the cache name based on the hash of the input stream ONLY...
            final ByteArrayOutputStream outStream = new ByteArrayOutputStream(4096); // will resize if necessary

            int read;
            byte[] buffer = new byte[2048];

            while ((read = resourceStream.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
                outStream.write(buffer, 0, read);
            }

            // convert to alpha-numeric. see https://stackoverflow.com/questions/29183818/why-use-tostring32-and-not-tostring36
            return new BigInteger(1, digest.digest()).toString(32).toUpperCase(Locale.US) + ".cache";
        } catch (IOException e) {
            // Send up exception
            String message = "Unable to copy InputStream to memory.";
            throw new IOException(message, e);
        } finally {
            try {
                resourceStream.close();
            } catch (Exception ignored) {
            }
        }
    }
}
