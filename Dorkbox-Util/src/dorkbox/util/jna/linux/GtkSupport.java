package dorkbox.util.jna.linux;



public class GtkSupport {
    public static final boolean isSupported;
    public static final boolean usesSwtMainLoop;

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

            // swt already init's gtk. Maybe this is the wrong way to go about this?
            if (!hasSwt) {
                Gtk instance = Gtk.INSTANCE;
                instance.gtk_init(0, null);
                GThread.INSTANCE.g_thread_init(null);
                instance.gdk_threads_init();

                usesSwtMainLoop = false;
            } else {
                usesSwtMainLoop = true;
            }
        } else {
            isSupported = false;
            usesSwtMainLoop = false;
        }
    }

    public static void init() {
        // placeholder to init GTK
    }
}
