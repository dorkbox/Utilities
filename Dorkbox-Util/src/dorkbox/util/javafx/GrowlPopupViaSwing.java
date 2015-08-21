package dorkbox.util.javafx;

import dorkbox.util.SwingUtil;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public
class GrowlPopupViaSwing extends StageViaSwing {

    private static
    AtomicInteger ID = new AtomicInteger(0);

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    static
    GrowlPopupViaSwing create(final Image icon, final String title) {
        final GrowlPopupViaSwing[] returnVal = new GrowlPopupViaSwing[1];

        // this MUST happen on the EDT!
        SwingUtil.invokeAndWait(() -> {
            synchronized (returnVal) {
                returnVal[0] = new GrowlPopupViaSwing(icon, title, ID.getAndIncrement());
            }
        });

        synchronized (returnVal) {
            return returnVal[0];
        }
    }

    private final int id;



    GrowlPopupViaSwing(final Image icon, final String title, final int ID) {
        super();

        this.id = ID;

        frame.setAlwaysOnTop(true);
        frame.setResizable(false);
        frame.setIconImage(icon);
        frame.setTitle(title);
    }

    @Override
    public
    boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final GrowlPopupViaSwing that = (GrowlPopupViaSwing) o;

        return id == that.id;

    }

    @Override
    public
    int hashCode() {
        return id;
    }
}
