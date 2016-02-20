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
package dorkbox.util.process;

import dorkbox.util.OS;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * If you want to save off the output from the process, set a PrintStream to the following:
 * <pre> {@code
 *
 * ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8196);
 * PrintStream outputStream = new PrintStream(byteArrayOutputStream);
 * ...
 *
 * String output = ShellProcessBuilder.getOutput(byteArrayOutputStream);
 * }</pre>
 */
public
class ShellProcessBuilder {

    private final PrintStream outputStream;
    private final PrintStream outputErrorStream;
    private final InputStream inputStream;

    protected List<String> arguments = new ArrayList<String>();
    private String workingDirectory = null;
    private String executableName = null;
    private String executableDirectory = null;
    private Process process = null;

    // true if we want to save off (usually for debugging) the initial output from this
    private boolean debugInfo = false;
    private boolean createReadWriterThreads = false;

    private boolean isShell;
    private String pipeToNullString = "";

    /**
     * This will cause the spawned process to pipe it's output to null.
     */
    public
    ShellProcessBuilder() {
        this(null, null, null);
    }

    public
    ShellProcessBuilder(PrintStream out) {
        this(null, out, out);
    }

    public
    ShellProcessBuilder(InputStream in, PrintStream out) {
        this(in, out, out);
    }

    public
    ShellProcessBuilder(InputStream in, PrintStream out, PrintStream err) {
        this.inputStream = in;
        this.outputStream = out;
        this.outputErrorStream = err;
    }

    /**
     * Creates extra reader/writer threads for the sub-process. This is useful depending on how the sub-process is designed to run.
     * </p>
     * For a process you want interactive IO with, this is required.
     * </p>
     * For a long-running sub-process, with no interactive IO, this is what you'd want.
     * </p>
     * For a run-and-get-the-results process, this isn't recommended.
     *
     */
    public final
    ShellProcessBuilder createReadWriterThreads() {
        createReadWriterThreads = true;
        return this;
    }

    /**
     * When launched from eclipse, the working directory is USUALLY the root of the project folder
     */
    public final
    ShellProcessBuilder setWorkingDirectory(String workingDirectory) {
        // MUST be absolute path!!
        this.workingDirectory = new File(workingDirectory).getAbsolutePath();
        return this;
    }

    public final
    ShellProcessBuilder addArgument(String argument) {
        this.arguments.add(argument);
        return this;
    }

    public final
    ShellProcessBuilder addArguments(String... paths) {
        for (String path : paths) {
            this.arguments.add(path);
        }
        return this;
    }

    public final
    ShellProcessBuilder addArguments(List<String> paths) {
        this.arguments.addAll(paths);
        return this;
    }

    public final
    ShellProcessBuilder setExecutable(String executableName) {
        this.executableName = executableName;
        return this;
    }

    public
    ShellProcessBuilder setExecutableDirectory(String executableDirectory) {
        // MUST be absolute path!!
        this.executableDirectory = new File(executableDirectory).getAbsolutePath();
        return this;
    }

    public
    ShellProcessBuilder addDebugInfo() {
        this.debugInfo = true;
        return this;
    }

    public
    ShellProcessBuilder pipeOutputToNull() {
        if (OS.isWindows()) {
            // >NUL on windows
            pipeToNullString = ">NUL";
        }
        else {
            // we will "pipe" it to /dev/null on *nix
            pipeToNullString = ">/dev/null 2>&1";
        }

        return this;
    }


    public
    int start() {
        List<String> argumentsList = new ArrayList<String>();

        // if no executable, then use the command shell
        if (this.executableName == null) {
            isShell = true;

            if (OS.isWindows()) {
                // windows
                this.executableName = "cmd";

                argumentsList.add(this.executableName);
                argumentsList.add("/c");
            }
            else {
                // *nix
                this.executableName = "/bin/bash";

                File file = new File(this.executableName);
                if (!file.canExecute()) {
                    this.executableName = "/bin/sh";
                }

                argumentsList.add(this.executableName);
                argumentsList.add("-c");
            }
        }
        else {
            // shell and working/exe directory are mutually exclusive

            if (this.workingDirectory != null) {
                if (!this.workingDirectory.endsWith(File.separator)) {
                    this.workingDirectory += File.separator;
                }
            }

            if (this.executableDirectory != null) {
                if (!this.executableDirectory.endsWith(File.separator)) {
                    this.executableDirectory += File.separator;
                }

                argumentsList.add(0, this.executableDirectory + this.executableName);
            } else {
                argumentsList.add(this.executableName);
            }
        }


        // if we don't want output...
        boolean pipeToNull = !pipeToNullString.isEmpty();

        if (isShell && !OS.isWindows()) {
            // when a shell AND on *nix, we have to place ALL the args into a single "arg" that is passed in
            final StringBuilder stringBuilder = new StringBuilder(1024);

            for (String arg : this.arguments) {
                stringBuilder.append(arg).append(" ");
            }

            if (!arguments.isEmpty()) {
                if (pipeToNull) {
                    stringBuilder.append(pipeToNullString);
                }
                else {
                    // delete last " "
                    stringBuilder.delete(stringBuilder.length() - 1, stringBuilder.length());
                }
            }

            argumentsList.add(stringBuilder.toString());

        } else {
            for (String arg : this.arguments) {
                argumentsList.add(arg);
            }

            if (pipeToNull) {
                argumentsList.add(pipeToNullString);
            }
        }



        if (this.debugInfo) {
            if (outputErrorStream != null) {
                this.outputErrorStream.print("Executing: ");
            } else {
                System.err.print("Executing: ");
            }

            Iterator<String> iterator = argumentsList.iterator();
            while (iterator.hasNext()) {
                String s = iterator.next();
                if (outputErrorStream != null) {
                    this.outputErrorStream.print(s);
                } else {
                    System.err.print(s);
                }
                if (iterator.hasNext()) {
                    if (outputErrorStream != null) {
                        this.outputErrorStream.print(" ");
                    } else {
                        System.err.print(" ");
                    }
                }
            }

            if (outputErrorStream != null) {
                this.outputErrorStream.print(OS.LINE_SEPARATOR);
            } else {
                System.err.print(OS.LINE_SEPARATOR);
            }
        }




        ProcessBuilder processBuilder = new ProcessBuilder(argumentsList);
        if (this.workingDirectory != null) {
            processBuilder.directory(new File(this.workingDirectory));
        }

        // combine these so output is properly piped to null.
        if (pipeToNull || this.outputErrorStream == null) {
            processBuilder.redirectErrorStream(true);
        }

        try {
            this.process = processBuilder.start();
        } catch (Exception ex) {
            if (outputErrorStream != null) {
                this.outputErrorStream.println("There was a problem executing the program.  Details:");
            } else {
                System.err.println("There was a problem executing the program.  Details:");
            }
            ex.printStackTrace(this.outputErrorStream);

            if (this.process != null) {
                try {
                    this.process.destroy();
                    this.process = null;
                } catch (Exception e) {
                    if (outputErrorStream != null) {
                        this.outputErrorStream.println("Error destroying process:");
                    } else {
                        System.err.println("Error destroying process:");
                    }
                    e.printStackTrace(this.outputErrorStream);
                }
            }
        }

        if (this.process != null) {
            ProcessProxy writeToProcess_input = null;
            ProcessProxy readFromProcess_output = null;
            ProcessProxy readFromProcess_error = null;

            if (this.outputErrorStream == null && this.outputStream == null) {
                if (!pipeToNull) {
                    NullOutputStream nullOutputStream = new NullOutputStream();

                    // readers (read process -> write console)
                    // have to keep the output buffers from filling in the target process.
                    readFromProcess_output = new ProcessProxy("Process Reader: " + this.executableName,
                                                              this.process.getInputStream(),
                                                              nullOutputStream);
                }
            }
            // we want to pipe our input/output from process to ourselves
            else {
                /**
                 * Proxy the System.out and System.err from the spawned process back
                 * to the user's window. This is important or the spawned process could block.
                 */
                // readers (read process -> write console)
                readFromProcess_output = new ProcessProxy("Process Reader: " + this.executableName,
                                                          this.process.getInputStream(),
                                                          this.outputStream);

                if (this.outputErrorStream != this.outputStream) {
                    readFromProcess_error = new ProcessProxy("Process Reader: " + this.executableName,
                                                             this.process.getErrorStream(),
                                                             this.outputErrorStream);
                }
            }

            if (this.inputStream != null) {
                /**
                 * Proxy System.in from the user's window to the spawned process
                 */
                // writer (read console -> write process)
                writeToProcess_input = new ProcessProxy("Process Writer: " + this.executableName,
                                                        this.inputStream,
                                                        this.process.getOutputStream());
            }


            // the process can be killed in two ways
            // If not in eclipse, by this shutdown hook. (clicking the red square to terminate a process will not run it's shutdown hooks)
            // Typing "exit" will always terminate the process
            Thread hook = new Thread(new Runnable() {
                @Override
                public
                void run() {
                    if (ShellProcessBuilder.this.debugInfo) {
                        final PrintStream errorStream = ShellProcessBuilder.this.outputErrorStream;
                        if (errorStream != null) {
                            errorStream.println("Terminating process: " + ShellProcessBuilder.this.executableName);
                        }
                    }
                    ShellProcessBuilder.this.process.destroy();
                }
            });
            // add a shutdown hook to make sure that we properly terminate our spawned processes.
            Runtime.getRuntime()
                   .addShutdownHook(hook);

            if (writeToProcess_input != null) {
                if (createReadWriterThreads) {
                    writeToProcess_input.start();
                }
                else {
                    writeToProcess_input.run();
                }
            }

            if (createReadWriterThreads) {
                readFromProcess_output.start();
            }
            else {
                readFromProcess_output.run();
            }
            if (readFromProcess_error != null) {
                if (createReadWriterThreads) {
                    readFromProcess_error.start();
                }
                else {
                    readFromProcess_error.run();
                }
            }

            int exitValue = 0;

            try {
                this.process.waitFor();

                exitValue = this.process.exitValue();

                // wait for the READER threads to die (meaning their streams have closed/EOF'd)
                if (writeToProcess_input != null) {
                    // the INPUT (from stdin). It should be via the InputConsole, but if it's in eclipse,etc -- then this doesn't do anything
                    // We are done reading input, since our program has closed...
                    writeToProcess_input.close();
                    writeToProcess_input.join();
                }
                readFromProcess_output.close();
                readFromProcess_output.join();
                if (readFromProcess_error != null) {
                    readFromProcess_error.close();
                    readFromProcess_error.join();
                }

                // forcibly terminate the process when it's streams have closed.
                // this is for cleanup ONLY, not to actually do anything.
                this.process.destroy();
            } catch (InterruptedException e) {
                Thread.currentThread()
                      .interrupt();
            }

            // remove the shutdown hook now that we've shutdown.
            Runtime.getRuntime()
                   .removeShutdownHook(hook);

            return exitValue;
        }

        // 1 means a problem
        return 1;
    }

    /**
     * Converts the baos to a string in a safe way. There might be a trailing newline character at the end of this output.
     *
     * @param byteArrayOutputStream the baos that is used in the {@link ShellProcessBuilder#ShellProcessBuilder(PrintStream)} (or similar
     *                              calls)
     *
     * @return A string representing the output of the process
     */
    public static
    String getOutput(final ByteArrayOutputStream byteArrayOutputStream) {
        String s;
        synchronized (byteArrayOutputStream) {
            s = byteArrayOutputStream.toString();
            byteArrayOutputStream.reset();
        }

        return s;
    }
}
