package pl.pekao.zesportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pekao.zesportal.entity.Server;

import java.util.List;

public interface ServerRepository extends JpaRepository<Server, Long> {

    List<Server> findAllByOrderByNameAsc();
}
