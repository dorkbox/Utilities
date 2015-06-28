package dorkbox.util.jna.linux;

import java.util.concurrent.CountDownLatch;


public
class GtkSupport {
    public static final boolean isSupported;
    private static boolean hasSwt = false;

    static {
        boolean hasSupport = false;
        try {
            if (Gtk.INSTANCE != null && Gobject.INSTANCE != null && GThread.INSTANCE != null) {
                hasSupport = true;

                try {
                    Class<?> swtClass = Class.forName("org.eclipse.swt.widgets.Display");
                    if (swtClass != null) {
                        hasSwt = true;
                    }
                } catch (Throwable ignore) {
                }

                // If we are using GTK, we need to make sure the event loop is running. There can be multiple/nested loops.
                // since SWT uses one already, it's not necessary to have two.
                if (!hasSwt) {
                    Gtk instance = Gtk.INSTANCE;
                    instance.gtk_init(0, null);
                    GThread.INSTANCE.g_thread_init(null);
                    instance.gdk_threads_init();

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
        } catch (Throwable ignored) {
        }

        isSupported = hasSupport;
    }

    public static
    void init() {
        // placeholder to init GTK
    }

    public static
    void shutdownGTK() {
        if (isSupported && !hasSwt) {
            Gtk.INSTANCE.gtk_main_quit();
        }
    }
}
