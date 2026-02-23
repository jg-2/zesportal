package pl.pekao.zesportal.model.tuxedo;

/**
 * Pojedyncze wywołanie usługi Tuxedo na liście: nazwa usługi, parametry wejściowe (JSON),
 * status po wywołaniu oraz rezultat (JSON).
 */
public class TuxedoInvocationEntry {

    /** Nazwa usługi Tuxedo (np. TOUPPER, GET_CUSTOMER). */
    private String serviceName;

    /** Parametry wejściowe w formacie JSON, np. {"PESEL":"12112","kod_klienta":"csotam"}. */
    private String parametersJson;

    /** Status po wywołaniu (przed wywołaniem: PENDING, po: OK / ERROR). */
    private InvocationStatus status = InvocationStatus.PENDING;

    /** Rezultat zwrócony przez usługę, w formacie JSON. */
    private String resultJson;

    /** Komunikat błędu lub informacja (gdy status = ERROR). */
    private String message;

    public TuxedoInvocationEntry() {
    }

    public TuxedoInvocationEntry(String serviceName, String parametersJson) {
        this.serviceName = serviceName;
        this.parametersJson = parametersJson;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getParametersJson() {
        return parametersJson;
    }

    public void setParametersJson(String parametersJson) {
        this.parametersJson = parametersJson;
    }

    public InvocationStatus getStatus() {
        return status;
    }

    public void setStatus(InvocationStatus status) {
        this.status = status;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public enum InvocationStatus {
        /** Oczekuje na wywołanie. */
        PENDING,
        /** Wywołanie zakończone sukcesem. */
        OK,
        /** Błąd wywołania lub połączenia. */
        ERROR
    }
}
