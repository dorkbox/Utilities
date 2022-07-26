/*
 * Copyright 2022 dorkbox, llc
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
package dorkbox.util

import java.util.concurrent.*
import java.util.concurrent.atomic.*

/**
 * The default thread factory with names and daemon state
 */
class NamedThreadFactory constructor(
    /** @param namePrefix what you want the subsequent threads to be named. */
    val namePrefix: String,

    /** @param group the group this thread will belong to. If NULL, it will belong to the current thread group. */
    val group: ThreadGroup = Thread.currentThread().threadGroup,

    /** @param threadPriority 1-10, with 5 being normal and 10 being max */
    val threadPriority: Int = Thread.NORM_PRIORITY,

    /** @param daemon true to stop this thread automatically when the JVM shutsdown */
    val daemon: Boolean = true

) : ThreadFactory {
    constructor(poolNamePrefix: String, group: ThreadGroup) : this(poolNamePrefix, group, Thread.NORM_PRIORITY, true)
    constructor(poolNamePrefix: String, isDaemon: Boolean) : this(poolNamePrefix, Thread.currentThread().threadGroup, isDaemon)
    constructor(poolNamePrefix: String, group: ThreadGroup, isDaemon: Boolean) : this(poolNamePrefix, group, Thread.NORM_PRIORITY, isDaemon)


    private val poolId = AtomicInteger()

    init {
        require(threadPriority >= Thread.MIN_PRIORITY) {
            String.format(
                "Thread priority (%s) must be >= %s", threadPriority, Thread.MIN_PRIORITY
            )
        }
        require(threadPriority <= Thread.MAX_PRIORITY) {
            String.format(
                "Thread priority (%s) must be <= %s", threadPriority, Thread.MAX_PRIORITY
            )
        }
    }

    override fun newThread(r: Runnable): Thread {
        // stack size is arbitrary based on JVM implementation. Default is 0
        // 8k is the size of the android stack. Depending on the version of android, this can either change, or will always be 8k
        // To be honest, 8k is pretty reasonable for an asynchronous/event based system (32bit) or 16k (64bit)
        // Setting the size MAY or MAY NOT have any effect!!!
        val t = Thread(group, r, namePrefix + '-' + poolId.incrementAndGet())
        t.isDaemon = daemon
        t.priority = threadPriority
        return t
    }
}
