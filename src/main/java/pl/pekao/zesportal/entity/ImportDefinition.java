package pl.pekao.zesportal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Definicja importu pliku do usługi Tuxedo: wybór usługi, rodzaj pliku (json, xml, txt, csv),
 * dla TXT: separator pól, separator linii, regex nazwy pliku oraz mapowania pól.
 */
@Entity
@Table(name = "import_definition")
public class ImportDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jtuxedo_service_id", nullable = false)
    private JtuxedoService jtuxedoService;

    /** Źródło danych: plik lub widok z bazy danych. */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "import_source", nullable = false, length = 20)
    private ImportSource importSource = ImportSource.FILE;

    /** Dla DATABASE: komponent typu DB (ServerService). */
    @Column(name = "db_server_service_id")
    private Long dbServerServiceId;

    /** Dla DATABASE: nazwa widoku (tabeli) do odczytu. */
    @Column(name = "view_name", length = 255)
    private String viewName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 10)
    private FileType fileType = FileType.TXT;

    /** Dla TXT: separator pól (np. ",", "\\t"). */
    @Column(name = "field_separator", length = 20)
    private String fieldSeparator = ",";

    /** Dla TXT: separator końca linii (np. "\\n", "\\r\\n"). */
    @Column(name = "line_separator", length = 20)
    private String lineSeparator = "\n";

    /** Dla TXT: regex do dopasowania nazwy pliku. */
    @Column(name = "file_name_regex", length = 500)
    private String fileNameRegex;

    @OneToMany(mappedBy = "importDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<ImportFieldMapping> mappings = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JtuxedoService getJtuxedoService() {
        return jtuxedoService;
    }

    public void setJtuxedoService(JtuxedoService jtuxedoService) {
        this.jtuxedoService = jtuxedoService;
    }

    public ImportSource getImportSource() {
        return importSource;
    }

    public void setImportSource(ImportSource importSource) {
        this.importSource = importSource;
    }

    public Long getDbServerServiceId() {
        return dbServerServiceId;
    }

    public void setDbServerServiceId(Long dbServerServiceId) {
        this.dbServerServiceId = dbServerServiceId;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public FileType getFileType() {
        return fileType;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public String getFieldSeparator() {
        return fieldSeparator;
    }

    public void setFieldSeparator(String fieldSeparator) {
        this.fieldSeparator = fieldSeparator;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    public String getFileNameRegex() {
        return fileNameRegex;
    }

    public void setFileNameRegex(String fileNameRegex) {
        this.fileNameRegex = fileNameRegex;
    }

    public List<ImportFieldMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<ImportFieldMapping> mappings) {
        this.mappings = mappings;
    }

    public enum ImportSource {
        FILE("Plik"),
        DATABASE("Baza danych");

        private final String displayName;

        ImportSource(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum FileType {
        JSON("JSON"),
        XML("XML"),
        TXT("TXT"),
        CSV("CSV");

        private final String displayName;

        FileType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
