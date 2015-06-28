package dorkbox.util.jna.linux;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public interface Gtk extends Library {
    Gtk INSTANCE = (Gtk) Native.loadLibrary("gtk-x11-2.0", Gtk.class);

    int FALSE = 0;
    int TRUE = 1;


    class GdkEventButton extends Structure {
        public int type;
        public Pointer window;
        public int send_event;
        public int time;
        public double x;
        public double y;
        public Pointer axes;
        public int state;
        public int button;
        public Pointer device;
        public double x_root;
        public double y_root;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("type",
                                 "window",
                                 "send_event",
                                 "time",
                                 "x",
                                 "y",
                                 "axes",
                                 "state",
                                 "button",
                                 "device",
                                 "x_root",
                                 "y_root");
        }
    }

    void gtk_init(int argc, String[] argv);

    /**
     * Runs the main loop until gtk_main_quit() is called.
     * You can nest calls to gtk_main(). In that case gtk_main_quit() will make the innermost invocation of the main loop return.
     */
    void gtk_main();
    /**
     * Makes the innermost invocation of the main loop return when it regains control. ONLY CALL FROM THE GtkSupport class, UNLESS
     * you know what you're doing!
     */
    void gtk_main_quit();

    void gdk_threads_init();
    void gdk_threads_enter();
    void gdk_threads_leave();


    Pointer gtk_menu_new();
    Pointer gtk_menu_item_new_with_label(String label);

    Pointer gtk_status_icon_new();
    void gtk_status_icon_set_from_file(Pointer widget, String lablel);

    void gtk_status_icon_set_visible(Pointer widget, boolean visible);
    void gtk_status_icon_set_tooltip(Pointer widget, String tooltipText);

    void gtk_menu_item_set_label(Pointer menu_item, String label);
    void gtk_menu_shell_append(Pointer menu_shell, Pointer child);
    void gtk_widget_set_sensitive(Pointer widget, int sesitive);

    void gtk_widget_show(Pointer widget);
    void gtk_widget_show_all(Pointer widget);
    void gtk_widget_destroy(Pointer widget);
}

