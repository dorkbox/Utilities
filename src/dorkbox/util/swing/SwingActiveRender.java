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

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Window;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JComponent;

import dorkbox.util.ActionHandlerLong;

/**
 * Contains all of the appropriate logic to setup and render via "Active" rendering (instead of "Passive" rendering). This permits us to
 * render Windows (and their contents), OFF of the EDT - even though there are other frames/components that are ON the EDT. <br> Because we
 * still want to react to mouse events, etc on the EDT, we do not completely remove the EDT -- we merely allow us to "synchronize" the EDT
 * object to our thread. It's a little bit hacky, but it works beautifully, and permits MUCH nicer animations. <br>
 * <p/>
 * <b>It is also important to REMEMBER -- if you add a component to an actively managed Window, YOU MUST make sure to call {@link
 * JComponent#setIgnoreRepaint(boolean)} otherwise this component will "fight" on the EDT for updates. </b>
 */
public final
class SwingActiveRender {
    private static Thread activeRenderThread = null;

    static final List<Window> activeRenders = new ArrayList<Window>();
    static final List<ActionHandlerLong> activeRenderEvents = new CopyOnWriteArrayList<ActionHandlerLong>();

    // volatile, so that access triggers thread synchrony, since 1.6. See the Java Language Spec, Chapter 17
    static volatile boolean hasActiveRenders = false;

    private static final Runnable renderLoop = new ActiveRenderLoop();

    private
    SwingActiveRender() {
    }


    /**
     * Enables the window to to added to an "Active Render" thread, at a target "Frames-per-second". This is to support smooth, swing-based
     * animations. <br> This works by removing this object from EDT updates, and instead manually calls paint(g) on the window, updating it
     * on our own thread.
     *
     * @param window the window to add to the ActiveRender thread.
     */
    public static
    void addActiveRender(final Window window) {
        // this should be on the EDT
        if (!EventQueue.isDispatchThread()) {
            throw new RuntimeException("adding a swing Window to be actively rendered must be done on the EDT.");
        }

        // setup double-buffering, so we can properly use Active-Rendering, so the animations will be smooth
        window.createBufferStrategy(2);

        // have to specify ALL children in Window to ignore EDT paint requests
        Deque<Component> components = new ArrayDeque<Component>(8);
        components.add(window);

        Component[] c;
        Component pop;
        while ((pop = components.poll()) != null) {
            pop.setIgnoreRepaint(true);

            if (pop instanceof JComponent) {
                c = ((JComponent) pop).getComponents();
                Collections.addAll(components, c);
            }
        }

        synchronized (activeRenders) {
            if (!hasActiveRenders) {
                setupActiveRenderThread();
            }

            hasActiveRenders = true;
            activeRenders.add(window);
        }
    }

    /**
     * Removes a window from the ActiveRender queue. This should happen when the window is closed.
     *
     * @param window the window to remove
     */
    public static
    void removeActiveRender(final Window window) {
        synchronized (activeRenders) {
            activeRenders.remove(window);

            final boolean hadActiveRenders = !activeRenders.isEmpty();
            hasActiveRenders = hadActiveRenders;

            if (!hadActiveRenders) {
                activeRenderThread = null;
            }
        }

        // have to specify ALL children in window to obey EDT paint requests
        Deque<Component> components = new ArrayDeque<Component>(8);
        components.add(window);

        Component[] c;
        Component pop;
        while ((pop = components.poll()) != null) {
            pop.setIgnoreRepaint(false);

            if (pop instanceof JComponent) {
                c = ((JComponent) pop).getComponents();
                Collections.addAll(components, c);
            }
        }
    }

    /**
     * Specifies an ActionHandler to be called when the ActiveRender thread starts to render at each tick.
     *
     * @param handler the handler to add
     */
    public static
    void addActiveRenderFrameStart(final ActionHandlerLong handler) {
        synchronized (activeRenders) {
            activeRenderEvents.add(handler);
        }
    }

    /**
     * Potentially SLOW calculation, as it compares each entry in a queue for equality
     *
     * @param handler this is the handler to check
     *
     * @return true if this handler already exists in the active render, on-frame-start queue
     */
    public static
    boolean containsActiveRenderFrameStart(final ActionHandlerLong handler) {
        synchronized (activeRenders) {
            return activeRenderEvents.contains(handler);
        }
    }

    /**
     * Removes the handler from the on-frame-start queue
     *
     * @param handler the handler to remove
     */
    public static
    void removeActiveRenderFrameStart(final ActionHandlerLong handler) {
        synchronized (activeRenders) {
            activeRenderEvents.remove(handler);
        }
    }

    /**
     * Creates (if necessary) the active-render thread. When there are no active-render targets, this thread will exit
     */
    private static
    void setupActiveRenderThread() {
        if (activeRenderThread != null) {
            return;
        }

        SynchronizedEventQueue.install();

        activeRenderThread = new Thread(renderLoop, "AWT-ActiveRender");
        activeRenderThread.setDaemon(true);
        activeRenderThread.start();
    }
}
