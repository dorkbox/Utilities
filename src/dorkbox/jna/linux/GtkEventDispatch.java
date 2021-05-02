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
package dorkbox.jna.linux;

import static dorkbox.jna.linux.Gtk2.FALSE;
import static dorkbox.jna.linux.Gtk2.Gtk2;

import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;

import dorkbox.jna.rendering.RenderProvider;


public
class GtkEventDispatch {
    static boolean FORCE_GTK2 = false;
    static boolean PREFER_GTK3 = false;
    static boolean DEBUG = false;

    // have to save these in a field to prevent GC on the objects (since they go out-of-scope from java)
    private static final LinkedList<FuncCallback> gtkCallbacks = new LinkedList<FuncCallback>();

    // This is required because the EDT needs to have it's own value for this boolean, that is a different value than the main thread
    private static ThreadLocal<Boolean> isDispatch = new ThreadLocal<Boolean>() {
        @Override
        protected
        Boolean initialValue() {
            return false;
        }
    };

    private static boolean started = false;

    @SuppressWarnings("FieldCanBeLocal")
    private static Thread gtkUpdateThread = null;

    private static GMainLoop mainloop;
    private static GMainContext context;

    // when debugging the EDT, we need a longer timeout.
    private static final boolean debugEDT = false;

    // timeout is in seconds
    private static final int TIMEOUT = debugEDT ? 10000000 : 2;


    /**
     * This will load and start GTK
     */
    public static synchronized
    void startGui(final boolean forceGtk2, final boolean preferGkt3, final boolean debug) {
        // only permit one startup per JVM instance
        if (!started) {
            started = true;

            GtkEventDispatch.FORCE_GTK2 = forceGtk2;
            GtkEventDispatch.PREFER_GTK3 = preferGkt3;
            GtkEventDispatch.DEBUG = debug;

            // startup the GTK GUI event loop. There can be multiple/nested loops.
            if (!GtkLoader.alreadyRunningGTK) {
                // If JavaFX/SWT is used, this is UNNECESSARY (we can detect if the GTK main_loop is running)

                gtkUpdateThread = new Thread() {
                    @Override
                    public
                    void run() {
                        Glib.GLogFunc orig = null;
                        if (debug) {
                            // don't suppress GTK warnings in debug mode
                            LoggerFactory.getLogger(GtkEventDispatch.class).debug("Running GTK Native Event Loop");
                        } else {
                            // NOTE: This can output warnings, so we suppress them. Additionally, setting System.err to null, or trying
                            // to filter it, will not suppress these errors/warnings
                            orig = Glib.g_log_set_default_handler(Glib.nullLogFunc, null);
                        }


                        if (!Gtk2.gtk_init_check(0)) {
                            throw new RuntimeException("Error starting GTK");
                        }


                        // create the main-loop
                        mainloop = Gtk2.g_main_loop_new(null, false);
                        context = Gtk2.g_main_loop_get_context(mainloop);

                        // blocks until we quit the main loop
                        Gtk2.g_main_loop_run(mainloop);


                        if (orig != null) {
                            Glib.g_log_set_default_handler(orig, null);
                        }
                    }
                };
                gtkUpdateThread.setDaemon(false); // explicitly NOT daemon so that this will hold the JVM open as necessary
                gtkUpdateThread.setName("GTK Native Event Loop");
                gtkUpdateThread.start();
            }
        }
    }

    /**
     * Waits for the all posted events to GTK to finish loading
     */
    @SuppressWarnings("Duplicates")
    public static synchronized
    void waitForEventsToComplete() {
        final CountDownLatch blockUntilStarted = new CountDownLatch(1);

        dispatch(new Runnable() {
            @Override
            public
            void run() {
                blockUntilStarted.countDown();
            }
        });

        if (!RenderProvider.isEventThread()) {
            try {
                if (!blockUntilStarted.await(10, TimeUnit.SECONDS)) {
                    if (DEBUG) {
                        LoggerFactory.getLogger(GtkEventDispatch.class)
                                     .error("Something is very wrong. The waitForEventsToComplete took longer than expected.",
                                            new Exception(""));
                    }
                }

                // we have to WAIT until all events are done processing, OTHERWISE we have initialization issues
                while (true) {
                    Thread.sleep(100);

                    synchronized (gtkCallbacks) {
                        if (gtkCallbacks.isEmpty()) {
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Dispatch the runnable to GTK and wait until it has finished executing. If this is called while on the GTK dispatch thread, it will
     * immediately execute the task, otherwise it will submit the task to run on the FIFO queue.
     */
    public static
    void dispatchAndWait(final Runnable runnable) {
        // if we are on the dispatch queue, do not block
        Boolean isDispatch = GtkEventDispatch.isDispatch.get();
        if (isDispatch) {
            // don't block. The ORIGINAL call (before items were queued) will still be blocking. If the original call was a "normal"
            // dispatch, then subsequent dispatchAndWait calls are irrelevant (as they happen in the GTK thread, and not the main thread).
            runnable.run();
            return;
        }


        final CountDownLatch countDownLatch = new CountDownLatch(1);
        dispatch(new Runnable() {
            @Override
            public
            void run() {
                try {
                    runnable.run();
                } catch (Exception e) {
                    LoggerFactory.getLogger(GtkEventDispatch.class).error("Error during GTK run loop: ", e);
                } finally {
                    countDownLatch.countDown();
                }
            }
        });

        // this is slightly different than how swing does it. We have a timeout here so that we can make sure that updates on the GUI
        // thread occur in REASONABLE time-frames, and alert the user if not.
        try {
            if (!countDownLatch.await(TIMEOUT, TimeUnit.SECONDS)) {
                if (DEBUG) {
                    LoggerFactory.getLogger(GtkEventDispatch.class).error(
                                    "Something is very wrong. The Event Dispatch Queue took longer than " + TIMEOUT + " seconds " +
                                    "to complete.", new Exception(""));
                }
                else {
                    throw new RuntimeException("Something is very wrong. The Event Dispatch Queue took longer than " + TIMEOUT +
                                               " seconds " + "to complete.");
                }
            }
        } catch (InterruptedException e) {
            LoggerFactory.getLogger(GtkEventDispatch.class).error("Error waiting for dispatch to complete.", new Exception(""));
        }
    }

    /**
     * Best practices for GTK, is to call everything for it on the GTK THREAD. If we are currently on the dispatch thread, then this
     * task will execute immediately.
     */
    public static
    void dispatch(final Runnable runnable) {
        if (GtkLoader.alreadyRunningGTK && RenderProvider.dispatch(runnable)) {
            return;
        }

        // not javafx
        // gtk/swt are **mostly** the same in how events are dispatched, so we can use "raw" gtk methods for SWT
        if (isDispatch.get()) {
            // Run directly on the dispatch thread. This will be false unless we are running the dispatch queue.
            runnable.run();
            return;
        }

        final FuncCallback callback = new FuncCallback() {
            @Override
            public
            int callback(final Pointer data) {
                isDispatch.set(true);

                try {
                    runnable.run();
                } finally {
                    isDispatch.set(false);
                }

                synchronized (gtkCallbacks) {
                    gtkCallbacks.removeFirst(); // now that we've 'handled' it, we can remove it from our callback list
                }

                return FALSE; // don't want to call this again
            }
        };

        synchronized (gtkCallbacks) {
            gtkCallbacks.offer(callback); // prevent GC from collecting this object before it can be called
        }

        // explicitly invoke on our new GTK main loop context
        Gtk2.g_main_context_invoke(context, callback, null);
    }

    /**
     * required to properly setup the dispatch flag when using native menus
     *
     * @param callback will never be null.
     */
    public static
    void proxyClick(final ActionListener callback) {
        isDispatch.set(true);

        try {
            callback.actionPerformed(null);
        } catch (Throwable t) {
            LoggerFactory.getLogger(GtkEventDispatch.class)
                         .error("Error during GTK click callback: ", t);
        }

        isDispatch.set(false);
    }
    public static synchronized
    void shutdownGui() {
        dispatchAndWait(new Runnable() {
            @Override
            public
            void run() {
                // If JavaFX/SWT is used, this is UNNECESSARY (and will break SWT/JavaFX shutdown)
                if (!GtkLoader.alreadyRunningGTK) {
                    Gtk2.g_main_loop_quit(mainloop);
                    // Gtk2.gtk_main_quit();
                }

                started = false;
            }
        });
    }
}
