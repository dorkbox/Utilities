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

package dorkbox.util.jna.linux;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * Helper for AppIndicator, because it is absolutely mindboggling how those whom maintain the standard, can't agree to what that standard
 * library naming convention or features set is. We just try until we find one that work, and are able to map the symbols we need.
 */
public
class AppIndicatorQuery {
    public static
    Object get_v1() {
        // version 1 is better than version 3, because of dumb shit redhat did.
        // to wit, installing the google chrome browser, will ALSO install the correct appindicator library.
        try {
            Object appindicator = Native.loadLibrary("appindicator", Library.class);
            if (appindicator != null) {
                String s = appindicator.toString();
                    // make sure it's actually v1. If it's NOT v1 (ie: v3) then fallback to GTK version
                if (s.indexOf(".so.1") < 1) {
                    return null;
                }
            }
            return appindicator;
        } catch (Throwable ignored) {
            ignored.printStackTrace();
        }

        return null;
    }

    public static
    Object get() {
        Object library;

        // start with base version
        try {
            library = Native.loadLibrary("appindicator", AppIndicator.class);
            if (library != null) {
                return library;
            }
        } catch (Throwable ignored) {
        }

        // whoops. Symbolic links are bugged out. Look manually for it...

        // version 1 is better than version 3, because of dumb shit redhat did.
        try {
            library = Native.loadLibrary("appindicator1", AppIndicator.class);
            if (library != null) {
                return library;
            }
        } catch (Throwable ignored) {
        }

        // now check all others. super hacky way to do this.
        for (int i = 10; i >= 0; i--) {
            try {
                library = Native.loadLibrary("appindicator" + i, AppIndicator.class);
                if (library != null) {
                    if (i == 3) {
                        System.err.println("AppIndicator3 detected. This version is SEVERELY limited, and menu icons WILL NOT be visible. " +
                                           "Please install a better version of libappindicator. One such command to do so is:   " +
                                           "sudo apt-get install libappindicator1");
                    }
                    return library;
                }
            } catch (Throwable ignored) {
            }
        }

        // another type. who knows...
        try {
            library = Native.loadLibrary("appindicator-gtk", AppIndicator.class);
            if (library != null) {
                return library;
            }
        } catch (Throwable ignored) {
        }

        // this is HORRID. such a PITA
        try {
            library = Native.loadLibrary("appindicator-gtk3", AppIndicator.class);
            if (library != null) {
                return library;
            }
        } catch (Throwable ignored) {
        }

        throw new RuntimeException("We apologize for this, but we are unable to determine the appIndicator library is in use, if " +
                                   "or even if it is in use... Please create an issue for this and include your OS type and configuration.");
    }
}
