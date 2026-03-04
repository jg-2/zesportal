package pl.pekao.zesportal.service.task;

import pl.pekao.zesportal.entity.Task;
import pl.pekao.zesportal.entity.TaskTemplate;
import pl.pekao.zesportal.entity.TaskTemplate.TaskTemplateType;

/**
 * Wykonuje zadanie według konfiguracji szablonu.
 * Dla każdego typu szablonu (TUXEDO, SSH, ...) istnieje odpowiednia implementacja.
 */
public interface TaskExecutor {

    TaskTemplateType getType();

    /**
     * Wykonuje zadanie. Konfiguracja w template.getConfig() (JSON).
     * Zwraca wynik (krótki komunikat + opcjonalnie pełne dane do zapisu JSON).
     * W razie błędu rzuca wyjątek – obsługa statusu FAILED jest w TaskService.
     */
    TaskExecutionResult execute(Task task, TaskTemplate template) throws Exception;
}
