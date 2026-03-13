package com.obd2.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

/**
 * Widget de gauge individual para exibir um parametro do veiculo.
 * Mostra nome, valor numerico, unidade e barra de progresso.
 */
public class GaugeWidget {

    private final VBox container;
    private final Label nameLabel;
    private final Label valueLabel;
    private final ProgressBar progressBar;
    private final double min;
    private final double max;
    private final String unit;

    public GaugeWidget(String name, String unit, double min, double max) {
        this.min = min;
        this.max = max;
        this.unit = unit;
        this.container = new VBox(5);
        this.nameLabel = new Label(name);
        this.valueLabel = new Label("--");
        this.progressBar = new ProgressBar(0);
        buildUI();
    }

    private void buildUI() {
        container.setPadding(new Insets(15));
        container.setAlignment(Pos.CENTER);
        container.setPrefWidth(280);
        container.setPrefHeight(130);
        container.getStyleClass().add("gauge-widget");

        nameLabel.getStyleClass().add("gauge-name");

        valueLabel.getStyleClass().add("gauge-value");

        Label unitLabel = new Label(unit);
        unitLabel.getStyleClass().add("gauge-unit");

        progressBar.setPrefWidth(250);
        progressBar.setPrefHeight(12);
        progressBar.getStyleClass().add("gauge-progress");

        container.getChildren().addAll(nameLabel, valueLabel, unitLabel, progressBar);
    }

    public void setValue(double value) {
        valueLabel.setText(String.format("%.1f", value));
        double progress = (value - min) / (max - min);
        progress = Math.max(0, Math.min(1, progress));
        progressBar.setProgress(progress);

        // Mudar cor baseado no nivel
        progressBar.getStyleClass().removeAll("progress-warning", "progress-danger");
        if (progress > 0.9) {
            progressBar.getStyleClass().add("progress-danger");
        } else if (progress > 0.75) {
            progressBar.getStyleClass().add("progress-warning");
        }
    }

    public void setTextValue(String text) {
        valueLabel.setText(text);
    }

    public Node getNode() {
        return container;
    }
}
