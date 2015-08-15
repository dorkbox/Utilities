package dorkbox.util.javafx;

import dorkbox.util.JavaFxUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.controlsfx.validation.ValidationMessage;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import java.util.Collection;

/**
 * WizardPane is the base class for all wizard pages. The API is essentially the {@link DialogPane}, with the addition of convenience
 * methods related to {@link #onEnteringPage(Wizard) entering} and {@link #onExitingPage(Wizard) exiting} the page.
 */
@SuppressWarnings("UnusedParameters")
public
class WizardPage {

    String headerText;
    Font headerFont;
    Node headerGraphic;

    Node anchorPane;

    Node firstFocusElement;

    final StringProperty invalidPropertyStrings = new SimpleStringProperty();
    final BooleanProperty invalidProperty = new SimpleBooleanProperty();
    final ValidationSupport validationSupport = new ValidationSupport();
    boolean autoFocusNext = false;

    /**
     * Creates an instance of wizard pane.
     */
    public
    WizardPage() {
        validationSupport.validationResultProperty()
                         .addListener((ObservableValue<? extends ValidationResult> o, ValidationResult ov, ValidationResult nv) -> {
                             final Collection<ValidationMessage> errors = nv.getErrors();

                             final boolean empty = errors.isEmpty();
                             if (empty) {
                                 invalidPropertyStrings.set(null);
                             } else {
                                 String errorText = errors.iterator()
                                                          .next()
                                                          .getText()
                                                          .trim();

                                 if (!errorText.equals(invalidPropertyStrings.get())) {
                                     invalidPropertyStrings.set(errorText);
                                 }
                             }
                             invalidProperty.set(!empty);
                         });
    }



    /**
     * Called on entering a page. This is a good place to read values from wizard settings and assign them to controls on the page
     *
     * @param wizard
     *                 which page will be used on
     */
    public
    void onEnteringPage(Wizard wizard) {
        // no-op
    }

    /**
     * Called on existing the page. This is a good place to read values from page controls and store them in wizard settings
     *
     * @param wizard
     *                 which page was used on
     */
    public
    void onExitingPage(Wizard wizard) {
        // no-opd
    }

    public
    void setHeaderText(final String headerText) {
        this.headerText = headerText;
    }

    public
    void setContent(final Node content) {
        // make this content fill the parent (which is a vbox)
        VBox.setVgrow(content, Priority.ALWAYS);
        this.anchorPane = content;
    }

    public
    void setContentText(final String contentText) {
        Text text = new Text();
        text.setFont(JavaFxUtil.DEFAULT_FONT);
        text.setText(contentText);

        // we use a Vbox, so that the text starts at the topleft.
        // if we used a stackpane, it would be centered in the parent node
        VBox region = new VBox();
        region.setPadding(new Insets(6));

        region.getChildren().add(text);
        region.setMinSize(0, 0);

        // make this content fill the parent (which is a vbox)
        VBox.setVgrow(region, Priority.ALWAYS);
        this.anchorPane = region;
    }


    public
    void setFirstFocusElement(final Node firstFocusElement) {
        this.firstFocusElement = firstFocusElement;
    }


    public
    void setHeaderFont(final Font headerFont) {
        this.headerFont = headerFont;
    }


    public
    void registerValidator(final Control control, final Validator<Object> validator) {
        this.validationSupport.registerValidator(control, validator);
    }

    /**
     * Necessary for enabling the "Next/Finish" button when a task is complete.
     */
    public
    void bind(final ObservableValue<Boolean> invalidProperty) {
        this.invalidProperty.bind(invalidProperty);
    }

    /**
     * Enables the wizard to automatically focus on the "Next" (or "Finish"), when this item is valid.
     */
    public
    void autoFocusNext() {
        autoFocusNext = true;
    }

    public
    void setHeaderGraphic(final Node headerGraphic) {
        this.headerGraphic = headerGraphic;
    }
}
