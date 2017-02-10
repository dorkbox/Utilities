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
package dorkbox.urlHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * A 'Box' URL is nothing like a JAR/ZIP, HOWEVER, it appears as though it is a jar/zip file.
 */

public
class BoxURLConnection extends URLConnection {

    public
    BoxURLConnection(URL url) {
        super(url);
    }

    /**
     * @return the base name of the url. This will be the internal container (inside the main jar file) that actually contains our resource.
     * This will be empty for class files.
     */
    public
    String getContainerName() {
        String spec = this.url.getPath();
        int separator = spec.indexOf(BoxHandler.jarUrlSeperator);
        int length = spec.length();

        if (separator > 0 && separator != length) {
            if (spec.charAt(0) == '/') {
                if (spec.charAt(separator - 1) == '/') {
                    String substring = spec.substring(1, separator - 1);
                    return substring;
                }
                else {
                    String substring = spec.substring(1, separator);
                    return substring;
                }
            }
            else {
                if (spec.charAt(separator - 1) == '/') {
                    String substring = spec.substring(0, separator - 1);
                    return substring;
                }
                else {
                    String substring = spec.substring(0, separator);
                    return substring;
                }
            }
        }
        else {
            return "";
        }
    }

    /**
     * @return the name of the entry that is nested inside an internal resource. This would be the name of a file, where the base URL would
     * be the internal resource container.
     */
    public
    String getResourceName() {
        String spec = this.url.getPath();
        int separator = spec.indexOf(BoxHandler.jarUrlSeperator);

        if (separator > -1 && separator != spec.length()) {
            if (spec.charAt(separator + 1) == '/') {
                return spec.substring(separator + 2);
            }
            else {
                return spec.substring(separator + 1);
            }

        }
        else {
            return "";
        }
    }

    @Override
    public
    void connect() throws IOException {
        this.connected = true;
    }

    @Override
    public
    int getContentLength() {
        // if we are inside our box file, this will return -1, so inputstreams will be used (which they have to be...)
        // if we return anything other than -1, then our box resource will try to be opened like a file (which we don't want)
        return -1;
    }

    @Override
    public
    long getLastModified() {
        return 0;
    }

    /**
     * Loads the resources stream, if applicable. You cannot load classes using this method
     */
    @Override
    public
    InputStream getInputStream() throws IOException {
        String path = this.url.getPath();

        int length = BoxHandler.protocolLength;
        StringBuilder stringBuilder = new StringBuilder(path.length() + length);
        stringBuilder.append(BoxHandler.protocolFull);
        if (path.charAt(0) == '/') {
            stringBuilder.deleteCharAt(length - 1);
        }
        stringBuilder.append(path);

        InputStream is = getClass().getClassLoader()
                                   .getResourceAsStream(stringBuilder.toString());
        return is;
    }

    @Override
    public final
    Object clone() throws java.lang.CloneNotSupportedException {
        throw new java.lang.CloneNotSupportedException();
    }

    public final
    void writeObject(ObjectOutputStream out) throws java.io.IOException {
        throw new java.io.NotSerializableException();
    }

    public final
    void readObject(ObjectInputStream in) throws java.io.IOException {
        throw new java.io.NotSerializableException();
    }
}
