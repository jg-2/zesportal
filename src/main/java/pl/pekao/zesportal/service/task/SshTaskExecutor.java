package pl.pekao.zesportal.service.task;

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
    public void execute(Task task, TaskTemplate template) throws Exception {
        log.info("SshTaskExecutor: wykonuję zadanie {} (szablon: {}, config: {})",
                task.getName(), template.getName(), template.getConfig());
        // TODO: odczyt config JSON (host, command, user), wywołanie polecenia SSH
        // Na razie symulacja
        Thread.sleep(500);
    }
}
