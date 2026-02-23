package pl.pekao.zesportal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Wspólna konfiguracja trybu testowego JOLT (test.jolt).
 * Jedna interpretacja wartości (tak/yes/true/1) w całej aplikacji.
 */
@Component
public class TestJoltProperties {

    @Value("${test.jolt:false}")
    private String raw;

    /** true, gdy test.jolt ustawione na true / tak / yes / 1 (bez rozróżniania wielkości liter). */
    public boolean isEnabled() {
        if (raw == null) return false;
        String v = raw.trim().toLowerCase();
        return "true".equals(v) || "tak".equals(v) || "yes".equals(v) || "1".equals(v);
    }
}
