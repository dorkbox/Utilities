/*
 * Copyright 2017 Pronghorn Technology LLC
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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mu.KLogger
import mu.KotlinLogging
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

inline fun <reified T> T.logger(name: String): KLogger {
    return KotlinLogging.logger(name)
}

inline fun <reified T> T.logger(): KLogger {
    if (T::class.isCompanion) {
        return KotlinLogging.logger(T::class.java.enclosingClass.simpleName)
    }
    return KotlinLogging.logger(T::class.java.simpleName)
}


fun Exception.stackTraceToString(): String {
    val exceptionWriter = StringWriter()
    printStackTrace(PrintWriter(exceptionWriter))
    return exceptionWriter.toString()
}

inline fun ignoreException(block: () -> Unit) {
    try {
        block()
    } catch (ex: Exception) {
        // no-op
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun ignoreExceptions(vararg blocks: () -> Unit) {
    blocks.forEach { block ->
        try {
            block()
        } catch (ex: Exception) {
            // no-op
        }
    }
}

fun async(dispatcher: CoroutineDispatcher = Dispatchers.IO, action: suspend CoroutineScope.() -> Unit): Job {
    return GlobalScope.launch(dispatcher) {
        action()
    }
}

suspend fun <T> Mutex.withReentrantLock(block: suspend () -> T): T {
    val key = ReentrantMutexContextKey(this)

    // call block directly when this mutex is already locked in the context
    if (coroutineContext[key] != null) return block()

    // otherwise add it to the context and lock the mutex
    return withContext(ReentrantMutexContextElement(key)) {
        withLock { block() }
    }
}

class ReentrantMutexContextElement(override val key: ReentrantMutexContextKey) : CoroutineContext.Element
data class ReentrantMutexContextKey(val mutex: Mutex) : CoroutineContext.Key<ReentrantMutexContextElement>
