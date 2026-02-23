package pl.pekao.zesportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pekao.zesportal.entity.JtuxedoServiceField;

import java.util.List;

public interface JtuxedoServiceFieldRepository extends JpaRepository<JtuxedoServiceField, Long> {

    List<JtuxedoServiceField> findByJtuxedoServiceIdOrderBySortOrderAscNameAsc(Long jtuxedoServiceId);

    void deleteByJtuxedoServiceId(Long jtuxedoServiceId);
}
