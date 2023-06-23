module dorkbox.utilities {
    exports dorkbox.exit;
    exports dorkbox.urlHandler;
    exports dorkbox.util;
    exports dorkbox.util.classes;
    exports dorkbox.util.crypto;
    exports dorkbox.util.crypto.signers;
    exports dorkbox.util.entropy;
    exports dorkbox.util.exceptions;
    exports dorkbox.util.gwt;
    exports dorkbox.util.properties;
    exports dorkbox.util.sync;
    exports dorkbox.util.userManagement;

    requires transitive dorkbox.executor;
    requires transitive dorkbox.updates;
    requires transitive dorkbox.os;

    requires transitive kotlin.stdlib;
    requires transitive kotlinx.coroutines.core;

    requires static org.slf4j;
}
