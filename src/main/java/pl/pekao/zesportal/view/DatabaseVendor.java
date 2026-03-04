package pl.pekao.zesportal.view;

/**
 * Preset typów bazy danych – domyślna klasa sterownika i podpowiedź URL (dla komponentu DB).
 */
public enum DatabaseVendor {
    INFORMIX("Informix", "com.informix.jdbc.IfxDriver", "jdbc:informix-sqli://host:port/dbname:INFORMIXSERVER=..."),
    ORACLE("Oracle", "oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@//host:port/servicename"),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", "jdbc:postgresql://host:port/database"),
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://host:port/database"),
    H2("H2", "org.h2.Driver", "jdbc:h2:file:./data/db"),
    OTHER("Inna", "", "jdbc:...");

    private final String displayName;
    private final String defaultDriver;
    private final String urlPlaceholder;

    DatabaseVendor(String displayName, String defaultDriver, String urlPlaceholder) {
        this.displayName = displayName;
        this.defaultDriver = defaultDriver;
        this.urlPlaceholder = urlPlaceholder;
    }

    public String getDisplayName() { return displayName; }
    public String getDefaultDriver() { return defaultDriver; }
    public String getUrlPlaceholder() { return urlPlaceholder; }
}
