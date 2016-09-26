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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

@SuppressWarnings("unused")
public
class IO {
    /**
     * Convenient close for a stream.
     */
    @SuppressWarnings("Duplicates")
    public static
    void close(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException ioe) {
                System.err.println("Error closing the input stream:" + inputStream);
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Convenient close for a stream.
     */
    @SuppressWarnings("Duplicates")
    public static
    void closeQuietly(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Convenient close for a stream.
     */
    @SuppressWarnings("Duplicates")
    public static
    void close(OutputStream outputStream) {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException ioe) {
                System.err.println("Error closing the output stream:" + outputStream);
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Convenient close for a stream.
     */
    @SuppressWarnings("Duplicates")
    public static
    void closeQuietly(OutputStream outputStream) {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Convenient close for a Reader.
     */
    @SuppressWarnings("Duplicates")
    public static
    void close(Reader inputReader) {
        if (inputReader != null) {
            try {
                inputReader.close();
            } catch (IOException ioe) {
                System.err.println("Error closing input reader: " + inputReader);
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Convenient close for a Reader.
     */
    @SuppressWarnings("Duplicates")
    public static
    void closeQuietly(Reader inputReader) {
        if (inputReader != null) {
            try {
                inputReader.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Convenient close for a Writer.
     */
    @SuppressWarnings("Duplicates")
    public static
    void close(Writer outputWriter) {
        if (outputWriter != null) {
            try {
                outputWriter.close();
            } catch (IOException ioe) {
                System.err.println("Error closing output writer: " + outputWriter);
                ioe.printStackTrace();
            }
        }
    }

    /**
     * Convenient close for a Writer.
     */
    @SuppressWarnings("Duplicates")
    public static
    void closeQuietly(Writer outputWriter) {
        if (outputWriter != null) {
            try {
                outputWriter.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Copy the contents of the input stream to the output stream.
     * <p/>
     * DOES NOT CLOSE THE STEAMS!
     */
    public static
    <T extends OutputStream> T copyStream(InputStream inputStream, T outputStream) throws IOException {
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, read);
        }
        outputStream.flush();

        return outputStream;
    }

}
