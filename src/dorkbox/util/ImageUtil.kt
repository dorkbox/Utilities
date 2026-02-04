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

import dorkbox.os.OS
import dorkbox.util.LocationResolver.Companion.getResource
import org.slf4j.LoggerFactory
import java.awt.*
import java.awt.image.BufferedImage
import java.io.*
import java.util.concurrent.atomic.*
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.stream.ImageInputStream
import javax.swing.Icon
import javax.swing.ImageIcon

@Suppress("unused")
object ImageUtil {
    /**
     * @return returns an image, where the aspect ratio is kept, but the maximum size is maintained.
     */
    fun clampMaxImageSize(image: BufferedImage, size: Int): BufferedImage {
        var width = image.getWidth(null)
        var height = image.getHeight(null)

        if (width <= size && height <= size) {
            return image
        }

        // scale width/height
        if (width > size) {
            val scaleRatio = size.toDouble() / width.toDouble()
            width = size
            height = (height * scaleRatio).toInt()
        }

        if (height > size) {
            val scaleRatio = size.toDouble() / height.toDouble()
            height = size
            width = (width * scaleRatio).toInt()
        }

        var type = image.type
        if (type == 0) {
            type = BufferedImage.TYPE_INT_ARGB
        }

        val resizedImage = BufferedImage(width, height, type)
        val g = resizedImage.createGraphics()
        g.addRenderingHints(RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY))
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER)

        g.drawImage(image, 0, 0, width, height, null)
        g.dispose()

        return resizedImage
    }

    /**
     * There are issues with scaled images on Windows. This correctly scales the image.
     */
    fun resizeImage(originalImage: BufferedImage, width: Int, height: Int): BufferedImage {
        var width = width
        var height = height
        val originalHeight = originalImage.height
        val originalWidth = originalImage.width
        val ratio = originalWidth.toDouble() / originalHeight.toDouble()

        if (width == -1 && height == -1) {
            // no resizing, so just use the original size.
            width = originalWidth
            height = originalHeight
        }
        else if (width == -1) {
            width = (height * ratio).toInt()
        }
        else if (height == -1) {
            height = (width / ratio).toInt()
        }


        var type = originalImage.type
        if (type == 0) {
            type = BufferedImage.TYPE_INT_ARGB
        }

        val resizedImage = BufferedImage(width, height, type)
        val g = resizedImage.createGraphics()
        g.addRenderingHints(RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY))
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER)

        g.drawImage(originalImage, 0, 0, width, height, null)
        g.dispose()

        return resizedImage
    }

    /**
     * Resizes the image, as either a a FILE on disk, or as a RESOURCE name, and saves the new size as a file on disk. This new file will
     * replace any other file with the same name.
     * 
     * @return the file string on disk that is the resized icon
     */
    @Throws(IOException::class)
    fun resizeFileOrResource(size: Int, fileName: String): String {
        val fileInputStream = FileInputStream(fileName)

        val imageSize = getImageSize(fileInputStream)
        if (size == (imageSize.getWidth().toInt()) && size == (imageSize.getHeight().toInt())) {
            // we can reuse this file.
            return fileName
        }

        // have to resize the file (and return the new path)
        var extension = File(fileName).extension
        if (extension.isEmpty()) {
            extension = "png" // made up
        }

        // now have to resize this file.
        val newFile: File = File(OS.TEMP_DIR, "temp_resize.$extension").getAbsoluteFile()
        val image: Image

        // is the file sitting on the drive
        val iconTest = File(fileName)
        if (iconTest.isFile() && iconTest.canRead()) {
            val absolutePath = iconTest.absolutePath
            image = ImageIcon(absolutePath).getImage()
        }
        else {
            // suck it out of a URL/Resource (with debugging if necessary)
            val systemResource = getResource(fileName)
            image = ImageIcon(systemResource).getImage()
        }

        waitForImageLoad(image)

        // make whatever dirs we need to.
        val mkdirs = newFile.getParentFile().mkdirs()

        if (!mkdirs) {
            throw IOException("Unable to create directories for " + newFile.getParentFile())
        }

        // if it's already there, we have to delete it
        val delete = newFile.delete()
        if (!delete) {
            throw IOException("Temporary file already in use, cannot delete it $newFile")
        }

        // the smaller dimension have padding, so the larger dimension is the size of this image.
        val bufferedImage = getSquareBufferedImage(image)

        // now write out the new one
        ImageIO.write(bufferedImage, extension, newFile)

        return newFile.absolutePath
    }


    /**
     * Creates an image of the specified size, and saves the PNG to disk
     * 
     * @param size the size of the image to create
     * @param color the color to use. NULL to create a transparent image
     * 
     * @return the PNG File output the created image (size + color specified)
     */
    @Throws(IOException::class)
    fun createImage(size: Int, fileToUse: File, color: Color?): File {
        if (fileToUse.canRead() && fileToUse.isFile()) {
            return fileToUse.getAbsoluteFile()
        }

        // make sure the directory exists
        fileToUse.getParentFile().mkdirs()

        val image = createImageAsBufferedImage(size, color)
        ImageIO.write(image, "png", fileToUse)
        return fileToUse.getAbsoluteFile()
    }

    /**
     * Creates an image of the specified size.
     * 
     * @param size the size of the image to create
     * @param color the color to use. NULL to create a transparent image
     * 
     * @return a BufferedImage of the size + color specified.
     */
    fun createImageAsBufferedImage(size: Int, color: Color?): BufferedImage {
        return createImageAsBufferedImage(size, size, color)
    }

    /**
     * Creates an image of the specified size.
     * 
     * @param width the width of the image to create
     * @param height the height of the image to create
     * @param color the color to use. NULL to create a transparent image
     * 
     * @return a BufferedImage of the size + color specified.
     */
    fun createImageAsBufferedImage(width: Int, height: Int, color: Color?): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        if (color == null) {
            g2d.color = Color(0, 0, 0, 0)
        }
        else {
            g2d.color = color
        }
        g2d.fillRect(0, 0, width, height)
        g2d.dispose()

        return image
    }

    /**
     * This will always return a square image, with whatever value is smaller to have padding (so it will be centered), and the larger
     * dimension will be the size of the image.
     * 
     * @return the image as a SQUARE Buffered Image
     */
    fun getSquareBufferedImage(image: Image): BufferedImage {
        val width = image.getWidth(null)
        val height = image.getHeight(null)

        var paddingX = 0
        var paddingY = 0

        var size = width

        if (width < height) {
            size = height
            paddingX = (height - width) / 2
        }
        else {
            paddingY = (width - height) / 2
        }

        val bimage = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)

        val g = bimage.createGraphics()
        g.addRenderingHints(RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY))
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER)

        g.drawImage(image, paddingX, paddingY, null)
        g.dispose()

        // Return the buffered image
        return bimage
    }

    /**
     * @return the image, unmodified, as a Buffered Image
     */
    fun getBufferedImage(image: Image): BufferedImage {
        if (image is BufferedImage) {
            return image
        }

        val bimage = BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB)

        val g = bimage.createGraphics()
        g.addRenderingHints(RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY))
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER)

        g.drawImage(image, 0, 0, null)
        g.dispose()

        // Return the buffered image
        return bimage
    }

    /**
     * @return the icon, unmodified, as a Buffered Image
     */
    fun getBufferedImage(icon: Icon): BufferedImage {
        if (icon is BufferedImage) {
            return icon as BufferedImage
        }

        val bimage = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)

        val g = bimage.createGraphics()
        g.addRenderingHints(RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY))
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER)

        icon.paintIcon(null, g, 0, 0)
        g.dispose()

        return bimage
    }

    /**
     * Converts an image to a byte array
     * 
     * @return the PNG File output the created buffered image, as a byte array
     */
    @Throws(IOException::class)
    fun toBytes(image: BufferedImage): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        ImageIO.write(image, "PNG", byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    /**
     * Reads the image size information from the specified file, without loading the entire file.
     * 
     * @param fileStream the input stream of the file
     * 
     * @return the image size dimensions. IOException if it could not be read
     */
    @Throws(IOException::class)
    fun getImageSize(fileStream: InputStream): Dimension {
        var `in`: ImageInputStream? = null
        var reader: ImageReader? = null
        try {
            // This will ONLY work for File, InputStream, and RandomAccessFile
            `in` = ImageIO.createImageInputStream(fileStream)

            val readers = ImageIO.getImageReaders(`in`)
            if (readers.hasNext()) {
                reader = readers.next()
                reader!!.setInput(`in`)

                return Dimension(reader.getWidth(0), reader.getHeight(0))
            }
        }
        finally {
            // `ImageInputStream` is not a closeable in 1.6, so we do this manually.
            if (`in` != null) {
                try {
                    `in`.close()
                }
                catch (ignored: IOException) {
                }
            }

            if (reader != null) {
                reader.dispose()
            }
        }

        throw IOException("Unable to read file inputStream for image size data.")
    }


    private val mediaTrackerLock = Any()
    private val imageTrackerIndex = AtomicInteger(0)
    private var tracker: MediaTracker? = null

    /**
     * Wait until the image is fully loaded and then init the graphics.
     * 
     * @param image the image you want load immediately
     */
    fun waitForImageLoad(image: Image?) {
        val imageId = imageTrackerIndex.getAndIncrement()

        // make sure the image if fully loaded
        synchronized(mediaTrackerLock) {
            if (tracker == null) {
                tracker = MediaTracker(object : Component() {})
            }
            tracker!!.addImage(image, imageId)
        }

        try {
            tracker!!.waitForID(imageId)
            if (tracker!!.isErrorID(imageId)) {
                LoggerFactory.getLogger(ImageUtil::class.java).error("Error loading image!")
            }
        }
        catch (ignored: InterruptedException) {
        }
        finally {
            synchronized(mediaTrackerLock) {
                tracker!!.removeImage(image)
            }
        }
    }
}
