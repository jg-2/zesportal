package pl.pekao.zesportal.view;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import pl.pekao.zesportal.entity.Environment;
import pl.pekao.zesportal.entity.ImportDefinition;
import pl.pekao.zesportal.entity.ImportDefinition.ImportSource;
import pl.pekao.zesportal.entity.ImportFieldMapping;
import pl.pekao.zesportal.entity.ImportFieldMapping.SourceType;
import pl.pekao.zesportal.entity.JtuxedoService;
import pl.pekao.zesportal.entity.ServerService;
import pl.pekao.zesportal.entity.ServerService.ServiceType;
import pl.pekao.zesportal.entity.TaskTemplate.TaskTemplateType;
import pl.pekao.zesportal.entity.JtuxedoServiceField;
import pl.pekao.zesportal.service.EnvironmentService;
import pl.pekao.zesportal.service.ImportDefinitionService;
import pl.pekao.zesportal.service.ImportRecordValidator;
import pl.pekao.zesportal.service.JtuxedoServiceConfigService;
import pl.pekao.zesportal.service.ServerServiceManagementService;
import pl.pekao.zesportal.service.TaskBatchConfigService;
import pl.pekao.zesportal.service.TaskService;
import pl.pekao.zesportal.service.task.TaskProgressHolder;
import pl.pekao.zesportal.entity.Task;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Route(value = "jtuxed0/wczytaj-z-pliku", layout = MainLayout.class)
@PageTitle("Import | Zesportal")
public class WczytajZPlikuView extends VerticalLayout {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final EnvironmentService environmentService;
    private final ServerServiceManagementService serverServiceManagement;
    private final JtuxedoServiceConfigService jtuxedoServiceConfigService;
    private final ImportDefinitionService importDefinitionService;
    private final TaskService taskService;
    private final TaskBatchConfigService taskBatchConfigService;
    private final TaskProgressHolder progressHolder;

    /** ID zadania wysłanego z tego widoku (do odświeżania statusów wierszy). */
    private Long lastSubmittedTaskId = null;
    private ListDataProvider<RecordWithValidation> gridDataProvider;

    private final Select<Environment> environmentSelect = new Select<>();
    private final Select<ServerService> componentSelect = new Select<>();
    private final Select<JtuxedoService> serviceSelect = new Select<>();
    private final ComboBox<ImportDefinition> definitionCombo = new ComboBox<>("Definicja importu");
    private final Checkbox archiwizujCheckbox = new Checkbox("Archiwizuj");
    private final Button wczytajButton = new Button("Wczytaj");
    private final Grid<RecordWithValidation> grid = new Grid<>();
    private final VerticalLayout validationReportPanel = new VerticalLayout();
    private final Checkbox saveResultCheckbox = new Checkbox("Zapisz rezultat do JSON (pełny wynik w polu Rezultat po zakończeniu)");
    private final Button uruchomUslugeButton = new Button("Uruchom usługę");

    /** Zawartość ostatnio przesłanego pliku (UTF-8). */
    private String uploadedFileContent = null;
    /** Ścieżka pliku na serwerze (do odczytu bezpośrednio z dysku). */
    private final TextField serverFilePathField = new TextField("Ścieżka pliku na serwerze");
    /** Lista rekordów z walidacją (dane + błędy na czerwono). */
    private List<RecordWithValidation> loadedRows = new ArrayList<>();
    /** Mapowania z ostatnio użytej definicji importu (do raportu: kolumna / stała). */
    private List<ImportFieldMapping> lastUsedMappings = new ArrayList<>();

    /** Status wywołania usługi dla wiersza (odświeżany po uruchomieniu zadania). */
    public enum InvocationRowStatus {
        PENDING, EXECUTING, OK, ERROR
    }

    /** Wiersz tabeli: dane rekordu + błędy walidacji per pole (do podświetlenia na czerwono). */
    public static final class RecordWithValidation {
        private final Map<String, Object> data;
        private final Map<String, String> errors;
        private volatile InvocationRowStatus invocationStatus = InvocationRowStatus.PENDING;

        public RecordWithValidation(Map<String, Object> data, Map<String, String> errors) {
            this.data = data != null ? data : Map.of();
            this.errors = errors != null ? errors : Map.of();
        }

        public Map<String, Object> getData() { return data; }
        public Map<String, String> getErrors() { return errors; }
        public InvocationRowStatus getInvocationStatus() { return invocationStatus; }
        public void setInvocationStatus(InvocationRowStatus invocationStatus) { this.invocationStatus = invocationStatus; }
    }

    public WczytajZPlikuView(EnvironmentService environmentService,
                             ServerServiceManagementService serverServiceManagement,
                             JtuxedoServiceConfigService jtuxedoServiceConfigService,
                             ImportDefinitionService importDefinitionService,
                             TaskService taskService,
                             TaskBatchConfigService taskBatchConfigService,
                             TaskProgressHolder progressHolder) {
        this.environmentService = environmentService;
        this.serverServiceManagement = serverServiceManagement;
        this.jtuxedoServiceConfigService = jtuxedoServiceConfigService;
        this.importDefinitionService = importDefinitionService;
        this.taskService = taskService;
        this.taskBatchConfigService = taskBatchConfigService;
        this.progressHolder = progressHolder;

        setPadding(true);
        setSpacing(true);
        setSizeFull();

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
            componentSelect.setValue(components.isEmpty() ? null : components.get(0));
            updateServiceList(componentSelect.getValue());
        });

        componentSelect.setLabel("Komponent");
        componentSelect.setItemLabelGenerator(ss -> ss.getName() + (ss.getServer() != null ? " @ " + ss.getServer().getName() : ""));
        componentSelect.setWidth("320px");
        componentSelect.setPlaceholder("Wybierz komponent");
        componentSelect.addValueChangeListener(e -> updateServiceList(e.getValue()));

        serviceSelect.setLabel("Usługa");
        serviceSelect.setItemLabelGenerator(js -> js.getName() + (js.getDescription() != null && !js.getDescription().isBlank() ? " — " + js.getDescription() : ""));
        serviceSelect.setWidth("320px");
        serviceSelect.setPlaceholder("Wybierz usługę");
        serviceSelect.addValueChangeListener(e -> updateDefinitionList(e.getValue()));

        definitionCombo.setItemLabelGenerator(ImportDefinition::getName);
        definitionCombo.setRenderer(new ComponentRenderer<>(this::buildDefinitionOptionComponent));
        definitionCombo.setWidth("320px");
        definitionCombo.setPlaceholder("Wybierz definicję importu");

        archiwizujCheckbox.setValue(false);

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".txt", ".csv", "text/plain", "text/csv", "application/csv");
        upload.setMaxFileSize(50 * 1024 * 1024); // 50 MB (zgodnie z spring.servlet.multipart.max-file-size)
        upload.addSucceededListener(event -> {
            try (InputStream is = buffer.getInputStream()) {
                uploadedFileContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Notification.show("Plik \"" + event.getFileName() + "\" załadowany do pamięci", 2500, Notification.Position.MIDDLE);
            } catch (Exception ex) {
                uploadedFileContent = null;
                Notification.show("Błąd odczytu pliku: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });

        serverFilePathField.setWidth("400px");
        serverFilePathField.setPlaceholder("np. C:\\data\\import.txt lub /opt/app/import.csv");
        serverFilePathField.setHelperText("Alternatywa dla uploadu – ścieżka pliku na serwerze aplikacji.");

        wczytajButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        wczytajButton.addClickListener(e -> wczytajPlik());

        grid.setPageSize(50);
        grid.setWidthFull();
        grid.setHeight("400px");
        grid.setVisible(false);
        grid.getStyle().set("font-size", "var(--lumo-font-size-s)");

        saveResultCheckbox.setValue(false);
        saveResultCheckbox.setWidthFull();

        uruchomUslugeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        uruchomUslugeButton.setVisible(false);
        uruchomUslugeButton.addClickListener(e -> uruchomUsluge());

        addAttachListener(e -> {
            if (e.isInitialAttach()) {
                getUI().ifPresent(ui -> ui.addPollListener(ev -> updateRowStatusesFromTask()));
            }
        });

        HorizontalLayout uploadRow = new HorizontalLayout(upload, archiwizujCheckbox);
        uploadRow.setAlignItems(FlexComponent.Alignment.CENTER);
        uploadRow.setSpacing(true);

        HorizontalLayout serverPathRow = new HorizontalLayout(serverFilePathField);
        serverPathRow.setAlignItems(FlexComponent.Alignment.CENTER);
        serverPathRow.setSpacing(true);

        HorizontalLayout defRow = new HorizontalLayout(definitionCombo, wczytajButton);
        defRow.setAlignItems(FlexComponent.Alignment.END);
        defRow.setSpacing(true);

        // Reakcja na wybór definicji: tekst przycisku + widoczność sekcji plikowych przy źródle Baza danych.
        definitionCombo.addValueChangeListener(e -> {
            ImportDefinition d = e.getValue();
            boolean fromDb = d != null && d.getImportSource() == ImportSource.DATABASE;
            wczytajButton.setText(fromDb ? "Wczytaj z bazy" : "Wczytaj z pliku");
            uploadRow.setVisible(!fromDb);
            serverPathRow.setVisible(!fromDb);
            archiwizujCheckbox.setVisible(!fromDb);
        });

        add(new H2("Import"));
        add(environmentSelect);
        add(componentSelect);
        add(serviceSelect);
        add(defRow);
        add(uploadRow);
        add(serverPathRow);
        add(grid);
        add(validationReportPanel);
        add(saveResultCheckbox);
        add(uruchomUslugeButton);

        validationReportPanel.setPadding(true);
        validationReportPanel.setSpacing(true);
        validationReportPanel.setVisible(false);
        validationReportPanel.getStyle()
                .set("background", "var(--lumo-error-color-10pct, rgba(214, 41, 62, 0.1))")
                .set("border-radius", "8px")
                .set("border", "1px solid var(--lumo-error-color, #d32f2f)");

        setAlignSelf(FlexComponent.Alignment.START, environmentSelect);
        setAlignSelf(FlexComponent.Alignment.START, componentSelect);
        setAlignSelf(FlexComponent.Alignment.START, serviceSelect);
        setAlignSelf(FlexComponent.Alignment.START, defRow);
        setAlignSelf(FlexComponent.Alignment.START, uploadRow);
        setAlignSelf(FlexComponent.Alignment.START, grid);
        setAlignSelf(FlexComponent.Alignment.START, validationReportPanel);
        setAlignSelf(FlexComponent.Alignment.START, saveResultCheckbox);
        setAlignSelf(FlexComponent.Alignment.START, uruchomUslugeButton);
    }

    /** Uzupełnia panel z raportem walidacji: źródło z definicji (kolumna / stała) oraz co nie pasowało. */
    private void updateValidationReport() {
        validationReportPanel.removeAll();
        if (loadedRows == null || loadedRows.isEmpty()) {
            validationReportPanel.setVisible(false);
            return;
        }
        Map<String, ImportFieldMapping> targetToMapping = new LinkedHashMap<>();
        for (ImportFieldMapping m : lastUsedMappings) {
            if (m.getTargetFieldName() != null && !m.getTargetFieldName().isBlank()) {
                targetToMapping.putIfAbsent(m.getTargetFieldName(), m);
            }
        }

        List<String> errorLines = new ArrayList<>();
        for (int i = 0; i < loadedRows.size(); i++) {
            RecordWithValidation row = loadedRows.get(i);
            if (row.getErrors().isEmpty()) continue;
            int wiersz = i + 1;
            for (Map.Entry<String, String> e : row.getErrors().entrySet()) {
                String fieldName = e.getKey();
                String message = e.getValue();
                Object val = row.getData().get(fieldName);
                String valueStr = val != null ? val.toString() : "";
                if (valueStr.length() > 50) valueStr = valueStr.substring(0, 47) + "...";
                String sourceDesc = formatMappingSource(targetToMapping.get(fieldName));
                String line = "Wiersz " + wiersz + " • Pole „" + fieldName + "” " + sourceDesc + ": " + message;
                if (!valueStr.isEmpty()) line += " (wartość: «" + valueStr + "»)";
                errorLines.add(line);
            }
        }

        List<ImportFieldMapping> constantMappings = lastUsedMappings.stream()
                .filter(m -> m.getSourceType() == SourceType.CONSTANT).toList();

        if (errorLines.isEmpty() && constantMappings.isEmpty()) {
            validationReportPanel.setVisible(false);
            return;
        }

        validationReportPanel.setVisible(true);
        boolean hasErrors = !errorLines.isEmpty();
        if (hasErrors) {
            validationReportPanel.getStyle()
                    .set("background", "var(--lumo-error-color-10pct, rgba(214, 41, 62, 0.1))")
                    .set("border", "1px solid var(--lumo-error-color, #d32f2f)");
        } else {
            validationReportPanel.getStyle()
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("border", "1px solid var(--lumo-contrast-20pct)");
        }

        if (hasErrors) {
            H3 heading = new H3("Wynik walidacji – " + errorLines.size() + " błędów");
            heading.getStyle().set("color", "var(--lumo-error-color, #d32f2f)").set("margin", "0 0 8px 0");
            validationReportPanel.add(heading);
            Paragraph info = new Paragraph("Wartości według definicji importu (kolumna w pliku lub stała). Pola na czerwono nie spełniają reguł usługi (wymagane, max długość, typ).");
            info.getStyle().set("margin", "0 0 8px 0").set("font-size", "var(--lumo-font-size-s)");
            validationReportPanel.add(info);
            UnorderedList list = new UnorderedList();
            list.getStyle().set("margin", "0").set("padding-left", "20px");
            for (String line : errorLines) {
                ListItem item = new ListItem(line);
                item.getStyle().set("margin-bottom", "4px");
                list.add(item);
            }
            validationReportPanel.add(list);
        }

        if (!constantMappings.isEmpty()) {
            H3 constHeading = new H3("Pola ze stałymi (z mapowań definicji)");
            constHeading.getStyle().set("margin", "16px 0 8px 0").set("font-size", "var(--lumo-font-size-m)");
            validationReportPanel.add(constHeading);
            UnorderedList constList = new UnorderedList();
            constList.getStyle().set("margin", "0").set("padding-left", "20px");
            for (ImportFieldMapping m : constantMappings) {
                String target = m.getTargetFieldName() != null ? m.getTargetFieldName() : "?";
                String constantVal = m.getConstantValue() != null ? m.getConstantValue() : "";
                if (constantVal.length() > 80) constantVal = constantVal.substring(0, 77) + "...";
                constList.add(new ListItem("Pole „" + target + "” = stała «" + constantVal + "»"));
            }
            validationReportPanel.add(constList);
        }
    }

    /** Opis źródła wartości z mapowania: "z kolumny 0" lub "stała «X»". */
    private static String formatMappingSource(ImportFieldMapping m) {
        if (m == null) return "(źródło: —)";
        if (m.getSourceType() == SourceType.CONSTANT) {
            String v = m.getConstantValue() != null ? m.getConstantValue() : "";
            if (v.length() > 40) v = v.substring(0, 37) + "...";
            return "(stała «" + v + "»)";
        }
        String col = m.getSourceField() != null && !m.getSourceField().isBlank() ? m.getSourceField() : "?";
        if (m.getSourceType() == SourceType.DATABASE_FIELD) {
            return "(z kolumny " + col + " w widoku)";
        }
        return "(z kolumny " + col + " w pliku)";
    }

    private void updateServiceList(ServerService component) {
        boolean tuxedo = component != null && component.getType() == ServiceType.TUXEDO;
        if (tuxedo) {
            List<JtuxedoService> services = jtuxedoServiceConfigService.findAllServices();
            serviceSelect.setItems(services);
            serviceSelect.setValue(services.isEmpty() ? null : services.get(0));
            updateDefinitionList(serviceSelect.getValue());
        } else {
            serviceSelect.setItems(List.of());
            serviceSelect.setValue(null);
            definitionCombo.setItems(List.of());
            definitionCombo.setValue(null);
        }
    }

    private void updateDefinitionList(JtuxedoService service) {
        if (service == null) {
            definitionCombo.setItems(List.of());
            definitionCombo.setValue(null);
            return;
        }
        List<ImportDefinition> definitions = importDefinitionService.findByServiceId(service.getId());
        definitionCombo.setItems(definitions);
        definitionCombo.setValue(definitions.isEmpty() ? null : definitions.get(0));
    }

    private HorizontalLayout buildDefinitionOptionComponent(ImportDefinition def) {
        if (def == null) {
            return new HorizontalLayout();
        }
        Icon icon;
        String tooltip;
        if (def.getImportSource() == ImportSource.DATABASE) {
            icon = VaadinIcon.DATABASE.create();
            tooltip = "Import z bazy danych";
        } else {
            var ft = def.getFileType();
            if (ft == ImportDefinition.FileType.JSON) {
                icon = VaadinIcon.FILE_CODE.create();
                tooltip = "Plik JSON";
            } else if (ft == ImportDefinition.FileType.XML) {
                icon = VaadinIcon.FILE_CODE.create();
                tooltip = "Plik XML";
            } else if (ft == ImportDefinition.FileType.CSV) {
                icon = VaadinIcon.TABLE.create();
                tooltip = "Plik CSV";
            } else {
                icon = VaadinIcon.FILE_TEXT.create();
                tooltip = "Plik tekstowy";
            }
        }
        icon.setSize("18px");
        icon.getElement().setAttribute("title", tooltip);
        Span name = new Span(def.getName() != null ? def.getName() : "");
        HorizontalLayout layout = new HorizontalLayout(icon, name);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);
        return layout;
    }

    private void wczytajPlik() {
        ImportDefinition def = definitionCombo.getValue();
        if (def == null) {
            Notification.show("Wybierz definicję importu", 3000, Notification.Position.MIDDLE);
            return;
        }
        Optional<ImportDefinition> defWithMappingsOpt = importDefinitionService.findByIdWithMappings(def.getId());
        if (defWithMappingsOpt.isEmpty()) {
            Notification.show("Nie znaleziono definicji z mapowaniami", 3000, Notification.Position.MIDDLE);
            return;
        }
        ImportDefinition defWithMappings = defWithMappingsOpt.get();
        lastUsedMappings = defWithMappings.getMappings() != null ? defWithMappings.getMappings() : List.of();
        List<Map<String, Object>> records;
        if (defWithMappings.getImportSource() == ImportSource.DATABASE) {
            try {
                records = importDefinitionService.loadFromDatabase(defWithMappings);
            } catch (Exception e) {
                Notification.show("Błąd odczytu z bazy: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                loadedRows = List.of();
                lastUsedMappings = List.of();
                refreshGrid();
                updateValidationReport();
                grid.setVisible(true);
                uruchomUslugeButton.setVisible(false);
                return;
            }
        } else {
            String serverPath = serverFilePathField.getValue();
            String contentToParse = null;
            if (serverPath != null && !serverPath.isBlank()) {
                try {
                    contentToParse = Files.readString(Path.of(serverPath.trim()), StandardCharsets.UTF_8);
                } catch (Exception ex) {
                    Notification.show("Błąd odczytu pliku z serwera: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
                    return;
                }
            } else {
                if (uploadedFileContent == null || uploadedFileContent.isBlank()) {
                    Notification.show("Najpierw wybierz plik (upload) lub podaj ścieżkę pliku na serwerze", 4000, Notification.Position.MIDDLE);
                    return;
                }
                contentToParse = uploadedFileContent;
            }
            try {
                records = importDefinitionService.parseFileContent(contentToParse, defWithMappings);
            } catch (Exception e) {
                Notification.show("Błąd parsowania: " + e.getMessage(), 4000, Notification.Position.MIDDLE);
                loadedRows = List.of();
                lastUsedMappings = List.of();
                refreshGrid();
                updateValidationReport();
                grid.setVisible(true);
                uruchomUslugeButton.setVisible(false);
                return;
            }
        }
        JtuxedoService svc = serviceSelect.getValue();
        List<JtuxedoServiceField> serviceFields = svc != null ? jtuxedoServiceConfigService.findFieldsByServiceId(svc.getId()) : List.of();
        loadedRows = new ArrayList<>();
        for (Map<String, Object> row : records) {
            Map<String, String> errs = ImportRecordValidator.validateRecord(row, serviceFields);
            loadedRows.add(new RecordWithValidation(row, errs));
        }
        refreshGrid();
        updateValidationReport();
        grid.setVisible(true);
        uruchomUslugeButton.setVisible(!loadedRows.isEmpty());
        long errorCount = loadedRows.stream().filter(r -> !r.getErrors().isEmpty()).count();
        if (!loadedRows.isEmpty()) {
            String msg = "Wczytano " + loadedRows.size() + " rekordów";
            if (errorCount > 0) msg += " (" + errorCount + " z błędami walidacji – na czerwono)";
            Notification.show(msg, 3000, Notification.Position.MIDDLE);
        } else {
            Notification.show("Brak rekordów do wyświetlenia", 2500, Notification.Position.MIDDLE);
        }
    }

    private void refreshGrid() {
        grid.removeAllColumns();
        if (loadedRows.isEmpty()) {
            gridDataProvider = new ListDataProvider<>(List.of());
            grid.setDataProvider(gridDataProvider);
            return;
        }
        grid.addComponentColumn(this::createStatusIcon).setHeader("Status").setWidth("80px").setFlexGrow(0).setKey("_status");
        Set<String> keys = new TreeSet<>(loadedRows.get(0).getData().keySet());
        for (String key : keys) {
            grid.addComponentColumn(row -> {
                Object val = row.getData().get(key);
                String text = formatCell(val);
                String error = row.getErrors().get(key);
                Span span = new Span(text != null ? text : "");
                if (error != null && !error.isEmpty()) {
                    span.getStyle().set("color", "var(--lumo-error-color, #d32f2f)");
                    span.getStyle().set("font-weight", "500");
                    span.getElement().setAttribute("title", error);
                }
                return span;
            }).setHeader(key).setFlexGrow(1).setKey(key);
        }
        gridDataProvider = new ListDataProvider<>(loadedRows);
        grid.setDataProvider(gridDataProvider);
    }

    private com.vaadin.flow.component.Component createStatusIcon(RecordWithValidation row) {
        Icon icon;
        String title;
        switch (row.getInvocationStatus()) {
            case PENDING -> {
                icon = VaadinIcon.CLOCK.create();
                title = "Oczekuje";
            }
            case EXECUTING -> {
                icon = VaadinIcon.SPINNER.create();
                title = "Wykonywanie";
            }
            case OK -> {
                icon = VaadinIcon.CHECK_CIRCLE.create();
                icon.getStyle().set("color", "var(--lumo-success-color, #2e7d32)");
                title = "OK";
            }
            case ERROR -> {
                icon = VaadinIcon.CLOSE_CIRCLE.create();
                icon.getStyle().set("color", "var(--lumo-error-color, #d32f2f)");
                title = "Błąd";
            }
            default -> {
                icon = VaadinIcon.CIRCLE_THIN.create();
                title = "";
            }
        }
        icon.setSize("20px");
        icon.getElement().setAttribute("title", title);
        return icon;
    }

    private void updateRowStatusesFromTask() {
        if (lastSubmittedTaskId == null || loadedRows.isEmpty()) return;
        Optional<Task> taskOpt = taskService.findById(lastSubmittedTaskId);
        if (taskOpt.isEmpty()) return;
        Task task = taskOpt.get();
        Task.TaskStatus status = task.getStatus();

        if (status == Task.TaskStatus.RUNNING) {
            TaskProgressHolder.ProgressSnapshot snap = progressHolder.getCurrent();
            if (snap != null && snap.taskId() == lastSubmittedTaskId) {
                int current = snap.currentStep();
                for (int i = 0; i < loadedRows.size(); i++) {
                    loadedRows.get(i).setInvocationStatus(i == current ? InvocationRowStatus.EXECUTING : InvocationRowStatus.PENDING);
                }
            }
        } else if (status == Task.TaskStatus.COMPLETED) {
            String resultJson = task.getResult();
            boolean anyStatusUpdated = false;
            if (resultJson != null && !resultJson.isBlank()) {
                try {
                    JsonNode root = JSON.readTree(resultJson);
                    JsonNode invocations = root.get("invocations");
                    if (invocations != null && invocations.isArray()) {
                        for (int i = 0; i < invocations.size() && i < loadedRows.size(); i++) {
                            JsonNode inv = invocations.get(i);
                            String st = inv.has("status") ? inv.get("status").asText() : null;
                            loadedRows.get(i).setInvocationStatus("OK".equals(st) ? InvocationRowStatus.OK : InvocationRowStatus.ERROR);
                            anyStatusUpdated = true;
                        }
                    }
                } catch (Exception ignored) { }
            }
            // Jeżeli nie mamy szczegółowych wyników (brak invocations – np. zapis wyniku wyłączony lub zbyt duży),
            // ale zadanie zakończyło się poprawnie, ustawiamy wszystkie wiersze jako OK, żeby nie wisiały wiecznie w PENDING.
            if (!anyStatusUpdated) {
                for (RecordWithValidation r : loadedRows) {
                    r.setInvocationStatus(InvocationRowStatus.OK);
                }
            }
            lastSubmittedTaskId = null;
            getUI().ifPresent(ui -> ui.setPollInterval(-1));
        } else if (status == Task.TaskStatus.FAILED) {
            for (RecordWithValidation r : loadedRows) {
                r.setInvocationStatus(InvocationRowStatus.ERROR);
            }
            lastSubmittedTaskId = null;
            getUI().ifPresent(ui -> ui.setPollInterval(-1));
        }

        if (gridDataProvider != null) {
            gridDataProvider.refreshAll();
        }
    }

    private static String formatCell(Object value) {
        if (value == null) return "";
        return value.toString();
    }

    private void uruchomUsluge() {
        ServerService component = componentSelect.getValue();
        JtuxedoService service = serviceSelect.getValue();
        if (component == null || component.getType() != ServiceType.TUXEDO) {
            Notification.show("Wybierz komponent Tuxedo", 3000, Notification.Position.MIDDLE);
            return;
        }
        if (service == null) {
            Notification.show("Wybierz usługę", 3000, Notification.Position.MIDDLE);
            return;
        }
        if (loadedRows.isEmpty()) {
            Notification.show("Brak wczytanych rekordów. Wczytaj plik.", 3000, Notification.Position.MIDDLE);
            return;
        }

        ObjectNode configNode = JSON.createObjectNode();
        configNode.put("serverServiceId", component.getId());
        configNode.put("jtuxedoServiceId", service.getId());
        ArrayNode parametersList = JSON.createArrayNode();
        for (RecordWithValidation row : loadedRows) {
            parametersList.add(JSON.valueToTree(row.getData()));
        }
        configNode.set("parametersList", parametersList);
        String config;
        try {
            config = JSON.writeValueAsString(configNode);
        } catch (JsonProcessingException e) {
            Notification.show("Błąd budowania config", 3000, Notification.Position.MIDDLE);
            return;
        }
        // Gdy config przekracza limit kolumny (4000), zapisujemy parametersList do pliku
        final int maxConfigLength = 3500;
        if (config.length() > maxConfigLength) {
            try {
                List<Map<String, Object>> listOfMaps = loadedRows.stream()
                        .map(RecordWithValidation::getData)
                        .toList();
                String batchFilename = taskBatchConfigService.saveParametersList(listOfMaps);
                configNode = JSON.createObjectNode();
                configNode.put("serverServiceId", component.getId());
                configNode.put("jtuxedoServiceId", service.getId());
                configNode.put("parametersListFile", batchFilename);
                config = JSON.writeValueAsString(configNode);
            } catch (Exception e) {
                Notification.show("Błąd zapisu listy do pliku: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                return;
            }
        }

        String taskName = "Import: " + component.getName() + " / " + service.getName() + " (" + loadedRows.size() + " rekordów)";
        var task = taskService.createTask(taskName, "Import danych (plik / baza danych)",
                pl.pekao.zesportal.entity.Task.TaskPriority.NORMAL, null, config, saveResultCheckbox.getValue(), TaskTemplateType.TUXEDO);
        taskService.addTaskToQueue(task);

        lastSubmittedTaskId = task.getId();
        for (RecordWithValidation r : loadedRows) {
            r.setInvocationStatus(InvocationRowStatus.PENDING);
        }
        getUI().ifPresent(ui -> ui.setPollInterval(2000));
        if (gridDataProvider != null) {
            gridDataProvider.refreshAll();
        }

        Notification.show("Zadanie dodane do kolejki: " + loadedRows.size() + " wywołań. Status wierszy odświeża się automatycznie.", 4000, Notification.Position.MIDDLE);
    }
}
