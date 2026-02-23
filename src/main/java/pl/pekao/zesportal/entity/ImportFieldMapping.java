package pl.pekao.zesportal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Mapowanie w definicji importu: pole docelowe w usłudze ← wartość z pliku lub stała.
 */
@Entity
@Table(name = "import_field_mapping")
public class ImportFieldMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_definition_id", nullable = false)
    private ImportDefinition importDefinition;

    /** Nazwa pola w usłudze (docelowe). */
    @NotBlank
    @Column(name = "target_field_name", nullable = false, length = 255)
    private String targetFieldName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType = SourceType.FILE_FIELD;

    /** Dla FILE_FIELD: indeks kolumny (0, 1, 2...) lub nazwa kolumny. */
    @Column(name = "source_field", length = 100)
    private String sourceField;

    /** Dla CONSTANT: wartość stała. */
    @Column(name = "constant_value", length = 1000)
    private String constantValue;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ImportDefinition getImportDefinition() {
        return importDefinition;
    }

    public void setImportDefinition(ImportDefinition importDefinition) {
        this.importDefinition = importDefinition;
    }

    public String getTargetFieldName() {
        return targetFieldName;
    }

    public void setTargetFieldName(String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceField() {
        return sourceField;
    }

    public void setSourceField(String sourceField) {
        this.sourceField = sourceField;
    }

    public String getConstantValue() {
        return constantValue;
    }

    public void setConstantValue(String constantValue) {
        this.constantValue = constantValue;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public enum SourceType {
        FILE_FIELD("Pole z pliku"),
        CONSTANT("Wartość stała");

        private final String displayName;

        SourceType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
