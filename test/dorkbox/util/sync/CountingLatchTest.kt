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

class CountingLatchTest {

    private fun randomDelay() = ThreadLocalRandom.current().nextInt(300, 500).toLong()

    @Test
    fun awaitCountingWithZero() {
        // await on a latch of 0 should not block
        val latch = CountingLatch(0)
        runBlocking {
            if (!latch.await(100)) {
                Assert.fail("latch did not trigger and it shouldn't have!")
            }
        }
    }

    @Test
    fun awaitCountingWithOne() {
        // await on a latch of 0 should not block
        val latch = CountingLatch(1)
        runBlocking {
            if (latch.await(100)) {
                Assert.fail("latch triggered and it shouldn't have!")
            }
        }
    }

    @Test
    fun awaitCountingWithOneStartZeroUpDown() {
        // await on a latch of 0 should not block
        val latch = CountingLatch(1)
        runBlocking {
            latch.countUp()
            latch.countDown()
            latch.countDown()
            if (!latch.await(100)) {
                Assert.fail("waited and it shouldn't have!")
            }
        }
    }

    @Test
    fun count() {
        val latch = CountingLatch(10)
        Assert.assertEquals(10, latch.initialCount)
        Assert.assertEquals(10, latch.count)
    }

    @Test
    fun await() {
        val count = 9
        val latch = CountingLatch(count)
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

    @Test(expected = TimeoutCancellationException::class)
    fun await_timeout_expires() {
        val count = 100
        val latch = CountingLatch(count)
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
        val latch = CountingLatch(count)
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
        val latch = CountingLatch(count)
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
        val latch = CountingLatch(count)
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
        val latch = CountingLatch(count)
        runBlocking {
            (0 until count).forEach { _ ->
                async {
                    latch.countDown()
                }
            }
            latch.await()
        }
    }

    @Test
    fun `when count is 0`() {
        val latch = CountingLatch(0)
        Assert.assertTrue(latch.isCompleted)
    }

    @Test
    fun `when count is 1`() {
        val latch = CountingLatch(1)
        Assert.assertFalse(latch.isCompleted)
    }

    @Test
    fun `when count is 1 to 0`() {
        val latch = CountingLatch(1)
        latch.countDown()
        Assert.assertTrue(latch.isCompleted)
    }

    @Test
    fun `when inverse count is 0`() {
        val latch = CountingLatchInverse(0)
        Assert.assertFalse(latch.isCompleted)
    }

    @Test
    fun `when inverse count is 1`() {
        val latch = CountingLatchInverse(0)
        latch.countDown()
        Assert.assertTrue(latch.isCompleted)
    }
}
