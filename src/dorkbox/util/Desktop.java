/*
 * Copyright 2017 dorkbox, llc
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

import dorkbox.util.jna.linux.Gtk2;
import dorkbox.util.jna.linux.GtkEventDispatch;
import dorkbox.util.process.ShellExecutor;

@SuppressWarnings({"WeakerAccess", "Convert2Lambda"})
public
class Desktop {
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

        // Prevent GTK2/3 conflict caused by Desktop.getDesktop(), which is GTK2 only (via AWT)
        // Prefer JNA method over AWT, since there are fewer chances for JNA to fail (even though they call the same method)
        // Additionally, xdg-open  can cause problems in Linux with Chrome installed but not the default browser. It will crash Chrome
        //      if Chrome was open before this app opened a URL
        if ((OS.isUnix() || OS.isLinux()) && OSUtil.DesktopEnv.isGtkLoaded) {
            GtkEventDispatch.dispatch(new Runnable() {
                @Override
                public
                void run() {
                    Gtk2.Gtk2.gtk_show_uri(Gtk2.Gtk2.gdk_screen_get_default(), uri.toString(), 0, null);
                }
            });
        }
        else if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop()
                                                                          .isSupported(java.awt.Desktop.Action.BROWSE)) {
            java.awt.Desktop.getDesktop()
                            .browse(uri);
        }
        else {
            throw new IOException("Current OS and desktop configuration does not support browsing for a URL");
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

        // Prevent GTK2/3 conflict caused by Desktop.getDesktop(), which is GTK2 only (via AWT)
        if ((OS.isUnix() || OS.isLinux()) && OSUtil.DesktopEnv.isGtkLoaded) {
            GtkEventDispatch.dispatch(new Runnable() {
                @Override
                public
                void run() {
                    Gtk2.Gtk2.gtk_show_uri(Gtk2.Gtk2.gdk_screen_get_default(), uri.toString(), 0, null);
                }
            });
        }
        else if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop()
                                                                          .isSupported(java.awt.Desktop.Action.MAIL)) {
            java.awt.Desktop.getDesktop()
                            .mail(uri);
        }
        else {
            throw new IOException("Current OS and desktop configuration does not support launching an email client");
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

        if (OS.isMacOsX()) {
            File directory = new File(path);

            // Mac tries to open the .app rather than browsing it.  Instead, pass a child with -R to select it in finder
            File[] files = directory.listFiles();
            if (files != null && files.length > 0) {
                // Get first child
                File child = files[0];
                if (!ShellExecutor.run("open", "-R", child.getCanonicalPath())) {
                    throw new IOException("Error opening the directory for " + path);
                }
            }
        }
        // Prevent GTK2/3 conflict caused by Desktop.getDesktop(), which is GTK2 only (via AWT)
        // Prefer JNA method over AWT, since there are fewer chances for JNA to fail (even though they call the same method)
        else if ((OS.isUnix() || OS.isLinux()) && OSUtil.DesktopEnv.isGtkLoaded) {
            // it can actually be MORE that just "file://" (ie, "ftp://" is legit as well)
            if (!path.contains("://")) {
                path = "file://" + path;
            }

            final String finalPath = path;
            GtkEventDispatch.dispatch(new Runnable() {
                @Override
                public
                void run() {
                    Gtk2.Gtk2.gtk_show_uri(Gtk2.Gtk2.gdk_screen_get_default(), finalPath, 0, null);
                }
            });
        }
        else if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop()
                                                                          .isSupported(java.awt.Desktop.Action.OPEN)) {
            java.awt.Desktop.getDesktop()
                            .open(new File(path));
        }
        else {
            throw new IOException("Current OS and desktop configuration does not support opening a directory to browse");
        }
    }
}
