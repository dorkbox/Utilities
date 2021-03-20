/*
 * Copyright 2015 dorkbox, llc
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import dorkbox.executor.Executor;

/**
 * Container for all OS specific tests and methods. These do not exist in OS.java, because of dependency issues (OS.java should not
 * depend on any native libraries)
 */
@SuppressWarnings("unused")
public
class OSUtil {
    public static
    class Windows {
        /**
         * Version info at release.
         *
         * https://en.wikipedia.org/wiki/Comparison_of_Microsoft_Windows_versions
         *
         * Windows XP               5.1.2600  (2001-10-25)
         * Windows Server 2003      5.2.3790  (2003-04-24)
         *
         * Windows Home Server		5.2.3790  (2007-06-16)
         *
         * -------------------------------------------------
         *
         * Windows Vista	        6.0.6000  (2006-11-08)
         * Windows Server 2008 SP1	6.0.6001  (2008-02-27)
         * Windows Server 2008 SP2	6.0.6002  (2009-04-28)
         *
         * -------------------------------------------------
         *
         * Windows 7                    6.1.7600  (2009-10-22)
         * Windows Server 2008 R2       6.1.7600  (2009-10-22)
         * Windows Server 2008 R2 SP1   6.1.7601  (?)
         *
         * Windows Home Server 2011		6.1.8400  (2011-04-05)
         *
         * -------------------------------------------------
         *
         * Windows 8                    6.2.9200  (2012-10-26)
         * Windows Server 2012	        6.2.9200  (2012-09-04)
         *
         * -------------------------------------------------
         *
         * Windows 8.1                  6.3.9600  (2013-10-18)
         * Windows Server 2012 R2       6.3.9600  (2013-10-18)
         *
         * -------------------------------------------------
         *
         * Windows 10	                10.0.10240  (2015-07-29)
         * Windows 10	                10.0.10586  (2015-11-12)
         * Windows 10	                10.0.14393  (2016-07-18)
         *
         * Windows Server 2016          10.0.14393  (2016-10-12)
         *
         * @return the [major][minor] version of windows, ie: Windows Version 10.0.10586 -> [10][0]
         */
        public static
        int[] getVersion() {
            int[] version = new int[2];

            if (!OS.isWindows()) {
                return version;
            }

            try {
                String output = System.getProperty("os.version");
                String[] split = output.split("\\.",-1);

                if (split.length <= 2) {
                    for (int i = 0; i < split.length; i++) {
                        version[i] = Integer.parseInt(split[i]);
                    }
                }
            } catch (Throwable ignored) {
            }

            return version;
        }

        /**
         * @return is windows XP or equivalent
         */
        public static
        boolean isWindowsXP() {
            return getVersion()[0] == 5;
        }

        /**
         * @return is windows Vista or equivalent
         */
        public static
        boolean isWindowsVista() {
            int[] version = getVersion();
            return version[0] == 6 && version[1] == 0;
        }

        /**
         * @return is windows 7 or equivalent
         */
        public static
        boolean isWindows7() {
            int[] version = getVersion();
            return version[0] == 6 && version[1] == 1;
        }

        /**
         * @return is windows 8 or equivalent
         */
        public static
        boolean isWindows8() {
            int[] version = getVersion();
            return version[0] == 6 && version[1] == 2;
        }

        /**
         * @return is windows 8.1 or equivalent
         */
        public static
        boolean isWindows8_1() {
            int[] version = getVersion();
            return version[0] == 6 && version[1] == 3;
        }

        /**
         * @return is greater than or equal to windows 8.1 or equivalent
         */
        public static
        boolean isWindows8_1_plus() {
            int[] version = getVersion();
            if (version[0] == 6 && version[1] >= 3) {
                return true;
            }
            else {
                return version[0] > 6;
            }
        }

        /**
         * @return is windows 10 or equivalent
         */
        public static
        boolean isWindows10() {
            return getVersion()[0] == 10;
        }

        /**
         * @return is windows 10 or greater
         */
        public static
        boolean isWindows10_plus() {
            return getVersion()[0] >= 10;
        }
    }

    public static
    class Unix {
        public static
        boolean isFreeBSD() {
            if (!OS.isUnix()) {
                return false;
            }

            try {
                // uname
                return Executor.Companion.run("uname").startsWith("FreeBSD");
            } catch (Throwable ignored) {
            }

            return false;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static
    class Linux {
        private static String info = null;

        /**
         * @return os release info or ""
         */
        public static
        String getInfo() {
            if (info != null) {
                return info;
            }

            if (!OS.isLinux()) {
                info = "";
                return info;
            }

            try {
                List<File> releaseFiles = new LinkedList<>();
                int totalLength = 0;

                // looking for files like /etc/os-release
                File file = new File("/etc");
                if (file.isDirectory()) {
                    File[] list = file.listFiles();
                    if (list != null) {
                        for (File f : list) {
                            if (f.isFile() && f.getName().contains("release")) {
                                // this is likely a file we are interested in.
                                releaseFiles.add(f);
                                totalLength += (int) file.length();
                            }
                        }
                    }
                }

                if (totalLength > 0) {
                    StringBuilder fileContents = new StringBuilder(totalLength);

                    for (File releaseFile : releaseFiles) {
                        try (BufferedReader reader = new BufferedReader(new FileReader(releaseFile))) {
                            String currentLine;

                            // NAME="Arch Linux"
                            // PRETTY_NAME="Arch Linux"
                            // ID=arch
                            // ID_LIKE=archlinux
                            // ANSI_COLOR="0;36"
                            // HOME_URL="https://www.archlinux.org/"
                            // SUPPORT_URL="https://bbs.archlinux.org/"
                            // BUG_REPORT_URL="https://bugs.archlinux.org/"

                            // similar on other distro's.  ID is always the "key" to the distro

                            while ((currentLine = reader.readLine()) != null) {
                                fileContents.append(currentLine)
                                            .append(OS.LINE_SEPARATOR_UNIX);
                            }
                        }
                    }

                    info = fileContents.toString();
                    return info;
                }
            } catch (Throwable ignored) {
            }

            info = "";
            return info;
        }

        /**
         * @param id the info ID to check, ie: ubuntu, arch, debian, etc... This is what the OS vendor uses to ID their OS.
         * @return true if this OS is identified as the specified ID.
         */
        public static
        boolean getInfo(String id) {
            // also matches on 'DISTRIB_ID' and 'VERSION_ID'
            // ID=linuxmint/fedora/arch/ubuntu/etc
            return getInfo().contains("ID=" + id + "\n");
        }

        private static volatile Boolean isArch = null;
        public static
        boolean isArch() {
            if (isArch == null) {
                isArch = getInfo("arch");
            }
            return isArch;
        }

        private static volatile Boolean isDebian = null;
        public static
        boolean isDebian() {
            if (isDebian == null) {
                isDebian = getInfo("debian");
            }
            return isDebian;
        }

        private static volatile Boolean isElementaryOS = null;
        public static
        boolean isElementaryOS() {
            if (isElementaryOS == null) {
                try {
                    String output = getInfo();
                    // ID="elementary"  (notice the extra quotes)
                    isElementaryOS = output.contains("ID=\"elementary\"\n") || output.contains("ID=elementary\n") ||

                                     // this is specific to eOS < 0.3.2
                                     output.contains("ID=\"elementary OS\"\n");
                } catch (Throwable ignored) {
                    isElementaryOS = false;
                }
            }
            return isElementaryOS;
        }

        private static volatile Boolean isFedora = null;
        public static
        boolean isFedora() {
            if (isFedora == null) {
                isFedora = getInfo("fedora");
            }
            return isFedora;
        }

        private static volatile Integer fedoraVersion = null;
        public static
        int getFedoraVersion() {
            if (fedoraVersion != null) {
                return fedoraVersion;
            }

            if (!isFedora()) {
                fedoraVersion = 0;
                return fedoraVersion;
            }

            try {
                String output = getInfo();

                // ID=fedora
                if (output.contains("ID=fedora\n")) {
                    // should be: VERSION_ID=23\n  or something
                    int beginIndex = output.indexOf("VERSION_ID=") + 11;
                    String fedoraVersion_ = output.substring(beginIndex, output.indexOf(OS.LINE_SEPARATOR_UNIX, beginIndex));

                    fedoraVersion = Integer.parseInt(fedoraVersion_);
                    return fedoraVersion;
                }
            } catch (Throwable ignored) {
            }

            fedoraVersion = 0;
            return fedoraVersion;
        }

        private static volatile Boolean isLinuxMint = null;
        public static
        boolean isLinuxMint() {
            if (isLinuxMint == null) {
                isLinuxMint = getInfo("linuxmint");
            }
            return isLinuxMint;
        }

        private static volatile Boolean isUbuntu = null;
        public static
        boolean isUbuntu() {
            if (isUbuntu == null) {
                isUbuntu = getInfo("ubuntu");
            }
            return isUbuntu;
        }

        private static volatile int[] ubuntuVersion = null;
        public static
        int[] getUbuntuVersion() {
            if (ubuntuVersion != null) {
                return ubuntuVersion;
            }

            if (!isUbuntu()) {
                ubuntuVersion = new int[]{0,0};
                return ubuntuVersion;
            }

            String distribReleaseInfo = getDistribReleaseInfo();
            if (distribReleaseInfo != null) {
                String[] split = distribReleaseInfo.split("\\.");

                ubuntuVersion = new int[] {Integer.parseInt(split[0]), Integer.parseInt(split[1])};
                return ubuntuVersion;
            }


            ubuntuVersion = new int[]{0,0};
            return ubuntuVersion;
        }

        private static volatile int[] elementaryOSVersion = null;
        public static
        int[] getElementaryOSVersion() {
            // 0.1 Jupiter. The first stable version of elementary OS was Jupiter, published on 31 March 2011 and based on Ubuntu 10.10. ...
            // 0.2 Luna. elementary OS 0.2 "Luna" ...
            // 0.3 Freya. elementary OS 0.3 "Freya" ...
            // 0.4 Loki. elementary OS 0.4, known by its codename, "Loki", was released on 9 September 2016. ...
            // 5.0 Juno

            if (elementaryOSVersion != null) {
                return elementaryOSVersion;
            }

            if (!isElementaryOS()) {
                elementaryOSVersion = new int[]{0,0};
                return elementaryOSVersion;
            }

            String distribReleaseInfo = getDistribReleaseInfo();
            if (distribReleaseInfo != null) {
                String[] split = distribReleaseInfo.split("\\.");

                elementaryOSVersion = new int[] {Integer.parseInt(split[0]), Integer.parseInt(split[1])};
                return elementaryOSVersion;
            }

            elementaryOSVersion = new int[]{0,0};
            return elementaryOSVersion;
        }


        private static Boolean isKali = null;
        public static
        boolean isKali() {
            if (isKali == null) {
                isKali = getInfo("kali");
            }
            return isKali;
        }

        private static Boolean isIgel = null;
        public static
        boolean isIgel() {
            if (isKali == null) {
                isKali = getInfo("IGEL");
            }
            return isKali;
        }

        /**
         * @return the `DISTRIB_RELEASE` info as a String, if possible. Otherwise NULL
         */
        public static
        String getDistribReleaseInfo() {
            String info = getInfo();
            String releaseString = "DISTRIB_RELEASE=";
            int index = info.indexOf(releaseString);
            try {
                if (index > -1) {
                    index += releaseString.length();
                    int newLine = info.indexOf(OS.LINE_SEPARATOR_UNIX, index);
                    if (newLine > index) {
                        return info.substring(index, newLine);
                    }
                }
            } catch (Throwable ignored) {
            }

            return null;
        }

        private static volatile Boolean isWSL = null;
        public static
        boolean isWSL() {
            if (isWSL == null) {
                try {
                    // looking for /proc/version
                    File file = new File("/proc/version");
                    if (file.canRead()) {
                        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                            // Linux version 4.4.0-19041-Microsoft (Microsoft@Microsoft.com) (gcc version 5.4.0 (GCC) ) #488-Microsoft Mon Sep 01 13:43:00 PST 2020

                            String currentLine = reader.readLine();
                            isWSL = currentLine.contains("-Microsoft");
                        } catch (Throwable ignored) {
                        }
                    }

                    if (isWSL == null) {
                        // reading the file didn't work for whatever reason...
                        // uname -v
                        isWSL = Executor.Companion.run("uname", "-v").contains("-Microsoft");
                    }
                } catch (Throwable ignored) {
                    isWSL = false;
                }
            }
            return isWSL;
        }

        public static
        boolean isRoot() {
            // this means we are running as sudo
            boolean isSudoOrRoot = System.getenv("SUDO_USER") != null;

            if (!isSudoOrRoot) {
                // running as root (also can be "sudo" user). A lot slower that checking a sys env, but this is guaranteed to work
                try {
                    // id -u
                    isSudoOrRoot = "0".equals(Executor.Companion.run("id", "-u"));
                } catch (Throwable ignored) {
                }
            }

            return isSudoOrRoot;
        }





        public static
        class PackageManager {
            public enum Type {
                APT("apt install"),
                APTGET("apt-get install"),
                YUM("yum install"),
                PACMAN("pacman -S "),
                ;

                private final String installString;

                Type(final String installString) {
                    this.installString = installString;
                }

                public
                String installString() {
                    return installString;
                }
            }

            public static
            Type get() {
                if (new File("/usr/bin/apt").canExecute()) {
                    return Type.APT;
                }

                if (new File("/usr/bin/apt-get").canExecute()) {
                    return Type.APTGET;
                }

                if (new File("/usr/bin/yum").canExecute()) {
                    return Type.YUM;
                }

                if (new File("/usr/bin/pacman").canExecute()) {
                    return Type.PACMAN;
                }

                // default is apt-get, even if it isn't correct
                return Type.APTGET;
            }


            /**
             * @return true if the package is installed
             */
            public static
            boolean isPackageInstalled(final String packageName) {
                // dpkg
                // dpkg -L libappindicator3
                // dpkg-query: package 'libappindicator3' is not installed
                boolean is_dpkg = new File("/usr/bin/dpkg").canExecute();
                if (is_dpkg) {
                    return !Executor.Companion.run("dpkg", "-L", packageName).contains("is not installed");
                }

                // rpm
                // rpm -q libappindicator234
                // package libappindicator234 is not installed
                boolean is_rpm = new File("/usr/bin/rpm").canExecute();
                if (is_rpm) {
                    return !Executor.Companion.run("rpm", "-q", packageName).contains("is not installed");
                }


                // pacman
                // pacman -Qi <packageName>
                // use the exit code to determine if the packages exists on the system or not (0 the package exists, 1 it doesn't)
                boolean is_pacmac = new File("/usr/bin/pacman").canExecute();
                try {
                    int start = new Executor().command("pacman", "-Qi", packageName)
                                                     .startBlocking()
                                                     .getExitValue();

                    // 0 the package exists, 1 it doesn't
                    return start == 0;
                } catch (Exception ignored) {
                }

                return false;
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static
    class DesktopEnv {
        public enum Env {
            Gnome,
            KDE,
            Unity,
            Unity7,
            XFCE,
            LXDE,
            MATE,
            Pantheon,
            ChromeOS,
            Unknown,
        }

        public enum EnvType {
            X11,
            WAYLAND,
            Unknown,
        }

        public static
        Env get() {
            // if we are running as ROOT, we *** WILL NOT *** have access to  'XDG_CURRENT_DESKTOP'
            //   *unless env's are preserved, but they are not guaranteed to be
            // see:  http://askubuntu.com/questions/72549/how-to-determine-which-window-manager-is-running
            String XDG = System.getenv("XDG_CURRENT_DESKTOP");
            if (XDG == null) {
                // maybe we are running as root???
                XDG = "unknown"; // try to autodetect if we should use app indicator or gtkstatusicon
            }

            // Ubuntu 17.10+ is special ... this is ubuntu:GNOME (it now uses wayland instead of x11, so many things have changed...)
            // So it's gnome, and gnome-shell, but with some caveats
            // see: https://bugs.launchpad.net/ubuntu/+source/gnome-shell/+bug/1700465

            // BLEH. if gnome-shell is running, IT'S REALLY GNOME!
            // we must ALWAYS do this check!!
            if (OSUtil.DesktopEnv.isGnome()) {
                XDG = "gnome";
            }
            else if (OSUtil.DesktopEnv.isKDE()) {
                // same thing with plasmashell!
                XDG = "kde";
            }

            // Ubuntu Unity is a weird combination. It's "Gnome", but it's not "Gnome Shell".
            if ("unity".equalsIgnoreCase(XDG)) {
                return Env.Unity;
            }
            // Ubuntu Unity7 is a weird combination. It's "Gnome", but it's not "Gnome Shell".
            if ("unity:unity7".equalsIgnoreCase(XDG)) {
                return Env.Unity7;
            }
            if ("xfce".equalsIgnoreCase(XDG)) {
                return Env.XFCE;
            }
            if ("lxde".equalsIgnoreCase(XDG)) {
                return Env.LXDE;
            }
            if ("kde".equalsIgnoreCase(XDG)) {
                return Env.KDE;
            }
            if ("pantheon".equalsIgnoreCase(XDG)) {
                return Env.Pantheon;
            }
            if ("gnome".equalsIgnoreCase(XDG)) {
                return Env.Gnome;
            }

            // maybe it's chromeOS?
            if (isChromeOS()) {
                return Env.ChromeOS;
            }

            if (isMATE()) {
                return Env.MATE;
            }

            return Env.Unknown;
        }

        private static
        boolean isValidCommand(final String partialExpectationInOutput, final String commandOutput) {
            return commandOutput.contains(partialExpectationInOutput)
                   && !commandOutput.contains("not installed")
                   && !commandOutput.contains ("No such file or directory");
        }


        public static EnvType getType() {
            String XDG = System.getenv("XDG_SESSION_TYPE");
            if (XDG == null) {
                XDG = "unknown"; // have no idea how this can happen....
            }

            if ("x11".equals(XDG)) {
                return EnvType.X11;
            }

            if ("wayland".equals(XDG)) {
                return EnvType.WAYLAND;
            }

            return EnvType.Unknown;
        }

        public static
        boolean isX11() {
            EnvType env = getType();
            return env == EnvType.X11;
        }

        public static
        boolean isWayland() {
            EnvType env = getType();
            return env == EnvType.WAYLAND;
        }

        public static
        boolean isUnity() {
            Env env = get();
            return isUnity(env);
        }

        public static
        boolean isUnity(final Env env) {
            return env == OSUtil.DesktopEnv.Env.Unity || env == OSUtil.DesktopEnv.Env.Unity7;
        }


        private static volatile Boolean isMATE = null;
        public static
        boolean isMATE() {
            if (!OS.isLinux() && !OS.isUnix()) {
                return false;
            }

            if (isMATE != null) {
                return isMATE;
            }

            try {
                isMATE = new File("/usr/bin/mate-about").exists();
                return isMATE;
            } catch (Throwable ignored) {
            }

            isMATE = false;
            return isMATE;
        }

        private static volatile Boolean isGnome = null;
        public static
        boolean isGnome() {
            if (!OS.isLinux() && !OS.isUnix()) {
                return false;
            }

            if (isGnome != null) {
                return isGnome;
            }

            try {
                // note: some versions of linux can ONLY access "ps a"; FreeBSD and most linux is "ps x"
                // we try "x" first

                // ps x | grep gnome-shell
                boolean contains = Executor.Companion.run("ps", "x").contains("gnome-shell");

                if (!contains && OS.isLinux()) {
                    // only try again if we are linux

                    // ps a | grep gnome-shell
                    contains = Executor.Companion.run("ps", "a").contains("gnome-shell");
                }

                isGnome = contains;
                return isGnome;
            } catch (Throwable ignored) {
            }

            isGnome = false;
            return isGnome;
        }

        private static volatile boolean hasGnomeVersion = false;
        private static volatile String gnomeVersion = null;
        /**
         * @return a string representing the current gnome-shell version, or NULL if it could not be found
         */
        public static
        String getGnomeVersion() {
            if (hasGnomeVersion) {
                return gnomeVersion;
            }

            if (!OS.isLinux() && !OS.isUnix()) {
                return null;
            }

            hasGnomeVersion = true;

            try {
                // gnome-shell --version
                String versionString = Executor.Companion.run("gnome-shell", "--version");

                if (!versionString.isEmpty()) {
                    // GNOME Shell 3.14.1
                    String version = versionString.replaceAll("[^\\d.]", "");
                    if (version.length() > 0 && version.indexOf('.') > 0) {
                        // should just be 3.14.1 or 3.20 or similar
                        gnomeVersion = version;
                        return gnomeVersion;
                    }
                }
            } catch (Throwable ignored) {
            }

            gnomeVersion = null;
            return gnomeVersion;
        }

        private static volatile Boolean isKDE = null;
        public static
        boolean isKDE() {
            if (isKDE != null) {
                return isKDE;
            }

            String XDG = System.getenv("XDG_CURRENT_DESKTOP");
            if (XDG == null) {
                // Check if plasmashell is running, if it is -- then we are most likely KDE
                double plasmaVersion = OSUtil.DesktopEnv.getPlasmaVersion();

                isKDE = plasmaVersion > 0;
                return isKDE;
            } else if ("kde".equalsIgnoreCase(XDG)) {
                isKDE = true;
                return false;
            }

            isKDE = false;
            return false;
        }

        /**
         * The first two decimal places of the version number of plasma shell (if running) as a double.
         *
         * @return cannot represent '5.6.5' as a number, so we return just the first two decimal places instead
         */
        public static
        double getPlasmaVersion() {
            String versionAsString = getPlasmaVersionFull();

            if (versionAsString == null) {
                return 0;
            }

            if (versionAsString.startsWith("0")) {
                return 0;
            }

            // this isn't the BEST way to do this, but it's simple and easy to understand
            String[] split = versionAsString.split("\\.",3);
            if (split.length > 2) {
                return Double.parseDouble(split[0] + "." + split[1]);
            } else {
                return Double.parseDouble(split[0]);
            }
        }


        private static volatile boolean hasPlasmaVersion = false;
        private static volatile String getPlasmaVersionFull = null;
        /**
         * The full version number of plasma shell (if running) as a String.
         *
         * @return cannot represent '5.6.5' as a number, so we return a String instead or NULL if unknown
         */
        public static
        String getPlasmaVersionFull() {
            if (hasPlasmaVersion) {
                return getPlasmaVersionFull;
            }

            if (!OS.isLinux() && !OS.isUnix()) {
                return null;
            }

            hasPlasmaVersion = true;

            try {
                // plasma-desktop -v
                // plasmashell --version
                String output = Executor.Companion.run("plasmashell", "--version");

                if (!output.isEmpty()) {
                    // DEFAULT icon size is 16. KDE is bananas on what they did with tray icon scale
                    // should be: plasmashell 5.6.5   or something
                    String s = "plasmashell ";
                    if (isValidCommand(s, output)) {
                        getPlasmaVersionFull = output.substring(output.indexOf(s) + s.length());
                        return getPlasmaVersionFull;
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

            return null;
        }


        private static volatile Boolean isNautilus = null;
        /**
         * There are sometimes problems with nautilus (the file browser) and some GTK methods. It is ridiculous for me to have to
         * work around their bugs like this.
         * <p>
         * see: https://askubuntu.com/questions/788182/nautilus-not-opening-up-showing-glib-error
         */
        public static
        boolean isNautilus() {
            if (isNautilus != null) {
                return isNautilus;
            }

            if (!OS.isLinux() && !OS.isUnix()) {
                isNautilus = false;
                return false;
            }

            try {
                // nautilus --version
                String output = Executor.Companion.run("nautilus", "--version");

                if (!output.isEmpty()) {
                    // should be: GNOME nautilus 3.14.3   or something
                    String s = "GNOME nautilus ";
                    if (isValidCommand(s, output)) {
                        isNautilus = true;
                        return true;
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

            isNautilus = false;
            return false;
        }


        private static volatile Boolean isChromeOS = null;
        public static
        boolean isChromeOS() {
            if (isChromeOS == null) {
                if (!OS.isLinux()) {
                    isChromeOS = false;
                    return false;
                }

                isChromeOS = false;
                try {
                    // ps aux | grep chromeos
                    String output = Executor.Companion.run("ps", "aux");

                    if (!output.isEmpty()) {
                        if (output.contains("chromeos")) {
                            isChromeOS = true;
                            return true;
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            return isChromeOS;
        }

        /**
         * @param channel which XFCE channel to query. Cannot be null
         * @param property which property (in the channel) to query. Null will list all properties in the channel
         *
         * @return the property value or "".
         */
        public static
        String queryXfce(String channel, String property) {
            if (!OS.isLinux() && !OS.isUnix()) {
                return "";
            }

            if (channel == null) {
                return "";
            }

            try {
                // xfconf-query -c xfce4-panel -l
                List<String> commands = new ArrayList<>();
                commands.add("xfconf-query");
                commands.add("-c " + channel);

                if (property != null) {
                    // get property for channel
                    commands.add("-p " + property);
                } else {
                    // list all properties for the channel
                    commands.add("-l");
                }

                return Executor.Companion.run(commands);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            return "";
        }
    }
}
