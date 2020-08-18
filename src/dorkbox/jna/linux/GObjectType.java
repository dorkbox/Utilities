/*
 * Copyright 2017 dorkbox, llc
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
package dorkbox.jna.linux;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

public
class GObjectType extends PointerType {
    public
    GObjectType() {
    }

    public
    GObjectType(Pointer p) {
        super(p);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected
    void finalize() throws Throwable {
        super.finalize();
    }

    public
    void ref() {
        GObject.g_object_ref(getPointer());
    }

    public
    void unref() {
        GObject.g_object_unref(getPointer());
    }
}
