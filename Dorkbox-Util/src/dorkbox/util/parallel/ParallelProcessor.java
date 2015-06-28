package dorkbox.util.parallel;

import dorkbox.util.messagebus.common.thread.NamedThreadFactory;
import dorkbox.util.objectPool.ObjectPool;
import dorkbox.util.objectPool.ObjectPoolFactory;
import dorkbox.util.objectPool.PoolableObject;
import org.jctools.queues.MpmcTransferArrayQueue;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public
class ParallelProcessor<T extends ParallelTask> {

    private final MpmcTransferArrayQueue<T> mtaq;
    private final ObjectPool<T> pool;
    private final int totalTaskCount;
    private final ParallelCollector<T> collector;
    private final ArrayList<Thread> threads;
    private final AtomicInteger processedCount;
    private final AtomicInteger assignedCount;

    private volatile boolean isShuttingDown = false;


    public
    ParallelProcessor(final PoolableObject<T> poolableObject, final int numberOfThreads, final int totalTaskCount, final Logger logger,
                      final ParallelCollector<T> collector) {

        this.totalTaskCount = totalTaskCount - 1; // x-1 because our ID's start at 0, not 1
        this.collector = collector;

        mtaq = new MpmcTransferArrayQueue<T>(numberOfThreads);
        pool = ObjectPoolFactory.create(poolableObject, numberOfThreads);
        this.processedCount = new AtomicInteger();
        this.assignedCount = new AtomicInteger();

        threads = new ArrayList<Thread>(numberOfThreads);

        NamedThreadFactory dispatchThreadFactory = new NamedThreadFactory("ParallelProcessor");
        for (int i = 0; i < numberOfThreads; i++) {
            Runnable runnable = new Runnable() {
                @Override
                public
                void run() {
                    MpmcTransferArrayQueue<T> IN_QUEUE = ParallelProcessor.this.mtaq;

                    //noinspection InfiniteLoopStatement
                    while (true) {
                        // we want to continue, even if there is an error (until we decide to shutdown)
                        try {
                            while (!isShuttingDown) {
                                final T work = IN_QUEUE.take();
                                doWork(work);
                            }
                        } catch (Throwable t) {
                            if (!isShuttingDown) {
                                logger.error("Parallel task error!", t);
                            }
                        }
                    }
                }
            };


            Thread runner = dispatchThreadFactory.newThread(runnable);
            this.threads.add(runner);
        }

//        for (Thread thread : threads) {
//            thread.start();
//        }
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
            for (Thread thread : threads) {
                thread.interrupt();
            }
            // whichever thread finishes, is the one that runs workComplete
            collector.workComplete();
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
        mtaq.transfer(work);
    }
}
