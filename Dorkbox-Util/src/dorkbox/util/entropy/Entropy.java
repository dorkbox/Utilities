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
package dorkbox.util.entropy;

import dorkbox.util.exceptions.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public final
class Entropy {

    @SuppressWarnings("StaticNonFinalField")
    private static EntropyProvider provider = null;

    /**
     * Starts the process, and gets, the next amount of entropy bytes
     */
    public static
    byte[] get(String messageForUser) throws InitializationException {
        synchronized (Entropy.class) {
            try {
                if (provider == null) {
                    Entropy.init(SimpleEntropy.class);
                }

                return provider.get(messageForUser);
            } catch (Exception e) {
                Logger logger = LoggerFactory.getLogger(Entropy.class);
                String error = "Unable to get entropy bytes for " + provider.getClass();
                logger.error(error, e);
                throw new InitializationException(error);
            }
        }
    }

    /**
     * Will only set the Entropy provider if it has not ALREADY been set!
     */
    public static
    void init(Class<? extends EntropyProvider> providerClass, Object... args) throws InitializationException {
        synchronized (Entropy.class) {
            if (provider == null) {
                Exception exception = null;

                // use reflection to create the provider.
                try {
                    Method createMethod = null;
                    Method[] declaredMethods = providerClass.getDeclaredMethods();
                    for (Method m : declaredMethods) {
                        if (m.getName()
                             .equals("create")) {
                            createMethod = m;
                            break;
                        }
                    }

                    if (createMethod != null) {
                        createMethod.setAccessible(true);

                        if (args.length == 0) {
                            provider = (EntropyProvider) createMethod.invoke(null);
                        }
                        else {
                            provider = (EntropyProvider) createMethod.invoke(null, args);
                        }
                        return;
                    }
                } catch (Exception e) {
                    exception = e;
                }

                Logger logger = LoggerFactory.getLogger(Entropy.class);
                String error = "Unable to create entropy provider for " + providerClass + " with " + args.length + " args";
                if (exception != null) {
                    logger.error(error, exception);
                }
                else {
                    logger.error(error);
                }

                throw new InitializationException(error);
            }
        }
    }

    private
    Entropy() {
    }
}
