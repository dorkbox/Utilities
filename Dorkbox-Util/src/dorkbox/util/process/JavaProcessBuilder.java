package dorkbox.util.process;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import dorkbox.util.OS;

/**
 * This will FORK the java process initially used to start the currently running JVM. Changing the java executable will change this behaviors
 */
public class JavaProcessBuilder extends ShellProcessBuilder {

    // this is NOT related to JAVA_HOME, but is instead the location of the JRE that was used to launch java initially.
    private String javaLocation = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    private String mainClass;
    private int startingHeapSizeInMegabytes = 40;
    private int maximumHeapSizeInMegabytes = 128;

    private List<String> jvmOptions = new ArrayList<String>();
    private List<String> classpathEntries = new ArrayList<String>();
    private List<String> mainClassArguments = new ArrayList<String>();

    private String jarFile;

    public JavaProcessBuilder() {
        super(null, null, null);
    }

    // what version of java??
    // so, this starts a NEW java, from an ALREADY existing java.

    public JavaProcessBuilder(InputStream in, PrintStream out, PrintStream err) {
        super(in, out, err);
    }

    public final void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public final void setStartingHeapSizeInMegabytes(int startingHeapSizeInMegabytes) {
        this.startingHeapSizeInMegabytes = startingHeapSizeInMegabytes;
    }

    public final void setMaximumHeapSizeInMegabytes(int maximumHeapSizeInMegabytes) {
        this.maximumHeapSizeInMegabytes = maximumHeapSizeInMegabytes;
    }

    public final void addJvmClasspath(String classpathEntry) {
        classpathEntries.add(classpathEntry);
    }

    public final void addJvmClasspaths(List<String> paths) {
        classpathEntries.addAll(paths);
    }

    public final void addJvmOption(String argument) {
        jvmOptions.add(argument);
    }

    public final void addJvmOptions(List<String> paths) {
        jvmOptions.addAll(paths);
    }

    public final void setJarFile(String jarFile) {
        this.jarFile = jarFile;
    }

    private String getClasspath() {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        final int totalSize = classpathEntries.size();
        final String pathseparator = File.pathSeparator;

        // DO NOT QUOTE the elements in the classpath!
        for (String classpathEntry : classpathEntries) {
            try {
                // make sure the classpath is ABSOLUTE pathname
                classpathEntry = new File(classpathEntry).getCanonicalFile().getAbsolutePath();

                // fix a nasty problem when spaces aren't properly escaped!
                classpathEntry = classpathEntry.replaceAll(" ", "\\ ");

                builder.append(classpathEntry);
                count++;
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (count < totalSize) {
                builder.append(pathseparator); // ; on windows, : on linux
            }
        }
        return builder.toString();
    }

    /**
     * Specify the JAVA exectuable to launch this process. By default, this will use the same java exectuable
     * as was used to start the current JVM.
     */
    public void setJava(String javaLocation) {
        this.javaLocation = javaLocation;
    }

    @Override
    public void start() {
        setExecutable(javaLocation);

        // save off the original arguments
        List<String> origArguments = new ArrayList<String>(arguments.size());
        origArguments.addAll(arguments);
        arguments = new ArrayList<String>(0);


        // two versions, java vs not-java
        arguments.add("-Xms" + startingHeapSizeInMegabytes + "M");
        arguments.add("-Xmx" + maximumHeapSizeInMegabytes + "M");
        arguments.add("-server");

        for (String option : jvmOptions) {
            arguments.add(option);
        }

        //same as -cp
        String classpath = getClasspath();

        // two more versions. jar vs classs
        if (jarFile != null) {
            arguments.add("-jar");
            arguments.add(jarFile);

            // interesting note. You CANNOT have a classpath specified on the commandline
            // when using JARs!! It must be set in the jar's MANIFEST.
            if (!classpath.isEmpty()) {
                System.err.println("WHOOPS.  You CANNOT have a classpath specified on the commandline when using JARs.");
                System.err.println("    It must be set in the JARs MANIFEST instead.");
                System.exit(1);
            }

        }
        // if we are running classes!
        else if (mainClass != null) {
            if (!classpath.isEmpty()) {
                arguments.add("-classpath");
                arguments.add(classpath);
            }

            // main class must happen AFTER the classpath!
            arguments.add(mainClass);
        } else {
            System.err.println("WHOOPS. You must specify a jar or main class when running java!");
            System.exit(1);
        }


        for (String arg : mainClassArguments) {
            if (arg.contains(" ")) {
                // individual arguments MUST be in their own element in order to
                //  be processed properly (this is how it works on the command line!)
                String[] split = arg.split(" ");
                for (String s : split) {
                    arguments.add(s);
                }
            } else {
                arguments.add(arg);
            }
        }

        arguments.addAll(origArguments);

        super.start();
    }


    /** The directory into which a local VM installation should be unpacked. */
    public static final String LOCAL_JAVA_DIR = "java_vm";

    /**
     * Reconstructs the path to the JVM used to launch this process.
     *
     * @param windebug if true we will use java.exe instead of javaw.exe on Windows.
     */
    public static String getJVMPath (File appdir, boolean windebug)
    {
        // first look in our application directory for an installed VM
        String vmpath = checkJvmPath(new File(appdir, LOCAL_JAVA_DIR).getPath(), windebug);

        // then fall back to the VM in which we're already running
        if (vmpath == null) {
            vmpath = checkJvmPath(System.getProperty("java.home"), windebug);
        }

        // then throw up our hands and hope for the best
        if (vmpath == null) {
            System.err.println("Unable to find java [appdir=" + appdir + ", java.home=" + System.getProperty("java.home") + "]!");
            vmpath = "java";
        }

        // Oddly, the Mac OS X specific java flag -Xdock:name will only work if java is launched
        // from /usr/bin/java, and not if launched by directly referring to <java.home>/bin/java,
        // even though the former is a symlink to the latter! To work around this, see if the
        // desired jvm is in fact pointed to by /usr/bin/java and, if so, use that instead.
        if (OS.isMacOsX()) {
            try {
                File localVM = new File("/usr/bin/java").getCanonicalFile();
                if (localVM.equals(new File(vmpath).getCanonicalFile())) {
                    vmpath = "/usr/bin/java";
                }
            } catch (IOException ioe) {
                System.err.println("Failed to check Mac OS canonical VM path." + ioe);
            }
        }

        return vmpath;
    }

    /**
     * Checks whether a Java Virtual Machine can be located in the supplied path.
     */
    private static String checkJvmPath(String vmhome, boolean windebug) {
        // linux does this...
        String vmbase = vmhome + File.separator + "bin" + File.separator;
        String vmpath = vmbase + "java";
        if (new File(vmpath).exists()) {
            return vmpath;
        }

        // windows does this
        if (!windebug) {
            vmpath = vmbase + "javaw.exe";
        } else {
            vmpath = vmbase + "java.exe"; // open a console on windows
        }

        if (new File(vmpath).exists()) {
            return vmpath;
        }

        return null;
    }
}