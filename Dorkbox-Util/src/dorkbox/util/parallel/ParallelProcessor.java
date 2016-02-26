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
package dorkbox.util.parallel;

import dorkbox.util.NamedThreadFactory;
import dorkbox.objectPool.ObjectPool;
import dorkbox.objectPool.PoolableObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This isn't the FASTEST implementation, but it is relatively easy and solid. Also minimal GC.
 * @param <T>
 */
public
class ParallelProcessor<T extends ParallelTask> {

    private final ObjectPool<T> pool;
    private final int totalTaskCount;
    private final ParallelCollector<T> collector;

    private final ArrayList<Thread> threads;
    private final AtomicInteger processedCount;
    private final AtomicInteger assignedCount;

    private final ArrayBlockingQueue<T> queue;

    private volatile boolean isShuttingDown = false;


    public
    ParallelProcessor(final PoolableObject<T> poolableObject, final int numberOfThreads, final int totalTaskCount, final Logger logger,
                      final ParallelCollector<T> collector) {

        this.totalTaskCount = totalTaskCount - 1; // x-1 because our ID's start at 0, not 1
        this.collector = collector;

        pool = new ObjectPool<T>(poolableObject, numberOfThreads);
        this.processedCount = new AtomicInteger();
        this.assignedCount = new AtomicInteger();

        queue = new ArrayBlockingQueue<T>(numberOfThreads * 2);
        threads = new ArrayList<Thread>(numberOfThreads);

        ThreadGroup threadGroup = new ThreadGroup(Thread.currentThread().getThreadGroup(), "ParallelProcessor");
        NamedThreadFactory dispatchThreadFactory = new NamedThreadFactory("Processor", threadGroup);
        for (int i = 0; i < numberOfThreads; i++) {
            Runnable runnable = new Runnable() {
                @Override
                public
                void run() {
                    ArrayBlockingQueue<T> queue = ParallelProcessor.this.queue;

                    //noinspection InfiniteLoopStatement
                    while (true) {
                        // we want to continue, even if there is an error (until we decide to shutdown).
                        // we get these threads to exit via `interrupt`
                        try {
                            while (!isShuttingDown) {
                                final T work = queue.take();
                                doWork(work);
                            }
                        } catch (Throwable t) {
                            if (!isShuttingDown) {
                                logger.error("Error during execution of parallel work!", t);
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

    private
    void doWork(final T work) {
        // this does the work, and stores the result
        work.doWork();
        collector.taskComplete(work);

        final int i = processedCount.getAndIncrement();
        pool.release(work);  // last valid position for 'work', since this releases it back so the client can reuse it

        if (i == totalTaskCount) {
            isShuttingDown = true;

            // whichever thread finishes, is the one that runs workComplete
            collector.workComplete();

            for (Thread thread : threads) {
                thread.interrupt();
            }
        }
    }

    public
    T next() throws InterruptedException {
        final T take = pool.take();
        take.setId(assignedCount.getAndIncrement());
        return take;
    }

    public
    void execute(final T work) throws InterruptedException {
        queue.put(work);
    }
}
