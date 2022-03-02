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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.LZMAInputStream;
import org.tukaani.xz.LZMAOutputStream;

public class LZMA {
    // https://tukaani.org/xz/java.html

    public static final void encode(InputStream input, OutputStream output) throws IOException  {
        try (OutputStream compressionStream = new LZMAOutputStream(output, new LZMA2Options(3), true)) {
            IO.copyStream(input, compressionStream);
        }
    }

    public static final ByteArrayOutputStream decode(InputStream input) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8192);

        try (LZMAInputStream compressedStream = new LZMAInputStream(input)) {
            IO.copyStream(compressedStream, byteArrayOutputStream);
        }

        return byteArrayOutputStream;
    }

    public static final void decode(InputStream input, OutputStream output) throws IOException {
        try (LZMAInputStream compressedStream = new LZMAInputStream(input)) {
            IO.copyStream(compressedStream, output);
        }
    }

    @Override
    public final Object clone() throws java.lang.CloneNotSupportedException {
        throw new java.lang.CloneNotSupportedException();
    }

    private final void writeObject(ObjectOutputStream out) throws java.io.IOException {
        throw new java.io.NotSerializableException();
    }

    private final void readObject(ObjectInputStream in) throws java.io.IOException {
        throw new java.io.NotSerializableException();
    }
}
