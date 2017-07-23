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
package dorkbox.util;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.TimeZone;


@SuppressWarnings({"unused", "WeakerAccess"})
public
class OS {
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String LINE_SEPARATOR_UNIX = "\n";
    public static final String LINE_SEPARATOR_WINDOWS = "\r\n";

    public static final Charset US_ASCII = Charset.forName("US-ASCII");
    public static final Charset UTF_8 = Charset.forName("UTF-8");
    public static final Charset UTF_16LE = Charset.forName("UTF-16LE");

    public static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));

    /**
     * The currently running java version as a NUMBER. For example, "Java version 1.7u45", and converts it into 7
     */
    public static final int javaVersion = _getJavaVersion();


    private static final OSType osType;
    private static final String originalTimeZone = TimeZone.getDefault()
                                                           .getID();

    static {
        /**
         * By default, the timer resolution in some operating systems are not particularly high-resolution (ie: 'Thread.sleep(1)' will not
         * really sleep for 1ms, but will really sleep for 16ms). This forces the JVM to use high resolution timers. This is USUALLY
         * necessary on Windows.
         */
        Thread timerAccuracyThread = new Thread(new Runnable() {
            @Override
            public
            void run() {
                //noinspection InfiniteLoopStatement
                while (true) {
                    try {
                        Thread.sleep(Long.MAX_VALUE);
                    } catch (Exception ignored) {
                    }
                }
            }
        }, "ForceHighResTimer");
        timerAccuracyThread.setDaemon(true);
        timerAccuracyThread.start();


        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");

        if (osName != null && osArch != null) {
            osName = osName.toLowerCase(Locale.US);
            osArch = osArch.toLowerCase(Locale.US);

            if (osName.startsWith("linux")) {
                // best way to determine if it's android or not
                boolean isAndroid;
                try {
                    Class.forName("android.app.Activity");
                    isAndroid = true;
                } catch (ClassNotFoundException e) { isAndroid = false; }


                if (isAndroid) {
                    // android check from https://stackoverflow.com/questions/14859954/android-os-arch-output-for-arm-mips-x86
                    if (osArch.equals("armeabi")) {
                        // really old/low-end non-hf 32bit cpu
                        osType = OSType.AndroidArm56;
                    }
                    else if (osArch.equals("armeabi-v7a")) {
                        // 32bit hf cpu
                        osType = OSType.AndroidArm7;
                    }
                    else if (osArch.equals("arm64-v8a")) {
                        // 64bit hf cpu
                        osType = OSType.AndroidArm8;
                    }
                    else if (osArch.equals("x86")) {
                        // 32bit x86 (usually emulator)
                        osType = OSType.AndroidX86;
                    }
                    else if (osArch.equals("x86_64")) {
                        // 64bit x86 (usually emulator)
                        osType = OSType.AndroidX86_64;
                    }
                    else if (osArch.equals("mips")) {
                        // 32bit mips
                        osType = OSType.AndroidMips;
                    }
                    else if (osArch.equals("mips64")) {
                        // 64bit mips
                        osType = OSType.AndroidMips64;
                    } else {
                        // who knows?
                        osType = null;
                    }
                }
                else {
                    // normal linux 32/64/arm32/arm64
                    if ("amd64".equals(osArch)) {
                        osType = OSType.Linux64;
                    }
                    else {
                        if (osArch.startsWith("arm")) {
                            if (osArch.contains("v8")) {
                                osType = OSType.LinuxArm64;
                            }
                            else {
                                osType = OSType.LinuxArm32;
                            }
                        }
                        else {
                            osType = OSType.Linux32;
                        }
                    }
                }
            }
            else if (osName.startsWith("windows")) {
                if ("amd64".equals(osArch)) {
                    osType = OSType.Windows64;
                }
                else {
                    osType = OSType.Windows32;
                }
            }
            else if (osName.startsWith("mac") || osName.startsWith("darwin")) {
                if ("x86_64".equals(osArch)) {
                    osType = OSType.MacOsX64;
                }
                else {
                    osType = OSType.MacOsX32;
                }
            }
            else if (osName.startsWith("freebsd") || osName.contains("nix") || osName.contains("nux") || osName.startsWith("aix")) {
                if ("x86".equals(osArch) || "i386".equals(osArch)) {
                    osType = OSType.Unix32;
                }
                else if ("arm".equals(osArch)) {
                    osType = OSType.UnixArm;
                }
                else {
                    osType = OSType.Unix64;
                }
            }
            else if (osName.startsWith("solaris") || osName.startsWith("sunos")) {
                osType = OSType.Solaris;
            }
            else {
                osType = null;
            }
        }
        else {
            osType = null;
        }
    }

    public static
    OSType get() {
        return osType;
    }

    public static
    boolean is64bit() {
        return osType.is64bit();
    }

    public static
    boolean is32bit() {
        return osType.is32bit();
    }

    /**
     * @return true if this is a "standard" x86/x64 architecture (intel/amd/etc) processor.
     */
    public static
    boolean isX86() {
        return osType.isX86();
    }

    public static
    boolean isMips() {
        return osType.isMips();
    }

    public static
    boolean isArm() {
        return osType.isArm();
    }

    public static
    boolean isLinux() {
        return osType.isLinux();
    }

    public static
    boolean isUnix() {
        return osType.isUnix();
    }

    public static
    boolean isSolaris() {
        return osType.isSolaris();
    }

    public static
    boolean isWindows() {
        return osType.isWindows();
    }

    public static
    boolean isMacOsX() {
        return osType.isMacOsX();
    }

    public static
    boolean isAndroid() {
        return osType.isAndroid();
    }


    /**
     * Gets the currently running java version as a NUMBER. For example, "Java version 1.7u45", and converts it into 7
     */
    private static
    int _getJavaVersion() {
        String fullJavaVersion = System.getProperty("java.version");

        char versionChar;
        if (fullJavaVersion.startsWith("1.")) {
            versionChar = fullJavaVersion.charAt(2);
        }
        else {
            versionChar = fullJavaVersion.charAt(0);
        }

        switch (versionChar) {
            case '4':
                return 4;
            case '5':
                return 5;
            case '6':
                return 6;
            case '7':
                return 7;
            case '8':
                return 8;
            case '9':
                return 9;
            default:
                return -1;
        }
    }

    /**
     * Set our system to UTC time zone. Retrieve the <b>original</b> time zone via {@link #getOriginalTimeZone()}
     */
    public static
    void setUTC() {
        // have to set our default timezone to UTC. EVERYTHING will be UTC, and if we want local, we must explicitly ask for it.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Returns the *ORIGINAL* system time zone, before (*IF*) it was changed to UTC
     */
    public static
    String getOriginalTimeZone() {
        return originalTimeZone;
    }

    /**
     * @return the optimum number of threads for a given task. Makes certain not to take ALL the threads, always returns at least one
     * thread.
     */
    public static
    int getOptimumNumberOfThreads() {
        return Math.max(Runtime.getRuntime()
                               .availableProcessors() - 2, 1);
    }

    @Override
    public final
    Object clone() throws java.lang.CloneNotSupportedException {
        throw new java.lang.CloneNotSupportedException();
    }

    public final
    void writeObject(ObjectOutputStream out) throws java.io.IOException {
        throw new java.io.NotSerializableException();
    }

    public final
    void readObject(ObjectInputStream in) throws java.io.IOException {
        throw new java.io.NotSerializableException();
    }
}
