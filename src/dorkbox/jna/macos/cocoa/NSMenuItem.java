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
 * https://developer.apple.com/documentation/appkit/nsmenuitem?language=objc
 */
public
class NSMenuItem extends NSObject {

    private static final Pointer objectClass = ObjectiveC.objc_lookUpClass("NSMenuItem");

    private static final Pointer setTitle = ObjectiveC.sel_getUid("setTitle:");
    private static final Pointer toolTip = ObjectiveC.sel_getUid("toolTip");
    private static final Pointer setKeyEquivalent = ObjectiveC.sel_getUid("setKeyEquivalent:");

    private static final Pointer setIndentationLevel = ObjectiveC.sel_getUid("setIndentationLevel:");

    private static final Pointer setImage = ObjectiveC.sel_getUid("setImage:");
    private static final Pointer setOnStateImage = ObjectiveC.sel_getUid("setOnStateImage:");
    private static final Pointer setEnabled = ObjectiveC.sel_getUid("setEnabled:");
    private static final Pointer separatorItem = ObjectiveC.sel_getUid("separatorItem");

    private static final Pointer setSubmenu = ObjectiveC.sel_getUid("setSubmenu:");

    private static final Pointer setState = ObjectiveC.sel_getUid("setState:");
    private static final Pointer setTarget = ObjectiveC.sel_getUid("setTarget:");
    private static final Pointer setAction = ObjectiveC.sel_getUid("setAction:");

    public
    NSMenuItem() {
        super(ObjectiveC.class_createInstance(objectClass, 0));
    }

    public
    NSMenuItem(long peer) {
        super(peer);
    }

    /**
     * A menu item that is used to separate logical groups of menu commands.
     * <p>
     * NOTE: This menu item is disabled. The default separator item is blank space.
     */
    public static
    NSMenuItem separatorItem() {
        return new NSMenuItem(ObjectiveC.objc_msgSend(objectClass, separatorItem));
    }

    /**
     * The menu item's title.
     */
    public
    void setTitle(NSString title) {
        ObjectiveC.objc_msgSend(this, setTitle, title);
    }

    /**
     * @return the menu item's title.
     */
    public
    NSString setToolTip(NSString tooltip) {
        return new NSString(ObjectiveC.objc_msgSend(this, toolTip, tooltip));
    }

    /**
     * Sets the menu item indentation level for the menu item.
     *
     * @param indentationLevel Value is from 0 to 15. The default indentation level is 0.
     */
    public
    void setIndentationLevel(NSInteger indentationLevel) {
        ObjectiveC.objc_msgSend(this, NSMenuItem.setIndentationLevel, indentationLevel);
    }

    /**
     * The menu item's shortcut key, if it's a capital letter, then it's a capital letter required for the shortcut
     * <p>
     * https://developer.apple.com/library/archive/documentation/Cocoa/Conceptual/Button/Tasks/SettingButtonKeyEquiv.html
     */
    public
    void setKeyEquivalent(NSString characters) {
        ObjectiveC.objc_msgSend(this, setKeyEquivalent, characters);
    }

    /**
     * The menu item’s image.
     */
    public
    void setImage(NSImage image) {
        ObjectiveC.objc_msgSend(this, setImage, image);
    }

    /**
     * The menu item’s image.
     */
    public
    void setOnStateImage(NSImage image) {
        ObjectiveC.objc_msgSend(this, setOnStateImage, image);
    }

    /**
     * A Boolean value that indicates whether the menu item is enabled.
     */
    public
    void setEnabled(boolean enabled) {
        ObjectiveC.objc_msgSend(this, setEnabled, enabled);
    }

    /**
     * The submenu of the menu item.
     */
    public
    void setSubmenu(NSMenu submenu) {
        ObjectiveC.objc_msgSend(this, setSubmenu, submenu);
    }

    /**
     * The state of the menu item.
     * <p>
     * NOTE: The image associated with the new state is displayed to the left of the menu item.
     */
    public
    void setState(int state) {
        ObjectiveC.objc_msgSend(this, setState, state);
    }

    /**
     * The menu item's target.
     * <p>
     * NOTE: To ensure that a menu item’s target can receive commands while a
     * modal dialog is open, the target object should return YES in worksWhenModal.
     */
    public
    void setTarget(NSObject target) {
        ObjectiveC.objc_msgSend(this, setTarget, target);
    }

    /**
     * The menu item's action-method selector.
     */
    public
    void setAction(Pointer pointer) {
        ObjectiveC.objc_msgSend(this, setAction, pointer);
    }
}
