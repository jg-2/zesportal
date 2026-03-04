package pl.pekao.zesportal.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Jednorazowa migracja schematu: usuwa kolumnę ENVIRONMENT_ID z tabeli SERVER
 * (encja Server już nie ma pola environment; Hibernate z ddl-auto=update nie usuwa kolumn).
 */
@Component
@Order(0)
public class SchemaMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        dropServerEnvironmentIdIfExists();
        ensureTaskTypeAcceptsSaveFile();
        ensureServerServiceTypeAcceptsDb();
        ensureImportDefinitionHasDatabaseColumns();
        ensureImportFieldMappingSourceTypeAcceptsDatabaseField();
        ensureJtuxedoServiceSingleName();
    }

    /**
     * Przywraca wersję z jedną nazwą: usuwa kolumnę display_name jeśli istnieje,
     * dodaje unikalność na name jeśli brak.
     */
    private void ensureJtuxedoServiceSingleName() {
        try {
            var rows = jdbcTemplate.queryForList(
                    "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'JTUXEDO_SERVICE' AND COLUMN_NAME = 'DISPLAY_NAME'");
            if (!rows.isEmpty()) {
                jdbcTemplate.execute("ALTER TABLE jtuxedo_service DROP COLUMN display_name");
            }
        } catch (Exception e) {
            // Kolumna nie istnieje lub inna baza – ignoruj
        }
        try {
            jdbcTemplate.execute("ALTER TABLE jtuxedo_service ADD CONSTRAINT jtuxedo_service_name_uk UNIQUE (name)");
        } catch (Exception e) {
            // Constraint już istnieje
        }
    }

    /**
     * Dodaje kolumny dla importu z bazy danych (import_source, db_server_service_id, view_name),
     * jeśli tabela import_definition ich nie ma. Próba ADD COLUMN – jeśli kolumna już jest, H2 rzuci wyjątkiem i go ignorujemy.
     */
    private void ensureImportDefinitionHasDatabaseColumns() {
        addColumnIfMissing("import_definition", "import_source", "VARCHAR(20) DEFAULT 'FILE'");
        addColumnIfMissing("import_definition", "db_server_service_id", "BIGINT");
        addColumnIfMissing("import_definition", "view_name", "VARCHAR(255)");
    }

    private void addColumnIfMissing(String table, String column, String typeSpec) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + typeSpec);
        } catch (Exception e) {
            // Kolumna już istnieje (42121) lub tabela nie istnieje – ignoruj
        }
    }

    /**
     * H2 mógł utworzyć kolumnę SOURCE_TYPE w IMPORT_FIELD_MAPPING jako ENUM('CONSTANT','FILE_FIELD').
     * Aby dopuścić DATABASE_FIELD, zmieniamy kolumnę na VARCHAR(20).
     */
    private void ensureImportFieldMappingSourceTypeAcceptsDatabaseField() {
        try {
            jdbcTemplate.execute("ALTER TABLE import_field_mapping ALTER COLUMN source_type VARCHAR(20)");
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("DATABASE_FIELD") || msg.contains("not permitted") || msg.contains("22030")) {
                try {
                    migrateImportFieldMappingSourceTypeEnumToVarchar();
                } catch (Exception ex) {
                    // Może kolumna już zmigrowana
                }
            }
            // Inne błędy (np. tabela nie istnieje / inna baza) – ignoruj
        }
    }

    private void migrateImportFieldMappingSourceTypeEnumToVarchar() {
        jdbcTemplate.execute("ALTER TABLE import_field_mapping ADD COLUMN source_type_new VARCHAR(20)");
        jdbcTemplate.execute("UPDATE import_field_mapping SET source_type_new = CAST(source_type AS VARCHAR(100))");
        jdbcTemplate.execute("ALTER TABLE import_field_mapping DROP COLUMN source_type");
        jdbcTemplate.execute("ALTER TABLE import_field_mapping ALTER COLUMN source_type_new RENAME TO source_type");
    }

    /**
     * H2 mógł utworzyć kolumnę TASK_TYPE jako ENUM('SSH','TUXEDO'). Aby dopuścić SAVE_FILE,
     * zmieniamy kolumnę na VARCHAR(30).
     */
    private void ensureTaskTypeAcceptsSaveFile() {
        try {
            jdbcTemplate.execute("ALTER TABLE task ALTER COLUMN task_type VARCHAR(30)");
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("SAVE_FILE") || msg.contains("not permitted") || msg.contains("22030")) {
                try {
                    migrateTaskTypeEnumToVarchar();
                } catch (Exception ex) {
                    // Może kolumna już zmigrowana
                }
            }
            // Inne błędy (np. inna baza niż H2) – ignoruj
        }
    }

    private void migrateTaskTypeEnumToVarchar() {
        jdbcTemplate.execute("ALTER TABLE task ADD COLUMN task_type_new VARCHAR(30)");
        jdbcTemplate.execute("UPDATE task SET task_type_new = CAST(task_type AS VARCHAR(100))");
        jdbcTemplate.execute("ALTER TABLE task DROP COLUMN task_type");
        jdbcTemplate.execute("ALTER TABLE task ALTER COLUMN task_type_new RENAME TO task_type");
    }

    /**
     * H2 mógł utworzyć kolumnę TYPE w SERVER_SERVICE jako ENUM('SSH','TUXEDO'). Aby dopuścić DB,
     * zmieniamy kolumnę na VARCHAR(20).
     */
    private void ensureServerServiceTypeAcceptsDb() {
        try {
            jdbcTemplate.execute("ALTER TABLE server_service ALTER COLUMN type VARCHAR(20)");
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("DB") || msg.contains("not permitted") || msg.contains("22030")) {
                try {
                    migrateServerServiceTypeEnumToVarchar();
                } catch (Exception ex) {
                    // Może kolumna już zmigrowana
                }
            }
        }
    }

    private void migrateServerServiceTypeEnumToVarchar() {
        jdbcTemplate.execute("ALTER TABLE server_service ADD COLUMN type_new VARCHAR(20)");
        jdbcTemplate.execute("UPDATE server_service SET type_new = CAST(type AS VARCHAR(100))");
        jdbcTemplate.execute("ALTER TABLE server_service DROP COLUMN type");
        jdbcTemplate.execute("ALTER TABLE server_service ALTER COLUMN type_new RENAME TO type");
    }

    private void dropServerEnvironmentIdIfExists() {
        try {
            var rows = jdbcTemplate.queryForList(
                    "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'SERVER' AND COLUMN_NAME = 'ENVIRONMENT_ID'"
            );
            if (!rows.isEmpty()) {
                jdbcTemplate.execute("ALTER TABLE server DROP COLUMN environment_id");
            }
        } catch (Exception e) {
            // Kolumna już usunięta lub baza w innym dialekcie – ignorujemy
        }
    }
}
