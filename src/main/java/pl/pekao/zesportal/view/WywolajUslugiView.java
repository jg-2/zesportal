package pl.pekao.zesportal.view;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import pl.pekao.zesportal.entity.Environment;
import pl.pekao.zesportal.entity.JtuxedoService;
import pl.pekao.zesportal.entity.JtuxedoServiceField;
import pl.pekao.zesportal.entity.ServerService;
import pl.pekao.zesportal.entity.ServerService.ServiceType;
import pl.pekao.zesportal.entity.TaskTemplate;
import pl.pekao.zesportal.entity.TaskTemplate.TaskTemplateType;
import pl.pekao.zesportal.service.EnvironmentService;
import pl.pekao.zesportal.service.JtuxedoServiceConfigService;
import pl.pekao.zesportal.service.ServerServiceManagementService;
import pl.pekao.zesportal.service.TaskService;
import pl.pekao.zesportal.service.TaskTemplateService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Route(value = "jtuxed0/wywolaj-uslugi", layout = MainLayout.class)
@PageTitle("Wywołanie usług | Zesportal")
public class WywolajUslugiView extends VerticalLayout {

    private final EnvironmentService environmentService;
    private final ServerServiceManagementService serverServiceManagement;
    private final JtuxedoServiceConfigService jtuxedoServiceConfigService;
    private final TaskService taskService;
    private final TaskTemplateService taskTemplateService;
    private final Select<Environment> environmentSelect = new Select<>();
    private final Select<ServerService> componentSelect = new Select<>();
    private final Select<JtuxedoService> tuxedoServiceSelect = new Select<>();
    private final VerticalLayout fieldsPanel = new VerticalLayout();
    private final Map<String, TextField> fieldInputsByName = new LinkedHashMap<>();
    private final Paragraph resultArea = new Paragraph();
    private static final ObjectMapper JSON = new ObjectMapper();

    public WywolajUslugiView(EnvironmentService environmentService,
                             ServerServiceManagementService serverServiceManagement,
                             JtuxedoServiceConfigService jtuxedoServiceConfigService,
                             TaskService taskService,
                             TaskTemplateService taskTemplateService) {
        this.environmentService = environmentService;
        this.serverServiceManagement = serverServiceManagement;
        this.jtuxedoServiceConfigService = jtuxedoServiceConfigService;
        this.taskService = taskService;
        this.taskTemplateService = taskTemplateService;

        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Wywołanie usług");
        environmentSelect.setLabel("Środowisko");
        environmentSelect.setItems(environmentService.findAll());
        environmentSelect.setItemLabelGenerator(e -> e.getName() + " (" + e.getCode() + ")");
        environmentSelect.setWidth("320px");
        environmentSelect.setPlaceholder("Wybierz środowisko");
        environmentSelect.addValueChangeListener(e -> {
            List<ServerService> components = e.getValue() != null
                    ? serverServiceManagement.findTuxedoServicesByEnvironmentId(e.getValue().getId())
                    : List.of();
            componentSelect.setItems(components);
            ServerService firstComponent = components.isEmpty() ? null : components.get(0);
            componentSelect.setValue(firstComponent);
            updateTuxedoServiceList(firstComponent);
            resultArea.setText("");
        });

        componentSelect.setLabel("Komponent");
        componentSelect.setItems(List.of());
        componentSelect.setItemLabelGenerator(ss -> ss.getName() + (ss.getServer() != null ? " @ " + ss.getServer().getName() : ""));
        componentSelect.setWidth("320px");
        componentSelect.setPlaceholder("Wybierz komponent (najpierw środowisko)");
        componentSelect.addValueChangeListener(e -> {
            updateTuxedoServiceList(e.getValue());
            resultArea.setText("");
        });

        tuxedoServiceSelect.setLabel("Usługa");
        tuxedoServiceSelect.setItems(List.of());
        tuxedoServiceSelect.setItemLabelGenerator(js -> js.getName() + (js.getDescription() != null && !js.getDescription().isBlank() ? " — " + js.getDescription() : ""));
        tuxedoServiceSelect.setWidth("320px");
        tuxedoServiceSelect.setPlaceholder("Wybierz usługę Tuxedo (najpierw komponent)");
        tuxedoServiceSelect.setVisible(false);
        tuxedoServiceSelect.addValueChangeListener(e -> {
            updateFieldsPanel(e.getValue());
            resultArea.setText("");
        });

        fieldsPanel.setPadding(false);
        fieldsPanel.setSpacing(true);
        fieldsPanel.setVisible(false);
        fieldsPanel.setWidthFull();

        Button callButton = new Button("Wywołaj", e -> callService());
        callButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        resultArea.getStyle()
                .set("margin-top", "16px")
                .set("padding", "12px")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "4px")
                .set("font-family", "monospace");
        resultArea.setVisible(false);

        add(title);
        add(environmentSelect);
        add(componentSelect);
        add(tuxedoServiceSelect);
        add(fieldsPanel);
        add(callButton);
        add(resultArea);
        setAlignSelf(FlexComponent.Alignment.START, environmentSelect);
        setAlignSelf(FlexComponent.Alignment.START, componentSelect);
        setAlignSelf(FlexComponent.Alignment.START, tuxedoServiceSelect);
        setAlignSelf(FlexComponent.Alignment.START, fieldsPanel);
        setAlignSelf(FlexComponent.Alignment.START, callButton);
    }

    private void updateFieldsPanel(JtuxedoService service) {
        fieldInputsByName.clear();
        fieldsPanel.removeAll();
        if (service == null) {
            fieldsPanel.setVisible(false);
            return;
        }
        List<JtuxedoServiceField> fields = jtuxedoServiceConfigService.findFieldsByServiceId(service.getId());
        if (fields.isEmpty()) {
            fieldsPanel.add(new Paragraph("Ta usługa nie ma zdefiniowanych pól wejściowych."));
            fieldsPanel.setVisible(true);
            return;
        }
        fieldsPanel.add(new H3("Parametry usługi"));
        for (JtuxedoServiceField field : fields) {
            String label = field.getName() + (field.getDataType() != null ? " (" + field.getDataType().getDisplayName() + ")" : "");
            TextField input = new TextField(label);
            if (field.getMaxLength() != null && field.getMaxLength() > 0) {
                input.setMaxLength(field.getMaxLength());
            }
            if (field.getDefaultValue() != null && !field.getDefaultValue().isBlank()) {
                input.setValue(field.getDefaultValue());
            }
            input.setWidthFull();
            input.setClearButtonVisible(true);
            fieldsPanel.add(input);
            fieldInputsByName.put(field.getName(), input);
        }
        fieldsPanel.setVisible(true);
    }

    private void updateTuxedoServiceList(ServerService component) {
        boolean tuxedo = component != null && component.getType() == ServiceType.TUXEDO;
        tuxedoServiceSelect.setVisible(tuxedo);
        if (tuxedo) {
            List<JtuxedoService> services = jtuxedoServiceConfigService.findAllServices();
            tuxedoServiceSelect.setItems(services);
            JtuxedoService firstService = services.isEmpty() ? null : services.get(0);
            tuxedoServiceSelect.setValue(firstService);
            updateFieldsPanel(firstService);
        } else {
            tuxedoServiceSelect.setItems(List.of());
            tuxedoServiceSelect.setValue(null);
            updateFieldsPanel(null);
        }
    }

    private void callService() {
        ServerService component = componentSelect.getValue();
        if (component == null) {
            Notification.show("Wybierz środowisko i komponent", 3000, Notification.Position.MIDDLE);
            return;
        }
        if (component.getType() != ServiceType.TUXEDO) {
            Notification.show("Wywołanie zadań jest dostępne tylko dla komponentu Tuxedo.", 3000, Notification.Position.MIDDLE);
            return;
        }
        JtuxedoService tuxedoService = tuxedoServiceSelect.getValue();
        if (tuxedoService == null) {
            Notification.show("Wybierz usługę Tuxedo", 3000, Notification.Position.MIDDLE);
            return;
        }

        TaskTemplate tuxedoTemplate = taskTemplateService.findByType(TaskTemplateType.TUXEDO).stream().findFirst().orElse(null);
        if (tuxedoTemplate == null) {
            Notification.show("Brak szablonu zadań Tuxedo. Dodaj szablon typu Tuxedo w Tasks → Task templates.", 5000, Notification.Position.MIDDLE);
            return;
        }

        ObjectNode parameters = JSON.createObjectNode();
        for (Map.Entry<String, TextField> e : fieldInputsByName.entrySet()) {
            String value = e.getValue().getValue();
            if (value != null && !value.isBlank()) {
                parameters.put(e.getKey(), value);
            }
        }
        String parametersJson = parameters.isEmpty() ? "{}" : parameters.toString();

        String taskName = component.getName() + " / " + tuxedoService.getName();
        ObjectNode configNode = JSON.createObjectNode();
        configNode.put("serverServiceId", component.getId());
        configNode.put("jtuxedoServiceId", tuxedoService.getId());
        configNode.put("parametersJson", parametersJson);
        String config;
        try {
            config = JSON.writeValueAsString(configNode);
        } catch (JsonProcessingException ex) {
            config = String.format("{\"serverServiceId\":%d,\"jtuxedoServiceId\":%d,\"parametersJson\":\"{}\"}",
                    component.getId(), tuxedoService.getId());
        }

        var task = taskService.createTask(taskName, "Wywołanie z widoku Wywołanie usług", pl.pekao.zesportal.entity.Task.TaskPriority.NORMAL, tuxedoTemplate.getId(), config);
        taskService.executeTaskImmediately(task);

        resultArea.setText("Zadanie \"" + taskName + "\" zostało dodane i uruchomione.\nStatus możesz sprawdzić w Tasks → Task list.");
        resultArea.setVisible(true);
        resultArea.getStyle().remove("color");
        Notification.show("Zadanie dodane i uruchomione", 3000, Notification.Position.MIDDLE);
    }
}
