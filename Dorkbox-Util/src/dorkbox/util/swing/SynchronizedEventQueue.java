package dorkbox.util.swing;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;

final
class SynchronizedEventQueue extends EventQueue {
    public static final Object MUTEX = new Object();

    private static final SynchronizedEventQueue instance = new SynchronizedEventQueue();
    private static boolean alreadyInUse = false;

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
