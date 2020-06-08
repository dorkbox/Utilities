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

import org.slf4j.LoggerFactory;

import dorkbox.jna.JnaClassUtils;
import dorkbox.util.OS;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;

/**
 * Utility methods for SWT. Some of the methods will be overwritten via Javassist so we don't require a hard dependency on SWT.
 * <p>
 * SWT system tray types are GtkStatusIcon trays (so we don't want to use them)
 */
@SuppressWarnings("Convert2Lambda")
public
class Swt {
    public final static boolean isLoaded;
    public final static boolean isGtk3;
    private static final int version;

    private static final Object currentDisplay;
    private static final Thread currentDisplayThread;

    // Methods are cached for performance
    private static final Method syncExecMethod;


    static {
        // There is a silly amount of redirection, simply because we have to be able to access SWT, but only if it's in use.
        // Since this class is the place other code interacts with, we can use SWT stuff if necessary without loading/linking
        // the SWT classes by accident

        boolean isSwtLoadable_ = isLoadable();

        int _version = 0;
        if (isSwtLoadable_) {
            try {
                // _version = SWT.getVersion();
                Class<?> clazz = Class.forName("org.eclipse.swt.SWT");
                Method _getVersionMethod = clazz.getMethod("getVersion", int.class);
                _version = (Integer) _getVersionMethod.invoke(null);
            } catch (Exception ignored) {
            }
        }

        version = _version;
        isLoaded = isSwtLoadable_;
        isGtk3 = isSwtLoadable_ && isGtk3();


        // we MUST save this on init, otherwise it is "null" when methods are run from the swing EDT.
        //
        // currentDisplay = org.eclipse.swt.widgets.Display.getCurrent();
        // currentDisplayThread = currentDisplay.getThread();

        Object _currentDisplay = null;
        Thread _currentDisplayThread = null;

        Method _syncExecMethod = null;

        if (isSwtLoadable_) {
            try {
                Class<?> clazz = Class.forName("org.eclipse.swt.widgets.Display");
                Method getCurrentMethod = clazz.getMethod("getCurrent");
                Method getThreadMethod = clazz.getMethod("getThread");
                _syncExecMethod = clazz.getDeclaredMethod("syncExec", Runnable.class);

                _currentDisplay = getCurrentMethod.invoke(null);
                _currentDisplayThread = (Thread) getThreadMethod.invoke(_currentDisplay);


                // re-write the part that is heavily SWT dependent, that cannot be done via reflection.
                byte[] bytes;
                String body;
                CtMethod method;
                CtField ctField;

                ClassPool pool = ClassPool.getDefault();
                CtClass swtOverriedClass = pool.get("dorkbox.util.Swt$SwtOverride");

                // the abstractions for listener are REQUIRED by javassist.
                {
                    CtClass listener = pool.makeClass("dorkbox.util.Swt_listener");
                    listener.addInterface(pool.get("org.eclipse.swt.widgets.Listener"));

                    ctField = new CtField(pool.get("java.lang.Runnable"), "runnable", listener);
                    ctField.setModifiers(Modifier.PROTECTED);
                    listener.addField(ctField);

                    method = CtNewMethod.make(CtClass.voidType, "handleEvent",
                                              new CtClass[]{pool.get("org.eclipse.swt.widgets.Event")}, null,
                                              "{" +
                                              "   this.runnable.run();" +
                                              "}", listener);
                    listener.addMethod(method);
                    bytes = listener.toBytecode();
                    JnaClassUtils.defineClass(bytes);
                }

                method = swtOverriedClass.getDeclaredMethod("onShutdown");
                body = "{" +
                       "org.eclipse.swt.widgets.Display currentDisplay = (org.eclipse.swt.widgets.Display)$1;" +
                       "Runnable runnable = $2;" +

                       "dorkbox.util.Swt_listener listener = new dorkbox.util.Swt_listener();" +
                       "listener.runnable = runnable;" +

                       "org.eclipse.swt.widgets.Shell shell = currentDisplay.getShells()[0];" +
                       "shell.addListener(org.eclipse.swt.SWT.Close, listener);" +
                       "}";
                method.setBody(body);
                bytes = swtOverriedClass.toBytecode();

                // define this new class in our current classloader
                JnaClassUtils.defineClass(bytes);
            } catch (Throwable e) {
                LoggerFactory.getLogger(Swt.class).error("Cannot initialize SWT", e);
            }
        }

        currentDisplay = _currentDisplay;
        currentDisplayThread = _currentDisplayThread;
        syncExecMethod = _syncExecMethod;
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

        boolean isLoadable = false;

        try {
            // org.eclipse.swt.SWT.isLoadable();
            Class<?> clazz = Class.forName("org.eclipse.swt.SWT");
            Method _isLoadableMethod = clazz.getMethod("isLoadable");
            isLoadable = (Boolean) _isLoadableMethod.invoke(null);
        } catch (Exception ignored) {
        }


        return null != swtErrorClass && isLoadable;
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


    // this class is over-written via Javassist, because reflection cannot create anonymous classes. Javassist can, with caveats.
    @SuppressWarnings("unused")
    public static
    class SwtOverride {
        static
        void onShutdown(final Object currentDisplay, final Runnable runnable) {
            // currentDisplay.getShells() must only be called inside the event thread!

//            org.eclipse.swt.widgets.Shell shell = currentDisplay.getShells()[0];
//            shell.addListener(org.eclipse.swt.SWT.Close, new org.eclipse.swt.widgets.Listener() {
//                @Override
//                public
//                void handleEvent(final org.eclipse.swt.widgets.Event event) {
//                    runnable.run();
//                }
//            });
            throw new RuntimeException("This should never happen, as this class is over-written at runtime.");
        }
    }


    public static
    int getVersion() {
        return version;
    }

    public static
    void dispatch(final Runnable runnable) {
        //         currentDisplay.syncExec(runnable);
        try {
            syncExecMethod.invoke(currentDisplay, runnable);
        } catch (Throwable e) {
            LoggerFactory.getLogger(Swt.class)
                         .error("Unable to execute JavaFX runLater(). Please create an issue with your OS and Java " +
                                "version so we may further investigate this issue.");
        }
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
