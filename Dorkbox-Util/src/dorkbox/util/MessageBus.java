package dorkbox.util;

import net.engio.mbassy.IMessageBus;
import net.engio.mbassy.MBassador;
import net.engio.mbassy.error.IPublicationErrorHandler;
import net.engio.mbassy.error.PublicationError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dorkbox.util.OS;

public class MessageBus {

    public static final IMessageBus bus;
    private static final Logger logger = LoggerFactory.getLogger(MessageBus.class);


    static {
        MBassador mBassador = new MBassador(OS.getOptimumNumberOfThreads()*2);

        IPublicationErrorHandler ExceptionCounter = new IPublicationErrorHandler() {
            @Override
            public void handleError(PublicationError error) {
                logger.error(error.toString());
            }
        };

        mBassador.addErrorHandler(ExceptionCounter);
        mBassador.start();

        bus = mBassador;
    }

    private MessageBus() {
    }
}
