package dorkbox.util.input;

import java.io.IOException;
import java.io.InputStream;

public abstract class Terminal {
    protected final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());


    public static final int DEFAULT_WIDTH = 80;
    public static final int DEFAULT_HEIGHT = 24;

    private volatile boolean echoEnabled;
    private volatile Thread shutdown;

    public Terminal() {
        if (this.shutdown != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(this.shutdown);
            }
            catch (IllegalStateException e) {
                // The VM is shutting down, ignore
            }
        }

        // Register a task to restore the terminal on shutdown
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    restore();
                } catch (IOException e) {
                    Terminal.this.logger.error("Unable to restore the terminal", e);
                }
            }
        };

        this.shutdown = new Thread(runnable, "Terminal");

        try {
            Runtime.getRuntime().addShutdownHook(this.shutdown);
        }
        catch (IllegalStateException e) {
            // The VM is shutting down, ignore
        }
    }


    public abstract void init() throws IOException;
    public abstract void restore() throws IOException;

    public void setEchoEnabled(boolean enabled) {
        this.echoEnabled = enabled;
    }

    public boolean isEchoEnabled() {
        return this.echoEnabled;
    }

    public abstract int getWidth();
    public abstract int getHeight();

    public InputStream wrapInIfNeeded(InputStream in) throws IOException {
        return in;
    }
}
