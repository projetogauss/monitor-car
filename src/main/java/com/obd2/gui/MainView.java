package com.obd2.gui;

import com.obd2.service.CANMonitorService;
import com.obd2.service.OBDDataService;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * View principal da aplicacao JavaFX.
 * Organiza as abas: Dashboard, DTCs, CAN Monitor, Terminal.
 */
public class MainView {

    private static final Logger logger = LoggerFactory.getLogger(MainView.class);

    private final BorderPane root;
    private final OBDDataService dataService;
    private DashboardTab dashboardTab;
    private DTCTab dtcTab;
    private CANMonitorTab canMonitorTab;
    private TerminalTab terminalTab;

    public MainView() {
        this.dataService = new OBDDataService();
        this.root = new BorderPane();
        buildUI();
    }

    private void buildUI() {
        // Header com conexao
        ConnectionBar connectionBar = new ConnectionBar(dataService, this::onConnectionChanged);
        root.setTop(connectionBar.getNode());

        // Abas principais
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        dashboardTab = new DashboardTab(dataService);
        Tab dashTab = new Tab("Dashboard", dashboardTab.getNode());
        dashTab.setStyle("-fx-font-size: 14px;");

        dtcTab = new DTCTab(dataService);
        Tab dtcTabView = new Tab("Codigos de Falha (DTC)", dtcTab.getNode());
        dtcTabView.setStyle("-fx-font-size: 14px;");

        CANMonitorService canService = new CANMonitorService(dataService.getConnection());
        canMonitorTab = new CANMonitorTab(canService, dataService);
        Tab canTab = new Tab("Monitor CAN Bus", canMonitorTab.getNode());
        canTab.setStyle("-fx-font-size: 14px;");

        terminalTab = new TerminalTab(dataService);
        Tab termTab = new Tab("Terminal OBD", terminalTab.getNode());
        termTab.setStyle("-fx-font-size: 14px;");

        tabPane.getTabs().addAll(dashTab, dtcTabView, canTab, termTab);
        root.setCenter(tabPane);

        // Status bar
        StatusBar statusBar = new StatusBar();
        root.setBottom(statusBar.getNode());
    }

    private void onConnectionChanged(boolean connected) {
        if (connected) {
            logger.info("Conectado - iniciando leitura de dados");
            dashboardTab.startReading();
        } else {
            logger.info("Desconectado - parando leitura de dados");
            dashboardTab.stopReading();
            canMonitorTab.stopMonitoring();
        }
    }

    public void shutdown() {
        dashboardTab.stopReading();
        canMonitorTab.stopMonitoring();
        dataService.disconnect();
    }

    public Parent getRoot() {
        return root;
    }
}
