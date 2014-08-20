package dorkbox.util.process;






import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import dorkbox.util.OS;

public class LauncherProcessBuilder extends ShellProcessBuilder {

    private String mainClass;

    private List<String> classpathEntries = new ArrayList<String>();
    private List<String> mainClassArguments = new ArrayList<String>();

    private String jarFile;

    public LauncherProcessBuilder() {
        super(null, null, null);
    }

    public LauncherProcessBuilder(InputStream in, PrintStream out, PrintStream err) {
        super(in, out, err);
    }

    public final void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public final void addJvmClasspath(String classpathEntry) {
        classpathEntries.add(classpathEntry);
    }

    public final void addJvmClasspaths(List<String> paths) {
        classpathEntries.addAll(paths);
    }

    public final void setJarFile(String jarFile) {
        this.jarFile = jarFile;
    }

    private String getClasspath() {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        final int totalSize = classpathEntries.size();
        final String pathseparator = File.pathSeparator;

        for (String classpathEntry : classpathEntries) {
            // fix a nasty problem when spaces aren't properly escaped!
            classpathEntry = classpathEntry.replaceAll(" ", "\\ ");
            builder.append(classpathEntry);
            count++;
            if (count < totalSize) {
                builder.append(pathseparator); // ; on windows, : on linux
            }
        }
        return builder.toString();
    }

    @Override
    public void start() {
        if (OS.isWindows()) {
            setExecutable("dorkboxc.exe");
        } else {
            setExecutable("dorkbox");
        }


        // save off the original arguments
        List<String> origArguments = new ArrayList<String>(arguments.size());
        origArguments.addAll(arguments);
        arguments = new ArrayList<String>(0);

        arguments.add("-Xms40M");
        arguments.add("-Xmx256M");
//        arguments.add("-XX:PermSize=256M"); // default is 96

        arguments.add("-server");

        //same as -cp
        String classpath = getClasspath();

        // two more versions. jar vs classs
        if (jarFile != null) {
            // JAR is added by the launcher (based in the ini file!)
//            arguments.add("-jar");
//            arguments.add(jarFile);

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
            arguments.add(mainClass);

            if (!classpath.isEmpty()) {
                arguments.add("-classpath");
                arguments.add(classpath);
            }
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
}