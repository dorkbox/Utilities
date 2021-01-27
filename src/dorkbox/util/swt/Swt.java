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

import java.security.AccessController;
import java.security.PrivilegedAction;

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

    // NOTE: This class cannot have SWT **ANYTHING** in it
    static {
        // There is a silly amount of redirection, simply because we have to be able to access SWT, but only if it's in use.

        // Since this class is the place other code interacts with, we can use SWT stuff if necessary without loading/linking
        // the SWT classes by accident

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

        if (swtErrorClass != null) {
            // this means that SWT is available in the system at runtime. We use the error class because that DOES NOT intitialize anything
            boolean isSwtLoadable_ = SwtAccess.isLoadable();
            version = SwtAccess.getVersion();
            isLoaded = isSwtLoadable_;
            isGtk3 = isSwtLoadable_ && SwtAccess.isGtk3();

            SwtAccess.init();
        } else {
            version = 0;
            isLoaded = false;
            isGtk3 = false;
        }
    }

    public static
    int getVersion() {
        return version;
    }

    public static
    void dispatch(final Runnable runnable) {
        SwtAccess.dispatch(runnable);
    }

    public static
    boolean isEventThread() {
        return SwtAccess.isEventThread();
    }

    public static
    void onShutdown(final Runnable runnable) {
        SwtAccess.onShutdown(runnable);
    }
}
