/*
 * Copyright 2017 dorkbox, llc
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
package dorkbox.util.jna.windows;

import static com.sun.jna.platform.win32.WinNT.HANDLE;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;

import dorkbox.util.jna.windows.structs.CONSOLE_SCREEN_BUFFER_INFO;
import dorkbox.util.jna.windows.structs.COORD;
import dorkbox.util.jna.windows.structs.INPUT_RECORD;
import dorkbox.util.jna.windows.structs.SMALL_RECT;

public
class Kernel32 {
    static {
        Native.register(NativeLibrary.getInstance("kernel32", W32APIOptions.DEFAULT_OPTIONS));
    }

    // see: http://msdn.microsoft.com/en-us/library/ms682013%28VS.85%29.aspx
    public static final short FOREGROUND_BLACK     = (short) 0x0000;
    public static final short FOREGROUND_BLUE      = (short) 0x0001;
    public static final short FOREGROUND_GREEN     = (short) 0x0002;
    public static final short FOREGROUND_CYAN      = (short) 0x0003;
    public static final short FOREGROUND_RED       = (short) 0x0004;
    public static final short FOREGROUND_MAGENTA   = (short) 0x0005;
    public static final short FOREGROUND_YELLOW    = (short) 0x0006;
    public static final short FOREGROUND_GREY      = (short) 0x0007;
    public static final short FOREGROUND_INTENSITY = (short) 0x0008; // foreground color is intensified.

    public static final short BACKGROUND_BLACK     = (short) 0x0000;
    public static final short BACKGROUND_BLUE      = (short) 0x0010;
    public static final short BACKGROUND_GREEN     = (short) 0x0020;
    public static final short BACKGROUND_CYAN      = (short) 0x0030;
    public static final short BACKGROUND_RED       = (short) 0x0040;
    public static final short BACKGROUND_MAGENTA   = (short) 0x0050;
    public static final short BACKGROUND_YELLOW    = (short) 0x0060;
    public static final short BACKGROUND_GREY      = (short) 0x0070;
    public static final short BACKGROUND_INTENSITY = (short) 0x0080; // background color is intensified.


    public static final short COMMON_LVB_LEADING_BYTE    = (short) 0x0100;
    public static final short COMMON_LVB_TRAILING_BYTE   = (short) 0x0200;
    public static final short COMMON_LVB_GRID_HORIZONTAL = (short) 0x0400;
    public static final short COMMON_LVB_GRID_LVERTICAL  = (short) 0x0800;
    public static final short COMMON_LVB_GRID_RVERTICAL  = (short) 0x1000;
    public static final short COMMON_LVB_REVERSE_VIDEO   = (short) 0x4000;
    public static final short COMMON_LVB_UNDERSCORE      = (short) 0x8000;

    public static final int FORMAT_MESSAGE_FROM_SYSTEM = 0x1000;

    public static void ASSERT(final int returnValue, final String message) {
        // if returnValue == 0, throw assertion error
        assert returnValue != 0 : message + " : " + getLastErrorMessage();
    }

    public static
    String getLastErrorMessage() {
        int errorCode = Native.getLastError();
        if (errorCode == 0) {
            return "ErrorCode: 0x0 [No Error]";
        } else {
            Memory memory = new Memory(1024);
            PointerByReference reference = new PointerByReference(memory);

            FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM, null, errorCode, 0, reference, (int) memory.size(), null);

            String memoryMessage = reference.getPointer()
                                            .getString(0, true);
            memoryMessage = memoryMessage.trim();

            return String.format("ErrorCode: 0x%08x [%s]", errorCode, memoryMessage);
        }
    }

    /**
     * https://msdn.microsoft.com/en-us/library/ms683231%28VS.85%29.aspx
     */
    public static native
    HANDLE GetStdHandle(int stdHandle);

    /**
     * https://msdn.microsoft.com/en-us/library/ms724211%28VS.85%29.aspx
     */
    public static native
    int CloseHandle(HANDLE handle);

    /**
     * https://msdn.microsoft.com/en-us/library/ms686047%28VS.85%29.aspx
     */
    public static native
    int SetConsoleTextAttribute(HANDLE consoleOutput, short attributes);

    /**
     * https://msdn.microsoft.com/en-us/library/windows/desktop/ms679351(v=vs.85).aspx
     */
    public static native
    int FormatMessage(int flags, Pointer source, int messageId, int languageId, PointerByReference buffer, int size, long[] args);

    /**
     * https://msdn.microsoft.com/en-us/library/ms683171%28VS.85%29.aspx
     */
    public static native
    int GetConsoleScreenBufferInfo(HANDLE consoleOutput, CONSOLE_SCREEN_BUFFER_INFO consoleScreenBufferInfo);

    /**
     * https://msdn.microsoft.com/en-us/library/windows/desktop/ms686025(v=vs.85).aspx
     */
    public static native
    int SetConsoleCursorPosition(HANDLE consoleOutput, COORD.ByValue cursorPosition);

    /**
     * https://msdn.microsoft.com/en-us/library/windows/desktop/ms685107(v=vs.85).aspx
     */
    public static native
    int ScrollConsoleScreenBuffer(HANDLE consoleOutput, SMALL_RECT.ByReference scrollRect, SMALL_RECT.ByReference clipRect, COORD.ByValue destinationOrigin, IntByReference fillAttributes);

    /**
     * https://msdn.microsoft.com/en-us/library/ms682662%28VS.85%29.aspx
     */
    public static native
    int FillConsoleOutputAttribute(HANDLE consoleOutput, short attribute, int length, COORD.ByValue writeCoord, IntByReference numberOfAttrsWritten);

    /**
     * https://msdn.microsoft.com/en-us/library/ms682663%28VS.85%29.aspx
     */
    public static native
    int FillConsoleOutputCharacter(HANDLE consoleOutput, char character, int length, COORD.ByValue writeCoord, IntByReference numberOfCharsWritten);

    /**
     * https://msdn.microsoft.com/en-us/library/ms683167%28VS.85%29.aspx
     */
    public static native
    int GetConsoleMode(HANDLE handle, IntByReference mode);

    /**
     * https://msdn.microsoft.com/en-us/library/ms686033%28VS.85%29.aspx
     */
    public static native
    int SetConsoleMode(HANDLE handle, int mode);

    /**
     * https://msdn.microsoft.com/en-us/library/ms684961(v=VS.85).aspx
     */
    public static native
    int ReadConsoleInput(HANDLE handle, INPUT_RECORD.ByReference inputRecords, int length, IntByReference eventsCount);
}
