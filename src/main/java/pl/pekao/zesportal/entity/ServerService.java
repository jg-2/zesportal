package pl.pekao.zesportal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Usługa na serwerze: konkretna usługa Tuxedo lub SSH na danym hoście.
 * Np. na serwerze tux.cdm: usługa SSH (user tuxmae1), usługa Tuxedo (port 1234).
 */
@Entity
@Table(name = "server_service")
public class ServerService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "environment_id", nullable = false)
    private Environment environment;

    @NotBlank
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ServiceType type = ServiceType.TUXEDO;

    @Column(name = "port")
    private Integer port;

    @Column(name = "username", length = 100)
    private String username;

    /** Tuxedo: minimalna wielkość puli. */
    @Column(name = "pool_min")
    private Integer poolMin;

    /** Tuxedo: maksymalna wielkość puli. */
    @Column(name = "pool_max")
    private Integer poolMax;

    /** Tuxedo: nazwa użytkownika (User Name). */
    @Column(name = "tuxedo_user_name", length = 100)
    private String tuxedoUserName;

    /** Tuxedo: true = połączenie bez autentykacji (NOAUTH), false = autentykacja użytkownika/hasła (USRPASSWORD). Domyślnie true. */
    @Column(name = "tuxedo_no_auth", nullable = false)
    private Boolean tuxedoNoAuth = true;

    /** Tuxedo: hasło użytkownika (gdy tuxedoNoAuth = false). */
    @Column(name = "tuxedo_password", length = 500)
    private String tuxedoPassword;

    /** SSH: klucz do podłączenia sesji. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ssh_key_id")
    private SshKey sshKey;

    /** Konfiguracja w JSON – uzupełniana z pól przy zapisie (kompatybilność wsteczna). */
    @Column(name = "config", length = 2000)
    private String config;

    @Column(name = "description", length = 500)
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServiceType getType() {
        return type;
    }

    public void setType(ServiceType type) {
        this.type = type;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getPoolMin() {
        return poolMin;
    }

    public void setPoolMin(Integer poolMin) {
        this.poolMin = poolMin;
    }

    public Integer getPoolMax() {
        return poolMax;
    }

    public void setPoolMax(Integer poolMax) {
        this.poolMax = poolMax;
    }

    public String getTuxedoUserName() {
        return tuxedoUserName;
    }

    public void setTuxedoUserName(String tuxedoUserName) {
        this.tuxedoUserName = tuxedoUserName;
    }

    public Boolean getTuxedoNoAuth() {
        return tuxedoNoAuth != null ? tuxedoNoAuth : true;
    }

    public void setTuxedoNoAuth(Boolean tuxedoNoAuth) {
        this.tuxedoNoAuth = tuxedoNoAuth;
    }

    public String getTuxedoPassword() {
        return tuxedoPassword;
    }

    public void setTuxedoPassword(String tuxedoPassword) {
        this.tuxedoPassword = tuxedoPassword;
    }

    public SshKey getSshKey() {
        return sshKey;
    }

    public void setSshKey(SshKey sshKey) {
        this.sshKey = sshKey;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public enum ServiceType {
        TUXEDO("Tuxedo", "Połączenie na port Tuxedo"),
        SSH("SSH", "SSH – logowanie (np. user tuxmae1) i wywołanie skryptów"),
        DB("Baza danych", "Połączenie JDBC (Informix, Oracle, PostgreSQL, itd.)");

        private final String displayName;
        private final String hint;

        ServiceType(String displayName, String hint) {
            this.displayName = displayName;
            this.hint = hint;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getHint() {
            return hint;
        }
    }
}
