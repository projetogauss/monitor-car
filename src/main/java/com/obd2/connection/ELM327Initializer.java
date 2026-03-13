package com.obd2.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Inicializa e configura o adaptador ELM327.
 * Envia os comandos AT necessarios para preparar a comunicacao OBD2.
 */
public class ELM327Initializer {

    private static final Logger logger = LoggerFactory.getLogger(ELM327Initializer.class);

    private final SerialConnection connection;
    private String elmVersion;
    private OBDProtocol detectedProtocol;

    public ELM327Initializer(SerialConnection connection) {
        this.connection = connection;
    }

    /**
     * Protocolos OBD2 suportados pelo ELM327.
     */
    public enum OBDProtocol {
        AUTO("0", "Automatico"),
        SAE_J1850_PWM("1", "SAE J1850 PWM (41.6 kbaud)"),
        SAE_J1850_VPW("2", "SAE J1850 VPW (10.4 kbaud)"),
        ISO_9141_2("3", "ISO 9141-2 (5 baud init)"),
        ISO_14230_4_KWP_SLOW("4", "ISO 14230-4 KWP (5 baud init)"),
        ISO_14230_4_KWP_FAST("5", "ISO 14230-4 KWP (fast init)"),
        ISO_15765_4_CAN_11BIT_500K("6", "ISO 15765-4 CAN (11 bit ID, 500 kbaud)"),
        ISO_15765_4_CAN_29BIT_500K("7", "ISO 15765-4 CAN (29 bit ID, 500 kbaud)"),
        ISO_15765_4_CAN_11BIT_250K("8", "ISO 15765-4 CAN (11 bit ID, 250 kbaud)"),
        ISO_15765_4_CAN_29BIT_250K("9", "ISO 15765-4 CAN (29 bit ID, 250 kbaud)"),
        SAE_J1939_CAN("A", "SAE J1939 CAN (29 bit ID, 250 kbaud)"),
        USER_CAN_1("B", "User1 CAN (11 bit ID, 125 kbaud)"),
        USER_CAN_2("C", "User2 CAN (11 bit ID, 50 kbaud)");

        private final String code;
        private final String description;

        OBDProtocol(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public boolean isCAN() {
            return this == ISO_15765_4_CAN_11BIT_500K
                    || this == ISO_15765_4_CAN_29BIT_500K
                    || this == ISO_15765_4_CAN_11BIT_250K
                    || this == ISO_15765_4_CAN_29BIT_250K
                    || this == SAE_J1939_CAN
                    || this == USER_CAN_1
                    || this == USER_CAN_2;
        }
    }

    /**
     * Executa a sequencia completa de inicializacao do ELM327.
     *
     * @return true se inicializou com sucesso
     */
    public boolean initialize() {
        try {
            logger.info("Inicializando adaptador ELM327...");

            // Reset do adaptador
            String resetResponse = connection.sendCommand("ATZ");
            logger.info("Reset ELM327: {}", resetResponse);

            if (resetResponse.contains("ELM327") || resetResponse.contains("ELM")) {
                elmVersion = resetResponse;
            }

            Thread.sleep(500);

            // Desligar echo
            sendATCommand("ATE0", "Desligar echo");

            // Desligar line feed
            sendATCommand("ATL0", "Desligar line feed");

            // Desligar espacos na resposta
            sendATCommand("ATS0", "Desligar espacos");

            // Ligar headers (necessario para CAN)
            sendATCommand("ATH1", "Ligar headers");

            // Configurar protocolo automatico
            sendATCommand("ATSP0", "Protocolo automatico");

            // Verificar voltagem da bateria
            String voltage = connection.sendCommand("ATRV");
            logger.info("Voltagem da bateria: {}", voltage);

            // Tentar comunicacao inicial com o veiculo
            String initResponse = connection.sendCommand("0100");
            logger.info("Resposta inicial OBD2: {}", initResponse);

            if (initResponse.contains("NO DATA") || initResponse.contains("UNABLE TO CONNECT")) {
                logger.warn("Nao foi possivel conectar ao veiculo. Verifique a ignicao.");
                return false;
            }

            // Detectar protocolo em uso
            String protocolResponse = connection.sendCommand("ATDPN");
            detectedProtocol = parseProtocol(protocolResponse);
            logger.info("Protocolo detectado: {}", detectedProtocol.getDescription());

            logger.info("ELM327 inicializado com sucesso!");
            return true;

        } catch (Exception e) {
            logger.error("Erro ao inicializar ELM327", e);
            return false;
        }
    }

    /**
     * Inicializa o adaptador para modo de monitoramento CAN.
     */
    public boolean initializeCANMonitor() {
        try {
            logger.info("Configurando modo de monitoramento CAN...");

            // Reset
            connection.sendCommand("ATZ");
            Thread.sleep(500);

            // Configuracoes basicas
            sendATCommand("ATE0", "Desligar echo");
            sendATCommand("ATL0", "Desligar line feed");
            sendATCommand("ATH1", "Ligar headers CAN");
            sendATCommand("ATS1", "Ligar espacos (para leitura)");

            // Configurar para CAN 500kbps 11-bit (mais comum)
            sendATCommand("ATSP6", "CAN 11-bit 500kbps");

            // Configurar filtro CAN (aceitar tudo)
            sendATCommand("ATCAF1", "CAN auto formatting ON");

            logger.info("Modo de monitoramento CAN configurado");
            return true;

        } catch (Exception e) {
            logger.error("Erro ao configurar modo CAN", e);
            return false;
        }
    }

    /**
     * Configura filtro de mensagens CAN por ID.
     *
     * @param canId ID CAN para filtrar (ex: "7E8")
     */
    public void setCANFilter(String canId) throws IOException {
        connection.sendCommand("ATCF" + canId);
        logger.info("Filtro CAN configurado para ID: {}", canId);
    }

    /**
     * Configura mascara de filtro CAN.
     *
     * @param mask mascara CAN (ex: "7FF" para match exato)
     */
    public void setCANMask(String mask) throws IOException {
        connection.sendCommand("ATCM" + mask);
        logger.info("Mascara CAN configurada: {}", mask);
    }

    /**
     * Inicia monitoramento CAN (modo passivo).
     */
    public void startCANMonitor() throws IOException {
        connection.sendCommand("ATMA");
    }

    /**
     * Para o monitoramento CAN.
     */
    public void stopCANMonitor() throws IOException {
        connection.sendRawBytes(new byte[]{'\r'});
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Enviar qualquer caractere para sair do modo monitor
        connection.sendCommand("ATZ");
    }

    private void sendATCommand(String command, String description) throws IOException {
        String response = connection.sendCommand(command);
        if (response.contains("OK") || response.contains("ELM")) {
            logger.debug("{}: OK", description);
        } else {
            logger.warn("{}: Resposta inesperada: {}", description, response);
        }
    }

    private OBDProtocol parseProtocol(String response) {
        String cleaned = response.replaceAll("[^0-9A-Ca-c]", "");
        if (cleaned.isEmpty()) {
            return OBDProtocol.AUTO;
        }

        // Remove 'A' prefix que indica protocolo automatico
        if (cleaned.startsWith("A") && cleaned.length() > 1) {
            cleaned = cleaned.substring(1);
        }

        for (OBDProtocol protocol : OBDProtocol.values()) {
            if (protocol.getCode().equalsIgnoreCase(cleaned)) {
                return protocol;
            }
        }
        return OBDProtocol.AUTO;
    }

    public String getElmVersion() {
        return elmVersion;
    }

    public OBDProtocol getDetectedProtocol() {
        return detectedProtocol;
    }
}
