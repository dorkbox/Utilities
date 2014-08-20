package dorkbox.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class CountingLatch {
    /**
     * Synchronization control for CountingLatch. Uses AQS state to represent
     * count.
     */
    private static final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -2911206339865903403L;

        private Sync() {}

        private Sync(final int initialState) {
            setState(initialState);
        }

        int getCount() {
            return getState();
        }

        @Override
        protected int tryAcquireShared(final int acquires) {
            return getState() == 0 ? 1 : -1;
        }

        @Override
        protected boolean tryReleaseShared(final int delta) {
            // Decrement count; signal when transition to zero
            for (;;) {
                final int c = getState();
                final int nextc = c + delta;
                if (nextc < 0) {
                    return false;
                }
                if (compareAndSetState(c, nextc)) {
                    return nextc == 0;
                }
            }
        }
    }

    private final Sync sync;

    public CountingLatch() {
        this.sync = new Sync();
    }

    public CountingLatch(final int initialCount) {
        this.sync = new Sync(initialCount);
    }

    public void increment() {
        this.sync.releaseShared(1);
    }

    public int getCount() {
        return this.sync.getCount();
    }

    public void decrement() {
        this.sync.releaseShared(-1);
    }

    public void await() throws InterruptedException {
        this.sync.acquireSharedInterruptibly(1);
    }

    public boolean await(final long timeout) throws InterruptedException {
        return this.sync.tryAcquireSharedNanos(1, TimeUnit.MILLISECONDS.toNanos(timeout));
    }
}