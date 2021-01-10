package dorkbox.util.swt;

public
class SwtAccess {
    static
    void onShutdown(final org.eclipse.swt.widgets.Display currentDisplay, final Runnable runnable) {
        // currentDisplay.getShells() must only be called inside the event thread!

        org.eclipse.swt.widgets.Shell shell = currentDisplay.getShells()[0];
        shell.addListener(org.eclipse.swt.SWT.Close, new org.eclipse.swt.widgets.Listener() {
            @Override
            public
            void handleEvent(final org.eclipse.swt.widgets.Event event) {
                runnable.run();
            }
        });
    }
}
