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

import com.sun.jna.Function;
import com.sun.jna.Native;
import dorkbox.util.Property;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

public
class GtkSupport {
    // RE: SWT
    // https://developer.gnome.org/glib/stable/glib-Deprecated-Thread-APIs.html#g-thread-init
    // Since version >= 2.24, threads can only init once. Multiple calls do nothing, and we can nest gtk_main()
    // in a nested loop.

    private static volatile boolean started = false;
    private static final ArrayBlockingQueue<Runnable> dispatchEvents = new ArrayBlockingQueue<Runnable>(256);
    private static volatile Thread gtkDispatchThread;

    @Property
    /** Forces the system to always choose GTK2 (even when GTK3 might be available). JavaFX uses GTK2! */
    public static boolean FORCE_GTK2 = false;

    /**
     * must call get() before accessing this! Only "Gtk" interface should access this!
     */
    static volatile Function gtk_status_icon_position_menu = null;

    public static volatile boolean isGtk2 = false;

    /**
     * Helper for GTK, because we could have v3 or v2.
     *
     * Observations: SWT & JavaFX both use GTK2, and we can't load GTK3 if GTK2 symbols are loaded
     */
    @SuppressWarnings("Duplicates")
    public static
    Gtk get() {
        Object library;

        boolean shouldUseGtk2 = GtkSupport.FORCE_GTK2;

        // in some cases, we ALWAYS want to try GTK2 first
        if (shouldUseGtk2) {
            try {
                gtk_status_icon_position_menu = Function.getFunction("gtk-x11-2.0", "gtk_status_icon_position_menu");
                library = Native.loadLibrary("gtk-x11-2.0", Gtk.class);
                if (library != null) {
                    isGtk2 = true;
                    return (Gtk) library;
                }
            } catch (Throwable ignored) {
            }
        }

        if (AppIndicatorQuery.isLoaded) {
            if (AppIndicatorQuery.isVersion3) {
                // appindicator3 requires GTK3
                try {
                    gtk_status_icon_position_menu = Function.getFunction("libgtk-3.so.0", "gtk_status_icon_position_menu");
                    library = Native.loadLibrary("libgtk-3.so.0", Gtk.class);
                    if (library != null) {
                        return (Gtk) library;
                    }
                } catch (Throwable ignored) {
                }
            } else {
                // appindicator1 requires GTK2
                try {
                    gtk_status_icon_position_menu = Function.getFunction("gtk-x11-2.0", "gtk_status_icon_position_menu");
                    library = Native.loadLibrary("gtk-x11-2.0", Gtk.class);
                    if (library != null) {
                        isGtk2 = true;
                        return (Gtk) library;
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        // now for the defaults...

        // start with version 3
        try {
            gtk_status_icon_position_menu = Function.getFunction("libgtk-3.so.0", "gtk_status_icon_position_menu");
            library = Native.loadLibrary("libgtk-3.so.0", Gtk.class);
            if (library != null) {
                return (Gtk) library;
            }
        } catch (Throwable ignored) {
        }

        // now version 2
        try {
            gtk_status_icon_position_menu = Function.getFunction("gtk-x11-2.0", "gtk_status_icon_position_menu");
            library = Native.loadLibrary("gtk-x11-2.0", Gtk.class);
            if (library != null) {
                isGtk2 = true;
                return (Gtk) library;
            }
        } catch (Throwable ignored) {
        }

        throw new RuntimeException("We apologize for this, but we are unable to determine the GTK library is in use, if " +
                                   "or even if it is in use... Please create an issue for this and include your OS type and configuration.");
    }

    public static
    void startGui() {
        // only permit one startup per JVM instance
        if (!started) {
            started = true;

            gtkDispatchThread = new Thread() {
                @Override
                public
                void run() {
                    final Gtk gtk = Gtk.INSTANCE;
                    while (started) {
                        try {
                            final Runnable take = dispatchEvents.take();

                            gtk.gdk_threads_enter();
                            take.run();
                            gtk.gdk_threads_leave();

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            gtkDispatchThread.setName("GTK Event Loop");
            gtkDispatchThread.start();


            // startup the GTK GUI event loop. There can be multiple/nested loops.
            final CountDownLatch blockUntilStarted = new CountDownLatch(1);
            Thread gtkUpdateThread = new Thread() {
                @Override
                public
                void run() {
                    Gtk instance = Gtk.INSTANCE;

                    // prep for the event loop.
                    instance.gdk_threads_init();
                    instance.gtk_init(0, null);
                    GThread.INSTANCE.g_thread_init(null);

                    // notify our main thread to continue
                    blockUntilStarted.countDown();

                    // blocks unit quit
                    instance.gtk_main();
                }
            };
            gtkUpdateThread.setName("GTK Event Loop (Native)");
            gtkUpdateThread.start();

            try {
                // we CANNOT continue until the GTK thread has started!
                blockUntilStarted.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Best practices for GTK, is to call EVERYTHING for it on a SINGLE THREAD. This accomplishes that.
     */
    public static
    void dispatch(Runnable runnable) {
        try {
            dispatchEvents.put(runnable);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static
    void shutdownGui() {
        Gtk.INSTANCE.gtk_main_quit();

        started = false;
        gtkDispatchThread.interrupt();
    }
}
