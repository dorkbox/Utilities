/*
 * Copyright 2023 dorkbox, llc
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
package dorkbox.util

import dorkbox.os.OS.isWindows
import java.io.*
import java.net.*
import java.util.*
import java.util.jar.*
import java.util.regex.*
import java.util.zip.*

/**
 * Convenience methods for working with resource/file/class locations
 */
class LocationResolver {
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
    @Throws(URISyntaxException::class, IOException::class)
    fun getDirectoryContents(clazz: Class<*>, path: String): Array<String> {
        var dirURL = clazz.classLoader.getResource(path)
        if (dirURL != null && dirURL.protocol == "file") {/* A file path: easy enough */
            return File(dirURL.toURI()).list()!!
        }
        if (dirURL == null) {/*
             * In case of a jar file, we can't actually find a directory.
             * Have to assume the same jar as clazz.
             */
            val me = clazz.name.replace(".", "/") + ".class"
            dirURL = clazz.classLoader.getResource(me)
        }
        if (dirURL!!.protocol == "jar") {/* A JAR path */
            val jarPath = dirURL.path.substring(5, dirURL.path.indexOf("!")) //strip out only the JAR file
            val jar = JarFile(URLDecoder.decode(jarPath, "UTF-8"))
            val entries = jar.entries() //gives ALL entries in jar
            val result: MutableSet<String> = HashSet() //avoid duplicates in case it is a subdirectory
            while (entries.hasMoreElements()) {
                val name = entries.nextElement().name
                if (name.startsWith(path)) { //filter according to the path
                    var entry = name.substring(path.length)
                    val checkSubdir = entry.indexOf("/")
                    if (checkSubdir >= 0) {
                        // if it is a subdirectory, we just return the directory name
                        entry = entry.substring(0, checkSubdir)
                    }
                    result.add(entry)
                }
            }
            return result.toTypedArray<String>()
        }
        throw UnsupportedOperationException("Cannot list files for URL $dirURL")
    }

    private class Root(entry: URL) {
        val entry: File?
        val resources: MutableList<String> = ArrayList()

        init {
            this.entry = visitRoot(entry, resources)
        }

        fun search(path: String, attempt: Int): Boolean {
            var path = path
            try {
                path = normalizePath(path)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            when (attempt) {
                1 -> {
                    for (resource in resources) {
                        if (path == resource) {
                            log("SUCCESS: found resource \"$path\" in root: $entry")
                            return true
                        }
                    }
                }

                2 -> {
                    for (resource in resources) {
                        if (path.lowercase(Locale.getDefault()) == resource.lowercase(Locale.getDefault())) {
                            log("FOUND: similarly named resource:")
                            log("               \"$resource\"")
                            log("         in classpath entry:")
                            log("               \"$entry\"")
                            log("         for access use:")
                            log("               getResourceAsStream(\"/$resource\");")
                            return true
                        }
                    }
                }

                3 -> {
                    for (resource in resources) {
                        var r1 = path
                        var r2 = resource
                        if (r1.contains("/")) {
                            r1 = r1.substring(r1.lastIndexOf('/') + 1)
                        }
                        if (r2.contains("/")) {
                            r2 = r2.substring(r2.lastIndexOf('/') + 1)
                        }
                        if (r1 == r2) {
                            log("FOUND: mislocated resource:")
                            log("               \"$resource\"")
                            log("         in classpath entry:")
                            log("               \"$entry\"")
                            log("         for access use:")
                            log("               getResourceAsStream(\"/$resource\");")
                            return true
                        }
                    }
                }

                4 -> {
                    for (resource in resources) {
                        var r1 = path.lowercase(Locale.getDefault())
                        var r2 = resource.lowercase(Locale.getDefault())
                        if (r1.contains("/")) {
                            r1 = r1.substring(r1.lastIndexOf('/') + 1)
                        }
                        if (r2.contains("/")) {
                            r2 = r2.substring(r2.lastIndexOf('/') + 1)
                        }
                        if (r1 == r2) {
                            log("FOUND: mislocated, similarly named resource:")
                            log("               \"$resource\"")
                            log("         in classpath entry:")
                            log("               \"$entry\"")
                            log("         for access use:")
                            log("               getResourceAsStream(\"/$resource\");")
                            return true
                        }
                    }
                }

                5 -> {
                    for (resource in resources) {
                        var r1 = path
                        var r2 = resource
                        if (r1.contains("/")) {
                            r1 = r1.substring(r1.lastIndexOf('/') + 1)
                        }
                        if (r2.contains("/")) {
                            r2 = r2.substring(r2.lastIndexOf('/') + 1)
                        }
                        if (r1.contains(".")) {
                            r1 = r1.substring(0, r1.lastIndexOf('.'))
                        }
                        if (r2.contains(".")) {
                            r2 = r2.substring(0, r2.lastIndexOf('.'))
                        }
                        if (r1 == r2) {
                            log("FOUND: resource with different extension:")
                            log("               \"$resource\"")
                            log("         in classpath entry:")
                            log("               \"$entry\"")
                            log("         for access use:")
                            log("               getResourceAsStream(\"/$resource\");")
                            return true
                        }
                    }
                }

                6 -> {
                    for (resource in resources) {
                        var r1 = path.lowercase(Locale.getDefault())
                        var r2 = resource.lowercase(Locale.getDefault())
                        if (r1.contains("/")) {
                            r1 = r1.substring(r1.lastIndexOf('/') + 1)
                        }
                        if (r2.contains("/")) {
                            r2 = r2.substring(r2.lastIndexOf('/') + 1)
                        }
                        if (r1.contains(".")) {
                            r1 = r1.substring(0, r1.lastIndexOf('.'))
                        }
                        if (r2.contains(".")) {
                            r2 = r2.substring(0, r2.lastIndexOf('.'))
                        }
                        if (r1 == r2) {
                            log("FOUND: similarly named resource with different extension:")
                            log("               \"$resource\"")
                            log("         in classpath entry:")
                            log("               \"$entry\"")
                            log("         for access use:")
                            log("               getResourceAsStream(\"/$resource\");")
                            return true
                        }
                    }
                }

                else -> return false
            }
            return false
        }
    }

    companion object {
        /**
         * Gets the version number.
         */
        val version = Sys.version

        private val SLASH_PATTERN = Pattern.compile("\\\\")

        private fun log(message: String) {
            System.err.println(prefix() + message)
        }

        /**
         * Normalizes the path. fixes %20 as spaces (in winxp at least). Converts \ -> /  (windows slash -> unix slash)
         *
         * @return a string pointing to the cleaned path
         * @throws IOException
         */
        @Throws(IOException::class)
        private fun normalizePath(path: String): String {
            // make sure the slashes are in unix format.
            val path = SLASH_PATTERN.matcher(path).replaceAll("/")

            // Can have %20 as spaces (in winxp at least). need to convert to proper path from URL
            return URLDecoder.decode(path, "UTF-8")
        }
        /**
         * Retrieve the location that this classfile was loaded from, or possibly null if the class was compiled on the fly
         */
        /**
         * Retrieve the location of the currently loaded jar, or possibly null if it was compiled on the fly
         */
        @JvmOverloads
        operator fun get(clazz: Class<*> = LocationResolver::class.java): File? {
            // Get the location of this class
            val pDomain = clazz.protectionDomain
            val cSource = pDomain.codeSource

            // file:/X:/workspace/XYZ/classes/  when it's in ide/flat
            // jar:/X:/workspace/XYZ/jarname.jar  when it's jar
            val loc = cSource.location ?: return null

            // we don't always have a protection domain (for example, when we compile classes on the fly, from memory)

            // Can have %20 as spaces (in winxp at least). need to convert to proper path from URL
            return try {
                File(normalizePath(loc.file)).absoluteFile.canonicalFile
            } catch (e: UnsupportedEncodingException) {
                throw RuntimeException("Unable to decode file path!", e)
            } catch (e: IOException) {
                throw RuntimeException("Unable to get canonical file path!", e)
            }
        }

        /**
         * Retrieves a URL of a given resourceName. If the resourceName is a directory, the returned URL will be the URL for the directory.
         *
         * This method searches the disk first (via new [File.File], then by [ClassLoader.getResource], then by
         * [ClassLoader.getSystemResource].
         *
         * @param resourceName the resource name to search for
         *
         * @return the URL for that given resource name
         */
        fun getResource(resourceName: String): URL? {
            var resourceName = resourceName
            try {
                resourceName = normalizePath(resourceName)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            var resource: URL? = null

            // 1) maybe it's on disk? priority is disk
            val file = File(resourceName)
            if (file.canRead()) {
                try {
                    resource = file.toURI().toURL()
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                }
            }

            // 2) is it in the context classloader
            if (resource == null) {
                resource = Thread.currentThread().contextClassLoader.getResource(resourceName)
            }

            // 3) is it in the system classloader
            if (resource == null) {
                // maybe it's in the system classloader?
                resource = ClassLoader.getSystemResource(resourceName)
            }

            // 4) look for it, and log the output (so we can find or debug it)
            if (resource == null) {
                try {
                    searchResource(resourceName)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            return resource
        }

        /**
         * Retrieves an enumeration of URLs of a given resourceName. If the resourceName is a directory, the returned list will be the URLs
         * of the contents of that directory. The first URL will always be the directory URL, as returned by [.getResource].
         *
         * This method searches the disk first (via new [File.File], then by [ClassLoader.getResources], then by
         * [ClassLoader.getSystemResources].
         *
         * @param resourceName the resource name to search for
         *
         * @return the enumeration of URLs for that given resource name
         */
        fun getResources(resourceName: String): Enumeration<URL>? {
            var resourceName = resourceName

            try {
                resourceName = normalizePath(resourceName)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            var resources: Enumeration<URL>? = null
            try {
                // 1) maybe it's on disk? priority is disk
                val file = File(resourceName)
                if (file.canRead()) {
                    val urlList = ArrayDeque<URL>(4)
                    // add self always
                    urlList.add(
                        file.toURI().toURL()
                    )
                    if (file.isDirectory) {
                        // add urls of all children
                        val files = file.listFiles()
                        if (files != null) {
                            var i = 0
                            val n = files.size
                            while (i < n) {
                                urlList.add(
                                    files[i].toURI().toURL()
                                )
                                i++
                            }
                        }
                    }
                    resources = Vector(urlList).elements()
                }

                // 2) is it in the context classloader
                if (resources == null) {
                    resources = Thread.currentThread().contextClassLoader.getResources(resourceName)
                }

                // 3) is it in the system classloader
                if (resources == null) {
                    // maybe it's in the system classloader?
                    resources = ClassLoader.getSystemResources(resourceName)
                }

                // 4) look for it, and log the output (so we can find or debug it)
                if (resources == null) {
                    searchResource(resourceName) // can throw an exception
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return resources
        }

        /**
         * Retrieves the resource as a stream.
         *
         *
         * 1) checks the disk in the relative location to the executing app<br></br>
         * 2) Checks the current thread context classloader <br></br>
         * 3) Checks the Classloader system resource
         *
         * @param resourceName the name, including path information (Only '\' is valid as the path separator)
         *
         * @return the resource stream, if it could be found, otherwise null.
         */
        fun getResourceAsStream(resourceName: String): InputStream? {
            var resourceName = resourceName
            try {
                resourceName = normalizePath(resourceName)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            var resourceAsStream: InputStream? = null

            // 1) maybe it's on disk? priority is disk
            if (File(resourceName).canRead()) {
                try {
                    resourceAsStream = FileInputStream(resourceName)
                } catch (e: FileNotFoundException) {
                    // shouldn't happen, but if there is something wonky...
                    e.printStackTrace()
                }
            }

            // 2) maybe it's in the context classloader
            if (resourceAsStream == null) {
                resourceAsStream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourceName)
            }

            // 3) maybe it's in the system classloader
            if (resourceAsStream == null) {
                resourceAsStream = ClassLoader.getSystemResourceAsStream(resourceName)
            }


            // 4) look for it, and log the output (so we can find or debug it)
            if (resourceAsStream == null) {
                try {
                    searchResource(resourceName)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            return resourceAsStream
        }

        // via RIVEN at JGO. CC0 as far as I can tell.
        @Throws(IOException::class)
        fun searchResource(path: String) {
            var path = path

            try {
                path = normalizePath(path)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            val roots: MutableList<Root> = ArrayList()
            val contextClassLoader = Thread.currentThread().contextClassLoader

            if (contextClassLoader is URLClassLoader) {
                val urLs = contextClassLoader.urLs
                for (url in urLs) {
                    roots.add(Root(url))
                }
                System.err.println()
                log("SEARCHING: \"$path\"")

                for (attempt in 1..6) {
                    for (root in roots) {
                        if (root.search(path, attempt)) {
                            return
                        }
                    }
                }

                log("FAILED: failed to find anything like")
                log("               \"$path\"")
                log("         in all classpath entries:")
                for (root in roots) {
                    val entry = root.entry
                    if (entry != null) {
                        log("               \"" + entry.absolutePath + "\"")
                    }
                }
            } else {
                throw IOException(
                    "Unable to search for '" + path + "' in the context classloader of type '" + contextClassLoader.javaClass + "'.  Please report this issue with as many specific details as possible (OS, Java version, application version"
                )
            }
        }

        @Throws(IOException::class)
        private fun visitRoot(url: URL, resources: MutableList<String>): File? {
            check(url.protocol == "file")
            var path = url.path
            if (isWindows) {
                if (path.startsWith("/")) {
                    path = path.substring(1)
                }
            }

            val root = File(path)
            if (!root.exists()) {
                log("failed to find classpath entry in filesystem: $path")
                return null
            }

            if (root.isDirectory) {
                visitDir(normalizePath(root.absolutePath), root, resources)
            } else {
                val s = root.name.lowercase(Locale.getDefault())
                if (s.endsWith(".zip")) {
                    visitZip(root, resources)
                } else if (s.endsWith(".jar")) {
                    visitZip(root, resources)
                } else {
                    log("unknown classpath entry type: $path")
                    return null
                }
            }

            return root
        }

        private fun visitDir(root: String, dir: File, out: MutableCollection<String>) {
            val files = dir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        visitDir(root, file, out)
                    }
                    out.add(
                        file.absolutePath.replace('\\', '/').substring(root.length + 1)
                    )
                }
            }
        }

        @Throws(IOException::class)
        private fun visitZip(jar: File, out: MutableCollection<String>) {
            val zis = ZipInputStream(FileInputStream(jar))
            while (true) {
                val entry = zis.nextEntry ?: break
                out.add(
                    entry.name.replace('\\', '/')
                )
            }
            zis.close()
        }

        private fun prefix(): String {
            return "[" + LocationResolver::class.java.simpleName + "] "
        }
    }
}
