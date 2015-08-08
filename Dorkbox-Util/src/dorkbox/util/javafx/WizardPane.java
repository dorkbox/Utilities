package dorkbox.util.javafx;

import dorkbox.util.JavaFxUtil;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.controlsfx.validation.ValidationMessage;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import java.util.Collection;
import java.util.Collections;

/**
 * WizardPane is the base class for all wizard pages. The API is essentially the {@link DialogPane}, with the addition of convenience
 * methods related to {@link #onEnteringPage(Wizard) entering} and {@link #onExitingPage(Wizard) exiting} the page.
 */
public
class WizardPane {

    String headerText;
    Font headerFont;

    AnchorPane content = new AnchorPane();

    Node firstFocusElement;

    ValidationSupport validationSupport = new ValidationSupport();
    volatile Collection<ValidationMessage> validationErrors = Collections.emptyList();

    /**
     * Creates an instance of wizard pane.
     * @param wizard necessary for validation support, to notify the wizard when this page becomes valid
     */
    public
    WizardPane(final Wizard wizard) {
        validationSupport.validationResultProperty()
                         .addListener((o, ov, nv) -> {
                             validationErrors = nv.getErrors();
                             wizard.notifyValidationChange(this, nv.getErrors());
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
    void setContent(final Region content) {
        content.setMinSize(0, 0);

        AnchorPane.setTopAnchor(content, 0.0);
        AnchorPane.setRightAnchor(content, 0.0);
        AnchorPane.setLeftAnchor(content, 0.0);
        AnchorPane.setBottomAnchor(content, 0.0);

        this.content.getChildren().setAll(content);
    }

    public
    void setContentText(final String contentText) {
        Text text = new Text();
        text.setFont(JavaFxUtil.DEFAULT_FONT);
        text.setText(contentText);

        VBox region = new VBox();
        region.getChildren().add(text);

        region.setMinSize(0, 0);

        AnchorPane.setTopAnchor(region, 0.0);
        AnchorPane.setRightAnchor(region, 0.0);
        AnchorPane.setLeftAnchor(region, 0.0);
        AnchorPane.setBottomAnchor(region, 0.0);

        this.content.getChildren().setAll(region);
    }

    public
    String getHeaderText() {
        return headerText;
    }

    public
    Region getContent() {
        return content;
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
}
