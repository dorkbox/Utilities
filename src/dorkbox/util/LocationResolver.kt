/*
 * Copyright 2010 dorkbox, llc
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dorkbox.os.OS;

/**
 * Convenience methods for working with resource/file/class locations
 */
public
class LocationResolver {
    private static final Pattern SLASH_PATTERN = Pattern.compile("\\\\");

    private static
    void log(String message) {
        System.err.println(prefix() + message);
    }

    /**
     * Normalizes the path. fixes %20 as spaces (in winxp at least). Converts \ -> /  (windows slash -> unix slash)
     *
     * @return a string pointing to the cleaned path
     * @throws IOException
     */
    private static
    String normalizePath(String path) throws IOException {
        // make sure the slashes are in unix format.
        path = SLASH_PATTERN.matcher(path)
                            .replaceAll("/");

        // Can have %20 as spaces (in winxp at least). need to convert to proper path from URL
        return URLDecoder.decode(path, "UTF-8");
    }

    /**
     * Retrieve the location of the currently loaded jar, or possibly null if it was compiled on the fly
     */
    public static
    File get() {
        return get(LocationResolver.class);
    }

    /**
     * Retrieve the location that this classfile was loaded from, or possibly null if the class was compiled on the fly
     */
    public static
    File get(Class<?> clazz) {
        // Get the location of this class
        ProtectionDomain pDomain = clazz.getProtectionDomain();
        CodeSource cSource = pDomain.getCodeSource();

        // file:/X:/workspace/XYZ/classes/  when it's in ide/flat
        // jar:/X:/workspace/XYZ/jarname.jar  when it's jar
        URL loc = cSource.getLocation();

        // we don't always have a protection domain (for example, when we compile classes on the fly, from memory)
        if (loc == null) {
            return null;
        }

        // Can have %20 as spaces (in winxp at least). need to convert to proper path from URL
        try {
            File file = new File(normalizePath(loc.getFile())).getAbsoluteFile()
                                                              .getCanonicalFile();
            return file;

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to decode file path!", e);
        } catch (IOException e) {
            throw new RuntimeException("Unable to get canonical file path!", e);
        }
    }

    /**
     * Retrieves a URL of a given resourceName. If the resourceName is a directory, the returned URL will be the URL for the directory.
     * </p>
     * This method searches the disk first (via new {@link File#File(String)}, then by {@link ClassLoader#getResource(String)}, then by
     * {@link ClassLoader#getSystemResource(String)}.
     *
     * @param resourceName the resource name to search for
     *
     * @return the URL for that given resource name
     */
    public static
    URL getResource(String resourceName) {
        try {
            resourceName = normalizePath(resourceName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        URL resource = null;

        // 1) maybe it's on disk? priority is disk
        File file = new File(resourceName);
        if (file.canRead()) {
            try {
                resource = file.toURI()
                               .toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        // 2) is it in the context classloader
        if (resource == null) {
            resource = Thread.currentThread()
                                     .getContextClassLoader()
                                     .getResource(resourceName);
        }

        // 3) is it in the system classloader
        if (resource == null) {
            // maybe it's in the system classloader?
            resource = ClassLoader.getSystemResource(resourceName);
        }

        // 4) look for it, and log the output (so we can find or debug it)
        if (resource == null) {
            try {
                searchResource(resourceName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return resource;
    }

    /**
     * Retrieves an enumeration of URLs of a given resourceName. If the resourceName is a directory, the returned list will be the URLs
     * of the contents of that directory. The first URL will always be the directory URL, as returned by {@link #getResource(String)}.
     * </p>
     * This method searches the disk first (via new {@link File#File(String)}, then by {@link ClassLoader#getResources(String)}, then by
     * {@link ClassLoader#getSystemResources(String)}.
     *
     * @param resourceName the resource name to search for
     *
     * @return the enumeration of URLs for that given resource name
     */
    public static
    Enumeration<URL> getResources(String resourceName) {
        try {
            resourceName = normalizePath(resourceName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Enumeration<URL> resources = null;
        try {
            // 1) maybe it's on disk? priority is disk
            File file = new File(resourceName);
            if (file.canRead()) {
                ArrayDeque<URL> urlList = new ArrayDeque<URL>(4);
                // add self always
                urlList.add(file.toURI()
                                .toURL());

                if (file.isDirectory()) {
                    // add urls of all children
                    File[] files = file.listFiles();
                    if (files != null) {
                        for (int i = 0, n = files.length; i < n; i++) {
                            urlList.add(files[i].toURI()
                                                .toURL());
                        }
                    }
                }
                resources = new Vector<URL>(urlList).elements();
            }

            // 2) is it in the context classloader
            if (resources == null) {
                resources = Thread.currentThread()
                                 .getContextClassLoader()
                                 .getResources(resourceName);
            }

            // 3) is it in the system classloader
            if (resources == null) {
                // maybe it's in the system classloader?
                resources = ClassLoader.getSystemResources(resourceName);

            }

            // 4) look for it, and log the output (so we can find or debug it)
            if (resources == null) {
                searchResource(resourceName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return resources;
    }

    /**
     * Retrieves the resource as a stream.
     * <p>
     * 1) checks the disk in the relative location to the executing app<br/>
     * 2) Checks the current thread context classloader <br/>
     * 3) Checks the Classloader system resource
     *
     * @param resourceName the name, including path information (Only '\' is valid as the path separator)
     *
     * @return the resource stream, if it could be found, otherwise null.
     */
    public static
    InputStream getResourceAsStream(String resourceName) {
        try {
            resourceName = normalizePath(resourceName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        InputStream resourceAsStream = null;

        // 1) maybe it's on disk? priority is disk
        if (new File(resourceName).canRead()) {
            try {
                resourceAsStream = new FileInputStream(resourceName);
            } catch (FileNotFoundException e) {
                // shouldn't happen, but if there is something wonky...
                e.printStackTrace();
            }
        }

        // 2) maybe it's in the context classloader
        if (resourceAsStream == null) {
            resourceAsStream = Thread.currentThread()
                                     .getContextClassLoader()
                                     .getResourceAsStream(resourceName);
        }

        // 3) maybe it's in the system classloader
        if (resourceAsStream == null) {
            resourceAsStream = ClassLoader.getSystemResourceAsStream(resourceName);
        }


        // 4) look for it, and log the output (so we can find or debug it)
        if (resourceAsStream == null) {
            try {
                searchResource(resourceName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return resourceAsStream;
    }


    // via RIVEN at JGO. CC0 as far as I can tell.
    public static
    void searchResource(String path) throws IOException {
        try {
            path = normalizePath(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Root> roots = new ArrayList<Root>();

        ClassLoader contextClassLoader = Thread.currentThread()
                                               .getContextClassLoader();

        if (contextClassLoader instanceof URLClassLoader) {
            URL[] urLs = ((URLClassLoader) contextClassLoader).getURLs();
            for (URL url : urLs) {
                roots.add(new Root(url));
            }

            System.err.println();
            log("SEARCHING: \"" + path + "\"");

            for (int attempt = 1; attempt <= 6; attempt++) {
                for (Root root : roots) {
                    if (root.search(path, attempt)) {
                        return;
                    }
                }
            }

            log("FAILED: failed to find anything like");
            log("               \"" + path + "\"");
            log("         in all classpath entries:");

            for (Root root : roots) {
                final File entry = root.entry;
                if (entry != null) {
                    log("               \"" + entry.getAbsolutePath() + "\"");
                }
            }
        }
        else {
            throw new IOException("Unable to search for '" + path + "' in the context classloader of type '" + contextClassLoader.getClass() +
                                  "'.  Please report this issue with as many specific details as possible (OS, Java version, application version");
        }
    }

    /**
     * List directory contents for a resource folder. Not recursive.
     * This is basically a brute-force implementation.
     * Works for regular files and also JARs.
     *
     * @author Greg Briggs
     * @param clazz Any java class that lives in the same place as the resources you want.
     * @param path Should end with "/", but not start with one.
     * @return Just the name of each member item, not the full paths.
     *
     *
     * @throws URISyntaxException
     * @throws IOException
     */
    String[] getDirectoryContents(Class clazz, String path) throws URISyntaxException, IOException {
        URL dirURL = clazz.getClassLoader().getResource(path);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            /* A file path: easy enough */
            return new File(dirURL.toURI()).list();
        }

        if (dirURL == null) {
            /*
             * In case of a jar file, we can't actually find a directory.
             * Have to assume the same jar as clazz.
             */
            String me = clazz.getName().replace(".", "/") + ".class";
            dirURL = clazz.getClassLoader().getResource(me);
        }

        if (dirURL.getProtocol()
                  .equals("jar")) {
            /* A JAR path */
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file

            JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
            Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
            Set<String> result = new HashSet<String>(); //avoid duplicates in case it is a subdirectory

            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(path)) { //filter according to the path
                    String entry = name.substring(path.length());
                    int checkSubdir = entry.indexOf("/");
                    if (checkSubdir >= 0) {
                        // if it is a subdirectory, we just return the directory name
                        entry = entry.substring(0, checkSubdir);
                    }
                    result.add(entry);
                }
            }
            return result.toArray(new String[result.size()]);
        }

        throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
    }


    @SuppressWarnings("Duplicates")
    private static
    class Root {
        final File entry;
        final List<String> resources = new ArrayList<String>();

        public
        Root(URL entry) throws IOException {
            this.entry = visitRoot(entry, resources);
        }

        public
        boolean search(String path, int attempt) {
            try {
                path = normalizePath(path);
            } catch (IOException e) {
                e.printStackTrace();
            }

            switch (attempt) {
                case 1: {
                    for (String resource : resources) {
                        if (path.equals(resource)) {
                            log("SUCCESS: found resource \"" + path + "\" in root: " + entry);
                            return true;
                        }
                    }
                    break;
                }


                case 2: {
                    for (String resource : resources) {
                        if (path.toLowerCase()
                                .equals(resource.toLowerCase())) {
                            log("FOUND: similarly named resource:");
                            log("               \"" + resource + "\"");
                            log("         in classpath entry:");
                            log("               \"" + entry + "\"");
                            log("         for access use:");
                            log("               getResourceAsStream(\"/" + resource + "\");");
                            return true;
                        }
                    }
                    break;
                }


                case 3: {
                    for (String resource : resources) {
                        String r1 = path;
                        String r2 = resource;

                        if (r1.contains("/")) {
                            r1 = r1.substring(r1.lastIndexOf('/') + 1);
                        }
                        if (r2.contains("/")) {
                            r2 = r2.substring(r2.lastIndexOf('/') + 1);
                        }

                        if (r1.equals(r2)) {
                            log("FOUND: mislocated resource:");
                            log("               \"" + resource + "\"");
                            log("         in classpath entry:");
                            log("               \"" + entry + "\"");
                            log("         for access use:");
                            log("               getResourceAsStream(\"/" + resource + "\");");
                            return true;
                        }
                    }
                    break;
                }


                case 4: {
                    for (String resource : resources) {
                        String r1 = path.toLowerCase();
                        String r2 = resource.toLowerCase();

                        if (r1.contains("/")) {
                            r1 = r1.substring(r1.lastIndexOf('/') + 1);
                        }
                        if (r2.contains("/")) {
                            r2 = r2.substring(r2.lastIndexOf('/') + 1);
                        }

                        if (r1.equals(r2)) {
                            log("FOUND: mislocated, similarly named resource:");
                            log("               \"" + resource + "\"");
                            log("         in classpath entry:");
                            log("               \"" + entry + "\"");
                            log("         for access use:");
                            log("               getResourceAsStream(\"/" + resource + "\");");
                            return true;
                        }
                    }

                    break;
                }


                case 5: {
                    for (String resource : resources) {
                        String r1 = path;
                        String r2 = resource;

                        if (r1.contains("/")) {
                            r1 = r1.substring(r1.lastIndexOf('/') + 1);
                        }
                        if (r2.contains("/")) {
                            r2 = r2.substring(r2.lastIndexOf('/') + 1);
                        }
                        if (r1.contains(".")) {
                            r1 = r1.substring(0, r1.lastIndexOf('.'));
                        }
                        if (r2.contains(".")) {
                            r2 = r2.substring(0, r2.lastIndexOf('.'));
                        }

                        if (r1.equals(r2)) {
                            log("FOUND: resource with different extension:");
                            log("               \"" + resource + "\"");
                            log("         in classpath entry:");
                            log("               \"" + entry + "\"");
                            log("         for access use:");
                            log("               getResourceAsStream(\"/" + resource + "\");");
                            return true;
                        }
                    }
                    break;
                }


                case 6: {
                    for (String resource : resources) {
                        String r1 = path.toLowerCase();
                        String r2 = resource.toLowerCase();

                        if (r1.contains("/")) {
                            r1 = r1.substring(r1.lastIndexOf('/') + 1);
                        }
                        if (r2.contains("/")) {
                            r2 = r2.substring(r2.lastIndexOf('/') + 1);
                        }
                        if (r1.contains(".")) {
                            r1 = r1.substring(0, r1.lastIndexOf('.'));
                        }
                        if (r2.contains(".")) {
                            r2 = r2.substring(0, r2.lastIndexOf('.'));
                        }

                        if (r1.equals(r2)) {
                            log("FOUND: similarly named resource with different extension:");
                            log("               \"" + resource + "\"");
                            log("         in classpath entry:");
                            log("               \"" + entry + "\"");
                            log("         for access use:");
                            log("               getResourceAsStream(\"/" + resource + "\");");
                            return true;
                        }
                    }

                    break;
                }

                default:
                    return false;
            }

            return false;
        }
    }


    private static
    File visitRoot(URL url, List<String> resources) throws IOException {
        if (!url.getProtocol()
                .equals("file")) {
            throw new IllegalStateException();
        }
        String path = url.getPath();

        if (OS.INSTANCE.isWindows()) {
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
        }

        File root = new File(path);
        if (!root.exists()) {
            log("failed to find classpath entry in filesystem: " + path);
            return null;
        }

        if (root.isDirectory()) {
            visitDir(normalizePath(root.getAbsolutePath()), root, resources);
        }
        else {
            final String s = root.getName()
                                 .toLowerCase();

            if (s.endsWith(".zip")) {
                visitZip(root, resources);
            }
            else if (s.endsWith(".jar")) {
                visitZip(root, resources);
            }
            else {
                log("unknown classpath entry type: " + path);
                return null;
            }
        }
        return root;
    }

    private static
    void visitDir(String root, File dir, Collection<String> out) {
        final File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    visitDir(root, file, out);
                }

                out.add(file.getAbsolutePath()
                            .replace('\\', '/')
                            .substring(root.length() + 1));
            }
        }
    }

    private static
    void visitZip(File jar, Collection<String> out) throws IOException {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(jar));
        while (true) {
            ZipEntry entry = zis.getNextEntry();
            if (entry == null) {
                break;
            }
            out.add(entry.getName()
                         .replace('\\', '/'));
        }
        zis.close();
    }

    private static
    String prefix() {
        return "[" + LocationResolver.class.getSimpleName() + "] ";
    }
}
