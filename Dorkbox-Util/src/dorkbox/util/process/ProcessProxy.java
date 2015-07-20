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
package dorkbox.util.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public
class ProcessProxy extends Thread {

    private final InputStream is;
    private final OutputStream os;

    // when reading from the stdin and outputting to the process
    public
    ProcessProxy(String processName, InputStream inputStreamFromConsole, OutputStream outputStreamToProcess) {
        this.is = inputStreamFromConsole;
        this.os = outputStreamToProcess;

        setName(processName);
        setDaemon(true);
    }

    public
    void close() {
        try {
            this.is.close();
        } catch (IOException e) {
        }
    }

    @Override
    public
    void run() {
        try {
            // this thread will read until there is no more data to read. (this is generally what you want)
            // the stream will be closed when the process closes it (usually on exit)
            int readInt;

            if (this.os == null) {
                // just read so it won't block.
                while ((readInt = this.is.read()) != -1) {
                }
            }
            else {
                while ((readInt = this.is.read()) != -1) {
                    this.os.write(readInt);
                    // always flush
                    this.os.flush();
                }
            }
        } catch (IOException ignore) {
        } catch (IllegalArgumentException e) {
        } finally {
            try {
                if (this.os != null) {
                    this.os.flush(); // this goes to the console, so we don't want to close it!
                }
                this.is.close();
            } catch (IOException ignore) {
            }
        }
    }
}
