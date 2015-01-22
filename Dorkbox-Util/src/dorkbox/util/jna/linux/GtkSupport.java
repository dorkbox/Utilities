package dorkbox.util.jna.linux;

import java.util.concurrent.CountDownLatch;


public class GtkSupport {
    public static final boolean isSupported;

    static {
        if (Gtk.INSTANCE != null && AppIndicator.INSTANCE != null && Gobject.INSTANCE != null && GThread.INSTANCE != null) {
            isSupported = true;

            boolean hasSwt = false;
            try {
                Class<?> swtClass = Class.forName("org.eclipse.swt.widgets.Display");
                if (swtClass != null) {
                    hasSwt = true;
                }
            } catch (Exception ignore) {}

            // swt already init's gtk. If we are using GTK, we need to make sure the event loop is runnign
            if (!hasSwt) {
                Gtk instance = Gtk.INSTANCE;
                instance.gtk_init(0, null);
                GThread.INSTANCE.g_thread_init(null);
                instance.gdk_threads_init();

                final CountDownLatch blockUntilStarted = new CountDownLatch(1);

                Thread gtkUpdateThread = new Thread() {
                    @Override
                    public void run() {
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
        } else {
            isSupported = false;
        }
    }

    public static void init() {
        // placeholder to init GTK
    }
}
