package pl.pekao.zesportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pekao.zesportal.entity.JtuxedoService;
import pl.pekao.zesportal.entity.JtuxedoServiceField;
import pl.pekao.zesportal.repository.JtuxedoServiceFieldRepository;
import pl.pekao.zesportal.repository.JtuxedoServiceRepository;
import pl.pekao.zesportal.service.dto.ParsedServiceDefinition;
import pl.pekao.zesportal.service.dto.ServiceImportDiff;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JtuxedoServiceConfigService {

    private final JtuxedoServiceRepository serviceRepository;
    private final JtuxedoServiceFieldRepository fieldRepository;

    public JtuxedoServiceConfigService(JtuxedoServiceRepository serviceRepository,
                                       JtuxedoServiceFieldRepository fieldRepository) {
        this.serviceRepository = serviceRepository;
        this.fieldRepository = fieldRepository;
    }

    @Transactional(readOnly = true)
    public List<JtuxedoService> findAllServices() {
        return serviceRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<JtuxedoService> findServiceById(Long id) {
        return serviceRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<JtuxedoService> findServiceByName(String name) {
        return name != null && !name.isBlank() ? serviceRepository.findByName(name.trim()) : Optional.empty();
    }

    /**
     * Oblicza różnice między istniejącą usługą a definicją z pliku XML (do podglądu przed nadpisaniem).
     */
    @Transactional(readOnly = true)
    public Optional<ServiceImportDiff> getImportDiff(String existingServiceName, ParsedServiceDefinition parsed) {
        if (existingServiceName == null || parsed == null) return Optional.empty();
        Optional<JtuxedoService> existingOpt = findServiceByName(existingServiceName);
        if (existingOpt.isEmpty()) return Optional.empty();
        JtuxedoService existing = existingOpt.get();
        List<JtuxedoServiceField> existingFields = findFieldsByServiceId(existing.getId());
        String descOld = existing.getDescription() != null ? existing.getDescription() : "";
        String descNew = parsed.getDescription() != null ? parsed.getDescription() : "";
        boolean descriptionChanged = !descOld.trim().equals(descNew.trim());

        Map<String, JtuxedoServiceField> existingByName = existingFields.stream()
                .filter(f -> f.getName() != null && !f.getName().isBlank())
                .collect(Collectors.toMap(JtuxedoServiceField::getName, f -> f, (a, b) -> a));
        Map<String, ParsedServiceDefinition.ParsedFieldDefinition> parsedByName = parsed.getInputFields().stream()
                .filter(f -> f.getName() != null && !f.getName().isBlank())
                .collect(Collectors.toMap(ParsedServiceDefinition.ParsedFieldDefinition::getName, f -> f, (a, b) -> a));

        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<String> modified = new ArrayList<>();

        for (String name : parsedByName.keySet()) {
            if (!existingByName.containsKey(name)) added.add(name);
        }
        for (String name : existingByName.keySet()) {
            if (!parsedByName.containsKey(name)) removed.add(name);
        }
        for (String name : parsedByName.keySet()) {
            JtuxedoServiceField ef = existingByName.get(name);
            ParsedServiceDefinition.ParsedFieldDefinition pf = parsedByName.get(name);
            if (ef != null && pf != null) {
                JtuxedoServiceField.FieldDataType newType = XmlServiceDefinitionParser.mapDataType(pf.getDataType());
                JtuxedoServiceField.FieldCardinality newCard = XmlServiceDefinitionParser.mapCardinality(pf.getCardinality());
                boolean typeDiff = ef.getDataType() != newType;
                boolean cardDiff = ef.getCardinality() != newCard;
                if (typeDiff || cardDiff) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(name).append(": ");
                    if (typeDiff) sb.append("typ ").append(ef.getDataType() != null ? ef.getDataType().getDisplayName() : "—")
                            .append(" → ").append(newType.getDisplayName());
                    if (typeDiff && cardDiff) sb.append(", ");
                    if (cardDiff) sb.append("liczeń ").append(ef.getCardinality() != null ? ef.getCardinality().getDisplayName() : "—")
                            .append(" → ").append(newCard.getDisplayName());
                    modified.add(sb.toString());
                }
            }
        }

        return Optional.of(new ServiceImportDiff(descriptionChanged, descOld, descNew, added, removed, modified));
    }

    /**
     * Importuje definicję usługi z zparsowanego XML. Jeśli usługa o tej nazwie istnieje i overwrite=true,
     * nadpisuje ją (wraz z polami). W przeciwnym razie tworzy nową usługę.
     */
    @Transactional
    public JtuxedoService importFromParsed(ParsedServiceDefinition parsed, boolean overwrite) {
        Optional<JtuxedoService> existing = findServiceByName(parsed.getName());
        JtuxedoService service;
        if (existing.isPresent() && overwrite) {
            service = existing.get();
            service.setDescription(parsed.getDescription());
            serviceRepository.save(service);
            fieldRepository.deleteByJtuxedoServiceId(service.getId());
        } else if (existing.isPresent()) {
            return existing.get();
        } else {
            service = new JtuxedoService();
            service.setName(parsed.getName());
            service.setDescription(parsed.getDescription());
            service = serviceRepository.save(service);
        }
        int order = 0;
        for (ParsedServiceDefinition.ParsedFieldDefinition pf : parsed.getInputFields()) {
            JtuxedoServiceField field = new JtuxedoServiceField();
            field.setJtuxedoService(service);
            field.setName(pf.getName());
            field.setDataType(XmlServiceDefinitionParser.mapDataType(pf.getDataType()));
            field.setCardinality(XmlServiceDefinitionParser.mapCardinality(pf.getCardinality()));
            field.setSortOrder(order++);
            fieldRepository.save(field);
        }
        return service;
    }

    @Transactional
    public JtuxedoService saveService(JtuxedoService service) {
        return serviceRepository.save(service);
    }

    @Transactional
    public void deleteServiceById(Long id) {
        fieldRepository.deleteByJtuxedoServiceId(id);
        serviceRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<JtuxedoServiceField> findFieldsByServiceId(Long serviceId) {
        return fieldRepository.findByJtuxedoServiceIdOrderBySortOrderAscNameAsc(serviceId);
    }

    @Transactional
    public JtuxedoServiceField saveField(JtuxedoServiceField field) {
        return fieldRepository.save(field);
    }

    @Transactional
    public void deleteFieldById(Long id) {
        fieldRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<JtuxedoServiceField> findFieldById(Long id) {
        return fieldRepository.findById(id);
    }
}
