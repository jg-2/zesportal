package pl.pekao.zesportal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "ssh_key")
public class SshKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Ścieżka do pliku klucza lub identyfikator (np. nazwa w agentcie). */
    @Column(name = "path_or_identifier", length = 500)
    private String pathOrIdentifier;

    @Column(name = "description", length = 500)
    private String description;

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

    public String getPathOrIdentifier() {
        return pathOrIdentifier;
    }

    public void setPathOrIdentifier(String pathOrIdentifier) {
        this.pathOrIdentifier = pathOrIdentifier;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
