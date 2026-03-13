package com.obd2.gui;

import com.obd2.service.OBDDataService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Aba de terminal OBD para envio de comandos manuais.
 * Permite enviar comandos AT e OBD2 diretamente ao adaptador.
 */
public class TerminalTab {

    private static final Logger logger = LoggerFactory.getLogger(TerminalTab.class);

    private final VBox container;
    private final OBDDataService dataService;
    private TextArea terminalOutput;
    private TextField commandInput;
    private final List<String> commandHistory;
    private int historyIndex;

    public TerminalTab(OBDDataService dataService) {
        this.dataService = dataService;
        this.container = new VBox(10);
        this.commandHistory = new ArrayList<>();
        this.historyIndex = -1;
        buildUI();
    }

    private void buildUI() {
        container.setPadding(new Insets(20));

        Label title = new Label("Terminal OBD - Comandos Manuais");
        title.getStyleClass().add("section-title");

        // Terminal output
        terminalOutput = new TextArea();
        terminalOutput.setEditable(false);
        terminalOutput.setWrapText(true);
        terminalOutput.getStyleClass().add("terminal-output");
        VBox.setVgrow(terminalOutput, Priority.ALWAYS);

        appendToTerminal("=== Terminal OBD2 ===");
        appendToTerminal("Digite comandos AT ou OBD2 abaixo.");
        appendToTerminal("Exemplos:");
        appendToTerminal("  ATZ      - Reset do adaptador ELM327");
        appendToTerminal("  ATRV     - Voltagem da bateria");
        appendToTerminal("  ATI      - Identificacao do adaptador");
        appendToTerminal("  ATDPN    - Protocolo atual");
        appendToTerminal("  010C     - RPM do motor");
        appendToTerminal("  010D     - Velocidade do veiculo");
        appendToTerminal("  0105     - Temperatura do liquido de arrefecimento");
        appendToTerminal("  03       - Ler codigos de falha (DTC)");
        appendToTerminal("  04       - Limpar codigos de falha");
        appendToTerminal("  ATMA     - Monitor CAN (todos os dados)");
        appendToTerminal("  ATSH7DF  - Definir header CAN para broadcast");
        appendToTerminal("=========================================\n");

        // Input bar
        HBox inputBar = new HBox(10);
        inputBar.setAlignment(Pos.CENTER_LEFT);

        Label promptLabel = new Label("OBD>");
        promptLabel.getStyleClass().add("terminal-prompt");

        commandInput = new TextField();
        commandInput.setPromptText("Digite um comando...");
        commandInput.getStyleClass().add("terminal-input");
        HBox.setHgrow(commandInput, Priority.ALWAYS);

        commandInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendCommand();
            } else if (event.getCode() == KeyCode.UP) {
                navigateHistory(-1);
            } else if (event.getCode() == KeyCode.DOWN) {
                navigateHistory(1);
            }
        });

        Button sendButton = new Button("Enviar");
        sendButton.getStyleClass().add("action-button");
        sendButton.setOnAction(e -> sendCommand());

        Button clearButton = new Button("Limpar");
        clearButton.setOnAction(e -> {
            terminalOutput.clear();
            appendToTerminal("Terminal limpo.\n");
        });

        inputBar.getChildren().addAll(promptLabel, commandInput, sendButton, clearButton);

        // Quick commands
        HBox quickCommands = new HBox(5);
        quickCommands.setAlignment(Pos.CENTER_LEFT);
        Label quickLabel = new Label("Rapidos:");
        quickLabel.getStyleClass().add("field-label");

        String[][] quickCmds = {
                {"Reset", "ATZ"}, {"Voltagem", "ATRV"}, {"RPM", "010C"},
                {"Velocidade", "010D"}, {"Temperatura", "0105"}, {"Protocolo", "ATDPN"},
                {"DTCs", "03"}, {"VIN", "0902"}
        };

        for (String[] cmd : quickCmds) {
            Button btn = new Button(cmd[0]);
            btn.getStyleClass().add("quick-button");
            btn.setTooltip(new Tooltip(cmd[1]));
            btn.setOnAction(e -> {
                commandInput.setText(cmd[1]);
                sendCommand();
            });
            quickCommands.getChildren().add(btn);
        }

        quickCommands.getChildren().add(0, quickLabel);

        container.getChildren().addAll(title, terminalOutput, quickCommands, inputBar);
    }

    private void sendCommand() {
        String command = commandInput.getText().trim().toUpperCase();
        if (command.isEmpty()) {
            return;
        }

        commandHistory.add(command);
        historyIndex = commandHistory.size();
        commandInput.clear();

        appendToTerminal("> " + command);

        if (!dataService.isConnected()) {
            appendToTerminal("ERRO: Nao conectado ao adaptador. Conecte-se primeiro.\n");
            return;
        }

        Thread sendThread = new Thread(() -> {
            String response = dataService.sendCustomCommand(command);
            Platform.runLater(() -> {
                appendToTerminal("< " + response + "\n");
            });
        }, "Terminal-Send-Thread");
        sendThread.setDaemon(true);
        sendThread.start();
    }

    private void navigateHistory(int direction) {
        if (commandHistory.isEmpty()) {
            return;
        }
        historyIndex += direction;
        if (historyIndex < 0) {
            historyIndex = 0;
        }
        if (historyIndex >= commandHistory.size()) {
            historyIndex = commandHistory.size();
            commandInput.clear();
            return;
        }
        commandInput.setText(commandHistory.get(historyIndex));
        commandInput.positionCaret(commandInput.getText().length());
    }

    private void appendToTerminal(String text) {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        terminalOutput.appendText("[" + timestamp + "] " + text + "\n");
    }

    public Node getNode() {
        return container;
    }
}
