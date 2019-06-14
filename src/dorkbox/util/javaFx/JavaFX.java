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


import java.security.AccessController;
import java.security.PrivilegedAction;

import dorkbox.util.ClassLoaderUtil;
import dorkbox.util.OS;
import dorkbox.util.swt.Swt;

/**
 * Utility methods for JavaFX.
 */
public
class JavaFX {
    public final static boolean isLoaded;
    public final static boolean isGtk3;


    static {
        // There is a silly amount of redirection, simply because we have to be able to access JavaFX, but only if it's in use.
        // Since this class is the place other code interacts with, we can use JavaFX stuff if necessary without loading/linking
        // the JavaFX classes by accident

        // We cannot use getToolkit(), because if JavaFX is not being used, calling getToolkit() will initialize it...
        // see: https://bugs.openjdk.java.net/browse/JDK-8090933


        boolean isJavaFxLoaded_ = ClassLoaderUtil.isClassLoaded(ClassLoader.getSystemClassLoader(), "javafx.application.Platform");
        if (!isJavaFxLoaded_) {
            // check both classloaders
            isJavaFxLoaded_ = ClassLoaderUtil.isClassLoaded(Thread.currentThread().getContextClassLoader(), "javafx.application.Platform");
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
    }

    public static
    void dispatch(final Runnable runnable) {
        JavaFxDispatch.dispatch(runnable);
    }

    public static
    boolean isEventThread() {
        return JavaFxDispatch.isEventThread();
    }

    public static
    void onShutdown(final Runnable runnable) {
        JavaFxDispatch.onShutdown(runnable);
    }
}
