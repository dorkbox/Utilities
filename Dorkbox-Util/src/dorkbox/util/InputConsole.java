package dorkbox.util;


import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicBoolean;

import jline.IDE_Terminal;
import jline.Terminal;
import jline.console.ConsoleReader;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

public class InputConsole {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InputConsole.class);
    private static final InputConsole consoleProxyReader;
    private static final char[] emptyLine = new char[0];

    static {
        consoleProxyReader = new InputConsole();
        // setup (if necessary) the JLINE console logger.
        // System.setProperty("jline.internal.Log.trace", "TRUE");
        // System.setProperty("jline.internal.Log.debug", "TRUE");
    }

    /**
     * empty method to allow code to initialize the input console.
     */
    public static void init() {
    }

    public static final void destroy() {
        consoleProxyReader.destroy0();
    }

    /** return null if no data */
    public static final String readLine() {
        char[] line = consoleProxyReader.readLine0();
        return new String(line);
    }

    /** return -1 if no data */
    public static final int read() {
        return consoleProxyReader.read0();
    }

    /** return null if no data */
    public static final char[] readLinePassword() {
        return consoleProxyReader.readLinePassword0();
    }

    public static InputStream getInputStream() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                return consoleProxyReader.read0();
            }

            @Override
            public void close() throws IOException {
                consoleProxyReader.release0();
            }
        };
    }

    public static void echo(boolean enableEcho) {
        consoleProxyReader.echo0(enableEcho);
    }

    public static boolean echo() {
        return consoleProxyReader.echo0();
    }


    private ConsoleReader jlineReader;

    private final Object inputLockSingle = new Object();
    private final Object inputLockLine = new Object();

    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private AtomicBoolean isInShutdown = new AtomicBoolean(false);
    private volatile char[] readLine = null;
    private volatile int readChar = -1;

    private boolean isIDE;

    // the streams are ALREADY buffered!
    //
    private InputConsole() {
        try {
            this.jlineReader = new ConsoleReader();

            Terminal terminal = this.jlineReader.getTerminal();
            terminal.setEchoEnabled(true);
            this.isIDE = terminal instanceof IDE_Terminal;

            if (this.isIDE) {
                logger.debug("Terminal is in IDE (best guess). Unable to support single key input. Only line input available.");
            } else {
                logger.debug("Terminal Type: {}", terminal.getClass().getSimpleName());
            }
        } catch (UnsupportedEncodingException ignored) {
        } catch (IOException ignored) {
        }
    }

    /**
     * make sure the input console reader thread is started.
     */
    private void startInputConsole() {
        // protected by atomic!
        if (!this.isRunning.compareAndSet(false, true) || this.isInShutdown.get()) {
            return;
        }

        Thread consoleThread = new Thread(new Runnable() {
            @Override
            public void run() {
                consoleProxyReader.run();
            }
        });
        consoleThread.setDaemon(true);
        consoleThread.setName("Console Input Reader");

        consoleThread.start();
    }

    private void destroy0() {
        // Don't change this, because we don't want to enable reading, etc from this once it's destroyed.
        // isRunning.set(false);

        if (this.isInShutdown.compareAndSet(true, true)) {
            return;
        }

        synchronized (this.inputLockSingle) {
            this.inputLockSingle.notifyAll();
        }

        synchronized (this.inputLockLine) {
            this.inputLockLine.notifyAll();
        }

        // we want to make sure this happens in a new thread, since this can BLOCK our main event dispatch thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputConsole.this.jlineReader.shutdown();
                InputConsole.this.jlineReader = null;
            }});
        thread.setDaemon(true);
        thread.setName("Console Input Shutdown");
        thread.start();
    }

    private void echo0(boolean enableEcho) {
        if (this.jlineReader != null) {
            Terminal terminal = this.jlineReader.getTerminal();
            if (terminal != null) {
                terminal.setEchoEnabled(enableEcho);
            }
        }
    }

    private boolean echo0() {
        if (this.jlineReader != null) {
            Terminal terminal = this.jlineReader.getTerminal();
            if (terminal != null) {
                return terminal.isEchoEnabled();
            }
        }
        return false;
    }


    /** return null if no data */
    private final char[] readLine0() {
        startInputConsole();

        synchronized (this.inputLockLine) {
            try {
                this.inputLockLine.wait();
            } catch (InterruptedException e) {
                return emptyLine;
            }
        }
        return this.readLine;
    }

    /** return null if no data */
    private final char[] readLinePassword0() {
        startInputConsole();

        // don't bother in an IDE. it won't work.
        return readLine0();
    }

    /** return -1 if no data */
    private final int read0() {
        startInputConsole();

        synchronized (this.inputLockSingle) {
            try {
                this.inputLockSingle.wait();
            } catch (InterruptedException e) {
                return -1;
            }
        }
        return this.readChar;
    }

    /**
     * releases any thread still waiting.
     */
    private void release0() {
        synchronized (this.inputLockSingle) {
            this.inputLockSingle.notifyAll();
        }

        synchronized (this.inputLockLine) {
            this.inputLockLine.notifyAll();
        }
    }

    private final void run() {
        if (this.jlineReader == null) {
            logger.error("Unable to start Console Reader");
            return;
        }

        // if we are eclipse, we MUST do this per line! (per character DOESN'T work.)
        if (this.isIDE) {
            try {
                while ((this.readLine = this.jlineReader.readLine()) != null) {
                    // notify everyone waiting for a line of text.
                    synchronized (this.inputLockSingle) {
                        if (this.readLine.length > 0) {
                            this.readChar = this.readLine[0];
                        } else {
                            this.readChar = -1;
                        }
                        this.inputLockSingle.notifyAll();
                    }
                    synchronized (this.inputLockLine) {
                        this.inputLockLine.notifyAll();
                    }
                }
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        } else {

            try {
                final boolean ansiEnabled = Ansi.isEnabled();
                Ansi ansi = Ansi.ansi();

                int typedChar;
                StringBuilder buf = new StringBuilder();

                // don't type ; in a bash shell, it quits everything
                // \n is replaced by \r in unix terminal?
                while ((typedChar = this.jlineReader.readCharacter()) != -1) {
                    char asChar = (char) typedChar;

                    logger.trace("READ: {} ({})", asChar, typedChar);

                    // notify everyone waiting for a character.
                    synchronized (this.inputLockSingle) {
                        this.readChar = typedChar;
                        this.inputLockSingle.notifyAll();
                    }

                    // if we type a backspace key, swallow it + previous in READLINE. READCHAR will have it passed.
                    if (typedChar == 127) {
                        int position = 0;

                        // clear ourself + one extra.
                        if (ansiEnabled) {
                            int amtToBackspace = 2; // ConsoleReader.getPrintableCharacters(typedChar).length();
                            int length = buf.length();
                            if (length > 1) {
                                char charAt = buf.charAt(length-1);
                                amtToBackspace += ConsoleReader.getPrintableCharacters(charAt).length();
                                buf.delete(length-1, length);

                                length--;

                                // now figure out where the cursor is at.
                                for (int i=0;i<length;i++) {
                                    charAt = buf.charAt(i);
                                    position += ConsoleReader.getPrintableCharacters(charAt).length();
                                }
                                position++;
                            }

                            char[] overwrite = new char[amtToBackspace];
                            for (int i=0;i<amtToBackspace;i++) {
                                overwrite[i] = ' ';
                            }

                            // move back however many, over write, then go back again
                            AnsiConsole.out.print(ansi.cursorToColumn(position));
                            AnsiConsole.out.print(overwrite);
                            AnsiConsole.out.print(ansi.cursorToColumn(position));
                            AnsiConsole.out.flush();
                        }

                        // readline will ignore backspace
                        continue;
                    }

                    // ignoring \r, because \n is ALWAYS the last character in a new line sequence.
                    if (asChar == '\n') {
                        int length = buf.length();

                        synchronized (this.inputLockLine) {
                            if (length > 0) {
                                this.readLine = new char[length];
                                buf.getChars(0, length, this.readLine, 0);
                            } else {
                                this.readLine = emptyLine;
                            }

                            this.inputLockLine.notifyAll();
                        }

                        // dump the characters in the backing array (slightly safer for passwords when using this method)
                        if (length > 0) {
                            buf.delete(0, buf.length());
                        }
                    } else if (asChar != '\r') {
                        // only append if we are not a new line.
                        buf.append(asChar);
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }
}