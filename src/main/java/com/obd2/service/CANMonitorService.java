package com.obd2.service;

import com.obd2.connection.ELM327Initializer;
import com.obd2.connection.SerialConnection;
import com.obd2.model.CANMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Servico de monitoramento da rede CAN Bus do veiculo.
 * Permite sniffing de mensagens CAN em tempo real,
 * filtragem por ID, e envio de mensagens CAN customizadas.
 */
public class CANMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(CANMonitorService.class);

    private final SerialConnection connection;
    private final ELM327Initializer initializer;
    private final List<CANMessage> capturedMessages;
    private final AtomicBoolean monitoring;
    private Thread monitorThread;
    private Consumer<CANMessage> messageCallback;
    private String filterCanId;
    private int maxMessages;

    public CANMonitorService(SerialConnection connection) {
        this.connection = connection;
        this.initializer = new ELM327Initializer(connection);
        this.capturedMessages = new CopyOnWriteArrayList<>();
        this.monitoring = new AtomicBoolean(false);
        this.maxMessages = 10000;
    }

    /**
     * Inicia o monitoramento CAN em uma thread separada.
     *
     * @param callback funcao chamada para cada mensagem recebida
     */
    public void startMonitoring(Consumer<CANMessage> callback) {
        if (monitoring.get()) {
            logger.warn("Monitoramento CAN ja esta ativo");
            return;
        }

        this.messageCallback = callback;

        if (!initializer.initializeCANMonitor()) {
            logger.error("Falha ao inicializar modo de monitoramento CAN");
            return;
        }

        // Configurar filtro se definido
        if (filterCanId != null && !filterCanId.isEmpty()) {
            try {
                initializer.setCANFilter(filterCanId);
                initializer.setCANMask("7FF");
            } catch (IOException e) {
                logger.error("Erro ao configurar filtro CAN", e);
            }
        }

        monitoring.set(true);

        monitorThread = new Thread(this::monitorLoop, "CAN-Monitor-Thread");
        monitorThread.setDaemon(true);
        monitorThread.start();

        logger.info("Monitoramento CAN iniciado");
    }

    /**
     * Para o monitoramento CAN.
     */
    public void stopMonitoring() {
        monitoring.set(false);
        if (monitorThread != null) {
            monitorThread.interrupt();
            try {
                monitorThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        try {
            initializer.stopCANMonitor();
        } catch (IOException e) {
            logger.error("Erro ao parar monitoramento CAN", e);
        }

        logger.info("Monitoramento CAN parado. Mensagens capturadas: {}", capturedMessages.size());
    }

    /**
     * Envia uma mensagem CAN customizada.
     *
     * @param canId  ID CAN (ex: "7DF")
     * @param data   dados hex (ex: "0201050000000000")
     * @return resposta ou null em caso de erro
     */
    public String sendCANMessage(String canId, String data) {
        try {
            // Usar o comando ATSH para definir o header CAN
            connection.sendCommand("ATSH" + canId);

            // Enviar os dados
            String response = connection.sendCommand(data);
            logger.info("Mensagem CAN enviada - ID: {} Data: {} -> Resposta: {}", canId, data, response);
            return response;

        } catch (IOException e) {
            logger.error("Erro ao enviar mensagem CAN", e);
            return null;
        }
    }

    /**
     * Configura protocolo CAN especifico.
     *
     * @param protocol protocolo OBD a usar
     */
    public void setProtocol(ELM327Initializer.OBDProtocol protocol) {
        try {
            connection.sendCommand("ATSP" + protocol.getCode());
            logger.info("Protocolo CAN configurado: {}", protocol.getDescription());
        } catch (IOException e) {
            logger.error("Erro ao configurar protocolo CAN", e);
        }
    }

    private void monitorLoop() {
        try {
            // Iniciar modo de monitoramento
            initializer.startCANMonitor();

            StringBuilder buffer = new StringBuilder();

            while (monitoring.get() && !Thread.currentThread().isInterrupted()) {
                byte[] rawData = connection.readRawBytes(256);

                if (rawData.length > 0) {
                    String rawString = new String(rawData);
                    buffer.append(rawString);

                    // Processar linhas completas
                    String content = buffer.toString();
                    int lastNewline = content.lastIndexOf('\r');
                    if (lastNewline >= 0) {
                        String complete = content.substring(0, lastNewline);
                        buffer = new StringBuilder(content.substring(lastNewline + 1));

                        String[] lines = complete.split("\\r");
                        for (String line : lines) {
                            line = line.trim();
                            if (!line.isEmpty() && !line.equals(">")) {
                                CANMessage message = parseCANMessage(line);
                                if (message != null) {
                                    addMessage(message);
                                }
                            }
                        }
                    }
                } else {
                    Thread.sleep(50);
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            if (monitoring.get()) {
                logger.error("Erro no monitoramento CAN", e);
            }
        }
    }

    /**
     * Parseia uma linha de resposta CAN do ELM327.
     * Formato tipico com header: "7E8 06 41 0C 1A F8 00 00"
     */
    private CANMessage parseCANMessage(String line) {
        try {
            String cleaned = line.replaceAll("\\s+", " ").trim();
            String[] parts = cleaned.split(" ");

            if (parts.length < 2) {
                return null;
            }

            // Primeiro campo eh o CAN ID
            String canId = parts[0];
            boolean extendedId = canId.length() > 3;

            // Resto sao bytes de dados
            byte[] data = new byte[parts.length - 1];
            for (int i = 1; i < parts.length; i++) {
                try {
                    data[i - 1] = (byte) Integer.parseInt(parts[i], 16);
                } catch (NumberFormatException e) {
                    return null;
                }
            }

            return new CANMessage(canId, data, extendedId);

        } catch (Exception e) {
            logger.debug("Falha ao parsear mensagem CAN: {}", line);
            return null;
        }
    }

    private void addMessage(CANMessage message) {
        if (capturedMessages.size() >= maxMessages) {
            capturedMessages.remove(0);
        }
        capturedMessages.add(message);

        if (messageCallback != null) {
            messageCallback.accept(message);
        }
    }

    public void setFilterCanId(String filterCanId) {
        this.filterCanId = filterCanId;
    }

    public void clearFilter() {
        this.filterCanId = null;
    }

    public List<CANMessage> getCapturedMessages() {
        return new ArrayList<>(capturedMessages);
    }

    public void clearCapturedMessages() {
        capturedMessages.clear();
    }

    public boolean isMonitoring() {
        return monitoring.get();
    }

    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }
}
