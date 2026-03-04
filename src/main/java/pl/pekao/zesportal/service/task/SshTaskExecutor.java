package pl.pekao.zesportal.service.task;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.pekao.zesportal.entity.Task;
import pl.pekao.zesportal.entity.TaskTemplate;
import pl.pekao.zesportal.entity.TaskTemplate.TaskTemplateType;

/**
 * Wykonuje zadanie typu SSH: wykonanie polecenia przez SSH
 * zgodnie z konfiguracją szablonu (host, command, user w config JSON).
 */
@Component
public class SshTaskExecutor implements TaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(SshTaskExecutor.class);

    @Override
    public TaskTemplateType getType() {
        return TaskTemplateType.SSH;
    }

    @Override
    public TaskExecutionResult execute(Task task, TaskTemplate template) throws Exception {
        log.info("SshTaskExecutor: wykonuję zadanie {} (szablon: {}, config: {})",
                task.getName(), template != null ? template.getName() : null, template != null ? template.getConfig() : null);
        Instant startedAt = task.getStartedAt() != null ? task.getStartedAt() : Instant.now();
        // TODO: odczyt config JSON (host, command, user), wywołanie polecenia SSH
        Thread.sleep(500);
        return new TaskExecutionResult(true, "Zadanie wykonane pomyślnie",
                TaskTemplateType.SSH, startedAt, Instant.now(), null);
    }
}
