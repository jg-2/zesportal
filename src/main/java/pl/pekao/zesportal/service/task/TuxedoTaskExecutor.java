package pl.pekao.zesportal.service.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;
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

    public TuxedoTaskExecutor(TaskProgressHolder progressHolder,
                              ServerServiceManagementService serverServiceManagement,
                              JtuxedoServiceConfigService jtuxedoServiceConfigService,
                              TuxedoJoltConnectionService joltConnectionService,
                              TestJoltProperties testJoltProperties) {
        this.progressHolder = progressHolder;
        this.serverServiceManagement = serverServiceManagement;
        this.jtuxedoServiceConfigService = jtuxedoServiceConfigService;
        this.joltConnectionService = joltConnectionService;
        this.testJoltProperties = testJoltProperties;
    }

    @Override
    public TaskTemplateType getType() {
        return TaskTemplateType.TUXEDO;
    }

    @Override
    public void execute(Task task, TaskTemplate template) throws Exception {
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

        progressHolder.setProgress(taskId, taskName, 1, 2);
        TuxedoJoltConnectionService.JoltConnectionResult result = joltConnectionService.connect(component);
        if (!result.isSuccess()) {
            throw new Exception("Połączenie JOLT: " + result.getMessage());
        }
        if (testJoltProperties.isEnabled()) {
            TuxedoInvocationList invocationList = buildInvocationList(component, configJson);
            simulateResults(invocationList);
            log.info("Tryb testowy JOLT: symulacja wywołania usług Tuxedo dla zadania [{}]\n{}", task.getName(), invocationList.formatToString());
            Thread.sleep(2000);
        }
        progressHolder.setProgress(taskId, taskName, 2, 2);
        log.info("Zadanie Tuxedo [{}] zakończone – połączenie nawiązane", task.getName());
    }

    /**
     * Buduje listę wywołań z konfiguracji zadania (serverServiceId → connection, jtuxedoServiceId → nazwa usługi, opcjonalnie parametersJson).
     */
    private TuxedoInvocationList buildInvocationList(ServerService component, String configJson) {
        TuxedoInvocationList list = new TuxedoInvocationList(TuxedoConnectionConfig.fromServerService(component));
        final String[] serviceNameRef = new String[]{"TOUPPER"};
        String parametersJson = "{}";
        if (configJson != null && !configJson.isBlank()) {
            try {
                JsonNode n = JSON.readTree(configJson);
                if (n.has("jtuxedoServiceId")) {
                    Long jtuxedoServiceId = n.get("jtuxedoServiceId").asLong();
                    jtuxedoServiceConfigService.findServiceById(jtuxedoServiceId)
                            .ifPresent(svc -> serviceNameRef[0] = svc.getName());
                }
                if (n.has("parametersJson") && n.get("parametersJson").isTextual()) {
                    parametersJson = n.get("parametersJson").asText();
                } else if (n.has("parameters") && n.get("parameters").isObject()) {
                    parametersJson = JSON.writeValueAsString(n.get("parameters"));
                }
            } catch (Exception e) {
                log.debug("Parsowanie config JSON (jtuxedoServiceId/parameters): {}", e.getMessage());
            }
        }
        list.addEntry(serviceNameRef[0], parametersJson);
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
}
