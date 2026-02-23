package pl.pekao.zesportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pekao.zesportal.entity.ImportFieldMapping;

import java.util.List;

public interface ImportFieldMappingRepository extends JpaRepository<ImportFieldMapping, Long> {

    List<ImportFieldMapping> findByImportDefinitionIdOrderBySortOrderAscIdAsc(Long importDefinitionId);
}
