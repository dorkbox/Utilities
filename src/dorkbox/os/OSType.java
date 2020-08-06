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
package dorkbox.os;

public enum OSType {
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

    OSType(String name, String... libraryNames) {
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
        return this == OSType.Linux64 || this == OSType.LinuxArm64 ||
               this == OSType.Windows64 || this == OSType.MacOsX64 ||
               this == OSType.AndroidArm8 || this == OSType.AndroidX86_64 || this == OSType.AndroidMips64 ||
               this == OSType.Unix64;
    }

    public
    boolean is32bit() {
        return this == OSType.Linux32 || this == OSType.LinuxArm32 ||
               this == OSType.Windows32 || this == OSType.MacOsX32 ||
               this == OSType.AndroidArm56 || this == OSType.AndroidArm7 || this == OSType.AndroidX86 || this == OSType.AndroidMips ||
               this == OSType.UnixArm || this == OSType.Unix32;
    }

    public
    boolean isMips() {
        return this == OSType.AndroidMips || this == OSType.AndroidMips64;
    }

    /**
     * @return true if this is a "standard" x86/x64 architecture (intel/amd/etc) processor.
     */
    public
    boolean isX86() {
        return this == OSType.Linux64 || this == OSType.LinuxArm64 ||
               this == OSType.Windows64 || this == OSType.MacOsX64 ||
               this == OSType.Linux32 || this == OSType.LinuxArm32 ||
               this == OSType.Windows32 || this == OSType.MacOsX32 ||
               this == OSType.Unix32 || this == OSType.Unix64 ||
               this == OSType.AndroidX86 || this == OSType.AndroidX86_64;
    }

    public
    boolean isArm() {
        return this == OSType.LinuxArm32 || this == OSType.LinuxArm64 ||
               this == OSType.AndroidArm56 || this == OSType.AndroidArm7 || this == OSType.AndroidArm8;
    }

    public
    boolean isLinux() {
        return this == OSType.Linux32 || this == OSType.Linux64 || this == OSType.LinuxArm64 || this == OSType.LinuxArm32;
    }

    public
    boolean isUnix() {
        return this == OSType.Unix32 || this == OSType.Unix64 || this == OSType.UnixArm;
    }

    public
    boolean isSolaris() {
        return this == OSType.Solaris;
    }

    public
    boolean isWindows() {
        return this == OSType.Windows64 || this == OSType.Windows32;
    }

    public
    boolean isMacOsX() {
        return this == OSType.MacOsX64 || this == OSType.MacOsX32;
    }

    public
    boolean isAndroid() {
        return this == OSType.AndroidArm56 || this == OSType.AndroidArm7 || this == OSType.AndroidX86 || this == OSType.AndroidMips ||
               this == OSType.AndroidArm8 || this == OSType.AndroidX86_64 || this == OSType.AndroidMips64;
    }
}
