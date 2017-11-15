/*
 * Copyright 2017 dorkbox, llc
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
package dorkbox.util.jna.linux;

import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

import dorkbox.util.jna.JnaHelper;

/**
 * bindings for gnome
 * <p>
 * Direct-mapping, See: https://github.com/java-native-access/jna/blob/master/www/DirectMapping.md
 * <p>
 * https://github.com/GNOME/libgnome/blob/master/libgnome/gnome-url.c
 *
 * NOTE: This is used to open URL/file/email/etc from java. In different places, they recommend using gtk_show_uri() -- we support that,
 * NOTE:   HOWEVER there are problems where GTK warnings/errors will STILL SHOW on the console for whatever target application is opened,
 * NOTE:   and because of these errors, it looks like crap. gnome_vfs_url_show_with_env() solves this problem.
 */
public
class GnomeVFS {
    public final static boolean isInited;

    static {
        boolean init = false;
        try {
            NativeLibrary library = JnaHelper.register("libgnomevfs-2", GnomeVFS.class);
            if (library == null) {
                // try with no version
                library = JnaHelper.register("libgnomevfs", GnomeVFS.class);
            }
            if (library == null) {
                // try v3 (maybe this happened? Not likely, but who knows)
                library = JnaHelper.register("libgnomevfs-3", GnomeVFS.class);
            }

            if (library == null) {
                // not loading :/
                // fail silently, because we only use this for loading URLs, and have fallbacks in place
                // LoggerFactory.getLogger(GnomeVFS.class).error("Error loading GnomeVFS library, it failed to load.");
            } else {
                // must call call gnome_vfs_init()
                GnomeVFS.gnome_vfs_init();
                init = true;
            }
        } catch (Throwable e) {
            // fail silently, because we only use this for loading URLs, and have fallbacks in place
            // LoggerFactory.getLogger(GnomeVFS.class).error("Error loading GnomeVFS library, it failed to load {}", e.getMessage());
        }

        isInited = init;
    }

    public static native
    void gnome_vfs_init();

    /**
     * Open a URL or path to display using the default/registered handlers.
     *
     * @param url The url or path to display. The path can be relative to the current working
     * directory or the user's home directory. This function will convert it into a fully
     * qualified url using the gnome_url_get_from_input function.
     *
     * @return 0 if successful, non-0 if there were issues.
     */
    public static native
    int gnome_vfs_url_show_with_env(String url, Pointer shouldbeNull);
}
