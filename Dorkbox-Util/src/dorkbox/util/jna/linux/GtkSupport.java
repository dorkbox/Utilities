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

import dorkbox.util.Property;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

public
class GtkSupport {
    public static final boolean isSupported;

    // RE: SWT
    // https://developer.gnome.org/glib/stable/glib-Deprecated-Thread-APIs.html#g-thread-init
    // Since version >= 2.24, threads can only init once. Multiple calls do nothing, and we can nest gtk_main()
    // in a nested loop.

    private static volatile boolean started = false;
    private static final ArrayBlockingQueue<Runnable> dispatchEvents = new ArrayBlockingQueue<Runnable>(256);
    private static volatile Thread gtkDispatchThread;

    @Property
    /** Enables/Disables the creation of a native GTK event loop. Useful if you are already creating one via SWT/etc. */
    public static boolean CREATE_EVENT_LOOP = true;

    static {
        boolean hasSupport = false;
        try {
            if (Gtk.INSTANCE != null && Gobject.INSTANCE != null && GThread.INSTANCE != null) {
                hasSupport = true;
            }
        } catch (Throwable ignored) {
        }

        isSupported = hasSupport;
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


            if (CREATE_EVENT_LOOP) {
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
        if (CREATE_EVENT_LOOP) {
            Gtk.INSTANCE.gtk_main_quit();
        }

        started = false;
        gtkDispatchThread.interrupt();
    }
}
