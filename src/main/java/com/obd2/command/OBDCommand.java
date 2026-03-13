package com.obd2.command;

/**
 * Representa um comando OBD2 (PID) com sua logica de parsing.
 */
public enum OBDCommand {

    // Mode 01 - Current Data
    SUPPORTED_PIDS_01_20("0100", "PIDs suportados [01-20]", "", 4) {
        @Override
        public String parseResponse(int[] data) {
            return String.format("%08X", ((long) data[0] << 24) | (data[1] << 16) | (data[2] << 8) | data[3]);
        }
    },
    ENGINE_LOAD("0104", "Carga do Motor", "%", 1) {
        @Override
        public String parseResponse(int[] data) {
            return String.format("%.1f", data[0] * 100.0 / 255.0);
        }
    },
    COOLANT_TEMP("0105", "Temperatura do Liquido de Arrefecimento", "\u00B0C", 1) {
        @Override
        public String parseResponse(int[] data) {
            return String.valueOf(data[0] - 40);
        }
    },
    SHORT_FUEL_TRIM_BANK1("0106", "Ajuste Combustivel Curto Prazo - Banco 1", "%", 1) {
        @Override
        public String parseResponse(int[] data) {
            return String.format("%.1f", (data[0] - 128) * 100.0 / 128.0);
        }
    },
    LONG_FUEL_TRIM_BANK1("0107", "Ajuste Combustivel Longo Prazo - Banco 1", "%", 1) {
        @Override
        public String parseResponse(int[] data) {
            return String.format("%.1f", (data[0] - 128) * 100.0 / 128.0);
        }
    },
    INTAKE_MANIFOLD_PRESSURE("010B", "Pressao do Coletor de Admissao", "kPa", 1) {
        @Override
        public String parseResponse(int[] data) {
            return String.valueOf(data[0]);
        }
    },
    ENGINE_RPM("010C", "Rotacao do Motor (RPM)", "rpm", 2) {
        @Override
        public String parseResponse(int[] data) {
            return String.format("%.0f", ((data[0] * 256.0) + data[1]) / 4.0);
        }
    },
    VEHICLE_SPEED("010D", "Velocidade do Veiculo", "km/h", 1) {
        @Override
        public String parseResponse(int[] data) {
            return String.valueOf(data[0]);
        }
    },
    TIMING_ADVANCE("010E", "Avanco de Ignicao", "\u00B0", 1) {
        @Override
        public String parseResponse(int[] data) {
            return String.format("%.1f", data[0] / 2.0 - 64);
        }
    },
    INTAKE_AIR_TEMP("010F", "Temperatura do Ar de Admissao", "\u00B0C", 1) {
        @Override
        public String parseResponse(int[] data) {
            return String.valueOf(data[0] - 40);
        }
    },
    MAF_FLOW_RATE("0110", "Fluxo de Ar (MAF)", "g/s", 2) {
        @Override
        public String parseResponse(int[] data) {
            return String.format("%.2f", ((data[0] * 256.0) + data[1]) / 100.0);
        }
    },
    THROTTLE_POSITION("0111", "Posicao do Acelerador", "%", 1) {
        @Override
        public String parseResponse(int[] data) {
            return String.format("%.1f", data[0] * 100.0 / 255.0);
        }
    },
    ENGINE_RUN_TIME("011F", "Tempo de Funcionamento do Motor", "s", 2) {
        @Override
        public String parseResponse(int[] data) {
            return String.valueOf((data[0] * 256) + data[1]);
        }
    },
    FUEL_TANK_LEVEL("012F", "Nivel do Tanque de Combustivel", "%", 1) {
        @Override
        public String parseResponse(int[] data) {
            return String.format("%.1f", data[0] * 100.0 / 255.0);
        }
    },
    BAROMETRIC_PRESSURE("0133", "Pressao Barometrica", "kPa", 1) {
        @Override
        public String parseResponse(int[] data) {
            return String.valueOf(data[0]);
        }
    },
    CONTROL_MODULE_VOLTAGE("0142", "Voltagem do Modulo de Controle", "V", 2) {
        @Override
        public String parseResponse(int[] data) {
            return String.format("%.3f", ((data[0] * 256.0) + data[1]) / 1000.0);
        }
    },
    AMBIENT_AIR_TEMP("0146", "Temperatura Ambiente", "\u00B0C", 1) {
        @Override
        public String parseResponse(int[] data) {
            return String.valueOf(data[0] - 40);
        }
    },
    OIL_TEMP("015C", "Temperatura do Oleo do Motor", "\u00B0C", 1) {
        @Override
        public String parseResponse(int[] data) {
            return String.valueOf(data[0] - 40);
        }
    },

    // Mode 01 - Vehicle Info
    VIN_REQUEST("0902", "Numero de Identificacao do Veiculo (VIN)", "", 17) {
        @Override
        public String parseResponse(int[] data) {
            StringBuilder vin = new StringBuilder();
            for (int b : data) {
                if (b > 0) {
                    vin.append((char) b);
                }
            }
            return vin.toString();
        }
    },

    // Mode 03 - Read DTCs
    READ_DTC("03", "Ler Codigos de Falha (DTC)", "", 0) {
        @Override
        public String parseResponse(int[] data) {
            return rawToString(data);
        }
    },

    // Mode 04 - Clear DTCs
    CLEAR_DTC("04", "Limpar Codigos de Falha (DTC)", "", 0) {
        @Override
        public String parseResponse(int[] data) {
            return "OK";
        }
    },

    // ELM327 AT Commands
    ELM_VOLTAGE("ATRV", "Voltagem da Bateria (ELM327)", "V", 0) {
        @Override
        public String parseResponse(int[] data) {
            return rawToString(data);
        }
    };

    private final String command;
    private final String description;
    private final String unit;
    private final int expectedBytes;

    OBDCommand(String command, String description, String unit, int expectedBytes) {
        this.command = command;
        this.description = description;
        this.unit = unit;
        this.expectedBytes = expectedBytes;
    }

    public abstract String parseResponse(int[] data);

    protected static String rawToString(int[] data) {
        StringBuilder sb = new StringBuilder();
        for (int b : data) {
            sb.append((char) b);
        }
        return sb.toString();
    }

    public String getCommand() {
        return command;
    }

    public String getDescription() {
        return description;
    }

    public String getUnit() {
        return unit;
    }

    public int getExpectedBytes() {
        return expectedBytes;
    }
}
