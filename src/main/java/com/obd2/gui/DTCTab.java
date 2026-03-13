package com.obd2.gui;

import com.obd2.model.DTCCode;
import com.obd2.service.OBDDataService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Aba de leitura e limpeza de codigos de falha (DTC).
 */
public class DTCTab {

    private static final Logger logger = LoggerFactory.getLogger(DTCTab.class);

    private final VBox container;
    private final OBDDataService dataService;
    private TableView<DTCCode> dtcTable;
    private ObservableList<DTCCode> dtcList;
    private Label statusLabel;

    public DTCTab(OBDDataService dataService) {
        this.dataService = dataService;
        this.container = new VBox(15);
        this.dtcList = FXCollections.observableArrayList();
        buildUI();
    }

    @SuppressWarnings("unchecked")
    private void buildUI() {
        container.setPadding(new Insets(20));

        Label title = new Label("Codigos de Diagnostico de Falha (DTC)");
        title.getStyleClass().add("section-title");

        // Botoes
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_LEFT);

        Button readButton = new Button("Ler DTCs");
        readButton.getStyleClass().add("action-button");
        readButton.setOnAction(e -> readDTCs());

        Button clearButton = new Button("Limpar DTCs");
        clearButton.getStyleClass().add("danger-button");
        clearButton.setOnAction(e -> clearDTCs());

        statusLabel = new Label("Clique em 'Ler DTCs' para verificar codigos de falha");
        statusLabel.getStyleClass().add("info-label");

        buttonBar.getChildren().addAll(readButton, clearButton, statusLabel);

        // Tabela de DTCs
        dtcTable = new TableView<>();
        dtcTable.setPlaceholder(new Label("Nenhum codigo de falha encontrado"));
        dtcTable.getStyleClass().add("dtc-table");
        VBox.setVgrow(dtcTable, Priority.ALWAYS);

        TableColumn<DTCCode, String> codeCol = new TableColumn<>("Codigo");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        codeCol.setPrefWidth(120);

        TableColumn<DTCCode, String> categoryCol = new TableColumn<>("Categoria");
        categoryCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getCategory().getDescription()));
        categoryCol.setPrefWidth(250);

        TableColumn<DTCCode, String> descCol = new TableColumn<>("Descricao");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(400);

        dtcTable.getColumns().addAll(codeCol, categoryCol, descCol);
        dtcTable.setItems(dtcList);

        // Info box
        VBox infoBox = new VBox(5);
        infoBox.getStyleClass().add("info-box");
        infoBox.setPadding(new Insets(10));
        Label infoTitle = new Label("Categorias de DTC:");
        infoTitle.setStyle("-fx-font-weight: bold;");
        Label infoP = new Label("P - Powertrain (Motor/Transmissao)");
        Label infoC = new Label("C - Chassis");
        Label infoB = new Label("B - Body (Carroceria)");
        Label infoU = new Label("U - Network (Rede de Comunicacao/CAN Bus)");
        infoBox.getChildren().addAll(infoTitle, infoP, infoC, infoB, infoU);

        container.getChildren().addAll(title, buttonBar, dtcTable, infoBox);
    }

    private void readDTCs() {
        if (!dataService.isConnected()) {
            showAlert("Conecte-se ao veiculo primeiro!");
            return;
        }

        statusLabel.setText("Lendo codigos de falha...");

        Thread readThread = new Thread(() -> {
            List<DTCCode> codes = dataService.readDTCs();
            Platform.runLater(() -> {
                dtcList.clear();
                dtcList.addAll(codes);
                if (codes.isEmpty()) {
                    statusLabel.setText("Nenhum codigo de falha encontrado (MIL desligada)");
                } else {
                    statusLabel.setText(codes.size() + " codigo(s) de falha encontrado(s)");
                }
            });
        }, "DTC-Reader-Thread");
        readThread.setDaemon(true);
        readThread.start();
    }

    private void clearDTCs() {
        if (!dataService.isConnected()) {
            showAlert("Conecte-se ao veiculo primeiro!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar");
        confirm.setHeaderText("Limpar codigos de falha?");
        confirm.setContentText("Isso vai apagar todos os DTCs e desligar a luz de Check Engine.\nDeseja continuar?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Thread clearThread = new Thread(() -> {
                    boolean success = dataService.clearDTCs();
                    Platform.runLater(() -> {
                        if (success) {
                            dtcList.clear();
                            statusLabel.setText("Codigos de falha limpos com sucesso!");
                        } else {
                            statusLabel.setText("Falha ao limpar codigos de falha");
                        }
                    });
                }, "DTC-Clear-Thread");
                clearThread.setDaemon(true);
                clearThread.start();
            }
        });
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
