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
package dorkbox.util.swt;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import dorkbox.os.OS;

/**
 * Utility methods for SWT. SWT is always available for compiling, so it is not necessary to use reflection to compile it.
 * <p>
 * SWT system tray types are GtkStatusIcon trays (so we don't want to use them)
 */
public
class Swt {
    public final static boolean isLoaded;
    public final static boolean isGtk3;
    private static final int version;

    private static final Display currentDisplay;
    private static final Thread currentDisplayThread;

    static {
        // There is a silly amount of redirection, simply because we have to be able to access SWT, but only if it's in use.
        // Since this class is the place other code interacts with, we can use SWT stuff if necessary without loading/linking
        // the SWT classes by accident

        boolean isSwtLoadable_ = isLoadable();

        version = SWT.getVersion();
        isLoaded = isSwtLoadable_;
        isGtk3 = isSwtLoadable_ && isGtk3();


        // we MUST save this on init, otherwise it is "null" when methods are run from the swing EDT.
        currentDisplay = org.eclipse.swt.widgets.Display.getCurrent();
        currentDisplayThread = currentDisplay.getThread();
    }

    /**
     * This uses reflection to check if SWT is loadable and if certain classes are available.
     * @return true if SWT is loadable
     */
    static boolean isLoadable() {
        Class<?> swtErrorClass = AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {
            @Override
            public
            Class<?> run() {
                try {
                    return Class.forName("org.eclipse.swt.SWTError", true, ClassLoader.getSystemClassLoader());
                } catch (Exception ignored) {
                }
                try {
                    return Class.forName("org.eclipse.swt.SWTError", true, Thread.currentThread().getContextClassLoader());
                } catch (Exception ignored) {
                }
                return null;
            }
        });

        if (null != swtErrorClass) {
            try {
                return org.eclipse.swt.SWT.isLoadable();
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    /**
     * This is only necessary for linux.
     *
     * @return true if SWT is GTK3. False if SWT is GTK2. If for some reason we DO NOT KNOW, then we return false (GTK2).
     */
    static boolean isGtk3() {
        if (!OS.isLinux()) {
            return false;
        }

        final String SWT_INTERNAL_CLASS = "org.eclipse.swt.internal.gtk.OS";
        Class<?> osClass = AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {
            @Override
            public
            Class<?> run() {
                try {
                    return Class.forName(SWT_INTERNAL_CLASS, true, ClassLoader.getSystemClassLoader());
                } catch (Exception ignored) {
                }

                try {
                    return Class.forName(SWT_INTERNAL_CLASS, true, Thread.currentThread().getContextClassLoader());
                } catch (Exception ignored) {
                }

                return null;
            }
        });


        if (osClass == null) {
            return false;
        }

        final Class<?> clazz = osClass;
        Method method = AccessController.doPrivileged(new PrivilegedAction<Method>() {
            @Override
            public
            Method run() {
                try {
                    return clazz.getMethod("gtk_major_version");
                } catch (Exception e) {
                    return null;
                }
            }
        });

        if (method == null) {
            return false;
        }

        int version = 0;
        try {
            version = ((Number)method.invoke(osClass)).intValue();
        } catch (Exception ignored) {
            // this method doesn't exist.
        }

        return version == 3;
    }


    public static
    class SwtOverride {
        static
        void onShutdown(final Display currentDisplay, final Runnable runnable) {
            // currentDisplay.getShells() must only be called inside the event thread!

           org.eclipse.swt.widgets.Shell shell = currentDisplay.getShells()[0];
           shell.addListener(org.eclipse.swt.SWT.Close, new org.eclipse.swt.widgets.Listener() {
               @Override
               public
               void handleEvent(final org.eclipse.swt.widgets.Event event) {
                   runnable.run();
               }
           });
        }
    }


    public static
    int getVersion() {
        return version;
    }

    public static
    void dispatch(final Runnable runnable) {
        currentDisplay.syncExec(runnable);
    }

    public static
    boolean isEventThread() {
        return Thread.currentThread() == currentDisplayThread;
    }

    public static
    void onShutdown(final Runnable runnable) {
        // currentDisplay.getShells() must only be called inside the event thread!
        if (isEventThread()) {
            SwtOverride.onShutdown(currentDisplay, runnable);
        } else {
            dispatch(new Runnable() {
                @Override
                public
                void run() {
                    SwtOverride.onShutdown(currentDisplay, runnable);
                }
            });
        }
    }
}
