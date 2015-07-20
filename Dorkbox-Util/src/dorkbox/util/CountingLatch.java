/*
 * Copyright 2014 dorkbox, llc
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public
class CountingLatch {
    private final Sync sync;


    public
    CountingLatch() {
        this.sync = new Sync();
    }

    public
    CountingLatch(final int initialCount) {
        this.sync = new Sync(initialCount);
    }

    public
    void increment() {
        this.sync.releaseShared(1);
    }

    public
    int getCount() {
        return this.sync.getCount();
    }

    public
    void decrement() {
        this.sync.releaseShared(-1);
    }

    public
    void await() throws InterruptedException {
        this.sync.acquireSharedInterruptibly(1);
    }

    public
    boolean await(final long timeout) throws InterruptedException {
        return this.sync.tryAcquireSharedNanos(1, TimeUnit.MILLISECONDS.toNanos(timeout));
    }


    /**
     * Synchronization control for CountingLatch. Uses AQS state to represent
     * count.
     */
    private static final
    class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1L;

        private
        Sync() {
        }

        private
        Sync(final int initialState) {
            setState(initialState);
        }

        int getCount() {
            return getState();
        }

        @Override
        protected
        int tryAcquireShared(final int acquires) {
            return getState() == 0 ? 1 : -1;
        }

        @Override
        protected
        boolean tryReleaseShared(final int delta) {
            // Decrement count; signal when transition to zero
            for (; ; ) {
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
}
