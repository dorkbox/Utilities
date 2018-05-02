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

    private static final Object currentDisplay;
    private static final Thread currentDisplayThread;

    // Methods are cached for performance
    private static final Method syncExecMethod;
    private static final int version;

    static {
        boolean isSwtLoaded_ = false;
        boolean isSwtGtk3_ = false;

        try {
            java.lang.reflect.Method m = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            m.setAccessible(true);
            ClassLoader cl = ClassLoader.getSystemClassLoader();

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
            LoggerFactory.getLogger(Swt.class).debug("Error detecting if SWT is loaded", e);
        }

        isLoaded = isSwtLoaded_;
        isGtk3 = isSwtGtk3_;

        // we MUST save this now, otherwise it is "null" when methods are run from the swing EDT.
        //
        // currentDisplay = org.eclipse.swt.widgets.Display.getCurrent();
        // currentDisplayThread = currentDisplay.getThread();

        // also save the SWT version

        Object _currentDisplay = null;
        Thread _currentDisplayThread = null;

        Method _syncExecMethod = null;
        int _version = 0;

        if (isSwtLoaded_) {
            try {
                // SWT.getVersion()
                Class<?> clazz = Class.forName("org.eclipse.swt.SWT");
                Method getVersionMethod = clazz.getMethod("getVersion");
                _version = (Integer) getVersionMethod.invoke(null);


                clazz = Class.forName("org.eclipse.swt.widgets.Display");
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
                    ClassLoaderUtil.defineClass(bytes);
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
                ClassLoaderUtil.defineClass(bytes);
            } catch (Throwable e) {
                LoggerFactory.getLogger(Swt.class).error("Cannot initialize SWT", e);
            }
        }

        currentDisplay = _currentDisplay;
        currentDisplayThread = _currentDisplayThread;
        syncExecMethod = _syncExecMethod;
        version = _version;
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

    public static
    int getVersion() {
        return version;
    }
}
