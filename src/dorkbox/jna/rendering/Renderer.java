/*
 * Copyright 2021 dorkbox, llc
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

package dorkbox.jna.rendering;

public
interface Renderer {
    boolean isSupported();

    /**
     * @return the GTK provider type. SWT or JAVAFX
     */
    ProviderType getType();

    /**
     * depending on how the system is initialized, SWT may, or may not, have the gtk_main loop running. It will EVENTUALLY run, so we
     * do not want to run our own GTK event loop.
     * <p>
     * JavaFX is not so strange in how GTK starts, so (at least for now), we only care about SWT being loaded
     *
     * @return if we are SWT, then we are considered "already running"
     */
    boolean alreadyRunning();

    /**
     * Necessary to determine if the current execution thread is the event dispatch thread, or a different thread
     *
     * @return true if the current thread is the dispatch/event thread
     */
    boolean isEventThread();

    /**
     * Used to discover the in-use version of GTK by the system (where appropriate) during startup.
     * <p>
     * If this provider is not 'loaded', then this method will not be called.
     *
     * @return the GTK version in use by the provider. A value of 0 means "ignore this"
     */
    int getGtkVersion();

    boolean dispatch(final Runnable runnable);
}
