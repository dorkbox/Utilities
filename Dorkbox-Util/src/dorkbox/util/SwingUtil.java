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
import javax.swing.UIManager;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;

public
class SwingUtil {
    static {
        /*
         * hack workaround for starting the Toolkit thread before any Timer stuff
         * javax.swing.Timer uses the Event Dispatch Thread, which is not
         * created until the Toolkit thread starts up.  Using the Swing
         * Timer before starting this stuff starts up may get unexpected
         * results (such as taking a long time before the first timer
         * event).
         */
        Toolkit.getDefaultToolkit();

        // loads fonts into the system, and sets the default look and feel.
        if (SystemProps.LoadAllFonts) {
            boolean isJava6 = OS.javaVersion == 6;
            String fontsLocation = SystemProps.FontsLocation;

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Enumeration<URL> fonts = LocationResolver.getResources(fontsLocation);
            if (fonts.hasMoreElements()) {
                // skip the FIRST one, since we always know that the first one is the directory we asked for
                fonts.nextElement();

                while (fonts.hasMoreElements()) {
                    URL url = fonts.nextElement();
                    InputStream is = null;

                    //noinspection TryWithIdenticalCatches
                    try {
                        String path = url.toURI()
                                         .getPath();

                        // only support TTF fonts (java6) and OTF fonts (7+).
                        if (path.endsWith(".ttf") || (!isJava6 && path.endsWith(".otf"))) {
                            is = url.openStream();

                            Font newFont = Font.createFont(Font.TRUETYPE_FONT, is);
                            // fonts that ALREADY exist are not re-registered
                            ge.registerFont(newFont);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    } catch (FontFormatException e) {
                        e.printStackTrace();
                    } finally {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }


        String customLandF = SystemProps.customLookAndFeel;
        if (customLandF != null && !customLandF.isEmpty()) {
            // register a better looking L&F (default we use is Nimbus)
            String name = UIManager.getLookAndFeel().getName();

            if (!customLandF.equals(name)) {
                try {
                    for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                        if (customLandF.equals(info.getName())) {
                            UIManager.setLookAndFeel(info.getClassName());
                            break;
                        }
                    }
                } catch (Exception e) {
                    // If Nimbus is not available, fall back to cross-platform
                    try {
                        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /**
     * Gets (or creates) a Font based on a specific system property. Remember: the FontManager caches system/loaded fonts, so we don't need
     * to ALSO cache them as well. see: https://stackoverflow.com/questions/6102602/java-awt-is-font-a-lightweight-object
     * <p>
     * Also remember that if requesting a BOLD hint for a font, the system will look for a font that is BOLD. If none are found, it
     * will then apply transforms to the specified font to create a font that is bold. Specifying a bold name AND a bold hint will not
     * "double bold" the font
     * <p></p>
     * For example:
     * <p>
     *
     * Font titleTextFont = SwingUtil.parseFont("Source Code Pro Bold 16");
     *
     * @param fontInfo This is the font "name style size", as a string. For example "Source Code Pro Bold BOLD 16"
     *
     * @return the specified font
     */
    public static
    Font parseFont(final String fontInfo) {
        try {
            final int sizeIndex = fontInfo.lastIndexOf(" ");

            String size = fontInfo.substring(sizeIndex + 1);

            // hint is at most 6 (ITALIC) before sizeIndex - we can use this to our benefit.
            int styleIndex = fontInfo.indexOf(" ", sizeIndex - 7);
            String styleString = fontInfo.substring(styleIndex + 1, sizeIndex);
            int style = Font.PLAIN;

            if (styleString.equalsIgnoreCase("bold")) {
                style = Font.BOLD;
            }
            else if (styleString.equalsIgnoreCase("italic")) {
                style = Font.ITALIC;
            }

            String fontName = fontInfo.substring(0, styleIndex);

            // this can be WRONG, in which case it will just error out
            //noinspection MagicConstant
            return new Font(fontName, style, Integer.parseInt(size));
        } catch (Exception e) {
            throw new RuntimeException("Unable to load font info from '" + fontInfo + "'", e);
        }
    }

    /** used when setting various icon components in the GUI to "nothing", since null doesn't work */
    public static final Image BLANK_ICON = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB_PRE);


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
