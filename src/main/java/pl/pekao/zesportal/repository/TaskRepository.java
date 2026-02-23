package pl.pekao.zesportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.pekao.zesportal.entity.Task;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.taskTemplate ORDER BY t.createdAt DESC")
    List<Task> findAllWithTemplate();

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.taskTemplate WHERE t.id = :id")
    Optional<Task> findByIdWithTemplate(Long id);

    List<Task> findByStatus(Task.TaskStatus status);

    List<Task> findByPriority(Task.TaskPriority priority);

    List<Task> findByStatusOrderByCreatedAtAsc(Task.TaskStatus status);
}
