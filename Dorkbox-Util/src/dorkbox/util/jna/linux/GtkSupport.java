package dorkbox.util.jna.linux;



public class GtkSupport {
    public static final boolean isSupported;
    public static final boolean usesSwtMainLoop;

    static {
        if (Gtk.INSTANCE != null && AppIndicator.INSTANCE != null && Gobject.INSTANCE != null) {
            isSupported = true;

            boolean hasSwt = false;
            try {
                Class<?> swtClass = Class.forName("org.eclipse.swt.widgets.Display");
                if (swtClass != null) {
                    hasSwt = true;
                }
            } catch (Exception ignore) {}

            // swt already init's gtk.
            if (!hasSwt) {
                Gtk.INSTANCE.gtk_init(0, null);
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
