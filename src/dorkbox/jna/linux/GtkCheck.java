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

import dorkbox.jna.rendering.RenderProvider;
import dorkbox.os.OS;

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
     * NOTE: this WILL NOT attempt to load swing, and will use reflection for JAVA <=8 to check GTK version info
     *
     * @return the version of GTK loaded. 0=no GTK loaded (or unknown), 2=GTK2, 3=GTK3
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

        int gtkVersion = RenderProvider.getGtkVersion();
        if (gtkVersion > 0) {
            return gtkVersion;
        }

        /*
         * Checks to see if GTK is loaded by Swing from the "Look and Feel", and if so - which version is loaded.
         *
         * NOTE: if the UI uses the 'getSystemLookAndFeelClassName' and is on Linux and it's the GtkLookAndFeel,
         *   this will cause GTK2 to get loaded FIRST, which will cause conflicts if one tries to use GTK3 (and it's GTK2)
         *
         * The **ONLY** issue we have is if WE are GTK3 and SWING is GTK2...
         */

        // java 8 cannot load GTK3. But we can know if GTK was loaded yet or not
        if (OS.INSTANCE.getJavaVersion() <= 8) {
            try {
                // Don't want to load the toolkit!!!
                Class<?> toolkitClass = Class.forName("java.awt.Toolkit");
                java.lang.reflect.Field kitField = toolkitClass.getDeclaredField("toolkit");
                kitField.setAccessible(true);
                Object toolkit = kitField.get(null);
                if (toolkit != null) {
                    Class<?> unixTkClazz = Class.forName("sun.awt.UNIXToolkit");
                    if (unixTkClazz.isAssignableFrom(toolkit.getClass())) {
                        java.lang.reflect.Field field = unixTkClazz.getDeclaredField("nativeGTKLoaded");
                        field.setAccessible(true);
                        Boolean o = (Boolean) field.get(toolkit);
                        //noinspection UnnecessaryUnboxing
                        if (o != null && o.booleanValue()) {
                            // if gtk is loaded, it **must* be version 2, since java <=8 cannot load GTK3
                            return 2;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        // don't know without forcing GTK to potentially load
        return 0;
    }
}
