package pl.pekao.zesportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pekao.zesportal.entity.ImportDefinition;

import java.util.List;

public interface ImportDefinitionRepository extends JpaRepository<ImportDefinition, Long> {

    List<ImportDefinition> findByJtuxedoServiceIdOrderByNameAsc(Long jtuxedoServiceId);
}
