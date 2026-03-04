package pl.pekao.zesportal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Konfiguracja puli sesji JOLT: rozmiar puli i czas bezczynności po którym pula jest zamykana.
 */
@Component
@ConfigurationProperties(prefix = "jolt.pool")
public class JoltPoolProperties {

    /** Maksymalna liczba sesji w jednej puli (per komponent ServerService). */
    private int maxSize = 10;

    /** Po ilu sekundach bez użycia pula jest zamykana (wszystkie sesje endSession). 0 = bez automatycznego zamykania. */
    private int idleTimeoutSeconds = 300;

    /** Timeout (sekundy) oczekiwania na zwolnienie sesji z puli przy borrow. */
    private int borrowTimeoutSeconds = 30;

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getIdleTimeoutSeconds() {
        return idleTimeoutSeconds;
    }

    public void setIdleTimeoutSeconds(int idleTimeoutSeconds) {
        this.idleTimeoutSeconds = idleTimeoutSeconds;
    }

    public int getBorrowTimeoutSeconds() {
        return borrowTimeoutSeconds;
    }

    public void setBorrowTimeoutSeconds(int borrowTimeoutSeconds) {
        this.borrowTimeoutSeconds = borrowTimeoutSeconds;
    }
}
