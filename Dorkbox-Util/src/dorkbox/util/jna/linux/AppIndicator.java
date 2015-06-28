package dorkbox.util.jna.linux;

import com.sun.jna.*;
import dorkbox.util.Keep;
import dorkbox.util.jna.linux.Gobject.GObjectClassStruct;
import dorkbox.util.jna.linux.Gobject.GObjectStruct;

import java.util.Arrays;
import java.util.List;

/* bindings for libappindicator 0.1 */
public
interface AppIndicator extends Library {
    AppIndicator INSTANCE = (AppIndicator) Native.loadLibrary("appindicator", AppIndicator.class);

    int CATEGORY_APPLICATION_STATUS = 0;
    int CATEGORY_COMMUNICATIONS = 1;
    int CATEGORY_SYSTEM_SERVICES = 2;
    int CATEGORY_HARDWARE = 3;
    int CATEGORY_OTHER = 4;

    int STATUS_PASSIVE = 0;
    int STATUS_ACTIVE = 1;
    int STATUS_ATTENTION = 2;


    @Keep
    interface Fallback extends Callback {
        Pointer callback(AppIndicatorInstanceStruct self);
    }


    @Keep
    interface Unfallback extends Callback {
        void callback(AppIndicatorInstanceStruct self, Pointer status_icon);
    }


    @Keep
    class AppIndicatorClassStruct extends Structure {
        public
        class ByReference extends AppIndicatorClassStruct implements Structure.ByReference {}


        public GObjectClassStruct parent_class;

        public Pointer new_icon;
        public Pointer new_attention_icon;
        public Pointer new_status;
        public Pointer new_icon_theme;
        public Pointer new_label;
        public Pointer connection_changed;
        public Pointer scroll_event;
        public Pointer app_indicator_reserved_ats;
        public Fallback fallback;
        public Pointer unfallback;
        public Pointer app_indicator_reserved_1;
        public Pointer app_indicator_reserved_2;
        public Pointer app_indicator_reserved_3;
        public Pointer app_indicator_reserved_4;
        public Pointer app_indicator_reserved_5;
        public Pointer app_indicator_reserved_6;

        public
        AppIndicatorClassStruct() {
        }

        public
        AppIndicatorClassStruct(Pointer p) {
            super(p);
            useMemory(p);
            read();
        }

        @Override
        protected
        List<String> getFieldOrder() {
            return Arrays.asList("parent_class", "new_icon", "new_attention_icon", "new_status", "new_icon_theme", "new_label",
                                 "connection_changed", "scroll_event", "app_indicator_reserved_ats", "fallback", "unfallback",
                                 "app_indicator_reserved_1", "app_indicator_reserved_2", "app_indicator_reserved_3",
                                 "app_indicator_reserved_4", "app_indicator_reserved_5", "app_indicator_reserved_6");
        }
    }


    @Keep
    class AppIndicatorInstanceStruct extends Structure {
        public GObjectStruct parent;
        public Pointer priv;

        @Override
        protected
        List<String> getFieldOrder() {
            return Arrays.asList("parent", "priv");
        }
    }


    AppIndicatorInstanceStruct app_indicator_new(String id, String icon_name, int category);

    AppIndicatorInstanceStruct app_indicator_new_with_path(String id, String icon_name, int category, String icon_theme_path);

    void app_indicator_set_status(AppIndicatorInstanceStruct self, int status);

    void app_indicator_set_attention_icon(AppIndicatorInstanceStruct self, String icon_name);

    void app_indicator_set_attention_icon_full(AppIndicatorInstanceStruct self, String name, String icon_desc);

    void app_indicator_set_menu(AppIndicatorInstanceStruct self, Pointer menu);

    void app_indicator_set_icon(AppIndicatorInstanceStruct self, String icon_name);

    void app_indicator_set_icon_full(AppIndicatorInstanceStruct self, String icon_name, String icon_desc);

    void app_indicator_set_label(AppIndicatorInstanceStruct self, String label, String guide);

    void app_indicator_set_icon_theme_path(AppIndicatorInstanceStruct self, String icon_theme_path);

    void app_indicator_set_ordering_index(AppIndicatorInstanceStruct self, int ordering_index);

    void app_indicator_set_secondary_active_target(AppIndicatorInstanceStruct self, Pointer menuitem);

    String app_indicator_get_id(AppIndicatorInstanceStruct self);

    int app_indicator_get_category(AppIndicatorInstanceStruct self);

    int app_indicator_get_status(AppIndicatorInstanceStruct self);

    String app_indicator_get_icon(AppIndicatorInstanceStruct self);

    String app_indicator_get_icon_desc(AppIndicatorInstanceStruct self);

    String app_indicator_get_icon_theme_path(AppIndicatorInstanceStruct self);

    String app_indicator_get_attention_icon(AppIndicatorInstanceStruct self);

    Pointer app_indicator_get_menu(AppIndicatorInstanceStruct self);

    String app_indicator_get_label(AppIndicatorInstanceStruct self);

    String app_indicator_get_label_guide(AppIndicatorInstanceStruct self);

    int app_indicator_get_ordering_index(AppIndicatorInstanceStruct self);

    Pointer app_indicator_get_secondary_active_target(AppIndicatorInstanceStruct self);

    void app_indicator_build_menu_from_desktop(AppIndicatorInstanceStruct self, String desktop_file, String destkop_profile);
}
