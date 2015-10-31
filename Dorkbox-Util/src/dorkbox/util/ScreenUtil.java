package dorkbox.util;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Screen utilities
 */
public final
class ScreenUtil {
    public static
    Rectangle getScreenBoundsAt(Point pos) {
        GraphicsDevice gd = getGraphicsDeviceAt(pos);
        Rectangle bounds = null;

        if (gd != null) {
            bounds = gd.getDefaultConfiguration()
                       .getBounds();
        }

        return bounds;
    }

    public static
    GraphicsDevice getGraphicsDeviceAt(Point pos) {

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
