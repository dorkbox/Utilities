package dorkbox.util;


import java.util.Timer;
import java.util.TimerTask;


public class DelayTimer {
    public interface Callback {
        public void execute();
    }

    private final String name;
    private final boolean isDaemon;
    private final Callback listener;

    private Timer timer;
    private long delay;

    public DelayTimer(Callback listener) {
        this(null, true, listener);
    }

    /**
     * Sometimes you want to make sure that this timer will complete, even if the calling thread has terminated.
     * @param name the name of the thread (if you want to specify one)
     * @param isDaemon true if you want this timer to be run on a daemon thread
     * @param listener the callback listener to execute
     */
    public DelayTimer(String name, boolean isDaemon, Callback listener) {
        this.name = name;
        this.listener = listener;
        this.isDaemon = isDaemon;
    }

    /**
     * @return true if this timer is still waiting to run.
     */
    public synchronized boolean isWaiting() {
        return this.timer != null;
    }

    /**
     * Cancel the delay timer!
     */
    public synchronized void cancel() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer.purge();
            this.timer = null;
        }
    }

    /**
     * @param delay milliseconds to wait
     */
    public synchronized void delay(long delay) {
        this.delay = delay;
        cancel();

        if (delay > 0) {
            if (this.name != null) {
                this.timer = new Timer(this.name, this.isDaemon);
            } else {
                this.timer = new Timer(this.isDaemon);
            }

            TimerTask t = new TimerTask() {
                @Override
                public void run() {
                    DelayTimer.this.listener.execute();
                    DelayTimer.this.cancel();
                }
            };
            this.timer.schedule(t, delay);
        } else {
            this.listener.execute();
            this.timer = null;
        }
    }

    public synchronized long getDelay() {
        return this.delay;
    }
}
