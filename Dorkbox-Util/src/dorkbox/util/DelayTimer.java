/*
 * Copyright 2010 dorkbox, llc
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

import java.util.Timer;
import java.util.TimerTask;

public
class DelayTimer {
    private final String name;
    private final boolean isDaemon;
    private final Runnable listener;
    private volatile Timer timer;
    private long delay;

    public
    DelayTimer(Runnable listener) {
        this(null, true, listener);
    }

    /**
     * Sometimes you want to make sure that this timer will complete, even if the calling thread has terminated.
     *
     * @param name     the name of the thread (if you want to specify one)
     * @param isDaemon true if you want this timer to be run on a daemon thread
     * @param listener the callback listener to execute
     */
    public
    DelayTimer(String name, boolean isDaemon, Runnable listener) {
        this.name = name;
        this.listener = listener;
        this.isDaemon = isDaemon;
    }

    /**
     * @return true if this timer is still waiting to run.
     */
    public synchronized
    boolean isWaiting() {
        return this.timer != null;
    }

    /**
     * Cancel the delay timer!
     */
    public synchronized
    void cancel() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer.purge();
            this.timer = null;
        }
    }

    /**
     * @param delay milliseconds to wait
     */
    public synchronized
    void delay(long delay) {
        this.delay = delay;
        cancel();

        if (delay > 0) {
            if (this.name != null) {
                this.timer = new Timer(this.name, this.isDaemon);
            }
            else {
                this.timer = new Timer(this.isDaemon);
            }

            TimerTask t = new TimerTask() {
                @Override
                public
                void run() {
                    // timer can change if the callback calls delay() or cancel()
                    Timer origTimer = DelayTimer.this.timer;

                    DelayTimer.this.listener.run();

                    if (origTimer != null) {
                        origTimer.cancel();
                        origTimer.purge();

                        if (origTimer == DelayTimer.this.timer) {
                            DelayTimer.this.timer = null;
                        }
                    }
                }
            };
            this.timer.schedule(t, delay);
        }
        else {
            this.listener.run();
            this.timer = null;
        }
    }

    public synchronized
    long getDelay() {
        return this.delay;
    }
}
