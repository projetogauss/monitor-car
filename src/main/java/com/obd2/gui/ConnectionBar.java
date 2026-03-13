package com.obd2.gui;

import com.fazecast.jSerialComm.SerialPort;
import com.obd2.connection.SerialConnection;
import com.obd2.service.OBDDataService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Barra de conexao serial (topo da janela).
 * Permite selecionar porta, baud rate, e conectar/desconectar.
 */
public class ConnectionBar {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionBar.class);

    private final HBox container;
    private final OBDDataService dataService;
    private final Consumer<Boolean> connectionCallback;
    private ComboBox<String> portCombo;
    private ComboBox<Integer> baudCombo;
    private Button connectButton;
    private Button refreshButton;
    private Circle statusIndicator;
    private Label statusLabel;
    private Label protocolLabel;

    public ConnectionBar(OBDDataService dataService, Consumer<Boolean> connectionCallback) {
        this.dataService = dataService;
        this.connectionCallback = connectionCallback;
        this.container = new HBox(10);
        buildUI();
    }

    private void buildUI() {
        container.setPadding(new Insets(10, 15, 10, 15));
        container.setAlignment(Pos.CENTER_LEFT);
        container.getStyleClass().add("connection-bar");

        // Status indicator
        statusIndicator = new Circle(8, Color.RED);
        statusLabel = new Label("Desconectado");
        statusLabel.getStyleClass().add("status-label");

        // Porta serial
        Label portLabel = new Label("Porta:");
        portLabel.getStyleClass().add("field-label");
        portCombo = new ComboBox<>();
        portCombo.setPrefWidth(200);
        portCombo.setPromptText("Selecione a porta...");
        refreshPorts();

        refreshButton = new Button("\u21BB");
        refreshButton.getStyleClass().add("icon-button");
        refreshButton.setTooltip(new Tooltip("Atualizar portas"));
        refreshButton.setOnAction(e -> refreshPorts());

        // Baud rate
        Label baudLabel = new Label("Baud Rate:");
        baudLabel.getStyleClass().add("field-label");
        baudCombo = new ComboBox<>();
        baudCombo.getItems().addAll(9600, 19200, 38400, 57600, 115200, 230400, 500000);
        baudCombo.setValue(38400);
        baudCombo.setPrefWidth(100);

        // Botao de conexao
        connectButton = new Button("Conectar");
        connectButton.getStyleClass().add("connect-button");
        connectButton.setOnAction(e -> toggleConnection());

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Protocolo detectado
        protocolLabel = new Label("");
        protocolLabel.getStyleClass().add("protocol-label");

        container.getChildren().addAll(
                statusIndicator, statusLabel,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                portLabel, portCombo, refreshButton,
                baudLabel, baudCombo,
                connectButton,
                spacer,
                protocolLabel
        );
    }

    private void refreshPorts() {
        portCombo.getItems().clear();
        SerialPort[] ports = SerialConnection.getAvailablePorts();
        for (SerialPort port : ports) {
            portCombo.getItems().add(port.getSystemPortName());
        }
        if (!portCombo.getItems().isEmpty()) {
            portCombo.setValue(portCombo.getItems().get(0));
        }
    }

    private void toggleConnection() {
        if (dataService.isConnected()) {
            disconnect();
        } else {
            connect();
        }
    }

    private void connect() {
        String port = portCombo.getValue();
        if (port == null || port.isEmpty()) {
            showAlert("Selecione uma porta serial primeiro!");
            return;
        }

        int baudRate = baudCombo.getValue();

        connectButton.setDisable(true);
        connectButton.setText("Conectando...");

        Thread connectThread = new Thread(() -> {
            boolean success = dataService.connect(port, baudRate);
            Platform.runLater(() -> {
                if (success) {
                    statusIndicator.setFill(Color.LIMEGREEN);
                    statusLabel.setText("Conectado");
                    connectButton.setText("Desconectar");
                    connectButton.getStyleClass().remove("connect-button");
                    connectButton.getStyleClass().add("disconnect-button");
                    portCombo.setDisable(true);
                    baudCombo.setDisable(true);
                    protocolLabel.setText("Protocolo: " + dataService.getDetectedProtocol());
                    connectionCallback.accept(true);
                } else {
                    statusIndicator.setFill(Color.RED);
                    statusLabel.setText("Falha na conexao");
                    connectButton.setText("Conectar");
                    showAlert("Nao foi possivel conectar.\nVerifique se o adaptador esta conectado e a ignicao ligada.");
                }
                connectButton.setDisable(false);
            });
        }, "Connection-Thread");
        connectThread.setDaemon(true);
        connectThread.start();
    }

    private void disconnect() {
        dataService.disconnect();
        statusIndicator.setFill(Color.RED);
        statusLabel.setText("Desconectado");
        connectButton.setText("Conectar");
        connectButton.getStyleClass().remove("disconnect-button");
        connectButton.getStyleClass().add("connect-button");
        portCombo.setDisable(false);
        baudCombo.setDisable(false);
        protocolLabel.setText("");
        connectionCallback.accept(false);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public Node getNode() {
        return container;
    }
}
