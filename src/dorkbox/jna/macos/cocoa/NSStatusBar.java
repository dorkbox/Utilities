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
 * https://developer.apple.com/documentation/appkit/nsstatusbar?language=objc
 */
public
class NSStatusBar extends NSObject {

    public final static float NSVariableStatusItemLength = -1;  // A status item length that dynamically adjusts to the width of its contents.
    public final static float NSSquareStatusItemLength = -2; // A status item length that is equal to the status bar’s thickness.

    static Pointer objectClass = ObjectiveC.objc_lookUpClass("NSStatusBar");

    static Pointer systemStatusBar = ObjectiveC.sel_getUid("systemStatusBar");
    static Pointer statusItemWithLength = ObjectiveC.sel_getUid("statusItemWithLength:");
    static Pointer removeStatusItem = ObjectiveC.sel_getUid("removeStatusItem:");

    public
    NSStatusBar(long peer) {
        super(peer);
    }

    public static
    NSStatusBar systemStatusBar() {
        return new NSStatusBar(ObjectiveC.objc_msgSend(objectClass, systemStatusBar));
    }

    public
    NSStatusItem newStatusItem() {
        long result = ObjectiveC.objc_msgSend(this, statusItemWithLength, NSStatusBar.NSSquareStatusItemLength);
        return result != 0 ? new NSStatusItem(result) : null;
    }

    public
    void removeStatusItem(NSStatusItem statusItem) {
        ObjectiveC.objc_msgSend(this, removeStatusItem, statusItem);
    }
}
