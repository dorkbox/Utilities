/*
 * Copyright 2023 dorkbox, llc
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
/*
 * Copyright 2018 Venkat Peri
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

package dorkbox.util.sync

import kotlinx.coroutines.Job

/**
 * A synchronization aid that allows one or more coroutines to wait
 * without blocking until a set of operations being performed in other
 * coroutines complete.
 *
 * A [CountDownLatch] is initialized with a given count. The
 * [await] methods block until the current count reaches zero due to
 * invocations of the [countDown] method, after which all waiting coroutines
 * are released and any subsequent invocations of await return immediately.
 *
 * Example:
 * ```
 * val count = 9L
 * val latch = CountDownLatch(count)
 * val counter = AtomicLong(0)
 *
 * runBlocking {
 *   (0 until count).forEach {
 *     async {
 *       delay(ThreadLocalRandom.current().nextInt(100, 500))
 *       counter.incrementAndGet()
 *       latch.countDown()
 *     }
 *   }
 *   latch.await()
 *   assertEquals(count, counter.get())
 *   println(counter.get())     //=> 9
 * }
 * ```
 *
 * @constructor Constructs a [CountDownLatch] initialized with the given count.
 * @param count the number of times [countDown] must be invoked before
 *      [await] will not block.
 */
class CountDownLatch(
    count: Int, parent: Job? = null
) : AbstractLatch(count, Trigger(count, true, parent)) {

    init {
        require(count >= 0) { "Count $count cannot be negative" }
    }

    fun countDown() {
        trigger.decrement()
    }
}

suspend fun withCountDown(count: Int, parent: Job? = null, block: suspend CountDownLatch.() -> Unit) {
    val latch = CountDownLatch(count, parent)
    block(latch)
    latch.await()
}
