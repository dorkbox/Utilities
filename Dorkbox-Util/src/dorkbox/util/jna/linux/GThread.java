package dorkbox.util.jna.linux;


import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public
interface GThread extends Library {
    GThread INSTANCE = (GThread) Native.loadLibrary("gthread-2.0", GThread.class);

    void g_thread_init(Pointer GThreadFunctions);
}
