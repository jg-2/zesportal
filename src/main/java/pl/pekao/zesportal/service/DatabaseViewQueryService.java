package pl.pekao.zesportal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import pl.pekao.zesportal.entity.ServerService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Odczyt danych z widoku bazy danych na podstawie konfiguracji JDBC z komponentu DB.
 */
@Service
public class DatabaseViewQueryService {

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * Pobiera dane z widoku. Nazwa widoku musi pasować do viewNameRegex z konfiguracji komponentu (jeśli ustawiony).
     *
     * @param dbComponent komponent typu DB z konfiguracją JDBC
     * @param viewName    nazwa widoku (tabeli)
     * @return lista wierszy – każdy to mapa: nazwa kolumny → wartość
     */
    public List<Map<String, Object>> queryView(ServerService dbComponent, String viewName) throws Exception {
        if (dbComponent == null || dbComponent.getType() != ServerService.ServiceType.DB) {
            throw new IllegalArgumentException("Komponent musi być typu DB");
        }
        String config = dbComponent.getConfig();
        if (config == null || config.isBlank()) {
            throw new IllegalStateException("Brak konfiguracji JDBC w komponencie");
        }
        JsonNode n = JSON.readTree(config);
        String url = n.has("jdbcUrl") ? n.get("jdbcUrl").asText() : null;
        String driver = n.has("driverClassName") ? n.get("driverClassName").asText() : null;
        String user = n.has("username") ? n.get("username").asText() : null;
        String password = n.has("password") ? n.get("password").asText() : null;
        String viewNameRegex = n.has("viewNameRegex") ? n.get("viewNameRegex").asText() : null;

        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Brak jdbcUrl w konfiguracji komponentu DB");
        }
        if (viewName == null || viewName.isBlank()) {
            throw new IllegalArgumentException("Brak nazwy widoku");
        }
        viewName = viewName.trim();
        if (viewNameRegex != null && !viewNameRegex.isBlank()) {
            // Regex traktujemy jako whitelistę NAZWY widoku (bez schematu). Użytkownik często wpisuje np. "v_.*",
            // a w UI wybiera "public.v_coś" – walidujemy wtedy tylko część po ostatniej kropce.
            String nameToValidate = viewName;
            int dot = viewName.lastIndexOf('.');
            if (dot >= 0 && dot + 1 < viewName.length()) {
                nameToValidate = viewName.substring(dot + 1);
            }
            if (!Pattern.compile(viewNameRegex).matcher(nameToValidate).matches()) {
                throw new SecurityException("Nazwa widoku \"" + viewName + "\" (część: \"" + nameToValidate + "\") nie pasuje do dozwolonego wzorca (viewNameRegex)");
            }
        }

        if (driver != null && !driver.isBlank()) {
            Class.forName(driver);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, user != null ? user : "", password != null ? password : "");
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + viewName)) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    String label = meta.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(label, value);
                }
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * Zwraca listę nazw widoków/tabel z bazy, pasujących do viewNameRegex z konfiguracji komponentu.
     */
    public List<String> getViewNames(ServerService dbComponent) throws Exception {
        if (dbComponent == null || dbComponent.getType() != ServerService.ServiceType.DB) {
            return List.of();
        }
        String config = dbComponent.getConfig();
        if (config == null || config.isBlank()) return List.of();
        JsonNode n = JSON.readTree(config);
        String url = n.has("jdbcUrl") ? n.get("jdbcUrl").asText() : null;
        String driver = n.has("driverClassName") ? n.get("driverClassName").asText() : null;
        String user = n.has("username") ? n.get("username").asText() : null;
        String password = n.has("password") ? n.get("password").asText() : null;
        String viewNameRegex = n.has("viewNameRegex") ? n.get("viewNameRegex").asText() : null;
        if (url == null || url.isBlank()) return List.of();
        if (driver != null && !driver.isBlank()) Class.forName(driver);
        Pattern pattern = (viewNameRegex != null && !viewNameRegex.isBlank()) ? Pattern.compile(viewNameRegex) : null;
        List<String> names = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, user != null ? user : "", password != null ? password : "")) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"VIEW", "TABLE"})) {
                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME");
                    if (name != null && !name.isBlank()) {
                        if (pattern == null || pattern.matcher(name).matches()) {
                            String schema = rs.getString("TABLE_SCHEM");
                            String full = (schema != null && !schema.isBlank()) ? schema + "." + name : name;
                            names.add(full);
                        }
                    }
                }
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /**
     * Zwraca listę nazw kolumn dla danego widoku/tabeli.
     */
    public List<String> getViewColumns(ServerService dbComponent, String viewName) throws Exception {
        if (dbComponent == null || viewName == null || viewName.isBlank()) return List.of();
        String config = dbComponent.getConfig();
        if (config == null || config.isBlank()) return List.of();
        JsonNode n = JSON.readTree(config);
        String url = n.has("jdbcUrl") ? n.get("jdbcUrl").asText() : null;
        String driver = n.has("driverClassName") ? n.get("driverClassName").asText() : null;
        String user = n.has("username") ? n.get("username").asText() : null;
        String password = n.has("password") ? n.get("password").asText() : null;
        if (url == null || url.isBlank()) return List.of();
        if (driver != null && !driver.isBlank()) Class.forName(driver);
        List<String> columns = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(url, user != null ? user : "", password != null ? password : "");
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + viewName + " WHERE 1=0")) {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                columns.add(meta.getColumnLabel(i));
            }
        }
        return columns;
    }
}
