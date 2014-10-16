/*
 * Copyright (c) 2002-2012, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package dorkbox.util.input.windows;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.fusesource.jansi.internal.WindowsSupport;

import dorkbox.util.input.Terminal;

/**
 * Terminal implementation for Microsoft Windows. Terminal initialization in
 * {@link #init} is accomplished by calling the Win32 APIs <a
 * href="http://msdn.microsoft.com/library/default.asp?
 * url=/library/en-us/dllproc/base/setconsolemode.asp">SetConsoleMode</a> and
 * <a href="http://msdn.microsoft.com/library/default.asp?
 * url=/library/en-us/dllproc/base/getconsolemode.asp">GetConsoleMode</a> to
 * disable character echoing.
 * <p/>
 * <p>
 * By default, the {@link #wrapInIfNeeded(java.io.InputStream)} method will attempt
 * to test to see if the specified {@link InputStream} is {@link System#in} or a wrapper
 * around {@link FileDescriptor#in}, and if so, will bypass the character reading to
 * directly invoke the readc() method in the JNI library. This is so the class
 * can read special keys (like arrow keys) which are otherwise inaccessible via
 * the {@link System#in} stream. Using JNI reading can be bypassed by setting
 * the <code>jline.WindowsTerminal.directConsole</code> system property
 * to <code>false</code>.
 * </p>
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.0
 */
public class WindowsTerminal extends Terminal
{
    public static final String DIRECT_CONSOLE = WindowsTerminal.class.getName() + ".directConsole";

    private int originalMode;

    public WindowsTerminal() {
    }

    @Override
    public void init() throws IOException {
        this.originalMode = WindowsSupport.getConsoleMode();
        WindowsSupport.setConsoleMode(this.originalMode & ~ConsoleMode.ENABLE_ECHO_INPUT.code);
    }

    /**
     * Restore the original terminal configuration, which can be used when
     * shutting down the console reader. The ConsoleReader cannot be
     * used after calling this method.
     */
    @Override
    public void restore() throws IOException {
        // restore the old console mode
        WindowsSupport.setConsoleMode(this.originalMode);
    }

    @Override
    public int getWidth() {
        int w = WindowsSupport.getWindowsTerminalWidth();
        return w < 1 ? DEFAULT_WIDTH : w;
    }

    @Override
    public int getHeight() {
        int h = WindowsSupport.getWindowsTerminalHeight();
        return h < 1 ? DEFAULT_HEIGHT : h;
    }

    @Override
    public void setEchoEnabled(final boolean enabled) {
        // Must set these four modes at the same time to make it work fine.
        if (enabled) {
            WindowsSupport.setConsoleMode(WindowsSupport.getConsoleMode() |
                           ConsoleMode.ENABLE_ECHO_INPUT.code |
                           ConsoleMode.ENABLE_LINE_INPUT.code |
                           ConsoleMode.ENABLE_PROCESSED_INPUT.code |
                           ConsoleMode.ENABLE_WINDOW_INPUT.code);
        }
        else {
            WindowsSupport.setConsoleMode(WindowsSupport.getConsoleMode() &
                ~(ConsoleMode.ENABLE_LINE_INPUT.code |
                  ConsoleMode.ENABLE_ECHO_INPUT.code |
                  ConsoleMode.ENABLE_PROCESSED_INPUT.code |
                  ConsoleMode.ENABLE_WINDOW_INPUT.code));
        }
        super.setEchoEnabled(enabled);
    }


    @Override
    public InputStream wrapInIfNeeded(InputStream in) throws IOException {
        if (isSystemIn(in)) {
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    return WindowsSupport.readByte();
                }
            };
        } else {
            return in;
        }
    }

    private boolean isSystemIn(final InputStream in) throws IOException {
        if (in == null) {
            return false;
        }
        else if (in == System.in) {
            return true;
        }
        else if (in instanceof FileInputStream && ((FileInputStream) in).getFD() == FileDescriptor.in) {
            return true;
        }

        return false;
    }
}
