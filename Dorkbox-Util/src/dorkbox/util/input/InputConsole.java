package dorkbox.util.input;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.Logger;

import dorkbox.util.OS;
import dorkbox.util.input.posix.UnixTerminal;
import dorkbox.util.input.unsupported.UnsupportedTerminal;
import dorkbox.util.input.windows.WindowsTerminal;

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

        // don't forget we have to shut down the ansi console as well
        // alternatively, shut everything down when the JVM closes.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                AnsiConsole.systemUninstall();
                InputConsole.destroy();
            }
        });
    }

    // called by our shutdown thread
    private static final void destroy() {
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


    private final Object inputLockSingle = new Object();
    private final Object inputLockLine = new Object();

    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private AtomicBoolean isInShutdown = new AtomicBoolean(false);
    private volatile char[] readLine = null;
    private volatile int readChar = -1;

    private final boolean unsupported;

    private final Terminal terminal;
    private Reader reader;
    private final String encoding;



    private InputConsole() {
        Logger logger = InputConsole.logger;
        boolean unsupported = false;

        String type = System.getProperty(TerminalType.TYPE, TerminalType.AUTO).toLowerCase();
        if ("dumb".equals(System.getenv("TERM"))) {
            type = "none";
            logger.debug("$TERM=dumb; setting type={}", type);
        }

        logger.debug("Creating terminal; type={}", type);

        Terminal t;
        try {
            if (type.equals(TerminalType.UNIX)) {
                t = new UnixTerminal();
            }
            else if (type.equals(TerminalType.WIN) || type.equals(TerminalType.WINDOWS)) {
                t = new WindowsTerminal();
            }
            else if (type.equals(TerminalType.NONE) || type.equals(TerminalType.OFF) || type.equals(TerminalType.FALSE)) {
                t = new UnsupportedTerminal();
                unsupported = true;
            } else {
                if (isIDEAutoDetect()) {
                    logger.debug("Terminal is in UNSUPPORTED (best guess). Unable to support single key input. Only line input available.");
                    t = new UnsupportedTerminal();
                    unsupported = true;
                } else {
                    if (OS.isWindows()) {
                        t = new WindowsTerminal();
                    } else {
                        t = new UnixTerminal();
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error("Failed to construct terminal, falling back to unsupported");
            t = new UnsupportedTerminal();
            unsupported = true;
        }

        InputStream in;

        try {
            t.init();
            in = t.wrapInIfNeeded(System.in);
        }
        catch (Throwable e) {
            logger.error("Terminal initialization failed, falling back to unsupported");
            t = new UnsupportedTerminal();
            unsupported = true;
            in = System.in;

            try {
                t.init();
            } catch (IOException e1) {
                // UnsupportedTerminal can't do this
            }
        }

        this.encoding = this.encoding != null ? this.encoding : getEncoding();
        this.reader = new InputStreamReader(in, this.encoding);

        if (unsupported) {
            this.reader = new BufferedReader(this.reader);
        }

        this.unsupported = unsupported;
        this.terminal = t;

        logger.debug("Created Terminal: {}", this.terminal);
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
        // so we pretend that it's still running
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

        try {
            InputConsole inputConsole = InputConsole.this;

            inputConsole.terminal.restore();
            inputConsole.reader.close();
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
        // don't bother in an IDE. it won't work.
        return readLine0();
    }

    ThreadLocal<Integer> indexOfStringForReadChar = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return -1;
        }
    };

    /** return -1 if no data */
    private final int read0() {
        startInputConsole();

        // if we are reading data (because we are in IDE mode), we want to return ALL
        // the chars of the line!

        // so, readChar is REALLY the index at which we return letters (until the whole string is returned
        if (this.unsupported) {
            int integer = this.indexOfStringForReadChar.get();

            if (integer == -1) {
                // we have to wait for more data.
                synchronized (this.inputLockLine) {
                    try {
                        this.inputLockLine.wait();
                    } catch (InterruptedException e) {
                        return -1;
                    }
                    integer = 0;
                    this.indexOfStringForReadChar.set(0);
                }
            }

            // EACH thread will have it's own count!
            char[] readLine2 = this.readLine;

            if (readLine2 == null) {
                return -1;
            } else if (integer == readLine2.length) {
                this.indexOfStringForReadChar.set(-1);
                return '\n';
            } else {
                this.indexOfStringForReadChar.set(integer+1);
            }

            char c = readLine2[integer];
            return c;
        }
        else {
            // we can read like normal.
            synchronized (this.inputLockSingle) {
                try {
                    this.inputLockSingle.wait();
                } catch (InterruptedException e) {
                    return -1;
                }
            }
            return this.readChar;
        }
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

        // if we are eclipse/etc, we MUST do this per line! (per character DOESN'T work.)
        // char readers will get looped for the WHOLE string, so reading by char will work,
        // it just waits until \n until it triggers
        if (this.unsupported) {
            BufferedReader reader = (BufferedReader) this.reader;

            try {
                while ((this.readLine = reader.readLine().toCharArray()) != null) {
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
        }
        else {
            try {
                final boolean ansiEnabled = Ansi.isEnabled();
                Ansi ansi = Ansi.ansi();

                int typedChar;
                StringBuilder buf = new StringBuilder();

                // don't type ; in a bash shell, it quits everything
                // \n is replaced by \r in unix terminal?
                while ((typedChar = this.reader.read()) != -1) {
                    char asChar = (char) typedChar;

                    if (logger2.isTraceEnabled()) {
                        logger2.trace("READ: {} ({})", asChar, typedChar);
                    }

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
                            // size of the buffer BEFORE our backspace was typed
                            int length = buf.length();
                            int amtToOverwrite = 2; // backspace is always 2 chars (^?)

                            if (length > 1) {
                                char charAt = buf.charAt(length-1);
                                amtToOverwrite += getPrintableCharacters(charAt);

                                // delete last item in our buffer
                                buf.setLength(--length);

                                // now figure out where the cursor is really at.
                                // this is more memory friendly than buf.toString.length
                                for (int i=0;i<length;i++) {
                                    charAt = buf.charAt(i);
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
                            AnsiConsole.out.print(ansi.cursorToColumn(position));
                            AnsiConsole.out.print(overwrite);
                            AnsiConsole.out.print(ansi.cursorToColumn(position));
                            AnsiConsole.out.flush();
                        }

                        // readline will ignore backspace
                        continue;
                    }

                    // ignoring \r, because \n is ALWAYS the last character in a new line sequence. (even for windows)
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

    /**
    +     * Get the default encoding.  Will first look at the LC_CTYPE environment variable, then the input.encoding
    +     * system property, then the default charset according to the JVM.
    +     *
    +     * @return The default encoding to use when none is specified.
    +     */
    public static String getEncoding() {
        // LC_CTYPE is usually in the form en_US.UTF-8
        String envEncoding = extractEncodingFromCtype(System.getenv("LC_CTYPE"));
        if (envEncoding != null) {
            return envEncoding;
        }
        return System.getProperty("input.encoding", Charset.defaultCharset().name());
    }

    /**
     * Parses the LC_CTYPE value to extract the encoding according to the POSIX standard, which says that the LC_CTYPE
     * environment variable may be of the format <code>[language[_territory][.codeset][@modifier]]</code>
     *
     * @param ctype The ctype to parse, may be null
     * @return The encoding, if one was present, otherwise null
     */
    static String extractEncodingFromCtype(String ctype) {
        if (ctype != null && ctype.indexOf('.') > 0) {
            String encodingAndModifier = ctype.substring(ctype.indexOf('.') + 1);
            if (encodingAndModifier.indexOf('@') > 0) {
                return encodingAndModifier.substring(0, encodingAndModifier.indexOf('@'));
            } else {
                return encodingAndModifier;
            }
        }
        return null;
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