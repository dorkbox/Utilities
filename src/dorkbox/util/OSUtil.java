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
package dorkbox.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import dorkbox.util.process.ShellProcessBuilder;

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
            version[0] = 0;
            version[1] = 0;

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
            else if (version[0] > 6) {
                return true;
            }


            return false;
        }

        /**
         * @return is windows 10 or equivalent
         */
        public static
        boolean isWindows10() {
            return getVersion()[0] == 10;
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
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8196);
                PrintStream outputStream = new PrintStream(byteArrayOutputStream);

                // uname
                final ShellProcessBuilder shell = new ShellProcessBuilder(outputStream);
                shell.setExecutable("uname");
                shell.start();

                String output = ShellProcessBuilder.getOutput(byteArrayOutputStream);
                return output.startsWith("FreeBSD");
            } catch (Throwable ignored) {
            }

            return false;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static
    class Linux {
        public static
        String getInfo() {
            if (!OS.isLinux()) {
                return "";
            }

            try {
                List<File> releaseFiles = new LinkedList<File>();
                int totalLength = 0;

                File file = new File("/etc");
                if (file.isDirectory()) {
                    File[] list = file.listFiles();
                    if (list != null) {
                        for (File f : list) {
                            if (f.getName()
                                 .contains("release")) {
                                // this is likely a file we are interested in.

                                releaseFiles.add(f);
                                totalLength += (int) file.length();
                            }
                        }
                    }
                }

                if (totalLength > 0) {
                    StringBuilder fileContents = new StringBuilder(totalLength);
                    BufferedReader reader = null;

                    for (File releaseFile : releaseFiles) {
                        try {
                            reader = new BufferedReader(new FileReader(releaseFile));

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
                        } finally {
                            if (reader != null) {
                                reader.close();
                            }
                        }
                    }

                    return fileContents.toString();
                }
            } catch (Throwable ignored) {
            }

            return "";
        }

        public static
        int getFedoraVersion() {
            try {
                String output = getInfo();

                // ID=fedora
                if (output.contains("ID=fedora\n")) {
                    // should be: VERSION_ID=23\n  or something
                    int beginIndex = output.indexOf("VERSION_ID=") + 11;
                    String fedoraVersion_ = output.substring(beginIndex, output.indexOf(OS.LINE_SEPARATOR_UNIX, beginIndex));
                    return Integer.parseInt(fedoraVersion_);
                }
            } catch (Throwable ignored) {
            }

            return 0;
        }

        /**
         * @param id the info ID to check, ie: ubuntu, arch, debian, etc... This is what the OS vendor uses to ID their OS.
         *
         * @return true if this OS is identified as the specified ID.
         */
        public static
        boolean getInfo(String id) {
            String output = getInfo();
            // ID=linuxmint/fedora/arch/ubuntu/etc
            return output.contains("ID=" + id +"\n");
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

        public static
        boolean isRoot() {
            // this means we are running as sudo
            boolean isSudoOrRoot = System.getenv("SUDO_USER") != null;

            if (!isSudoOrRoot) {
                // running as root (also can be "sudo" user). A lot slower that checking a sys env, but this is guaranteed to work
                try {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8196);
                    PrintStream outputStream = new PrintStream(byteArrayOutputStream);

                    // id -u
                    final ShellProcessBuilder shell = new ShellProcessBuilder(outputStream);
                    shell.setExecutable("id");
                    shell.addArgument("-u");
                    shell.start();

                    String output = ShellProcessBuilder.getOutput(byteArrayOutputStream);
                    isSudoOrRoot = "0".equals(output);
                } catch (Throwable ignored) {
                }
            }

            return isSudoOrRoot;
        }
    }

    public static
    class DesktopEnv {
        public enum Env {
            Gnome,
            KDE,
            Unity,
            XFCE,
            LXDE,
            Pantheon,
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
            else if ("xfce".equalsIgnoreCase(XDG)) {
                return Env.XFCE;
            }
            else if ("lxde".equalsIgnoreCase(XDG)) {
                return Env.LXDE;
            }
            else if ("kde".equalsIgnoreCase(XDG)) {
                return Env.KDE;
            }
            else if ("pantheon".equalsIgnoreCase(XDG)) {
                return Env.Pantheon;
            }
            else if ("gnome".equalsIgnoreCase(XDG)) {
                return Env.Gnome;
            }

            return Env.Unknown;
        }


        private static volatile Boolean isGnome = null;
        private static volatile Boolean isKDE = null;

        public static
        boolean isGnome() {
            if (!OS.isLinux() && !OS.isUnix()) {
                return false;
            }

            if (isGnome != null) {
                return isGnome;
            }

            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8196);
                PrintStream outputStream = new PrintStream(byteArrayOutputStream);

                // note: some versions of linux can ONLY access "ps a"; FreeBSD and most linux is "ps x"
                // we try "x" first

                // ps x | grep gnome-shell
                ShellProcessBuilder shell = new ShellProcessBuilder(outputStream);
                shell.setExecutable("ps");
                shell.addArgument("x");
                shell.start();

                String output = ShellProcessBuilder.getOutput(byteArrayOutputStream);
                boolean contains = output.contains("gnome-shell");

                if (!contains && OS.isLinux()) {
                    // only try again if we are linux

                    // ps a | grep gnome-shell
                    shell = new ShellProcessBuilder(outputStream);
                    shell.setExecutable("ps");
                    shell.addArgument("a");
                    shell.start();

                    output = ShellProcessBuilder.getOutput(byteArrayOutputStream);
                    contains = output.contains("gnome-shell");
                }

                isGnome = contains;
                return contains;
            } catch (Throwable ignored) {
            }

            isGnome = false;
            return false;
        }

        public static
        String getGnomeVersion() {
            if (!OS.isLinux() && !OS.isUnix()) {
                return "";
            }

            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8196);
                PrintStream outputStream = new PrintStream(byteArrayOutputStream);

                // gnome-shell --version
                final ShellProcessBuilder shellVersion = new ShellProcessBuilder(outputStream);
                shellVersion.setExecutable("gnome-shell");
                shellVersion.addArgument("--version");
                shellVersion.start();

                String versionString = ShellProcessBuilder.getOutput(byteArrayOutputStream);

                if (!versionString.isEmpty()) {
                    // GNOME Shell 3.14.1
                    String version = versionString.replaceAll("[^\\d.]", "");
                    if (version.length() > 0 && version.indexOf('.') > 0) {
                        // should just be 3.14.1 or 3.20 or similar
                        return version;
                    }
                }
            } catch (Throwable ignored) {
            }

            return null;
        }

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

        // cannot represent '5.6.5' as a number, so we return just the first two decimal places instead
        public static
        double getPlasmaVersion() {
            String versionAsString = getPlasmaVersionFull();
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

        // cannot represent '5.6.5' as a number, so we return a String instead
        public static
        String getPlasmaVersionFull() {
            if (!OS.isLinux() && !OS.isUnix()) {
                return "";
            }

            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8196);
                PrintStream outputStream = new PrintStream(byteArrayOutputStream);

                // plasma-desktop -v
                // plasmashell --version
                final ShellProcessBuilder shellVersion = new ShellProcessBuilder(outputStream);
                shellVersion.setExecutable("plasmashell");
                shellVersion.addArgument("--version");
                shellVersion.start();

                String output = ShellProcessBuilder.getOutput(byteArrayOutputStream);

                if (!output.isEmpty()) {
                    // DEFAULT icon size is 16. KDE is bananas on what they did with tray icon scale
                    // should be: plasmashell 5.6.5   or something
                    String s = "plasmashell ";
                    if (output.contains(s) && !output.contains("not installed")) {
                        return output.substring(output.indexOf(s) + s.length(), output.length());
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

            return "0";
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
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8196);
                PrintStream outputStream = new PrintStream(byteArrayOutputStream);

                // xfconf-query -c xfce4-panel -l
                final ShellProcessBuilder xfconf_query = new ShellProcessBuilder(outputStream);
                xfconf_query.setExecutable("xfconf-query");
                xfconf_query.addArgument("-c " + channel);
                if (property != null) {
                    // get property for channel
                    xfconf_query.addArgument("-p " + property);
                } else {
                    // list all properties for the channel
                    xfconf_query.addArgument("-l");
                }
                xfconf_query.start();

                return ShellProcessBuilder.getOutput(byteArrayOutputStream);
            } catch (Throwable e) {
                e.printStackTrace();
            }

            return "";
        }
    }
}
