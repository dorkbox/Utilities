package dorkbox.util.process;
import java.io.IOException;
import java.io.OutputStream;

public class NullOutputStream extends OutputStream {
    @Override
    public void write(int i) throws IOException {
        //do nothing
    }

    @Override
    public void write(byte[] b) throws IOException {
        //do nothing
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        //do nothing
    }

    @Override
    public void flush() throws IOException {
        //do nothing
    }
}