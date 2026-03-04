package pl.pekao.zesportal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "jtuxedo_service_field")
public class JtuxedoServiceField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jtuxedo_service_id", nullable = false)
    private JtuxedoService jtuxedoService;

    @NotBlank(message = "Podaj nazwę pola.")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 30)
    private FieldDataType dataType = FieldDataType.STRING;

    @Column(name = "max_length")
    private Integer maxLength;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "cardinality", nullable = false, length = 20)
    private FieldCardinality cardinality = FieldCardinality.ONE_TO_ONE;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    /** Stała wartość domyślna – podpowiedź przy wywołaniu usługi. */
    @Column(name = "default_value", length = 500)
    private String defaultValue;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public JtuxedoService getJtuxedoService() {
        return jtuxedoService;
    }

    public void setJtuxedoService(JtuxedoService jtuxedoService) {
        this.jtuxedoService = jtuxedoService;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FieldDataType getDataType() {
        return dataType;
    }

    public void setDataType(FieldDataType dataType) {
        this.dataType = dataType;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public FieldCardinality getCardinality() {
        return cardinality;
    }

    public void setCardinality(FieldCardinality cardinality) {
        this.cardinality = cardinality;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public enum FieldDataType {
        STRING("String"),
        INTEGER("Integer"),
        LONG("Long"),
        DECIMAL("Decimal"),
        DATE("Date"),
        BOOLEAN("Boolean");

        private final String displayName;

        FieldDataType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum FieldCardinality {
        ZERO_TO_ONE("0..1"),
        ONE_TO_ONE("1..1"),
        ZERO_TO_MANY("0..n"),
        ONE_TO_MANY("1..n");

        private final String displayName;

        FieldCardinality(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
