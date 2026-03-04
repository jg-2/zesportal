package pl.pekao.zesportal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "task_template")
public class TaskTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TaskTemplateType type = TaskTemplateType.TUXEDO;

    /** Konfiguracja zależna od typu: JSON (np. dla TUXEDO: serviceId, serverId; dla SSH: host, command). */
    @Column(name = "config", length = 4000)
    private String config;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskTemplateType getType() {
        return type;
    }

    public void setType(TaskTemplateType type) {
        this.type = type;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public enum TaskTemplateType {
        TUXEDO("Tuxedo", "Wywołanie komponentu Oracle Tuxedo"),
        SSH("SSH", "Wykonanie polecenia przez SSH"),
        SAVE_FILE("Zapisz do pliku", "Zapis wyniku zadania do pliku");

        private final String displayName;
        private final String description;

        TaskTemplateType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }
}
