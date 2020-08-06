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
package dorkbox.jna.linux.structs;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import dorkbox.util.Keep;
import dorkbox.jna.linux.AppIndicator;

@Keep
public
class AppIndicatorInstanceStruct extends Structure {
    public GObjectStruct parent;
    public Pointer priv;

    @Override
    protected
    List<String> getFieldOrder() {
        return Arrays.asList("parent", "priv");
    }

    public
    void app_indicator_set_title(String title) {
        AppIndicator.app_indicator_set_title(getPointer(), title);
    }

    public
    void app_indicator_set_status(int status) {
        AppIndicator.app_indicator_set_status(getPointer(), status);
    }

    public
    void app_indicator_set_menu(Pointer menu) {
        AppIndicator.app_indicator_set_menu(getPointer(), menu);
    }

    public
    void app_indicator_set_icon(String icon_name) {
        AppIndicator.app_indicator_set_icon(getPointer(), icon_name);
    }
}
