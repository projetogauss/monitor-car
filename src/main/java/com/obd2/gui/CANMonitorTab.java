package com.obd2.gui;

import com.obd2.connection.ELM327Initializer;
import com.obd2.model.CANMessage;
import com.obd2.service.CANMonitorService;
import com.obd2.service.OBDDataService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aba de monitoramento da rede CAN Bus.
 * Permite sniffing de mensagens, filtragem e envio de comandos CAN.
 */
public class CANMonitorTab {

    private static final Logger logger = LoggerFactory.getLogger(CANMonitorTab.class);

    private final VBox container;
    private final CANMonitorService canService;
    private final OBDDataService dataService;
    private TableView<CANMessage> messageTable;
    private ObservableList<CANMessage> messageList;
    private Button startButton;
    private Button stopButton;
    private TextField filterField;
    private Label countLabel;
    private boolean autoScroll;

    public CANMonitorTab(CANMonitorService canService, OBDDataService dataService) {
        this.canService = canService;
        this.dataService = dataService;
        this.container = new VBox(10);
        this.messageList = FXCollections.observableArrayList();
        this.autoScroll = true;
        buildUI();
    }

    @SuppressWarnings("unchecked")
    private void buildUI() {
        container.setPadding(new Insets(20));

        Label title = new Label("Monitor CAN Bus - Rede do Veiculo");
        title.getStyleClass().add("section-title");

        // Controles
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);

        startButton = new Button("Iniciar Monitor");
        startButton.getStyleClass().add("action-button");
        startButton.setOnAction(e -> startMonitoring());

        stopButton = new Button("Parar Monitor");
        stopButton.getStyleClass().add("danger-button");
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> stopMonitoring());

        Button clearButton = new Button("Limpar");
        clearButton.setOnAction(e -> {
            messageList.clear();
            canService.clearCapturedMessages();
            countLabel.setText("Mensagens: 0");
        });

        Label filterLabel = new Label("Filtro CAN ID:");
        filterField = new TextField();
        filterField.setPromptText("Ex: 7E8");
        filterField.setPrefWidth(100);

        CheckBox autoScrollCheck = new CheckBox("Auto-scroll");
        autoScrollCheck.setSelected(true);
        autoScrollCheck.setOnAction(e -> autoScroll = autoScrollCheck.isSelected());

        countLabel = new Label("Mensagens: 0");
        countLabel.getStyleClass().add("info-label");

        controls.getChildren().addAll(
                startButton, stopButton, clearButton,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                filterLabel, filterField,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                autoScrollCheck, countLabel
        );

        // Protocolo selector
        HBox protocolBar = new HBox(10);
        protocolBar.setAlignment(Pos.CENTER_LEFT);
        Label protoLabel = new Label("Protocolo:");
        ComboBox<String> protocolCombo = new ComboBox<>();
        for (ELM327Initializer.OBDProtocol proto : ELM327Initializer.OBDProtocol.values()) {
            if (proto.isCAN()) {
                protocolCombo.getItems().add(proto.getDescription());
            }
        }
        protocolCombo.setValue("ISO 15765-4 CAN (11 bit ID, 500 kbaud)");
        protocolBar.getChildren().addAll(protoLabel, protocolCombo);

        // Tabela de mensagens CAN
        messageTable = new TableView<>();
        messageTable.setPlaceholder(new Label("Nenhuma mensagem CAN capturada. Inicie o monitor."));
        messageTable.getStyleClass().add("can-table");
        VBox.setVgrow(messageTable, Priority.ALWAYS);

        TableColumn<CANMessage, String> timeCol = new TableColumn<>("Timestamp");
        timeCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getTimestamp().toLocalTime().toString()));
        timeCol.setPrefWidth(120);

        TableColumn<CANMessage, String> idCol = new TableColumn<>("CAN ID");
        idCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCanId()));
        idCol.setPrefWidth(100);

        TableColumn<CANMessage, String> dlcCol = new TableColumn<>("DLC");
        dlcCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        String.valueOf(cellData.getValue().getDataLength())));
        dlcCol.setPrefWidth(60);

        TableColumn<CANMessage, String> dataCol = new TableColumn<>("Dados (Hex)");
        dataCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDataHex()));
        dataCol.setPrefWidth(350);

        TableColumn<CANMessage, String> typeCol = new TableColumn<>("Tipo ID");
        typeCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().isExtendedId() ? "29-bit" : "11-bit"));
        typeCol.setPrefWidth(80);

        messageTable.getColumns().addAll(timeCol, idCol, dlcCol, dataCol, typeCol);
        messageTable.setItems(messageList);

        // Envio de mensagem CAN
        HBox sendBar = new HBox(10);
        sendBar.setAlignment(Pos.CENTER_LEFT);
        sendBar.getStyleClass().add("send-bar");
        sendBar.setPadding(new Insets(10));

        Label sendLabel = new Label("Enviar CAN:");
        TextField canIdField = new TextField();
        canIdField.setPromptText("CAN ID (ex: 7DF)");
        canIdField.setPrefWidth(120);

        TextField canDataField = new TextField();
        canDataField.setPromptText("Dados hex (ex: 0201050000000000)");
        canDataField.setPrefWidth(300);

        Button sendButton = new Button("Enviar");
        sendButton.getStyleClass().add("action-button");
        sendButton.setOnAction(e -> {
            String canId = canIdField.getText().trim();
            String data = canDataField.getText().trim();
            if (!canId.isEmpty() && !data.isEmpty()) {
                sendCANMessage(canId, data);
            }
        });

        Label sendResponseLabel = new Label("");
        sendResponseLabel.getStyleClass().add("info-label");

        sendBar.getChildren().addAll(sendLabel, canIdField, canDataField, sendButton, sendResponseLabel);

        container.getChildren().addAll(title, controls, protocolBar, messageTable, sendBar);
    }

    private void startMonitoring() {
        if (!dataService.isConnected()) {
            showAlert("Conecte-se ao veiculo primeiro!");
            return;
        }

        String filter = filterField.getText().trim();
        if (!filter.isEmpty()) {
            canService.setFilterCanId(filter);
        } else {
            canService.clearFilter();
        }

        messageList.clear();
        startButton.setDisable(true);
        stopButton.setDisable(false);

        canService.startMonitoring(message -> {
            Platform.runLater(() -> {
                messageList.add(message);
                countLabel.setText("Mensagens: " + messageList.size());
                if (autoScroll && !messageList.isEmpty()) {
                    messageTable.scrollTo(messageList.size() - 1);
                }
            });
        });

        logger.info("Monitoramento CAN iniciado");
    }

    public void stopMonitoring() {
        canService.stopMonitoring();
        startButton.setDisable(false);
        stopButton.setDisable(true);
        logger.info("Monitoramento CAN parado");
    }

    private void sendCANMessage(String canId, String data) {
        Thread sendThread = new Thread(() -> {
            String response = canService.sendCANMessage(canId, data);
            Platform.runLater(() -> {
                if (response != null) {
                    logger.info("Resposta CAN: {}", response);
                }
            });
        }, "CAN-Send-Thread");
        sendThread.setDaemon(true);
        sendThread.start();
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
