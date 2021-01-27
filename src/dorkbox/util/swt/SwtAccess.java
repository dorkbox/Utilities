package dorkbox.util.swt;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import dorkbox.os.OS;

public
class SwtAccess {
    private static Display currentDisplay = null;
    private static Thread currentDisplayThread = null;

    public static
    void init() {
        // we MUST save this on init, otherwise it is "null" when methods are run from the swing EDT.
        currentDisplay = org.eclipse.swt.widgets.Display.getCurrent();
        currentDisplayThread = currentDisplay.getThread();
    }

    static
    boolean isLoadable() {
        return org.eclipse.swt.SWT.isLoadable();
    }

    static
    void onShutdown(final org.eclipse.swt.widgets.Display currentDisplay, final Runnable runnable) {
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

    static
    int getVersion() {
        return SWT.getVersion();
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

        // required to use reflection, because this is an internal class!
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
            SwtAccess.onShutdown(currentDisplay, runnable);
        } else {
            dispatch(new Runnable() {
                @Override
                public
                void run() {
                    SwtAccess.onShutdown(currentDisplay, runnable);
                }
            });
        }
    }
}
