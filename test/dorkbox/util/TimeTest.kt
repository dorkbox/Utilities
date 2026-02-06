/*
 * Copyright 2026 dorkbox, llc
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

import org.junit.Assert
import org.junit.Test
import java.util.concurrent.*

class TimeTest {
    @Test
    fun time() {
        TimeUnit.DAYS.toNanos(3).also {
            Assert.assertEquals("3 days", it.asTimeFull())
        }
        TimeUnit.DAYS.toNanos(30).also {
            Assert.assertEquals("30 days", it.asTimeFull())
        }
        TimeUnit.DAYS.toNanos(300).also {
            Assert.assertEquals("300 days", it.asTimeFull())
        }
        TimeUnit.DAYS.toNanos(3000).also {
            Assert.assertEquals("3000 days", it.asTimeFull())
        }


        TimeUnit.HOURS.toNanos(3).also {
            Assert.assertEquals("3 hours", it.asTimeFull())
        }
        TimeUnit.MINUTES.toNanos(3).also {
            Assert.assertEquals("3 minutes", it.asTimeFull())
        }
        TimeUnit.SECONDS.toNanos(3).also {
            Assert.assertEquals("3 seconds", it.asTimeFull())
        }
        TimeUnit.MILLISECONDS.toNanos(3).also {
            Assert.assertEquals("3 milli-seconds", it.asTimeFull())
        }
        TimeUnit.MICROSECONDS.toNanos(3).also {
            Assert.assertEquals("3 micro-seconds", it.asTimeFull())
        }
        TimeUnit.NANOSECONDS.toNanos(3).also {
            Assert.assertEquals("3 nano-seconds", it.asTimeFull())
        }


        TimeUnit.NANOSECONDS.toNanos(1).also {
            Assert.assertEquals("1 nano-second", it.asTimeFull())
        }
    }
}
