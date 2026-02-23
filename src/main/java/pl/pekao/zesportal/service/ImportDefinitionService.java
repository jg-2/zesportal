package pl.pekao.zesportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pekao.zesportal.entity.ImportDefinition;
import pl.pekao.zesportal.entity.ImportFieldMapping;
import pl.pekao.zesportal.repository.ImportDefinitionRepository;
import pl.pekao.zesportal.repository.ImportFieldMappingRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ImportDefinitionService {

    private final ImportDefinitionRepository definitionRepository;
    private final ImportFieldMappingRepository mappingRepository;

    public ImportDefinitionService(ImportDefinitionRepository definitionRepository,
                                   ImportFieldMappingRepository mappingRepository) {
        this.definitionRepository = definitionRepository;
        this.mappingRepository = mappingRepository;
    }

    @Transactional(readOnly = true)
    public List<ImportDefinition> findByServiceId(Long jtuxedoServiceId) {
        return definitionRepository.findByJtuxedoServiceIdOrderByNameAsc(jtuxedoServiceId);
    }

    @Transactional(readOnly = true)
    public Optional<ImportDefinition> findById(Long id) {
        return definitionRepository.findById(id);
    }

    /** Pobiera definicję wraz z mapowaniami (do edycji w UI). */
    @Transactional(readOnly = true)
    public Optional<ImportDefinition> findByIdWithMappings(Long id) {
        return definitionRepository.findById(id).map(def -> {
            def.getMappings().size();
            return def;
        });
    }

    @Transactional
    public ImportDefinition save(ImportDefinition definition) {
        if (definition.getMappings() != null) {
            for (int i = 0; i < definition.getMappings().size(); i++) {
                ImportFieldMapping m = definition.getMappings().get(i);
                m.setImportDefinition(definition);
                m.setSortOrder(i);
            }
        }
        return definitionRepository.save(definition);
    }

    @Transactional
    public void deleteById(Long id) {
        definitionRepository.deleteById(id);
    }
}
