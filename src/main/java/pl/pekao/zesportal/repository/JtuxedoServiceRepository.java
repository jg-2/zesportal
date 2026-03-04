package pl.pekao.zesportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pekao.zesportal.entity.JtuxedoService;

import java.util.List;
import java.util.Optional;

public interface JtuxedoServiceRepository extends JpaRepository<JtuxedoService, Long> {

    List<JtuxedoService> findAllByOrderByNameAsc();

    Optional<JtuxedoService> findByName(String name);
}
