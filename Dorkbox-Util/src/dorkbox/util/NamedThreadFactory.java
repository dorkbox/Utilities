package dorkbox.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The default thread factory with names.
 */
public class NamedThreadFactory implements ThreadFactory {
    private static final AtomicInteger poolId = new AtomicInteger();

    // permit this to be changed!
    /**
     * The stack size is arbitrary based on JVM implementation. Default is 0
     * 8k is the size of the android stack. Depending on the version of android, this can either change, or will always be 8k
     *
     * To be honest, 8k is pretty reasonable for an asynchronous/event based system (32bit) or 16k (64bit)
     * Setting the size MAY or MAY NOT have any effect!!!
     *
     * Stack size must be specified in bytes. Default is 8k
     */
    public static int stackSizeForThreads = 8192;

    private final AtomicInteger nextId = new AtomicInteger();

    private final ThreadGroup group;
    private final String      namePrefix;
    private final int         threadPriority;

    public NamedThreadFactory(String poolNamePrefix, ThreadGroup group) {
        this(poolNamePrefix, group, Thread.MAX_PRIORITY);
    }

    public NamedThreadFactory(String poolNamePrefix, int threadPriority) {
       this(poolNamePrefix, null, threadPriority);
    }

    /**
     *
     * @param poolNamePrefix what you want the subsequent threads to be named.
     * @param group the group this thread will belong to. If NULL, it will belong to the current thread group.
     * @param threadPriority Thread.MIN_PRIORITY, Thread.NORM_PRIORITY, Thread.MAX_PRIORITY
     */
    public NamedThreadFactory(String poolNamePrefix, ThreadGroup group, int threadPriority) {
        this.namePrefix = poolNamePrefix + '-' + poolId.incrementAndGet();
        if (group == null) {
            this.group = Thread.currentThread().getThreadGroup();
        } else {
            this.group = group;
        }

        if (threadPriority != Thread.MAX_PRIORITY && threadPriority != Thread.NORM_PRIORITY && threadPriority != Thread.MIN_PRIORITY) {
            throw new IllegalArgumentException("Thread priority must be valid!");
        }
        this.threadPriority = threadPriority;
    }

    @Override
    public Thread newThread(Runnable r) {
        // stack size is arbitrary based on JVM implementation. Default is 0
        // 8k is the size of the android stack. Depending on the version of android, this can either change, or will always be 8k
        // To be honest, 8k is pretty reasonable for an asynchronous/event based system (32bit) or 16k (64bit)
        // Setting the size MAY or MAY NOT have any effect!!!
        Thread t = new Thread(this.group, r, this.namePrefix  + '-' + this.nextId.incrementAndGet(), stackSizeForThreads);
        if (!t.isDaemon()) {
            t.setDaemon(true);
        }
        if (t.getPriority() != this.threadPriority) {
            t.setPriority(this.threadPriority);
        }
        return t;
    }
}