package com.obd2.command;

import com.obd2.connection.SerialConnection;
import com.obd2.model.OBDData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Executa comandos OBD2 e faz o parsing das respostas.
 */
public class OBDCommandExecutor {

    private static final Logger logger = LoggerFactory.getLogger(OBDCommandExecutor.class);

    private final SerialConnection connection;

    public OBDCommandExecutor(SerialConnection connection) {
        this.connection = connection;
    }

    /**
     * Executa um comando OBD2 e retorna os dados parseados.
     *
     * @param command comando OBD2 a executar
     * @return dados parseados ou null se houve erro
     */
    public OBDData execute(OBDCommand command) {
        try {
            String rawResponse = connection.sendCommand(command.getCommand());

            if (rawResponse == null || rawResponse.isEmpty()) {
                logger.warn("Resposta vazia para comando: {}", command.getCommand());
                return null;
            }

            if (rawResponse.contains("NO DATA") || rawResponse.contains("ERROR")
                    || rawResponse.contains("UNABLE") || rawResponse.contains("?")) {
                logger.debug("Comando nao suportado ou sem dados: {} -> {}", command.getCommand(), rawResponse);
                return null;
            }

            // Para comandos AT (ELM327), retornar resposta direta
            if (command.getCommand().startsWith("AT")) {
                return new OBDData(command.getDescription(), rawResponse.trim(), command.getUnit(), rawResponse);
            }

            int[] dataBytes = parseHexResponse(rawResponse, command);
            if (dataBytes == null || dataBytes.length == 0) {
                logger.warn("Falha ao parsear resposta: {} -> {}", command.getCommand(), rawResponse);
                return null;
            }

            String parsedValue = command.parseResponse(dataBytes);
            return new OBDData(command.getDescription(), parsedValue, command.getUnit(), rawResponse);

        } catch (IOException e) {
            logger.error("Erro de comunicacao ao executar comando: {}", command.getCommand(), e);
            return null;
        } catch (Exception e) {
            logger.error("Erro ao processar comando: {}", command.getCommand(), e);
            return null;
        }
    }

    /**
     * Executa um comando OBD2 personalizado (string raw).
     *
     * @param rawCommand comando raw a enviar
     * @return resposta raw
     */
    public String executeRaw(String rawCommand) throws IOException {
        return connection.sendCommand(rawCommand);
    }

    /**
     * Parseia a resposta hexadecimal do ELM327.
     * Remove o header da resposta e extrai os bytes de dados.
     */
    private int[] parseHexResponse(String rawResponse, OBDCommand command) {
        // Remove espacos, CR, LF e caracteres de controle
        String cleaned = rawResponse.replaceAll("[\\s\\r\\n]", "").toUpperCase();

        // Remove echo do comando se presente
        String cmdUpper = command.getCommand().toUpperCase();
        if (cleaned.startsWith(cmdUpper)) {
            cleaned = cleaned.substring(cmdUpper.length());
        }

        // Para respostas OBD2 padrao, o primeiro byte eh o mode + 0x40, seguido do PID
        // Exemplo: "0100" -> resposta "4100XXXXXXXX"
        // Precisamos pular o header (mode response + PID)
        int headerLength = 0;
        if (command.getCommand().length() >= 4 && !command.getCommand().startsWith("AT")) {
            headerLength = 4; // 2 bytes hex = mode response + PID
        } else if (command.getCommand().length() == 2) {
            headerLength = 2; // 1 byte hex = mode response
        }

        if (cleaned.length() < headerLength) {
            return new int[0];
        }

        String dataHex = cleaned.substring(headerLength);

        // Converter hex string para array de int
        int numBytes = dataHex.length() / 2;
        int[] result = new int[numBytes];
        for (int i = 0; i < numBytes; i++) {
            try {
                result[i] = Integer.parseInt(dataHex.substring(i * 2, i * 2 + 2), 16);
            } catch (NumberFormatException e) {
                logger.debug("Caractere hex invalido na posicao {}: {}", i, dataHex);
                return new int[0];
            }
        }

        return result;
    }
}
