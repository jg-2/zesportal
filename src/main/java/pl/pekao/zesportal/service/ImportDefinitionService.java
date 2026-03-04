package pl.pekao.zesportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pekao.zesportal.entity.ImportDefinition;
import pl.pekao.zesportal.entity.ImportDefinition.ImportSource;
import pl.pekao.zesportal.entity.ImportFieldMapping;
import pl.pekao.zesportal.entity.ImportFieldMapping.SourceType;
import pl.pekao.zesportal.entity.ServerService;
import pl.pekao.zesportal.repository.ImportDefinitionRepository;
import pl.pekao.zesportal.repository.ImportFieldMappingRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class ImportDefinitionService {

    private final ImportDefinitionRepository definitionRepository;
    private final ImportFieldMappingRepository mappingRepository;
    private final pl.pekao.zesportal.service.ServerServiceManagementService serverServiceManagementService;
    private final DatabaseViewQueryService databaseViewQueryService;

    public ImportDefinitionService(ImportDefinitionRepository definitionRepository,
                                   ImportFieldMappingRepository mappingRepository,
                                   pl.pekao.zesportal.service.ServerServiceManagementService serverServiceManagementService,
                                   DatabaseViewQueryService databaseViewQueryService) {
        this.definitionRepository = definitionRepository;
        this.mappingRepository = mappingRepository;
        this.serverServiceManagementService = serverServiceManagementService;
        this.databaseViewQueryService = databaseViewQueryService;
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

    /**
     * Parsuje treść pliku tekstowego według definicji importu.
     * Dla TXT/CSV: dzieli po separatorze linii, potem po separatorze pól; mapowania określają kolumny (indeks) lub stałe.
     *
     * @param content   treść pliku (np. z uploadu)
     * @param definition definicja z mapowaniami (musi być załadowana z mappings)
     * @return lista rekordów – każdy to mapa: nazwa pola docelowego → wartość
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> parseFileContent(String content, ImportDefinition definition) {
        List<Map<String, Object>> records = new ArrayList<>();
        if (content == null || definition == null || definition.getMappings() == null || definition.getMappings().isEmpty()) {
            return records;
        }
        String normalized = content.replace("\r\n", "\n").replace("\r", "\n");
        String lineSep = unescapeSeparator(definition.getLineSeparator() != null ? definition.getLineSeparator() : "\n");
        String fieldSep = unescapeSeparator(definition.getFieldSeparator() != null ? definition.getFieldSeparator() : ",");
        Pattern linePattern = Pattern.compile(Pattern.quote(lineSep));
        Pattern fieldPattern = Pattern.compile(Pattern.quote(fieldSep));
        String[] lines = linePattern.split(normalized);
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = fieldPattern.split(line, -1);
            Map<String, Object> row = new LinkedHashMap<>();
            for (ImportFieldMapping m : definition.getMappings()) {
                String target = m.getTargetFieldName();
                if (target == null || target.isBlank()) continue;
                if (m.getSourceType() == SourceType.CONSTANT) {
                    row.put(target, m.getConstantValue() != null ? m.getConstantValue() : "");
                } else {
                    int idx = 0;
                    if (m.getSourceField() != null && !m.getSourceField().isBlank()) {
                        try {
                            int num = Integer.parseInt(m.getSourceField().trim());
                            idx = num >= 1 ? num - 1 : num;
                        } catch (NumberFormatException ignored) { }
                    }
                    String value = idx >= 0 && idx < parts.length ? parts[idx].trim() : "";
                    row.put(target, value);
                }
            }
            if (!row.isEmpty()) records.add(row);
        }
        return records;
    }

    private static String unescapeSeparator(String s) {
        if (s == null) return "\n";
        return s.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
    }

    /**
     * Pobiera dane z widoku bazy danych według definicji importu (źródło DATABASE).
     * Mapowania określają: pole docelowe Tuxedo ← kolumna widoku (DATABASE_FIELD) lub stała (CONSTANT).
     *
     * @param definition definicja z importSource=DATABASE, dbServerServiceId, viewName i mapowaniami
     * @return lista rekordów – każdy to mapa: nazwa pola docelowego → wartość (jak parseFileContent)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> loadFromDatabase(ImportDefinition definition) throws Exception {
        List<Map<String, Object>> records = new ArrayList<>();
        if (definition == null || definition.getImportSource() != ImportSource.DATABASE
                || definition.getDbServerServiceId() == null || definition.getViewName() == null
                || definition.getMappings() == null || definition.getMappings().isEmpty()) {
            return records;
        }
        ServerService dbComponent = serverServiceManagementService.findById(definition.getDbServerServiceId()).orElse(null);
        if (dbComponent == null) {
            return records;
        }
        List<Map<String, Object>> rows = databaseViewQueryService.queryView(dbComponent, definition.getViewName());
        for (Map<String, Object> row : rows) {
            Map<String, Object> record = new LinkedHashMap<>();
            for (ImportFieldMapping m : definition.getMappings()) {
                String target = m.getTargetFieldName();
                if (target == null || target.isBlank()) continue;
                if (m.getSourceType() == SourceType.CONSTANT) {
                    record.put(target, m.getConstantValue() != null ? m.getConstantValue() : "");
                } else if (m.getSourceType() == SourceType.DATABASE_FIELD) {
                    String col = m.getSourceField();
                    Object val = (col != null && !col.isBlank() && row.containsKey(col)) ? row.get(col) : null;
                    record.put(target, val != null ? val : "");
                }
            }
            if (!record.isEmpty()) records.add(record);
        }
        return records;
    }
}
