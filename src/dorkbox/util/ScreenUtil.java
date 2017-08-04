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
package dorkbox.util;

import java.awt.Container;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Screen utilities
 */
public final
class ScreenUtil {
    public static
    Rectangle getScreenBoundsAt(Point pos) {
        GraphicsDevice gd = getMonitorAtLocation(pos);
        Rectangle bounds = null;

        if (gd != null) {
            bounds = gd.getDefaultConfiguration()
                       .getBounds();
        }

        return bounds;
    }

    public static
    GraphicsDevice getMonitorAtMouseLocation() {
        Point mouseLocation = MouseInfo.getPointerInfo()
                                       .getLocation();

        return getMonitorAtLocation(mouseLocation);
    }

    public static
    GraphicsDevice getMonitorAtLocation(Point pos) {

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice screenDevices[] = ge.getScreenDevices();

        GraphicsDevice device = null;
        for (GraphicsDevice device1 : screenDevices) {
            GraphicsConfiguration gc = device1.getDefaultConfiguration();
            Rectangle screenBounds = gc.getBounds();

            if (screenBounds.contains(pos)) {
                device = device1;
                break;
            }
        }

        if (device == null) {
            device = ge.getDefaultScreenDevice();
        }

        return device;
    }

    public static
    int getMonitorNumberAtMouseLocation() {
        Point mouseLocation = MouseInfo.getPointerInfo()
                                       .getLocation();

        return getMonitorNumberAtLocation(mouseLocation);
    }

    public static
    int getMonitorNumberAtLocation(Point pos) {

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice screenDevices[] = ge.getScreenDevices();

        for (int i = 0; i < screenDevices.length; i++) {
            final GraphicsDevice device1 = screenDevices[i];
            GraphicsConfiguration gc = device1.getDefaultConfiguration();
            Rectangle screenBounds = gc.getBounds();

            if (screenBounds.contains(pos)) {
                return i;
            }
        }

        // we are the primary monitor, so return 0.
        return 0;
    }

    public static
    void showOnSameScreenAsMouse_Center(final Container frame) {
        Point mouseLocation = MouseInfo.getPointerInfo()
                                       .getLocation();

        GraphicsDevice monitorAtMouse = ScreenUtil.getMonitorAtLocation(mouseLocation);
        Rectangle bounds = monitorAtMouse.getDefaultConfiguration()
                                         .getBounds();
        frame.setLocation(bounds.x + bounds.width / 2 - frame.getWidth() / 2, bounds.y + bounds.height / 2 - frame.getHeight() / 2);
    }

    public static
    void showOnSameScreenAsMouse(final Container frame) {
        Point mouseLocation = MouseInfo.getPointerInfo()
                                       .getLocation();

        GraphicsDevice monitorAtMouse = ScreenUtil.getMonitorAtLocation(mouseLocation);
        Rectangle bounds = monitorAtMouse.getDefaultConfiguration()
                                         .getBounds();
        frame.setLocation(bounds.x, bounds.y);
    }

    private
    ScreenUtil() {
    }


//  public static Rectangle getSafeScreenBounds(Point pos) {
//      Rectangle bounds = getScreenBoundsAt(pos);
//      Insets insets = getScreenInsetsAt(pos);
//
//      bounds.x += insets.left;
//      bounds.y += insets.top;
//      bounds.width -= insets.left + insets.right;
//      bounds.height -= insets.top + insets.bottom;
//
//      return bounds;
//  }

//  public static Insets getScreenInsetsAt(Point pos) {
//      GraphicsDevice gd = getGraphicsDeviceAt(pos);
//      Insets insets = null;
//      if (gd != null) {
//          insets = Toolkit.getDefaultToolkit().getScreenInsets(gd.getDefaultConfiguration());
//      }
//
//      return insets;
//  }
}
