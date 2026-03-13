package com.obd2;

import com.obd2.gui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ponto de entrada principal da aplicacao OBD2.
 * Conecta seu PC ao carro via adaptador ELM327 (USB/Bluetooth)
 * com suporte a rede CAN Bus para diagnostico avancado.
 */
public class OBD2Application extends Application {

    private static final Logger logger = LoggerFactory.getLogger(OBD2Application.class);
    private MainView mainView;

    @Override
    public void start(Stage primaryStage) {
        logger.info("Iniciando aplicacao OBD2...");

        mainView = new MainView();
        Scene scene = new Scene(mainView.getRoot(), 1100, 750);
        scene.getStylesheets().add(
                getClass().getResource("/styles/dark-theme.css").toExternalForm()
        );

        primaryStage.setTitle("OBD2 Car Connect - Diagnostico Veicular");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        primaryStage.setOnCloseRequest(event -> {
            logger.info("Encerrando aplicacao...");
            mainView.shutdown();
        });

        primaryStage.show();
        logger.info("Interface grafica JavaFX iniciada com sucesso");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
