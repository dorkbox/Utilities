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

import dorkbox.util.Sys
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.util.concurrent.*

/**
 * @param initialCount this is the initial count specified when the latch was created
 */
open class AbstractLatch(val initialCount: Int, val trigger: Trigger) : Deferred<Unit> by trigger {
    companion object {
        /**
         * Gets the version number.
         */
        val version = Sys.version
    }

    /**
     * The current latch count affected by the count*() methods.
     *
     * Can be manually changed
     */
    var count: Int
        get() { return trigger.get() }
        set(value) { trigger.set(value) }

    /**
     * Waits the specified amount of time for the latch to reach 0
     *
     * @return true if the latch reached 0 before the timeout
     */
    suspend fun await(timeMillis: Long): Boolean {
        return await(timeMillis, TimeUnit.MILLISECONDS)
    }

    /**
     * Waits the specified amount of time for the latch to reach 0
     *
     * @return true if the latch reached 0 before the timeout
     */
    suspend fun await(time: Long, timeUnit: TimeUnit): Boolean {
        return try {
            withTimeout(timeUnit.toMillis(time)) {
                await()
                true
            }
        } catch (exception: TimeoutCancellationException) {
            false
        }
    }
}
