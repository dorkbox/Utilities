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

public enum OsType {
    Windows32("windows_32", ".dll"),
    Windows64("windows_64", ".dll"),
    Linux32("linux_32", ".so"),
    Linux64("linux_64", ".so"),
    MacOsX32("macosx_32", ".jnilib", ".dylib"),
    MacOsX64("macosx_64", ".jnilib", ".dylib"),

    UnixArm("unix_arm", ".so"),
    Unix32("unix_32", ".so"),
    Unix64("unix_64", ".so"),

    Solaris("solaris", ".so"),

    AndroidArm56("android_arm56", ".so"), // 32bit no hardware float support
    AndroidArm7("android_arm7", ".so"),   // 32bit hardware float support
    AndroidArm8("android_arm8", ".so"),   // 64bit (w/ hardware float. everything now has hard float)

    AndroidMips("android_mips", ".so"), // 32bit mips
    AndroidX86("android_x86", ".so"),   // 32bit x86 (usually emulator)

    AndroidMips64("android_mips64", ".so"), // 64bit mips
    AndroidX86_64("android_x86_64", ".so"),   // 64bit x86 (usually emulator)

    /*
     * Linux OS, Hard float, meaning floats are handled in hardware. WE ONLY SUPPORT HARD FLOATS for linux ARM!.
     * For Raspberry-PI, Beaglebone, Odroid, etc PCs
     */
    LinuxArm32("linux_arm7_hf", ".so"),
    LinuxArm64("linux_arm8_hf", ".so"),
    ;

    private final String name;
    private final String[] libraryNames;

    OsType(String name, String... libraryNames) {
        this.name = name;
        this.libraryNames = libraryNames;
    }

    public String getName() {
        return this.name;
    }
    public String[] getLibraryNames() {
        return this.libraryNames;
    }


    public
    boolean is64bit() {
        return this == OsType.Linux64 || this == OsType.LinuxArm64 ||
               this == OsType.Windows64 || this == OsType.MacOsX64 ||
               this == OsType.AndroidArm8 || this == OsType.AndroidX86_64 || this == OsType.AndroidMips64 ||
               this == OsType.Unix64;
    }

    public
    boolean is32bit() {
        return this == OsType.Linux32 || this == OsType.LinuxArm32 ||
               this == OsType.Windows32 || this == OsType.MacOsX32 ||
               this == OsType.AndroidArm56 || this == OsType.AndroidArm7 || this == OsType.AndroidX86 || this == OsType.AndroidMips ||
               this == OsType.UnixArm || this == OsType.Unix32;
    }

    public
    boolean isMips() {
        return this == OsType.AndroidMips || this == OsType.AndroidMips64;
    }

    /**
     * @return true if this is a "standard" x86/x64 architecture (intel/amd/etc) processor.
     */
    public
    boolean isX86() {
        return this == OsType.Linux64 || this == OsType.LinuxArm64 ||
               this == OsType.Windows64 || this == OsType.MacOsX64 ||
               this == OsType.Linux32 || this == OsType.LinuxArm32 ||
               this == OsType.Windows32 || this == OsType.MacOsX32 ||
               this == OsType.Unix32 || this == OsType.Unix64 ||
               this == OsType.AndroidX86 || this == OsType.AndroidX86_64;
    }

    public
    boolean isArm() {
        return this == OsType.LinuxArm32 || this == OsType.LinuxArm64 ||
               this == OsType.AndroidArm56 || this == OsType.AndroidArm7 || this == OsType.AndroidArm8;
    }

    public
    boolean isLinux() {
        return this == OsType.Linux32 || this == OsType.Linux64 || this == OsType.LinuxArm64 || this == OsType.LinuxArm32;
    }

    public
    boolean isUnix() {
        return this == OsType.Unix32 || this == OsType.Unix64 || this == OsType.UnixArm;
    }

    public
    boolean isSolaris() {
        return this == OsType.Solaris;
    }

    public
    boolean isWindows() {
        return this == OsType.Windows64 || this == OsType.Windows32;
    }

    public
    boolean isMacOsX() {
        return this == OsType.MacOsX64 || this == OsType.MacOsX32;
    }

    public
    boolean isAndroid() {
        return this == OsType.AndroidArm56 || this == OsType.AndroidArm7 || this == OsType.AndroidX86 || this == OsType.AndroidMips ||
               this == OsType.AndroidArm8 || this == OsType.AndroidX86_64 || this == OsType.AndroidMips64;
    }
}
