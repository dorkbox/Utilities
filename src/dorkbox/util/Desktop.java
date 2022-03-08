/*
 * Copyright 2021 dorkbox, llc
 *
 * Copyright (C) 2016 Tres Finocchiaro, QZ Industries, LLC
 * Derivative code has been released as Apache 2.0, used with permission.
 *
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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import dorkbox.executor.Executor;
import dorkbox.jna.linux.GnomeVFS;
import dorkbox.jna.linux.GtkCheck;
import dorkbox.jna.linux.GtkEventDispatch;
import dorkbox.os.OS;

@SuppressWarnings({"WeakerAccess", "Convert2Lambda", "Duplicates"})
public
class Desktop {
    // used for any linux system that has it...
    private static final String GVFS = "/usr/bin/gvfs-open";
    private static final boolean GVFS_VALID = new File(GVFS).canExecute();

    /**
     * Gets the version number.
     */
    public static
    String getVersion() {
        return "1.18";
    }

    static {
        // Add this project to the updates system, which verifies this class + UUID + version information
        dorkbox.updates.Updates.INSTANCE.add(Desktop.class, "b4c69a68f6b747228592db0800809e30", getVersion());
    }

    /**
     * Launches the associated application to open the file.
     * <p>
     * If the specified file is a directory, the file manager of the current platform is launched to open it.
     * <p>
     * Important Note:
     *   Apple tries to launch <code>.app</code> bundle directories as applications rather than browsing contents.
     *   If this is the case, use `browseDirectory()` instead.
     *
     *
     * @param file the file to open
     */
    public static
    void open(final File file) throws IOException {
        if (file == null) {
            throw new IOException("File must not be null.");
        }

        // Apple tries to launch <code>.app</code> bundle directories as applications rather than browsing contents
        // WE DO NOTHING HERE TO PREVENT THAT.


        if (requireUnixLauncher()) {
            launchNix(file.toString());
        }
        else if (awtSupported(java.awt.Desktop.Action.OPEN)) {
            // make sure this doesn't block the current UI
            SwingUtil.invokeLater(new Runnable() {
                @Override
                public
                void run() {
                    try {
                        java.awt.Desktop.getDesktop().open(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        else {
            throw new IOException("Current OS and desktop configuration does not support `open`");
        }
    }






    /**
     * Launches the default browser to display the specified HTTP address.
     * <p>
     * If the default browser is not able to handle the specified address, the application registered for handling
     * HTTP requests of the specified type is invoked.
     *
     * @param address the URL to browse/open
     */
    public static
    void browseURL(String address) throws IOException {
        if (address == null || address.isEmpty()) {
            throw new IOException("Address must not be null or empty.");
        }

        URI uri;
        try {
            uri = new URI(address);
            browseURL(uri);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URI " + address);
        }
    }

    /**
     * Launches the default browser to display a {@code URI}.
     * <p>
     * If the default browser is not able to handle the specified {@code URI}, the application registered for handling
     * {@code URIs} of the specified type is invoked. The application is determined from the protocol and path of the {@code URI}, as
     * defined by the {@code URI} class.
     *
     * @param uri the URL to browse/open
     */
    public static
    void browseURL(final URI uri) throws IOException {
        if (uri == null) {
            throw new IOException("URI must not be null.");
        }

        if (requireUnixLauncher()) {
            launchNix(uri.toString());
        }
        else if (awtSupported(java.awt.Desktop.Action.BROWSE)) {
            // make sure this doesn't block the current UI
            SwingUtil.invokeLater(new Runnable() {
                @Override
                public
                void run() {
                    try {
                        java.awt.Desktop.getDesktop().browse(uri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        else {
            throw new IOException("Current OS and desktop configuration does not support `browseURL`");
        }
    }

    /**
     * Launches the mail composing window of the user default mail client for the specified address.
     *
     * @param address who the email goes to
     */
    public static
    void launchEmail(String address) throws IOException {
        if (address == null || address.isEmpty()) {
            throw new IOException("Address must not be null or empty.");
        }

        URI uri;
        try {
            if (!address.startsWith("mailto:")) {
                address = "mailto:" + address;
            }

            uri = new URI(address);
            launchEmail(uri);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URI " + address);
        }
    }

    /**
     * Launches the mail composing window of the user default mail client, filling the message fields specified by a {@code mailto:} URI.
     * <p>
     * A <code>mailto:</code> URI can specify message fields including <i>"to"</i>, <i>"cc"</i>, <i>"subject"</i>, <i>"body"</i>, etc.
     * See <a href="http://www.ietf.org/rfc/rfc2368.txt">The mailto URL scheme (RFC 2368)</a> for the {@code mailto:} URI specification
     * details.
     *
     * @param uri the specified {@code mailto:} URI
     */
    public static
    void launchEmail(final URI uri) throws IOException {
        if (uri == null) {
            throw new IOException("URI must not be null.");
        }

        if (requireUnixLauncher()) {
            launchNix(uri.toString());
        }
        else if (awtSupported(java.awt.Desktop.Action.MAIL)) {
            // make sure this doesn't block the current UI
            SwingUtil.invokeLater(new Runnable() {
                @Override
                public
                void run() {
                    try {
                        java.awt.Desktop.getDesktop().mail(uri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        else {
            throw new IOException("Current OS and desktop configuration does not support `launchEmail`");
        }
    }

    /**
     * Opens the specified path in the system-default file browser.
     * <p>
     * Works around several OS limitations:
     * - Apple tries to launch <code>.app</code> bundle directories as applications rather than browsing contents
     * - Linux has mixed support for <code>Desktop.getDesktop()</code>.  Uses <code>JNA</code> instead.
     *
     * @param path The directory to browse
     */
    public static
    void browseDirectory(String path) throws IOException {
        if (path == null || path.isEmpty()) {
            throw new IOException("Path must not be null or empty.");
        }

        if (OS.INSTANCE.isMacOsX()) {
            File directory = new File(path);

            // Mac tries to open the .app rather than browsing it.  Instead, pass a child with -R to select it in finder
            File[] files = directory.listFiles();
            if (files != null && files.length > 0) {
                // Get first child
                File child = files[0];
                new Executor().command("open", "-R", child.getCanonicalPath()).startAsync();
            }
        }
        if (requireUnixLauncher()) {
            // it can actually be MORE that just "file://" (ie, "ftp://" is legit as well)
            if (!path.contains("://")) {
                path = "file://" + path;
            }

            launchNix(path);
        }
        else if (awtSupported(java.awt.Desktop.Action.OPEN)) {
            final String finalPath = path;

            // make sure this doesn't block the current UI
            SwingUtil.invokeLater(new Runnable() {
                @Override
                public
                void run() {
                    try {
                        java.awt.Desktop.getDesktop().open(new File(finalPath));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        else {
            throw new IOException("Current OS and desktop configuration does not support `browseDirectory`");
        }
    }

    /**
     * Check if this action is required to use the unix launcher. This prevent GTK2/3 conflict
     * caused by Desktop.getDesktop(), which is GTK2 only (via AWT)
     *
     * CLI calls and AWT call the same methods...
     *  - AWT doesn't work with GTK3
     *  - CLI has thread/memory overhead
     */
    private static boolean requireUnixLauncher() {
        return ((OS.INSTANCE.isUnix() || OS.INSTANCE.isLinux()) && (GtkCheck.isGtkLoaded && GtkCheck.isGtk3));
    }

    /**
     * Check if this action is supported by AWT
     */
    private static boolean awtSupported(java.awt.Desktop.Action action) {
        return java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(action);
    }

    /**
     * Only called when (OS.isUnix() || OS.isLinux()) && GtkCheck.isGtkLoaded
     *
     * Of important note, xdg-open can cause problems in Linux with Chrome installed but not the default browser. It will crash Chrome
     * if Chrome was open before this app opened a URL
     *
     * There are a number of strange bugs with `xdg-open` and `gnome_vfs_url_show_with_env`, ubuntu, once again takes the cake for stupidity.
     *
     * @param path the path to open
     */
    private static
    void launchNix(final String path) throws IOException {
        if (GVFS_VALID) {
            // ubuntu, fedora, etc MIGHT have access to gvfs-open. Ubuntu is also VERY buggy with xdg-open!!
            new Executor().command(GVFS, path).startAsync();
        }
        else if (OS.DesktopEnv.INSTANCE.isGnome() && GnomeVFS.isInited) {
            GtkEventDispatch.dispatch(new Runnable() {
                @Override
                public
                void run() {
                    // try to open the URL via gnome. This is exactly how (ultimately) java natively does this, but we do it via our own
                    // loaded version of GTK via JNA
                    int errorCode = GnomeVFS.gnome_vfs_url_show_with_env(path, null);
                    if (errorCode != 0) {
                        // if there are problems, use xdg-open
                        //
                        // there are problems with ubuntu and practically everything. Errors galore, and sometimes things don't even work.
                        // see: https://bugzilla.mozilla.org/show_bug.cgi?id=672671
                        // this can be really buggy ... you have been warned
                        try {
                            new Executor().command("xdg-open", path).startAsync();
                        } catch (IOException ignored) {
                        }
                    }
                }
            });
        }
        else {
            // just use xdg-open, since it's not gnome.
            // this can be really buggy ... you have been warned
            new Executor().command("xdg-open", path).startAsync();
        }
    }
}
