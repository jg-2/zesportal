package pl.pekao.zesportal.service.dto;

import java.util.List;

/**
 * Definicja usługi odczytana z pliku XML (np. tuxedo_maestro.xml).
 */
public class ParsedServiceDefinition {

    private final String name;
    private final String description;
    private final List<ParsedFieldDefinition> inputFields;

    public ParsedServiceDefinition(String name, String description, List<ParsedFieldDefinition> inputFields) {
        this.name = name != null ? name.trim() : "";
        this.description = description != null ? description.trim() : "";
        this.inputFields = inputFields != null ? inputFields : List.of();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<ParsedFieldDefinition> getInputFields() {
        return inputFields;
    }

    /**
     * Pole wejściowe usługi odczytane z sekcji input w XML.
     */
    public static class ParsedFieldDefinition {
        private final String name;
        private final String dataType;  // string, integer, long, decimal, date, boolean
        private final String cardinality; // 0..1, 1..1, 0..n, 1..n lub "0 lub 1", "1", "N"
        private final String defaultValue;

        public ParsedFieldDefinition(String name, String dataType, String cardinality, String defaultValue) {
            this.name = name != null ? name.trim() : "";
            this.dataType = dataType != null ? dataType.trim().toLowerCase() : "string";
            this.cardinality = cardinality != null ? cardinality.trim() : "";
            this.defaultValue = defaultValue != null ? defaultValue.trim() : null;
        }

        public String getName() { return name; }
        public String getDataType() { return dataType; }
        public String getCardinality() { return cardinality; }
        public String getDefaultValue() { return defaultValue; }
    }
}
