package com.obd2.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Barra de status no rodape da aplicacao.
 */
public class StatusBar {

    private final HBox container;

    public StatusBar() {
        this.container = new HBox(15);
        buildUI();
    }

    private void buildUI() {
        container.setPadding(new Insets(5, 15, 5, 15));
        container.setAlignment(Pos.CENTER_LEFT);
        container.getStyleClass().add("status-bar");

        Label appLabel = new Label("OBD2 Car Connect v1.0.0");
        appLabel.getStyleClass().add("status-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label techLabel = new Label("Protocolos: CAN Bus | ISO 15765 | SAE J1850 | ISO 9141 | KWP2000");
        techLabel.getStyleClass().add("status-text");

        container.getChildren().addAll(appLabel, spacer, techLabel);
    }

    public Node getNode() {
        return container;
    }
}
