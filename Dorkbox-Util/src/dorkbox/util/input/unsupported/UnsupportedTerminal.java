package dorkbox.util.input.unsupported;

import java.io.IOException;

import dorkbox.util.input.Terminal;

public class UnsupportedTerminal extends Terminal {
    public UnsupportedTerminal() {
//        setAnsiSupported(false);
        setEchoEnabled(true);
    }

    @Override
    public void init() throws IOException {
    }

    @Override
    public void restore() {
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }
}