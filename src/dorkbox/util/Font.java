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
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;

/**
 * Java Font utilities
 */
@SuppressWarnings("unused")
public
class Font {
    /** Default location where all the fonts are stored */
    @Property
    public static String FONTS_LOCATION = "resources/fonts";


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

                        java.awt.Font newFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, is);
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
    java.awt.Font parseFont(final String fontInfo) {
        try {
            final int sizeIndex = fontInfo.lastIndexOf(" ");

            String size = fontInfo.substring(sizeIndex + 1);

            // hint is at most 6 (ITALIC) before sizeIndex - we can use this to our benefit.
            int styleIndex = fontInfo.indexOf(" ", sizeIndex - 7);
            String styleString = fontInfo.substring(styleIndex + 1, sizeIndex);
            int style = java.awt.Font.PLAIN;

            if (styleString.equalsIgnoreCase("bold")) {
                style = java.awt.Font.BOLD;
            }
            else if (styleString.equalsIgnoreCase("italic")) {
                style = java.awt.Font.ITALIC;
            }

            String fontName = fontInfo.substring(0, styleIndex);

            // this can be WRONG, in which case it will just error out
            //noinspection MagicConstant
            return new java.awt.Font(fontName, style, Integer.parseInt(size));
        } catch (Exception e) {
            throw new RuntimeException("Unable to load font info from '" + fontInfo + "'", e);
        }
    }


    /**
     * Gets the correct font for a specified pixel height. This measures the maximum font height possible for the specified font. This
     * can be different than the alpha-numeric height.
     *
     * @param font the font we are checking
     * @param height the height in pixels we want to get as close as possible to
     *
     * @return the font (derived from the specified font) that is as close as possible to the requested height. If our font-size is less
     *          than the height, then the approach is from the low size (so the returned font will always fit inside the box)
     */
    public static
    java.awt.Font getFontForSpecificHeight(final java.awt.Font font, final int height) {
        int size = font.getSize();
        Boolean lastAction = null;

        while (true) {
            java.awt.Font fontCheck = new java.awt.Font(font.getName(), java.awt.Font.PLAIN, size);
            int maxFontHeight = getMaxFontHeight(fontCheck);

            if (maxFontHeight < height && lastAction != Boolean.FALSE) {
                size++;
                lastAction = Boolean.TRUE;
            } else if (maxFontHeight > height && lastAction != Boolean.TRUE) {
                size--;
                lastAction = Boolean.FALSE;
            } else {
                // either we are the exact size, or we are ONE font size to big/small (depending on what our initial guess was)
                return fontCheck;
            }
        }
    }

    /**
     * Gets the specified font height for a specific string, as rendered on the screen.
     *
     * @param font the font to use for rendering the string
     * @param string the string used to get the height
     *
     * @return the height of the string
     */
    public static
    int getFontHeight(final java.awt.Font font, final String string) {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = image.createGraphics();
        FontRenderContext frc = g.getFontRenderContext();

        GlyphVector gv = font.createGlyphVector(frc, string);
        int height = gv.getPixelBounds(null, 0, 0).height;
        g.dispose();

        return height;
    }

    /**
     * Gets the maximum font height used by alpha-numeric characters ONLY, as recorded by the font.
     */
    public static
    int getAlphaNumericFontHeight(final java.awt.Font font) {
        // Because font metrics is based on a graphics context, we need to create a small, temporary image to determine the width and height
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        FontMetrics metrics = g.getFontMetrics(font);
        int height = metrics.getAscent() + metrics.getDescent();
        g.dispose();

        return height;
    }

    /**
     * Gets the maximum font height used by of ALL characters, as recorded by the font.
     */
    public static
    int getMaxFontHeight(final java.awt.Font font) {
        // Because font metrics is based on a graphics context, we need to create a small, temporary image to determine the width and height
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        FontMetrics metrics = g.getFontMetrics(font);
        int height = metrics.getMaxAscent() + metrics.getMaxDescent();

        g.dispose();

        return height;
    }

    /**
     * Gets the specified text (with a font) and as an image
     *
     * @param font the specified font to render the image
     * @return a BufferedImage of the specified text, font, and color
     */
    public static
    BufferedImage getFontAsImage(final java.awt.Font font, String text, Color foregroundColor) {
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
}
