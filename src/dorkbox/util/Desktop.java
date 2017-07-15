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

import dorkbox.util.process.ShellExecutor;

@SuppressWarnings("WeakerAccess")
public
class Desktop {
    /**
     * Launches the default browser to display a {@code URI}.
     *
     * If the default browser is not able to handle the specified {@code URI}, the application registered for handling
     * {@code URIs} of the specified type is invoked. The application is determined from the protocol and path of the {@code URI}, as
     * defined by the {@code URI} class.
     *
     * @param uri the URL to browse/open
     *
     * @throws IOException
     */
    public static void browseURL(URI uri) throws IOException {
        // Prevent GTK2/3 conflict caused by Desktop.getDesktop(), which is GTK2 only (via AWT)
        if ((OS.isUnix() || OS.isLinux()) && OSUtil.DesktopEnv.isGtkLoaded && OSUtil.DesktopEnv.isGtk3) {
            if (!ShellExecutor.run("xdg-open", uri.toString())) {
                throw new IOException("Error running xdg-open for " + uri.toString());
            }
        }
        else {
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(uri);
            } else {
                throw new IOException("Current OS and desktop configuration does not support browsing for a URL");
            }
        }
    }

    /**
     * Launches the mail composing window of the user default mail client for the specified address.
     *
     * @param address who the email goes to
     *
     * @throws IOException
     */
    public static void launchEmail(String address) throws IOException {
        URI uri = null;
        try {
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
     *
     * @throws IOException
     */
    public static void launchEmail(final URI uri) throws IOException {
        // Prevent GTK2/3 conflict caused by Desktop.getDesktop(), which is GTK2 only (via AWT)
        if ((OS.isUnix() || OS.isLinux()) && OSUtil.DesktopEnv.isGtkLoaded && OSUtil.DesktopEnv.isGtk3) {
            if (!ShellExecutor.run("xdg-email", uri.toString())) {
                throw new IOException("Error running xdg-email for " + uri.toString());
            }
        }
        else {
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.MAIL)) {
                java.awt.Desktop.getDesktop().mail(uri);
            } else {
                throw new IOException("Current OS and desktop configuration does not support launching an email client");
            }
        }
    }

    /**
     * Opens the specified path in the system-default file browser.
     *
     * Works around several OS limitations:
     *  - Apple tries to launch <code>.app</code> bundle directories as applications rather than browsing contents
     *  - Linux has mixed support for <code>Desktop.getDesktop()</code>.  Uses the <code>xdg-open</code> fallback.
     *
     * @param path The directory to browse
     *
     * @throws IOException
     */
    public static void browseDirectory(String path) throws IOException {
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
        } else {
            // Prevent GTK2/3 conflict caused by Desktop.getDesktop(), which is GTK2 only (via AWT)
            if ((OS.isUnix() || OS.isLinux()) && OSUtil.DesktopEnv.isGtkLoaded && OSUtil.DesktopEnv.isGtk3) {
                if (!ShellExecutor.run("xdg-open", path)) {
                    throw new IOException("Error running xdg-open for " + path);
                }
            }
            else {
                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                    java.awt.Desktop.getDesktop().open(new File(path));
                    return;
                } else {
                    throw new IOException("Current OS and desktop configuration does not support opening a directory to browse");
                }
            }
        }

        throw new IOException("Unable to open " + path);
    }
}
