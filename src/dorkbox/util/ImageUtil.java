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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;

@SuppressWarnings("WeakerAccess")
public
class ImageUtil {

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

        String extension = FileUtil.getExtension(fileName);
        if (extension.isEmpty()) {
            extension = "png"; // made up
        }

        // now have to resize this file.
        File newFile = new File(OS.TEMP_DIR, "temp_resize." + extension).getAbsoluteFile();
        Image image;

        // is file sitting on drive
        File iconTest = new File(fileName);
        if (iconTest.isFile() && iconTest.canRead()) {
            final String absolutePath = iconTest.getAbsolutePath();

            // resize the image, keep aspect
            image = new ImageIcon(absolutePath).getImage();
        }
        else {
            // suck it out of a URL/Resource (with debugging if necessary)
            final URL systemResource = LocationResolver.getResource(fileName);

            // resize the image, keep aspect
            image = new ImageIcon(systemResource).getImage();
        }

        // have to do this twice, so that it will finish loading the image (weird callback stuff is required if we don't do this)
        image = new ImageIcon(image).getImage();

        int height = image.getHeight(null);
        int width = image.getWidth(null);

        if (width > height) {
            // fit on the width
            image = image.getScaledInstance(size, -1, Image.SCALE_SMOOTH);
        } else {
            // fit on the height
            image = image.getScaledInstance(-1, size, Image.SCALE_SMOOTH);
        }

        image.flush();

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

        // now write out the new one
        BufferedImage bufferedImage = getSquareBufferedImage(image);
        ImageIO.write(bufferedImage, extension, newFile);

        return newFile.getAbsolutePath();
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static
    File getTransparentImage(final int size, final File fileToUse) throws IOException {
        if (fileToUse.canRead() && fileToUse.isFile()) {
            return fileToUse.getAbsoluteFile();
        }

        // make sure the directory exists
        fileToUse.getParentFile().mkdirs();

        final BufferedImage image = getTransparentImageAsBufferedImage(size);
        ImageIO.write(image, "png", fileToUse);
        return fileToUse.getAbsoluteFile();
    }

    @SuppressWarnings("WeakerAccess")
    public static
    BufferedImage getTransparentImageAsBufferedImage(final int size) {
        final BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(new Color(0, 0, 0, 0));
        g2d.fillRect(0, 0, size, size);
        g2d.dispose();

        return image;
    }


    /**
     * This will always return a square image, with whatever value is smaller to have padding (so it will be centered)
     *
     * @return the image as a SQUARE Buffered Image
     */
    public static
    BufferedImage getSquareBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }

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

        Graphics2D bGr = bimage.createGraphics();
        bGr.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING,
                                                 RenderingHints.VALUE_RENDER_QUALITY));
        bGr.drawImage(image, paddingX, paddingY, null);
        bGr.dispose();

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

        Graphics2D bGr = bimage.createGraphics();
        bGr.addRenderingHints(new RenderingHints(RenderingHints.KEY_RENDERING,
                                                 RenderingHints.VALUE_RENDER_QUALITY));
        bGr.drawImage(image, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
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

    /**
     * Because of the way image loading works in Java, if one wants to IMMEDIATELY get a fully loaded image, one must resort to "hacks"
     * by loading the image twice.
     *
     * @param image the image you want load immediately
     *
     * @return a fully loaded image
     */
    public static
    Image getImageImmediate(final Image image) {
        // have to do this twice, so that it will finish loading the image (weird callback stuff is required if we don't do this)
        image.flush();

        final Image loadedImage = new ImageIcon(image).getImage();
        loadedImage.flush();

        return loadedImage;
    }
}
