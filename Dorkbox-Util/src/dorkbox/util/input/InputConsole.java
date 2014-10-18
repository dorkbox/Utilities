package dorkbox.util.input;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.concurrent.CopyOnWriteArrayList;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.Logger;

import dorkbox.util.OS;
import dorkbox.util.bytes.ByteBuffer2;
import dorkbox.util.bytes.ByteBuffer2Poolable;
import dorkbox.util.input.posix.UnixTerminal;
import dorkbox.util.input.unsupported.UnsupportedTerminal;
import dorkbox.util.input.windows.WindowsTerminal;
import dorkbox.util.objectPool.ObjectPool;
import dorkbox.util.objectPool.ObjectPoolFactory;
import dorkbox.util.objectPool.ObjectPoolHolder;

public class InputConsole {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InputConsole.class);
    private static final InputConsole consoleProxyReader = new InputConsole();
    private static final char[] emptyLine = new char[0];

    /**
     * empty method to allow code to initialize the input console.
     */
    public static void init() {
    }

    // this is run by our init...
    {
        AnsiConsole.systemInstall();

        Thread consoleThread = new Thread(new Runnable() {
            @Override
            public void run() {
                consoleProxyReader.run();
            }
        });
        consoleThread.setDaemon(true);
        consoleThread.setName("Console Input Reader");

        consoleThread.start();

        // has to be NOT DAEMON thread, since it must run before the app closes.

        // don't forget we have to shut down the ansi console as well
        // alternatively, shut everything down when the JVM closes.
        Thread shutdownThread = new Thread() {
            @Override
            public void run() {
                AnsiConsole.systemUninstall();

                consoleProxyReader.shutdown0();
            }
        };
        shutdownThread.setName("Console Input Shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownThread);
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


    private final Object inputLockSingle = new Object();
    private final Object inputLockLine = new Object();

    private final ObjectPool<ByteBuffer2> pool = ObjectPoolFactory.create(new ByteBuffer2Poolable());
    private ThreadLocal<ObjectPoolHolder<ByteBuffer2>> threadBufferForRead = new ThreadLocal<ObjectPoolHolder<ByteBuffer2>>();
    private CopyOnWriteArrayList<ObjectPoolHolder<ByteBuffer2>> threadBuffersForRead = new CopyOnWriteArrayList<ObjectPoolHolder<ByteBuffer2>>();

    private volatile int readChar = -1;
    private final Terminal terminal;

    private InputConsole() {
        Logger logger = InputConsole.logger;

        String type = System.getProperty(TerminalType.TYPE, TerminalType.AUTO).toLowerCase();
        if ("dumb".equals(System.getenv("TERM"))) {
            type = "none";
            logger.debug("$TERM=dumb; setting type={}", type);
        }

        logger.debug("Creating terminal; type={}", type);


        String encoding = Encoding.get();
        Terminal t;
        try {
            if (type.equals(TerminalType.UNIX)) {
                t = new UnixTerminal(encoding);
            }
            else if (type.equals(TerminalType.WIN) || type.equals(TerminalType.WINDOWS)) {
                t = new WindowsTerminal();
            }
            else if (type.equals(TerminalType.NONE) || type.equals(TerminalType.OFF) || type.equals(TerminalType.FALSE)) {
                t = new UnsupportedTerminal(encoding);
            } else {
                if (isIDEAutoDetect()) {
                    logger.debug("Terminal is in UNSUPPORTED (best guess). Unable to support single key input. Only line input available.");
                    t = new UnsupportedTerminal(encoding);
                } else {
                    if (OS.isWindows()) {
                        t = new WindowsTerminal();
                    } else {
                        t = new UnixTerminal(encoding);
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error("Failed to construct terminal, falling back to unsupported");
            t = new UnsupportedTerminal(encoding);
        }

        try {
            t.init();
        }
        catch (Throwable e) {
            logger.error("Terminal initialization failed, falling back to unsupported");
            t = new UnsupportedTerminal(encoding);

            try {
                t.init();
            } catch (IOException e1) {
                // UnsupportedTerminal can't do this
            }
        }

        t.setEchoEnabled(true);

        this.terminal = t;
        logger.debug("Created Terminal: {} ({}x{})", this.terminal.getClass().getSimpleName(), t.getWidth(), t.getHeight());
    }

    // called when the JVM is shutting down.
    private void shutdown0() {
        synchronized (this.inputLockSingle) {
            this.inputLockSingle.notifyAll();
        }

        synchronized (this.inputLockLine) {
            this.inputLockLine.notifyAll();
        }

        try {
            InputConsole inputConsole = InputConsole.this;

            inputConsole.terminal.restore();
            // this will 'hang' our shutdown, and honestly, who cares? We're shutting down anyways.
            // inputConsole.reader.close(); // hangs on shutdown
        } catch (IOException ignored) {
            ignored.printStackTrace();
        }
    }

    private void echo0(boolean enableEcho) {
        this.terminal.setEchoEnabled(enableEcho);
    }

    private boolean echo0() {
        return this.terminal.isEchoEnabled();
    }


    /** return null if no data */
    private final char[] readLine0() {
        if (this.threadBufferForRead.get() == null) {
            ObjectPoolHolder<ByteBuffer2> holder = this.pool.take();
            this.threadBufferForRead.set(holder);
            this.threadBuffersForRead.add(holder);
        }

        synchronized (this.inputLockLine) {
            try {
                this.inputLockLine.wait();
            } catch (InterruptedException e) {
                return emptyLine;
            }
        }

        ObjectPoolHolder<ByteBuffer2> objectPoolHolder = this.threadBufferForRead.get();
        ByteBuffer2 buffer = objectPoolHolder.getValue();
        int len = buffer.position();
        if (len == 0) {
            return emptyLine;
        }

        buffer.rewind();
        char[] readChars = buffer.readChars(len/2); // java always stores chars in 2 bytes

        // dump the chars in the buffer (safer for passwords, etc)
        buffer.clearSecure();

        this.threadBuffersForRead.remove(objectPoolHolder);
        this.pool.release(objectPoolHolder);
        this.threadBufferForRead.set(null);

        return readChars;
    }

    /** return null if no data */
    private final char[] readLinePassword0() {
        // don't bother in an IDE. it won't work.
        boolean echoEnabled = this.terminal.isEchoEnabled();
        this.terminal.setEchoEnabled(false);
        char[] readLine0 = readLine0();
        this.terminal.setEchoEnabled(echoEnabled);

        return readLine0;
    }

    /** return -1 if no data */
    private final int read0() {
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
        Logger logger2 = logger;

        final boolean ansiEnabled = Ansi.isEnabled();
        Ansi ansi = Ansi.ansi();
        PrintStream out = AnsiConsole.out;

        int typedChar;
        char asChar;

        // don't type ; in a bash shell, it quits everything
        // \n is replaced by \r in unix terminal?
        while ((typedChar = this.terminal.read()) != -1) {
            asChar = (char) typedChar;

            if (logger2.isTraceEnabled()) {
                logger2.trace("READ: {} ({})", asChar, typedChar);
            }

            // notify everyone waiting for a character.
            synchronized (this.inputLockSingle) {
                if (this.terminal.wasSequence() && typedChar == '\n') {
                    // don't want to forward \n if it was a part of a sequence in the unsupported terminal
                    // the JIT will short-cut this out if we are not the unsupported terminal
                } else {
                    this.readChar = typedChar;
                    this.inputLockSingle.notifyAll();
                }
            }

            // if we type a backspace key, swallow it + previous in READLINE. READCHAR will have it passed.
            if (typedChar == '\b') {
                int position = 0;

                // clear ourself + one extra.
                if (ansiEnabled) {
                    for (ObjectPoolHolder<ByteBuffer2> objectPoolHolder : this.threadBuffersForRead) {
                        ByteBuffer2 buffer = objectPoolHolder.getValue();
                        // size of the buffer BEFORE our backspace was typed
                        int length = buffer.position();
                        int amtToOverwrite = 2 * 2; // backspace is always 2 chars (^?) * 2 because it's bytes

                        if (length > 1) {
                            char charAt = buffer.readChar(length-2);
                            amtToOverwrite += getPrintableCharacters(charAt);

                            // delete last item in our buffer
                            length -= 2;
                            buffer.setPosition(length);

                            // now figure out where the cursor is really at.
                            // this is more memory friendly than buf.toString.length
                            for (int i=0;i<length;i+=2) {
                                charAt = buffer.readChar(i);
                                position += getPrintableCharacters(charAt);
                            }

                            position++;
                        }

                        char[] overwrite = new char[amtToOverwrite];
                        char c = ' ';
                        for (int i=0;i<amtToOverwrite;i++) {
                            overwrite[i] = c;
                        }

                        // move back however many, over write, then go back again
                        out.print(ansi.cursorToColumn(position));
                        out.print(overwrite);
                        out.print(ansi.cursorToColumn(position));
                        out.flush();
                    }
                }

                // read-line will ignore backspace
                continue;
            }

            // ignoring \r, because \n is ALWAYS the last character in a new line sequence. (even for windows)
            if (asChar == '\n') {
                synchronized (this.inputLockLine) {
                    this.inputLockLine.notifyAll();
                }
            } else {
                // our windows console PREVENTS us from returning '\r' (it truncates '\r\n', and returns just '\n')

                // only append if we are not a new line.
                for (ObjectPoolHolder<ByteBuffer2> objectPoolHolder : this.threadBuffersForRead) {
                    ByteBuffer2 buffer = objectPoolHolder.getValue();
                    buffer.writeChar(asChar);
                }
            }
        }
    }

    /**
     * try to guess if we are running inside an IDE
     */
    private boolean isIDEAutoDetect() {
        try {
            // Get the location of this class
            ProtectionDomain pDomain = getClass().getProtectionDomain();
            CodeSource cSource = pDomain.getCodeSource();
            URL loc = cSource.getLocation();  // file:/X:/workspace/xxxx/classes/  when it's in eclipse

            // if we are in eclipse, this won't be a jar -- it will be a class directory.
            File locFile = new File(loc.getFile());
            return locFile.isDirectory();

        } catch (Exception e) {
        }

        // fall-back to unsupported
        return true;
    }


    /**
     * Return the number of characters that will be printed when the specified
     * character is echoed to the screen
     *
     * Adapted from cat by Torbjorn Granlund, as repeated in stty by David MacKenzie.
     */
    public static int getPrintableCharacters(final int ch) {
//        StringBuilder sbuff = new StringBuilder();

        if (ch >= 32) {
            if (ch < 127) {
//                sbuff.append((char) ch);
                return 1;
            }
            else if (ch == 127) {
//                sbuff.append('^');
//                sbuff.append('?');
                return 2;
            }
            else {
//                sbuff.append('M');
//                sbuff.append('-');
                int count = 2;

                if (ch >= 128 + 32) {
                    if (ch < 128 + 127) {
//                        sbuff.append((char) (ch - 128));
                        count++;
                    }
                    else {
//                        sbuff.append('^');
//                        sbuff.append('?');
                        count += 2;
                    }
                }
                else {
//                    sbuff.append('^');
//                    sbuff.append((char) (ch - 128 + 64));
                    count += 2;
                }
                return count;
            }
        }
        else {
//            sbuff.append('^');
//            sbuff.append((char) (ch + 64));
            return 2;
        }

//        return sbuff;
    }
}