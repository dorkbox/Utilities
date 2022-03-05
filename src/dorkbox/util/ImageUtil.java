/*
 * Copyright 2016 dorkbox, llc
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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.slf4j.LoggerFactory;

import dorkbox.os.OS;

@SuppressWarnings("WeakerAccess")
public
class ImageUtil {

    /**
     * @return returns an image, where the aspect ratio is kept, but the maximum size is maintained.
     */
    public static
    BufferedImage clampMaxImageSize(final BufferedImage image, final int size) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);

        if (width <= size && height <= size) {
            return image;
        }

        // scale width/height
        if (width > size) {
            double scaleRatio = (double) size / (double) width;
            width = size;
            height = (int) (height * scaleRatio);
        }

        if (height > size) {
            double scaleRatio = (double) size / (double) height;
            height = size;
            width = (int) (width * scaleRatio);
        }

        int type = image.getType();
        if (type == 0) {
            type = BufferedImage.TYPE_INT_ARGB;
        }

        BufferedImage resizedImage = new BufferedImage(width, height, type);
        Graphics2D g = resizedImage.createGraphics();
        g.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();

        return resizedImage;
    }

    /**
     * There are issues with scaled images on Windows. This correctly scales the image.
     */
    public static
    BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        int originalHeight = originalImage.getHeight();
        int originalWidth = originalImage.getWidth();
        double ratio = (double) originalWidth / (double) originalHeight;

        if (width == -1 && height == -1) {
            // no resizing, so just use the original size.
            width = originalWidth;
            height = originalHeight;
        }
        else if (width == -1) {
            width = (int) (height * ratio);
        }
        else if (height == -1) {
            height = (int) (width / ratio);
        }


        int type = originalImage.getType();
        if (type == 0) {
            type = BufferedImage.TYPE_INT_ARGB;
        }

        BufferedImage resizedImage = new BufferedImage(width, height, type);
        Graphics2D g = resizedImage.createGraphics();
        g.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();

        return resizedImage;
    }

    /**
     * Resizes the image, as either a a FILE on disk, or as a RESOURCE name, and saves the new size as a file on disk. This new file will
     * replace any other file with the same name.
     *
     * @return the file string on disk that is the resized icon
     */
    public static
    String resizeFileOrResource(final int size, final String fileName) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(fileName);

        Dimension imageSize = getImageSize(fileInputStream);
        //noinspection NumericCastThatLosesPrecision
        if (size == ((int) imageSize.getWidth()) && size == ((int) imageSize.getHeight())) {
            // we can reuse this file.
            return fileName;
        }

        // have to resize the file (and return the new path)

        String extension = FileUtil.INSTANCE.getExtension(fileName);
        if (extension.isEmpty()) {
            extension = "png"; // made up
        }

        // now have to resize this file.
        File newFile = new File(OS.INSTANCE.getTEMP_DIR(), "temp_resize." + extension).getAbsoluteFile();
        Image image;

        // is file sitting on drive
        File iconTest = new File(fileName);
        if (iconTest.isFile() && iconTest.canRead()) {
            final String absolutePath = iconTest.getAbsolutePath();
            image = new ImageIcon(absolutePath).getImage();
        }
        else {
            // suck it out of a URL/Resource (with debugging if necessary)
            final URL systemResource = LocationResolver.getResource(fileName);
            image = new ImageIcon(systemResource).getImage();
        }

        ImageUtil.waitForImageLoad(image);

        // make whatever dirs we need to.
        boolean mkdirs = newFile.getParentFile()
                                .mkdirs();

        if (!mkdirs) {
            throw new IOException("Unable to create directories for " + newFile.getParentFile());
        }

        // if it's already there, we have to delete it
        boolean delete = newFile.delete();
        if (!delete) {
            throw new IOException("Temporary file already in use, cannot delete it " + newFile);
        }

        // the smaller dimension have padding, so the larger dimension is the size of this image.
        BufferedImage bufferedImage = getSquareBufferedImage(image);

        // now write out the new one
        ImageIO.write(bufferedImage, extension, newFile);

        return newFile.getAbsolutePath();
    }


    /**
     * Creates an image of the specified size, and saves the PNG to disk
     *
     * @param size the size of the image to create
     * @param color the color to use. NULL to create a transparent image
     *
     * @return the PNG File output the created image (size + color specified)
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static
    File createImage(final int size, final File fileToUse, final Color color) throws IOException {
        if (fileToUse.canRead() && fileToUse.isFile()) {
            return fileToUse.getAbsoluteFile();
        }

        // make sure the directory exists
        fileToUse.getParentFile().mkdirs();

        final BufferedImage image = createImageAsBufferedImage(size, color);
        ImageIO.write(image, "png", fileToUse);
        return fileToUse.getAbsoluteFile();
    }

    /**
     * Creates an image of the specified size.
     *
     * @param size the size of the image to create
     * @param color the color to use. NULL to create a transparent image
     *
     * @return a BufferedImage of the size + color specified.
     */
    @SuppressWarnings("WeakerAccess")
    public static
    BufferedImage createImageAsBufferedImage(final int size, final Color color) {
        return createImageAsBufferedImage(size, size, color);
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
    @SuppressWarnings("WeakerAccess")
    public static
    BufferedImage createImageAsBufferedImage(final int width, final int height, final Color color) {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        if (color == null) {
            g2d.setColor(new Color(0, 0, 0, 0));
        } else {
            g2d.setColor(color);
        }
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();

        return image;
    }

    /**
     * This will always return a square image, with whatever value is smaller to have padding (so it will be centered), and the larger
     * dimension will be the size of the image.
     *
     * @return the image as a SQUARE Buffered Image
     */
    public static
    BufferedImage getSquareBufferedImage(Image image) {
        int width = image.getWidth(null);
        int height = image.getHeight(null);

        int paddingX = 0;
        int paddingY = 0;

        int size = width;

        if (width < height) {
            size = height;
            paddingX = (height - width) / 2;
        } else {
            paddingY = (width - height) / 2;
        }

        BufferedImage bimage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = bimage.createGraphics();
        g.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        g.drawImage(image, paddingX, paddingY, null);
        g.dispose();

        // Return the buffered image
        return bimage;
    }

    /**
     * @return the image, unmodified, as a Buffered Image
     */
    public static
    BufferedImage getBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

        BufferedImage bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = bimage.createGraphics();
        g.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        g.drawImage(image, 0, 0, null);
        g.dispose();

        // Return the buffered image
        return bimage;
    }

    /**
     * @return the icon, unmodified, as a Buffered Image
     */
    public static
    BufferedImage getBufferedImage(Icon icon) {
        if (icon instanceof BufferedImage) {
            return (BufferedImage) icon;
        }

        BufferedImage bimage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = bimage.createGraphics();
        g.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        icon.paintIcon(null, g, 0, 0);
        g.dispose();

        return bimage;
    }

    /**
     * Converts an image to a byte array
     *
     * @return the PNG File output the created buffered image, as a byte array
     */
    public static
    byte[] toBytes(final BufferedImage image) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Reads the image size information from the specified file, without loading the entire file.
     *
     * @param fileStream the input stream of the file
     *
     * @return the image size dimensions. IOException if it could not be read
     */
    public static
    Dimension getImageSize(InputStream fileStream) throws IOException {
        ImageInputStream in = null;
        ImageReader reader = null;
        try {
            // This will ONLY work for File, InputStream, and RandomAccessFile
            in = ImageIO.createImageInputStream(fileStream);

            final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                reader = readers.next();
                reader.setInput(in);

                return new Dimension(reader.getWidth(0), reader.getHeight(0));
            }
        } finally {
            // `ImageInputStream` is not a closeable in 1.6, so we do this manually.
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }

            if (reader != null) {
                reader.dispose();
            }
        }

        throw new IOException("Unable to read file inputStream for image size data.");
    }


    private static final Object mediaTrackerLock = new Object();
    private static final AtomicInteger imageTrackerIndex = new AtomicInteger(0);
    private static MediaTracker tracker = null;

    /**
     * Wait until the image is fully loaded and then init the graphics.
     *
     * @param image the image you want load immediately
     */
    public static
    void waitForImageLoad(final Image image) {
        int imageId = imageTrackerIndex.getAndIncrement();

        // make sure the image if fully loaded
        synchronized (mediaTrackerLock) {
            if (tracker == null) {
                tracker = new MediaTracker(new Component() {});
            }

            tracker.addImage(image, imageId);
        }

        try {
            tracker.waitForID(imageId);
            if (tracker.isErrorID(imageId)) {
                LoggerFactory.getLogger(ImageUtil.class).error("Error loading image!");
            }
        }
        catch (InterruptedException ignored) {

        } finally {
            synchronized (mediaTrackerLock) {
                tracker.removeImage(image);
            }
        }
    }
}
