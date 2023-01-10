module dorkbox.utilities {
    exports dorkbox.exit;
    exports dorkbox.jna;
    exports dorkbox.jna.rendering;
    exports dorkbox.jna.linux;
    exports dorkbox.jna.linux.structs;
    exports dorkbox.jna.macos;
    exports dorkbox.jna.macos.cocoa;
    exports dorkbox.jna.macos.foundation;
    exports dorkbox.jna.windows;
    exports dorkbox.jna.windows.structs;
    exports dorkbox.urlHandler;
    exports dorkbox.util;
    exports dorkbox.util.classes;
    exports dorkbox.util.crypto;
    exports dorkbox.util.crypto.signers;
    exports dorkbox.util.entropy;
    exports dorkbox.util.exceptions;
    exports dorkbox.util.gwt;
    exports dorkbox.util.properties;
    exports dorkbox.util.userManagement;

    requires transitive dorkbox.executor;
    requires transitive dorkbox.updates;
    requires transitive dorkbox.os;

    requires transitive kotlin.stdlib;
    requires kotlinx.coroutines.core.jvm;

    requires static com.sun.jna;
    requires static com.sun.jna.platform;

    requires static org.slf4j;
    requires static java.desktop;
}
