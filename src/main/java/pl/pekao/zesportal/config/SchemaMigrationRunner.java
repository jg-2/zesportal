package pl.pekao.zesportal.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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
