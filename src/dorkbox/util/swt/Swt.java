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

import dorkbox.util.ClassLoaderUtil;

/**
 * Utility methods for SWT.
 */
@SuppressWarnings("Convert2Lambda")
public
class Swt {
    public final static boolean isLoaded;
    public final static boolean isGtk3;
    private static final int version;

    static {
        // There is a silly amount of redirection, simply because we have to be able to access SWT, but only if it's in use.
        // Since this class is the place other code interacts with, we can use SWT stuff if necessary without loading/linking
        // the SWT classes by accident

        boolean isSwtLoaded_ = ClassLoaderUtil.isClassLoaded(ClassLoader.getSystemClassLoader(), "org.eclipse.swt.widgets.Display");
        if (!isSwtLoaded_) {
            // check both classloaders
            isSwtLoaded_ = ClassLoaderUtil.isClassLoaded(Thread.currentThread().getContextClassLoader(), "org.eclipse.swt.widgets.Display");
        }

        int _version = 0;
        if (isSwtLoaded_) {
            _version = SwtDispatch.getVersion();
        }

        version = _version;
        isLoaded = isSwtLoaded_;
        isGtk3 = isSwtLoaded_ && SwtDispatch.isGtk3();
    }

    public static
    void main(String[] args) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        boolean classLo2aded = ClassLoaderUtil.isClassLoaded(contextClassLoader, "dorkbox/util/swt/Swt.java");
        boolean classLoaded = ClassLoaderUtil.isClassLoaded(contextClassLoader, "org.eclipse.swt.SWTError");
        boolean classLo3aded = ClassLoaderUtil.isClassLoaded(contextClassLoader, ":asd");
    }

    public static
    int getVersion() {
        return version;
    }

    public static
    void dispatch(final Runnable runnable) {
        SwtDispatch.dispatch(runnable);
    }

    public static
    boolean isEventThread() {
        return SwtDispatch.isEventThread();
    }

    public static
    void onShutdown(final Runnable runnable) {
        SwtDispatch.onShutdown(runnable);
    }
}
