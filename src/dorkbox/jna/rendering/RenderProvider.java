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

/**
 * A "RenderProvider", is either None (internal implementation), JavaFX (either from Oracle, or the OpenJavaFX), or SWT (from Eclipse).
 */
public
class RenderProvider {
    private static Renderer provider = new DefaultProvider();

    /**
     * Assigns the specified provider. This is primarily used by the SystemTray and GTK applications.
     *
     * @param provider which provider is used. This will change from the default provider to SWT or JAVAFX
     */
    public static void set(Renderer provider) {
        RenderProvider.provider = provider;
    }

    /**
     * @return true if the render provider is supported (correct library versions, etc)
     */
    public static
    boolean isSupported() {
        return provider.isSupported();
    }

    /**
     * @return true if the GTK provider type is the default provider.
     */
    public static
    boolean isDefault() {
        return provider.getType() == ProviderType.NONE;
    }

    /**
     * @return true if the GTK provider type is JavaFX.
     */
    public static
    boolean isJavaFX() {
        return provider.getType() == ProviderType.JAVAFX;
    }

    /**
     * @return true if the GTK provider type is SWT
     */
    public static
    boolean isSwt() {
        return provider.getType() == ProviderType.SWT;
    }

    /**
     * @return the GTK provider type. SWT or JAVAFX (or none, if it's the null provider)
     */
    public static
    ProviderType getType() {
        return provider.getType();
    }

    /**
     * Necessary to determine if the current execution thread is the event dispatch thread, or a different thread
     *
     * @return true if the current thread is the dispatch/event thread
     */
    public static
    boolean isEventThread() {
        return provider.isEventThread();
    }

    /**
     * Used to discover the in-use version of GTK by the system (where appropriate) during startup.
     *
     * If this provider is not 'loaded', then this method will not be called.
     *
     * @return the GTK version in use by the provider. A return 0 will skip this provider's GTK version info
     */
    public static
    int getGtkVersion() {
        return provider.getGtkVersion();
    }

    /**
     * If we are currently on the dispatch thread, then we want to execute this task immediately. Otherwise, this task should be executed
     * on the dispatch thread
     *
     * @return true if this task was dispatched by this provider, false if the default provider should handle it
     */
    public static
    boolean dispatch(final Runnable runnable) {
        return provider.dispatch(runnable);
    }

    /**
     * depending on how the system is initialized, SWT may, or may not, have the gtk_main loop running. It will EVENTUALLY run, so we
     * do not want to run our own GTK event loop.
     *
     * JavaFX is not so strange in how GTK starts, so (at least for now), we only care about SWT being loaded
     *
     * @return if we are SWT, then we are considered "already running". JavaFx provides a method to detected if it's running at startup
     */
    public static
    boolean alreadyRunning() {
        return provider.alreadyRunning();
    }
}
