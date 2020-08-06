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
 * https://developer.apple.com/documentation/appkit/nsmenu?language=objc
 */
public
class NSMenu extends NSObject {

    private static final Pointer objectClass = ObjectiveC.objc_lookUpClass("NSMenu");

    private static final Pointer addItem = ObjectiveC.sel_getUid("addItem:");
    private static final Pointer removeItem = ObjectiveC.sel_getUid("removeItem:");
    private static final Pointer setAutoenablesItems = ObjectiveC.sel_getUid("setAutoenablesItems:");
    private static final Pointer itemAtIndex = ObjectiveC.sel_getUid("itemAtIndex:");
    private static final Pointer insertItemAtIndex = ObjectiveC.sel_getUid("insertItem:atIndex:");

    public
    NSMenu() {
        super(ObjectiveC.class_createInstance(objectClass, 0));

        // make this part of the constructor. By default, we want to have items automatically enabled
        setAutoEnablesItems(true);
    }

    /**
     * Adds a menu item to the end of the menu.
     * <p>
     * NOTE: This method invokes insertItem(_:at:). Thus, the menu does not accept the
     * menu item if it already belongs to another menu. After adding the menu item, the menu updates itself.
     *
     * @param newItem The menu item (an object conforming to the NSMenuItem protocol) to add to the menu.
     */
    public
    void addItem(NSMenuItem newItem) {
        ObjectiveC.objc_msgSend(this, addItem, newItem);
    }

    /**
     * Inserts a menu item into the menu at a specific location.
     * <p>
     * NOTE: This method posts an NSMenuDidAddItemNotification, allowing
     * interested observers to update as appropriate. This method is a primitive
     * method. All item-addition methods end up calling this method, so this is
     * where you should implement custom behavior on adding new items to a menu
     * in a custom subclass. If the menu item already exists in another menu, it
     * is not inserted and the method raises an exception of type
     * NSInternalInconsistencyException.
     *
     * @param newItem An object conforming to the NSMenuItem protocol that represents a menu item.
     * @param index An integer index identifying the location of the menu item in the menu.
     */
    public
    void insertItemAtIndex(NSMenuItem newItem, int index) {
        ObjectiveC.objc_msgSend(this, insertItemAtIndex, newItem, index);
    }

    /**
     * Returns the menu item at a specific location of the menu.
     *
     * @param index An integer index locating a menu item in a menu.
     *
     * @return The found menu item (an object conforming to the NSMenuItem protocol) or nil if the object couldn't be found.
     */
    public
    NSMenuItem itemAtIndex(int index) {
        return new NSMenuItem(ObjectiveC.objc_msgSend(this, itemAtIndex, index));
    }

    /**
     * Indicates whether the menu automatically enables and disables its menu items.
     * <p>
     * This property contains a Boolean value, indicating whether the menu automatically
     * enables and disables its menu items. If set to true, menu items of the menu are automatically
     * enabled and disabled according to rules computed by the NSMenuValidation informal protocol.
     * By default, NSMenu objects autoenable their menu items.
     *
     * @param enable true to enable the items by default, false to disable items by default
     */
    public
    void setAutoEnablesItems(boolean enable) {
        // WEIRDLY enough, the logic is backwards...
        ObjectiveC.objc_msgSend(this, setAutoenablesItems, !enable);
    }


    /**
     * Removes a menu item from the menu.
     *
     * @param anItem The menu item to remove.
     */
    public
    void removeItem(final NSMenuItem anItem) {
        ObjectiveC.objc_msgSend(this, removeItem, anItem);
    }
}
