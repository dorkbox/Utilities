/*
 * Copyright 2010 dorkbox, llc
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
package dorkbox.util.entropy

import dorkbox.util.exceptions.InitializationException
import org.slf4j.LoggerFactory

object Entropy {
    private var provider: EntropyProvider? = null

    /**
     * Starts the process, and gets, the next amount of entropy bytes
     */
    @Throws(InitializationException::class)
    operator fun get(messageForUser: String): ByteArray {
        init(SimpleEntropy::class.java)

        return try {
            provider!!.get(messageForUser)
        } catch (e: Exception) {
            val logger = LoggerFactory.getLogger(Entropy::class.java)
            val error = "Unable to get entropy bytes for " + provider!!.javaClass
            logger.error(error, e)
            throw InitializationException(error)
        }
    }

    /**
     * Will only set the Entropy provider if it has not ALREADY been set!
     */
    @Throws(InitializationException::class)
    fun init(providerClass: Class<out EntropyProvider>, vararg args: Any) {
        synchronized(Entropy::class.java) {
            if (provider == null) {
                try {
                    provider = if (args.isEmpty()) {
                        providerClass.getDeclaredConstructor().newInstance()
                    } else {
                        providerClass.getDeclaredConstructor().newInstance(args)
                    }
                } catch (e: Exception) {
                    val logger = LoggerFactory.getLogger(Entropy::class.java)
                    val error = "Unable to create entropy provider for " + providerClass + " with " + args.size + " args"
                    logger.error(error, e)
                    throw InitializationException(error)
                }
            }
        }
    }
}
