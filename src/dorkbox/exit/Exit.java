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
package dorkbox.exit;


import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import dorkbox.os.OS;

/**
 * The EXIT system uses ERRORS to exit the application. We must make sure NOT to swallow them higher up! -- so don't catch "throwable"!
 */
public final class Exit {
    /**
     * Used to set the data that will be
     * 1) used for relaunch,
     * 2) used to display an error message on exit
     */
    private static native void setExitError(String data);

    private static native boolean isNative0();

    /**
     * Check to see if we are native.
     * Used for determining how exit's are handled.
     */
    public static final boolean isNative() {
        try {
            return isNative0();
        } catch (Throwable t) {
            return false;
        }
    }

    // MIN/MAX values are -128 - 127, because of parent process limitations (anything over 127 will be converted to negative)
    private static int               UNDEFINED          = -1; // must match C source (also anything < 0)
    private static int               NORMAL             = 0;  // must match C source
    private static int               FAILED_CONFIG      = 1; // when we are trying to configure something, and it fails. Generally this is for required configurations
    private static int               FAILED_INIT        = 2;
    private static int               FAILED_SECURITY    = 3;

    // must match C source!!
    private static int           RESERVED       = 100; // must match C source
    private static List<Integer> RESERVED_LIST  = new ArrayList<Integer>(2);

    private static int               SAVED_IN_LOG_FILE  = 101; // must match C source
    private static int               RESTART            = 102; // must match C source
    private static int               UPGRADE_EXECUTABLE = 103; // must match C source

    static {
        RESERVED_LIST.add(new Integer(SAVED_IN_LOG_FILE));
        RESERVED_LIST.add(new Integer(RESTART));
        RESERVED_LIST.add(new Integer(UPGRADE_EXECUTABLE));
    }


    // so, it is important to observe, that WHILE the application is starting (Launcher.java),
    // throwing exceptions is ACCEPTABLE. Afterwards, we MUST rely on the blocking structure to notify
    // the main thread, so it can return normally.


    private static AtomicInteger exitValue = new AtomicInteger(NORMAL);
    private static ExitBase exitError = null;
    private Exit() {}



    // This is what tells us that we are DONE launching, and have moved onto the bootstrap.
    public static int getExitValue() {
        _setExitData(exitError);

        int exit = exitValue.get();
        if (exit > RESERVED) {
            if (RESERVED_LIST.contains(exitError)) {
                return exit;
            } else {
                setExitError("You cannot use any number greater than 99");
                return RESERVED;
            }
        } else {
            return exit;
        }
    }

    /**
     * Only used on the inside of a thrown exception by the main thread
     *
     * <p>
     * sets the exit data (ONLY IF there isn't already an error set in the system) then gets the return code
     */
    public static int getExitDuringException(ExitBase exitError) {
        if (Exit.exitError != null) {
            _setExitData(Exit.exitError);
        } else {
            _setExitData(exitError);
        }

        // just in case.
        int exit = exitValue.get();
        if (exit > RESERVED) {
            if (RESERVED_LIST.contains(exitError)) {
                return exit;
            } else {
                setExitError("You cannot use any number greater than 99");
                return RESERVED;
            }
        } else {
            return exit;
        }
    }
    /**
     * Writes a message to the log file
     */
    public static void writeToLogFile(String title, String message) {
        try {
            Writer output = new BufferedWriter(new FileWriter("error.log", true));

            try {
                if (message != null) {
                    // FileWriter always assumes default encoding is OK
                    if (title != null) {
                        output.write(title);
                        output.write(OS.LINE_SEPARATOR + "   ");
                    }

                    output.write(new java.util.Date() + OS.LINE_SEPARATOR + "     ");
                    output.write(message);
                    output.write(OS.LINE_SEPARATOR);
                } else {
                    output.write("Execption thrown! Unknown error.");
                }
                output.write(OS.LINE_SEPARATOR);
            } finally {
                output.close();
            }
        } catch (IOException e) {
        }
    }



    /**
     * called by the uncaught exception hander.
     */
    public static void handleUncaughtException(Throwable error) {
        // only manage this if it's not on purpose!
        if (!(error instanceof ExitBase)) {
            // Not always undefined, although sometimes it is.
            String message = error.getMessage();
            if (message == null) {
                message = Exit.getStackStrace(error);
            }

            Exit.writeToLogFile("Uncaught Exception: " + error.getClass(), message);

            // if we are launching, then throw the error. If we FINISHED launching, trip the block
            // if we are closing (happens with a LEGIT error), then do nothing, since we are already
            // handling it.
            _throwIfLaunching(new ExitError(Exit.UNDEFINED,
                              "Abnormal termination from an uncaught exception! See log file.)"));
        }
    }


    /**
     * Undefined/unknown exit, and the info has been written to the log file.
     * @param string
     * @return
     */
    public static int Undefined(Throwable e) {
        // Not always undefined, although sometimes it is.
        String message = e.getMessage();
        if (message == null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8196);
            PrintStream printStream = new PrintStream(byteArrayOutputStream);
            e.printStackTrace(printStream);
            message = byteArrayOutputStream.toString();
            printStream.close();
        }

        message = e.getClass().toString() + " : " + message;

        // undefined exit! The launcher will restart the JVM in this case
        Exit.writeToLogFile("Error", message);

        // will actually return the ACTUAL exit error, if there is one.
        return Generic(Exit.UNDEFINED, "Undefined exception! Saved in log file.", message);
    }

    /**
     * Exit normally (from the launcher).
     */
    public static int Normal() {
        return Generic(Exit.NORMAL, "Normal exit called.");
    }

    /**
     * when we are trying to configure something, and it fails. Generally this is for required configurations
     */
    public static int FailedConfiguration(String errorMessage, Throwable throwable) {
        return Generic(Exit.FAILED_CONFIG, "FailedConfiguration called: " + errorMessage, throwable);
    }

    /**
     * when we are trying to configure something, and it fails. Generally this is for required configurations
     */
    public static int FailedInitialization(Throwable throwable) {
        return Generic(Exit.FAILED_INIT, "FailedInitialization called: " + throwable.getMessage(), throwable);
    }

    public static int FailedConfiguration(String errorMessage) {
        return Generic(Exit.FAILED_CONFIG, "FailedConfiguration called: " + errorMessage);
    }

    public static int FailedInitialization(String errorMessage, Throwable throwable) {
        return Generic(Exit.FAILED_INIT, "FailedInitialization called: " + errorMessage, throwable);
    }

    public static int FailedInitialization(String errorMessage) {
        return Generic(Exit.FAILED_INIT, "FailedInitialization called: " + errorMessage);
    }

    public static int FailedSecurity(Throwable throwable) {
        return Generic(Exit.FAILED_SECURITY, "FailedSecurity called: " + throwable.getMessage(), throwable);
    }

    public static int FailedSecurity(String errorMessage, Throwable throwable) {
        return Generic(Exit.FAILED_SECURITY, "FailedSecurity called: " + errorMessage, throwable);
    }

    public static int FailedSecurity(String errorMessage) {
        return Generic(Exit.FAILED_SECURITY, "FailedSecurity called: " + errorMessage);
    }





    public static int Generic(int exitCode, String errorMessage, Throwable throwable) {
        return Generic(exitCode, errorMessage + OS.LINE_SEPARATOR + throwable.getClass() + OS.LINE_SEPARATOR + throwable.getMessage());
    }

    public static int Generic(int exitCode, String errorMessage) {
        // if we are launching, then throw the error. If we FINISHED launching, trip the block
        _throwIfLaunching(new ExitError(exitCode, errorMessage));
        return exitCode;
    }

    public static int Generic(int exitCode, String errorTitle, String errorMessage) {
        // if we are launching, then throw the error. If we FINISHED launching, trip the block
        _throwIfLaunching(new ExitError(exitCode, errorTitle, errorMessage));
        return exitCode;
    }

    /**
     * restart the application with the current arguments.
     * If we need to modify launch args, use the ini file. (or create a new one)
     */
    public static void Restart() {
        _throwIfLaunching(new ExitRestart(Exit.RESTART));
    }

    /**
     * Specify that we want to upgrade the launcher executable. Other types of upgrade should use
     * restart, where the launcher will automatically detect and upgrade the components in place.
     */
    public static void UpgradeExectuable() {
        _throwIfLaunching(new ExitRestart(Exit.UPGRADE_EXECUTABLE));
    }


    static void _setExitData(ExitBase e) {
        if (e == null) {
            return;
        }

        exitValue.set(e.getExitCode());

        // exitData is passed to the launcher.
        if (e.getMessage() != null) {
            String message = e.getMessage();
            if (isNative()) {
                if (e.getTitle() != null) {
                    // can set the title if we want to. Normally it's just the program name.
                    setExitError("<title>" + e.getTitle() + "</title>" + OS.LINE_SEPARATOR + message);
                } else {
                    setExitError(message);
                }
            }
        }
    }

    static void _setExitCode(int exitCode) {
        exitValue.set(exitCode);
    }

    // throw error if we are still launcher, otherwise set and notify the block
    static void _throwIfLaunching(ExitBase exitError) {
        // save the error. It's not always passed up the chain.
        if (exitError != null && Exit.exitError == null) {
            Exit.exitError = exitError;
        }

        // throw the error. This makes sure we exit/quit the thread we are currently in.
        // we will end up in our "uncaught exception handler"!!
        if (exitError != null) {
            throw exitError;
        } else {
            throw new ExitError(Exit.FAILED_INIT, "Unable to have a null errorMessage!");
        }
    }

    /**
     * Utility method to get the stack trace from an exception, and convert it to a string.
     */
    public static String getStackStrace(Throwable throwable) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8196);
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        throwable.printStackTrace(printStream);

        String message = byteArrayOutputStream.toString();

        printStream.flush();
        printStream.close();

        return message;
    }

    @Override
    public final Object clone() throws java.lang.CloneNotSupportedException {
        throw new java.lang.CloneNotSupportedException();
    }

    public final void writeObject(ObjectOutputStream out) throws java.io.IOException {
        throw new java.io.NotSerializableException();
    }

    public final void readObject(ObjectInputStream in) throws java.io.IOException {
        throw new java.io.NotSerializableException();
    }
}
