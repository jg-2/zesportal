package pl.pekao.zesportal.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Zapis i odczyt dużej listy parametrów (parametersList) do pliku, gdy nie mieści się w kolumnie task.config.
 */
@Service
public class TaskBatchConfigService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAP = new TypeReference<>() {};

    @Value("${task.batch-data-dir:./data/batch}")
    private String batchDataDir;

    /**
     * Zapisuje listę parametrów do pliku JSON. Nazwa pliku: batch-{uuid}.json.
     *
     * @param parametersList lista map (każda mapa = jeden rekord do wywołania)
     * @return nazwa pliku (np. batch-xxx.json) do wpisania w config jako parametersListFile
     */
    public String saveParametersList(List<Map<String, Object>> parametersList) throws Exception {
        if (parametersList == null || parametersList.isEmpty()) {
            throw new IllegalArgumentException("parametersList nie może być puste");
        }
        Path dir = Path.of(batchDataDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String filename = "batch-" + UUID.randomUUID() + ".json";
        Path file = dir.resolve(filename).normalize();
        if (!file.startsWith(dir)) {
            throw new IllegalArgumentException("Nieprawidłowa ścieżka batch");
        }
        String json = JSON.writeValueAsString(parametersList);
        Files.writeString(file, json, StandardCharsets.UTF_8);
        return filename;
    }

    /**
     * Wczytuje listę parametrów z pliku zapisanego przez saveParametersList.
     *
     * @param filename nazwa pliku (zwrócona przez saveParametersList)
     * @return lista map
     */
    public List<Map<String, Object>> loadParametersList(String filename) throws Exception {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Brak nazwy pliku batch");
        }
        Path dir = Path.of(batchDataDir).toAbsolutePath().normalize();
        Path file = dir.resolve(filename).normalize();
        if (!file.startsWith(dir)) {
            throw new IllegalArgumentException("Nieprawidłowa ścieżka batch: " + filename);
        }
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Plik batch nie istnieje: " + filename);
        }
        String json = Files.readString(file, StandardCharsets.UTF_8);
        return JSON.readValue(json, LIST_OF_MAP);
    }
}
