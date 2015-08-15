package dorkbox.util;

import java.awt.*;
import java.util.ArrayList;

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
        }
        else {
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
