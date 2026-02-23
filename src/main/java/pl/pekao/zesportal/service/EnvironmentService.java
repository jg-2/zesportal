package pl.pekao.zesportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pekao.zesportal.entity.Environment;
import pl.pekao.zesportal.repository.EnvironmentRepository;

import java.util.List;
import java.util.Optional;

@Service
public class EnvironmentService {

    private final EnvironmentRepository repository;

    public EnvironmentService(EnvironmentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Environment> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Environment> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Environment> findByCode(String code) {
        return repository.findByCode(code);
    }

    @Transactional(readOnly = true)
    public List<Environment> findByStatus(Environment.EnvironmentStatus status) {
        return repository.findByStatus(status);
    }

    @Transactional
    public Environment save(Environment environment) {
        return repository.save(environment);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
