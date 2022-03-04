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
    @kotlin.jvm.JvmField
    val LINE_SEPARATOR = getProperty("line.separator", "\n")!!

    const val LINE_SEPARATOR_UNIX = "\n"
    const val LINE_SEPARATOR_WINDOWS = "\r\n"

    @kotlin.jvm.JvmField
    val TEMP_DIR = File(getProperty("java.io.tmpdir", "temp")!!).absoluteFile

    /**
     * The currently running MAJOR java version as a NUMBER. For example, "Java version 1.7u45", and converts it into 7, uses JEP 223 for java > 9
     */
    @kotlin.jvm.JvmField
    val javaVersion = _getJavaVersion()
    private var osType: OSType? = null

    /**
     * Returns the *ORIGINAL* system time zone, before (*IF*) it was changed to UTC
     */
    val originalTimeZone = TimeZone.getDefault().id

    init {
        if (!TEMP_DIR.isDirectory) {
            // create the temp dir if necessary because the TEMP dir doesn't exist.
            TEMP_DIR.mkdirs()
        }
        var osName = getProperty("os.name", "")!!
        var osArch = getProperty("os.arch", "")!!

        osName = osName.lowercase()
        osArch = osArch.lowercase()
        if (osName.startsWith("linux")) {
            // best way to determine if it's android.
            // Sometimes java binaries include Android classes on the classpath, even if it isn't actually Android, so we check the VM
            val value = getProperty("java.vm.name", "")
            val isAndroid = "Dalvik" == value
            if (isAndroid) {
                // android check from https://stackoverflow.com/questions/14859954/android-os-arch-output-for-arm-mips-x86
                osType = when (osArch) {
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
                        null // who knows?
                    }
                }
            } else {
                // http://mail.openjdk.java.net/pipermail/jigsaw-dev/2017-April/012107.html
                osType = when {
                    osArch == "i386" -> {
                        OSType.Linux32
                    }
                    osArch == "x86" -> {
                        OSType.Linux32
                    }
                    osArch == "arm" -> {
                        OSType.LinuxArm32
                    }

                    osArch == "amd64" -> {
                        OSType.Linux64
                    }
                    osArch == "x86_64" -> {
                        OSType.Linux64
                    }
                    osArch == "aarch64" -> {
                        OSType.LinuxArm64
                    }


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
                        OSType.Linux32
                    }
                }
            }
        } else if (osName.startsWith("windows")) {
            osType = if ("amd64" == osArch) {
                OSType.Windows64
            } else {
                OSType.Windows32
            }
        } else if (osName.startsWith("macos") || osName.startsWith("darwin")) {
            osType = if ("x86_64" == osArch) {
                OSType.MacOsX64
            } else {
                OSType.MacOsX32
            }
        } else if (osName.startsWith("freebsd") || osName.contains("nix") || osName.contains("nux") || osName.startsWith(
                "aix"
            )
        ) {
            osType = when (osArch) {
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
        } else if (osName.startsWith("solaris") || osName.startsWith("sunos")) {
            osType = OSType.Solaris
        } else {
            osType = null
        }

        /*
         * By default, the timer resolution on Windows ARE NOT high-resolution (16ms vs 1ms)
         *
         * 'Thread.sleep(1)' will not really sleep for 1ms, but will really sleep for ~16ms. This long-running sleep will trick Windows
         *  into using higher resolution timers.
         *
         * See: https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6435126
         */
        val osType_ = osType
        if (osType_ == null || osType_.isWindows) {
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
        return getProperty(property, null)
    }

    /**
     * @return the System Property in a safe way for a given property, and if null - returns the specified default value.
     */
    fun getProperty(property: String, defaultValue: String?): String? {
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

    /**
     * @return the OS Type that is running on this machine
     */
    fun get(): OSType? {
        return osType
    }

    fun is64bit(): Boolean {
        return osType!!.is64bit
    }

    fun is32bit(): Boolean {
        return osType!!.is32bit
    }

    /**
     * @return true if this is a "standard" x86/x64 architecture (intel/amd/etc) processor.
     */
    val isX86: Boolean
        get() = osType!!.isX86
    val isMips: Boolean
        get() = osType!!.isMips
    val isArm: Boolean
        get() = osType!!.isArm
    val isLinux: Boolean
        get() = osType!!.isLinux
    val isUnix: Boolean
        get() = osType!!.isUnix
    val isSolaris: Boolean
        get() = osType!!.isSolaris
    val isWindows: Boolean
        get() = osType!!.isWindows
    val isMacOsX: Boolean
        get() = osType!!.isMacOsX
    val isAndroid: Boolean
        get() = osType!!.isAndroid


    /**
     * Gets the currently running MAJOR java version as a NUMBER. For example, "Java version 1.7u45", and converts it into 7, uses JEP 223 for java > 9
     */
    private fun _getJavaVersion(): Int {
        // this should never be a problem, but just in case
        var fullJavaVersion = getProperty("java.version", "")
        if (fullJavaVersion!!.startsWith("1.")) {
            when (fullJavaVersion[2]) {
                '4' -> return 4
                '5' -> return 5
                '6' -> return 6
                '7' -> return 7
                '8' -> return 8
                '9' -> return 9
            }
        } else {
            // We are >= java 10, use JEP 223 to get the version (early releases of 9 might not have JEP 223, so 10 is guaranteed to have it)
            fullJavaVersion = getProperty("java.specification.version", "10")
            try {
                // it will ALWAYS be the major release version as an integer. See http://openjdk.java.net/jeps/223
                return fullJavaVersion!!.toInt()
            } catch (ignored: Exception) {
            }
        }

        // the last valid guess we have, since the current Java implementation, whatever it is, decided not to cooperate with JEP 223.
        return 10
    }

    /**
     * Set our system to UTC time zone. Retrieve the **original** time zone via [.getOriginalTimeZone]
     */
    fun setUTC() {
        // have to set our default timezone to UTC. EVERYTHING will be UTC, and if we want local, we must explicitly ask for it.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    /**
     * @return the optimum number of threads for a given task. Makes certain not to take ALL the threads, always returns at least one
     * thread.
     */
    val optimumNumberOfThreads: Int
        get() = (Runtime.getRuntime().availableProcessors() - 2).coerceAtLeast(1)

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
