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
package dorkbox.jna.linux;

import dorkbox.javaFx.JavaFx;
import dorkbox.swt.Swt;
import dorkbox.util.SwingUtil;

/**
 * Accessor methods/logic for determining if GTK is already loaded by the Swing/JavaFX/SWT, or if GTK has been manually loaded via
 * GtkEventDispatch.startGui().
 */
@SuppressWarnings("WeakerAccess")
public
class GtkCheck {
    /**
     * Only valid if `isGtkLoaded=true`. Determine if the application is running via GTK2.
     * <p>
     * This does not cause GTK to load, where calls to Gtk.isGtk2 will
     */
    public static volatile boolean isGtk2 = false;

    /**
     * Only valid if `isGtkLoaded=true`. Determine if the application is running via GTK3.
     * <p>
     * This does not cause GTK to load, where calls to Gtk.isGtk2 will
     */
    public static volatile boolean isGtk3 = false;

    /**
     * Determine if the application has *MANUALLY* loaded GTK yet or not. This does not cause GTK to load, where calls to Gtk.isLoaded will
     */
    public static volatile boolean isGtkLoaded = false;


    /** If GTK is loaded, this is the GTK MAJOR version */
    public static volatile int MAJOR = 0;

    /** If GTK is loaded, this is the GTK MINOR version */
    public static volatile int MINOR = 0;

    /** If GTK is loaded, this is the GTK MICRO version */
    public static volatile int MICRO = 0;


    /**
     * @return true if the currently loaded GTK version is greater to or equal to the passed-in major.minor.mico
     */
    public static
    boolean gtkIsGreaterOrEqual(final int major, final int minor, final int micro) {
        if (MAJOR > major) {
            return true;
        }
        if (MAJOR < major) {
            return false;
        }

        if (MINOR > minor) {
            return true;
        }
        if (MINOR < minor) {
            return false;
        }

        if (MICRO > micro) {
            return true;
        }
        if (MICRO < micro) {
            return false;
        }

        // same exact version
        return true;
    }

    /**
     * This method is agnostic w.r.t. how GTK is loaded, which can be manually loaded or loaded via JavaFX/SWT/Swing.
     *
     * @return the version of GTK loaded. 0=no GTK loaded, 2=GTK2, 3=GTK3
     */
    public static
    int getLoadedGtkVersion() {
        // if we have ALREADY loaded GTK, then return that information.
        if (isGtkLoaded) {
            if (isGtk3) {
                return 3;
            } else {
                return 2;
            }
        }

        if (Swt.isLoaded) {
            if (Swt.isGtk3) {
                return 3;
            } else {
                return 2;
            }
        }

        if (JavaFx.isLoaded) {
            if (JavaFx.isGtk3) {
                return 3;
            } else {
                return 2;
            }
        }

        // now check if swing has loaded GTK from the Look and Feel
        return SwingUtil.getLoadedGtkVersion();
    }


}
