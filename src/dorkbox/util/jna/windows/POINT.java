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
package dorkbox.util.jna.windows;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

/**
 * https://msdn.microsoft.com/en-us/library/windows/desktop/dd162805(v=vs.85).aspx
 */
public
class POINT extends Structure {
    public long x;
    public long y;

    public
    POINT() {
    }

    public
    POINT(final long x, final long y) {
        this.x = x;
        this.y = y;
    }

    public
    POINT.ByValue asValue() {
        POINT.ByValue copy = new POINT.ByValue();
        copy.x = this.x;
        copy.y = this.y;
        return copy;
    }

    @Override
    protected
    List<String> getFieldOrder() {
        return Arrays.asList("x", "y");
    }

    @Override
    public
    String toString() {
        return x + ":" + y;
    }


    static public
    class ByValue extends POINT implements Structure.ByValue {}
}
