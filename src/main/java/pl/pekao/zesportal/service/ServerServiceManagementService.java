package pl.pekao.zesportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pekao.zesportal.entity.ServerService;
import pl.pekao.zesportal.entity.ServerService.ServiceType;
import pl.pekao.zesportal.repository.ServerServiceRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ServerServiceManagementService {

    private final ServerServiceRepository repository;

    public ServerServiceManagementService(ServerServiceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ServerService> findAllWithServerAndEnvironment() {
        return repository.findAllWithServerAndEnvironment();
    }

    @Transactional(readOnly = true)
    public List<ServerService> findTuxedoServicesByEnvironmentId(Long environmentId) {
        return repository.findByEnvironmentIdAndType(environmentId, ServiceType.TUXEDO);
    }

    @Transactional(readOnly = true)
    public List<ServerService> findDbServices() {
        return repository.findByType(ServiceType.DB);
    }

    @Transactional(readOnly = true)
    public List<ServerService> findByServerId(Long serverId) {
        return repository.findByServerIdOrderByNameAsc(serverId);
    }

    @Transactional(readOnly = true)
    public Optional<ServerService> findById(Long id) {
        return repository.findById(id);
    }

    /** Pobiera komponent z serwerem i środowiskiem (JOIN FETCH) – do użycia poza kontekstem transakcji, np. w executorze zadań. */
    @Transactional(readOnly = true)
    public Optional<ServerService> findByIdWithServer(Long id) {
        return repository.findByIdWithServer(id);
    }

    @Transactional
    public ServerService save(ServerService serverService) {
        return repository.save(serverService);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
