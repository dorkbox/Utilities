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
package dorkbox.os

enum class OSType(name: String, vararg libraryNames: String) {
    Windows32("windows_32", ".dll"),
    Windows64("windows_64", ".dll"),
    Linux32("linux_32", ".so"),
    Linux64("linux_64", ".so"),

    MacOsX32("macosx_32", ".jnilib", ".dylib"),
    MacOsX64("macosx_64", ".jnilib", ".dylib"),
    UnixArm("unix_arm", ".so"),
    Unix32("unix_32",".so"),

    Unix64("unix_64", ".so"),
    Solaris("solaris", ".so"),
    AndroidArm56("android_arm56", ".so"),  // 32bit no hardware float support
    AndroidArm7("android_arm7", ".so"),  // 32bit hardware float support
    AndroidArm8("android_arm8", ".so"),  // 64bit (w/ hardware float. everything now has hard float)
    AndroidMips("android_mips", ".so"),  // 32bit mips
    AndroidX86("android_x86", ".so"),  // 32bit x86 (usually emulator)
    AndroidMips64("android_mips64", ".so"),  // 64bit mips
    AndroidX86_64("android_x86_64", ".so"),  // 64bit x86 (usually emulator)

    /*
     * Linux OS, Hard float, meaning floats are handled in hardware. WE ONLY SUPPORT HARD FLOATS for linux ARM!.
     * For Raspberry-PI, Beaglebone, Odroid, etc PCs
     */
    LinuxArm32("linux_arm7_hf", ".so"),
    LinuxArm64("linux_arm8_hf", ".so");


    val libraryNames: Array<out String>

    init {
        this.libraryNames = libraryNames
    }

    val is64bit: Boolean
    get() {
        return this == Linux64 || this == LinuxArm64 || this == Windows64 || this == MacOsX64 || this == AndroidArm8 || this == AndroidX86_64 || this == AndroidMips64 || this == Unix64
    }

    val is32bit: Boolean
    get() {
        return this == Linux32 || this == LinuxArm32 || this == Windows32 || this == MacOsX32 || this == AndroidArm56 || this == AndroidArm7 || this == AndroidX86 || this == AndroidMips || this == UnixArm || this == Unix32
    }

    val isMips: Boolean
        get() = this == AndroidMips || this == AndroidMips64

    /**
     * @return true if this is a "standard" x86/x64 architecture (intel/amd/etc) processor.
     */
    val isX86: Boolean
        get() = this == Linux64 || this == LinuxArm64 || this == Windows64 || this == MacOsX64 || this == Linux32 || this == LinuxArm32 || this == Windows32 || this == MacOsX32 || this == Unix32 || this == Unix64 || this == AndroidX86 || this == AndroidX86_64
    val isArm: Boolean
        get() = this == LinuxArm32 || this == LinuxArm64 || this == AndroidArm56 || this == AndroidArm7 || this == AndroidArm8
    val isLinux: Boolean
        get() = this == Linux32 || this == Linux64 || this == LinuxArm64 || this == LinuxArm32
    val isUnix: Boolean
        get() = this == Unix32 || this == Unix64 || this == UnixArm
    val isSolaris: Boolean
        get() = this == Solaris
    val isWindows: Boolean
        get() = this == Windows64 || this == Windows32
    val isMacOsX: Boolean
        get() = this == MacOsX64 || this == MacOsX32
    val isAndroid: Boolean
        get() = this == AndroidArm56 || this == AndroidArm7 || this == AndroidX86 || this == AndroidMips || this == AndroidArm8 || this == AndroidX86_64 || this == AndroidMips64
}
