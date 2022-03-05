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
@file:Suppress("unused")

package dorkbox.os

import java.awt.Color
import java.io.File
import java.security.AccessController
import java.security.PrivilegedAction
import java.util.*

object OS {
    // make the default unix
    val LINE_SEPARATOR = getProperty("line.separator", "\n")

    const val LINE_SEPARATOR_UNIX = "\n"
    const val LINE_SEPARATOR_MACOS = "\r"
    const val LINE_SEPARATOR_WINDOWS = "\r\n"

    val TEMP_DIR = File(getProperty("java.io.tmpdir", "temp")).absoluteFile

    /**
     * The currently running MAJOR java version as a NUMBER. For example, "Java version 1.7u45", and converts it into 7, uses JEP 223 for java > 9
     */
    val javaVersion: Int by lazy {
        // this should never be a problem, but just in case
        var fullJavaVersion = getProperty("java.version", "8")
        if (fullJavaVersion.startsWith("1.")) {
            when (fullJavaVersion[2]) {
                '4' -> 4
                '5' -> 5
                '6' -> 6
                '7' -> 7
                '8' -> 8
                '9' -> 9
                else -> {
                    8
                }
            }
        } else {
            // We are >= java 10, use JEP 223 to get the version (early releases of 9 might not have JEP 223, so 10 is guaranteed to have it)
            fullJavaVersion = getProperty("java.specification.version", "10")

            try {
                // it will ALWAYS be the major release version as an integer. See http://openjdk.java.net/jeps/223
                fullJavaVersion.toInt()
            } catch (ignored: Exception) {
            }
        }

        // the last valid guess we have, since the current Java implementation, whatever it is, decided not to cooperate with JEP 223.
        8
    }

    /**
     * Returns the *ORIGINAL* system time zone, before (*IF*) it was changed to UTC
     */
    val originalTimeZone = TimeZone.getDefault().id

    /**
     * JVM reported osName, the default (if there is none detected) is 'linux'
     */
    val osName = getProperty("os.name", "linux").lowercase()

    /**
     * JVM reported osArch, the default (if there is none detected) is 'amd64'
     */
    val osArch = getProperty("os.arch", "amd64").lowercase()

    /**
     * @return the optimum number of threads for a given task. Makes certain not to take ALL the threads, always returns at least one
     * thread.
     */
    val optimumNumberOfThreads = (Runtime.getRuntime().availableProcessors() - 2).coerceAtLeast(1)

    /**
     * The determined OS type
     */
    val type: OSType by lazy {
        if (osName.startsWith("linux")) {
            // best way to determine if it's android.
            // Sometimes java binaries include Android classes on the classpath, even if it isn't actually Android, so we check the VM
            val isAndroid = "Dalvik" == getProperty("java.vm.name", "")
            if (isAndroid) {
                // android check from https://stackoverflow.com/questions/14859954/android-os-arch-output-for-arm-mips-x86
                when (osArch) {
                    "armeabi" -> {
                        OSType.AndroidArm56 // really old/low-end non-hf 32bit cpu
                    }
                    "armeabi-v7a" -> {
                        OSType.AndroidArm7 // 32bit hf cpu
                    }
                    "arm64-v8a" -> {
                        OSType.AndroidArm8  // 64bit hf cpu
                    }
                    "x86" -> {
                        OSType.AndroidX86 // 32bit x86 (usually emulator)
                    }
                    "x86_64" -> {
                        OSType.AndroidX86_64 // 64bit x86 (usually emulator)
                    }
                    "mips" -> {
                        OSType.AndroidMips // 32bit mips
                    }
                    "mips64" -> {
                        OSType.AndroidMips64  // 64bit mips
                    }
                    else -> {
                        throw java.lang.RuntimeException("Unable to determine OS type for $osName $osArch")
                    }
                }
            } else {
                // http://mail.openjdk.java.net/pipermail/jigsaw-dev/2017-April/012107.html
                when(osArch) {
                    "i386", "x86" -> {
                        OSType.Linux32
                    }
                    "arm" -> {
                        OSType.LinuxArm32
                    }

                    "x86_64", "amd64" -> {
                        OSType.Linux64
                    }
                    "aarch64" -> {
                        OSType.LinuxArm64
                    }
                    else -> {
                        when {
                            // oddballs (android usually)
                            osArch.startsWith("arm64") -> {
                                OSType.LinuxArm64
                            }
                            osArch.startsWith("arm") -> {
                                if (osArch.contains("v8")) {
                                    OSType.LinuxArm64
                                } else {
                                    OSType.LinuxArm32
                                }
                            }
                            else -> {
                                throw java.lang.RuntimeException("Unable to determine OS type for $osName $osArch")
                            }
                        }
                    }
                }
            }
        } else if (osName.startsWith("windows")) {
            if ("amd64" == osArch) {
                OSType.Windows64
            } else {
                OSType.Windows32
            }
        } else if (osName.startsWith("mac") || osName.startsWith("darwin")) {
            when (osArch) {
                "x86_64", "aarch64" -> {
                    OSType.MacOsX64
                }
                else -> {
                    OSType.MacOsX32  // new macosx is no longer 32 bit, but just in case.
                }
            }
        } else if (osName.startsWith("freebsd") ||
            osName.contains("nix") ||
            osName.contains("nux") ||
            osName.startsWith("aix")) {
            when (osArch) {
                "x86", "i386" -> {
                    OSType.Unix32
                }
                "arm" -> {
                    OSType.UnixArm
                }
                else -> {
                    OSType.Unix64
                }
            }
        } else if (osName.startsWith("solaris") ||
            osName.startsWith("sunos")) {
            OSType.Solaris
        } else {
            throw java.lang.RuntimeException("Unable to determine OS type for $osName $osArch")
        }
    }

    init {
        if (!TEMP_DIR.isDirectory) {
            // create the temp dir if necessary because the TEMP dir doesn't exist.
            TEMP_DIR.mkdirs()
        }

        /*
         * By default, the timer resolution on Windows ARE NOT high-resolution (16ms vs 1ms)
         *
         * 'Thread.sleep(1)' will not really sleep for 1ms, but will really sleep for ~16ms. This long-running sleep will trick Windows
         *  into using higher resolution timers.
         *
         * See: https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6435126
         */
        if (type.isWindows) {
            // only necessary on windows
            val timerAccuracyThread = Thread(
            {
                 while (true) {
                     try {
                         Thread.sleep(Long.MAX_VALUE)
                     } catch (ignored: Exception) {
                     }
                 }
            }, "FixWindowsHighResTimer")
            timerAccuracyThread.isDaemon = true
            timerAccuracyThread.start()
        }
    }

    /**
     * @return the System Property in a safe way for a given property, or null if it does not exist.
     */
    fun getProperty(property: String): String? {
        return try {
            if (System.getSecurityManager() == null) {
                System.getProperty(property, null)
            } else {
                AccessController.doPrivileged(PrivilegedAction { System.getProperty(property, null) })
            }
        } catch (ignored: Exception) {
            null
        }
    }

    /**
     * @return the value of the Java system property with the specified `property`, while falling back to the
     * specified default value if the property access fails.
     */
    fun getProperty(property: String, defaultValue: String): String {
        return try {
            if (System.getSecurityManager() == null) {
                System.getProperty(property, defaultValue)
            } else {
                AccessController.doPrivileged(PrivilegedAction { System.getProperty(property, defaultValue) })
            }
        } catch (ignored: Exception) {
            defaultValue
        }
    }

    /**
     * @return the value of the Java system property with the specified `property`, while falling back to the
     * specified default value if the property access fails.
     */
    fun getBoolean(property: String, defaultValue: Boolean): Boolean {
        var value = getProperty(property) ?: return defaultValue
        value = value.trim { it <= ' ' }.lowercase(Locale.getDefault())
        if (value.isEmpty()) {
            return defaultValue
        }

        if ("false" == value || "no" == value || "0" == value) {
            return false
        }

        return if ("true" == value || "yes" == value || "1" == value) {
            true
        } else defaultValue
    }

    /**
     * @return the value of the Java system property with the specified `property`, while falling back to the
     * specified default value if the property access fails.
     */
    fun getInt(property: String, defaultValue: Int): Int {
        var value = getProperty(property) ?: return defaultValue
        value = value.trim { it <= ' ' }

        try {
            return value.toInt()
        } catch (ignored: Exception) {
        }
        return defaultValue
    }

    /**
     * @return the value of the Java system property with the specified `property`, while falling back to the
     * specified default value if the property access fails.
     */
    fun getLong(property: String, defaultValue: Long): Long {
        var value = getProperty(property) ?: return defaultValue
        value = value.trim { it <= ' ' }

        try {
            return value.toLong()
        } catch (ignored: Exception) {
        }
        return defaultValue
    }

    /**
     * @return the value of the Java system property with the specified `property`, while falling back to the
     * specified default value if the property access fails.
     */
    fun getFloat(property: String, defaultValue: Float): Float {
        var value = getProperty(property) ?: return defaultValue
        value = value.trim { it <= ' ' }

        try {
            return value.toFloat()
        } catch (ignored: Exception) {
        }
        return defaultValue
    }

    /**
     * @return the value of the Java system property with the specified `property`, while falling back to the
     * specified default value if the property access fails.
     */
    fun getDouble(property: String, defaultValue: Double): Double {
        var value = getProperty(property) ?: return defaultValue
        value = value.trim { it <= ' ' }

        try {
            return value.toDouble()
        } catch (ignored: Exception) {
        }
        return defaultValue
    }

    fun getColor(property: String, defaultValue: Color): Color {
        var value = getProperty(property) ?: return defaultValue
        value = value.trim { it <= ' ' }

        try {
            return Color.decode(value)
        } catch (ignored: Exception) {
        }
        return defaultValue
    }



    val is32bit = type.is32bit
    val is64bit = type.is64bit

    /**
     * @return true if this is a x86/x64/arm architecture (intel/amd/etc) processor.
     */
    val isX86 = type.isX86
    val isMips = type.isMips
    val isArm = type.isArm


    val isLinux = type.isLinux
    val isUnix = type.isUnix
    val isSolaris = type.isSolaris
    val isWindows = type.isWindows
    val isMacOsX = type.isMacOsX
    val isAndroid = type.isAndroid

    /**
     * Set our system to UTC time zone. Retrieve the **original** time zone via [.getOriginalTimeZone]
     */
    fun setUTC() {
        // have to set our default timezone to UTC. EVERYTHING will be UTC, and if we want local, we must explicitly ask for it.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    /**
     * @return the first line of the exception message from 'throwable', or the type if there was no message.
     */
    fun getExceptionMessage(throwable: Throwable): String? {
        var message = throwable.message
        if (message != null) {
            val index = message.indexOf(LINE_SEPARATOR)
            if (index > -1) {
                message = message.substring(0, index)
            }
        } else {
            message = throwable.javaClass.simpleName
        }
        return message
    }
}
