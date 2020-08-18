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
 * https://developer.apple.com/documentation/objectivec/nsobject?language=objc
 */
public
class NSObject extends Pointer {
    public static final Pointer objectClass = ObjectiveC.objc_lookUpClass("NSObject");

    private static final Pointer alloc = ObjectiveC.sel_getUid("alloc");
    private static final Pointer retain = ObjectiveC.sel_getUid("retain");
    private static final Pointer release = ObjectiveC.sel_getUid("release");

    public
    NSObject(long peer) {
        super(peer);

        retain();
    }

    public
    NSObject(Pointer pointer) {
        this(Pointer.nativeValue(pointer));
    }

    @Override
    @SuppressWarnings("deprecation")
    protected
    void finalize() throws Throwable {
        release();

        super.finalize();
    }

    public
    void alloc() {
        ObjectiveC.objc_msgSend(this, alloc);
    }


    public
    void retain() {
        ObjectiveC.objc_msgSend(this, retain);
    }

    public
    void release() {
        ObjectiveC.objc_msgSend(this, release);
    }

    @SuppressWarnings("WeakerAccess")
    public
    long asPointer() {
        return Pointer.nativeValue(this);
    }
}
