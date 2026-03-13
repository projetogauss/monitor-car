package com.obd2.service;

import com.obd2.command.OBDCommand;
import com.obd2.command.OBDCommandExecutor;
import com.obd2.connection.ELM327Initializer;
import com.obd2.connection.SerialConnection;
import com.obd2.model.DTCCode;
import com.obd2.model.OBDData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servico principal para leitura de dados OBD2 do veiculo.
 * Gerencia a conexao, inicializacao e leitura ciclica de dados.
 */
public class OBDDataService {

    private static final Logger logger = LoggerFactory.getLogger(OBDDataService.class);

    private final SerialConnection connection;
    private final ELM327Initializer initializer;
    private OBDCommandExecutor executor;
    private boolean initialized;

    private final List<OBDCommand> monitoredCommands;
    private final Map<OBDCommand, OBDData> latestData;

    public OBDDataService() {
        this.connection = new SerialConnection();
        this.initializer = new ELM327Initializer(connection);
        this.initialized = false;
        this.monitoredCommands = new CopyOnWriteArrayList<>();
        this.latestData = new LinkedHashMap<>();

        // Comandos padrao para monitoramento
        monitoredCommands.add(OBDCommand.ENGINE_RPM);
        monitoredCommands.add(OBDCommand.VEHICLE_SPEED);
        monitoredCommands.add(OBDCommand.COOLANT_TEMP);
        monitoredCommands.add(OBDCommand.ENGINE_LOAD);
        monitoredCommands.add(OBDCommand.THROTTLE_POSITION);
        monitoredCommands.add(OBDCommand.INTAKE_AIR_TEMP);
        monitoredCommands.add(OBDCommand.MAF_FLOW_RATE);
        monitoredCommands.add(OBDCommand.FUEL_TANK_LEVEL);
    }

    /**
     * Conecta ao adaptador ELM327 e inicializa a comunicacao.
     *
     * @param portName nome da porta serial
     * @param baudRate taxa de transmissao
     * @return true se conectou e inicializou com sucesso
     */
    public boolean connect(String portName, int baudRate) {
        if (!connection.connect(portName, baudRate)) {
            return false;
        }

        executor = new OBDCommandExecutor(connection);

        if (!initializer.initialize()) {
            logger.warn("Inicializacao parcial - veiculo pode nao estar com ignicao ligada");
        }

        initialized = true;
        return true;
    }

    /**
     * Desconecta do adaptador.
     */
    public void disconnect() {
        connection.disconnect();
        initialized = false;
        latestData.clear();
    }

    /**
     * Le todos os comandos monitorados uma vez.
     *
     * @return mapa de dados lidos
     */
    public Map<OBDCommand, OBDData> readAllMonitored() {
        if (!initialized || executor == null) {
            return latestData;
        }

        for (OBDCommand cmd : monitoredCommands) {
            OBDData data = executor.execute(cmd);
            if (data != null) {
                latestData.put(cmd, data);
            }
        }

        return new LinkedHashMap<>(latestData);
    }

    /**
     * Le um comando especifico.
     */
    public OBDData readCommand(OBDCommand command) {
        if (!initialized || executor == null) {
            return null;
        }
        return executor.execute(command);
    }

    /**
     * Le a voltagem da bateria via ELM327.
     */
    public String readBatteryVoltage() {
        if (!initialized || executor == null) {
            return "N/A";
        }
        OBDData data = executor.execute(OBDCommand.ELM_VOLTAGE);
        return data != null ? data.getValue() : "N/A";
    }

    /**
     * Le os codigos de falha (DTCs) do veiculo.
     */
    public List<DTCCode> readDTCs() {
        List<DTCCode> dtcList = new ArrayList<>();
        if (!initialized || executor == null) {
            return dtcList;
        }

        try {
            String response = executor.executeRaw("03");
            if (response == null || response.contains("NO DATA")) {
                return dtcList;
            }

            // Parsear DTCs da resposta
            String cleaned = response.replaceAll("[\\s\\r\\n]", "").toUpperCase();
            // Remover header "43" (mode 03 response)
            if (cleaned.startsWith("43")) {
                cleaned = cleaned.substring(2);
            }

            // Cada DTC sao 4 hex chars (2 bytes)
            for (int i = 0; i + 3 < cleaned.length(); i += 4) {
                String dtcHex = cleaned.substring(i, i + 4);
                if (dtcHex.equals("0000")) {
                    continue;
                }
                String dtcCode = decodeDTC(dtcHex);
                if (dtcCode != null) {
                    dtcList.add(new DTCCode(dtcCode, getGenericDTCDescription(dtcCode)));
                }
            }

        } catch (IOException e) {
            logger.error("Erro ao ler DTCs", e);
        }

        return dtcList;
    }

    /**
     * Limpa os codigos de falha do veiculo.
     */
    public boolean clearDTCs() {
        if (!initialized || executor == null) {
            return false;
        }
        OBDData result = executor.execute(OBDCommand.CLEAR_DTC);
        return result != null;
    }

    /**
     * Decodifica um DTC a partir de 2 bytes hex.
     */
    private String decodeDTC(String hexBytes) {
        try {
            int value = Integer.parseInt(hexBytes, 16);
            if (value == 0) {
                return null;
            }

            // Primeiro 2 bits determinam a categoria
            int firstNibble = (value >> 12) & 0x0F;
            char category;
            switch (firstNibble >> 2) {
                case 0: category = 'P'; break;
                case 1: category = 'C'; break;
                case 2: category = 'B'; break;
                case 3: category = 'U'; break;
                default: category = 'P';
            }

            int second = (firstNibble & 0x03);
            int rest = value & 0x0FFF;

            return String.format("%c%d%03X", category, second, rest);

        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getGenericDTCDescription(String code) {
        // Descricoes genericas baseadas no prefixo
        if (code.startsWith("P0")) return "Codigo generico de powertrain";
        if (code.startsWith("P1")) return "Codigo especifico do fabricante (powertrain)";
        if (code.startsWith("P2")) return "Codigo generico de powertrain (reservado)";
        if (code.startsWith("P3")) return "Codigo generico de powertrain (reservado)";
        if (code.startsWith("C")) return "Codigo de chassis";
        if (code.startsWith("B")) return "Codigo de carroceria";
        if (code.startsWith("U")) return "Codigo de rede de comunicacao";
        return "Codigo desconhecido";
    }

    /**
     * Envia um comando OBD2 customizado via CAN.
     */
    public String sendCustomCommand(String command) {
        if (!initialized || executor == null) {
            return "Nao conectado";
        }
        try {
            return executor.executeRaw(command);
        } catch (IOException e) {
            return "Erro: " + e.getMessage();
        }
    }

    public void addMonitoredCommand(OBDCommand command) {
        if (!monitoredCommands.contains(command)) {
            monitoredCommands.add(command);
        }
    }

    public void removeMonitoredCommand(OBDCommand command) {
        monitoredCommands.remove(command);
    }

    public List<OBDCommand> getMonitoredCommands() {
        return new ArrayList<>(monitoredCommands);
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public SerialConnection getConnection() {
        return connection;
    }

    public ELM327Initializer getInitializer() {
        return initializer;
    }

    public String getDetectedProtocol() {
        if (initializer.getDetectedProtocol() != null) {
            return initializer.getDetectedProtocol().getDescription();
        }
        return "Nao detectado";
    }

    public String getElmVersion() {
        return initializer.getElmVersion() != null ? initializer.getElmVersion() : "Desconhecido";
    }
}
