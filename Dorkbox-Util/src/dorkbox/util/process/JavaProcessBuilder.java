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

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import dorkbox.util.FileUtil;
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
        this.classpathEntries.add(classpathEntry);
    }

    public final void addJvmClasspaths(List<String> paths) {
        this.classpathEntries.addAll(paths);
    }

    public final void addJvmOption(String argument) {
        this.jvmOptions.add(argument);
    }

    public final void addJvmOptions(List<String> paths) {
        this.jvmOptions.addAll(paths);
    }

    public final void setJarFile(String jarFile) {
        this.jarFile = jarFile;
    }

    private String getClasspath() {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        final int totalSize = this.classpathEntries.size();
        final String pathseparator = File.pathSeparator;

        // DO NOT QUOTE the elements in the classpath!
        for (String classpathEntry : this.classpathEntries) {
            try {
                // make sure the classpath is ABSOLUTE pathname
                classpathEntry = FileUtil.normalize(new File(classpathEntry).getAbsolutePath());

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
        setExecutable(this.javaLocation);

        // save off the original arguments
        List<String> origArguments = new ArrayList<String>(this.arguments.size());
        origArguments.addAll(this.arguments);
        this.arguments = new ArrayList<String>(0);


        // two versions, java vs not-java
        this.arguments.add("-Xms" + this.startingHeapSizeInMegabytes + "M");
        this.arguments.add("-Xmx" + this.maximumHeapSizeInMegabytes + "M");
        this.arguments.add("-server");

        for (String option : this.jvmOptions) {
            this.arguments.add(option);
        }

        //same as -cp
        String classpath = getClasspath();

        // two more versions. jar vs classs
        if (this.jarFile != null) {
            this.arguments.add("-jar");
            this.arguments.add(this.jarFile);

            // interesting note. You CANNOT have a classpath specified on the commandline
            // when using JARs!! It must be set in the jar's MANIFEST.
            if (!classpath.isEmpty()) {
                System.err.println("WHOOPS.  You CANNOT have a classpath specified on the commandline when using JARs.");
                System.err.println("    It must be set in the JARs MANIFEST instead.");
                System.exit(1);
            }

        }
        // if we are running classes!
        else if (this.mainClass != null) {
            if (!classpath.isEmpty()) {
                this.arguments.add("-classpath");
                this.arguments.add(classpath);
            }

            // main class must happen AFTER the classpath!
            this.arguments.add(this.mainClass);
        } else {
            System.err.println("WHOOPS. You must specify a jar or main class when running java!");
            System.exit(1);
        }


        for (String arg : this.mainClassArguments) {
            if (arg.contains(" ")) {
                // individual arguments MUST be in their own element in order to
                //  be processed properly (this is how it works on the command line!)
                String[] split = arg.split(" ");
                for (String s : split) {
                    this.arguments.add(s);
                }
            } else {
                this.arguments.add(arg);
            }
        }

        this.arguments.addAll(origArguments);

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
            String localVM = FileUtil.normalize(new File("/usr/bin/java").getAbsolutePath());
            String vmCheck = FileUtil.normalize(new File(vmpath).getAbsolutePath());
            if (localVM.equals(vmCheck)) {
                vmpath = "/usr/bin/java";
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