package dorkbox.util.process;




import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dorkbox.util.OS;

/**
 * If you want to save off the output from the process, set a PrintStream to the following:
 *  ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8196);
 *  PrintStream outputStream = new PrintStream(byteArrayOutputStream);
 *  ...
 *  String output = byteArrayOutputStream.toString();
 */
public class ShellProcessBuilder {

    // TODO: see  http://mark.koli.ch/2009/12/uac-prompt-from-java-createprocess-error740-the-requested-operation-requires-elevation.html
    // for more information on copying files in windows with UAC protections.
    // maybe we want to hook into our launcher executor so that we can "auto elevate" commands?

    private String workingDirectory = null;
    private String executableName = null;
    private String executableDirectory = null;

    protected List<String> arguments = new ArrayList<String>();

    private final PrintStream outputStream;
    private final PrintStream errorStream;
    private final InputStream inputStream;

    private Process process = null;

    // true if we want to save off (usually for debugging) the initial output from this
    private boolean debugInfo = false;

    /**
     * This will cause the spawned process to pipe it's output to null.
     */
    public ShellProcessBuilder() {
        this(null, null, null);
    }

    public ShellProcessBuilder(PrintStream out) {
        this(null, out, out);
    }

    public ShellProcessBuilder(InputStream in, PrintStream out) {
        this(in, out, out);
    }

    public ShellProcessBuilder(InputStream in, PrintStream out, PrintStream err) {
        outputStream = out;
        errorStream = err;
        inputStream = in;
    }

    /**
     * When launched from eclipse, the working directory is USUALLY the root of the project folder
     */
    public final ShellProcessBuilder setWorkingDirectory(String workingDirectory) {
        // MUST be absolute path!!
        this.workingDirectory = new File(workingDirectory).getAbsolutePath();
        return this;
    }

    public final ShellProcessBuilder addArgument(String argument) {
        arguments.add(argument);
        return this;
    }

    public final ShellProcessBuilder addArguments(String... paths) {
        for (String path : paths) {
            arguments.add(path);
        }
        return this;
    }

    public final ShellProcessBuilder addArguments(List<String> paths) {
        arguments.addAll(paths);
        return this;
    }

    public final ShellProcessBuilder setExecutable(String executableName) {
        this.executableName = executableName;
        return this;
    }

    public ShellProcessBuilder setExecutableDirectory(String executableDirectory) {
        // MUST be absolute path!!
        this.executableDirectory = new File(executableDirectory).getAbsolutePath();
        return this;
    }

    public ShellProcessBuilder addDebugInfo() {
        this.debugInfo = true;
        return this;
    }


    public void start() {
        // if no executable, then use the command shell
        if (executableName == null) {
            if (OS.isWindows()) {
                // windows
                executableName = "cmd";
                arguments.add(0, "/c");

            } else {
                // *nix
                executableName = "/bin/bash";
                File file = new File(executableName);
                if (!file.canExecute()) {
                    executableName = "/bin/sh";
                }
                arguments.add(0, "-c");
            }
        } else if (workingDirectory != null) {
            if (!workingDirectory.endsWith("/") && !workingDirectory.endsWith("\\")) {
                workingDirectory += File.separator;
            }
        }

        if (executableDirectory != null) {
            if (!executableDirectory.endsWith("/") && !executableDirectory.endsWith("\\")) {
                executableDirectory += File.separator;
            }

            executableName = executableDirectory + executableName;
        }

        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add(executableName);

        for (String arg : arguments) {
            if (arg.contains(" ")) {
                // individual arguments MUST be in their own element in order to
                //  be processed properly (this is how it works on the command line!)
                String[] split = arg.split(" ");
                for (String s : split) {
                    argumentsList.add(s);
                }
            } else {
                argumentsList.add(arg);
            }
        }


        // if we don't want output... TODO: i think we want to "exec" (this calls exec -c, which calls our program)
        // this code as well, since calling it directly won't work
        boolean pipeToNull = errorStream == null || outputStream == null;
        if (pipeToNull) {
            if (OS.isWindows()) {
                // >NUL on windows
                argumentsList.add(">NUL");
            } else {
                // we will "pipe" it to /dev/null on *nix
                argumentsList.add(">/dev/null 2>&1");
            }
        }

        if (debugInfo) {
            errorStream.print("Executing: ");
            Iterator<String> iterator = argumentsList.iterator();
            while (iterator.hasNext()) {
                String s = iterator.next();
                errorStream.print(s);
                if (iterator.hasNext()) {
                    errorStream.print(" ");
                }
            }
            errorStream.print(OS.LINE_SEPARATOR);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(argumentsList);
        if (workingDirectory != null) {
            processBuilder.directory(new File(workingDirectory));
        }

        // combine these so output is properly piped to null.
        if (pipeToNull) {
            processBuilder.redirectErrorStream(true);
        }

        try {
            process = processBuilder.start();
        } catch (Exception ex) {
            errorStream.println("There was a problem executing the program.  Details:\n");
            ex.printStackTrace(errorStream);

            if (process != null) {
                try {
                    process.destroy();
                    process = null;
                } catch (Exception e) {
                    errorStream.println("Error destroying process: \n");
                    e.printStackTrace(errorStream);
                }
            }
        }

        if (process != null) {
            ProcessProxy writeToProcess_input;
            ProcessProxy readFromProcess_output;
            ProcessProxy readFromProcess_error;


            if (pipeToNull) {
                NullOutputStream nullOutputStream = new NullOutputStream();

                processBuilder.redirectErrorStream(true);

                // readers (read process -> write console)
                // have to keep the output buffers from filling in the target process.
                readFromProcess_output = new ProcessProxy("Process Reader: " + executableName, process.getInputStream(), nullOutputStream);
                readFromProcess_error = null;
            }
            // we want to pipe our input/output from process to ourselves
            else {
                /**
                 * Proxy the System.out and System.err from the spawned process back
                 * to the user's window. This is important or the spawned process could block.
                 */
                // readers (read process -> write console)
                readFromProcess_output = new ProcessProxy("Process Reader: " + executableName, process.getInputStream(), outputStream);
                if (errorStream != outputStream) {
                    readFromProcess_error = new ProcessProxy("Process Reader: " + executableName, process.getErrorStream(), errorStream);
                } else {
                    processBuilder.redirectErrorStream(true);
                    readFromProcess_error = null;
                }
            }

            if (inputStream != null) {
                /**
                 * Proxy System.in from the user's window to the spawned process
                 */
                // writer (read console -> write process)
                writeToProcess_input = new ProcessProxy("Process Writer: " + executableName, inputStream, process.getOutputStream());
            } else {
                writeToProcess_input = null;
            }


            // the process can be killed in two ways
            // If not in eclipse, by this shutdown hook. (clicking the red square to terminate a process will not run it's shutdown hooks)
            // Typing "exit" will always terminate the process
            Thread hook = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (debugInfo) {
                            errorStream.println("Terminating process: " + executableName);
                        }
                        process.destroy();
                    }
                }
             );
            // add a shutdown hook to make sure that we properly terminate our spawned processes.
            Runtime.getRuntime().addShutdownHook(hook);

            if (writeToProcess_input != null) {
                writeToProcess_input.start();
            }
            readFromProcess_output.start();
            if (readFromProcess_error != null) {
                readFromProcess_error.start();
            }

            try {
                process.waitFor();

                @SuppressWarnings("unused")
                int exitValue = process.exitValue();

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
                process.destroy();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // remove the shutdown hook now that we've shutdown.
            Runtime.getRuntime().removeShutdownHook(hook);
        }
    }
}