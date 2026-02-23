package pl.pekao.zesportal.model.tuxedo;

/**
 * Konfiguracja połączenia do Tuxedo (JOLT): host, port, opcjonalnie użytkownik.
 * Może być uzupełniona z bazy (ServerService) lub z pliku/JSON.
 */
public class TuxedoConnectionConfig {

    /** Adres hosta (np. tux.cdm.local). */
    private String host;

    /** Port JOLT (np. 14000). */
    private Integer port;

    /** Opcjonalna nazwa użytkownika Tuxedo (User Name). */
    private String tuxedoUserName;

    /** Id komponentu (ServerService) w bazie – gdy ustawione, host/port mogą być uzupełniane z bazy. */
    private Long serverServiceId;

    public TuxedoConnectionConfig() {
    }

    public TuxedoConnectionConfig(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Tworzy konfigurację na podstawie komponentu systemowego (ServerService).
     */
    public static TuxedoConnectionConfig fromServerService(pl.pekao.zesportal.entity.ServerService component) {
        if (component == null) return null;
        TuxedoConnectionConfig cfg = new TuxedoConnectionConfig();
        cfg.setServerServiceId(component.getId());
        if (component.getServer() != null) {
            cfg.setHost(component.getServer().getAddress());
        }
        cfg.setPort(component.getPort());
        cfg.setTuxedoUserName(component.getTuxedoUserName());
        return cfg;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getTuxedoUserName() {
        return tuxedoUserName;
    }

    public void setTuxedoUserName(String tuxedoUserName) {
        this.tuxedoUserName = tuxedoUserName;
    }

    public Long getServerServiceId() {
        return serverServiceId;
    }

    public void setServerServiceId(Long serverServiceId) {
        this.serverServiceId = serverServiceId;
    }
}
