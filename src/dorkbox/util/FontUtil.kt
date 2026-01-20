/*
 * Copyright 2026 dorkbox, llc
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

import dorkbox.os.OS.getProperty
import java.awt.*
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.InputStream
import java.net.URISyntaxException

/**
 * Java Font utilities
 */
@Suppress("unused")
object FontUtil {
    /**
     * Gets the version number.
     */
    val version = Sys.version

    /** Default location where all the fonts are stored  */
    @Volatile
    var FONTS_LOCATION = getProperty(FontUtil::class.java.canonicalName + ".FONTS_LOCATION", "resources/fonts")

    /** All of the fonts in the [.FONTS_LOCATION] will be loaded by the Font manager  */
    fun loadAllFonts() {
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val fonts = LocationResolver.getResources(FONTS_LOCATION) ?: return
        if (fonts.hasMoreElements()) {
            // skip the FIRST one, since we always know that the first one is the directory we asked for
            fonts.nextElement()
            while (fonts.hasMoreElements()) {
                val url = fonts.nextElement()
                var `is`: InputStream? = null
                try {
                    val path = url.toURI().path

                    // only support TTF and OTF fonts (java 7+).
                    if (path.endsWith(".ttf") || path.endsWith(".otf")) {
                        `is` = url.openStream()
                        val newFont = Font.createFont(Font.TRUETYPE_FONT, `is`)
                        // fonts that ALREADY exist are not re-registered
                        ge.registerFont(newFont)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                } catch (e: FontFormatException) {
                    e.printStackTrace()
                } finally {
                    if (`is` != null) {
                        try {
                            `is`.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets (or creates) a Font based on a specific system property. Remember: the FontManager caches system/loaded fonts, so we don't need
     * to ALSO cache them as well. see: https://stackoverflow.com/questions/6102602/java-awt-is-font-a-lightweight-object
     *
     *
     * Also remember that if requesting a BOLD hint for a font, the system will look for a font that is BOLD. If none are found, it
     * will then apply transforms to the specified font to create a font that is bold. Specifying a bold name AND a bold hint will not
     * "double bold" the font
     *
     *
     * For example:
     *
     *
     *
     * Font titleTextFont = FontUtil.parseFont("Source Code Pro Bold 16");
     *
     * @param fontInfo This is the font "name style size", as a string. For example "Source Code Pro Bold BOLD 16"
     *
     * @return the specified font
     */
    fun parseFont(fontInfo: String): Font {
        return try {
            val sizeIndex = fontInfo.lastIndexOf(" ")
            val size = fontInfo.substring(sizeIndex + 1)

            // hint is at most 6 (ITALIC) before sizeIndex - we can use this to our benefit.
            val styleIndex = fontInfo.indexOf(" ", sizeIndex - 7)
            val styleString = fontInfo.substring(styleIndex + 1, sizeIndex)
            var style = Font.PLAIN
            if (styleString.equals("bold", ignoreCase = true)) {
                style = Font.BOLD
            } else if (styleString.equals("italic", ignoreCase = true)) {
                style = Font.ITALIC
            }
            val fontName = fontInfo.substring(0, styleIndex)

            // this can be WRONG, in which case it will just error out
            Font(fontName, style, size.toInt())
        } catch (e: Exception) {
            throw RuntimeException("Unable to load font info from '$fontInfo'", e)
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
     * than the height, then the approach is from the low size (so the returned font will always fit inside the box)
     */
    fun getFontForSpecificHeight(font: Font, height: Int): Font {
        var size = font.size
        var lastAction: Boolean? = null
        while (true) {
            val fontCheck = Font(font.name, Font.PLAIN, size)
            val maxFontHeight = getMaxFontHeight(fontCheck)
            lastAction = if (maxFontHeight < height && (lastAction == null || lastAction)) {
                size++
                true
            } else if (maxFontHeight > height && (lastAction == null || !lastAction)) {
                size--
                false
            } else {
                // either we are the exact size, or we are ONE font size to big/small (depending on what our initial guess was)
                return fontCheck
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
    fun getFontHeight(font: Font, string: String): Int {
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        val frc = g.fontRenderContext
        val gv = font.createGlyphVector(frc, string)
        val height = gv.getPixelBounds(null, 0f, 0f).height
        g.dispose()
        return height
    }

    /**
     * Gets the specified font width for a specific string, as rendered on the screen.
     *
     * @param font the font to use for rendering the string
     * @param string the string used to get the width
     *
     * @return the width of the string
     */
    fun getFontWidth(font: Font, string: String): Int {
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        val frc = g.fontRenderContext
        val gv = font.createGlyphVector(frc, string)
        val width = gv.getPixelBounds(null, 0f, 0f).width
        g.dispose()
        return width
    }

    /**
     * Gets the maximum font height used by alpha-numeric characters ONLY, as recorded by the font.
     */
    fun getAlphaNumericFontHeight(font: Font): Int {
        // Because font metrics is based on a graphics context, we need to create a small, temporary image to determine the width and height
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        val metrics = g.getFontMetrics(font)
        val height = metrics.ascent + metrics.descent
        g.dispose()
        return height
    }

    /**
     * Gets the maximum font height used by of ALL characters, as recorded by the font.
     */
    fun getMaxFontHeight(font: Font): Int {
        // Because font metrics is based on a graphics context, we need to create a small, temporary image to determine the width and height
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        val metrics = g.getFontMetrics(font)
        val height = metrics.maxAscent + metrics.maxDescent
        g.dispose()
        return height
    }

    /**
     * Gets the specified text (with a font) and as an image
     *
     * @param font the specified font to render the image
     * @return a BufferedImage of the specified text, font, and color
     */
    fun getFontAsImage(font: Font, text: String, foregroundColor: Color): BufferedImage {
        // Because font metrics is based on a graphics context, we need to create a small, temporary image to determine the width and height
        var img = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        var g2d = img.createGraphics()
        g2d.font = font

        var fm = g2d.fontMetrics
        var width = fm.stringWidth(text)
        var height = fm.height
        g2d.dispose()

        // make it square
        if (width > height) {
            height = width
        } else {
            width = height
        }

        img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        g2d = img.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE)
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

        g2d.font = font
        fm = g2d.fontMetrics
        g2d.color = foregroundColor

        // width/4 centers the text in the image
        g2d.drawString(text, width / 4.0f, fm.ascent.toFloat())
        g2d.dispose()
        return img
    }
}
