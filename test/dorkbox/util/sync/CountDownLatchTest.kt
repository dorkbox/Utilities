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

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.*
import java.util.concurrent.atomic.*

class CountDownLatchTest {

    private fun randomDelay() = ThreadLocalRandom.current().nextInt(300, 500).toLong()

    @Test
    fun count() {
        val latch = CountDownLatch(10)
        Assert.assertEquals(10, latch.count)
    }

    @Test
    fun awaitWithZero() {
        // await on a latch of 0 should not block
        val latch = CountDownLatch(0)
        runBlocking {
            if (!latch.await(100)) {
                Assert.fail("waited and it shouldn't have!")
            }
        }
    }

    @Test
    fun awaitCountingWithZero() {
        // await on a latch of 0 should not block
        val latch = CountingLatch(0)
        runBlocking {
            if (!latch.await(100)) {
                Assert.fail("waited and it shouldn't have!")
            }
        }
    }

    @Test
    fun awaitCountingWithOne() {
        // await on a latch of 0 should not block
        val latch = CountingLatch(1)
        runBlocking {
            if (latch.await(100)) {
                Assert.fail("waited and it shouldn't have!")
            }
        }
    }

    @Test
    fun await() {
        val count = 9
        val latch = CountDownLatch(count)
        val counter = AtomicInteger(0)

        runBlocking {
            (0 until count).forEach { _ ->
                async {
                    delay(randomDelay())

                    counter.incrementAndGet()
                    latch.countDown()
                }
            }
            latch.await()
            Assert.assertEquals(count, counter.get())
            println(counter.get())
        }
    }

    @Test
    fun builder() {
        val count = 9
        val counter = AtomicInteger(0)
        runBlocking {
            withCountDown(count) {
                (0 until count).forEach { _ ->
                    async {
                        delay(randomDelay())

                        counter.incrementAndGet()
                        countDown()
                    }
                }
            }
        }
        Assert.assertEquals(count, counter.get())
    }

    @Test(expected = TimeoutCancellationException::class)
    fun await_timeout_expires() {
        val count = 100
        val latch = CountDownLatch(count)
        runBlocking {
            withTimeout(50) {
                (0 until count).forEach { _ ->
                    async {
                        delay(randomDelay())
                        latch.countDown()
                    }
                }
                latch.await()
            }
        }
    }

    @Test
    fun await_timeout_expires2() {
        val count = 100
        val latch = CountDownLatch(count)
        runBlocking {
            (0 until count).forEach { _ ->
                async {

                    delay(randomDelay())
                    latch.countDown()
                }
            }
            val success = latch.await(50)

            Assert.assertFalse(success)
        }
    }

    @Test
    fun await_timeout_does_not_expire() {
        val count = 100
        val latch = CountDownLatch(count)
        var x = 0
        runBlocking {
            withTimeout(1000) {
                (0 until count).forEach { _ ->
                    async {
                        delay(randomDelay())
                        latch.countDown()
                    }
                }
                latch.await()
                x = 1
            }
            Assert.assertEquals(1, x)
        }
    }

    @Test
    fun await_timeout_does_not_expire2() {
        val count = 100
        val latch = CountDownLatch(count)
        var x = 0
        runBlocking {
            withTimeout(1000) {
                (0 until count).forEach { _ ->
                    async {
                        delay(randomDelay())
                        latch.countDown()
                    }
                }
                Assert.assertTrue(latch.await(1000))
                x = 1
            }
            Assert.assertEquals(1, x)
        }
    }

    @Test
    fun stress() {
        val count = 1000000
        val latch = CountDownLatch(count)
        runBlocking {
            (0 until count).forEach { _ ->
                async {
                    latch.countDown()
                }
            }
            latch.await()
        }
    }

}
