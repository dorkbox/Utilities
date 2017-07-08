package dorkbox.util.jna.windows;

/**
 * Important
 *
 * Previous versions of Windows required you to set the DPI awareness for the entire application. Now the DPI awareness is tied to
 * individual threads, processes, or windows. This means that the DPI awareness can change while the app is running and that multiple
 * windows can have their own independent DPI awareness values. See DPI_AWARENESS for more information about how DPI awareness currently
 * works. The recommendations below about setting the DPI awareness in the application manifest are still supported, but the current
 * recommendation is to use the DPI_AWARENESS_CONTEXT.
 *
 *
 * The DPI awareness for an application should be set through the application manifest so that it is determined before any actions are
 * taken which depend on the DPI of the system. Alternatively, you can set the DPI awareness using SetProcessDpiAwareness, but if you do
 * so, you need to make sure to set it before taking any actions dependent on the system DPI. Once you set the DPI awareness for a
 * process, it cannot be changed.
 *
 *
 * Tip
 * If your app is PROCESS_DPI_UNAWARE, there is no need to set any value in the application manifest. PROCESS_DPI_UNAWARE is the
 * default value for apps unless another value is specified.
 *
 *
 * PROCESS_DPI_UNAWARE and PROCESS_SYSTEM_DPI_AWARE apps do not need to respond to WM_DPICHANGED and are not expected to handle changes
 * in DPI. The system will automatically scale these types of apps up or down as necessary when the DPI changes.
 * PROCESS_PER_MONITOR_DPI_AWARE apps are responsible for recognizing and responding to changes in DPI, signaled by WM_DPICHANGED. These
 * will not be scaled by the system. If an app of this type does not resize the window and its content, it will appear to grow or shrink
 * by the relative DPI changes as the window is moved from one display to the another with a different DPI setting.
 *
 *
 * Tip
 * In previous versions of Windows, there was no setting for PROCESS_PER_MONITOR_DPI_AWARE. Apps were either DPI unaware or DPI aware.
 * Legacy applications that were classified as DPI aware before Windows 8.1 are considered to have a PROCESS_DPI_AWARENESS setting of
 * PROCESS_SYSTEM_DPI_AWARE in current versions of Windows.
 *
 *
 *
 * To understand the importance and impact of the different DPI awareness values, consider a user who has three displays: A, B, and C.
 * Display A is set to 100% scaling factor (96 DPI), display B is set to 200% scaling factor (192 DPI), and display C is set to 300%
 * scaling factor (288 DPI). The system DPI is set to 200%.
 *
 * An application that is PROCESS_DPI_UNAWARE will always use a scaling factor of 100% (96 DPI). In this scenario, a PROCESS_DPI_UNAWARE
 * window is created with a size of 500 by 500. On display A, it will render natively with no scaling. On displays B and C, it will be
 * scaled up by the system automatically by a factor of 2 and 3 respectively. This is because a PROCESS_DPI_UNAWARE always assumes a DPI
 * of 96, and the system accounts for that. If the app queries for window size, it will always get a value of 500 by 500 regardless of
 * what display it is in. If this app were to ask for the DPI of any of the three monitors, it will receive 96.
 *
 * Now consider an application that is PROCESS_SYSTEM_DPI_AWARE. Remember that in the sample, the system DPI is 200% or 192 DPI. This
 * means that any windows created by this app will render natively on display B. It the window moves to display A, it will automatically
 * be scaled down by a factor of 2. This is because a PROCESS_SYSTEM_DPI_AWARE app in this scenario assumes that the DPI will always be
 * 192. It queries for the DPI on startup, and then never changes it. The system accommodates this by automatically scaling down when
 * moving to display A. Likewise, if the window moves to display C, the system will automatically scale up by a factor of 1.5. If the app
 * queries for window size, it will always get the same value, similar to PROCESS_DPI_UNAWARE. If it asks for the DPI of any of the three
 * monitors, it will receive 192.
 *
 * Unlike the other awareness values, PROCESS_PER_MONITOR_DPI_AWARE should adapt to the display that it is on. This means that it is
 * always rendered natively and is never scaled by the system. The responsibility is on the app to adjust the scale factor when receiving
 * the WM_DPICHANGED message. Part of this message includes a suggested rect for the window. This suggestion is the current window scaled
 * from the old DPI value to the new DPI value. For example, a window that is 500 by 500 on display A and moved to display B will receive
 * a suggested window rect that is 1000 by 1000. If that same window is moved to display C, the suggested window rect attached to
 * WM_DPICHANGED will be 1500 by 1500. Furthermore, when this app queries for the window size, it will always get the actual native value.
 * Likewise, if it asks for the DPI of any of the three monitors, it will receive 96, 192, and 288 respectively.
 *
 * Because of DPI virtualization, if one application queries another with a different awareness level for DPI-dependent information,
 * the system will automatically scale values to match the awareness level of the caller. One example of this is if you call GetWindowRect
 * and pass in a window created by another application. Using the situation described above, assume that a PROCESS_DPI_UNAWARE app
 * created a 500 by 500 window on display C. If you query for the window rect from a different application, the size of the rect will
 * vary based upon the DPI awareness of your app.
 */
public
class ProcessDpiAwareness {
    /**
     * DPI unaware. This app does not scale for DPI changes and is always assumed to have a scale factor of 100% (96 DPI).
     * It will be automatically scaled by the system on any other DPI setting.
     */
    public static final int PROCESS_DPI_UNAWARE = 0;

    /**
     * System DPI aware. This app does not scale for DPI changes. It will query for the DPI once and use that value for the lifetime
     * of the app. If the DPI changes, the app will not adjust to the new DPI value. It will be automatically scaled up or down by the
     * system when the DPI changes from the system value.
     */
    public static final int PROCESS_SYSTEM_DPI_AWARE = 1;

    /**
     * Per monitor DPI aware. This app checks for the DPI when it is created and adjusts the scale factor whenever the DPI changes.
     * These applications are not automatically scaled by the system.
     */
    public static final int PROCESS_PER_MONITOR_DPI_AWARE = 2;
}
