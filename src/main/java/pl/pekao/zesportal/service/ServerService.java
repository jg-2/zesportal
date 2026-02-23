package pl.pekao.zesportal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pekao.zesportal.entity.Server;
import pl.pekao.zesportal.repository.ServerRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ServerService {

    private final ServerRepository repository;

    public ServerService(ServerRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Server> findAll() {
        return repository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Server> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Server save(Server server) {
        return repository.save(server);
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
