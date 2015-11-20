package dorkbox.util.swing;

import dorkbox.util.ActionHandlerLong;
import dorkbox.util.Property;

import javax.swing.JFrame;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;

/**
 * Loop that controls the active rendering process
 */
class ActiveRenderLoop implements Runnable {

    @Property
    /** How many frames per second we want the Swing ActiveRender thread to run at */
    public static int TARGET_FPS = 30;

    @SuppressWarnings("WhileLoopReplaceableByForEach")
    @Override
    public
    void run() {
        long lastTime = System.nanoTime();

        // 30 FPS is usually just fine. This isn't a game where we need 60+ FPS. We permit this to be changed though, just in case it is.
        final long OPTIMAL_TIME = 1000000000 / TARGET_FPS;
        Graphics graphics = null;

        while (SwingActiveRender.hasActiveRenders) {
            long now = System.nanoTime();
            long updateDeltaNanos = now - lastTime;
            lastTime = now;

            // not synchronized, because we don't care. The worst case, is one frame of animation behind.
            for (int i = 0; i < SwingActiveRender.activeRenderEvents.size(); i++) {
                ActionHandlerLong actionHandlerLong = SwingActiveRender.activeRenderEvents.get(i);

                //noinspection unchecked
                actionHandlerLong.handle(updateDeltaNanos);
            }

            for (int i = 0; i < SwingActiveRender.activeRenders.size(); i++) {
                JFrame jFrame = SwingActiveRender.activeRenders.get(i);

                final BufferStrategy buffer = jFrame.getBufferStrategy();

                // maybe the frame was closed
                if (buffer != null) {
                    try {
                        graphics = buffer.getDrawGraphics();
                        jFrame.paint(graphics);
                    } catch (IllegalStateException ignored) {
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (graphics != null) {
                            graphics.dispose();

                            // blit the back buffer to the screen
                            if (!buffer.contentsLost()) {
                                buffer.show();
                            }
                        }
                    }
                }
            }

            // Sync the display on some systems (on Linux, this fixes event queue problems)
            Toolkit.getDefaultToolkit()
                   .sync();

            try {
                // Converted to int before the division, because IDIV is
                // 1 order magnitude faster than LDIV (and int's work for us anyways)
                // see: http://www.cs.nuim.ie/~jpower/Research/Papers/2008/lambert-qapl08.pdf
                // Also, down-casting (long -> int) is not expensive w.r.t IDIV/LDIV
                //noinspection NumericCastThatLosesPrecision
                final int l = (int) (lastTime - System.nanoTime() + OPTIMAL_TIME);
                final int millis = l / 1000000;
                if (millis > 1) {
                    Thread.sleep(millis);
                }
                else {
                    // try to keep the CPU from getting slammed. We couldn't match our target FPS, so loop again
                    Thread.yield();
                }
            } catch (InterruptedException ignored) {
            }
        }
    }
}
