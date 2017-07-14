package dorkbox.util;

import java.awt.Component;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JOptionPane;

import org.slf4j.LoggerFactory;

/**
 * This class defines a mix of useful methods for different frameworks such as JavaFX or SWT. Swing, being that it is builtin, and always
 * available to the JRE (JavaFX can be excluded...) is in it's own class ({@link Swing}).
 */
public
class Framework {

    public final static boolean isJavaFxLoaded;
    public final static boolean isJavaFxGtk3;

    public final static boolean isSwtLoaded;
    public final static boolean isSwtGtk3;


    static {
        boolean isJavaFxLoaded_ = false;
        boolean isJavaFxGtk3_ = false;

        boolean isSwtLoaded_ = false;
        boolean isSwtGtk3_ = false;

        try {
            // this is important to use reflection, because if JavaFX is not being used, calling getToolkit() will initialize it...
            java.lang.reflect.Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            m.setAccessible(true);
            ClassLoader cl = ClassLoader.getSystemClassLoader();

            // JavaFX Java7,8 is GTK2 only. Java9 can have it be GTK3 if -Djdk.gtk.version=3 is specified
            // see http://mail.openjdk.java.net/pipermail/openjfx-dev/2016-May/019100.html
            isJavaFxLoaded_ = (null != m.invoke(cl, "com.sun.javafx.tk.Toolkit")) || (null != m.invoke(cl, "javafx.application.Application"));

            if (isJavaFxLoaded_) {
                // JavaFX Java7,8 is GTK2 only. Java9 can MAYBE have it be GTK3 if `-Djdk.gtk.version=3` is specified
                // see
                // http://mail.openjdk.java.net/pipermail/openjfx-dev/2016-May/019100.html
                // https://docs.oracle.com/javafx/2/system_requirements_2-2-3/jfxpub-system_requirements_2-2-3.htm
                // from the page: JavaFX 2.2.3 for Linux requires gtk2 2.18+.

                isJavaFxGtk3_ = OS.javaVersion >= 9 && System.getProperty("jdk.gtk.version", "2").equals("3");
            }

            // maybe we should load the SWT version? (In order for us to work with SWT, BOTH must be the same!!
            // SWT is GTK2, but if -DSWT_GTK3=1 is specified, it can be GTK3
            isSwtLoaded_ = null != m.invoke(cl, "org.eclipse.swt.widgets.Display");

            if (isSwtLoaded_) {
                // Necessary for us to work with SWT based on version info. We can try to set us to be compatible with whatever it is set to
                // System.setProperty("SWT_GTK3", "0");  (or -DSWT_GTK3=1)

                // was SWT forced?
                String swt_gtk3 = System.getProperty("SWT_GTK3");
                isSwtGtk3_ = swt_gtk3 != null && !swt_gtk3.equals("0");
                if (!isSwtGtk3_) {
                    // check a different property
                    String property = System.getProperty("org.eclipse.swt.internal.gtk.version");
                    isSwtGtk3_ = property != null && !property.startsWith("2.");
                }
            }
        } catch (Throwable e) {
            LoggerFactory.getLogger(Framework.class).debug("Error detecting if JavaFX/SWT is loaded", e);
        }

        isJavaFxLoaded = isJavaFxLoaded_;
        isJavaFxGtk3 = isJavaFxGtk3_;

        isSwtLoaded = isSwtLoaded_;
        isSwtGtk3 = isSwtGtk3_;
    }

    public static
    void initDispatch() {
        if (Framework.isJavaFxLoaded) {
            // This will initialize javaFX dispatch methods
            JavaFX.init();
        }
        else if (Framework.isSwtLoaded) {
            // This will initialize swt dispatch methods
            SwtBytecodeOverride.init(); // necessary to properly fix methods in {@link Swt}
            dorkbox.util.Swt.init();
        }
    }


    /**
     * Opens the given website in the default browser, or show a message saying that no default browser could be accessed.
     *
     * @param parent The parent of the error message, if raised
     * @param uri The website uri
     */
    public static
    void browse(final Component parent, final String uri) {
        boolean cannotBrowse = false;
        if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                                                   .isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop()
                       .browse(new URI(uri));
            } catch (URISyntaxException ignored) {
            } catch (IOException ex) {
                cannotBrowse = true;
            }
        }
        else {
            cannotBrowse = true;
        }

        if (cannotBrowse) {
            JOptionPane.showMessageDialog(parent, "It seems that I can't open a website using your default browser, sorry.");
        }
    }
}
