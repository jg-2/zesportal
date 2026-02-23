package pl.pekao.zesportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pekao.zesportal.entity.TaskTemplate;
import pl.pekao.zesportal.entity.TaskTemplate.TaskTemplateType;

import java.util.List;

public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, Long> {

    List<TaskTemplate> findAllByOrderByNameAsc();

    List<TaskTemplate> findByTypeOrderByNameAsc(TaskTemplateType type);
}
