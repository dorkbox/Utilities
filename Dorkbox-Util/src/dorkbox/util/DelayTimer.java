package dorkbox.util;


import java.util.Timer;
import java.util.TimerTask;


public class DelayTimer {
    public interface Callback {
        public void execute();
    }

    private final String name;
    private final Callback listener;

    private Timer timer;

    public DelayTimer(Callback listener) {
        this(null, listener);
    }

    public DelayTimer(String name, Callback listener) {
        this.name = name;
        this.listener = listener;
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
        cancel();

        if (delay > 0) {
            if (this.name != null) {
                this.timer = new Timer(this.name, true);
            } else {
                this.timer = new Timer(true);
            }


            TimerTask t = new TimerTask() {
                @Override
                public void run() {
                    DelayTimer.this.listener.execute();
                    cancel();
                }
            };
            this.timer.schedule(t, delay);
        } else {
            this.listener.execute();
            this.timer = null;
        }
    }
}
