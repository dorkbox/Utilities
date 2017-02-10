/*
 * Copyright 2015 dorkbox, llc
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
package dorkbox.util;

import dorkbox.messagebus.IMessageBus;
import dorkbox.messagebus.MessageBus;
import dorkbox.messagebus.error.IPublicationErrorHandler;
import dorkbox.messagebus.error.PublicationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public
class MBus {

    public static final IMessageBus bus;
    private static final Logger logger = LoggerFactory.getLogger(MBus.class);


    static {
        MessageBus messageBus = new MessageBus(OS.getOptimumNumberOfThreads() * 2);

        IPublicationErrorHandler ExceptionCounter = new IPublicationErrorHandler() {
            @Override
            public
            void handleError(PublicationError error) {
                logger.error(error.toString());

                if (error.getCause() != null) {
                    error.getCause()
                         .printStackTrace();
                }
            }

            @Override
            public
            void handleError(final String error, final Class<?> listenerClass) {
                // Printout the error itself
                logger.error(new StringBuilder().append(error)
                                                .append(": ")
                                                .append(listenerClass.getSimpleName())
                                                .toString());
            }
        };

        messageBus.addErrorHandler(ExceptionCounter);
        bus = messageBus;
    }

    private
    MBus() {
    }
}
