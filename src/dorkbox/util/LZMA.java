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

import lzma.sdk.lzma.Decoder;
import lzma.sdk.lzma.Encoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class LZMA {
    // LZMA (Java) 4.61  2008-11-23
    // http://jponge.github.com/lzma-java/

    public static final void encode(long fileSize, InputStream input, OutputStream output) throws IOException  {
        try {
            final Encoder encoder = new Encoder();

            if (!encoder.setDictionarySize(1 << 23)) {
                throw new RuntimeException("Incorrect dictionary size");
            }
            if (!encoder.setNumFastBytes(273)) {
                throw new RuntimeException("Incorrect -fb value");
            }
            if (!encoder.setMatchFinder(1)) {
                throw new RuntimeException("Incorrect -mf value");
            }
            if (!encoder.setLcLpPb(3, 0, 2)) {
                throw new RuntimeException("Incorrect -lc or -lp or -pb value");
            }
            encoder.setEndMarkerMode(false);
            encoder.writeCoderProperties(output);

            for (int ii = 0; ii < 8; ii++) {
                output.write((int)(fileSize >>> 8 * ii) & 0xFF);
            }

            encoder.code(input, output, -1, -1, null);
        } finally {
            input.close();
            output.flush();
        }
    }

    public static final ByteArrayOutputStream decode(InputStream input) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8192);
        decode(input, byteArrayOutputStream);
        return byteArrayOutputStream;
    }

    public static final void decode(InputStream input, OutputStream output) throws IOException {
        try {
            int propertiesSize = 5;
            byte[] properties = new byte[propertiesSize];
            if (input.read(properties, 0, propertiesSize) != propertiesSize) {
                throw new IOException("input .lzma file is too short");
            }

            Decoder decoder = new Decoder();
            if (!decoder.setDecoderProperties(properties)) {
                throw new IOException("Incorrect stream properties");
            }

            long outSize = 0;
            for (int i = 0; i < 8; i++)
            {
                int v = input.read();
                if (v < 0) {
                    throw new IOException("Can't read stream size");
                }
                outSize |= (long)v << 8 * i;
            }
            if (!decoder.code(input, output, outSize)) {
                throw new IOException("Error in data stream");
            }
        } finally {
            output.flush();
            input.close();
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
