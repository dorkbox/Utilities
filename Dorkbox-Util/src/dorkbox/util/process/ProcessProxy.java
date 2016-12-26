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
import java.util.concurrent.CountDownLatch;

public
class ProcessProxy extends Thread {

    private final InputStream is;
    private final OutputStream os;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

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
        this.interrupt();
        try {
            if (os != null) {
                os.flush(); // this goes to the console, so we don't want to close it!
            }
            this.is.close();
        } catch (IOException e) {
        }
    }

    @Override
    public synchronized
    void start() {
        super.start();

        // now we have to for it to actually start up. The process can run & complete before this starts, resulting in no input/output
        // captured
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public
    void run() {
        final OutputStream os = this.os;
        final InputStream is = this.is;

        countDownLatch.countDown();

        try {
            // this thread will read until there is no more data to read. (this is generally what you want)
            // the stream will be closed when the process closes it (usually on exit)
            int readInt;

            if (os == null) {
                // just read so it won't block.
                while (is.read() != -1) {
                }
            }
            else {
                while ((readInt = is.read()) != -1) {
                    os.write(readInt);


                    // flush the output on new line. (same for both windows '\r\n' and linux '\n')
                    if (readInt == '\n') {
                        os.flush();

                        synchronized (os) {
                            os.notify();
                        }
                    }
                }
            }
        } catch (Exception ignore) {
        }
    }
}
