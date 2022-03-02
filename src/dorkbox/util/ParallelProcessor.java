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

import dorkbox.os.OS;

/**
 * A Parallel processor to simplify processing data on multiple threads and provide back-pressure to the main thread (that
 * creates the processor and adds work to it), so that memory is constrained at the expense of CPU waiting
 * <p>
 * Remember that the JMM requires that empty 'synchronize' will not be optimized out by the compiler or JIT!
 * <p>
 * This is NOT the FASTEST implementation, but it is relatively easy and solid.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract
class ParallelProcessor<Task> {
    public
    interface Worker<Task> {
        /**
         * Runs the work.
         *
         * @return true if there was work done, otherwise false.
         */
        boolean process(Task objectToProcess);
    }

    private static final Object SENTINEL = new Object[0];

    private final int numberOfThreads;
    private final ArrayList<Thread> threads;
    private final ArrayBlockingQueue<Object> queue;
    private final CountDownLatch latch;

    private final int totalWorkload;
    private final AtomicInteger currentProgress = new AtomicInteger(0);

    /**
     * Creates a Parallel processor to simplify processing data on multiple threads and provide back-pressure to the main thread (that
     * creates the processor and adds work to it), so that memory is constrained at the expense of CPU waiting
     * <p>
     * This will not track the progress from (0-1) but instead will record the total number of processed tasks
     * This will use the OS optimum number of threads (based on the CPU core count)
     * This will not assign a logger and errors will be printed to std.err
     */
    public
    ParallelProcessor() {
        this(-1, OS.INSTANCE.getOptimumNumberOfThreads(), null);
    }

    /**
     * Creates a Parallel processor to simplify processing data on multiple threads.
     * <p>
     * This implementation will provide back-pressure to the main thread (that creates the processor and adds work to it), so
     * that memory is constrained at the expense of CPU waiting
     *
     * This will use the OS optimum number of threads (based on the CPU core count)
     * This will not assign a logger and errors will be printed to std.err
     *
     * @param totalWorkload this is the total number of elements that need to be processed. -1 to disable
     */
    public
    ParallelProcessor(final int totalWorkload) {
        this(totalWorkload, OS.INSTANCE.getOptimumNumberOfThreads(), null);
    }

    /**
     * Creates a Parallel processor to simplify processing data on multiple threads.
     * <p>
     * This implementation will provide back-pressure to the main thread (that creates the processor and adds work to it), so
     * that memory is constrained at the expense of CPU waiting
     *
     * This will not assign a logger and errors will be printed to std.err
     *
     * @param totalWorkload this is the total number of elements that need to be processed. -1 to disable
     * @param numberOfThreads this is the number of threads requested to do the work
     */
    public
    ParallelProcessor(final int totalWorkload, final int numberOfThreads) {
        this(totalWorkload, numberOfThreads, null);
    }

    /**
     * Creates a Parallel processor to simplify processing data on multiple threads.
     * <p>
     * This implementation will provide back-pressure to the main thread (that creates the processor and adds work to it), so
     * that memory is constrained at the expense of CPU waiting
     *
     * @param totalWorkload this is the total number of elements that need to be processed. -1 to disable
     * @param numberOfThreads this is the number of threads requested to do the work
     * @param logger this is the logger to report errors (can be null)
     */
    public
    ParallelProcessor(final int totalWorkload, final int numberOfThreads, final Logger logger) {
        this.totalWorkload = totalWorkload;
        this.numberOfThreads = numberOfThreads;

        latch = new CountDownLatch(this.numberOfThreads);
        queue = new ArrayBlockingQueue<Object>(numberOfThreads);

        threads = new ArrayList<Thread>(numberOfThreads);

        ThreadGroup threadGroup = new ThreadGroup(Thread.currentThread()
                                                        .getThreadGroup(), "ParallelProcessor");
        NamedThreadFactory dispatchThreadFactory = new NamedThreadFactory("Processor", threadGroup);
        for (int i = 0; i < numberOfThreads; i++) {
            java.lang.Runnable runnable = new java.lang.Runnable() {
                @SuppressWarnings("unchecked")
                @Override
                public
                void run() {
                    final ParallelProcessor<Task> processor = ParallelProcessor.this;
                    final ArrayBlockingQueue<Object> queue = processor.queue;
                    final Worker worker = createWorker();

                    Object taken;
                    while (true) {
                        // we want to continue, even if there is an error (until we decide to shutdown).
                        try {
                            taken = queue.take();

                            // only two types, the sentinel or the work to be done
                            if (taken == SENTINEL) {
                                latch.countDown();
                                return;
                            }
                        } catch (Throwable ignored) {
                            // this thread was interrupted. Shouldn't ever really happen.
                            return;
                        }

                        Task task = (Task) taken;

                        try {
                            // this does the work
                            worker.process(task);
                            workComplete(ParallelProcessor.this, task);
                        } catch (Throwable t) {
                            if (logger != null) {
                                logger.error("Error during execution of work!", t);
                            }
                            else {
                                t.printStackTrace();
                            }
                        } finally {
                            // record how much work was done
                            currentProgress.getAndIncrement();

                            // notify all threads that are waiting for processing to finish
                            synchronized (currentProgress) {
                                currentProgress.notifyAll();
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
     * Creates a worker which will perform work.
     */
    public abstract
    Worker createWorker();

    /**
     * Called each time a single piece of work (a task) is completed.
     */
    public abstract
    void workComplete(ParallelProcessor processor, Task task);

    /**
     * Returns true if there are workers immediately able to do work.
     */
    public
    boolean hasAvailableWorker() {
        return this.queue.size() < numberOfThreads;
    }

    /**
     * Queues task to be worked on
     *
     * @throws InterruptedException if the current thread is interrupted while waiting for a worker to process the task
     */
    public
    void process(final Task taskToProcess) throws InterruptedException {
        queue.put(taskToProcess);
    }

    /**
     * Waits for the results to finish processing. No more work can be done after this is called.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public
    void waitUntilDone() throws InterruptedException {
        if (totalWorkload > 0) {
            while (currentProgress.get() - totalWorkload != 0) {
                synchronized (currentProgress) {
                    currentProgress.wait(10000L); // waits 10 seconds
                }
            }
        }

        // stop all workers.
        for (int i = 0; i < threads.size(); i++) {
            // this tells our threads that we have finished work and can exit
            queue.put(SENTINEL);
        }

        latch.await();
    }

    /**
     * Gets the amount of progress made, between 0-1 OR return the number of tasks completed (if called with totalWorkload = -1).
     * <p>
     * If this returns 0, it is safe to call {@link ParallelProcessor#waitUntilDone()} while will block until the worker threads shutdown
     */
    public
    float getProgress() {
        int i = currentProgress.get();

        if (this.totalWorkload == -1) {
            return (float) i;
        }

        if (i == 0) {
            return 0.0f;
        }
        if (i == totalWorkload) {
            return 1.0f;
        }

        return (float) i / (float) totalWorkload;
    }
}
