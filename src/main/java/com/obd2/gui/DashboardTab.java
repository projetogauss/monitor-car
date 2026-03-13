package com.obd2.gui;

import com.obd2.command.OBDCommand;
import com.obd2.model.OBDData;
import com.obd2.service.OBDDataService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Aba de dashboard com gauges e dados em tempo real do veiculo.
 */
public class DashboardTab {

    private static final Logger logger = LoggerFactory.getLogger(DashboardTab.class);

    private final VBox container;
    private final OBDDataService dataService;
    private final Map<OBDCommand, GaugeWidget> gaugeWidgets;
    private ScheduledExecutorService scheduler;

    public DashboardTab(OBDDataService dataService) {
        this.dataService = dataService;
        this.gaugeWidgets = new LinkedHashMap<>();
        this.container = new VBox(15);
        buildUI();
    }

    private void buildUI() {
        container.setPadding(new Insets(20));
        container.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Dashboard - Dados em Tempo Real");
        title.getStyleClass().add("section-title");

        // Grid de gauges
        GridPane gaugeGrid = new GridPane();
        gaugeGrid.setHgap(15);
        gaugeGrid.setVgap(15);
        gaugeGrid.setAlignment(Pos.CENTER);

        // Criar widgets para cada parametro monitorado
        addGauge(gaugeGrid, OBDCommand.ENGINE_RPM, "RPM", 0, 8000, 0, 0);
        addGauge(gaugeGrid, OBDCommand.VEHICLE_SPEED, "km/h", 0, 260, 1, 0);
        addGauge(gaugeGrid, OBDCommand.COOLANT_TEMP, "\u00B0C", -40, 215, 2, 0);
        addGauge(gaugeGrid, OBDCommand.ENGINE_LOAD, "%", 0, 100, 0, 1);
        addGauge(gaugeGrid, OBDCommand.THROTTLE_POSITION, "%", 0, 100, 1, 1);
        addGauge(gaugeGrid, OBDCommand.INTAKE_AIR_TEMP, "\u00B0C", -40, 215, 2, 1);
        addGauge(gaugeGrid, OBDCommand.MAF_FLOW_RATE, "g/s", 0, 655, 0, 2);
        addGauge(gaugeGrid, OBDCommand.FUEL_TANK_LEVEL, "%", 0, 100, 1, 2);

        container.getChildren().addAll(title, gaugeGrid);
    }

    private void addGauge(GridPane grid, OBDCommand command, String unit,
                          double min, double max, int col, int row) {
        GaugeWidget widget = new GaugeWidget(command.getDescription(), unit, min, max);
        gaugeWidgets.put(command, widget);
        grid.add(widget.getNode(), col, row);
    }

    /**
     * Inicia a leitura ciclica de dados.
     */
    public void startReading() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "OBD-Reader-Thread");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> {
            try {
                Map<OBDCommand, OBDData> data = dataService.readAllMonitored();
                Platform.runLater(() -> updateGauges(data));
            } catch (Exception e) {
                logger.error("Erro ao ler dados OBD2", e);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);

        logger.info("Leitura ciclica de dados iniciada");
    }

    /**
     * Para a leitura ciclica.
     */
    public void stopReading() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        // Reset gauges
        for (GaugeWidget widget : gaugeWidgets.values()) {
            widget.setValue(0);
        }
    }

    private void updateGauges(Map<OBDCommand, OBDData> data) {
        for (Map.Entry<OBDCommand, OBDData> entry : data.entrySet()) {
            GaugeWidget widget = gaugeWidgets.get(entry.getKey());
            if (widget != null && entry.getValue() != null) {
                try {
                    double value = Double.parseDouble(entry.getValue().getValue());
                    widget.setValue(value);
                } catch (NumberFormatException e) {
                    widget.setTextValue(entry.getValue().getValue());
                }
            }
        }
    }

    public Node getNode() {
        return container;
    }
}
