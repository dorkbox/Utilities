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

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

/**
 * The NullRepaintManager is a RepaintManager that doesn't do any repainting. Useful when all of the rendering is done manually by the
 * application.
 */
public
class NullRepaintManager extends RepaintManager {
    /**
     * Installs the NullRepaintManager onto the EDT (WARNING: This disables painting/rendering by the EDT, for the entire JVM)
     */
    public static
    void install() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public
            void run() {
                RepaintManager repaintManager = new NullRepaintManager();
                repaintManager.setDoubleBufferingEnabled(false);
                RepaintManager.setCurrentManager(repaintManager);
            }
        });
    }

    public
    void addInvalidComponent(JComponent c) {
        // do nothing
    }

    public
    void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
        // do nothing
    }

    public
    void markCompletelyDirty(JComponent c) {
        // do nothing
    }

    public
    void paintDirtyRegions() {
        // do nothing
    }
}
