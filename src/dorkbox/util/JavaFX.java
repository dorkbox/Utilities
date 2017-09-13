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
package dorkbox.util;


import java.lang.reflect.Method;

import org.slf4j.LoggerFactory;

/**
 * Utility methods for JavaFX.
 * <p>
 * We use reflection for these methods so that we can compile everything under Java 1.6 (which doesn't have JavaFX).
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
        boolean isJavaFxLoaded_ = false;
        boolean isJavaFxGtk3_ = false;

        try {
            // this is important to use reflection, because if JavaFX is not being used, calling getToolkit() will initialize it...
            java.lang.reflect.Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            m.setAccessible(true);
            ClassLoader cl = JavaFX.class.getClassLoader();

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
        } catch (Throwable e) {
            LoggerFactory.getLogger(JavaFX.class).debug("Error detecting if JavaFX is loaded", e);
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

    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
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
                return (Boolean) isEventThreadMethod.invoke(isEventThreadObject, (java.lang.Class<?>[])null);
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
