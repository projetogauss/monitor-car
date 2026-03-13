package com.obd2.connection;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerencia a conexao serial com o adaptador ELM327.
 * Suporta conexao via USB ou Bluetooth (porta COM/ttyUSB).
 */
public class SerialConnection {

    private static final Logger logger = LoggerFactory.getLogger(SerialConnection.class);

    private static final int DEFAULT_BAUD_RATE = 38400;
    private static final int DEFAULT_DATA_BITS = 8;
    private static final int DEFAULT_STOP_BITS = SerialPort.ONE_STOP_BIT;
    private static final int DEFAULT_PARITY = SerialPort.NO_PARITY;
    private static final int READ_TIMEOUT_MS = 2000;
    private static final int WRITE_TIMEOUT_MS = 1000;

    private SerialPort serialPort;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean connected;
    private int baudRate;

    public SerialConnection() {
        this.baudRate = DEFAULT_BAUD_RATE;
        this.connected = false;
    }

    /**
     * Lista todas as portas seriais disponiveis no sistema.
     */
    public static List<String> listAvailablePorts() {
        List<String> ports = new ArrayList<>();
        SerialPort[] serialPorts = SerialPort.getCommPorts();
        for (SerialPort port : serialPorts) {
            ports.add(port.getSystemPortName() + " - " + port.getDescriptivePortName());
        }
        return ports;
    }

    /**
     * Retorna os objetos SerialPort disponiveis.
     */
    public static SerialPort[] getAvailablePorts() {
        return SerialPort.getCommPorts();
    }

    /**
     * Conecta a uma porta serial especifica.
     *
     * @param portName nome da porta (ex: COM3, /dev/ttyUSB0)
     * @return true se a conexao foi bem sucedida
     */
    public boolean connect(String portName) {
        return connect(portName, this.baudRate);
    }

    /**
     * Conecta a uma porta serial com baud rate especifico.
     *
     * @param portName nome da porta
     * @param baudRate taxa de transmissao
     * @return true se conectou com sucesso
     */
    public boolean connect(String portName, int baudRate) {
        try {
            logger.info("Conectando a porta serial: {} com baud rate: {}", portName, baudRate);

            serialPort = SerialPort.getCommPort(portName);
            serialPort.setBaudRate(baudRate);
            serialPort.setNumDataBits(DEFAULT_DATA_BITS);
            serialPort.setNumStopBits(DEFAULT_STOP_BITS);
            serialPort.setParity(DEFAULT_PARITY);
            serialPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                    READ_TIMEOUT_MS,
                    WRITE_TIMEOUT_MS
            );

            if (!serialPort.openPort()) {
                logger.error("Falha ao abrir porta serial: {}", portName);
                return false;
            }

            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
            connected = true;
            this.baudRate = baudRate;

            logger.info("Conectado com sucesso a porta: {}", portName);
            return true;

        } catch (Exception e) {
            logger.error("Erro ao conectar a porta serial: {}", portName, e);
            connected = false;
            return false;
        }
    }

    /**
     * Desconecta da porta serial.
     */
    public void disconnect() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
            }
            connected = false;
            logger.info("Desconectado da porta serial");
        } catch (IOException e) {
            logger.error("Erro ao desconectar", e);
        }
    }

    /**
     * Envia um comando pela porta serial e aguarda a resposta.
     *
     * @param command comando a ser enviado
     * @return resposta recebida
     */
    public String sendCommand(String command) throws IOException {
        if (!connected || outputStream == null || inputStream == null) {
            throw new IOException("Nao conectado a porta serial");
        }

        // Limpa o buffer de entrada
        while (inputStream.available() > 0) {
            inputStream.read();
        }

        String fullCommand = command + "\r";
        outputStream.write(fullCommand.getBytes(StandardCharsets.US_ASCII));
        outputStream.flush();

        logger.debug("Comando enviado: {}", command);

        return readResponse();
    }

    /**
     * Le a resposta da porta serial ate receber o prompt '>'.
     */
    private String readResponse() throws IOException {
        StringBuilder response = new StringBuilder();
        long startTime = System.currentTimeMillis();
        long timeout = READ_TIMEOUT_MS;

        while ((System.currentTimeMillis() - startTime) < timeout) {
            if (inputStream.available() > 0) {
                int b = inputStream.read();
                if (b == -1) {
                    break;
                }
                char c = (char) b;
                if (c == '>') {
                    break;
                }
                response.append(c);
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        String result = response.toString().trim();
        logger.debug("Resposta recebida: {}", result);
        return result;
    }

    /**
     * Envia dados brutos pela porta serial (para comunicacao CAN direta).
     */
    public void sendRawBytes(byte[] data) throws IOException {
        if (!connected || outputStream == null) {
            throw new IOException("Nao conectado a porta serial");
        }
        outputStream.write(data);
        outputStream.flush();
    }

    /**
     * Le bytes brutos da porta serial.
     */
    public byte[] readRawBytes(int maxBytes) throws IOException {
        if (!connected || inputStream == null) {
            throw new IOException("Nao conectado a porta serial");
        }

        byte[] buffer = new byte[maxBytes];
        int totalRead = 0;
        long startTime = System.currentTimeMillis();

        while (totalRead < maxBytes && (System.currentTimeMillis() - startTime) < READ_TIMEOUT_MS) {
            if (inputStream.available() > 0) {
                int read = inputStream.read(buffer, totalRead, maxBytes - totalRead);
                if (read > 0) {
                    totalRead += read;
                }
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        byte[] result = new byte[totalRead];
        System.arraycopy(buffer, 0, result, 0, totalRead);
        return result;
    }

    public boolean isConnected() {
        return connected && serialPort != null && serialPort.isOpen();
    }

    public SerialPort getSerialPort() {
        return serialPort;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }
}
