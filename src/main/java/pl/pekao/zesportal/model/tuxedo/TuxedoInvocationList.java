package pl.pekao.zesportal.model.tuxedo;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista wywołań usług Tuxedo: konfiguracja połączenia (serwer, port) oraz kolejne usługi do wywołania.
 * Po wywołaniu każdej usługi uzupełniane są status i rezultat w odpowiednim elemencie listy.
 * W przyszłości obiekt może być budowany m.in. z załadowanego pliku (np. według definicji importu).
 */
public class TuxedoInvocationList {

    /** Konfiguracja połączenia (serwer, port, opcjonalnie użytkownik). */
    private TuxedoConnectionConfig connectionConfig;

    /** Lista usług do wywołania (kolejność ma znaczenie). */
    private final List<TuxedoInvocationEntry> entries = new ArrayList<>();

    public TuxedoInvocationList() {
    }

    public TuxedoInvocationList(TuxedoConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    public TuxedoConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public void setConnectionConfig(TuxedoConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    /** Zwraca listę wpisów (można dodawać/usuwać elementy). */
    public List<TuxedoInvocationEntry> getEntries() {
        return entries;
    }

    /** Dodaje wpis do listy. */
    public TuxedoInvocationList addEntry(String serviceName, String parametersJson) {
        entries.add(new TuxedoInvocationEntry(serviceName, parametersJson));
        return this;
    }

    /**
     * Zwraca czytelny opis listy (konfiguracja + usługi do wywołania) do wypisania na ekran/log.
     */
    public String formatToString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== TuxedoInvocationList ===");
        if (connectionConfig != null) {
            sb.append("\n  Konfiguracja połączenia:");
            sb.append("\n    host = ").append(connectionConfig.getHost());
            sb.append("\n    port = ").append(connectionConfig.getPort());
            sb.append("\n    tuxedoUserName = ").append(connectionConfig.getTuxedoUserName() != null ? connectionConfig.getTuxedoUserName() : "(brak)");
            sb.append("\n    serverServiceId = ").append(connectionConfig.getServerServiceId() != null ? connectionConfig.getServerServiceId() : "(brak)");
        } else {
            sb.append("\n  Konfiguracja połączenia: (brak)");
        }
        sb.append("\n  Lista usług do wywołania (").append(entries.size()).append("):");
        for (int i = 0; i < entries.size(); i++) {
            TuxedoInvocationEntry e = entries.get(i);
            sb.append("\n    [").append(i + 1).append("] ");
            sb.append("nazwa = ").append(e.getServiceName() != null ? e.getServiceName() : "(brak)");
            sb.append(", parametry (JSON) = ").append(e.getParametersJson() != null ? e.getParametersJson() : "{}");
            sb.append(", status = ").append(e.getStatus());
            if (e.getResultJson() != null && !e.getResultJson().isBlank()) {
                sb.append(", rezultat (JSON) = ").append(e.getResultJson());
            }
            if (e.getMessage() != null && !e.getMessage().isBlank()) {
                sb.append(", message = ").append(e.getMessage());
            }
        }
        sb.append("\n==============================");
        return sb.toString();
    }
}
