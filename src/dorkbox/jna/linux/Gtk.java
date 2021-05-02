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

import com.sun.jna.Function;
import com.sun.jna.Pointer;

import dorkbox.jna.linux.structs.GtkStyle;

/**
 * Bindings for GTK+ 2. Bindings that are exclusively for GTK+ 3 are in that respective class
 * <p>
 * Direct-mapping, See: https://github.com/java-native-access/jna/blob/master/www/DirectMapping.md
 */
@SuppressWarnings({"Duplicates", "SameParameterValue", "DeprecatedIsStillUsed", "WeakerAccess", "UnusedReturnValue"})
public
interface Gtk {
    // objdump -T /usr/lib/x86_64-linux-gnu/libgtk-x11-2.0.so.0 | grep gtk
    // objdump -T /usr/lib/x86_64-linux-gnu/libgtk-3.so.0 | grep gtk
    // objdump -T /usr/local/lib/libgtk-3.so.0 | grep gtk

    // For funsies to look at, SyncThing did a LOT of work on compatibility in python (unfortunate for us, but interesting).
    // https://github.com/syncthing/syncthing-gtk/blob/b7a3bc00e3bb6d62365ae62b5395370f3dcc7f55/syncthing_gtk/statusicon.py

    int FALSE = 0;
    int TRUE = 1;

    // use GtkCheck for a safe accessor of these
    int MAJOR = GtkLoader.MAJOR;
    int MINOR = GtkLoader.MINOR;
    int MICRO = GtkLoader.MICRO;

    // make specific versions of GTK2 vs GTK3 APIs
    // ALSO, GTK must be loaded via .init()
    Gtk Gtk2 = GtkLoader.isGtk2 ? new Gtk2() : new Gtk3();
    dorkbox.jna.linux.Gtk3 Gtk3 = GtkLoader.isGtk2 ? null : (Gtk3) Gtk2;

    // use GtkCheck for a safe accessor of these
    boolean isGtk2 = GtkLoader.isGtk2;
    boolean isGtk3 = GtkLoader.isGtk3;
    boolean isLoaded = GtkLoader.isLoaded;

    boolean alreadyRunningGTK = GtkLoader.alreadyRunningGTK;

    Function gtk_status_icon_position_menu = GtkLoader.gtk_status_icon_position_menu;

    /**
     * This would NORMALLY have a 2nd argument that is a String[] -- however JNA direct-mapping DOES NOT support this. We are lucky
     * enough that we just pass 'null' as the second argument, therefore, we don't have to define that parameter here.
     *
     * This does the same thing as gtk_init(), but without a hard quit
     */
    boolean gtk_init_check(int argc);

    /**
     * Creates a new GMainLoop structure.
     */
    GMainLoop g_main_loop_new(Pointer context, boolean is_running);

    /**
     * Runs a main loop until g_main_loop_quit() is called on the loop. If this is called for the thread of the loop's GMainContext,
     * it will process events from the loop, otherwise it will simply wait.
     */
    void g_main_loop_run(GMainLoop loop);

    /**
     * Stops a GMainLoop from running. Any calls to g_main_loop_run() for the loop will return.
     * Note that sources that have already been dispatched when g_main_loop_quit() is called will still be executed.
     */
    void g_main_loop_quit(GMainLoop loop);

    /**
     * Returns the GMainContext of loop .
     */
    GMainContext g_main_loop_get_context(GMainLoop loop);

    /**
     * Invokes a function in such a way that context is owned during the invocation of function .
     */
    void g_main_context_invoke(GMainContext c, FuncCallback func, Pointer data);

    /**
     * Creates a new GtkMenu
     */
    Pointer gtk_menu_new();

    /**
     * Sets or replaces the menu item’s submenu, or removes it when a NULL submenu is passed.
     */
    void gtk_menu_item_set_submenu(Pointer menuEntry, Pointer menu);

    /**
     * Creates a new GtkSeparatorMenuItem.
     */
    Pointer gtk_separator_menu_item_new();

    /**
     * Creates a new GtkImage displaying the file filename . If the file isn’t found or can’t be loaded, the resulting GtkImage will
     * display a "broken image" icon. This function never returns NULL, it always returns a valid GtkImage widget.
     * <p>
     * If the file contains an animation, the image will contain an animation.
     */
    Pointer gtk_image_new_from_file(String iconPath);

    /**
     * Sets the active state of the menu item’s check box.
     */
    void gtk_check_menu_item_set_active(Pointer check_menu_item, boolean isChecked);

    /**
     * Creates a new GtkImageMenuItem containing a label. The label will be created using gtk_label_new_with_mnemonic(), so underscores
     * in label indicate the mnemonic for the menu item.
     * <p>
     * uses '_' to define which key is the mnemonic
     * <p>
     * gtk_image_menu_item_new_with_mnemonic has been deprecated since version 3.10 and should not be used in newly-written code.
     * NOTE: Use gtk_menu_item_new_with_mnemonic() instead.
     */
    Pointer gtk_image_menu_item_new_with_mnemonic(String label);

    Pointer gtk_check_menu_item_new_with_mnemonic(String label);

    /**
     * Sets the image of image_menu_item to the given widget. Note that it depends on the show-menu-images setting whether the image
     * will be displayed or not.
     * <p>
     * gtk_image_menu_item_set_image has been deprecated since version 3.10 and should not be used in newly-written code.
     */
    void gtk_image_menu_item_set_image(Pointer image_menu_item, Pointer image);

    /**
     * If TRUE, the menu item will ignore the "gtk-menu-images" setting and always show the image, if available.
     * Use this property if the menuitem would be useless or hard to use without the image
     * <p>
     * gtk_image_menu_item_set_always_show_image has been deprecated since version 3.10 and should not be used in newly-written code.
     */
    void gtk_image_menu_item_set_always_show_image(Pointer menu_item, boolean forceShow);

    /**
     * Creates an empty status icon object.
     * <p>
     * gtk_status_icon_new has been deprecated since version 3.14 and should not be used in newly-written code.
     * Use notifications
     */
    Pointer gtk_status_icon_new();

    /**
     * Obtains the root window (parent all other windows are inside) for the default display and screen.
     *
     * @return the default root window
     */
    Pointer gdk_get_default_root_window();

    /**
     * Gets the default screen for the default display. (See gdk_display_get_default()).
     *
     * @return a GdkScreen, or NULL if there is no default display.
     *
     * @since 2.2
     */
    Pointer gdk_screen_get_default();

    /**
     * Gets the resolution for font handling on the screen; see gdk_screen_set_resolution() for full details.
     *
     * IE:
     *
     * The resolution for font handling on the screen. This is a scale factor between points specified in a PangoFontDescription and
     * cairo units. The default value is 96, meaning that a 10 point font will be 13 units high. (10 * 96. / 72. = 13.3).
     *
     * @return the current resolution, or -1 if no resolution has been set.
     *
     * @since Since: 2.10
     */
    double gdk_screen_get_resolution(Pointer screen);

    /**
     * Makes status_icon display the file filename . See gtk_status_icon_new_from_file() for details.
     * <p>
     * gtk_status_icon_set_from_file has been deprecated since version 3.14 and should not be used in newly-written code.
     * Use notifications
     */
    void gtk_status_icon_set_from_file(Pointer widget, String label);

    /**
     * Shows or hides a status icon.
     * <p>
     * gtk_status_icon_set_visible has been deprecated since version 3.14 and should not be used in newly-written code.
     * Use notifications
     */
    void gtk_status_icon_set_visible(Pointer widget, boolean visible);


    /**
     * Sets text as the contents of the tooltip.
     * This function will take care of setting "has-tooltip" to TRUE and of the default handler for the "query-tooltip" signal.
     *
     * app indicators don't support this
     *
     * gtk_status_icon_set_tooltip_text has been deprecated since version 3.14 and should not be used in newly-written code.
     * Use notifications
     */
    void gtk_status_icon_set_tooltip_text(Pointer widget, String tooltipText);

    /**
     * Sets the title of this tray icon. This should be a short, human-readable, localized string describing the tray icon. It may be used
     * by tools like screen readers to render the tray icon.
     * <p>
     * gtk_status_icon_set_title has been deprecated since version 3.14 and should not be used in newly-written code.
     * Use notifications
     */
    void gtk_status_icon_set_title(Pointer widget, String titleText);

    /**
     * Sets the name of this tray icon. This should be a string identifying this icon. It is may be used for sorting the icons in the
     * tray and will not be shown to the user.
     * <p>
     * gtk_status_icon_set_name has been deprecated since version 3.14 and should not be used in newly-written code.
     * Use notifications
     */
    void gtk_status_icon_set_name(Pointer widget, String name);

    /**
     * Displays a menu and makes it available for selection.
     * <p>
     * gtk_menu_popup has been deprecated since version 3.22 and should not be used in newly-written code.
     * NOTE: Please use gtk_menu_popup_at_widget(), gtk_menu_popup_at_pointer(). or gtk_menu_popup_at_rect() instead
     */
    void gtk_menu_popup(Pointer menu, Pointer widget, Pointer bla, Function func, Pointer data, int button, int time);

    /**
     * Sets text on the menu_item label
     */
    void gtk_menu_item_set_label(Pointer menu_item, String label);

    /**
     * Adds a new GtkMenuItem to the end of the menu shell's item list.
     */
    void gtk_menu_shell_append(Pointer menu_shell, Pointer child);

    /**
     * Sets the sensitivity of a widget. A widget is sensitive if the user can interact with it. Insensitive widgets are "grayed out"
     * and the user can’t interact with them. Insensitive widgets are known as "inactive", "disabled", or "ghosted" in some other toolkits.
     */
    void gtk_widget_set_sensitive(Pointer widget, boolean sensitive);

    /**
     * Recursively shows a widget, and any child widgets (if the widget is a container)
     */
    void gtk_widget_show_all(Pointer widget);

    /**
     * Removes widget from container . widget must be inside container . Note that container will own a reference to widget , and that
     * this may be the last reference held; so removing a widget from its container can destroy that widget.
     * <p>
     * If you want to use widget again, you need to add a reference to it before removing it from a container, using g_object_ref().
     * If you don’t want to use widget again it’s usually more efficient to simply destroy it directly using gtk_widget_destroy()
     * since this will remove it from the container and help break any circular reference count cycles.
     */
    void gtk_container_remove(Pointer parentWidget, Pointer widget);

    /**
     * Destroys a widget.
     * When a widget is destroyed all references it holds on other objects will be released:
     * - if the widget is inside a container, it will be removed from its parent
     * - if the widget is a container, all its children will be destroyed, recursively
     * - if the widget is a top level, it will be removed from the list of top level widgets that GTK+ maintains internally
     * <p>
     * It's expected that all references held on the widget will also be released; you should connect to the "destroy" signal if you
     * hold a reference to widget and you wish to remove it when this function is called. It is not necessary to do so if you are
     * implementing a GtkContainer, as you'll be able to use the GtkContainerClass.remove() virtual function for that.
     * <p>
     * It's important to notice that gtk_widget_destroy() will only cause the widget to be finalized if no additional references,
     * acquired using g_object_ref(), are held on it. In case additional references are in place, the widget will be in an "inert" state
     * after calling this function; widget will still point to valid memory, allowing you to release the references you hold, but you
     * may not query the widget's own state.
     * <p>
     * NOTE You should typically call this function on top level widgets, and rarely on child widgets.
     */
    void gtk_widget_destroy(Pointer widget);

    /**
     * Gets the GtkSettings object for the screen, creating it if necessary.
     *
     * @since 2.2
     */
    Pointer gtk_settings_get_for_screen(Pointer screen);

    /**
     * Gets the GtkSettings object for the default GDK screen, creating it if necessary.
     *
     * @since 2.2
     */
    Pointer gtk_settings_get_default();

    /**
     * Finds all matching RC styles for a given widget, composites them together, and then creates a GtkStyle representing the composite
     * appearance. (GTK+ actually keeps a cache of previously created styles, so a new style may not be created.)
     */
    GtkStyle gtk_rc_get_style(Pointer widget);

    /**
     * Adds widget to container . Typically used for simple containers such as GtkWindow, GtkFrame, or GtkButton; for more complicated
     * layout containers such as GtkBox or GtkTable, this function will pick default packing parameters that may not be correct. So
     * consider functions such as gtk_box_pack_start() and gtk_table_attach() as an alternative to gtk_container_add() in those cases.
     * A widget may be added to only one container at a time; you can't place the same widget inside two different containers.
     */
    void gtk_container_add(Pointer offscreen, Pointer widget);

    /**
     * Get's the child from a GTK Bin object
     */
    Pointer gtk_bin_get_child(Pointer bin);

    /**
     * Gets the PangoLayout used to display the label. The layout is useful to e.g. convert text positions to pixel positions, in
     * combination with gtk_label_get_layout_offsets(). The returned layout is owned by the label so need not be freed by the caller.
     *
     * The label is free to recreate its layout at any time, so it should be considered read-only.
     */
    Pointer gtk_label_get_layout(Pointer label);

    /**
     * Computes the logical and ink extents of layout in device units. This function just calls pango_layout_get_extents() followed
     * by two pango_extents_to_pixels() calls, rounding ink_rect and logical_rect such that the rounded rectangles fully contain the
     * unrounded one (that is, passes them as first argument to pango_extents_to_pixels()).
     *
     * @param layout a PangoLayout
     * @param ink_rect rectangle used to store the extents of the layout as drawn or NULL to indicate that the result is not needed.
     * @param logical_rect rectangle used to store the logical extents of the layout or NULL to indicate that the result is not needed.
     */
    void pango_layout_get_pixel_extents(Pointer layout, Pointer ink_rect, Pointer logical_rect);

    /**
     * Creates the GDK (windowing system) resources associated with a widget. For example, widget->window will be created when a widget
     * is realized. Normally realization happens implicitly; if you show a widget and all its parent containers, then the widget will
     * be realized and mapped automatically.
     *
     * Realizing a widget requires all the widget’s parent widgets to be realized; calling gtk_widget_realize() realizes the widget’s
     * parents in addition to widget itself. If a widget is not yet inside a toplevel window when you realize it, bad things will happen.
     *
     * This function is primarily used in widget implementations, and isn’t very useful otherwise. Many times when you think you might
     * need it, a better approach is to connect to a signal that will be called after the widget is realized automatically, such as
     * "draw". Or simply g_signal_connect() to the "realize" signal.
     */
    void gtk_widget_realize(Pointer widget);

    /**
     * Creates a toplevel container widget that is used to retrieve snapshots of widgets without showing them on the screen.
     *
     * @since 2.20
     */
    Pointer	gtk_offscreen_window_new();

    /**
     * This function is typically used when implementing a GtkContainer subclass. Obtains the preferred size of a widget. The
     * container uses this information to arrange its child widgets and decide what size allocations to give them with
     * gtk_widget_size_allocate().
     *
     * You can also call this function from an application, with some caveats. Most notably, getting a size request requires the
     * widget to be associated with a screen, because font information may be needed. Multihead-aware applications should keep this in mind.
     *
     * Also remember that the size request is not necessarily the size a widget will actually be allocated.
     */
    void gtk_widget_size_request(final Pointer widget, final Pointer requisition);

    /**
     * Creates a new GtkImageMenuItem containing the image and text from a stock item. Some stock ids have preprocessor macros
     * like GTK_STOCK_OK and GTK_STOCK_APPLY.
     *
     * @param stock_id the name of the stock item.
     * @param accel_group the GtkAccelGroup to add the menu items accelerator to, or NULL.
     *
     * @return a new GtkImageMenuItem.
     */
    Pointer gtk_image_menu_item_new_from_stock(String stock_id, Pointer accel_group);

    /**
     * A convenience function for launching the default application to show the uri. Like gtk_show_uri_on_window(), but takes a screen
     * as transient parent instead of a window.
     *
     * @param timestamp GDK_CURRENT_TIME = 0 (this is what you should use)
     * @since 2.14
     */
    @Deprecated
    boolean gtk_show_uri(Pointer screen, String uri, int timestamp, Pointer error);

    /**
     * Sets text as the contents of the tooltip. This function will take care of setting "has-tooltip" to TRUE and of the default
     * handler for the "query-tooltip" signal. Null text will remove the tooltip
     *
     * @since 2.12
     */
    void gtk_widget_set_tooltip_text(Pointer widget, String text);

    /**
     * Gets the default GdkDisplay. This is a convenience function for gdk_display_manager_get_default_display (gdk_display_manager_get()).
     *
     * @since: 2.2
     */
    Pointer gdk_display_get_default();
}

