/*
 * Copyright 2026 dorkbox, llc
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
package dorkbox.exit

import dorkbox.os.OS
import java.io.*
import java.util.*
import java.util.concurrent.atomic.*


/**
 * The EXIT system uses ERRORS to exit the application. We must make sure NOT to swallow them higher up! -- so don't catch "throwable"!
 */
object Exit {
    /**
     * Used to set the data that will be
     * 1) used for relaunch,
     * 2) used to display an error message on exit
     */
    private external fun setExitError(data: String?)

    private val isNative0: Boolean
        external get

    val isNative: Boolean
        /**
         * Check to see if we are native.
         * Used for determining how exits are handled.
         */
        get() {
            return try {
                isNative0
            }
            catch (_: Throwable) {
                false
            }
        }

    // MIN/MAX values are -128 - 127, because of parent process limitations (anything over 127 will be converted to negative)
    private const val UNDEFINED       = -1 // must match C source (also anything < 0)
    private const val NORMAL          = 0 // must match C source
    private const val FAILED_CONFIG   = 1 // when we are trying to configure something, and it fails. Generally this is for required configurations
    private const val FAILED_INIT     = 2
    private const val FAILED_SECURITY = 3

    // must match C source!!
    private const val RESERVED = 100 // must match C source
    private val RESERVED_LIST  = mutableListOf<Int>()

    private const val SAVED_IN_LOG_FILE  = 101 // must match C source
    private const val RESTART            = 102 // must match C source
    private const val UPGRADE_EXECUTABLE = 103 // must match C source

    init {
        RESERVED_LIST.add(SAVED_IN_LOG_FILE)
        RESERVED_LIST.add(RESTART)
        RESERVED_LIST.add(UPGRADE_EXECUTABLE)
    }


    // so, it is important to observe, that WHILE the application is starting (Launcher.java),
    // throwing exceptions is ACCEPTABLE. Afterwards, we MUST rely on the blocking structure to notify
    // the main thread, so it can return normally.
    private val exitValue = AtomicInteger(NORMAL)
    private var exitError: ExitBase? = null

    // This is what tells us that we are DONE launching and have moved onto the bootstrap.
    fun getExitValue(): Int {
        if (exitError != null) {
            _setExitData(exitError!!)
        }

        val exit: Int = exitValue.get()
        if (exit > RESERVED) {
            if (RESERVED_LIST.contains(exit)) {
                return exit
            }
            else {
                setExitError("You cannot use any number greater than 99")
                return RESERVED
            }
        }
        else {
            return exit
        }
    }

    /**
     * Only used on the inside of a thrown exception by the main thread
     *
     * sets the exit data (ONLY IF there isn't already an error set in the system) then gets the return code
     */
     fun <T: ExitBase> getExitDuringException(exitError: T?): Int {
        if (Exit.exitError != null) {
            _setExitData(Exit.exitError!!)
        }
        else {
            _setExitData(exitError!!)
        }

        // just in case.
        val exit: Int = exitValue.get()
        if (exit > RESERVED) {
            if (RESERVED_LIST.contains(exit)) {
                return exit
            }
            else {
                setExitError("You cannot use any number greater than 99")
                return RESERVED
            }
        }
        else {
            return exit
        }
    }

    /**
     * Writes a message to the log file
     */
    fun writeToLogFile(title: String?, message: String?) {
        try {
            val output: Writer = BufferedWriter(FileWriter("error.log", true))

            try {
                if (message != null) {
                    // FileWriter always assumes default encoding is OK
                    if (title != null) {
                        output.write(title)
                        output.write(OS.LINE_SEPARATOR + "   ")
                    }

                    output.write(Date().toString() + OS.LINE_SEPARATOR + "     ")
                    output.write(message)
                    output.write(OS.LINE_SEPARATOR)
                }
                else {
                    output.write("Execption thrown! Unknown error.")
                }
                output.write(OS.LINE_SEPARATOR)
            }
            finally {
                output.close()
            }
        }
        catch (e: IOException) {
        }
    }



    /**
     * called by the uncaught exception handler.
     */
    fun handleUncaughtException(error: Throwable) {
        // only manage this if it's not on purpose!
        if (error !is ExitBase) {
            // Not always undefined, although sometimes it is.
            var message = error.message
            if (message == null) {
                message = getStackStrace(error)
            }

            writeToLogFile("Uncaught Exception: " + error.javaClass, message)

            // if we are launching, then throw the error. If we FINISHED launching, trip the block
            // if we are closing (happens with a LEGIT error), then do nothing, since we are already
            // handling it.
            _throwIfLaunching(
                ExitError(
                    UNDEFINED, "Abnormal termination from an uncaught exception! See log file.)"
                )
            )
        }
    }


    /**
     * Undefined/unknown exit, and the info has been written to the log file.
     */
    fun Undefined(throwable: Throwable): Int {
        // Not always undefined, although sometimes it is.
        var message = throwable.message
        if (message == null) {
            val byteArrayOutputStream = ByteArrayOutputStream(8196)
            val printStream = PrintStream(byteArrayOutputStream)
            throwable.printStackTrace(printStream)
            message = byteArrayOutputStream.toString()
            printStream.close()
        }

        message = throwable.javaClass.toString() + " : " + message

        // undefined exit! The launcher will restart the JVM in this case
        writeToLogFile("Error", message)

        // will actually return the ACTUAL exit error, if there is one.
        return Generic(UNDEFINED, "Undefined exception! Saved in log file.", message)
    }

    /**
     * Exit normally (from the launcher).
     */
    fun Normal(): Int {
        return Generic(NORMAL, "Normal exit called.")
    }

    /**
     * when we are trying to configure something, and it fails. Generally this is for required configurations
     */
    fun FailedConfiguration(errorMessage: String?, throwable: Throwable): Int {
        return Generic(FAILED_CONFIG, "FailedConfiguration called: " + errorMessage, throwable)
    }

    /**
     * when we are trying to configure something, and it fails. Generally this is for required configurations
     */
    fun FailedInitialization(throwable: Throwable): Int {
        return Generic(FAILED_INIT, "FailedInitialization called: " + throwable.message, throwable)
    }

    fun FailedConfiguration(errorMessage: String?): Int {
        return Generic(FAILED_CONFIG, "FailedConfiguration called: " + errorMessage)
    }

    fun FailedInitialization(errorMessage: String?, throwable: Throwable): Int {
        return Generic(FAILED_INIT, "FailedInitialization called: " + errorMessage, throwable)
    }

    fun FailedInitialization(errorMessage: String?): Int {
        return Generic(FAILED_INIT, "FailedInitialization called: " + errorMessage)
    }

    fun FailedSecurity(throwable: Throwable): Int {
        return Generic(FAILED_SECURITY, "FailedSecurity called: " + throwable.message, throwable)
    }

    fun FailedSecurity(errorMessage: String?, throwable: Throwable): Int {
        return Generic(FAILED_SECURITY, "FailedSecurity called: " + errorMessage, throwable)
    }

    fun FailedSecurity(errorMessage: String?): Int {
        return Generic(FAILED_SECURITY, "FailedSecurity called: " + errorMessage)
    }



    fun Generic(exitCode: Int, errorMessage: String?, throwable: Throwable): Int {
        return Generic(
            exitCode, errorMessage + OS.LINE_SEPARATOR + throwable.javaClass + OS.LINE_SEPARATOR + throwable.message
        )
    }

    fun Generic(exitCode: Int, errorMessage: String?): Int {
        // if we are launching, then throw the error. If we FINISHED launching, trip the block
        _throwIfLaunching(ExitError(exitCode, errorMessage))
        return exitCode
    }

    fun Generic(exitCode: Int, errorTitle: String?, errorMessage: String?): Int {
        // if we are launching, then throw the error. If we FINISHED launching, trip the block
        _throwIfLaunching(ExitError(exitCode, errorTitle, errorMessage))
        return exitCode
    }

    /**
     * restart the application with the current arguments.
     * If we need to modify launch args, use the ini file. (or create a new one)
     */
    fun Restart() {
        _throwIfLaunching(ExitRestart(RESTART))
    }

    /**
     * Specify that we want to upgrade the launcher executable. Other types of upgrade should use
     * restart, where the launcher will automatically detect and upgrade the components in place.
     */
    fun UpgradeExectuable() {
        _throwIfLaunching(ExitRestart(UPGRADE_EXECUTABLE))
    }


    fun <T: ExitBase> _setExitData(exit: T) {
        exitValue.set(exit.exitCode)

        // exitData is passed to the launcher.
        if (exit.message != null) {
            val message = exit.message
            if (isNative) {
                if (exit.title != null) {
                    // can set the title if we want to. Normally it's just the program name.
                    setExitError("<title>" + exit.title + "</title>" + OS.LINE_SEPARATOR + message)
                }
                else {
                    setExitError(message)
                }
            }
        }
    }

    fun _setExitCode(exitCode: Int) {
        exitValue.set(exitCode)
    }

    // throw error if we are still launcher, otherwise set and notify the block
    fun _throwIfLaunching(exitError: ExitBase?) {
        // save the error. It's not always passed up the chain.
        if (exitError != null && Exit.exitError == null) {
            Exit.exitError = exitError
        }

        // throw the error. This makes sure we exit/quit the thread we are currently in.
        // we will end up in our "uncaught exception handler"!!
        if (exitError != null) {
            throw exitError
        }
        else {
            throw ExitError(FAILED_INIT, "Unable to have a null errorMessage!")
        }
    }

    /**
     * Utility method to get the stack trace from an exception, and convert it to a string.
     */
    fun getStackStrace(throwable: Throwable): String {
        val byteArrayOutputStream = ByteArrayOutputStream(8196)
        val printStream = PrintStream(byteArrayOutputStream)
        throwable.printStackTrace(printStream)

        val message: String = byteArrayOutputStream.toString()

        printStream.flush()
        printStream.close()

        return message
    }
}
