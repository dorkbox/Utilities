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

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import java.util.concurrent.atomic.*

class Trigger(
    initial: Int, triggerInitially: Boolean = true,
    parent: Job? = null
) : CompletableDeferred<Unit> by CompletableDeferred(parent) {

    private val value = AtomicInteger(initial)

    init {
        validate {
            when (triggerInitially && initial == 0) {
                true -> 0
                else -> 1  // only has to be a non zero value
            }
        }
    }

    fun increment(): Int = validate { value.incrementAndGet() }

    fun decrement(): Int = validate { value.decrementAndGet() }

    fun set(value: Int): Int = validate { this.value.getAndSet(value) }

    fun get(): Int = value.get()

    private fun validate(block: () -> Int): Int {
        val v = block()
        if (!isCompleted && v == 0) {
            complete(Unit)
        }
        return v
    }
}
