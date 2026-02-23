package pl.pekao.zesportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pekao.zesportal.entity.SshKey;

import java.util.List;

public interface SshKeyRepository extends JpaRepository<SshKey, Long> {

    List<SshKey> findAllByOrderByNameAsc();
}
