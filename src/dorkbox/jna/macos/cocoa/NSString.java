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

import java.nio.charset.Charset;

import com.sun.jna.Pointer;

import dorkbox.jna.macos.foundation.ObjectiveC;

/**
 * https://developer.apple.com/documentation/foundation/nsstring?language=objc
 */
public
class NSString extends NSObject {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    static
    Object asBytes(String string) {
        return (string + "\0").getBytes(UTF8);
    }


    private static final Pointer objectClass = ObjectiveC.objc_lookUpClass("NSString");

    private static final Pointer stringWithUTF8String = ObjectiveC.sel_getUid("stringWithUTF8String:");
    private static final Pointer UTF8String = ObjectiveC.sel_getUid("UTF8String");

    public
    NSString(long peer) {
        super(peer);
    }

    public
    NSString(String string) {
        this(ObjectiveC.objc_msgSend(objectClass, stringWithUTF8String, asBytes(string)));
    }

    public
    String toString() {
        long pointerReference = ObjectiveC.objc_msgSend(this, UTF8String);
        return new Pointer(pointerReference).getString(0);
    }
}
