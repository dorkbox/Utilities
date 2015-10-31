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

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

public
class SwingUtil {

    /** used when setting various icon components in the GUI to "nothing", since null doesn't work */
    public static final Image BLANK_ICON = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB_PRE);

    public static final Font FONT_BOLD_12 = new Font("Source Code Pro", Font.BOLD, 12);
    public static final Font FONT_BOLD_14 = new Font("Source Code Pro", Font.BOLD, 14);
    public static final Font FONT_BOLD_16 = new Font("Source Code Pro", Font.BOLD, 16);

    public static final Font FONT_12 = new Font("Source Code Pro", Font.PLAIN, 12);
    public static final Font FONT_14 = new Font("Source Code Pro", Font.PLAIN, 14);


    public static
    void showOnSameScreenAsMouse_Center(final Container frame) {
        Point mouseLocation = MouseInfo.getPointerInfo()
                                       .getLocation();

        GraphicsDevice deviceAtMouse = ScreenUtil.getGraphicsDeviceAt(mouseLocation);
        Rectangle bounds = deviceAtMouse.getDefaultConfiguration()
                                        .getBounds();
        frame.setLocation(bounds.x + bounds.width / 2 - frame.getWidth() / 2, bounds.y + bounds.height / 2 - frame.getHeight() / 2);
    }

    public static
    void showOnSameScreenAsMouse(final Container frame) {
        Point mouseLocation = MouseInfo.getPointerInfo()
                                       .getLocation();

        GraphicsDevice deviceAtMouse = ScreenUtil.getGraphicsDeviceAt(mouseLocation);
        frame.setLocation(deviceAtMouse.getDefaultConfiguration()
                                       .getBounds().x, frame.getY());
    }


    /**
     * Adds a listener to the window parent of the given component. Can be before the component is really added to its hierarchy.
     *
     * @param source The source component
     * @param listener The listener to add to the window
     */
    public static
    void addWindowListener(final Component source, final WindowListener listener) {
        if (source instanceof Window) {
            ((Window) source).addWindowListener(listener);
        }
        else {
            source.addHierarchyListener(new HierarchyListener() {
                @Override
                public
                void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == HierarchyEvent.SHOWING_CHANGED) {
                        SwingUtilities.getWindowAncestor(source)
                                      .addWindowListener(listener);
                    }
                }
            });
        }
    }

    /**
     * Centers a component according to the window location.
     *
     * @param window The parent window
     * @param component A component, usually a dialog
     */
    public static
    void centerInWindow(final Window window, final Component component) {
        Dimension size = window.getSize();
        Point loc = window.getLocationOnScreen();
        Dimension cmpSize = component.getSize();
        loc.x += (size.width - cmpSize.width) / 2;
        loc.y += (size.height - cmpSize.height) / 2;
        component.setBounds(loc.x, loc.y, cmpSize.width, cmpSize.height);
    }

    /**
     * Opens the given website in the default browser, or show a message saying that no default browser could be accessed.
     *
     * @param parent The parent of the error message, if raised
     * @param uri The website uri
     */
    public static
    void browse(final Component parent, final String uri) {
        boolean cannotBrowse = false;
        if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                                                   .isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop()
                       .browse(new URI(uri));
            } catch (URISyntaxException ex) {
            } catch (IOException ex) {
                cannotBrowse = true;
            }
        }
        else {
            cannotBrowse = true;
        }

        if (cannotBrowse) {
            JOptionPane.showMessageDialog(parent, "It seems that I can't open a website using your" + "default browser, sorry.");
        }
    }

    public static
    void invokeLater(final Runnable runnable) {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
        }
        else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    public static
    void invokeAndWait(final Runnable runnable) {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
        }
        else {
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
