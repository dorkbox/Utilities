package dorkbox.util.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProcessProxy extends Thread {

    private final InputStream is;
    private final OutputStream os;

    // when reading from the stdin and outputting to the process
    public ProcessProxy(String processName, InputStream inputStreamFromConsole, OutputStream outputStreamToProcess) {
        is = inputStreamFromConsole;
        os = outputStreamToProcess;

        setName(processName);
        setDaemon(true);
    }

    public void close() {
        try {
            is.close();
        } catch (IOException e) {
        }
    }

    @Override
    public void run() {
        try {
            // this thread will read until there is no more data to read. (this is generally what you want)
            // the stream will be closed when the process closes it (usually on exit)
            int readInt;

            if (os == null) {
                // just read so it won't block.
                while ((readInt = is.read()) != -1) {
                }
            } else {
                while ((readInt = is.read()) != -1) {
                    os.write(readInt);

                    // flush the output on new line. Works for windows/linux, since \n is always the last char in the sequence.
                    if (readInt == '\n') {
                        os.flush();
                    }
                }
            }
        } catch (IOException ignore) {
        } catch (IllegalArgumentException e) {
        } finally {
            try {
                if (os != null) {
                    os.flush(); // this goes to the console, so we don't want to close it!
                }
                is.close();
            } catch (IOException ignore) {
            }
        }
    }
}