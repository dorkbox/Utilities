package dorkbox.util.javafx;

import dorkbox.util.ScreenUtil;
import dorkbox.util.SwingUtil;
import javafx.animation.*;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.tools.Utils;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;

/**
 *
 */
public
class GrowlPopup {

    private static final java.util.List<GrowlPopup> popups = new ArrayList<>();
    // for animating in the notifications
    private static final ParallelTransition parallelTransition = new ParallelTransition();

    private static final double padding = 40;

    private final GrowlPopupViaSwing frame;

    final double startX;
    final double startY;
    final javafx.stage.Window window;
    final double screenWidth;
    final double screenHeight;

    private final Pos position;

    private final double anchorX;
    private final double anchorY;

    final Timeline animationTimeline = new Timeline();
    private double newX;
    private double newY;

    GrowlPopup(final Growl notification) {
        final Image icon;
        if (notification.graphic instanceof ImageView) {
            icon = SwingFXUtils.fromFXImage(((ImageView) notification.graphic).getImage(), null);
        } else {
            icon = SwingUtil.BLANK_ICON;
        }

        // created on the swing EDT
        frame = GrowlPopupViaSwing.create(icon, notification.title);
        // don't actually show anything. This will be done by our own animator
        frame.setShowAnimation(() -> {
            frame.completeShowTransition();
        });

        // set screen position
        final javafx.stage.Window owner = notification.owner;
        if (owner == null) {
            final Point mouseLocation = MouseInfo.getPointerInfo()
                                                 .getLocation();

            final GraphicsDevice deviceAtMouse = ScreenUtil.getGraphicsDeviceAt(mouseLocation);

            final Rectangle screenBounds = deviceAtMouse.getDefaultConfiguration()
                                                        .getBounds();

            /*
             * If the owner is not set, we work with the whole screen.
             * EDIT: we use the screen that the mouse is currently on.
             */
            startX = screenBounds.getX();
            startY = screenBounds.getY();
            screenWidth = screenBounds.getWidth();
            screenHeight = screenBounds.getHeight();

            window = Utils.getWindow(null);
        }
        else {
            /*
             * If the owner is set, we will make the notifications popup
             * inside its window.
             */
            startX = owner.getX();
            startY = owner.getY();
            screenWidth = owner.getWidth();
            screenHeight = owner.getHeight();
            window = owner;
        }


        // need to install our CSS
        if (owner instanceof Stage) {
            Scene ownerScene = owner.getScene();
            ownerScene.getStylesheets()
                      .add(org.controlsfx.control.Notifications.class.getResource("notificationpopup.css")
                                                                     .toExternalForm()); //$NON-NLS-1$
        }


        this.position = notification.position;


        VBox region = new VBox();
        final ObservableList<String> styleClass1 = region.getStyleClass();
        styleClass1.add("notification-bar");
        styleClass1.addAll(notification.styleClass);

        region.setVisible(true);
        region.setMinWidth(300);
        region.setMinHeight(40);


        GridPane pane = new GridPane();
        pane.getStyleClass()
            .add("pane");
        pane.setAlignment(Pos.BASELINE_LEFT);
        region.getChildren()
              .add(pane);

//            pane.setStyle("-fx-background-color: #2046ff;");

        // title
        if (notification.title != null && !notification.title.isEmpty()) {
            javafx.scene.control.Label titleLabel = new javafx.scene.control.Label();
            titleLabel.getStyleClass()
                 .add("title");
            titleLabel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            GridPane.setHgrow(titleLabel, Priority.ALWAYS);

            titleLabel.setText(notification.title);
            pane.add(titleLabel, 0, 0);
        }


        // close button
        if (!notification.hideCloseButton) {
            javafx.scene.control.Button closeBtn = new javafx.scene.control.Button();
            closeBtn.getStyleClass()
                    .setAll("close-button");

            StackPane graphic = new StackPane();
            graphic.getStyleClass()
                   .setAll("graphic");

            closeBtn.setGraphic(graphic);
            closeBtn.setMinSize(17, 17);
            closeBtn.setPrefSize(17, 17);

            GridPane.setMargin(closeBtn, new javafx.geometry.Insets(0, 0, 0, 8));

            // position the close button in the best place, depending on the height
            double minHeight = pane.minHeight(-1);
            GridPane.setValignment(closeBtn, minHeight == 40 ? VPos.CENTER : VPos.TOP);

            closeBtn.setOnAction(arg0 -> createHideTimeline(Duration.ZERO).play());

            pane.add(closeBtn, 2, 0, 1, 1);
        }


        // graphic + text area
        javafx.scene.control.Label label = new javafx.scene.control.Label();
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        GridPane.setVgrow(label, Priority.ALWAYS);
        GridPane.setHgrow(label, Priority.ALWAYS);

        label.setText(notification.text);
        label.setGraphic(notification.graphic);
        label.setPadding(new javafx.geometry.Insets(10, 0, 10, 5));
        pane.add(label, 0, 2);


        region.setOnMouseClicked(e -> createHideTimeline(Duration.ZERO).play());

        Scene scene = new Scene(region);
        scene.getStylesheets()
             .add(org.controlsfx.control.Notifications.class.getResource("notificationpopup.css")
                                                            .toExternalForm()); //$NON-NLS-1$
        frame.setScene(scene);

        frame.sizeToScene();

        // determine location for the popup
        final Dimension size = frame.getSize();
        final double barWidth = size.getWidth();
        final double barHeight = size.getHeight();

        // get anchorX
        switch (position) {
            case TOP_LEFT:
            case CENTER_LEFT:
            case BOTTOM_LEFT:
                anchorX = startX + padding;
                break;

            case TOP_CENTER:
            case CENTER:
            case BOTTOM_CENTER:
                anchorX = startX + (screenWidth / 2.0) - barWidth / 2.0 - padding / 2.0;
                break;

            default:
            case TOP_RIGHT:
            case CENTER_RIGHT:
            case BOTTOM_RIGHT:
                anchorX = startX + screenWidth - barWidth - padding;
                break;
        }

        // get anchorY
        switch (position) {
            case TOP_LEFT:
            case TOP_CENTER:
            case TOP_RIGHT:
                anchorY = padding + startY;
                break;

            case CENTER_LEFT:
            case CENTER:
            case CENTER_RIGHT:
                anchorY = startY + (screenHeight / 2.0) - barHeight / 2.0 - padding / 2.0;
                break;

            default:
            case BOTTOM_LEFT:
            case BOTTOM_CENTER:
            case BOTTOM_RIGHT:
                anchorY = startY + screenHeight - barHeight - padding;
                break;
        }
    }

    public
    void show() {
        this.newX = anchorX;
        this.newY = anchorY;
        frame.show(anchorX, anchorY);

        addPopupToMap();

        // begin a timeline to get rid of the popup (default is 5 seconds)
//            if (notification.hideAfterDuration != Duration.INDEFINITE) {
//                Timeline timeline = createHideTimeline(popup, growlNotification, p, notification.hideAfterDuration);
//                timeline.play();
//            }
    }

    @Override
    public
    boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final GrowlPopup that = (GrowlPopup) o;
        return frame.equals(that.frame);
    }

    @Override
    public
    int hashCode() {
        return frame.hashCode();
    }

    void close() {
        // set it off screen (which is what the close method also does)
        this.newX = Short.MIN_VALUE;
        this.newY = Short.MIN_VALUE;

        frame.close();
    }

    Dimension2D getSize() {
        return frame.getSize();
    }

    void animateToTarget(final boolean shouldFadeIn, final double x, final double y) {

        if (shouldFadeIn) {
            if (frame.getOpacityProperty().getValue() == 0F) {
                frame.setLocation((int)x, (int)y);
                Timeline timeline = new Timeline();
                timeline.getKeyFrames()
                        .addAll(new KeyFrame(Duration.millis(500), new KeyValue(frame.getOpacityProperty(), 1F, Interpolator.LINEAR)));
                timeline.play();
            }
        } else {
            frame.setLocation((int)x, (int)y);

//            final boolean xEqual = x == frame.getX();
//            final boolean yEqual = y == frame.getY();
//
//            if (xEqual && yEqual) {
//                return;
//            }
//Transition t = new Transition() {
//                {
//                    setCycleDuration(Duration.millis(500));
//                }
//
//                @Override
//                protected
//                void interpolate(final double frac) {
//                    final double y1 = frame.getY();
//                    final double distance = ((y-y1) * frac);
//
//                    frame.setLocation(x, y1 + distance);
//                }
//            };
//            parallelTransition.getChildren().add(t);
        }

//                final ObservableList<KeyFrame> keyFrames = animationTimeline.getKeyFrames();
//                keyFrames.clear();
//
//                if (!xEqual) {
//                    keyFrames.addAll(new KeyFrame(Duration.millis(300), new KeyValue(xProperty, x, Interpolator.EASE_OUT)));
//                }
//                if (!yEqual) {
//                    keyFrames.addAll(new KeyFrame(Duration.millis(300), new KeyValue(yProperty, y, Interpolator.EASE_OUT)));
//                }
//
//                // x/y can change, keep running the animation until it's stable
//                animationTimeline.setOnFinished(event -> animateToTarget(GrowlPopup.this.newX, GrowlPopup.this.newY));
//                animationTimeline.playFromStart();
//            }
//        }
    }

    private
    Timeline createHideTimeline(final Duration startDelay) {
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(500), new KeyValue(frame.getOpacityProperty(), 0.0F)));
        timeline.setDelay(startDelay);
        timeline.setOnFinished(e -> {
            close();
            removePopupFromMap();
        });

        return timeline;
    }

    // only called on the JavaFX app thread
    private
    void addPopupToMap() {
        popups.add(this);
        doAnimation(true);
    }

    // only called on the JavaFX app thread
    private
    void removePopupFromMap() {
        popups.remove(this);

        if (!popups.isEmpty()) {
            doAnimation(false);
        }
    }

    // only called on the JavaFX app thread
    private static
    void doAnimation(boolean shouldFadeIn) {
        parallelTransition.stop();
        parallelTransition.getChildren()
                          .clear();


        // the logic for this, is that the first popup in place, doesn't move. EVERY other popup after it will be moved
        // this behavior trickles down to the remaining popups, until all popups have been assigned new locations

        final int length = popups.size();
        final GrowlPopup[] copies = popups.toArray(new GrowlPopup[length]);

        for (int i = 0; i < length; i++) {
            final GrowlPopup popup = copies[i];
            final boolean isShowFromTop = isShowFromTop(popup.position);

            final Dimension2D size = popup.getSize();
            final double x = popup.newX;
            final double y = popup.newY;
            final double width = size.getWidth();
            final double height = size.getHeight();

            if (isShowFromTop) {
                for (int j = i+1; j < length; j++) {
                    final GrowlPopup copy = copies[j];

                    final Dimension2D size1 = copy.getSize();
                    final double x1 = copy.newX;
                    final double y1 = copy.newY;
                    final double width1 = size1.getWidth();
                    final double height1 = size1.getHeight();

                    if (intersectRect(x, y, width, height, x1, y1, width1, height1)) {
                        copy.newY = y + height + 10;
                    }
                }

                popup.animateToTarget(shouldFadeIn, popup.newX, popup.newY);
            }

//
//                    // first one is always as base location with padding
//                    if (i == 0) {
//                        newY = 30 + _popup.startY;
//                    }
//                    else {
//                        // we add a little bit of padding, so they are not on top of eachother
//                        newY += popupHeight + 10;
//                    }
//                }
//                else {
//                    if (i == size - 1) {
////                    newY = changedPopup.getTargetY() - popupHeight;
//                    }
//                    else {
//                        newY -= popupHeight;
//                    }
//                }
//
//                if (newY < 0) {
//                    System.err.println("closing");
//                    _popup.close();
//                    continue;
//                }

//            popup.animateToTarget(popup.anchorX, newY);
        }

        if (!parallelTransition.getChildren().isEmpty()) {
//            parallelTransition.play();
        }
    }

    static boolean intersectRect(double x1, double y1, double w1, double h1, double x2, double y2, double w2, double h2) {
        return intersectRange(x1, x1+w1, x2, x2+w2) && intersectRange(y1, y1+h1, y2, y2+h2);
    }
    static boolean intersectRange(double ax1, double ax2, double bx1, double bx2) {
        return Math.max(ax1, bx1) <= Math.min(ax2, bx2);
    }

    private static
    boolean isShowFromTop(final Pos p) {
        switch (p) {
            case TOP_LEFT:
            case TOP_CENTER:
            case TOP_RIGHT:
                return true;
            default:
                return false;
        }
    }
}
