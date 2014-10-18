package dorkbox.util.input.unsupported;

import java.io.BufferedReader;
import java.io.IOException;

import dorkbox.util.bytes.ByteBuffer2;
import dorkbox.util.input.Terminal;
import dorkbox.util.input.posix.InputStreamReader;

public class UnsupportedTerminal extends Terminal {

    private final ByteBuffer2 buffer = new ByteBuffer2(8, -1);

    private BufferedReader reader;
    private String readLine = null;
    private char[] line;

    private ThreadLocal<Integer> indexOfStringForReadChar = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return -1;
        }
    };

    public UnsupportedTerminal(String encoding) {
        this.reader = new BufferedReader(new InputStreamReader(System.in, encoding));
    }

    @Override
    public final void init() throws IOException {
    }

    @Override
    public final void restore() {
    }

    @Override
    public final int getWidth() {
        return 0;
    }

    @Override
    public final int getHeight() {
        return 0;
    }

    @Override
    public final int read() {
        // if we are reading data (because we are in IDE mode), we want to return ALL
        // the chars of the line!

        // so, readChar is REALLY the index at which we return letters (until the whole string is returned)
        int readerCount = this.indexOfStringForReadChar.get();

        if (readerCount == -1) {

            // we have to wait for more data.
            try {
                this.readLine = this.reader.readLine();
            } catch (IOException e) {
               return -1;
            }

            this.line = this.readLine.toCharArray();
            this.buffer.clear();
            for (char c : this.line) {
                this.buffer.writeChar(c);
            }

            readerCount = 0;
            this.indexOfStringForReadChar.set(0);
        }


        // EACH thread will have it's own count!
       if (readerCount == this.buffer.position()) {
            this.indexOfStringForReadChar.set(-1);
            return '\n';
        } else {
            this.indexOfStringForReadChar.set(readerCount+2); // because 2 bytes per char in java
        }

        char c = this.buffer.readChar(readerCount);
        return c;
    }

    @Override
    public final boolean wasSequence() {
        return this.line.length > 0;
    }
}