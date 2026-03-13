package com.obd2.model;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Representa uma mensagem CAN Bus capturada da rede do veiculo.
 */
public class CANMessage {

    private final String canId;
    private final byte[] data;
    private final int dataLength;
    private final LocalDateTime timestamp;
    private final boolean extendedId;

    public CANMessage(String canId, byte[] data, boolean extendedId) {
        this.canId = canId;
        this.data = Arrays.copyOf(data, data.length);
        this.dataLength = data.length;
        this.timestamp = LocalDateTime.now();
        this.extendedId = extendedId;
    }

    public String getCanId() {
        return canId;
    }

    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    public int getDataLength() {
        return dataLength;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isExtendedId() {
        return extendedId;
    }

    /**
     * Retorna os dados em formato hexadecimal.
     */
    public String getDataHex() {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * Retorna o valor de um byte especifico dos dados.
     */
    public int getByte(int index) {
        if (index >= 0 && index < data.length) {
            return data[index] & 0xFF;
        }
        return 0;
    }

    /**
     * Retorna um valor de 16 bits (2 bytes) a partir de um indice.
     */
    public int getWord(int startIndex) {
        if (startIndex >= 0 && startIndex + 1 < data.length) {
            return ((data[startIndex] & 0xFF) << 8) | (data[startIndex + 1] & 0xFF);
        }
        return 0;
    }

    @Override
    public String toString() {
        return String.format("[%s] ID: %s DLC: %d Data: %s",
                timestamp.toString(), canId, dataLength, getDataHex());
    }
}
