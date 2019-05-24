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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.slf4j.LoggerFactory;

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

    private static final Display currentDisplay;
    private static final Thread currentDisplayThread;

    private static final int version;

    static {
        boolean isSwtLoaded_ = false;
        boolean isSwtGtk3_ = false;

        try {
            // maybe we should load the SWT version? (In order for us to work with SWT, BOTH must be the same!!
            // SWT is GTK2, but if -DSWT_GTK3=1 is specified, it can be GTK3
            isSwtLoaded_ = null != Class.forName("org.eclipse.swt.widgets.Display");

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


        int _version = 0;

        // we MUST save the currentDisplay now, otherwise it is "null" when methods are run from the swing EDT.
        // also save the SWT version
        // NOTE: we cannot check if there is a default display, because JUST CHECKING will initialize a new one
        Display _currentDisplay = null;
        Thread _currentDisplayThread = null;


        if (isSwtLoaded_ && SWT.isLoadable()) {
            try {
                _version = SWT.getVersion();
                _currentDisplay = Display.getCurrent();

                if (_currentDisplay != null) {
                    _currentDisplayThread = Display.getCurrent().getThread();
                }
            } catch (Throwable e) {
                LoggerFactory.getLogger(Swt.class).error("Cannot initialize SWT", e);
            }
        }

        // if we use SWT incorrectly (ie, it's available on the classpath, but we didn't start the application) then
        // '_currentDisplay' will be null. This is a reasonable way to detect if SWT is being used or not.
        if (_currentDisplay == null) {
            _version = 0;
            isSwtLoaded_ = false;
            isSwtGtk3_ = false;
        }

        currentDisplay = _currentDisplay;
        currentDisplayThread = _currentDisplayThread;
        version = _version;
        isLoaded = isSwtLoaded_;
        isGtk3 = isSwtGtk3_;
     }


    private static
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

    public static
    int getVersion() {
        return version;
    }
}
