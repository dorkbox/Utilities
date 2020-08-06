package dorkbox.jna.windows;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;

public
class Parameter extends IntegerType {
    public
    Parameter() {
        this(0);
    }

    public
    Parameter(long value) {
        super(Native.POINTER_SIZE, value);
    }
}
