package pl.pekao.zesportal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Zapis pełnego wyniku zadania do pliku, gdy nie mieści się w kolumnie task.result (VARCHAR 32000).
 */
@Service
public class TaskResultFileService {

    @Value("${task.result-file-dir:./data/task-results-json}")
    private String resultFileDir;

    /**
     * Zapisuje pełny wynik (JSON) do pliku. Nazwa: task-result-{taskId}.json
     *
     * @param taskId       ID zadania
     * @param fullResultJson pełny JSON wyniku (np. z invocations)
     * @return nazwa pliku (np. task-result-123.json)
     */
    public String saveLargeResult(long taskId, String fullResultJson) throws Exception {
        if (fullResultJson == null || fullResultJson.isBlank()) {
            throw new IllegalArgumentException("Pełny wynik nie może być pusty");
        }
        Path dir = Path.of(resultFileDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String filename = "task-result-" + taskId + ".json";
        Path file = dir.resolve(filename).normalize();
        if (!file.startsWith(dir)) {
            throw new IllegalArgumentException("Nieprawidłowa ścieżka result file");
        }
        Files.writeString(file, fullResultJson, StandardCharsets.UTF_8);
        return filename;
    }
}
