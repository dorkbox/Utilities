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
import org.slf4j.LoggerFactory;

import dorkbox.util.OS;

/**
 * Utility methods for SWT.
 */
@SuppressWarnings("Convert2Lambda")
class SwtDispatch {
    private static final Display currentDisplay;
    private static final Thread currentDisplayThread;

    private static final int version;

    static {
        int _version = 0;

        // Necessary for us to work with SWT based on version info. We can try to set us to be compatible with whatever it is set to
        // System.setProperty("SWT_GTK3", "0");  (or -DSWT_GTK3=1)


        // we MUST save the currentDisplay now, otherwise it is "null" when methods are run from the swing EDT.
        // also save the SWT version
        // NOTE: we cannot check if there is a default display, because JUST CHECKING will initialize a new one
        Display _currentDisplay = null;
        Thread _currentDisplayThread = null;

        try {
            _version = SWT.getVersion();
            _currentDisplay = Display.getCurrent();

            if (_currentDisplay != null) {
                _currentDisplayThread = Display.getCurrent().getThread();
            }
        } catch (Throwable e) {
            LoggerFactory.getLogger(SwtDispatch.class).error("Cannot initialize SWT", e);
        }

        // if we use SWT incorrectly (ie, it's available on the classpath, but we didn't start the application) then
        // '_currentDisplay' will be null. This is a reasonable way to detect if SWT is being used or not.
        if (_currentDisplay == null) {
            _version = 0;
        }

        currentDisplay = _currentDisplay;
        currentDisplayThread = _currentDisplayThread;
        version = _version;
    }

    /**
     * This is an indirect reference to SWT, so we only check if SWT is loadable if certain classes are available.
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

        return null != swtErrorClass && SWT.isLoadable();
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

        Method method = AccessController.doPrivileged(new PrivilegedAction<Method>() {
            @Override
            public
            Method run() {
                try {
                    return osClass.getMethod("gtk_major_version");
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


    private static
    void onShutdown(final Display currentDisplay, final Runnable runnable) {
        // currentDisplay.getShells() must only be called inside the event thread!

       org.eclipse.swt.widgets.Shell shell = currentDisplay.getShells()[0];
       shell.addListener(SWT.Close, new org.eclipse.swt.widgets.Listener() {
           @Override
           public
           void handleEvent(final org.eclipse.swt.widgets.Event event) {
               runnable.run();
           }
       });
    }

    static
    void dispatch(final Runnable runnable) {
        currentDisplay.syncExec(runnable);
    }

    static
    boolean isEventThread() {
         return Thread.currentThread() == currentDisplayThread;
    }

    static
    void onShutdown(final Runnable runnable) {
        // currentDisplay.getShells() must only be called inside the event thread!
        if (isEventThread()) {
            onShutdown(currentDisplay, runnable);
         } else {
             dispatch(new Runnable() {
                 @Override
                 public
                 void run() {
                     onShutdown(currentDisplay, runnable);
                 }
             });
         }
    }

    static
    int getVersion() {
        return version;
    }
}
