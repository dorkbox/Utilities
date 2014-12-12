/*
 * Copyright 2014 dorkbox, llc
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
import java.awt.EventQueue;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

public class SwingUtil {
    public static void showOnSameScreenAsMouseCenter(Container frame) {
        Point mouseLocation = MouseInfo.getPointerInfo().getLocation();

        GraphicsDevice deviceAtMouse = getGraphicsDeviceAt(mouseLocation);
        Rectangle bounds = deviceAtMouse.getDefaultConfiguration().getBounds();
        frame.setLocation(bounds.x + bounds.width / 2 - frame.getWidth() / 2, bounds.height / 2 - frame.getHeight() / 2);
    }

    public static void showOnSameScreenAsMouse(Container frame) {
        Point mouseLocation = MouseInfo.getPointerInfo().getLocation();

        GraphicsDevice deviceAtMouse = getGraphicsDeviceAt(mouseLocation);
        frame.setLocation(deviceAtMouse.getDefaultConfiguration().getBounds().x, frame.getY());
    }

    public static Rectangle getScreenBoundsAt(Point pos) {
        GraphicsDevice gd = SwingUtil.getGraphicsDeviceAt(pos);
        Rectangle bounds = null;
        if (gd != null) {
            bounds = gd.getDefaultConfiguration().getBounds();
        }

        return bounds;
    }

    public static GraphicsDevice getGraphicsDeviceAt(Point pos) {
        GraphicsDevice device;

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice lstGDs[] = ge.getScreenDevices();

        ArrayList<GraphicsDevice> lstDevices = new ArrayList<GraphicsDevice>(lstGDs.length);

        for (GraphicsDevice gd : lstGDs) {

            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            Rectangle screenBounds = gc.getBounds();

            if (screenBounds.contains(pos)) {
                lstDevices.add(gd);
            }
        }

        if (lstDevices.size() > 0) {
            device = lstDevices.get(0);
        } else {
            device = ge.getDefaultScreenDevice();
        }

        return device;
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

    public static void invokeLater(Runnable runnable) {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    public static void invokeAndWait(Runnable runnable) {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
        } else {
            try {
                EventQueue.invokeAndWait(runnable);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
