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
import java.util.Scanner;

import dorkbox.util.process.ShellProcessBuilder;

/**
 * Container for all OS specific tests and methods. These do not exist in OS.java, because of dependency issues (OS.java should not
 * depend on any native libraries)
 */
public
class OsUtil {
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
         * Windows Vista	        6.0.6000  (2006-11-08)
         * Windows Server 2008 SP1	6.0.6001  (2008-02-27)
         * Windows Server 2008 SP2	6.0.6002  (2009-04-28)
         *
         *
         * Windows 7                    6.1.7600  (2009-10-22)
         * Windows Server 2008 R2       6.1.7600  (2009-10-22)
         * Windows Server 2008 R2 SP1   6.1.7601  (?)
         *
         * Windows Home Server 2011		6.1.8400  (2011-04-05)
         *
         * Windows 8                    6.2.9200  (2012-10-26)
         * Windows Server 2012	        6.2.9200  (2012-09-04)
         *
         * Windows 8.1                  6.3.9600  (2013-10-18)
         * Windows Server 2012 R2       6.3.9600  (2013-10-18)
         *
         * Windows 10	                10.0.10240  (2015-07-29)
         * Windows 10	                10.0.10586  (2015-11-12)
         * Windows 10	                10.0.14393  (2016-07-18)
         *
         * Windows Server 2016          10.0.14393  (2016-10-12)
         *
         *
         * @return the [major][minor][patch] version of windows, ie: Windows Version 10.0.10586 -> [10][0][10586]
         */
        public static
        int[] getVersion() {
            int[] version = new int[3];
            version[0] = 0;
            version[1] = 0;
            version[3] = 0;

            if (!OS.isWindows()) {
                return version;
            }

            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8196);
                PrintStream outputStream = new PrintStream(byteArrayOutputStream);

                // cmd.exe /c ver
                final ShellProcessBuilder shellVersion = new ShellProcessBuilder(outputStream);
                shellVersion.setExecutable("cmd.exe");
                shellVersion.addArgument("/c");
                shellVersion.addArgument("ver");
                shellVersion.start();

                String output = ShellProcessBuilder.getOutput(byteArrayOutputStream);

                if (!output.isEmpty()) {
                    // should be: Microsoft Windows [Version 10.0.10586]   or something
                    if (output.contains("ersion ")) {
                        int beginIndex = output.indexOf("ersion ") + 7;
                        int endIndex = output.lastIndexOf("]");

                        String versionString = output.substring(beginIndex, endIndex);
                        String[] split = versionString.split("\\.",-1);

                        if (split.length <= 3) {
                            for (int i = 0; i < split.length; i++) {
                                version[i] = Integer.parseInt(split[i]);
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            return version;
        }
    }

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
                    for (File releaseFile : releaseFiles) {
                        Scanner scanner = new Scanner(releaseFile);

                        BufferedReader reader = new BufferedReader(new FileReader(releaseFile));
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
                    int fedoraVersion = 0;

                    // should be: VERSION_ID=23\n  or something
                    int beginIndex = output.indexOf("VERSION_ID=") + 11;
                    String fedoraVersion_ = output.substring(beginIndex, output.indexOf(OS.LINE_SEPARATOR_UNIX, beginIndex));
                    return Integer.parseInt(fedoraVersion_);
                }
            } catch (Throwable ignored) {
            }

            return 0;
        }

        public static
        boolean getInfo(String id) {
            String output = getInfo();
            // ID=linuxmint/fedora/arch/ubuntu/etc
            return output.contains("ID=" + id +"\n");
        }

        public static
        boolean isArch() {
            return getInfo("arch");
        }

        public static
        boolean isElementaryOS() {
            try {
                String output = getInfo();
                // ID="elementary"  (notice the extra quotes)
                return output.contains("ID=\"elementary\"\n") || output.contains("ID=elementary\n");
            } catch (Throwable ignored) {
            }

            return false;
        }

        public static
        boolean isLinuxMint() {
            return getInfo("linuxmint");
        }

        public static
        boolean isUbuntu() {
            return getInfo("ubuntu");
        }

        public static
        class DesktopEnv {
            public static
            boolean isGnome() {
                if (!OS.isLinux()) {
                    return false;
                }

                try {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8196);
                    PrintStream outputStream = new PrintStream(byteArrayOutputStream);

                    // ps a | grep [g]nome-shell
                    final ShellProcessBuilder shell = new ShellProcessBuilder(outputStream);
                    shell.setExecutable("ps");
                    shell.addArgument("a");
                    shell.start();

                    String output = ShellProcessBuilder.getOutput(byteArrayOutputStream);
                    return output.contains("gnome-shell");
                } catch (Throwable ignored) {
                }

                return false;
            }

            public static
            String getGnomeVersion() {
                if (!OS.isLinux()) {
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
                if (!OS.isLinux()) {
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
        }
    }
}
