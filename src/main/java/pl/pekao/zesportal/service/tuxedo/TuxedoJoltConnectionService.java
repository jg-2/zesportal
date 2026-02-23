package pl.pekao.zesportal.service.tuxedo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.pekao.zesportal.config.TestJoltProperties;
import pl.pekao.zesportal.entity.ServerService;
import pl.pekao.zesportal.entity.ServerService.ServiceType;

/**
 * Nawiązuje połączenie z Tuxedo przez JOLT na podstawie parametrów komponentu (serwer, port, opcjonalnie User Name).
 * Wymaga jolt.jar (Oracle Tuxedo JOLT) na classpath – w przeciwnym razie zwraca komunikat o braku biblioteki.
 * Gdy test.jolt=true (lub tak/yes), tylko symuluje połączenie bez rzeczywistego połączenia.
 */
@Service
public class TuxedoJoltConnectionService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(TuxedoJoltConnectionService.class);

    private static final String JOLT_ATTRS = "bea.jolt.JoltSessionAttributes";
    private static final String JOLT_SESSION = "bea.jolt.JoltSession";

    private final TestJoltProperties testJoltProperties;

    public TuxedoJoltConnectionService(TestJoltProperties testJoltProperties) {
        this.testJoltProperties = testJoltProperties;
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

        if (testJoltProperties.isEnabled()) {
            log.info("Tryb testowy JOLT: symulacja połączenia do {} (komponent [{}])", appAddress, component.getName());
            return JoltConnectionResult.success("Tryb testowy: symulacja połączenia do " + appAddress + " (brak rzeczywistego połączenia).");
        }

        String userName = component.getTuxedoUserName() != null && !component.getTuxedoUserName().isBlank()
                ? component.getTuxedoUserName()
                : "";
        return connectWithJolt(appAddress, userName, component.getName());
    }

    /**
     * Wykonuje połączenie przez API JOLT (przez refleksję, żeby aplikacja działała bez jolt.jar).
     */
    private JoltConnectionResult connectWithJolt(String appAddress, String userName, String componentName) {
        try {
            Class<?> attrsClass = Class.forName(JOLT_ATTRS);
            Class<?> sessionClass = Class.forName(JOLT_SESSION);
            String appAddressConst = (String) attrsClass.getField("APPADDRESS").get(null);
            Object attrs = attrsClass.getDeclaredConstructor().newInstance();
            attrsClass.getMethod("setString", String.class, String.class).invoke(attrs, appAddressConst, appAddress);

            Object session = sessionClass.getConstructor(attrsClass, String.class, String.class, String.class, String.class)
                    .newInstance(attrs, userName, "", "", "");
            try {
                sessionClass.getMethod("endSession").invoke(session);
            } catch (Exception e) {
                log.debug("endSession: {}", e.getMessage());
            }
            log.info("Połączenie JOLT OK: {} ({}), komponent [{}]", appAddress, userName.isEmpty() ? "bez user" : userName, componentName);
            return JoltConnectionResult.success("Połączenie z " + appAddress + " nawiązane pomyślnie.");
        } catch (ClassNotFoundException e) {
            log.warn("Brak biblioteki JOLT (jolt.jar): {}", e.getMessage());
            return JoltConnectionResult.failure(
                    "Biblioteka JOLT (jolt.jar) nie jest dostępna. Dodaj jolt.jar z instalacji Oracle Tuxedo do classpath (np. do katalogu lib/ i zależności systemowej w pom.xml).");
        } catch (NoClassDefFoundError e) {
            log.warn("Brak zależności JOLT: {}", e.getMessage());
            return JoltConnectionResult.failure("Biblioteka JOLT nie jest dostępna: " + e.getMessage());
        } catch (Exception e) {
            log.warn("Błąd połączenia JOLT do {}: {}", appAddress, e.getMessage(), e);
            return JoltConnectionResult.failure("Błąd połączenia: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
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
