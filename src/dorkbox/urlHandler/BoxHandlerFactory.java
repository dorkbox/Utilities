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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public
class BoxHandlerFactory implements URLStreamHandlerFactory {
    private final BoxHandler transparentJar;

    public
    BoxHandlerFactory(BoxHandler transparentJar) {
        this.transparentJar = transparentJar;
    }

    @Override
    public
    URLStreamHandler createURLStreamHandler(String protocol) {
        // transparent jar handler.
        if (BoxHandler.protocol.equals(protocol)) {
            return this.transparentJar;
        }
        else {
            // use the default URLStreamHandlers
            return null;
        }
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
