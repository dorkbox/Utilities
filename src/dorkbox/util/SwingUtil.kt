/*
 * Copyright 2023 dorkbox, llc
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
package dorkbox.util

import dorkbox.util.ScreenUtil.showOnSameScreenAsMouse_Center
import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.KeyEvent
import java.awt.event.WindowListener
import java.awt.image.BufferedImage
import java.lang.reflect.InvocationTargetException
import javax.swing.*

@Suppress("unused")
object SwingUtil {
    /**
     * Gets the version number.
     */
    val version = Sys.version

    init {/*
         * hack workaround for starting the Toolkit thread before any Timer stuff javax.swing.Timer uses the Event Dispatch Thread, which is not
         * created until the Toolkit thread starts up.  Using the Swing Timer before starting this stuff starts up may get unexpected
         * results (such as taking a long time before the first timer event).
         */
        Toolkit.getDefaultToolkit()
    }

    /**
     * Sets the entire L&F based on "simple" name. Null to set to the system L&F. If this is not called (or set), Swing will use the
     * default CrossPlatform L&F, which is 'Metal'.
     *
     * NOTE: On Linux + swing if the SystemLookAndFeel is the GtkLookAndFeel, this will cause GTK2 to get first which
     * will cause conflicts if one tries to use GTK3
     *
     * @param lookAndFeel the class or null for the system default
     */
    fun setLookAndFeel(lookAndFeel: Class<*>?) {
        if (lookAndFeel == null) {
            setLookAndFeelByName(null)
        } else {
            setLookAndFeelByName(lookAndFeel.name)
        }
    }

    /**
     * Sets the entire L&F based on "simple" name. Null to set to the system L&F. If this is not called (or set), Swing will use the
     * default CrossPlatform L&F, which is 'Metal'.
     *
     * NOTE: On Linux + swing if the SystemLookAndFeel is the GtkLookAndFeel, this will cause GTK2 to get first which
     * will cause conflicts if one tries to use GTK3
     *
     * @param lookAndFeel the simple name or null for the system default
     */
    fun setLookAndFeelByName(lookAndFeel: String?) {
        if (lookAndFeel == null) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        // register a different L&F
        val specified = lookAndFeel.lowercase().trim { it <= ' ' }
        val current = UIManager.getLookAndFeel().name.lowercase()
        if (specified != current) {
            try {
                for (info in UIManager.getInstalledLookAndFeels()) {
                    val name = info.name.lowercase()
                    val className = info.className.lowercase()
                    if (specified == name || specified == className) {
                        UIManager.setLookAndFeel(info.className)
                        return
                    }
                }
            } catch (e: Exception) {
                // whoops. something isn't right!
                e.printStackTrace()
            }
        }

//        // display available look and feels by name
//        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
//            System.err.println(info.getClassName());
//        }
//
//        // display all properties for the specified look and feel
//        Set<Map.Entry<Object, Object>> entries = UIManager.getLookAndFeelDefaults()
//                                                          .entrySet();
//        for (Map.Entry<Object, Object> e : entries) {
//            String key;
//
//
//            if (e.getKey() instanceof StringBuffer) {
//                StringBuffer s = (StringBuffer) e.getKey();
//                key = s.toString();
//            }
//            else {
//                key = (String) e.getKey();
//            }
//
//            //Print all the keys available in UI manager
//            if (e.getValue() != null && (key.toLowerCase().contains("icon") || key.toLowerCase().contains("image"))) {
//                System.out.println("Key:  " + key);
//            }
//
//            if (key.endsWith("TabbedPane.font")) {
//                Font font = UIManager.getFont(key);
//                Font biggerFont = font.deriveFont(2 * font.getSize2D());
//                // change ui default to bigger font
//                UIManager.put(key, biggerFont);
//            }
//            else if (key.endsWith("Tree.rowHeight")) {
//                int rowHeight = UIManager.getInt(key);
//                int bigRowHeight = 2 * rowHeight;
//                UIManager.put(key, bigRowHeight);
//            }
//        }

        // this means we couldn't find our L&F
        Exception("Could not load $lookAndFeel, it was not available.").printStackTrace()
    }

    val isDefaultLookAndFeel: Boolean
        /**
         * @return true if the System is configured to use the System L&F. False otherwise
         */
        get() = (UIManager.getLookAndFeel().javaClass.name == UIManager.getSystemLookAndFeelClassName())

    /** used when setting various icon components in the GUI to "nothing", since null doesn't work  */
    val BLANK_ICON: Image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB_PRE)

    /**
     * Adds a listener to the window parent of the given component. Can be before the component is really added to its hierarchy.
     *
     * @param source The source component
     * @param listener The listener to add to the window
     */
    fun addWindowListener(source: Component, listener: WindowListener) {
        if (source is Window) {
            source.addWindowListener(listener)
        } else {
            source.addHierarchyListener { e ->
                if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() == HierarchyEvent.SHOWING_CHANGED.toLong()) {
                    SwingUtilities.getWindowAncestor(source).addWindowListener(listener)
                }
            }
        }
    }

    /**
     * Gets the largest icon/image for a button (or other JComponent that has .setIcon(image) method) without affecting the size of the
     * button. An image that is any larger will require that the button increases it's height or width.
     *
     * @param button The button (or JMenuItem, etc that has the .setIcon() method ) that you want to measure
     *
     * @return the largest height of an icon before the button will increase in size (as a result of a larger image)
     */
    fun getLargestIconHeightForButton(button: AbstractButton): Int {
        // of note, components can ALSO have different sizes attached to them!
        // mini
        //      myButton.putClientProperty("JComponent.sizeVariant", "mini");
        // small
        //      mySlider.putClientProperty("JComponent.sizeVariant", "small");
        // large
        //      myTextField.putClientProperty("JComponent.sizeVariant", "large");

        // save the icon + text
        val icon = button.icon
        val text = button.text
        button.text = "`Tj|┃" // `Tj|┃ are glyphs that are at the top/bottom of the fontset (usually));
        var minHeight = 0
        var iconSize = 0
        for (i in 1..127) {
            val imageIcon = ImageIcon(BufferedImage(1, i, BufferedImage.TYPE_BYTE_BINARY))
            button.icon = imageIcon
            button.invalidate()
            val height = button.preferredSize.getHeight().toInt()

            // System.err.println(imageIcon.getIconHeight() + "x" + imageIcon.getIconHeight() + " icon \t>>>>>>>>> " + height + "px tall item")
            if (minHeight == 0) {
                minHeight = height
            } else if (minHeight != height) {
                break
            }

            // this is the largest icon size before the size of the component changes
            iconSize = imageIcon.iconHeight
        }

        // restore original values
        button.icon = icon
        button.text = text
        return iconSize
    }

    /**
     * Centers a component according to the window location.
     *
     * @param window The parent window
     * @param component A component, usually a dialog
     */
    fun centerInWindow(window: Window, component: Component) {
        val size = window.size
        val loc = window.locationOnScreen
        val cmpSize = component.size
        loc.x += (size.width - cmpSize.width) / 2
        loc.y += (size.height - cmpSize.height) / 2

        component.setBounds(loc.x, loc.y, cmpSize.width, cmpSize.height)
    }

    fun invokeLater(runnable: Runnable) {
        if (EventQueue.isDispatchThread()) {
            runnable.run()
        } else {
            SwingUtilities.invokeLater(runnable)
        }
    }

    @Throws(InvocationTargetException::class, InterruptedException::class)
    fun invokeAndWait(runnable: Runnable) {
        if (EventQueue.isDispatchThread()) {
            runnable.run()
        } else {
            EventQueue.invokeAndWait(runnable)
        }
    }

    fun invokeAndWaitQuietly(runnable: Runnable) {
        if (EventQueue.isDispatchThread()) {
            runnable.run()
        } else {
            try {
                EventQueue.invokeAndWait(runnable)
            } catch (ignore: Exception) {
            }
        }
    }

    /**
     * Converts a key character into it's corresponding VK entry, or 0 if it's 0
     */
    fun getVirtualKey(key: Char): Int {
        when (key.code) {
            0x08 -> return KeyEvent.VK_BACK_SPACE
            0x09 -> return KeyEvent.VK_TAB
            0x0a -> return KeyEvent.VK_ENTER
            0x1B -> return KeyEvent.VK_ESCAPE
            0x20AC -> return KeyEvent.VK_EURO_SIGN
            0x20 -> return KeyEvent.VK_SPACE
            0x21 -> return KeyEvent.VK_EXCLAMATION_MARK
            0x22 -> return KeyEvent.VK_QUOTEDBL
            0x23 -> return KeyEvent.VK_NUMBER_SIGN
            0x24 -> return KeyEvent.VK_DOLLAR
            0x26 -> return KeyEvent.VK_AMPERSAND
            0x27 -> return KeyEvent.VK_QUOTE
            0x28 -> return KeyEvent.VK_LEFT_PARENTHESIS
            0x29 -> return KeyEvent.VK_RIGHT_PARENTHESIS
            0x2A -> return KeyEvent.VK_ASTERISK
            0x2B -> return KeyEvent.VK_PLUS
            0x2C -> return KeyEvent.VK_COMMA
            0x2D -> return KeyEvent.VK_MINUS
            0x2E -> return KeyEvent.VK_PERIOD
            0x2F -> return KeyEvent.VK_SLASH
            0x30 -> return KeyEvent.VK_0
            0x31 -> return KeyEvent.VK_1
            0x32 -> return KeyEvent.VK_2
            0x33 -> return KeyEvent.VK_3
            0x34 -> return KeyEvent.VK_4
            0x35 -> return KeyEvent.VK_5
            0x36 -> return KeyEvent.VK_6
            0x37 -> return KeyEvent.VK_7
            0x38 -> return KeyEvent.VK_8
            0x39 -> return KeyEvent.VK_9
            0x3A -> return KeyEvent.VK_COLON
            0x3B -> return KeyEvent.VK_SEMICOLON
            0x3C -> return KeyEvent.VK_LESS
            0x3D -> return KeyEvent.VK_EQUALS
            0x3E -> return KeyEvent.VK_GREATER
            0x40 -> return KeyEvent.VK_AT
            0x41 -> return KeyEvent.VK_A
            0x42 -> return KeyEvent.VK_B
            0x43 -> return KeyEvent.VK_C
            0x44 -> return KeyEvent.VK_D
            0x45 -> return KeyEvent.VK_E
            0x46 -> return KeyEvent.VK_F
            0x47 -> return KeyEvent.VK_G
            0x48 -> return KeyEvent.VK_H
            0x49 -> return KeyEvent.VK_I
            0x4A -> return KeyEvent.VK_J
            0x4B -> return KeyEvent.VK_K
            0x4C -> return KeyEvent.VK_L
            0x4D -> return KeyEvent.VK_M
            0x4E -> return KeyEvent.VK_N
            0x4F -> return KeyEvent.VK_O
            0x50 -> return KeyEvent.VK_P
            0x51 -> return KeyEvent.VK_Q
            0x52 -> return KeyEvent.VK_R
            0x53 -> return KeyEvent.VK_S
            0x54 -> return KeyEvent.VK_T
            0x55 -> return KeyEvent.VK_U
            0x56 -> return KeyEvent.VK_V
            0x57 -> return KeyEvent.VK_W
            0x58 -> return KeyEvent.VK_X
            0x59 -> return KeyEvent.VK_Y
            0x5A -> return KeyEvent.VK_Z
            0x5B -> return KeyEvent.VK_OPEN_BRACKET
            0x5C -> return KeyEvent.VK_BACK_SLASH
            0x5D -> return KeyEvent.VK_CLOSE_BRACKET
            0x5E -> return KeyEvent.VK_CIRCUMFLEX
            0x5F -> return KeyEvent.VK_UNDERSCORE
            0x60 -> return KeyEvent.VK_BACK_QUOTE
            0x61 -> return KeyEvent.VK_A
            0x62 -> return KeyEvent.VK_B
            0x63 -> return KeyEvent.VK_C
            0x64 -> return KeyEvent.VK_D
            0x65 -> return KeyEvent.VK_E
            0x66 -> return KeyEvent.VK_F
            0x67 -> return KeyEvent.VK_G
            0x68 -> return KeyEvent.VK_H
            0x69 -> return KeyEvent.VK_I
            0x6A -> return KeyEvent.VK_J
            0x6B -> return KeyEvent.VK_K
            0x6C -> return KeyEvent.VK_L
            0x6D -> return KeyEvent.VK_M
            0x6E -> return KeyEvent.VK_N
            0x6F -> return KeyEvent.VK_O
            0x70 -> return KeyEvent.VK_P
            0x71 -> return KeyEvent.VK_Q
            0x72 -> return KeyEvent.VK_R
            0x73 -> return KeyEvent.VK_S
            0x74 -> return KeyEvent.VK_T
            0x75 -> return KeyEvent.VK_U
            0x76 -> return KeyEvent.VK_V
            0x77 -> return KeyEvent.VK_W
            0x78 -> return KeyEvent.VK_X
            0x79 -> return KeyEvent.VK_Y
            0x7A -> return KeyEvent.VK_Z
            0x7B -> return KeyEvent.VK_BRACELEFT
            0x7D -> return KeyEvent.VK_BRACERIGHT
            0x7F -> return KeyEvent.VK_DELETE
            0xA1 -> return KeyEvent.VK_INVERTED_EXCLAMATION_MARK
        }
        return 0
    }

    /**
     * Converts a VK key character into it's corresponding char entry
     */
    fun getFromVirtualKey(key: Int): Char {
        val code = when (key) {
            KeyEvent.VK_BACK_SPACE -> 0x08
            KeyEvent.VK_TAB -> 0x09
            KeyEvent.VK_ENTER -> 0x0a
            KeyEvent.VK_ESCAPE -> 0x1B
            KeyEvent.VK_EURO_SIGN -> 0x20AC
            KeyEvent.VK_SPACE -> 0x20
            KeyEvent.VK_EXCLAMATION_MARK -> 0x21
            KeyEvent.VK_QUOTEDBL -> 0x22
            KeyEvent.VK_NUMBER_SIGN -> 0x23
            KeyEvent.VK_DOLLAR -> 0x24
            KeyEvent.VK_AMPERSAND -> 0x26
            KeyEvent.VK_QUOTE -> 0x27
            KeyEvent.VK_LEFT_PARENTHESIS -> 0x28
            KeyEvent.VK_RIGHT_PARENTHESIS -> 0x29
            KeyEvent.VK_ASTERISK -> 0x2A
            KeyEvent.VK_PLUS -> 0x2B
            KeyEvent.VK_COMMA -> 0x2C
            KeyEvent.VK_MINUS -> 0x2D
            KeyEvent.VK_PERIOD -> 0x2E
            KeyEvent.VK_SLASH -> 0x2F
            KeyEvent.VK_0 -> 0x30
            KeyEvent.VK_1 -> 0x31
            KeyEvent.VK_2 -> 0x32
            KeyEvent.VK_3 -> 0x33
            KeyEvent.VK_4 -> 0x34
            KeyEvent.VK_5 -> 0x35
            KeyEvent.VK_6 -> 0x36
            KeyEvent.VK_7 -> 0x37
            KeyEvent.VK_8 -> 0x38
            KeyEvent.VK_9 -> 0x39
            KeyEvent.VK_COLON -> 0x3A
            KeyEvent.VK_SEMICOLON -> 0x3B
            KeyEvent.VK_LESS -> 0x3C
            KeyEvent.VK_EQUALS -> 0x3D
            KeyEvent.VK_GREATER -> 0x3E
            KeyEvent.VK_AT -> 0x40
            KeyEvent.VK_A -> 0x41
            KeyEvent.VK_B -> 0x42
            KeyEvent.VK_C -> 0x43
            KeyEvent.VK_D -> 0x44
            KeyEvent.VK_E -> 0x45
            KeyEvent.VK_F -> 0x46
            KeyEvent.VK_G -> 0x47
            KeyEvent.VK_H -> 0x48
            KeyEvent.VK_I -> 0x49
            KeyEvent.VK_J -> 0x4A
            KeyEvent.VK_K -> 0x4B
            KeyEvent.VK_L -> 0x4C
            KeyEvent.VK_M -> 0x4D
            KeyEvent.VK_N -> 0x4E
            KeyEvent.VK_O -> 0x4F
            KeyEvent.VK_P -> 0x50
            KeyEvent.VK_Q -> 0x51
            KeyEvent.VK_R -> 0x52
            KeyEvent.VK_S -> 0x53
            KeyEvent.VK_T -> 0x54
            KeyEvent.VK_U -> 0x55
            KeyEvent.VK_V -> 0x56
            KeyEvent.VK_W -> 0x57
            KeyEvent.VK_X -> 0x58
            KeyEvent.VK_Y -> 0x59
            KeyEvent.VK_Z -> 0x5A
            KeyEvent.VK_OPEN_BRACKET -> 0x5B
            KeyEvent.VK_BACK_SLASH -> 0x5C
            KeyEvent.VK_CLOSE_BRACKET -> 0x5D
            KeyEvent.VK_CIRCUMFLEX -> 0x5E
            KeyEvent.VK_UNDERSCORE -> 0x5F
            KeyEvent.VK_BACK_QUOTE -> 0x60
            KeyEvent.VK_BRACELEFT -> 0x7B
            KeyEvent.VK_BRACERIGHT -> 0x7D
            KeyEvent.VK_DELETE -> 0x7F
            KeyEvent.VK_INVERTED_EXCLAMATION_MARK -> 0xA1
            else -> 0
        }

        return code.toChar()
    }

    /**
     * Displays up a dialog in the center of the screen (where the mouse is located) that displays a message using a default icon
     * determined by the `messageType` parameter.
     *
     * @param title the title for the dialog
     * @param message the message to display
     * @param messageType the type (ERROR, QUESTION, etc)
     *
     * @return the clicked on value, if any.
     * @throws HeadlessException
     */
    @Throws(HeadlessException::class)
    fun showMessageDialog(title: String, message: String, messageType: Int): Int {
        val pane = JOptionPane(message, messageType, JOptionPane.DEFAULT_OPTION, null, null, null)
        pane.initialValue = null

        val rootFrame = JOptionPane.getRootFrame()
        pane.componentOrientation = rootFrame.componentOrientation
        val style: Int

        style = when (messageType) {
            JOptionPane.ERROR_MESSAGE -> JRootPane.ERROR_DIALOG
            JOptionPane.QUESTION_MESSAGE -> JRootPane.QUESTION_DIALOG
            JOptionPane.WARNING_MESSAGE -> JRootPane.WARNING_DIALOG
            JOptionPane.INFORMATION_MESSAGE -> JRootPane.INFORMATION_DIALOG
            JOptionPane.PLAIN_MESSAGE -> JRootPane.PLAIN_DIALOG
            else -> JRootPane.PLAIN_DIALOG
        }

        val dialog = pane.createDialog(title)
        dialog.isModal = true

        if (JDialog.isDefaultLookAndFeelDecorated()) {
            val supportsWindowDecorations = UIManager.getLookAndFeel().supportsWindowDecorations
            if (supportsWindowDecorations) {
                dialog.isUndecorated = true
                dialog.rootPane.windowDecorationStyle = style
            }
        }

        pane.selectInitialValue()
        showOnSameScreenAsMouse_Center(dialog)
        dialog.isVisible = true
        dialog.dispose()

        val selectedValue = pane.value
        return if (selectedValue is Int) {
            selectedValue
        } else JOptionPane.CLOSED_OPTION
    }
}
