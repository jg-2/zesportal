package pl.pekao.zesportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pekao.zesportal.entity.JtuxedoService;
import pl.pekao.zesportal.entity.JtuxedoServiceField;
import pl.pekao.zesportal.repository.JtuxedoServiceFieldRepository;
import pl.pekao.zesportal.repository.JtuxedoServiceRepository;

import java.util.List;
import java.util.Optional;

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
