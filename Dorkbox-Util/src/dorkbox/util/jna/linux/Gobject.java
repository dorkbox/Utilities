package dorkbox.util.jna.linux;


import java.util.Arrays;
import java.util.List;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import dorkbox.util.Keep;
import dorkbox.util.jna.linux.Gtk.GdkEventButton;

public interface Gobject extends Library {
    public static final Gobject INSTANCE = (Gobject) Native.loadLibrary("gobject-2.0", Gobject.class);

    @Keep
    public class GTypeClassStruct extends Structure {
        public class ByValue extends GTypeClassStruct implements Structure.ByValue {}
        public class ByReference extends GTypeClassStruct implements Structure.ByReference {}

        public NativeLong g_type;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("g_type");
        }
    }

    @Keep
    public class GTypeInstanceStruct extends Structure {
        public class ByValue extends GTypeInstanceStruct implements Structure.ByValue {}
        public class ByReference extends GTypeInstanceStruct implements Structure.ByReference {}

        public Pointer g_class;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("g_class");
        }
    }

    @Keep
    public class GObjectStruct extends Structure {
        public class ByValue extends GObjectStruct implements Structure.ByValue {}
        public class ByReference extends GObjectStruct implements Structure.ByReference {}

        public GTypeInstanceStruct g_type_instance;
        public int ref_count;
        public Pointer qdata;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("g_type_instance",
                                 "ref_count",
                                 "qdata");
        }
    }

    @Keep
    public class GObjectClassStruct extends Structure {
        public class ByValue extends GObjectClassStruct implements Structure.ByValue {}
        public class ByReference extends GObjectClassStruct implements Structure.ByReference {}

        public GTypeClassStruct g_type_class;
        public Pointer construct_properties;
        public Pointer constructor;
        public Pointer set_property;
        public Pointer get_property;
        public Pointer dispose;
        public Pointer finalize;
        public Pointer dispatch_properties_changed;
        public Pointer notify;
        public Pointer constructed;
        public NativeLong flags;
        public Pointer dummy1;
        public Pointer dummy2;
        public Pointer dummy3;
        public Pointer dummy4;
        public Pointer dummy5;
        public Pointer dummy6;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("g_type_class",
                                 "construct_properties",
                                 "constructor",
                                 "set_property",
                                 "get_property",
                                 "dispose",
                                 "finalize",
                                 "dispatch_properties_changed",
                                 "notify",
                                 "constructed",
                                 "flags",
                                 "dummy1",
                                 "dummy2",
                                 "dummy3",
                                 "dummy4",
                                 "dummy5",
                                 "dummy6");
        }
    }

    @Keep
    public interface GCallback extends Callback {
        public void callback(Pointer instance, Pointer data);
    }

    @Keep
    public interface GEventCallback extends Callback {
        public void callback(Pointer instance, GdkEventButton event);
    }


    @Keep
    public class xyPointer extends Structure {
        public int value;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("value");
        }
    }

    @Keep
    public interface GPositionCallback extends Callback {
        public void callback(Pointer menu, xyPointer x, xyPointer y, Pointer push_in_bool, Pointer user_data);
    }


    public void g_object_unref(Pointer object);
    public void g_signal_connect_data(Pointer instance, String detailed_signal, Callback c_handler,
                                      Pointer data, Pointer destroy_data, int connect_flags);
}
