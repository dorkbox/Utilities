package dorkbox.util.jna.linux;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public interface Gtk extends Library {
    public static final Gtk INSTANCE = (Gtk) Native.loadLibrary("gtk-x11-2.0", Gtk.class);

    public static final int FALSE = 0;
    public static final int TRUE = 1;


    public static class GdkEventButton extends Structure {
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

    public void gtk_init(int argc, String[] argv);

    /**
     * Runs the main loop until gtk_main_quit() is called.
     * You can nest calls to gtk_main(). In that case gtk_main_quit() will make the innermost invocation of the main loop return.
     */
    public void gtk_main();
    /**
     * Makes the innermost invocation of the main loop return when it regains control.
     */
    public void gtk_main_quit();

    public void gdk_threads_init();
    public void gdk_threads_enter();
    public void gdk_threads_leave();


    public Pointer gtk_menu_new();
    public Pointer gtk_menu_item_new_with_label(String label);

    public Pointer gtk_status_icon_new();
    public void gtk_status_icon_set_from_file(Pointer widget, String lablel);

    public void gtk_status_icon_set_visible(Pointer widget, boolean visible);
    public void gtk_status_icon_set_tooltip(Pointer widget, String tooltipText);

    public void gtk_menu_item_set_label(Pointer menu_item, String label);
    public void gtk_menu_shell_append(Pointer menu_shell, Pointer child);
    public void gtk_widget_set_sensitive(Pointer widget, int sesitive);

    public void gtk_widget_show(Pointer widget);
    public void gtk_widget_show_all(Pointer widget);
    public void gtk_widget_destroy(Pointer widget);
}

