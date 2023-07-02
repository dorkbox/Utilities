/*
 * Copyright 2023 dorkbox, llc
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
package dorkbox.util

import java.awt.*

/**
 * Screen utilities
 */
object ScreenUtil {
    /**
     * Gets the version number.
     */
    val version = Sys.version


    /**
     * @return the screen bounds for the monitor at a specific point. Will return null if there is no screen at the point
     */
    fun getScreenBoundsAt(pos: Point): Rectangle? {
        return getMonitorAtLocation(pos)?.defaultConfiguration?.bounds
    }

    /**
     * @return the monitor that is presently at the current mouse location. Can never return null.
     */
    val monitorAtMouseLocation: GraphicsDevice
        get() {
            val mouseLocation = MouseInfo.getPointerInfo().location
            return getMonitorAtLocation(mouseLocation)!! // the mouse is ALWAYS on a screen!
        }

    /**
     * @return the monitor at the specified location. Will return null if there is no monitor for the specified location
     */
    fun getMonitorAtLocation(pos: Point): GraphicsDevice? {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screenDevices = ge.screenDevices

        for (device1 in screenDevices) {
            val gc = device1.defaultConfiguration
            val screenBounds = gc.bounds
            if (screenBounds.contains(pos)) {
                return device1
            }
        }

        return null
    }

    /**
     * @return the monitor number that is presently at the current mouse location. Can never return null.
     */
    val monitorNumberAtMouseLocation: Int
        get() {
            val mouseLocation = MouseInfo.getPointerInfo().location
            return getMonitorNumberAtLocation(mouseLocation)
        }

    /**
     * @return the monitor number that is presently at the current mouse location. Can never return null.
     */
    fun getMonitorNumberAtLocation(pos: Point): Int {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screenDevices = ge.screenDevices

        for (i in screenDevices.indices) {
            val device1 = screenDevices[i]
            val gc = device1.defaultConfiguration
            val screenBounds = gc.bounds
            if (screenBounds.contains(pos)) {
                return i
            }
        }

        // we are the primary monitor, so return 0.
        return 0
    }

    /**
     * Shows the Container at the same monitor, in the CENTER, as the mouse
     */
    fun showOnSameScreenAsMouse_Center(frame: Container) {
        val mouseLocation = MouseInfo.getPointerInfo().location
        val monitorAtMouse = getMonitorAtLocation(mouseLocation)
        val bounds = monitorAtMouse!!.defaultConfiguration.bounds
        frame.setLocation(bounds.x + bounds.width / 2 - frame.width / 2, bounds.y + bounds.height / 2 - frame.height / 2)
    }

    /**
     * Shows the Container at the same monitor as the mouse, at its default location
     */
    fun showOnSameScreenAsMouse(frame: Container) {
        val mouseLocation = MouseInfo.getPointerInfo().location
        val monitorAtMouse = getMonitorAtLocation(mouseLocation)!!
        val bounds = monitorAtMouse.defaultConfiguration.bounds
        frame.setLocation(bounds.x, bounds.y)
    }
}
