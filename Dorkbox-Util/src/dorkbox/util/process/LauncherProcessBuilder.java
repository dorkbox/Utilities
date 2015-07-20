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

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public
class LauncherProcessBuilder extends ShellProcessBuilder {

    private String mainClass;

    private List<String> classpathEntries = new ArrayList<String>();
    private List<String> mainClassArguments = new ArrayList<String>();

    private String jarFile;

    public
    LauncherProcessBuilder() {
        super(null, null, null);
    }

    public
    LauncherProcessBuilder(InputStream in, PrintStream out, PrintStream err) {
        super(in, out, err);
    }

    public final
    void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public final
    void addJvmClasspath(String classpathEntry) {
        this.classpathEntries.add(classpathEntry);
    }

    public final
    void addJvmClasspaths(List<String> paths) {
        this.classpathEntries.addAll(paths);
    }

    public final
    void setJarFile(String jarFile) {
        this.jarFile = jarFile;
    }

    private
    String getClasspath() {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        final int totalSize = this.classpathEntries.size();
        final String pathseparator = File.pathSeparator;

        for (String classpathEntry : this.classpathEntries) {
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
    public
    void start() {
        if (OS.isWindows()) {
            setExecutable("dorkboxc.exe");
        }
        else {
            setExecutable("dorkbox");
        }


        // save off the original arguments
        List<String> origArguments = new ArrayList<String>(this.arguments.size());
        origArguments.addAll(this.arguments);
        this.arguments = new ArrayList<String>(0);

        this.arguments.add("-Xms40M");
        this.arguments.add("-Xmx256M");
//        arguments.add("-XX:PermSize=256M"); // default is 96

        this.arguments.add("-server");

        //same as -cp
        String classpath = getClasspath();

        // two more versions. jar vs classs
        if (this.jarFile != null) {
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
        else if (this.mainClass != null) {
            this.arguments.add(this.mainClass);

            if (!classpath.isEmpty()) {
                this.arguments.add("-classpath");
                this.arguments.add(classpath);
            }
        }
        else {
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
            }
            else {
                this.arguments.add(arg);
            }
        }

        this.arguments.addAll(origArguments);

        super.start();
    }
}
