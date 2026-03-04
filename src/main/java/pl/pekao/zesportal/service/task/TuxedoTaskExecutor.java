package pl.pekao.zesportal.service.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.pekao.zesportal.config.TestJoltProperties;
import pl.pekao.zesportal.entity.ServerService;
import pl.pekao.zesportal.entity.Task;
import pl.pekao.zesportal.entity.TaskTemplate;
import pl.pekao.zesportal.entity.TaskTemplate.TaskTemplateType;
import pl.pekao.zesportal.model.tuxedo.TuxedoConnectionConfig;
import pl.pekao.zesportal.model.tuxedo.TuxedoInvocationEntry;
import pl.pekao.zesportal.model.tuxedo.TuxedoInvocationList;
import pl.pekao.zesportal.service.JtuxedoServiceConfigService;
import pl.pekao.zesportal.service.ServerServiceManagementService;
import pl.pekao.zesportal.service.TaskBatchConfigService;
import pl.pekao.zesportal.service.tuxedo.TuxedoJoltConnectionService;

/**
 * Wykonuje zadanie typu Tuxedo: nawiązanie połączenia JOLT (i w przyszłości wywołanie usługi)
 * według config zadania lub szablonu. Config JSON: serverServiceId, jtuxedoServiceId (opcjonalnie), parametersJson (opcjonalnie).
 */
@Component
public class TuxedoTaskExecutor implements TaskExecutor {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(TuxedoTaskExecutor.class);

    private final TaskProgressHolder progressHolder;
    private final ServerServiceManagementService serverServiceManagement;
    private final JtuxedoServiceConfigService jtuxedoServiceConfigService;
    private final TuxedoJoltConnectionService joltConnectionService;
    private final TestJoltProperties testJoltProperties;
    private final TaskBatchConfigService taskBatchConfigService;

    public TuxedoTaskExecutor(TaskProgressHolder progressHolder,
                              ServerServiceManagementService serverServiceManagement,
                              JtuxedoServiceConfigService jtuxedoServiceConfigService,
                              TuxedoJoltConnectionService joltConnectionService,
                              TestJoltProperties testJoltProperties,
                              TaskBatchConfigService taskBatchConfigService) {
        this.progressHolder = progressHolder;
        this.serverServiceManagement = serverServiceManagement;
        this.jtuxedoServiceConfigService = jtuxedoServiceConfigService;
        this.joltConnectionService = joltConnectionService;
        this.testJoltProperties = testJoltProperties;
        this.taskBatchConfigService = taskBatchConfigService;
    }

    @Override
    public TaskTemplateType getType() {
        return TaskTemplateType.TUXEDO;
    }

    @Override
    public TaskExecutionResult execute(Task task, TaskTemplate template) throws Exception {
        Instant startedAt = task.getStartedAt() != null ? task.getStartedAt() : Instant.now();
        String configJson = (task.getConfig() != null && !task.getConfig().isBlank())
                ? task.getConfig()
                : (template != null ? template.getConfig() : null);
        log.info("TuxedoTaskExecutor: wykonuję zadanie {} (config: {})", task.getName(), configJson);

        long taskId = task.getId();
        String taskName = task.getName() != null ? task.getName() : "Tuxedo task";

        progressHolder.setProgress(taskId, taskName, 0, 2);

        Long serverServiceId = null;
        if (configJson != null && !configJson.isBlank()) {
            try {
                JsonNode n = JSON.readTree(configJson);
                if (n.has("serverServiceId")) serverServiceId = n.get("serverServiceId").asLong();
            } catch (Exception e) {
                log.warn("Nie można odczytać serverServiceId z config: {}", configJson, e);
            }
        }
        final Long componentId = serverServiceId;
        if (componentId == null) {
            throw new IllegalStateException("Brak serverServiceId w konfiguracji zadania (config JSON).");
        }

        ServerService component = serverServiceManagement.findByIdWithServer(componentId)
                .orElseThrow(() -> new IllegalStateException("Nie znaleziono komponentu (ServerService) o id " + componentId));

        TuxedoInvocationList invocationList = buildInvocationList(component, configJson);
        List<TuxedoInvocationEntry> entries = invocationList.getEntries();
        int totalSteps = Math.max(1, entries.size());
        progressHolder.setProgress(taskId, taskName, 0, totalSteps);

        boolean saveResult = Boolean.TRUE.equals(task.getSaveResult());

        if (testJoltProperties.isEnabled()) {
            TuxedoJoltConnectionService.JoltConnectionResult result = joltConnectionService.connect(component);
            if (!result.isSuccess()) {
                throw new Exception("Połączenie JOLT: " + result.getMessage());
            }
            if (saveResult) {
                simulateResults(invocationList);
            }
            log.info("Tryb testowy JOLT: symulacja wywołania usług Tuxedo dla zadania [{}]", task.getName());
            Thread.sleep(2000);
        } else {
            for (int i = 0; i < entries.size(); i++) {
                progressHolder.setProgress(taskId, taskName, i, totalSteps);
                TuxedoInvocationEntry entry = entries.get(i);
                Map<String, Object> params = paramsFromJson(entry.getParametersJson());
                String serviceName = entry.getServiceName() != null ? entry.getServiceName() : "TOUPPER";
                try {
                    Object result = joltConnectionService.call(component, serviceName, params);
                    if (saveResult) {
                        applyResult(entry, result);
                    }
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String msg = cause != null ? cause.getMessage() : null;
                    if (msg != null && msg.contains("J_ESTCON")) {
                        throw new Exception("JOLT J_ESTCON – błąd nawiązania sesji. Jeśli serwer JOLT wymaga logowania, w komponencie odznacz „Autentykacja bez hasła” i ustaw hasło. Szczegóły: " + msg, e);
                    }
                    throw e;
                }
            }
        }
        progressHolder.setProgress(taskId, taskName, totalSteps, totalSteps);
        log.info("Zadanie Tuxedo [{}] zakończone", task.getName());

        List<Map<String, Object>> invocations = null;
        if (saveResult) {
            invocations = new ArrayList<>(entries.size());
            for (TuxedoInvocationEntry e : invocationList.getEntries()) {
                Map<String, Object> map = new LinkedHashMap<>(4);
                map.put("serviceName", e.getServiceName());
                map.put("inputParams", e.getParametersJson());
                map.put("output", e.getResultJson());
                map.put("status", e.getStatus() != null ? e.getStatus().name() : null);
                if (e.getMessage() != null) map.put("message", e.getMessage());
                invocations.add(map);
            }
        }
        return new TaskExecutionResult(true, "Zadanie wykonane pomyślnie",
                TaskTemplateType.TUXEDO, startedAt, Instant.now(), invocations);
    }

    /**
     * Buduje listę wywołań z konfiguracji zadania. Obsługuje:
     * - parametersListFile – nazwa pliku z listą (duży batch z „Wczytanie z pliku”),
     * - parametersList (tablica) – jeden wpis na każdy element,
     * - parametersJson / parameters – pojedyncze wywołanie.
     */
    private TuxedoInvocationList buildInvocationList(ServerService component, String configJson) {
        TuxedoInvocationList list = new TuxedoInvocationList(TuxedoConnectionConfig.fromServerService(component));
        final String[] serviceNameRef = new String[]{"TOUPPER"};
        if (configJson != null && !configJson.isBlank()) {
            try {
                JsonNode n = JSON.readTree(configJson);
                if (n.has("jtuxedoServiceId")) {
                    Long jtuxedoServiceId = n.get("jtuxedoServiceId").asLong();
                    jtuxedoServiceConfigService.findServiceById(jtuxedoServiceId)
                            .ifPresent(svc -> serviceNameRef[0] = svc.getName());
                }
                if (n.has("parametersListFile") && n.get("parametersListFile").isTextual()) {
                    String filename = n.get("parametersListFile").asText();
                    List<Map<String, Object>> paramsList = taskBatchConfigService.loadParametersList(filename);
                    for (Map<String, Object> params : paramsList) {
                        String paramsJson = params == null || params.isEmpty() ? "{}" : JSON.writeValueAsString(params);
                        list.addEntry(serviceNameRef[0], paramsJson);
                    }
                } else if (n.has("parametersList") && n.get("parametersList").isArray()) {
                    for (JsonNode item : n.get("parametersList")) {
                        String paramsJson = item.isTextual() ? item.asText() : JSON.writeValueAsString(item);
                        list.addEntry(serviceNameRef[0], paramsJson != null && !paramsJson.isBlank() ? paramsJson : "{}");
                    }
                } else {
                    String parametersJson = "{}";
                    if (n.has("parametersJson") && n.get("parametersJson").isTextual()) {
                        parametersJson = n.get("parametersJson").asText();
                    } else if (n.has("parameters") && n.get("parameters").isObject()) {
                        parametersJson = JSON.writeValueAsString(n.get("parameters"));
                    }
                    list.addEntry(serviceNameRef[0], parametersJson);
                }
            } catch (Exception e) {
                log.debug("Parsowanie config JSON: {}", e.getMessage());
            }
        }
        if (list.getEntries().isEmpty()) {
            list.addEntry(serviceNameRef[0], "{}");
        }
        return list;
    }

    /**
     * Symulacja rezultatu wywołania: dla każdego wpisu ustawia resultJson (powtórzenie parametrów + RESULT:OK) oraz status OK.
     */
    private void simulateResults(TuxedoInvocationList invocationList) {
        for (TuxedoInvocationEntry entry : invocationList.getEntries()) {
            String resultJson = "{\"RESULT\":\"OK\"}";
            String params = entry.getParametersJson();
            if (params != null && !params.isBlank()) {
                try {
                    JsonNode paramsNode = JSON.readTree(params);
                    ObjectNode resultNode = paramsNode.isObject() ? (ObjectNode) paramsNode.deepCopy() : JSON.createObjectNode();
                    resultNode.put("RESULT", "OK");
                    resultJson = JSON.writeValueAsString(resultNode);
                } catch (Exception e) {
                    ObjectNode fallback = JSON.createObjectNode();
                    fallback.put("RESULT", "OK");
                    try {
                        resultJson = JSON.writeValueAsString(fallback);
                    } catch (JsonProcessingException ex) {
                        resultJson = "{\"RESULT\":\"OK\"}";
                    }
                }
            }
            entry.setResultJson(resultJson);
            entry.setStatus(TuxedoInvocationEntry.InvocationStatus.OK);
        }
    }

    /** Parametry do DataSet z JSON (jak ds.setValue w przykładzie). */
    private Map<String, Object> paramsFromJson(String parametersJson) {
        if (parametersJson == null || parametersJson.isBlank()) {
            return new java.util.HashMap<>();
        }
        try {
            return JSON.readValue(parametersJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Nie można sparsować parametersJson: {}", parametersJson, e);
            return new java.util.HashMap<>();
        }
    }

    /** Ustawia resultJson (pełny recordset z JOLT) i status na podstawie bea.jolt.pool.Result (getApplicationCode()). */
    private void applyResult(TuxedoInvocationEntry entry, Object result) {
        if (result == null) {
            entry.setStatus(TuxedoInvocationEntry.InvocationStatus.ERROR);
            entry.setMessage("Brak wyniku");
            entry.setResultJson("{}");
            return;
        }
        String fullRecordsetJson = joltConnectionService.recordsetToJson(result);
        if (fullRecordsetJson != null && !fullRecordsetJson.isBlank()) {
            entry.setResultJson(fullRecordsetJson);
        }
        try {
            Object appCodeObj = result.getClass().getMethod("getApplicationCode").invoke(result);
            int appCode = appCodeObj instanceof Number ? ((Number) appCodeObj).intValue() : 0;
            if (appCode == 0) {
                entry.setStatus(TuxedoInvocationEntry.InvocationStatus.OK);
            } else {
                entry.setStatus(TuxedoInvocationEntry.InvocationStatus.ERROR);
                entry.setMessage("applicationCode=" + appCode);
            }
        } catch (Exception e) {
            entry.setStatus(TuxedoInvocationEntry.InvocationStatus.ERROR);
            entry.setMessage(e.getMessage());
            if (fullRecordsetJson == null || fullRecordsetJson.isBlank()) {
                try {
                    entry.setResultJson("{\"error\":\"" + (e.getMessage() != null ? e.getMessage().replace("\"", "'") : "") + "\"}");
                } catch (Exception ignored) { }
            }
        }
    }
}
