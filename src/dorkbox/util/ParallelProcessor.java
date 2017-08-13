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

    public
    ParallelProcessor() {
        this(OS.getOptimumNumberOfThreads(), null);
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


    public
    ParallelProcessor(final int numberOfThreads) {
        this(numberOfThreads, null);
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
     * Waits for the results to finish processing. No more work should be queued after this is called.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public
    void waitUntilDone() throws InterruptedException {
        for (int i = 0; i < threads.size(); i++) {
            // this tells out threads that we have finished work and can exit
            queue.put(SENTINEL);
        }

        latch.await();
    }
}
