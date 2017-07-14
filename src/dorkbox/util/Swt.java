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

import org.slf4j.LoggerFactory;

/**
 * Utility methods for SWT. This will by written by {@link SwtBytecodeOverride}, via Javassist so we don't require a hard dependency on
 * SWT.
 * <p>
 * The methods/fields that this originated from * are commented out.
 * <p>
 * SWT system tray types are just GTK trays.
 */
public
class Swt {
     private static final org.eclipse.swt.widgets.Display currentDisplay;
     private static final Thread currentDisplayThread;

     static {
         // we MUST save this, otherwise it is "null" when methods are run from the swing EDT.
         currentDisplay = org.eclipse.swt.widgets.Display.getCurrent();

         currentDisplayThread = currentDisplay.getThread();
     }

    public static
    void init() {
         if (currentDisplay == null) {
             LoggerFactory.getLogger(Swt.class)
                          .error("Unable to get the current display for SWT. Please create an issue with your OS and Java " +
                                 "version so we may further investigate this issue.");
         }
//        throw new RuntimeException("This should never happen, as this class is over-written at runtime.");
    }

    public static
    void dispatch(final Runnable runnable) {
         currentDisplay.syncExec(runnable);
//        throw new RuntimeException("This should never happen, as this class is over-written at runtime.");
    }

    public static
    boolean isEventThread() {
         return Thread.currentThread() == currentDisplayThread;
//        throw new RuntimeException("This should never happen, as this class is over-written at runtime.");
    }

    public static
    void onShutdown(final Runnable runnable) {
         // currentDisplay.getShells() can only happen inside the event thread!
         if (isEventThread()) {
             currentDisplay.getShells()[0].addListener(org.eclipse.swt.SWT.Close, new org.eclipse.swt.widgets.Listener() {
                 @Override
                 public
                 void handleEvent(final org.eclipse.swt.widgets.Event event) {
                     runnable.run();
                 }
             });
         } else {
             dispatch(new Runnable() {
                 @Override
                 public
                 void run() {
                     currentDisplay.getShells()[0].addListener(org.eclipse.swt.SWT.Close, new org.eclipse.swt.widgets.Listener() {
                         @Override
                         public
                         void handleEvent(final org.eclipse.swt.widgets.Event event) {
                             runnable.run();
                         }
                     });
                 }
             });
         }
//        throw new RuntimeException("This should never happen, as this class is over-written at runtime.");
    }
}
