/*
 * Copyright 2018 dorkbox, llc
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
package dorkbox.jna.macos.cocoa;

import com.sun.jna.Pointer;

import dorkbox.jna.macos.foundation.ObjectiveC;

/**
 * https://developer.apple.com/documentation/appkit/nsstatusitem?language=objc
 */
public
class NSStatusItem extends NSObject {

    private static final Pointer setHighlightMode = ObjectiveC.sel_getUid("setHighlightMode:");
    private static final Pointer setToolTip = ObjectiveC.sel_getUid("setToolTip:");
    private static final Pointer setMenu = ObjectiveC.sel_getUid("setMenu:");
    private static final Pointer setImage = ObjectiveC.sel_getUid("setImage:");

    public
    NSStatusItem(long peer) {
        super(peer);
    }

    public
    void setHighlightMode(boolean highlight) {
        ObjectiveC.objc_msgSend(this, setHighlightMode, highlight);
    }

    public
    void setImage(NSImage image) {
        ObjectiveC.objc_msgSend(this, setImage, image);
    }

    public
    void setMenu(NSMenu menu) {
        ObjectiveC.objc_msgSend(this, setMenu, menu);
    }

    public
    void setToolTip(NSString tooltip) {
        ObjectiveC.objc_msgSend(this, setToolTip, tooltip);
    }
}
