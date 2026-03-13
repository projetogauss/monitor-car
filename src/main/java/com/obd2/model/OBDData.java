package com.obd2.model;

import java.time.LocalDateTime;

/**
 * Modelo de dados para uma leitura OBD2.
 */
public class OBDData {

    private final String name;
    private final String value;
    private final String unit;
    private final String rawResponse;
    private final LocalDateTime timestamp;

    public OBDData(String name, String value, String unit, String rawResponse) {
        this.name = name;
        this.value = value;
        this.unit = unit;
        this.rawResponse = rawResponse;
        this.timestamp = LocalDateTime.now();
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedValue() {
        return value + " " + unit;
    }

    @Override
    public String toString() {
        return name + ": " + value + " " + unit;
    }
}
