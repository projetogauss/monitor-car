package com.obd2.model;

/**
 * Representa um codigo de diagnostico de falha (DTC - Diagnostic Trouble Code).
 */
public class DTCCode {

    private final String code;
    private final String description;
    private final DTCCategory category;

    public enum DTCCategory {
        POWERTRAIN("P", "Powertrain (Motor/Transmissao)"),
        CHASSIS("C", "Chassis"),
        BODY("B", "Body (Carroceria)"),
        NETWORK("U", "Network (Rede de Comunicacao)");

        private final String prefix;
        private final String description;

        DTCCategory(String prefix, String description) {
            this.prefix = prefix;
            this.description = description;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getDescription() {
            return description;
        }

        public static DTCCategory fromCode(String dtcCode) {
            if (dtcCode == null || dtcCode.isEmpty()) {
                return POWERTRAIN;
            }
            char first = dtcCode.charAt(0);
            return switch (first) {
                case 'C', 'c' -> CHASSIS;
                case 'B', 'b' -> BODY;
                case 'U', 'u' -> NETWORK;
                default -> POWERTRAIN;
            };
        }
    }

    public DTCCode(String code, String description) {
        this.code = code;
        this.description = description;
        this.category = DTCCategory.fromCode(code);
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public DTCCategory getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return code + " - " + description + " [" + category.getDescription() + "]";
    }
}
