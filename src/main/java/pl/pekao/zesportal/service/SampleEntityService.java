package pl.pekao.zesportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pekao.zesportal.entity.SampleEntity;
import pl.pekao.zesportal.repository.SampleEntityRepository;

import java.util.List;
import java.util.Optional;

@Service
public class SampleEntityService {

    private final SampleEntityRepository repository;

    public SampleEntityService(SampleEntityRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SampleEntity> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<SampleEntity> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public SampleEntity save(SampleEntity entity) {
        return repository.save(entity);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
