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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Loads the specified library, extracting it from the jar, if necessary
 */
public
class NativeLoader {

    public static
    File extractLibrary(final String sourceFileName, final String destinationDirectory, final String destinationName, String version) throws IOException {
        try {
            String suffix;
            if (OS.isLinux()) {
                suffix = ".so";
            }
            else if (OS.isWindows()) {
                suffix = ".dll";
            }
            else {
                suffix = ".dylib";
            }

            final String outputFileName;
            if (version == null) {
                outputFileName = destinationName + suffix;
            }
            else {
                outputFileName = destinationName + "." + version + suffix;
            }

            final File file = new File(destinationDirectory, outputFileName);
            if (!file.canRead() || file.length() == 0 || !file.canExecute()) {
                // now we copy it out
                final InputStream inputStream = LocationResolver.getResourceAsStream(sourceFileName);

                OutputStream outStream = null;
                try {
                    outStream = new FileOutputStream(file);

                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = inputStream.read(buffer)) > 0) {
                        outStream.write(buffer, 0, read);
                    }

                    outStream.flush();
                    outStream.close();
                    outStream = null;
                } finally {
                    try {
                        inputStream.close();
                    } catch (Exception ignored) {
                    }
                    try {
                        if (outStream != null) {
                            outStream.close();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            return file;
        } catch (Exception e) {
            throw new IOException("Error extracting library: " + sourceFileName, e);
        }
    }

    public static
    void loadLibrary(final File file) {
        // inject into the correct classloader
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public
            Object run() {
                System.load(file.getAbsolutePath());
                return null;
            }
        });
    }

    private
    NativeLoader() {
    }
}
