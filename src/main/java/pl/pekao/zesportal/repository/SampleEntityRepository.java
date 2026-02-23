package pl.pekao.zesportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pekao.zesportal.entity.SampleEntity;

public interface SampleEntityRepository extends JpaRepository<SampleEntity, Long> {
}
