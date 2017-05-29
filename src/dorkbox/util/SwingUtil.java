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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Locale;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public
class SwingUtil {

    /** Default location where all the fonts are stored */
    @Property
    public static String FONTS_LOCATION = "resources/fonts";

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
    }

    /**
     * Sets the entire L&F based on "simple" name. Null to set to the system L&F. If this is not called (or set), Swing will use the
     * default CrossPlatform L&F, which is 'Metal'.
     *
     * NOTE: if null (which is the 'getSystemLookAndFeelClassName') and on Linux, this will cause GTK2 to get loaded first, which
     * will cause conflicts if one tries to use GTK3
     *
     * @param lookAndFeel the simple name or null for the system default
     */
    public static
    void setLookAndFeel(final String lookAndFeel) {
        if (lookAndFeel == null) {
            try {
                // NOTE: On Linux + swing, this will cause GTK2 to get loaded, which will cause conflicts if one tries to ALSO use GTK3
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        // register a different L&F
        String specified = lookAndFeel.toLowerCase(Locale.US).trim();
        String current = UIManager.getLookAndFeel().getName().toLowerCase(Locale.US);

        if (!specified.equals(current)) {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if (specified.equals(info.getName().toLowerCase(Locale.US))) {
                        UIManager.setLookAndFeel(info.getClassName());
                        return;
                    }
                }
            } catch (Exception e) {
                // whoops. something isn't right!
                e.printStackTrace();
            }
        }

        // display all properties for the specified look and feel
//        Set<Map.Entry<Object, Object>> entries = UIManager.getLookAndFeelDefaults()
//                                                          .entrySet();
//        for (Map.Entry<Object, Object> e : entries) {
//            String key;
//
//
//            if (e.getKey() instanceof StringBuffer) {
//                StringBuffer s = (StringBuffer) e.getKey();
//                key = s.toString();
//            }
//            else {
//                key = (String) e.getKey();
//            }
//
//            //Print all the keys available in UI manager
//            if (e.getValue() != null && (key.toLowerCase().contains("icon") || key.toLowerCase().contains("image"))) {
//                System.out.println("Key:  " + key);
//            }
//
//            if (key.endsWith("TabbedPane.font")) {
//                Font font = UIManager.getFont(key);
//                Font biggerFont = font.deriveFont(2 * font.getSize2D());
//                // change ui default to bigger font
//                UIManager.put(key, biggerFont);
//            }
//            else if (key.endsWith("Tree.rowHeight")) {
//                int rowHeight = UIManager.getInt(key);
//                int bigRowHeight = 2 * rowHeight;
//                UIManager.put(key, bigRowHeight);
//            }
//        }

        // this means we couldn't find our L&F
        new Exception("Could not load " + lookAndFeel + ", it was not available.").printStackTrace();
    }

    /** All of the fonts in the {@link #FONTS_LOCATION} will be loaded by the Font manager */
    public static
    void loadAllFonts() {
        boolean isJava6 = OS.javaVersion == 6;

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Enumeration<URL> fonts = LocationResolver.getResources(FONTS_LOCATION);

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
     * Gets the correct font (in GENERAL) for a specified pixel height.
     *
     * @param font the font we are checking
     * @param height the height in pixels we want to get as close as possible to
     *
     * @return the font (derived from the specified font) that is as close as possible to the requested height. If our font-size is less
     *          than the height, then the approach is from the low size (so the returned font will always fit inside the box)
     */
    public static
    Font getFontForSpecificHeight(final Font font, final int height) {
        int size = font.getSize();
        Boolean lastAction = null;
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        while (true) {
            Font fontCheck = new Font(font.getName(), Font.PLAIN, size);

            FontMetrics metrics = g.getFontMetrics(fontCheck);
            Rectangle2D rect = metrics.getStringBounds("`Tj|┃", g);  // `Tj|┃ are glyphs that are at the top/bottom of the fontset (usually)
            double testHeight = rect.getHeight();

            if (testHeight < height && lastAction != Boolean.FALSE) {
                size++;
                lastAction = Boolean.TRUE;
            } else if (testHeight > height && lastAction != Boolean.TRUE) {
                size--;
                lastAction = Boolean.FALSE;
            } else {
                // either we are the exact size, or we are ONE font size to big/small (depending on what our initial guess was)
                g.dispose();
                return fontCheck;
            }
        }
    }


    /**
     * Gets the specified font height
     * @param font
     * @return
     */
    public static
    int getFontHeight(final Font font) {
        // Because font metrics is based on a graphics context, we need to create a small, temporary image to determine the width and height
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        FontMetrics metrics = g.getFontMetrics(font);
        Rectangle2D rect = metrics.getStringBounds("`Tj|┃", g);  // `Tj|┃ are glyphs that are at the top/bottom of the fontset (usually)
        int testHeight = (int) rect.getHeight();

        g.dispose();

        return testHeight;
    }

    /**
     * Gets the specified text (with a font) and as an image
     *
     * @param font the specified font to render the image
     * @return a BufferedImage of the specified text, font, and color
     */
    public static
    BufferedImage getFontAsImage(final Font font, String text, Color foregroundColor) {
        // Because font metrics is based on a graphics context, we need to create a small, temporary image to determine the width and height
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);

        FontMetrics fm = g2d.getFontMetrics();
        int width = fm.stringWidth(text);
        int height = fm.getHeight();
        g2d.dispose();

        // make it square
        if (width > height) {
            height = width;
        } else {
            width = height;
        }

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = img.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        g2d.setFont(font);
        fm = g2d.getFontMetrics();

        g2d.setColor(foregroundColor);

        // width/4 centers the text in the image
        g2d.drawString(text, width/4.0f, fm.getAscent());
        g2d.dispose();

        return img;
    }

    /**
     * Gets the largest icon/image for a button (or other JComponent that has .setIcon(image) method) without affecting the size of the
     * button. An image that is any larger will require that the button increases it's height or width.
     *
     * @param button The button (or JMenuItem, etc that has the .setIcon() method ) that you want to measure
     *
     * @return the largest height of an icon before the button will increase in size (as a result of a larger image)
     */
    public static
    int getLargestIconHeightForButton(AbstractButton button) {
        // of note, components can ALSO have different sizes attached to them!
        // mini
        //      myButton.putClientProperty("JComponent.sizeVariant", "mini");
        // small
        //      mySlider.putClientProperty("JComponent.sizeVariant", "small");
        // large
        //      myTextField.putClientProperty("JComponent.sizeVariant", "large");

        int minHeight = 0;
        int iconSize = 0;
        for (int i = 1; i < 128; i++) {
            ImageIcon imageIcon = new ImageIcon(new BufferedImage(1, i, BufferedImage.TYPE_BYTE_BINARY));
            button.setIcon(imageIcon);
            button.invalidate();
            int height = (int) button.getPreferredSize()
                                     .getHeight();

            if (minHeight == 0) {
                minHeight = height;
            } else if (minHeight != height) {
                break;
            }

            // this is the largest icon size before the size of the component changes
            iconSize = imageIcon.getIconHeight();
        }

        return iconSize;
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
            } catch (URISyntaxException ignored) {
            } catch (IOException ex) {
                cannotBrowse = true;
            }
        }
        else {
            cannotBrowse = true;
        }

        if (cannotBrowse) {
            JOptionPane.showMessageDialog(parent, "It seems that I can't open a website using your default browser, sorry.");
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
    void invokeAndWait(final Runnable runnable) throws InvocationTargetException, InterruptedException {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
        }
        else {
            EventQueue.invokeAndWait(runnable);
        }
    }

    public static
    void invokeAndWaitQuietly(final Runnable runnable) {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
        }
        else {
            try {
                EventQueue.invokeAndWait(runnable);
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Converts a key character into it's corresponding VK entry
     */
    public static
    int getVirtualKey(final char key) {
        switch (key) {
            case 0x08: return KeyEvent.VK_BACK_SPACE;
            case 0x09: return KeyEvent.VK_TAB;
            case 0x0a: return KeyEvent.VK_ENTER;
            case 0x1B: return KeyEvent.VK_ESCAPE;
            case 0x20AC: return KeyEvent.VK_EURO_SIGN;
            case 0x20: return KeyEvent.VK_SPACE;
            case 0x21: return KeyEvent.VK_EXCLAMATION_MARK;
            case 0x22: return KeyEvent.VK_QUOTEDBL;
            case 0x23: return KeyEvent.VK_NUMBER_SIGN;
            case 0x24: return KeyEvent.VK_DOLLAR;
            case 0x26: return KeyEvent.VK_AMPERSAND;
            case 0x27: return KeyEvent.VK_QUOTE;
            case 0x28: return KeyEvent.VK_LEFT_PARENTHESIS;
            case 0x29: return KeyEvent.VK_RIGHT_PARENTHESIS;
            case 0x2A: return KeyEvent.VK_ASTERISK;
            case 0x2B: return KeyEvent.VK_PLUS;
            case 0x2C: return KeyEvent.VK_COMMA;
            case 0x2D: return KeyEvent.VK_MINUS;
            case 0x2E: return KeyEvent.VK_PERIOD;
            case 0x2F: return KeyEvent.VK_SLASH;
            case 0x30: return KeyEvent.VK_0;
            case 0x31: return KeyEvent.VK_1;
            case 0x32: return KeyEvent.VK_2;
            case 0x33: return KeyEvent.VK_3;
            case 0x34: return KeyEvent.VK_4;
            case 0x35: return KeyEvent.VK_5;
            case 0x36: return KeyEvent.VK_6;
            case 0x37: return KeyEvent.VK_7;
            case 0x38: return KeyEvent.VK_8;
            case 0x39: return KeyEvent.VK_9;
            case 0x3A: return KeyEvent.VK_COLON;
            case 0x3B: return KeyEvent.VK_SEMICOLON;
            case 0x3C: return KeyEvent.VK_LESS;
            case 0x3D: return KeyEvent.VK_EQUALS;
            case 0x3E: return KeyEvent.VK_GREATER;
            case 0x40: return KeyEvent.VK_AT;
            case 0x41: return KeyEvent.VK_A;
            case 0x42: return KeyEvent.VK_B;
            case 0x43: return KeyEvent.VK_C;
            case 0x44: return KeyEvent.VK_D;
            case 0x45: return KeyEvent.VK_E;
            case 0x46: return KeyEvent.VK_F;
            case 0x47: return KeyEvent.VK_G;
            case 0x48: return KeyEvent.VK_H;
            case 0x49: return KeyEvent.VK_I;
            case 0x4A: return KeyEvent.VK_J;
            case 0x4B: return KeyEvent.VK_K;
            case 0x4C: return KeyEvent.VK_L;
            case 0x4D: return KeyEvent.VK_M;
            case 0x4E: return KeyEvent.VK_N;
            case 0x4F: return KeyEvent.VK_O;
            case 0x50: return KeyEvent.VK_P;
            case 0x51: return KeyEvent.VK_Q;
            case 0x52: return KeyEvent.VK_R;
            case 0x53: return KeyEvent.VK_S;
            case 0x54: return KeyEvent.VK_T;
            case 0x55: return KeyEvent.VK_U;
            case 0x56: return KeyEvent.VK_V;
            case 0x57: return KeyEvent.VK_W;
            case 0x58: return KeyEvent.VK_X;
            case 0x59: return KeyEvent.VK_Y;
            case 0x5A: return KeyEvent.VK_Z;
            case 0x5B: return KeyEvent.VK_OPEN_BRACKET;
            case 0x5C: return KeyEvent.VK_BACK_SLASH;
            case 0x5D: return KeyEvent.VK_CLOSE_BRACKET;
            case 0x5E: return KeyEvent.VK_CIRCUMFLEX;
            case 0x5F: return KeyEvent.VK_UNDERSCORE;
            case 0x60: return KeyEvent.VK_BACK_QUOTE;
            case 0x61: return KeyEvent.VK_A;
            case 0x62: return KeyEvent.VK_B;
            case 0x63: return KeyEvent.VK_C;
            case 0x64: return KeyEvent.VK_D;
            case 0x65: return KeyEvent.VK_E;
            case 0x66: return KeyEvent.VK_F;
            case 0x67: return KeyEvent.VK_G;
            case 0x68: return KeyEvent.VK_H;
            case 0x69: return KeyEvent.VK_I;
            case 0x6A: return KeyEvent.VK_J;
            case 0x6B: return KeyEvent.VK_K;
            case 0x6C: return KeyEvent.VK_L;
            case 0x6D: return KeyEvent.VK_M;
            case 0x6E: return KeyEvent.VK_N;
            case 0x6F: return KeyEvent.VK_O;
            case 0x70: return KeyEvent.VK_P;
            case 0x71: return KeyEvent.VK_Q;
            case 0x72: return KeyEvent.VK_R;
            case 0x73: return KeyEvent.VK_S;
            case 0x74: return KeyEvent.VK_T;
            case 0x75: return KeyEvent.VK_U;
            case 0x76: return KeyEvent.VK_V;
            case 0x77: return KeyEvent.VK_W;
            case 0x78: return KeyEvent.VK_X;
            case 0x79: return KeyEvent.VK_Y;
            case 0x7A: return KeyEvent.VK_Z;
            case 0x7B: return KeyEvent.VK_BRACELEFT;
            case 0x7D: return KeyEvent.VK_BRACERIGHT;
            case 0x7F: return KeyEvent.VK_DELETE;
            case 0xA1: return KeyEvent.VK_INVERTED_EXCLAMATION_MARK;
        }

        return 0;
    }

    /**
     * Converts a VK key character into it's corresponding char entry
     */
    public static
    char getFromVirtualKey(final int key) {
        switch (key) {
            case KeyEvent.VK_BACK_SPACE: return 0x08;
            case KeyEvent.VK_TAB: return 0x09;
            case KeyEvent.VK_ENTER: return 0x0a;
            case KeyEvent.VK_ESCAPE: return 0x1B;
            case KeyEvent.VK_EURO_SIGN: return 0x20AC;
            case KeyEvent.VK_SPACE: return 0x20;
            case KeyEvent.VK_EXCLAMATION_MARK: return 0x21;
            case KeyEvent.VK_QUOTEDBL: return 0x22;
            case KeyEvent.VK_NUMBER_SIGN: return 0x23;
            case KeyEvent.VK_DOLLAR: return 0x24;
            case KeyEvent.VK_AMPERSAND: return 0x26;
            case KeyEvent.VK_QUOTE: return 0x27;
            case KeyEvent.VK_LEFT_PARENTHESIS: return 0x28;
            case KeyEvent.VK_RIGHT_PARENTHESIS: return 0x29;
            case KeyEvent.VK_ASTERISK: return 0x2A;
            case KeyEvent.VK_PLUS: return 0x2B;
            case KeyEvent.VK_COMMA: return 0x2C;
            case KeyEvent.VK_MINUS: return 0x2D;
            case KeyEvent.VK_PERIOD: return 0x2E;
            case KeyEvent.VK_SLASH: return 0x2F;
            case KeyEvent.VK_0: return 0x30;
            case KeyEvent.VK_1: return 0x31;
            case KeyEvent.VK_2: return 0x32;
            case KeyEvent.VK_3: return 0x33;
            case KeyEvent.VK_4: return 0x34;
            case KeyEvent.VK_5: return 0x35;
            case KeyEvent.VK_6: return 0x36;
            case KeyEvent.VK_7: return 0x37;
            case KeyEvent.VK_8: return 0x38;
            case KeyEvent.VK_9: return 0x39;
            case KeyEvent.VK_COLON: return 0x3A;
            case KeyEvent.VK_SEMICOLON: return 0x3B;
            case KeyEvent.VK_LESS: return 0x3C;
            case KeyEvent.VK_EQUALS: return 0x3D;
            case KeyEvent.VK_GREATER: return 0x3E;
            case KeyEvent.VK_AT: return 0x40;
            case KeyEvent.VK_A: return 0x41;
            case KeyEvent.VK_B: return 0x42;
            case KeyEvent.VK_C: return 0x43;
            case KeyEvent.VK_D: return 0x44;
            case KeyEvent.VK_E: return 0x45;
            case KeyEvent.VK_F: return 0x46;
            case KeyEvent.VK_G: return 0x47;
            case KeyEvent.VK_H: return 0x48;
            case KeyEvent.VK_I: return 0x49;
            case KeyEvent.VK_J: return 0x4A;
            case KeyEvent.VK_K: return 0x4B;
            case KeyEvent.VK_L: return 0x4C;
            case KeyEvent.VK_M: return 0x4D;
            case KeyEvent.VK_N: return 0x4E;
            case KeyEvent.VK_O: return 0x4F;
            case KeyEvent.VK_P: return 0x50;
            case KeyEvent.VK_Q: return 0x51;
            case KeyEvent.VK_R: return 0x52;
            case KeyEvent.VK_S: return 0x53;
            case KeyEvent.VK_T: return 0x54;
            case KeyEvent.VK_U: return 0x55;
            case KeyEvent.VK_V: return 0x56;
            case KeyEvent.VK_W: return 0x57;
            case KeyEvent.VK_X: return 0x58;
            case KeyEvent.VK_Y: return 0x59;
            case KeyEvent.VK_Z: return 0x5A;
            case KeyEvent.VK_OPEN_BRACKET: return 0x5B;
            case KeyEvent.VK_BACK_SLASH: return 0x5C;
            case KeyEvent.VK_CLOSE_BRACKET: return 0x5D;
            case KeyEvent.VK_CIRCUMFLEX: return 0x5E;
            case KeyEvent.VK_UNDERSCORE: return 0x5F;
            case KeyEvent.VK_BACK_QUOTE: return 0x60;
//            case KeyEvent.VK_A: return 0x61;
//            case KeyEvent.VK_B: return 0x62;
//            case KeyEvent.VK_C: return 0x63;
//            case KeyEvent.VK_D: return 0x64;
//            case KeyEvent.VK_E: return 0x65;
//            case KeyEvent.VK_F: return 0x66;
//            case KeyEvent.VK_G: return 0x67;
//            case KeyEvent.VK_H: return 0x68;
//            case KeyEvent.VK_I: return 0x69;
//            case KeyEvent.VK_J: return 0x6A;
//            case KeyEvent.VK_K: return 0x6B;
//            case KeyEvent.VK_L: return 0x6C;
//            case KeyEvent.VK_M: return 0x6D;
//            case KeyEvent.VK_N: return 0x6E;
//            case KeyEvent.VK_O: return 0x6F;
//            case KeyEvent.VK_P: return 0x70;
//            case KeyEvent.VK_Q: return 0x71;
//            case KeyEvent.VK_R: return 0x72;
//            case KeyEvent.VK_S: return 0x73;
//            case KeyEvent.VK_T: return 0x74;
//            case KeyEvent.VK_U: return 0x75;
//            case KeyEvent.VK_V: return 0x76;
//            case KeyEvent.VK_W: return 0x77;
//            case KeyEvent.VK_X: return 0x78;
//            case KeyEvent.VK_Y: return 0x79;
//            case KeyEvent.VK_Z: return 0x7A;
            case KeyEvent.VK_BRACELEFT: return 0x7B;
            case KeyEvent.VK_BRACERIGHT: return 0x7D;
            case KeyEvent.VK_DELETE: return 0x7F;
            case KeyEvent.VK_INVERTED_EXCLAMATION_MARK: return 0xA1;
        }

        return 0;
    }
}
