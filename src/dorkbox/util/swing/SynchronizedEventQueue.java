/*
 * Copyright 2015 dorkbox, llc
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
package dorkbox.util.swing;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;

public final
class SynchronizedEventQueue extends EventQueue {
    public static final Object MUTEX = new Object();

    private static final SynchronizedEventQueue instance = new SynchronizedEventQueue();
    private static volatile boolean alreadyInUse = false;

    public static synchronized
    void install() {
        if (!alreadyInUse) {
            // set up the synchronized event queue
            EventQueue eventQueue = Toolkit.getDefaultToolkit()
                                           .getSystemEventQueue();
            eventQueue.push(instance);
            alreadyInUse = true;
        }
    }

    /**
     * Enforce singleton property.
     */
    private
    SynchronizedEventQueue() {
    }

    protected
    void dispatchEvent(AWTEvent aEvent) {
        synchronized (MUTEX) {
            try {
                super.dispatchEvent(aEvent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
