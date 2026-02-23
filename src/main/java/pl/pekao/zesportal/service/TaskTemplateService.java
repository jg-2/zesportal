package pl.pekao.zesportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pekao.zesportal.entity.TaskTemplate;
import pl.pekao.zesportal.entity.TaskTemplate.TaskTemplateType;
import pl.pekao.zesportal.repository.TaskTemplateRepository;

import java.util.List;
import java.util.Optional;

@Service
public class TaskTemplateService {

    private final TaskTemplateRepository repository;

    public TaskTemplateService(TaskTemplateRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TaskTemplate> findAll() {
        return repository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<TaskTemplate> findByType(TaskTemplateType type) {
        return repository.findByTypeOrderByNameAsc(type);
    }

    @Transactional(readOnly = true)
    public Optional<TaskTemplate> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public TaskTemplate save(TaskTemplate template) {
        return repository.save(template);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
