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
package dorkbox.util.javaFx;


import javafx.application.Platform;

/**
 * Utility methods for JavaFX. Redirection is used here so we don't load/link the JavaFX classes if we aren't using them
 */
class JavaFxDispatch {
    static
    void dispatch(final Runnable runnable) {
        Platform.runLater(runnable);
    }

    static
    boolean isEventThread() {
        return Platform.isFxApplicationThread();
    }

    static
    void onShutdown(final Runnable runnable) {
        com.sun.javafx.tk.Toolkit.getToolkit().addShutdownHook(runnable);
    }
}
