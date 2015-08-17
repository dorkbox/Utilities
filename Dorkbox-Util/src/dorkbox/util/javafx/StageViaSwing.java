package dorkbox.util.javafx;

import com.sun.javafx.application.PlatformImpl;
import dorkbox.util.JavaFxUtil;
import dorkbox.util.SwingUtil;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.WritableValue;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.util.Duration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

/**
 * This class is necessary, because JavaFX stage is crap on linux. This offers sort-of the same functionality, but via swing instead.
 */
public
class StageViaSwing {
    final JFrame frame;
    final JFXPanel panel;

    private boolean inNestedEventLoop = false;
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


    private
    StageViaSwing() {
        frame = new JFrame() {

        };
        panel = new JFXPanel();
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        frame.setUndecorated(true);
        frame.add(panel);



        opacityProperty = new WritableValue<Float>() {
            @Override
            public Float getValue() {
                return frame.getOpacity();
            }

            @Override
            public void setValue(Float value) {
                frame.setOpacity(value);
            }
        };


        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                Thread thread = new Thread(() -> {
                    try {
                        // If this runs now, it will bug out, and flash on the screen before we want it to.
                        // REALLY dumb, but we have to wait for the system to draw the window and finish BEFORE we move it
                        // otherwise, it'll 'flash' onscreen because it will still be in the middle of it's initial "on-show" animation.
                        Thread.sleep(500);

                        sizeToScene();
                        SwingUtil.showOnSameScreenAsMouseCenter(frame);

                        Timeline timeline = new Timeline();
                        timeline.setCycleCount(1);
                        timeline.getKeyFrames()
                                .addAll(new KeyFrame(Duration.millis(700),
                                                     new KeyValue(opacityProperty, 1F, Interpolator.EASE_OUT)));
                        timeline.play();
                    } catch(InterruptedException ignored) {
                    }
                });
                thread.setDaemon(true);
                thread.setName("Window centering");
                thread.start();
            }
        });
    }

    public
    void setTitle(final String title) {
        frame.setTitle(title);
    }

    public
    String getTitle() {
        return frame.getTitle();
    }

    public
    void close() {
        SwingUtil.invokeAndWait(() -> frame.dispose());
        if (inNestedEventLoop) {
            com.sun.javafx.tk.Toolkit.getToolkit().exitNestedEventLoop(this, null);
        } else {
            latch.countDown();
        }
    }

    public
    void setSize(final double width, final double height) {
        SwingUtil.invokeAndWait(() -> frame.setSize((int)width, (int)height));
    }

    public
    void setResizable(final boolean resizable) {
        SwingUtil.invokeAndWait(() -> frame.setResizable(resizable));
    }

    public
    void setApplicationIcon(final java.awt.Image icon) {
        SwingUtil.invokeAndWait(() -> frame.setIconImage(icon));
    }

    private final
    CountDownLatch latch = new CountDownLatch(1);

    public
    void show() {
        SwingUtil.invokeAndWait(() -> {
            frame.setOpacity(.0f);
            frame.setSize(0, 0);

            frame.setVisible(false);
            frame.setLocation(Short.MIN_VALUE, Short.MIN_VALUE);

            // Figure out the size of everything. Because JFXPanel DOES NOT do this.
            frame.pack();
        });

        // has javafx stuff on it, must not be called on the EDT
        sizeToScene();

        SwingUtil.invokeAndWait(() -> frame.setVisible(true));
    }

    public
    void showAndWait() {
        show();

        if (Platform.isFxApplicationThread()) {
            inNestedEventLoop = true;
            com.sun.javafx.tk.Toolkit.getToolkit().enterNestedEventLoop(this);
        } else {
            try {
                latch.await();
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
    void setMinSize(final double width, final double height) {
        SwingUtil.invokeAndWait(() -> frame.setMinimumSize(new Dimension((int)width, (int)height)));
    }

    public
    void sizeToScene() {
        SwingUtil.invokeAndWait(() -> {
                frame.invalidate();
                frame.validate();
            }
        );

        // Figure out the size of everything. Because JFXPanel DOES NOT do this.
        // must be on the FX app thread
        JavaFxUtil.invokeAndWait(() -> {
            final Scene scene = panel.getScene();

            try {
                // use reflection. This is lame, but necessary. must be on the jfx thread
                method.invoke(scene);

                // must be on the EDT
                SwingUtil.invokeAndWait(() -> {
                    frame.setSize((int)scene.getWidth(), (int)scene.getHeight());
                });


            } catch (InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    }

    public
    void setScene(final Scene scene) {
        // must be on the JFX or EDT threads
        if (!Platform.isFxApplicationThread() || !EventQueue.isDispatchThread()) {
            JavaFxUtil.invokeAndWait(() -> panel.setScene(scene));
        } else {
            panel.setScene(scene);
        }
    }

    public
    WritableValue<Float> getOpacityProperty() {
        return opacityProperty;
    }
}
