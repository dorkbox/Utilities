package dorkbox.util.process;

/**
 *
 */
public
class ShellAsyncExecutor {
    /**
     * This is a convenience method to easily create a default process. Will immediately return, and does not wait for the process to finish
     *
     * @param executableName the name of the executable to run
     * @param args the arguments for the executable
     *
     * @return true if the process ran successfully (exit value was 0), otherwise false
     */
    public static void run(String executableName, String... args) {
        ShellExecutor shell = new ShellExecutor();
        shell.setExecutable(executableName);
        shell.addArguments(args);
        shell.createReadWriterThreads();

        shell.start(false);
    }
}
