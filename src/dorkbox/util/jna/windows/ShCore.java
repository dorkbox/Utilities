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
package dorkbox.util.jna.windows;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import dorkbox.util.jna.JnaHelper;


/**
 * bindings for ShCore.dll
 *
 * Minimum supported client
 *  Windows 8.1 [desktop apps only]
 *
 * Minimum supported server
 *  Windows Server 2012 R2 [desktop apps only]
 *
 * <p>
 * Direct-mapping, See: https://github.com/java-native-access/jna/blob/master/www/DirectMapping.md
 */
public
class ShCore {
    static {
        JnaHelper.register("shcore", ShCore.class);
    }


    /**
     * https://msdn.microsoft.com/de-de/library/windows/desktop/dn280510(v=vs.85).aspx
     *
     * @param hmonitor A handle to the window whose DC is to be retrieved. If this value is NULL, GetDC retrieves the DC for the entire
     * screen.
     *
     * @param dpiType
     *    MDT_EFFECTIVE_DPI  = 0,
     *      The effective DPI. This value should be used when determining the correct scale factor for scaling UI elements. This
     *      incorporates the scale factor set by the user for this specific display.
     *    MDT_ANGULAR_DPI    = 1,
     *      The angular DPI. This DPI ensures rendering at a compliant angular resolution on the screen. This does not include the
     *      scale factor set by the user for this specific display.
     *    MDT_RAW_DPI        = 2,
     *      The raw DPI. This value is the linear DPI of the screen as measured on the screen itself. Use this value when you want
     *      to read the pixel density and not the recommended scaling setting. This does not include the scale factor set by the user
     *      for this specific display and is not guaranteed to be a supported DPI value.
     *    MDT_DEFAULT        = MDT_EFFECTIVE_DPI
     *
     *
     * @return if the function succeeds, the return value is a handle to the DC for the specified window's client area. If the function
     * fails, the return value is NULL.
     */
    public static native
    Pointer GetDpiForMonitor(Pointer hmonitor, int dpiType, IntByReference dpiX, IntByReference dpiY);
}
