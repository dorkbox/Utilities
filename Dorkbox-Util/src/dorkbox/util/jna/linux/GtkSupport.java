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

import java.util.concurrent.CountDownLatch;

public
class GtkSupport {
    public static final boolean isSupported;
    private static final boolean hasSwt;

    private static volatile boolean started = false;

    static {
        boolean hasSupport = false;
        boolean hasSWT_ = false;
        try {
            if (Gtk.INSTANCE != null && Gobject.INSTANCE != null && GThread.INSTANCE != null) {
                hasSupport = true;

                try {
                    Class<?> swtClass = Class.forName("org.eclipse.swt.widgets.Display");
                    if (swtClass != null) {
                        hasSWT_ = true;
                    }
                } catch (Throwable ignore) {
                }


                // prep for the event loop.
                // since SWT uses one already, it's not necessary to have two.
                if (!hasSWT_) {
                    Gtk instance = Gtk.INSTANCE;
                    instance.gtk_init(0, null);
                    GThread.INSTANCE.g_thread_init(null);
                    instance.gdk_threads_init();
                }
            }
        } catch (Throwable ignored) {
        }

        isSupported = hasSupport;
        hasSwt = hasSWT_;
    }

    public static
    void startGui() {
        // only permit one startup per JVM instance
        if (!started) {
            started = true;

            // startup the GTK GUI event loop. There can be multiple/nested loops.
            // since SWT uses one already, it's not necessary to have two.
            if (!hasSwt) {
                final CountDownLatch blockUntilStarted = new CountDownLatch(1);
                Thread gtkUpdateThread = new Thread() {
                    @Override
                    public
                    void run() {
                        Gtk instance = Gtk.INSTANCE;

                        // notify our main thread to continue
                        blockUntilStarted.countDown();
                        instance.gdk_threads_enter();
                        instance.gtk_main();
                        // MUST leave as well!
                        instance.gdk_threads_leave();
                    }
                };
                gtkUpdateThread.setName("GTK Event Loop");
                gtkUpdateThread.start();

                try {
                    // we CANNOT continue until the GTK thread has started! (ignored if SWT is used)
                    blockUntilStarted.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static
    void shutdownGui() {
        if (isSupported && !hasSwt) {
            Gtk.INSTANCE.gtk_main_quit();
            started = false;
        }
    }
}
