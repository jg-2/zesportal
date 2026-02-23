package pl.pekao.zesportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pekao.zesportal.entity.Environment;

import java.util.List;
import java.util.Optional;

public interface EnvironmentRepository extends JpaRepository<Environment, Long> {
    
    Optional<Environment> findByCode(String code);
    
    List<Environment> findByStatus(Environment.EnvironmentStatus status);
}
