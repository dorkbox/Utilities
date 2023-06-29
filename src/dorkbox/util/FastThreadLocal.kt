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
 * Copyright © 2012-2014 Lightweight Java Game Library Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright notice, this list
 *     of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice, this
 *     list of conditions and the following disclaimer in the documentation and/or
 *     other materials provided with the distribution.
 *   - Neither the name of 'Light Weight Java Game Library' nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *   ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *   LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *   INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *   CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 *   IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 *   OF SUCH DAMAGE.
 */
package dorkbox.util

/**
 * Fast `ThreadLocal` implementation, adapted from the
 * [LibStruct](https://github.com/riven8192/LibStruct/blob/master/src/net/indiespot/struct/runtime/FastThreadLocal.java) library.
 *
 *
 * This implementation replaces the `ThreadLocalMap` lookup in [ThreadLocal] with a simple array access. The big advantage of this method is
 * that thread-local accesses are identified as invariant by the JVM, which enables significant code-motion optimizations.
 *
 *
 * The underlying array contains a slot for each thread that uses the [FastThreadLocal] instance. The slot is indexed by [Thread.getId]. The
 * array grows if necessary when the [.set] method is called.
 *
 *
 * It is assumed that usages of this class will be read heavy, so any contention/false-sharing issues caused by the [.set] method are ignored.
 *
 * @param <T> the thread-local value type
 *
 * @author Riven
 * @see java.lang.ThreadLocal
</T> */
abstract class FastThreadLocal<T> {
    /** Creates a thread local variable.  */
    @Suppress("UNCHECKED_CAST")
    private var threadIDMap = arrayOfNulls<Any>(1) as Array<T?>

    /**
     * Returns the current thread's "initial value" for this thread-local variable.
     */
    abstract fun initialValue(): T

    /**
     * Sets the current thread's copy of this thread-local variable to the specified value.
     *
     * @param value the value to be stored in the current thread's copy of this thread-local.
     *
     * @see ThreadLocal.set
     */
    fun set(value: T?) {
        val id = Thread.currentThread().id.toInt()

        synchronized(this) {
            val len = threadIDMap.size
            if (len <= id) {
                threadIDMap = threadIDMap.copyOf(id + 1)
            }
            threadIDMap[id] = value
        }
    }

    /**
     * Returns the value in the current thread's copy of this thread-local variable.
     *
     * @see ThreadLocal.get
     */
    fun get(): T {
        val id = Thread.currentThread().id.toInt()
        val threadIDMap: Array<T?> = threadIDMap // It's OK if the array is resized after this access, will just use the old array.
        var value = if (threadIDMap.size <= id) null else threadIDMap[id]

        if (value == null) {
            value = initialValue()
            set(value)
        }

        return value!!
    }

    /**
     * Removes the current thread's value for this thread-local variable.
     *
     * @see ThreadLocal.remove
     */
    fun remove() {
        set(null)
    }
}
