package pl.pekao.zesportal.service.task;

import pl.pekao.zesportal.entity.TaskTemplate.TaskTemplateType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Wynik wykonania zadania. Krótki komunikat zawsze; pełne dane (do JSON) tylko gdy saveResult=true.
 */
public class TaskExecutionResult {

    private final boolean success;
    private final String shortMessage;
    private final TaskTemplateType taskType;
    private final Instant startedAt;
    private final Instant completedAt;
    /** Dla każdego wywołania: parametry wejściowe i wyjściowe (np. lista wywołań Tuxedo). */
    private final List<Map<String, Object>> invocations;

    public TaskExecutionResult(boolean success, String shortMessage,
                               TaskTemplateType taskType, Instant startedAt, Instant completedAt,
                               List<Map<String, Object>> invocations) {
        this.success = success;
        this.shortMessage = shortMessage;
        this.taskType = taskType;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.invocations = invocations;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getShortMessage() {
        return shortMessage;
    }

    public TaskTemplateType getTaskType() {
        return taskType;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public List<Map<String, Object>> getInvocations() {
        return invocations;
    }
}
