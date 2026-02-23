package pl.pekao.zesportal.service.task;

import org.springframework.stereotype.Component;
import pl.pekao.zesportal.entity.TaskTemplate.TaskTemplateType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TaskExecutorRegistry {

    private final Map<TaskTemplateType, TaskExecutor> byType;

    public TaskExecutorRegistry(List<TaskExecutor> executors) {
        this.byType = executors.stream()
                .collect(Collectors.toMap(TaskExecutor::getType, Function.identity()));
    }

    public Optional<TaskExecutor> getExecutor(TaskTemplateType type) {
        return Optional.ofNullable(byType.get(type));
    }
}
