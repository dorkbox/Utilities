/*
 * Copyright 2017 dorkbox, llc
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
package dorkbox.jna.windows;

import static com.sun.jna.platform.win32.WinDef.HBITMAP;
import static com.sun.jna.platform.win32.WinDef.HDC;
import static dorkbox.jna.windows.User32.User32;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.ptr.PointerByReference;

public class HBITMAPWrap extends HBITMAP {

    private static final Object lockObject = new Object();

    // NOTE: This is a field (instead of private) so that GC does not try to collect this object
    private HBITMAP bitmap;

    // https://github.com/twall/jna/blob/master/contrib/alphamaskdemo/com/sun/jna/contrib/demo/AlphaMaskDemo.java
    private static
    HBITMAP createBitmap(BufferedImage image) {
        int w = image.getWidth(null);
        int h = image.getHeight(null);

        // all sorts of issues occur if this is called quickly from different threads!
        synchronized(lockObject) {
            HDC screenDC = User32.GetDC(null);
            HDC memDC = GDI32.CreateCompatibleDC(screenDC);
            HBITMAP hBitmap = null;

            try {
                BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics2D g = (Graphics2D) buf.getGraphics();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
                g.drawImage(image, 0, 0, w, h, null);

                WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
                bmi.bmiHeader.biWidth = w;
                bmi.bmiHeader.biHeight = h;
                bmi.bmiHeader.biPlanes = 1;
                bmi.bmiHeader.biBitCount = 32;
                bmi.bmiHeader.biCompression = WinGDI.BI_RGB;
                bmi.bmiHeader.biSizeImage = w * h * 4;

                Memory memory = new Memory(w*h*32*4);
                PointerByReference pointerRef = new PointerByReference(memory);
                hBitmap = GDI32.CreateDIBSection(memDC, bmi, WinGDI.DIB_RGB_COLORS, pointerRef, null, 0);
                Pointer pointerToBits = pointerRef.getValue();

                if (pointerToBits == null) {
                    // the bitmap was invalid
                    LoggerFactory.getLogger(HBITMAPWrap.class).error("The image was invalid", Kernel32Util.getLastErrorMessage());
                }
                else {
                    Raster raster = buf.getData();
                    int[] pixel = new int[4];
                    int[] bits = new int[w * h];
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            raster.getPixel(x, h - y - 1, pixel);
                            int red = (pixel[2] & 0xFF) << 0;
                            int green = (pixel[1] & 0xFF) << 8;
                            int blue = (pixel[0] & 0xFF) << 16;
                            int alpha = (pixel[3] & 0xFF) << 24;
                            bits[x + y * w] = alpha | red | green | blue;
                        }
                    }
                    pointerToBits.write(0, bits, 0, bits.length);
                }

                return hBitmap;
            } finally {
                User32.ReleaseDC(null, screenDC);
                GDI32.DeleteDC(memDC);
            }
        }
    }

    BufferedImage img;

    public HBITMAPWrap(BufferedImage img) {
        bitmap = createBitmap(img);
        setPointer(bitmap.getPointer());

        this.img = img;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public void close() {
        if (Pointer.nativeValue(getPointer()) != 0) {
            GDI32.DeleteObject(this);
            setPointer(new Pointer(0));
            bitmap = null;
        }
    }

    public BufferedImage getImage() {
        return img;
    }
}
