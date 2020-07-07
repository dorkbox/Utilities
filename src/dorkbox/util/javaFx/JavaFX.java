/*
 * Copyright 2016 dorkbox, llc
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
package dorkbox.util.javaFx;


import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.slf4j.LoggerFactory;

import dorkbox.util.OS;
import dorkbox.util.swt.Swt;

/**
 * Utility methods for JavaFX.
 * <p>
 * We use reflection for these methods so that we can compile everything under a version of Java that might now have JavaFX.
 */
public
class JavaFX {
    public final static boolean isLoaded;
    public final static boolean isGtk3;


    // Methods are cached for performance
    private static final Method dispatchMethod;
    private static final Method isEventThreadMethod;
    private static final Object isEventThreadObject;


    static {
        // There is a silly amount of redirection, simply because we have to be able to access JavaFX, but only if it's in use.
        // Since this class is the place other code interacts with, we can use JavaFX stuff if necessary without loading/linking
        // the JavaFX classes by accident

        // We cannot use getToolkit(), because if JavaFX is not being used, calling getToolkit() will initialize it...
        // see: https://bugs.openjdk.java.net/browse/JDK-8090933


        boolean isJavaFxLoaded_ = false;
        try {
            // this is important to use reflection, because if JavaFX is not being used, calling getToolkit() will initialize it...
            java.lang.reflect.Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            m.setAccessible(true);
            ClassLoader cl = ClassLoader.getSystemClassLoader();

            // JavaFX Java7,8 is GTK2 only. Java9 can have it be GTK3 if -Djdk.gtk.version=3 is specified
            // see http://mail.openjdk.java.net/pipermail/openjfx-dev/2016-May/019100.html
            isJavaFxLoaded_ = (null != m.invoke(cl, "com.sun.javafx.tk.Toolkit")) || (null != m.invoke(cl, "javafx.application.Application"));
        } catch (Throwable e) {
            LoggerFactory.getLogger(JavaFX.class).debug("Error detecting if JavaFX is loaded", e);
        }

        boolean isJavaFxGtk3_ = false;
        if (isJavaFxLoaded_) {
            // JavaFX Java7,8 is GTK2 only. Java9 can MAYBE have it be GTK3 if `-Djdk.gtk.version=3` is specified
            // see
            // http://mail.openjdk.java.net/pipermail/openjfx-dev/2016-May/019100.html
            // https://docs.oracle.com/javafx/2/system_requirements_2-2-3/jfxpub-system_requirements_2-2-3.htm
            // from the page: JavaFX 2.2.3 for Linux requires gtk2 2.18+.


            if (OS.javaVersion >= 9) {
                // HILARIOUSLY enough, you can use JavaFX + SWT..... And the javaFX GTK version info SHOULD
                // be based on what SWT has loaded

                // https://github.com/teamfx/openjfx-9-dev-rt/blob/master/modules/javafx.graphics/src/main/java/com/sun/glass/ui/gtk/GtkApplication.java

                if (Swt.isLoaded && !Swt.isGtk3) {
                    isJavaFxGtk3_ = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                        @Override
                        public
                        Boolean run() {
                            String version = System.getProperty("jdk.gtk.version", "2");
                            return "3".equals(version) || version.startsWith("3.");
                        }
                    });
                }
            }
        }


        isLoaded = isJavaFxLoaded_;
        isGtk3 = isJavaFxGtk3_;


        Method _isEventThreadMethod = null;
        Method _dispatchMethod = null;
        Object _isEventThreadObject = null;

        if (isJavaFxLoaded_) {
            try {
                Class<?> clazz = Class.forName("javafx.application.Platform");
                _dispatchMethod = clazz.getMethod("runLater", Runnable.class);

                // JAVA 7
                // javafx.application.Platform.isFxApplicationThread();

                // JAVA 8
                // com.sun.javafx.tk.Toolkit.getToolkit().isFxUserThread();
                if (OS.javaVersion <= 7) {
                    clazz = Class.forName("javafx.application.Platform");
                    _isEventThreadMethod = clazz.getMethod("isFxApplicationThread");
                    _isEventThreadObject = null;
                } else {
                    clazz = Class.forName("com.sun.javafx.tk.Toolkit");
                    _isEventThreadMethod = clazz.getMethod("getToolkit");

                    _isEventThreadObject = _isEventThreadMethod.invoke(null);
                    _isEventThreadMethod = _isEventThreadObject.getClass()
                                                               .getMethod("isFxUserThread", (java.lang.Class<?>[])null);
                }
            } catch (Throwable e) {
                LoggerFactory.getLogger(JavaFX.class).error("Cannot initialize JavaFX", e);
            }
        }

        dispatchMethod = _dispatchMethod;
        isEventThreadMethod = _isEventThreadMethod;
        isEventThreadObject = _isEventThreadObject;
    }

    public static
    void dispatch(final Runnable runnable) {
        //         javafx.application.Platform.runLater(runnable);

        try {
            dispatchMethod.invoke(null, runnable);
        } catch (Throwable e) {
            LoggerFactory.getLogger(JavaFX.class)
                         .error("Unable to execute JavaFX runLater(). Please create an issue with your OS and Java " +
                                "version so we may further investigate this issue.");
        }
    }

    public static
    boolean isEventThread() {
        // JAVA 7
        // javafx.application.Platform.isFxApplicationThread();

        // JAVA 8
        // com.sun.javafx.tk.Toolkit.getToolkit().isFxUserThread();

        try {
            if (OS.javaVersion <= 7) {
                return (Boolean) isEventThreadMethod.invoke(null);
            } else {
                Class<?>[] args = null;
                //noinspection ConstantConditions
                return (Boolean) isEventThreadMethod.invoke(isEventThreadObject, (Object) args);
            }
        } catch (Throwable e) {
            LoggerFactory.getLogger(JavaFX.class)
                         .error("Unable to check if JavaFX is in the event thread. Please create an issue with your OS and Java " +
                                "version so we may further investigate this issue.");
        }

        return false;
    }

    public static
    void onShutdown(final Runnable runnable) {
        // com.sun.javafx.tk.Toolkit.getToolkit()
        //                          .addShutdownHook(runnable);

        try {
            Class<?> clazz = Class.forName("com.sun.javafx.tk.Toolkit");
            Method method = clazz.getMethod("getToolkit");
            Object o = method.invoke(null);
            Method m = o.getClass()
                        .getMethod("addShutdownHook", Runnable.class);
            m.invoke(o, runnable);
        } catch (Throwable e) {
            LoggerFactory.getLogger(JavaFX.class)
                         .error("Unable to insert shutdown hook into JavaFX. Please create an issue with your OS and Java " +
                                "version so we may further investigate this issue.");
        }
    }
}
