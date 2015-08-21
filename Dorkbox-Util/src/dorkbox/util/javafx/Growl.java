/**
 * Copyright (c) 2014, ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * MODIFIED BY DORKBOX, LLC
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
package dorkbox.util.javafx;

import com.sun.javafx.application.PlatformImpl;
import dorkbox.util.JavaFxUtil;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.stage.Window;
import javafx.util.Duration;
import org.controlsfx.tools.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * An API to show popup notification messages to the user in the corner of their
 * screen, unlike the {@link org.controlsfx.control.NotificationPane} which shows notification messages
 * within your application itself.
 * 
 * <h3>Screenshot</h3>
 * <p>
 * The following screenshot shows a sample notification rising from the
 * bottom-right corner of my screen:
 * 
 * <br/>
 * <br/>
 * <img src="notifications.png"/>
 * 
 * <h3>Code Example:</h3>
 * <p>
 * To create the notification shown in the screenshot, simply do the following:
 * 
 * <pre>
 * {@code
 * Notifications.create()
 *              .title("Title Text")
 *              .text("Hello World 0!")
 *              .showWarning();
 * }
 * </pre>
 */
public class Growl {

    /***************************************************************************
     * * Static fields * *
     **************************************************************************/

    private static final String STYLE_CLASS_DARK = "dark"; //$NON-NLS-1$

    /***************************************************************************
     * * Private fields * *
     **************************************************************************/

    String title;
    String text;
    Node graphic;

    Pos position = Pos.BOTTOM_RIGHT;
    private Duration hideAfterDuration = Duration.seconds(5);
    boolean hideCloseButton;
    private EventHandler<ActionEvent> onAction;
    Window owner;

    List<String> styleClass = new ArrayList<>();

    /***************************************************************************
     * * Constructors * *
     **************************************************************************/

    // we do not allow instantiation of the Notifications class directly - users
    // must go via the builder API (that is, calling create())
    private
    Growl() {
        // no-op
    }

    /***************************************************************************
     * * Public API * *
     **************************************************************************/

    /**
     * Call this to begin the process of building a notification to show.
     */
    public static
    Growl create() {
        // make sure that javafx application thread is started
        // Note that calling PlatformImpl.startup more than once is OK
        PlatformImpl.startup(() -> {
            // No need to do anything here
        });

        return new Growl();
    }

    /**
     * Specify the text to show in the notification.
     */
    public
    Growl text(String text) {
        this.text = text;
        return this;
    }

    /**
     * Specify the title to show in the notification.
     */
    public
    Growl title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Specify the graphic to show in the notification.
     */
    public
    Growl graphic(Node graphic) {
        this.graphic = graphic;
        return this;
    }

    /**
     * Specify the position of the notification on screen, by default it is
     * {@link Pos#BOTTOM_RIGHT bottom-right}.
     */
    public
    Growl position(Pos position) {
        this.position = position;
        return this;
    }

    /**
     * The dialog window owner - if specified the notifications will be inside
     * the owner, otherwise the notifications will be shown within the whole
     * screen.
     */
    public
    Growl owner(Object owner) {
        this.owner = Utils.getWindow(owner);
        return this;
    }

    /**
     * Specify the duration that the notification should show, after which it
     * will be hidden.
     */
    public
    Growl hideAfter(Duration duration) {
        this.hideAfterDuration = duration;
        return this;
    }

    /**
     * Specify what to do when the user clicks on the notification (in addition
     * to the notification hiding, which happens whenever the notification is
     * clicked on).
     */
    public
    Growl onAction(EventHandler<ActionEvent> onAction) {
        this.onAction = onAction;
        return this;
    }

    /**
     * Specify that the notification should use the built-in dark styling,
     * rather than the default 'modena' notification style (which is a
     * light-gray).
     */
    public
    Growl darkStyle() {
        styleClass.add(STYLE_CLASS_DARK);
        return this;
    }

    /**
     * Specify that the close button in the top-right corner of the notification
     * should not be shown.
     */
    public
    Growl hideCloseButton() {
        this.hideCloseButton = true;
        return this;
    }

    /**
     * Instructs the notification to be shown, and that it should use the built-in 'warning' graphic.
     */
    public
    void showWarning() {
        graphic(new ImageView(Growl.class.getResource("/org/controlsfx/dialog/dialog-warning.png")
                                         .toExternalForm())); //$NON-NLS-1$
        show();
    }

    /**
     * Instructs the notification to be shown, and that it should use the built-in 'information' graphic.
     */
    public
    void showInformation() {
        graphic(new ImageView(Growl.class.getResource("/org/controlsfx/dialog/dialog-information.png")
                                         .toExternalForm())); //$NON-NLS-1$
        show();
    }

    /**
     * Instructs the notification to be shown, and that it should use the built-in 'error' graphic.
     */
    public
    void showError() {
        graphic(new ImageView(Growl.class.getResource("/org/controlsfx/dialog/dialog-error.png")
                                         .toExternalForm())); //$NON-NLS-1$
        show();
    }

    /**
     * Instructs the notification to be shown, and that it should use the built-in 'confirm' graphic.
     */
    public
    void showConfirm() {
        graphic(new ImageView(Growl.class.getResource("/org/controlsfx/dialog/dialog-confirm.png")
                                         .toExternalForm())); //$NON-NLS-1$
        show();
    }

    /**
     * Instructs the notification to be shown.
     */
    public
    void show() {
        // we can't use regular popup, because IF WE HAVE NO OWNER, it won't work!
        // instead, we just create a JFRAME (and use our StageViaSwing class) to put javaFX inside it
        JavaFxUtil.invokeAndWait(() -> new GrowlPopup(this).show());
    }


}

