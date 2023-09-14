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
package dorkbox.util

import org.junit.Assert
import org.junit.Test
import java.util.concurrent.*

class TimeTest {
    @Test
    fun time() {
        TimeUnit.DAYS.toNanos(3).also {
            Assert.assertEquals("3 days", Sys.getTimePrettyFull(it))
        }
        TimeUnit.DAYS.toNanos(30).also {
            Assert.assertEquals("30 days", Sys.getTimePrettyFull(it))
        }
        TimeUnit.DAYS.toNanos(300).also {
            Assert.assertEquals("300 days", Sys.getTimePrettyFull(it))
        }
        TimeUnit.DAYS.toNanos(3000).also {
            Assert.assertEquals("3000 days", Sys.getTimePrettyFull(it))
        }


        TimeUnit.HOURS.toNanos(3).also {
            Assert.assertEquals("3 hours", Sys.getTimePrettyFull(it))
        }
        TimeUnit.MINUTES.toNanos(3).also {
            Assert.assertEquals("3 minutes", Sys.getTimePrettyFull(it))
        }
        TimeUnit.SECONDS.toNanos(3).also {
            Assert.assertEquals("3 seconds", Sys.getTimePrettyFull(it))
        }
        TimeUnit.MILLISECONDS.toNanos(3).also {
            Assert.assertEquals("3 milli-seconds", Sys.getTimePrettyFull(it))
        }
        TimeUnit.MICROSECONDS.toNanos(3).also {
            Assert.assertEquals("3 micro-seconds", Sys.getTimePrettyFull(it))
        }
        TimeUnit.NANOSECONDS.toNanos(3).also {
            Assert.assertEquals("3 nano-seconds", Sys.getTimePrettyFull(it))
        }


        TimeUnit.NANOSECONDS.toNanos(1).also {
            Assert.assertEquals("1 nano-second", Sys.getTimePrettyFull(it))
        }
    }
}
