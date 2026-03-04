package pl.pekao.zesportal.service.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.pekao.zesportal.entity.Task;
import pl.pekao.zesportal.entity.TaskTemplate;
import pl.pekao.zesportal.entity.TaskTemplate.TaskTemplateType;
import pl.pekao.zesportal.repository.TaskRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Wykonuje zadanie typu SAVE_FILE: zapisuje wynik wskazanego zadania (sourceTaskId) do pliku.
 * Config JSON: {"sourceTaskId": &lt;id&gt;} lub {"sourceTaskId": &lt;id&gt;, "filePath": "opcjonalna/ścieżka"}.
 */
@Component
public class SaveFileTaskExecutor implements TaskExecutor {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(SaveFileTaskExecutor.class);
    private static final Pattern SAFE_FILENAME = Pattern.compile("[^a-zA-Z0-9._-]");

    private final TaskRepository taskRepository;

    @Value("${task.save-file.output-dir:./data/task-results}")
    private String outputDir;

    public SaveFileTaskExecutor(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public TaskTemplateType getType() {
        return TaskTemplateType.SAVE_FILE;
    }

    @Override
    public TaskExecutionResult execute(Task task, TaskTemplate template) throws Exception {
        Instant startedAt = Instant.now();
        String configJson = task.getConfig();
        if (configJson == null || configJson.isBlank()) {
            throw new IllegalArgumentException("Brak config (wymagane sourceTaskId)");
        }
        JsonNode node = JSON.readTree(configJson);
        if (!node.has("sourceTaskId")) {
            throw new IllegalArgumentException("Config musi zawierać sourceTaskId");
        }
        long sourceTaskId = node.get("sourceTaskId").asLong();
        String customPath = node.has("filePath") && !node.get("filePath").isNull()
                ? node.get("filePath").asText()
                : null;

        Optional<Task> sourceOpt = taskRepository.findById(sourceTaskId);
        if (sourceOpt.isEmpty()) {
            throw new IllegalArgumentException("Nie znaleziono zadania źródłowego o id: " + sourceTaskId);
        }
        Task source = sourceOpt.get();

        String content = buildContent(source);
        Path outPath = resolveOutputPath(source, customPath);
        Files.createDirectories(outPath.getParent());
        Files.writeString(outPath, content, StandardCharsets.UTF_8);

        log.info("SaveFileTaskExecutor: zapisano wynik zadania {} do {}", sourceTaskId, outPath);
        String message = "Zapisano do " + outPath.toAbsolutePath().normalize();
        return new TaskExecutionResult(true, message, TaskTemplateType.SAVE_FILE, startedAt, Instant.now(), null);
    }

    private String buildContent(Task source) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Zadanie: ").append(source.getName()).append(" ===\n");
        sb.append("ID: ").append(source.getId()).append("\n");
        sb.append("Typ: ").append(source.getType() != null ? source.getType().getDisplayName() : "—").append("\n");
        sb.append("Status: ").append(source.getStatus().getDisplayName()).append("\n");
        sb.append("Priorytet: ").append(source.getPriority().getDisplayName()).append("\n");
        sb.append("Utworzono: ").append(format(source.getCreatedAt())).append("\n");
        sb.append("Start: ").append(format(source.getStartedAt())).append("\n");
        sb.append("Zakończono: ").append(format(source.getCompletedAt())).append("\n");
        if (source.getDescription() != null && !source.getDescription().isBlank()) {
            sb.append("Opis: ").append(source.getDescription()).append("\n");
        }
        if (source.getConfig() != null && !source.getConfig().isBlank()) {
            sb.append("Config: ").append(source.getConfig()).append("\n");
        }
        sb.append("\n--- Rezultat ---\n");
        sb.append(source.getResult() != null ? source.getResult() : "(brak)");
        return sb.toString();
    }

    private static String format(Instant instant) {
        if (instant == null) return "—";
        return instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private Path resolveOutputPath(Task source, String customPath) throws IOException {
        Path base = Path.of(outputDir).toAbsolutePath().normalize();
        if (customPath != null && !customPath.isBlank()) {
            Path resolved = base.resolve(customPath).normalize();
            if (!resolved.startsWith(base)) {
                throw new IllegalArgumentException("filePath nie może wychodzić poza katalog wyników");
            }
            return resolved;
        }
        String safeName = SAFE_FILENAME.matcher(source.getName() != null ? source.getName() : "task").replaceAll("_");
        String fileName = "task-" + source.getId() + "-" + safeName + ".txt";
        if (fileName.length() > 200) fileName = "task-" + source.getId() + ".txt";
        return base.resolve(fileName);
    }
}
