package pl.pekao.zesportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pekao.zesportal.entity.JtuxedoService;

import java.util.List;

public interface JtuxedoServiceRepository extends JpaRepository<JtuxedoService, Long> {

    List<JtuxedoService> findAllByOrderByNameAsc();
}
