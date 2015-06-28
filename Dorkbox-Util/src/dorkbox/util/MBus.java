package dorkbox.util;

import dorkbox.util.messagebus.IMessageBus;
import dorkbox.util.messagebus.MessageBus;
import dorkbox.util.messagebus.error.IPublicationErrorHandler;
import dorkbox.util.messagebus.error.PublicationError;
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
            public void handleError(PublicationError error) {
                logger.error(error.toString());

                if (error.getCause() != null) {
                    error.getCause().printStackTrace();
                }
            }

            @Override
            public
            void handleError(final String error, final Class<?> listenerClass) {
                // Printout the error itself
                logger.error(new StringBuilder().append(error).append(": ").append(listenerClass.getSimpleName()).toString());
            }
        };

        messageBus.getErrorHandler().addErrorHandler(ExceptionCounter);
        messageBus.start();

        bus = messageBus;
    }

    private
    MBus() {
    }
}
