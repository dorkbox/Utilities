/*
 * Copyright 2015 dorkbox, llc
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
package dorkbox.util.javafx;

import com.sun.javafx.application.PlatformImpl;
import dorkbox.util.JavaFxUtil;
import dorkbox.util.NamedThreadFactory;
import dorkbox.util.ScreenUtil;
import dorkbox.util.SwingUtil;
import javafx.application.Platform;
import javafx.beans.value.WritableValue;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * This class is necessary, because JavaFX stage is crap on linux. This offers sort-of the same functionality, but via swing instead.
 * Annoying caveat. All swing setters MUST happen on the EDT.
 */
@SuppressWarnings("unused")
public
class StageViaSwing {
    public static final Executor frameDisposer = Executors.newSingleThreadExecutor(new NamedThreadFactory("Swing Disposer",
                                                                                                          Thread.MIN_PRIORITY,
                                                                                                          true));


    final JFrame frame;
    final JFXPanel panel;

    private volatile boolean inNestedEventLoop = false;
    private final CountDownLatch showLatch = new CountDownLatch(1);
    private final CountDownLatch showAndWaitLatch = new CountDownLatch(1);

    final WritableValue<Float> opacityProperty;


    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    static
    StageViaSwing create() {
        final StageViaSwing[] returnVal = new StageViaSwing[1];

        // this MUST happen on the EDT!
        SwingUtil.invokeAndWait(() -> {
            synchronized (returnVal) {
                returnVal[0] = new StageViaSwing();
            }
        });

        synchronized (returnVal) {
            return returnVal[0];
        }
    }

    /**
     * Necessary for us to be able to size our frame based on it's content
     */
    private static Method method;
    static {
        try {
            method = Scene.class.getDeclaredMethod("preferredSize");
            method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        // make sure that javafx application thread is started
        // Note that calling PlatformImpl.startup more than once is OK
        PlatformImpl.startup(() -> {
            // No need to do anything here
        });
    }

    private volatile boolean center = false;
    private volatile double x;
    private volatile double y;
    private volatile double width;
    private volatile double height;
    private volatile boolean closing;
    private volatile boolean resizable;

    public
    void setAlwaysOnTop(final boolean alwaysOnTop) {
        frame.setAlwaysOnTop(alwaysOnTop);
    }

    interface OnShowAnimation {
    void doShow();
}

    private OnShowAnimation showAnimation = null;


    public
    void setShowAnimation(final OnShowAnimation showAnimation) {
        this.showAnimation = showAnimation;
    }

    StageViaSwing() {
        frame = new JFrame();
        panel = new JFXPanel();

//        frame.setLayout(null);
        frame.setUndecorated(true);
        frame.setOpacity(0F);
        frame.add(panel);


        opacityProperty = new WritableValue<Float>() {
            @Override
            public Float getValue() {
                return frame.getOpacity();
            }

            @Override
            public void setValue(Float value) {
                SwingUtil.invokeAndWait(() -> frame.setOpacity(value));
            }
        };


        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                if (showAnimation != null) {
//                    Thread thread = new Thread(() -> {
//                        try {
                            // If this runs now, it will bug out, and flash on the screen before we want it to.
                            // REALLY dumb, but we have to wait for the system to draw the window and finish BEFORE we move it
                            // otherwise, it'll 'flash' onscreen because it will still be in the middle of it's initial "on-show" animation.
//                            Thread.sleep(5000);

                            if (!inNestedEventLoop) {
                                renderContents();
                            } else {
                                // notify we are done showing, to prevent race conditions with the JFX app thread
                                // the show method continues
                                showLatch.countDown();
                            }
//                        } catch(InterruptedException ignored) {
//                        }
//                    });
//                    thread.setDaemon(true);
//                    thread.setName("Window centering");
//                    thread.start();
                } else if (!inNestedEventLoop) {
                    renderContents();
                } else {
                    // notify we are done showing, to prevent race conditions with the JFX app thread
                    // the show method continues
                    showLatch.countDown();
                }
            }

            private
            void renderContents() {
                sizeToScene();

                SwingUtil.invokeLater(StageViaSwing.this::recheckSize);

                if (showAnimation == null) {
                    opacityProperty.setValue(1F);
                    completeShowTransition();
                } else {
                    showAnimation.doShow();
                }
            }
        });
    }


    // absolutely stupid - swing doesn't want to be forced to a certain size, unless specific incantations are performed. These seem to work
    void recheckSize() {
        if (frame.getX() != x || frame.getY() != y || frame.getWidth() != width || frame.getHeight() != height) {
//            System.err.println("FAILED SIZE CHECK");
//            System.err.println("SIZE: " + width + " : " + height);
//            System.err.println("actual: " + frame.getWidth() + " " + frame.getHeight());

            final Dimension size = new Dimension((int) width, (int) height);
            if (!resizable) {
                frame.setMinimumSize(size);
                frame.setMaximumSize(size);
                panel.setMinimumSize(size);
                panel.setMaximumSize(size);
            }

            panel.setPreferredSize(size);
            frame.setPreferredSize(size);

            if (center) {
                // same as in screenUtils, but here we set bound instead of just location
                final Point mouseLocation = MouseInfo.getPointerInfo()
                                                     .getLocation();

                final GraphicsDevice deviceAtMouse = ScreenUtil.getGraphicsDeviceAt(mouseLocation);
                final Rectangle bounds = deviceAtMouse.getDefaultConfiguration()
                                                      .getBounds();


                panel.setBounds(bounds.x + (bounds.width / 2) - (int)width / 2,
                                bounds.y + (bounds.height / 2) - (int)height / 2,
                                (int)width,
                                (int)height);

                frame.setBounds(bounds.x + (bounds.width / 2) - (int)width / 2,
                                bounds.y + (bounds.height / 2) - (int)height / 2,
                                (int)width,
                                (int)height);
            } else {
                panel.setBounds((int) x,
                                (int) y,
                                (int) width,
                                (int) height);
                frame.setBounds((int) x,
                                (int) y,
                                (int) width,
                                (int) height);
            }

            frame.pack();
            frame.revalidate();
            frame.repaint();

//            System.err.println("recheck SIZE: " + frame.getWidth()  + " " + frame.getHeight());
//            System.err.println("recheck SIZE: " + panel.getWidth()  + " " + frame.getHeight());
        }
    }

    public final
    void completeShowTransition() {
        showLatch.countDown();
    }

    public
    void setTitle(final String title) {
        SwingUtil.invokeAndWait(() -> frame.setTitle(title));
    }

    public
    String getTitle() {
        return frame.getTitle();
    }


    public
    void close() {
        closing = true;

        // "hide" it until we can properly do so.
        SwingUtil.invokeAndWait(() -> {
            frame.setOpacity(0F);
            frame.setBounds(Short.MIN_VALUE, Short.MIN_VALUE, 0, 0);
            //noinspection deprecation
            frame.hide();
        });

        frameDisposer.execute(() -> {
            // stupid thing flashes on-screen if we run this right away...
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            SwingUtil.invokeLater(frame::dispose);
        });

        releaseLatch(showAndWaitLatch);
    }

    private
    void releaseLatch(final CountDownLatch latch) {
        if (inNestedEventLoop) {
            inNestedEventLoop = false;

            if (!Platform.isFxApplicationThread()) {
                JavaFxUtil.invokeAndWait(() -> com.sun.javafx.tk.Toolkit.getToolkit().exitNestedEventLoop(StageViaSwing.this, null));
            } else {
                com.sun.javafx.tk.Toolkit.getToolkit().exitNestedEventLoop(StageViaSwing.this, null);
            }
        } else {
            latch.countDown();
        }
    }

    public
    void setSize(final double width, final double height) {
        this.width = width;
        this.height = height;
        SwingUtil.invokeAndWait(() -> frame.setSize((int)width, (int)height));
    }

    public
    void setResizable(final boolean resizable) {
        this.resizable = resizable;
        SwingUtil.invokeAndWait(() -> frame.setResizable(resizable));
    }

    public
    void setApplicationIcon(final java.awt.Image icon) {
        SwingUtil.invokeAndWait(() -> frame.setIconImage(icon));
    }

    public
    void show(final double x, final double y) {
        // we want to make sure we go BACK to this location when we show the JFRAME on screen
        this.x = x;
        this.y = y;

        show();
    }

    public
    void showAndWait(final double x, final double y) {
        // we want to make sure we go BACK to this location when we show the JFRAME on screen
        this.x = x;
        this.y = y;

        showAndWait();
    }

    public
    void show() {
        SwingUtil.invokeAndWait(() -> {
            frame.setSize(0, 0);
            frame.setVisible(false);
            frame.setOpacity(0f);
            frame.setBounds(Short.MIN_VALUE, Short.MIN_VALUE, 0, 0);

            frame.revalidate();
            frame.repaint();
        });

            // Figure out the size of everything. Because JFXPanel DOES NOT do this.

        // wait until our show animation is complete. There is a small delay out of necessity
        // false-positive
        //noinspection Duplicates
        if (Platform.isFxApplicationThread()) {
            inNestedEventLoop = true;

            SwingUtil.invokeAndWait(() -> frame.setVisible(true));

            try {
                showLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            inNestedEventLoop = false;

            sizeToScene();

            SwingUtil.invokeAndWait(() -> {
                if (showAnimation == null) {
                    opacityProperty.setValue(1F);
                    completeShowTransition();
                } else {
                    showAnimation.doShow();
                }
            });
        } else {
            SwingUtil.invokeAndWait(() -> frame.setVisible(true));

            try {
                showLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public
    void showAndWait() {
        show();

        // false-positive
        //noinspection Duplicates
        if (Platform.isFxApplicationThread()) {
            inNestedEventLoop = true;
            com.sun.javafx.tk.Toolkit.getToolkit().enterNestedEventLoop(this);
        } else {
            try {
                showAndWaitLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public
    void initModality(final Dialog.ModalExclusionType modal) {
        // we take in the javaFX modality, and pass it on to the correct swing version
        JavaFxUtil.invokeAndWait(() -> frame.setModalExclusionType(modal));
    }

    public
    void sizeToScene() {
        SwingUtil.invokeAndWait(() -> {
            frame.revalidate();
            frame.repaint();
        });

        // Figure out the size of everything. Because JFXPanel DOES NOT do this.
        // must be on the FX app thread
        JavaFxUtil.invokeAndWait(() -> {
            final Scene scene = panel.getScene();

            try {
                // use reflection. This is lame, but necessary. must be on the jfx thread
                method.invoke(scene);

                width = scene.getWidth();
                height = scene.getHeight();
            } catch (InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });

        SwingUtil.invokeAndWait(this::recheckSize);
    }

    public
    void setScene(final Scene scene) {
        // must be on the JFX or EDT threads
        if (!Platform.isFxApplicationThread() && !EventQueue.isDispatchThread()) {
            JavaFxUtil.invokeAndWait(() -> panel.setScene(scene));
        } else {
            panel.setScene(scene);
        }
    }

    public
    WritableValue<Float> getOpacityProperty() {
        return opacityProperty;
    }

    public
    void setLocation(final double x, final double y) {
        // we want to make sure we go BACK to this location when we show the JFRAME on screen
        if (x != this.x || y != this.y) {
            this.x = x;
            this.y = y;

            if (!closing) {
                SwingUtil.invokeAndWait(() -> frame.setLocation((int)x, (int)y));
            }
        }
    }

    public
    double getX() {
        return x;
    }

    public
    double getY() {
        return y;
    }

    public
    void center() {
        this.center = true;
    }

    public
    Dimension getSize() {
        return frame.getSize();
    }
}
