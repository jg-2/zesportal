package pl.pekao.zesportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.pekao.zesportal.entity.ServerService;
import pl.pekao.zesportal.entity.ServerService.ServiceType;

import java.util.List;
import java.util.Optional;

public interface ServerServiceRepository extends JpaRepository<ServerService, Long> {

    @Query("SELECT ss FROM ServerService ss " +
           "LEFT JOIN FETCH ss.server s LEFT JOIN FETCH ss.environment e LEFT JOIN FETCH ss.sshKey " +
           "ORDER BY e.code, s.name, ss.name")
    List<ServerService> findAllWithServerAndEnvironment();

    @Query("SELECT ss FROM ServerService ss " +
           "LEFT JOIN FETCH ss.server s LEFT JOIN FETCH ss.environment LEFT JOIN FETCH ss.sshKey " +
           "WHERE ss.environment.id = :environmentId AND ss.type = :type " +
           "ORDER BY s.name, ss.name")
    List<ServerService> findByEnvironmentIdAndType(Long environmentId, ServiceType type);

    List<ServerService> findByServerIdOrderByNameAsc(Long serverId);

    @Query("SELECT ss FROM ServerService ss LEFT JOIN FETCH ss.server LEFT JOIN FETCH ss.environment WHERE ss.id = :id")
    Optional<ServerService> findByIdWithServer(Long id);

    @Query("SELECT ss FROM ServerService ss LEFT JOIN FETCH ss.server LEFT JOIN FETCH ss.environment WHERE ss.type = :type ORDER BY ss.environment.code, ss.name")
    List<ServerService> findByType(ServiceType type);
}
