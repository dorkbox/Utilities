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
package dorkbox.util;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

/**
 * A utility class to simplify processing data/work/tasks on multiple threads.
 * <p>
 * Remember that the JMM requires that empty 'synchronize' will not be optimized out by the compiler or JIT!
 * <p>
 * This isn't the FASTEST implementation, but it is relatively easy and solid. Also minimal GC through object pools
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract
class ParallelProcessor<T extends Runnable> {
    private static final Object SENTINEL = new Object[0];

    private final ArrayBlockingQueue<T> workerPool;

    private final ArrayList<Thread> threads;
    private final ArrayBlockingQueue<Object> queue;
    private final CountDownLatch latch;

    private int totalWorkload = 0;
    private AtomicInteger currentProgress = new AtomicInteger(0);

    public
    ParallelProcessor() {
        this(OS.getOptimumNumberOfThreads(), null);
    }

    public
    ParallelProcessor(final int numberOfThreads) {
        this(numberOfThreads, null);
    }

    public
    ParallelProcessor(final int numberOfThreads, final Logger logger) {
        latch = new CountDownLatch(numberOfThreads);

        workerPool = new ArrayBlockingQueue<T>(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            T e = createWorker();
            this.workerPool.add(e);
        }

        queue = new ArrayBlockingQueue<Object>(numberOfThreads * 2);
        threads = new ArrayList<Thread>(numberOfThreads);

        ThreadGroup threadGroup = new ThreadGroup(Thread.currentThread()
                                                        .getThreadGroup(), "ParallelProcessor");
        NamedThreadFactory dispatchThreadFactory = new NamedThreadFactory("Processor", threadGroup);
        for (int i = 0; i < numberOfThreads; i++) {
            Runnable runnable = new Runnable() {
                @SuppressWarnings("unchecked")
                @Override
                public
                void run() {
                    ArrayBlockingQueue<Object> queue = ParallelProcessor.this.queue;

                    Object taken = null;
                    while (true) {
                        // we want to continue, even if there is an error (until we decide to shutdown).
                        try {
                            taken = queue.take();
                            // only two types, the sentinel or the work to be done
                            if (taken == SENTINEL) {
                                latch.countDown();
                                return;
                            }

                            T work = (T) taken;

                            // this does the work, and stores the result
                            work.run();
                            workComplete(work);
                        } catch (Throwable t) {
                            if (logger != null) {
                                logger.error("Error during execution of work!", t.getMessage());
                            }
                            else {
                                String message = t.getMessage();
                                int index = message.indexOf(OS.LINE_SEPARATOR);
                                if (index > -1) {
                                    message = message.substring(0, index);
                                }
                                System.err.println("Error during execution of work! " + message);
                            }
                        } finally {
                            if (taken instanceof Runnable) {
                                currentProgress.getAndIncrement();
                                // Return object to the pool, waking the threads that have blocked during take()
                                ParallelProcessor.this.workerPool.offer((T) taken);
                            }
                        }
                    }
                }
            };


            Thread runner = dispatchThreadFactory.newThread(runnable);
            this.threads.add(runner);
        }

        for (Thread thread : threads) {
            thread.start();
        }
    }

    /**
     * Creates a worker to be placed into the worker pool. This is only called when necessary.
     */
    public abstract
    T createWorker();

    /**
     * Called each time a single piece of work (a task) is completed.
     */
    public abstract
    void workComplete(T worker);

    /**
     * Returns true if there are workers immediately available for work.
     */
    public
    boolean hasAvailableWorker() {
        return !this.workerPool.isEmpty();
    }

    /**
     * Gets the next available worker, blocks until a worker is available.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public
    T nextWorker() throws InterruptedException {
        return this.workerPool.take();
    }

    /**
     * Queues task to be completed
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public
    void queueTask(final T work) throws InterruptedException {
        queue.put(work);
    }

    /**
     * Notifies the threads that no more work will be queued after this is called.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public
    void doneQueueingTasks() throws InterruptedException {
        for (int i = 0; i < threads.size(); i++) {
            // this tells out threads that we have finished work and can exit
            queue.put(SENTINEL);
        }
    }

    /**
     * Waits for the results to finish processing. No more work should be queued after this is called.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public
    void waitUntilDone() throws InterruptedException {
        doneQueueingTasks();

        latch.await();
    }

    /**
     * Sets the total amount of work to be performed. Also resets the count for the amount of work done.
     */
    public
    void setTotalWorkload(int totalWorkload) {
        currentProgress.set(0);
        this.totalWorkload = totalWorkload;
    }

    /**
     * Gets the amount of progress made, between 0-1
     * <p>
     * If this returns 0, it is safe to call {@link ParallelProcessor#waitUntilDone()}. It will block, but only until the processing
     * threads shutdown (which is quick)
     */
    public
    float getProgress() {
        int i = currentProgress.get();

        if (i == 0) {
            return 0.0f;
        }
        if (i == totalWorkload) {
            return 1.0f;
        }

        return (float) i / (float) totalWorkload;
    }
}
