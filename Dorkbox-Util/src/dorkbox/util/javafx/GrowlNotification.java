/**
 * Copyright (c) 2014, ControlsFX All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are
 * met: * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution. * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products derived from this software without specific prior written
 * permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL CONTROLSFX
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * <p>
 * MODIFIED BY DORKBOX, LLC Copyright 2015 dorkbox, llc
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package dorkbox.util.javafx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public
class GrowlNotification extends Region {

    private static final double MIN_HEIGHT = 40;

    private final String textText;
    private final Node graphicNode;

    protected final GridPane pane;

    public
    GrowlNotification(final Growl notification) {
        this.textText = notification.text;
        this.graphicNode = notification.graphic;

        getStyleClass().add("notification-bar"); //$NON-NLS-1$

        setVisible(true);

        pane = new GridPane();
        pane.getStyleClass()
            .add("pane"); //$NON-NLS-1$
        pane.setAlignment(Pos.BASELINE_LEFT);
        getChildren().setAll(pane);

        // put it all together
        pane.getChildren()
            .clear();

        int row = 0;

        // title
        if (notification.title != null && !notification.title.isEmpty()) {
            Label title = new Label();
            title.getStyleClass()
                 .add("title"); //$NON-NLS-1$
            title.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            GridPane.setHgrow(title, Priority.ALWAYS);

            title.setText(notification.title);
            pane.add(title, 0, row++);
        }

        Region spacer = new Region();
        spacer.setPrefHeight(10);

        pane.add(spacer, 0, row++);

        // graphic + text area
        Label label = new Label();
        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        GridPane.setVgrow(label, Priority.ALWAYS);
        GridPane.setHgrow(label, Priority.ALWAYS);

        label.setText(textText);
        label.setGraphic(graphicNode);
        pane.add(label, 0, row);


        // close button
        if (!notification.hideCloseButton) {
            Button closeBtn = new Button();
            closeBtn.getStyleClass()
                    .setAll("close-button"); //$NON-NLS-1$

            StackPane graphic = new StackPane();
            graphic.getStyleClass()
                   .setAll("graphic"); //$NON-NLS-1$

            closeBtn.setGraphic(graphic);
            closeBtn.setMinSize(17, 17);
            closeBtn.setPrefSize(17, 17);

            GridPane.setMargin(closeBtn, new Insets(0, 0, 0, 8));

            // position the close button in the best place, depending on the height
            double minHeight = minHeight(-1);
            GridPane.setValignment(closeBtn, minHeight == MIN_HEIGHT ? VPos.CENTER : VPos.TOP);
            closeBtn.setOnAction(arg0 -> hide());

            pane.add(closeBtn, 2, 0, 1, row + 1);
        }
    }

    public
    void hide() {
    }

    @Override
    protected
    void layoutChildren() {
        final double w = getWidth();
        double h = computePrefHeight(-1);

        pane.resize(w, h);
    }

    @Override
    protected
    double computeMinWidth(double height) {
        String text = textText;
        Node graphic = graphicNode;

        if ((text == null || text.isEmpty()) && (graphic != null)) {
            return graphic.minWidth(height) + 20;
        }
        return 400;
    }

    @Override
    protected
    double computeMinHeight(double width) {
        String text = textText;
        Node graphic = graphicNode;

        if ((text == null || text.isEmpty()) && (graphic != null)) {
            return graphic.minHeight(width) + 20;
        }
        return 100;
    }

    @Override
    protected
    double computePrefHeight(double width) {
        return Math.max(pane.prefHeight(width), minHeight(width));
    }
}

