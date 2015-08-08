/**
 * Copyright (c) 2014, 2015 ControlsFX
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
 * MODIFIED BY DORKBOX
 */
package dorkbox.util.javafx;

import dorkbox.util.JavaFxUtil;
import impl.org.controlsfx.ImplUtils;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.controlsfx.control.PopOver;
import org.controlsfx.tools.ValueExtractor;
import org.controlsfx.validation.ValidationMessage;
import org.controlsfx.validation.ValidationSupport;

import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * <p>The API for creating multi-page Wizards, based on JavaFX {@link Dialog} API.<br/>
 * Wizard can be setup in following few steps:<p/>
 * <ul>
 *    <li>Design wizard pages by inheriting them from {@link WizardPane}</li>
 *    <li>Define wizard flow by implementing {@link org.controlsfx.dialog.Wizard.Flow}</li>
 *    <li>Create and instance of the Wizard and assign flow to it</li>
 *    <li>Execute the wizard using showAndWait method</li>
 *    <li>Values can be extracted from settings map by calling getSettings
 * </ul>
 * <p>For simple, linear wizards, the {@link LinearFlow} can be used.
 * It is a flow based on a collection of wizard pages. Here is the example:</p>
 *
 * <pre>{@code // Create pages. Here for simplicity we just create and instance of WizardPane.
 * WizardPane page1 = new WizardPane();
 * WizardPane page2 = new WizardPane();
 * WizardPane page2 = new WizardPane();
 *
 * // create wizard
 * Wizard wizard = new Wizard();
 *
 * // create and assign the flow
 * wizard.setFlow(new LinearFlow(page1, page2, page3));
 *
 * // show wizard and wait for response
 * wizard.showAndWait().ifPresent(result -> {
 *     if (result == ButtonType.FINISH) {
 *         System.out.println("Wizard finished, settings: " + wizard.getSettings());
 *     }
 * });}</pre>
 *
 * <p>For more complex wizard flows we suggest to create a custom ones, describing page traversal logic.
 * Here is a simplified example: </p>
 *
 * <pre>{@code Wizard.Flow branchingFlow = new Wizard.Flow() {
 *     public Optional<WizardPane> advance(WizardPane currentPage) {
 *         return Optional.of(getNext(currentPage));
 *     }
 *
 *     public boolean canAdvance(WizardPane currentPage) {
 *         return currentPage != page3;
 *     }
 *
 *     private WizardPane getNext(WizardPane currentPage) {
 *         if ( currentPage == null ) {
 *             return page1;
 *         } else if ( currentPage == page1) {
 *             // skipNextPage() does not exist - this just represents that you
 *             // can add a conditional statement here to change the page.
 *             return page1.skipNextPage()? page3: page2;
 *         } else {
 *             return page3;
 *         }
 *     }
 * };}</pre>
 */
public class Wizard {
    final Stage stage = new Stage(StageStyle.UTILITY);
    private final Text headerText;
    private final VBox center;

    private final ObservableMap<String, Object> settings = FXCollections.observableHashMap();

    final Stack<WizardPane> pageHistory = new Stack<>();
    Optional<WizardPane> currentPage = Optional.empty();

    private final BooleanProperty invalidProperty = new SimpleBooleanProperty(false);


    // Read settings activated by default for backward compatibility
    private final BooleanProperty readSettingsProperty = new SimpleBooleanProperty(true);

    volatile boolean success = false;

    private final Button BUTTON_PREVIOUS = new Button("Previous");

    private final Button BUTTON_NEXT = new Button("Next");
    private final EventHandler<Event> BUTTON_NEXT_EVENT_HANDLER = event -> {
        if (event instanceof KeyEvent) {
            final KeyCode code = ((KeyEvent)event).getCode();
            if (code == KeyCode.ENTER || code == KeyCode.SPACE) {
                event.consume();
                goNext();
            }
        } else {
            event.consume();
            goNext();
        }
    };

    private final EventHandler<Event> BUTTON_FINISH_EVENT_HANDLER = event -> {
        if (event instanceof KeyEvent) {
            final KeyCode code = ((KeyEvent)event).getCode();
            if (code == KeyCode.ENTER || code == KeyCode.SPACE) {
                event.consume();
                goFinish();
            }
        } else {
            event.consume();
            goFinish();
        }
    };

    private final StringProperty titleProperty = new SimpleStringProperty();
    private volatile boolean useSpecifiedSize = false;
    private final PopOver popOver;
    private Text errorText;
    private Font defaultHeaderFont;



    /**************************************************************************
     *
     * Constructors
     *
     **************************************************************************/

    /**
     * Creates an instance of the wizard without an owner.
     */
    public
    Wizard() {
        this(null);
    }

    /**
     * Creates an instance of the wizard with the given owner.
     * @param owner The object from which the owner window is deduced (typically
     *      this is a Node, but it may also be a Scene or a Stage).
     */
    public
    Wizard(Object owner) {
        this(owner, ""); //$NON-NLS-1$
    }

    /**
     * Creates an instance of the wizard with the given owner and title.
     *
     * @param owner The object from which the owner window is deduced (typically
     *      this is a Node, but it may also be a Scene or a Stage).
     * @param title The wizard title.
     */
    public
    Wizard(Object owner, String title) {
        BUTTON_PREVIOUS.setDisable(true);
        BUTTON_NEXT.setDisable(true);

        BUTTON_PREVIOUS.setId("prev-button");
        BUTTON_NEXT.setId("next-button");

        BUTTON_PREVIOUS.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            goPrev();
        });

        BUTTON_PREVIOUS.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            final KeyCode code = event.getCode();
            if (code == KeyCode.ENTER || code == KeyCode.SPACE) {
                event.consume();
                goPrev();
            }
        });


        stage.titleProperty()
             .bind(this.titleProperty);
        setTitle(title);

        popOver = new PopOver();
        popOver.setDetachable(false);
        popOver.setDetached(false);
        popOver.setAutoHide(false);

        popOver.setArrowSize(12);
        popOver.setArrowIndent(12);
        popOver.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
        popOver.setCornerRadius(6);

        VBox content = new VBox();
        content.setPadding(new Insets(10));

        errorText = new Text();
        errorText.setFont(new Font(13));

        content.setPadding(new Insets(20, 10, 0, 10));
        content.getChildren()
               .add(errorText);

        popOver.setContentNode(content);

        invalidProperty.addListener((o, ov, nv) -> validateActionState());

        BorderPane borderPane = new BorderPane();

        // Auto-sizing spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);


        Button cancel = new Button("Cancel");
        cancel.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            success = false;
            stage.close();
        });


        ToolBar toolbar = new ToolBar(cancel, spacer, BUTTON_PREVIOUS, BUTTON_NEXT);
        toolbar.setPadding(new Insets(8));
        borderPane.setBottom(toolbar);

        headerText = new Text();
        defaultHeaderFont = new Font(25);
        headerText.setFont(defaultHeaderFont);

        ToolBar region = new ToolBar(headerText);
        region.setPadding(new Insets(15, 12, 15, 12));
        borderPane.setTop(region);

        center = new VBox();
        borderPane.setCenter(center);

        Scene scene2 = new Scene(borderPane);
        stage.setScene(scene2);
        stage.setMinWidth(300);
        stage.setMinHeight(140);
        stage.setWidth(300);
        stage.setHeight(140);

        Window window = null;
        if (owner instanceof Window) {
            window = (Window) owner;
        }
        else if (owner instanceof Node) {
            window = ((Node) owner).getScene()
                                   .getWindow();
        }

        stage.initOwner(window);
    }

    /**************************************************************************
     *
     * Public API
     *
     **************************************************************************/


    /**
     * Allows you to customize the width and height of the wizard. Must be set before the flow is set
     */
    public final
    void setSize(int width, int height) {
        stage.setWidth(width);
        stage.setHeight(height);
        useSpecifiedSize = true;
    }


    /**
     * Shows the wizard but does not wait for a user response (in other words, this brings up a non-blocking dialog).
     */
    public final
    void show() {
        JavaFxUtil.showOnSameScreenAsMouseCenter(stage);
        stage.show();
    }

    /**
     * Shows the wizard and waits for the user response (in other words, brings up a blocking dialog, with the returned value (true=finished
     * or false=cancel/close)
     *
     * @return An true/false depending on how they closed the wizard.
     */
    public final
    boolean showAndWait() {
        JavaFxUtil.showOnSameScreenAsMouseCenter(stage);
        stage.showAndWait();

        return success;
    }

    /**
     * The settings map is the place where all data from pages is kept once the user moves on from the page, assuming there is a {@link
     * ValueExtractor} that is capable of extracting a value out of the various fields on the page.
     */
    public final
    ObservableMap<String, Object> getSettings() {
        return settings;
    }

    /**
     * Goes to the next page, or finishes the wizard
     */
    public
    void goNext() {
        currentPage.ifPresent(pageHistory::push);
        currentPage = getFlow().advance(currentPage.orElse(null));
        updatePage(stage, true);
    }

    private
    void goPrev() {
        currentPage = Optional.ofNullable(pageHistory.isEmpty() ? null : pageHistory.pop());
        updatePage(stage, false);
    }

    private
    void goFinish() {
        success = true;
        stage.close();
    }

    /**************************************************************************
     *
     * Properties
     *
     **************************************************************************/

    // --- title

    /**
     * Return the titleProperty of the wizard.
     */
    public final
    StringProperty titleProperty() {
        return titleProperty;
    }

    /**
     * Return the title of the wizard.
     */
    public final
    String getTitle() {
        return titleProperty.get();
    }

    /**
     * Change the Title of the wizard.
     */
    public final
    void setTitle(String title) {
        titleProperty.set(title);
    }

    // --- flow
    /**
     * The {@link Flow} property represents the flow of pages in the wizard.
     */
    private final
    ObjectProperty<Flow> flow = new SimpleObjectProperty<Flow>(new LinearFlow()) {
        @Override
        protected
        void invalidated() {
            updatePage(stage, false);
        }

        @Override
        public
        void set(Flow flow) {
            super.set(flow);
            pageHistory.clear();

            if (flow != null) {
                currentPage = flow.advance(currentPage.orElse(null));
                updatePage(stage, true);
            }
        }
    };

    public final
    ObjectProperty<Flow> flowProperty() {
        return flow;
    }

    /**
     * Returns the currently set {@link Flow}, which represents the flow of pages in the wizard.
     */
    public final
    Flow getFlow() {
        return flow.get();
    }

    /**
     * Sets the {@link Flow}, which represents the flow of pages in the wizard.
     */
    public final
    void setFlow(Flow flow) {
        this.flow.set(flow);
    }


    // --- Properties
    private static final Object USER_DATA_KEY = new Object();

    // A map containing a set of properties for this Wizard
    private ObservableMap<Object, Object> properties;

    /**
     * Returns an observable map of properties on this Wizard for use primarily by application developers - not to be confused with the
     * {@link #getSettings()} map that represents the values entered by the user into the wizard.
     *
     * @return an observable map of properties on this Wizard for use primarily by application developers
     */
    @SuppressWarnings("FieldRepeatedlyAccessedInMethod")
    public final
    ObservableMap<Object, Object> getProperties() {
        if (properties == null) {
            properties = FXCollections.observableMap(new HashMap<>());
        }
        return properties;
    }

    /**
     * Tests if this Wizard has properties.
     *
     * @return true if this Wizard has properties.
     */
    public
    boolean hasProperties() {
        return properties != null && !properties.isEmpty();
    }


    // --- UserData

    /**
     * Returns a previously set Object property, or null if no such property has been set using the {@link #setUserData(Object)} method.
     *
     * @return The Object that was previously set, or null if no property has been set or if null was set.
     */
    public
    Object getUserData() {
        return getProperties().get(USER_DATA_KEY);
    }

    /**
     * Convenience method for setting a single Object property that can be retrieved at a later date. This is functionally equivalent to
     * calling the getProperties().put(Object key, Object value) method. This can later be retrieved by calling {@link #getUserData()}.
     *
     * @param value
     *                 The value to be stored - this can later be retrieved by calling {@link #getUserData()}.
     */
    public
    void setUserData(Object value) {
        getProperties().put(USER_DATA_KEY, value);
    }

    /**
     * Gets the value of the property {@code invalid}.
     *
     * @return The validation state
     *
     * @see #invalidProperty()
     */
    public final
    boolean isInvalid() {
        return invalidProperty.get();
    }

    /**
     * Sets the value of the property {@code invalid}.
     *
     * @param invalid
     *                 The new validation state {@link #invalidProperty() }
     */
    public final
    void setInvalid(boolean invalid) {
        invalidProperty.set(invalid);
    }

    /**
     * Property for overriding the individual validation state of this {@link org.controlsfx.dialog.Wizard}. Setting {@code invalid} to true
     * will disable the next/finish Button and the user will not be able to advance to the next page of the {@link
     * org.controlsfx.dialog.Wizard}. Setting {@code invalid} to false will enable the next/finish Button. <br> <br> For example you can use
     * the {@link ValidationSupport#invalidProperty()} of a page and bind it to the {@code invalid} property: <br> {@code
     * wizard.invalidProperty().bind(page.validationSupport.invalidProperty()); }
     *
     * @return The validation state property
     */
    public final
    BooleanProperty invalidProperty() {
        return invalidProperty;
    }

    /**
     * Gets the value of the property {@code readSettings}.
     *
     * @return The read-settings state
     *
     * @see #readSettingsProperty()
     */
    public final
    boolean isReadSettings() {
        return readSettingsProperty.get();
    }

    /**
     * Sets the value of the property {@code readSettings}.
     *
     * @param readSettings
     *                 The new read-settings state
     *
     * @see #readSettingsProperty()
     */
    public final
    void setReadSettings(boolean readSettings) {
        readSettingsProperty.set(readSettings);
    }

    /**
     * Property for overriding the individual read-settings state of this {@link org.controlsfx.dialog.Wizard}. Setting {@code readSettings}
     * to true will enable the value extraction for this {@link org.controlsfx.dialog.Wizard}. Setting {@code readSettings} to false will
     * disable the value extraction for this {@link org.controlsfx.dialog.Wizard}.
     *
     * @return The readSettings state property
     */
    public final
    BooleanProperty readSettingsProperty() {
        return readSettingsProperty;
    }



    /**************************************************************************
     * Private implementation
     **************************************************************************/
    boolean BUTTON_PREV_INIT = false;
    boolean BUTTON_NEXT_INIT = false;

    void updatePage(Stage stage, boolean advancing) {
        Flow flow = getFlow();
        if (flow == null) {
            return;
        }

        Optional<WizardPane> prevPage = Optional.ofNullable(pageHistory.isEmpty() ? null : pageHistory.peek());
        prevPage.ifPresent(page -> {
            // if we are going forward in the wizard, we read in the settings
            // from the page and store them in the settings map.
            // If we are going backwards, we do nothing
            // This is only performed if readSettings is true.
            if (advancing && isReadSettings()) {
                readSettings(page);
            }

            // give the previous wizard page a chance to update the pages list
            // based on the settings it has received
            page.onExitingPage(this);

            invalidProperty.set(false);
            popOver.hide();
        });

        currentPage.ifPresent(currentPage -> {
            // put in default actions
            if (!BUTTON_PREV_INIT) {
                BUTTON_PREV_INIT = true;
                BUTTON_PREVIOUS.setDisable(false);
            }
            if (!BUTTON_NEXT_INIT) {
                BUTTON_NEXT_INIT = true;
                BUTTON_NEXT.setDisable(false);

                BUTTON_NEXT.addEventFilter(ActionEvent.ACTION, BUTTON_NEXT_EVENT_HANDLER);
                BUTTON_NEXT.addEventFilter(KeyEvent.KEY_PRESSED, BUTTON_NEXT_EVENT_HANDLER);
            }

            // then give user a chance to modify the default actions
            currentPage.onEnteringPage(this);

            final Node firstFocusElement = currentPage.firstFocusElement;
            if (firstFocusElement != null) {
                Platform.runLater(() -> {
                    if (isInvalid()) {
                        firstFocusElement.requestFocus();
                    }
                    else {
                        Platform.runLater(BUTTON_NEXT::requestFocus);
                    }
                });
            }
            else {
                if (isInvalid()) {
                    Platform.runLater(BUTTON_PREVIOUS::requestFocus);
                }
                else {
                    Platform.runLater(BUTTON_NEXT::requestFocus);
                }
            }

            // and then switch to the new pane
            if (currentPage.headerFont != null) {
                headerText.setFont(currentPage.headerFont);
            }
            else {
                headerText.setFont(defaultHeaderFont);
            }
            headerText.setText(currentPage.getHeaderText());
            ObservableList<Node> children = center.getChildren();
            children.clear();
            children.add(currentPage.getContent());


            if (!useSpecifiedSize) {
                currentPage.getContent()
                           .autosize();
                stage.sizeToScene();
            }

            currentPage.validationSupport.redecorate();
            notifyValidationChange(currentPage, currentPage.validationErrors);
        });

        validateActionState();
    }

    private
    void validateActionState() {
        // Note that we put the 'next' and 'finish' actions at the beginning of
        // the actions list, so that it takes precedence as the default button,
        // over, say, cancel. We will probably want to handle this better in the
        // future...

        if (!getFlow().canAdvance(currentPage.orElse(null))) {
            BUTTON_NEXT.setText("Finish");
            BUTTON_NEXT.removeEventFilter(ActionEvent.ACTION, BUTTON_NEXT_EVENT_HANDLER);
            BUTTON_NEXT.removeEventFilter(KeyEvent.KEY_PRESSED, BUTTON_NEXT_EVENT_HANDLER);

            BUTTON_NEXT.addEventFilter(ActionEvent.ACTION, BUTTON_FINISH_EVENT_HANDLER);
            BUTTON_NEXT.addEventFilter(KeyEvent.KEY_PRESSED, BUTTON_FINISH_EVENT_HANDLER);
        }
        else {
            if (!BUTTON_NEXT.getText()
                            .equals("Next")) {
                BUTTON_NEXT.addEventFilter(ActionEvent.ACTION, BUTTON_NEXT_EVENT_HANDLER);
                BUTTON_NEXT.addEventFilter(KeyEvent.KEY_PRESSED, BUTTON_NEXT_EVENT_HANDLER);

                BUTTON_NEXT.removeEventFilter(ActionEvent.ACTION, BUTTON_FINISH_EVENT_HANDLER);
                BUTTON_NEXT.removeEventFilter(KeyEvent.KEY_PRESSED, BUTTON_FINISH_EVENT_HANDLER);
            }
            BUTTON_NEXT.setText("Next");
        }

        validateButton(BUTTON_PREVIOUS, pageHistory::isEmpty);
        validateButton(BUTTON_NEXT, invalidProperty::get);
    }

    // Functional design allows to delay condition evaluation until it is actually needed
    private static
    void validateButton(Button button, BooleanSupplier condition) {
        if ( button != null ) {
            button.setDisable(condition.getAsBoolean());
        }
    }

    private int settingCounter;

    private
    void readSettings(WizardPane page) {
        // for now we cannot know the structure of the page, so we just drill down
        // through the entire scenegraph (from page.content down) until we get
        // to the leaf nodes. We stop only if we find a node that is a
        // ValueContainer (either by implementing the interface), or being
        // listed in the internal valueContainers map.

        settingCounter = 0;
        checkNode(page.getContent());
    }

    private
    boolean checkNode(Node n) {
        boolean success = readSetting(n);

        if (success) {
            // we've added the setting to the settings map and we should stop drilling deeper
            return true;
        }
        else {
            /**
             * go into children of this node (if possible) and see if we can get
             * a value from them (recursively) We use reflection to fix
             * https://bitbucket.org/controlsfx/controlsfx/issue/412 .
             */
            List<Node> children = ImplUtils.getChildren(n, true);

            // we're doing a depth-first search, where we stop drilling down
            // once we hit a successful read
            boolean childSuccess = false;
            for (Node child : children) {
                childSuccess |= checkNode(child);
            }
            return childSuccess;
        }
    }

    private
    boolean readSetting(Node n) {
        if (n == null) {
            return false;
        }

        Object setting = ValueExtractor.getValue(n);

        if (setting != null) {
            // save it into the settings map.
            // if the node has an id set, we will use that as the setting name
            String settingName = n.getId();

            // but if the id is not set, we will use a generic naming scheme
            if (settingName == null || settingName.isEmpty()) {
                settingName = "page_" /*+ previousPageIndex*/ + ".setting_" + settingCounter;  //$NON-NLS-1$ //$NON-NLS-2$
            }

            getSettings().put(settingName, setting);

            settingCounter++;
        }

        return setting != null;
    }

    public
    void requestNextFocus() {
        BUTTON_NEXT.requestFocus();
    }

    public
    void requestPrevFocus() {
        BUTTON_PREVIOUS.requestFocus();
    }

    void notifyValidationChange(final WizardPane wizardPane, Collection<ValidationMessage> errors) {
        if (currentPage.orElse(null) == wizardPane) {
            Platform.runLater(() -> {
                boolean hasErrors = !errors.isEmpty();
                invalidProperty.set(hasErrors);

                final PopOver popOver = this.popOver;
                if (hasErrors) {
                    String errorText = errors.iterator()
                                             .next()
                                             .getText()
                                             .trim();

                    this.errorText.setText(errorText);

                    if (!popOver.isShowing()) {
                        popOver.setX(0);
                        popOver.setY(0);
                        popOver.show(BUTTON_NEXT, -10);
                    }
                }
                else {
                    popOver.hide();
                }

                validateActionState();
            });
        }
    }


    /**************************************************************************
     *
     * Support classes
     *
     **************************************************************************/


    /**
     * Represents the page flow of the wizard. It defines only methods required to move forward in the wizard logic, as backward movement is
     * automatically handled by wizard itself, using internal page history.
     */
    public
    interface Flow {

        /**
         * Advances the wizard to the next page if possible.
         *
         * @param currentPage
         *                 The current wizard page
         *
         * @return {@link Optional} value containing the next wizard page.
         */
        Optional<WizardPane> advance(WizardPane currentPage);

        /**
         * Check if advancing to the next page is possible
         *
         * @param currentPage
         *                 The current wizard page
         *
         * @return true if it is possible to advance to the next page, false otherwise.
         */
        boolean canAdvance(WizardPane currentPage);
    }


    /**
     * LinearFlow is an implementation of the {@link org.controlsfx.dialog.Wizard.Flow} interface, designed to support the most common type
     * of wizard flow - namely, a linear wizard page flow (i.e. through all pages in the order that they are specified). Therefore, this
     * {@link Flow} implementation simply traverses a collections of {@link WizardPane WizardPanes}.
     * <p>
     * <p>For example of how to use this API, please refer to the {@link org.controlsfx.dialog.Wizard} documentation</p>
     *
     * @see org.controlsfx.dialog.Wizard
     * @see WizardPane
     */
    public static
    class LinearFlow implements Wizard.Flow {

        private final List<WizardPane> pages;

        /**
         * Creates a new LinearFlow instance that will allow for stepping through the given collection of {@link WizardPane} instances.
         */
        public
        LinearFlow(Collection<WizardPane> pages) {
            this.pages = new ArrayList<>(pages);
        }

        /**
         * Creates a new LinearFlow instance that will allow for stepping through the given varargs array of {@link WizardPane} instances.
         */
        public
        LinearFlow(WizardPane... pages) {
            this(Arrays.asList(pages));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public
        Optional<WizardPane> advance(WizardPane currentPage) {
            int pageIndex = pages.indexOf(currentPage);
            return Optional.ofNullable(pages.get(++pageIndex));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public
        boolean canAdvance(WizardPane currentPage) {
            int pageIndex = pages.indexOf(currentPage);
            return pages.size() - 1 > pageIndex;
        }
    }
}
