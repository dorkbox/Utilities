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

import java.util.*

/**
 * Sometimes you want to make sure that this timer will complete, even if the calling thread has terminated.
 */
class DelayTimer(
    /**
     * the name of the thread (if you want to specify one)
     */
    private val name: String,
    /**
     * true if you want this timer to be run on a daemon thread
     */
    private val isDaemon: Boolean,
    /**
     * the callback listener to execute
     */
    private val listener: Runnable) {

    companion object {
        /**
         * Gets the version number.
         */
        val version = "1.42"
    }

    @Volatile
    private var timer: Timer? = null

    @get:Synchronized
    var delay: Long = 0
        private set

    constructor(listener: Runnable) : this("Timer-", true, listener)

    @get:Synchronized
    val isWaiting: Boolean
        /**
         * @return true if this timer is still waiting to run.
         */
        get() = timer != null

    /**
     * Cancel the delay timer!
     */
    @Synchronized
    fun cancel() {
        if (timer != null) {
            timer!!.cancel()
            timer!!.purge()
            timer = null
        }
    }

    /**
     * @param delay milliseconds to wait
     */
    @Synchronized
    fun delay(delay: Long) {
        this.delay = delay
        cancel()

        if (delay > 0) {
            timer = Timer(name, isDaemon)
            val t: TimerTask = object : TimerTask() {
                override fun run() {
                    // timer can change if the callback calls delay() or cancel()
                    val origTimer = timer
                    listener.run()
                    if (origTimer != null) {
                        origTimer.cancel()
                        origTimer.purge()
                        if (origTimer === timer) {
                            timer = null
                        }
                    }
                }
            }
            timer!!.schedule(t, delay)
        } else {
            listener.run()
            timer = null
        }
    }
}
