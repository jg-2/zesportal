package pl.pekao.zesportal.service.tuxedo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import pl.pekao.zesportal.config.TestJoltProperties;
import pl.pekao.zesportal.entity.ServerService;
import pl.pekao.zesportal.entity.ServerService.ServiceType;

/**
 * Nawiązuje połączenie z Tuxedo przez JOLT na podstawie parametrów komponentu (serwer, port, opcjonalnie User Name).
 * Wymaga jolt.jar (Oracle Tuxedo JOLT) na classpath – w przeciwnym razie zwraca komunikat o braku biblioteki.
 * Gdy test.jolt=true (lub tak/yes), tylko symuluje połączenie bez rzeczywistego połączenia.
 * Wywołania usług jak w przykładzie: createSessionPool(addr, null, min, max, ui, spName), getSessionPool(spName), sp.call(serviceName, ds, null).
 */
@Service
public class TuxedoJoltConnectionService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(TuxedoJoltConnectionService.class);

    private static final String JOLT_ATTRS = "bea.jolt.JoltSessionAttributes";
    private static final String JOLT_SESSION = "bea.jolt.JoltSession";

    private final TestJoltProperties testJoltProperties;
    private final ServletSessionPoolManager poolManager;

    public TuxedoJoltConnectionService(TestJoltProperties testJoltProperties,
                                       @Lazy ServletSessionPoolManager poolManager) {
        this.testJoltProperties = testJoltProperties;
        this.poolManager = poolManager;
    }

    /**
     * Zwraca pulę dla komponentu (jak w przykładzie: spm.getSessionPool(spName)). Pula tworzona przez createSessionPool(addr, null, min, max, ui, spName).
     */
    public Object getSessionPool(ServerService serverService) throws Exception {
        return poolManager.getSessionPool(serverService);
    }

    /**
     * Wywołanie serwisu jak w przykładzie: callRes = sp.call(serviceName, ds, null).
     */
    public Object call(ServerService serverService, String serviceName, Object dataSet) throws Exception {
        return poolManager.call(serverService, serviceName, dataSet);
    }

    /**
     * Wywołanie serwisu z mapą parametrów (tworzy DataSet i wywołuje sp.call(serviceName, ds, null)).
     */
    public Object call(ServerService serverService, String serviceName, java.util.Map<String, Object> params) throws Exception {
        return poolManager.call(serverService, serviceName, params);
    }

    /**
     * Konwertuje JOLT Result (DataSet) na JSON – pełny recordset wyjścia do zapisu w rezultacie zadania.
     */
    public String recordsetToJson(Object joltResult) {
        return poolManager.recordsetToJson(joltResult);
    }

    /**
     * Zamyka sesję JOLT (endSession). Używane wewnętrznie przez pulę przy zwalnianiu zasobów.
     */
    public void endSession(Object session) {
        if (session == null) return;
        try {
            Class<?> sessionClass = Class.forName(JOLT_SESSION);
            sessionClass.getMethod("endSession").invoke(session);
        } catch (Exception e) {
            log.debug("endSession: {}", e.getMessage());
        }
    }

    /**
     * Próba nawiązania połączenia JOLT z parametrami wybranego komponentu Tuxedo.
     * Sesja jest od razu zamykana po udanym połączeniu.
     * Gdy test.jolt=tak – zwraca sukces bez rzeczywistego połączenia.
     *
     * @param component komponent systemowy typu TUXEDO (serwer, port, opcjonalnie tuxedoUserName)
     * @return wynik: sukces + komunikat lub błąd
     */
    public JoltConnectionResult connect(ServerService component) {
        if (component == null || component.getType() != ServiceType.TUXEDO) {
            return JoltConnectionResult.failure("Komponent musi być typu Tuxedo.");
        }
        String host = component.getServer() != null ? component.getServer().getAddress() : null;
        if (host == null || host.isBlank()) {
            return JoltConnectionResult.failure("Brak adresu serwera w komponencie.");
        }
        int port = component.getPort() != null ? component.getPort() : 0;
        if (port <= 0 && component.getConfig() != null && !component.getConfig().isBlank()) {
            try {
                JsonNode n = JSON.readTree(component.getConfig());
                if (n.has("port")) port = n.get("port").asInt();
            } catch (Exception ignored) {
            }
        }
        if (port <= 0) {
            return JoltConnectionResult.failure("Brak portu w komponencie.");
        }
        String appAddress = "//" + host.trim() + ":" + port;
        boolean noAuth = component.getTuxedoNoAuth() == null || component.getTuxedoNoAuth();
        String userName = component.getTuxedoUserName() != null && !component.getTuxedoUserName().isBlank()
                ? component.getTuxedoUserName().trim()
                : null;
        String password = !noAuth && component.getTuxedoPassword() != null && !component.getTuxedoPassword().isBlank()
                ? component.getTuxedoPassword()
                : null;
        boolean hasPassword = password != null && !password.isBlank();

        log.info("JOLT nawiązywanie połączenia, komponent [{}]: {}", component.getName(), formatSessionParams(appAddress, noAuth, userName, hasPassword));

        if (testJoltProperties.isEnabled()) {
            log.info("Tryb testowy JOLT: symulacja połączenia (brak rzeczywistego połączenia)");
            return JoltConnectionResult.success("Tryb testowy: symulacja połączenia do " + appAddress + " (brak rzeczywistego połączenia).");
        }

        try {
            Object session = createSessionInternal(appAddress, noAuth, userName, password);
            endSession(session);
            log.info("Połączenie JOLT OK: {} ({}), komponent [{}]", appAddress, noAuth ? "NOAUTH" : ("user=" + (userName != null ? userName : "")), component.getName());
            return JoltConnectionResult.success("Połączenie z " + appAddress + " nawiązane pomyślnie.");
        } catch (ClassNotFoundException e) {
            log.warn("Brak biblioteki JOLT (jolt.jar): {}", e.getMessage());
            return JoltConnectionResult.failure(
                    "Biblioteka JOLT (jolt.jar) nie jest dostępna. Dodaj jolt.jar z instalacji Oracle Tuxedo do classpath (np. do katalogu lib/).");
        } catch (NoClassDefFoundError e) {
            log.warn("Brak zależności JOLT: {}", e.getMessage());
            return JoltConnectionResult.failure("Biblioteka JOLT nie jest dostępna: " + e.getMessage());
        } catch (Exception e) {
            String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            log.warn("Błąd połączenia JOLT do {}: {}", appAddress, msg, e);
            if (msg != null && msg.contains("J_ESTCON")) {
                return JoltConnectionResult.failure(
                        "JOLT J_ESTCON (błąd nawiązania sesji). Sprawdź: (1) poziom autentykacji po stronie JSL (NOAUTH vs USRPASSWORD/APPASSWORD), "
                                + "(2) adres i port JOLT, (3) wersję protokołu JOLT klienta i serwera. Szczegóły: " + msg);
            }
            if (msg != null && msg.toUpperCase().contains("AUTHLEVEL")) {
                return JoltConnectionResult.failure(
                        "JOLT AUTHLEVEL: niezgodność poziomu autentykacji. Upewnij się, że w komponencie masz zaznaczone „Autentykacja bez hasła (NOAUTH)” "
                                + "i że JSL (Jolt Server) jest skonfigurowany na NOAUTH – albo odznacz NOAUTH i podaj user/hasło, jeśli JSL wymaga USRPASSWORD. Szczegóły: " + msg);
            }
            return JoltConnectionResult.failure("Błąd połączenia: " + msg);
        }
    }

    /**
     * Tworzy sesję JOLT – wyłącznie przez konstruktor bea.jolt.JoltSession (refleksja). Używane tylko w connect (test).
     * Pula sesji używa wyłącznie SessionPool.getSession() (ServletSessionPoolManager), bez własnej sesji.
     */
    private Object createSessionInternal(String appAddress, boolean noAuth, String userName, String password) throws Exception {
        Class<?> attrsClass = Class.forName(JOLT_ATTRS);
        Class<?> sessionClass = Class.forName(JOLT_SESSION);
        String appAddressConst = (String) attrsClass.getField("APPADDRESS").get(null);
        Object attrs = attrsClass.getDeclaredConstructor().newInstance();
        attrsClass.getMethod("setString", String.class, String.class).invoke(attrs, appAddressConst, appAddress);

        // CONNECTIONMODE=ANY – klient akceptuje tryb narzucony przez JSL (RETAINED lub RECONNECT). Bez tego JSL może zwrócić "inappropriate session type".
        try {
            int anyValue = attrsClass.getField("ANY").getInt(null);
            attrsClass.getMethod("setInt", String.class, int.class).invoke(attrs, "CONNECTIONMODE", anyValue);
        } catch (Exception ex) {
            log.debug("Nie ustawiono CONNECTIONMODE (możliwa inna wersja jolt.jar): {}", ex.getMessage());
        }

        // Przy noAuth: część JSL wymaga jawnego AUTHLEVEL=NOAUTH; część działa bez ustawiania. Próbujemy NOAUTH.
        // Przy user+hasło: AUTHLEVEL=USRPASSWORD.
        try {
            String authLevelKey = (String) attrsClass.getField("AUTHLEVEL").get(null);
            if (noAuth) {
                int noAuthValue = attrsClass.getField("NOAUTH").getInt(null);
                attrsClass.getMethod("setInt", String.class, int.class).invoke(attrs, authLevelKey, noAuthValue);
            } else {
                int usrPasswordValue = attrsClass.getField("USRPASSWORD").getInt(null);
                attrsClass.getMethod("setInt", String.class, int.class).invoke(attrs, authLevelKey, usrPasswordValue);
            }
        } catch (Exception ex) {
            log.debug("Nie ustawiono AUTHLEVEL (możliwa inna wersja jolt.jar): {}", ex.getMessage());
        }

        // Pusty UserInfo: puste stringi zamiast null (niektóre JSL oczekują "" przy noAuth).
        String role = noAuth ? "" : null;
        String userPassword = noAuth ? "" : (password != null ? password : "");
        String appPassword = noAuth ? "" : null;
        String sessionUser = noAuth ? "" : (userName != null ? userName : "");
        return sessionClass.getConstructor(attrsClass, String.class, String.class, String.class, String.class)
                .newInstance(attrs, sessionUser, role, userPassword, appPassword);
    }

    private static String formatSessionParams(String appAddress, boolean noAuth, String userName, boolean hasPassword) {
        return String.format("APPADDRESS=%s, AUTHLEVEL=%s, userName=%s, hasło=%s",
                appAddress, noAuth ? "NOAUTH (pusty UserInfo)" : "USRPASSWORD", userName != null ? userName : "(brak)", hasPassword ? "ustawione" : "nie");
    }

    public static final class JoltConnectionResult {
        private final boolean success;
        private final String message;

        private JoltConnectionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static JoltConnectionResult success(String message) {
            return new JoltConnectionResult(true, message);
        }

        public static JoltConnectionResult failure(String message) {
            return new JoltConnectionResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
