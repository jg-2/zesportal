package pl.pekao.zesportal.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import pl.pekao.zesportal.entity.ImportDefinition;
import pl.pekao.zesportal.entity.ImportDefinition.FileType;
import pl.pekao.zesportal.entity.ImportDefinition.ImportSource;
import pl.pekao.zesportal.entity.ImportFieldMapping;
import pl.pekao.zesportal.entity.ImportFieldMapping.SourceType;
import pl.pekao.zesportal.entity.JtuxedoService;
import pl.pekao.zesportal.entity.JtuxedoServiceField;
import pl.pekao.zesportal.entity.ServerService;
import pl.pekao.zesportal.service.DatabaseViewQueryService;
import pl.pekao.zesportal.service.ImportDefinitionService;
import pl.pekao.zesportal.service.JtuxedoServiceConfigService;
import pl.pekao.zesportal.service.ServerServiceManagementService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Route(value = "jtuxed0/definicje-importow", layout = MainLayout.class)
@PageTitle("Definicje importów | Zesportal")
public class DefinicjeImportowView extends VerticalLayout {

    private final JtuxedoServiceConfigService jtuxedoServiceConfigService;
    private final ImportDefinitionService importDefinitionService;
    private final ServerServiceManagementService serverServiceManagementService;
    private final DatabaseViewQueryService databaseViewQueryService;

    private final ComboBox<JtuxedoService> serviceSelect = new ComboBox<>("Usługa");
    private final Grid<ImportDefinition> definitionsGrid = new Grid<>(ImportDefinition.class, false);
    private final Button addDefinitionButton = new Button("Dodaj definicję");

    public DefinicjeImportowView(JtuxedoServiceConfigService jtuxedoServiceConfigService,
                                ImportDefinitionService importDefinitionService,
                                ServerServiceManagementService serverServiceManagementService,
                                DatabaseViewQueryService databaseViewQueryService) {
        this.jtuxedoServiceConfigService = jtuxedoServiceConfigService;
        this.importDefinitionService = importDefinitionService;
        this.serverServiceManagementService = serverServiceManagementService;
        this.databaseViewQueryService = databaseViewQueryService;

        setPadding(true);
        setSpacing(true);
        setSizeFull();

        serviceSelect.setWidth("400px");
        serviceSelect.setItemLabelGenerator(JtuxedoService::getName);
        serviceSelect.setItems(jtuxedoServiceConfigService.findAllServices());
        serviceSelect.addValueChangeListener(e -> refreshDefinitionsGrid());

        addDefinitionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addDefinitionButton.setEnabled(false);
        addDefinitionButton.addClickListener(e -> {
            JtuxedoService svc = serviceSelect.getValue();
            if (svc != null) openDefinitionDialog(svc, null);
        });

        definitionsGrid.addComponentColumn(this::buildTypeAndNameCell)
                .setHeader("Nazwa").setWidth("260px").setFlexGrow(0);
        definitionsGrid.addColumn(d -> d.getImportSource() == ImportSource.DATABASE ? "Baza: " + (d.getViewName() != null ? d.getViewName() : "—") : "Plik")
                .setHeader("Źródło").setWidth("180px").setFlexGrow(0);
        definitionsGrid.addColumn(d -> d.getImportSource() == ImportSource.FILE ? d.getFileType().getDisplayName() : "—").setHeader("Rodzaj pliku").setWidth("90px").setFlexGrow(0);
        definitionsGrid.addColumn(ImportDefinition::getFieldSeparator).setHeader("Separator pól").setWidth("100px").setFlexGrow(0);
        definitionsGrid.addColumn(ImportDefinition::getLineSeparator).setHeader("Separator linii").setWidth("100px").setFlexGrow(0);
        definitionsGrid.addColumn(ImportDefinition::getFileNameRegex).setHeader("Regex pliku").setFlexGrow(1);
        definitionsGrid.addComponentColumn(def -> {
            Button editBtn = new Button("Edytuj", ev -> openDefinitionDialog(serviceSelect.getValue(), def));
            Button deleteBtn = new Button("Usuń", ev -> deleteDefinition(def));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("Akcje").setWidth("180px").setFlexGrow(0);
        definitionsGrid.setWidthFull();
        definitionsGrid.setHeight("400px");
        definitionsGrid.getStyle().set("font-size", "var(--lumo-font-size-s)");

        HorizontalLayout topBar = new HorizontalLayout(serviceSelect, addDefinitionButton);
        topBar.setAlignItems(FlexComponent.Alignment.END);
        topBar.setSpacing(true);

        add(topBar);
        add(definitionsGrid);
    }

    private HorizontalLayout buildTypeAndNameCell(ImportDefinition def) {
        Icon icon;
        String tooltip;
        if (def.getImportSource() == ImportSource.DATABASE) {
            icon = VaadinIcon.DATABASE.create();
            tooltip = "Import z bazy danych";
        } else {
            FileType ft = def.getFileType();
            if (ft == FileType.JSON) {
                icon = VaadinIcon.FILE_CODE.create();
                tooltip = "Plik JSON";
            } else if (ft == FileType.XML) {
                icon = VaadinIcon.FILE_CODE.create();
                tooltip = "Plik XML";
            } else if (ft == FileType.CSV) {
                icon = VaadinIcon.TABLE.create();
                tooltip = "Plik CSV";
            } else {
                icon = VaadinIcon.FILE_TEXT.create();
                tooltip = "Plik tekstowy";
            }
        }
        icon.setSize("20px");
        icon.getElement().setAttribute("title", tooltip);
        Span name = new Span(def.getName() != null ? def.getName() : "");
        HorizontalLayout layout = new HorizontalLayout(icon, name);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);
        return layout;
    }

    private void refreshDefinitionsGrid() {
        JtuxedoService svc = serviceSelect.getValue();
        addDefinitionButton.setEnabled(svc != null);
        if (svc == null) {
            definitionsGrid.setItems(List.of());
            return;
        }
        definitionsGrid.setItems(importDefinitionService.findByServiceId(svc.getId()));
    }

    private void openDefinitionDialog(JtuxedoService service, ImportDefinition existing) {
        if (service == null) return;
        boolean isNew = (existing == null);
        List<JtuxedoServiceField> serviceFields = jtuxedoServiceConfigService.findFieldsByServiceId(service.getId());

        ImportDefinition bean = new ImportDefinition();
        bean.setJtuxedoService(service);
        bean.setFileType(FileType.TXT);
        bean.setFieldSeparator(",");
        bean.setLineSeparator("\n");
        List<ImportFieldMapping> mappingsList = new ArrayList<>();
        if (!isNew && existing != null) {
            Optional<ImportDefinition> loaded = importDefinitionService.findByIdWithMappings(existing.getId());
            if (loaded.isPresent()) {
                ImportDefinition def = loaded.get();
                bean.setId(def.getId());
                bean.setName(def.getName());
                bean.setImportSource(def.getImportSource() != null ? def.getImportSource() : ImportSource.FILE);
                bean.setDbServerServiceId(def.getDbServerServiceId());
                bean.setViewName(def.getViewName());
                bean.setFileType(def.getFileType());
                bean.setFieldSeparator(def.getFieldSeparator());
                bean.setLineSeparator(def.getLineSeparator());
                bean.setFileNameRegex(def.getFileNameRegex());
                for (ImportFieldMapping m : def.getMappings()) {
                    ImportFieldMapping copy = new ImportFieldMapping();
                    copy.setTargetFieldName(m.getTargetFieldName());
                    copy.setSourceType(m.getSourceType());
                    copy.setSourceField(m.getSourceField());
                    copy.setConstantValue(m.getConstantValue());
                    mappingsList.add(copy);
                }
            }
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "Nowa definicja importu" : "Edycja definicji importu");
        dialog.setWidth("720px");

        TextField nameField = new TextField("Nazwa definicji");
        nameField.setWidthFull();
        nameField.setRequired(true);

        Select<ImportSource> sourceSelect = new Select<>();
        sourceSelect.setLabel("Źródło importu");
        sourceSelect.setItems(ImportSource.values());
        sourceSelect.setItemLabelGenerator(ImportSource::getDisplayName);
        sourceSelect.setValue(bean.getImportSource());
        sourceSelect.setWidthFull();

        List<ServerService> dbComponents = serverServiceManagementService.findDbServices();
        ComboBox<ServerService> dbComponentSelect = new ComboBox<>("Komponent bazy danych");
        dbComponentSelect.setItems(dbComponents);
        dbComponentSelect.setItemLabelGenerator(ss -> ss.getName() + (ss.getServer() != null ? " @ " + ss.getServer().getName() : ""));
        dbComponentSelect.setWidthFull();
        dbComponentSelect.setVisible(bean.getImportSource() == ImportSource.DATABASE);
        if (bean.getDbServerServiceId() != null) {
            dbComponents.stream().filter(ss -> ss.getId().equals(bean.getDbServerServiceId())).findFirst().ifPresent(dbComponentSelect::setValue);
        }

        ComboBox<String> viewCombo = new ComboBox<>("Widok");
        viewCombo.setWidthFull();
        viewCombo.setPlaceholder("Wybierz komponent bazy, potem widok");
        viewCombo.setItemLabelGenerator(s -> s);
        viewCombo.setVisible(bean.getImportSource() == ImportSource.DATABASE);
        final List<String> viewColumnsList = new ArrayList<>();

        dbComponentSelect.addValueChangeListener(e -> {
            ServerService comp = e.getValue();
            viewCombo.clear();
            viewCombo.setItems(List.of());
            viewColumnsList.clear();
            if (comp != null) {
                try {
                    List<String> viewNames = databaseViewQueryService.getViewNames(comp);
                    viewCombo.setItems(viewNames);
                    if (bean.getViewName() != null && viewNames.contains(bean.getViewName())) {
                        viewCombo.setValue(bean.getViewName());
                    }
                } catch (Exception ex) {
                    Notification.show("Nie udało się odczytać listy widoków: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                }
            }
        });

        viewCombo.addValueChangeListener(e -> {
            String viewName = e.getValue();
            viewColumnsList.clear();
            if (viewName != null && !viewName.isBlank()) {
                ServerService comp = dbComponentSelect.getValue();
                if (comp != null) {
                    try {
                        viewColumnsList.addAll(databaseViewQueryService.getViewColumns(comp, viewName));
                        // Automatyczne mapowanie: kolumny widoku o tej samej nazwie co pole usługi
                        Set<String> alreadyMapped = mappingsList.stream()
                                .map(ImportFieldMapping::getTargetFieldName)
                                .filter(Objects::nonNull)
                                .filter(n -> !n.isBlank())
                                .collect(Collectors.toSet());
                        Set<String> serviceFieldNames = serviceFields.stream()
                                .map(JtuxedoServiceField::getName)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                        for (String col : viewColumnsList) {
                            if (col != null && !col.isBlank() && serviceFieldNames.contains(col) && !alreadyMapped.contains(col)) {
                                ImportFieldMapping m = new ImportFieldMapping();
                                m.setSourceType(SourceType.DATABASE_FIELD);
                                m.setSourceField(col);
                                m.setTargetFieldName(col);
                                mappingsList.add(m);
                                alreadyMapped.add(col);
                            }
                        }
                    } catch (Exception ex) {
                        Notification.show("Nie udało się odczytać kolumn widoku: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                    }
                }
            }
        });

        // Po ustawieniu komponentu przy edycji załaduj widoki i kolumny
        if (bean.getImportSource() == ImportSource.DATABASE && bean.getDbServerServiceId() != null) {
            dbComponents.stream().filter(ss -> ss.getId().equals(bean.getDbServerServiceId())).findFirst().ifPresent(comp -> {
                try {
                    List<String> viewNames = databaseViewQueryService.getViewNames(comp);
                    viewCombo.setItems(viewNames);
                    if (bean.getViewName() != null && viewNames.contains(bean.getViewName())) {
                        viewCombo.setValue(bean.getViewName());
                        viewColumnsList.clear();
                        viewColumnsList.addAll(databaseViewQueryService.getViewColumns(comp, bean.getViewName()));
                    }
                } catch (Exception ignored) { }
            });
        }

        VerticalLayout dbOptions = new VerticalLayout(dbComponentSelect, viewCombo);
        dbOptions.setPadding(false);
        dbOptions.setSpacing(true);
        dbOptions.setVisible(bean.getImportSource() == ImportSource.DATABASE);

        Select<FileType> fileTypeSelect = new Select<>();
        fileTypeSelect.setLabel("Rodzaj pliku");
        fileTypeSelect.setItems(FileType.values());
        fileTypeSelect.setItemLabelGenerator(FileType::getDisplayName);
        fileTypeSelect.setValue(bean.getFileType());
        fileTypeSelect.setWidthFull();
        fileTypeSelect.setVisible(bean.getImportSource() == ImportSource.FILE);

        TextField fieldSeparatorField = new TextField("Separator pól (TXT)");
        fieldSeparatorField.setWidthFull();
        fieldSeparatorField.setPlaceholder("np. , lub \\t");

        TextField lineSeparatorField = new TextField("Separator końca linii (TXT)");
        lineSeparatorField.setWidthFull();
        lineSeparatorField.setPlaceholder("np. \\n lub \\r\\n");

        TextField fileNameRegexField = new TextField("Regex nazwy pliku (TXT)");
        fileNameRegexField.setWidthFull();
        fileNameRegexField.setPlaceholder("np. .*\\.txt");

        VerticalLayout txtOptions = new VerticalLayout(fieldSeparatorField, lineSeparatorField, fileNameRegexField);
        txtOptions.setPadding(false);
        txtOptions.setSpacing(true);
        txtOptions.setVisible(bean.getImportSource() == ImportSource.FILE && bean.getFileType() == FileType.TXT);

        fileTypeSelect.addValueChangeListener(e -> txtOptions.setVisible(bean.getImportSource() == ImportSource.FILE && FileType.TXT.equals(e.getValue())));

        Binder<ImportDefinition> binder = new Binder<>(ImportDefinition.class);
        binder.forField(nameField)
                .asRequired("Uzupełnij nazwę definicji")
                .bind("name");
        binder.forField(sourceSelect).bind("importSource");
        binder.forField(fileTypeSelect).bind("fileType");
        binder.forField(fieldSeparatorField).bind("fieldSeparator");
        binder.forField(lineSeparatorField).bind("lineSeparator");
        binder.forField(fileNameRegexField).bind("fileNameRegex");
        binder.readBean(bean);

        VerticalLayout mappingsContainer = new VerticalLayout();
        mappingsContainer.setPadding(false);
        mappingsContainer.setSpacing(true);
        final Runnable[] refreshRef = new Runnable[1];
        refreshRef[0] = () -> {
            mappingsContainer.removeAll();
            List<String> allFieldNames = serviceFields.stream().map(JtuxedoServiceField::getName).toList();
            Set<String> allUsedTargets = mappingsList.stream()
                    .map(ImportFieldMapping::getTargetFieldName)
                    .filter(Objects::nonNull)
                    .filter(n -> !n.isBlank())
                    .collect(Collectors.toSet());
            for (int i = 0; i < mappingsList.size(); i++) {
                ImportFieldMapping m = mappingsList.get(i);
                int idx = i;
                String currentTarget = m.getTargetFieldName();
                // W tym wierszu wykluczamy tylko pola zajęte przez INNE wiersze (bieżący wiersz może pokazać swoje pole)
                Set<String> usedByOtherRows = new HashSet<>(allUsedTargets);
                if (currentTarget != null && !currentTarget.isBlank()) {
                    usedByOtherRows.remove(currentTarget);
                }
                List<String> availableForThisRow = allFieldNames.stream()
                        .filter(n -> !usedByOtherRows.contains(n))
                        .toList();
                ComboBox<String> targetSelect = new ComboBox<>("Pole w usłudze");
                targetSelect.setItems(availableForThisRow);
                if (currentTarget != null && availableForThisRow.contains(currentTarget)) {
                    targetSelect.setValue(currentTarget);
                } else if (!availableForThisRow.isEmpty()) {
                    String fallback = availableForThisRow.get(0);
                    targetSelect.setValue(fallback);
                    m.setTargetFieldName(fallback);
                }
                targetSelect.setWidth("180px");
                targetSelect.addValueChangeListener(ev -> {
                    String newVal = ev.getValue();
                    m.setTargetFieldName(newVal);
                    refreshRef[0].run();
                });

                ImportSource currentSource = sourceSelect.getValue();
                boolean fromDb = (currentSource == ImportSource.DATABASE);
                List<SourceType> allowedTypes = fromDb
                        ? List.of(SourceType.DATABASE_FIELD, SourceType.CONSTANT)
                        : List.of(SourceType.FILE_FIELD, SourceType.CONSTANT);
                if (!allowedTypes.contains(m.getSourceType())) {
                    m.setSourceType(allowedTypes.get(0));
                }

                Select<SourceType> sourceTypeSelect = new Select<>();
                sourceTypeSelect.setLabel("Źródło");
                sourceTypeSelect.setItems(allowedTypes);
                sourceTypeSelect.setItemLabelGenerator(SourceType::getDisplayName);
                sourceTypeSelect.setValue(m.getSourceType());
                sourceTypeSelect.setWidth("140px");
                sourceTypeSelect.addValueChangeListener(ev -> {
                    m.setSourceType(ev.getValue());
                    refreshRef[0].run();
                });

                com.vaadin.flow.component.Component sourceValueComponent;
                if (m.getSourceType() == SourceType.DATABASE_FIELD) {
                    ComboBox<String> columnCombo = new ComboBox<>();
                    columnCombo.setPlaceholder("Kolumna widoku");
                    columnCombo.setWidth("200px");
                    columnCombo.setItems(viewColumnsList);
                    columnCombo.setItemLabelGenerator(s -> s);
                    if (m.getSourceField() != null && viewColumnsList.contains(m.getSourceField())) {
                        columnCombo.setValue(m.getSourceField());
                    } else if (!viewColumnsList.isEmpty()) {
                        columnCombo.setValue(viewColumnsList.get(0));
                        m.setSourceField(viewColumnsList.get(0));
                    }
                    columnCombo.addValueChangeListener(ev -> m.setSourceField(ev.getValue()));
                    sourceValueComponent = columnCombo;
                } else {
                    TextField sourceValueField = new TextField();
                    sourceValueField.setPlaceholder(m.getSourceType() == SourceType.CONSTANT ? "Wartość stała" : "Kolumna (np. 0, 1)");
                    sourceValueField.setWidth("200px");
                    if (m.getSourceType() == SourceType.CONSTANT) {
                        sourceValueField.setValue(m.getConstantValue() != null ? m.getConstantValue() : "");
                    } else {
                        sourceValueField.setValue(m.getSourceField() != null ? m.getSourceField() : "");
                    }
                    sourceValueField.addValueChangeListener(ev -> {
                        if (m.getSourceType() == SourceType.CONSTANT) m.setConstantValue(ev.getValue());
                        else m.setSourceField(ev.getValue());
                    });
                    sourceValueComponent = sourceValueField;
                }

                Button removeBtn = new Button("Usuń", ev -> {
                    mappingsList.remove(idx);
                    refreshRef[0].run();
                });
                removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

                HorizontalLayout row = new HorizontalLayout(targetSelect, sourceTypeSelect, sourceValueComponent, removeBtn);
                row.setAlignItems(FlexComponent.Alignment.END);
                row.setSpacing(true);
                mappingsContainer.add(row);
            }
        };

        Button addMappingBtn = new Button("Dodaj mapowanie (pole z pliku)", e -> {
            if (serviceFields.isEmpty()) {
                Notification.show("Brak pól w usłudze. Skonfiguruj pola w Konfiguracja usług.", 3000, Notification.Position.MIDDLE);
                return;
            }
            Set<String> alreadyUsed = mappingsList.stream()
                    .map(ImportFieldMapping::getTargetFieldName)
                    .filter(Objects::nonNull)
                    .filter(n -> !n.isBlank())
                    .collect(Collectors.toSet());
            String firstFree = serviceFields.stream()
                    .map(JtuxedoServiceField::getName)
                    .filter(n -> !alreadyUsed.contains(n))
                    .findFirst()
                    .orElse(serviceFields.get(0).getName());
            ImportFieldMapping newMapping = new ImportFieldMapping();
            newMapping.setTargetFieldName(firstFree);
            newMapping.setSourceType(SourceType.FILE_FIELD);
            newMapping.setSourceField("0");
            mappingsList.add(newMapping);
            refreshRef[0].run();
        });
        addMappingBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button addConstantBtn = new Button("Dodaj wartość stałą", e -> {
            if (serviceFields.isEmpty()) {
                Notification.show("Brak pól w usłudze.", 3000, Notification.Position.MIDDLE);
                return;
            }
            Set<String> alreadyUsed = mappingsList.stream()
                    .map(ImportFieldMapping::getTargetFieldName)
                    .filter(Objects::nonNull)
                    .filter(n -> !n.isBlank())
                    .collect(Collectors.toSet());
            String firstFree = serviceFields.stream()
                    .map(JtuxedoServiceField::getName)
                    .filter(n -> !alreadyUsed.contains(n))
                    .findFirst()
                    .orElse(serviceFields.get(0).getName());
            ImportFieldMapping newMapping = new ImportFieldMapping();
            newMapping.setTargetFieldName(firstFree);
            newMapping.setSourceType(SourceType.CONSTANT);
            newMapping.setConstantValue("");
            mappingsList.add(newMapping);
            refreshRef[0].run();
        });
        addConstantBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button addDbFieldBtn = new Button("Dodaj pole z widoku", e -> {
            if (serviceFields.isEmpty()) {
                Notification.show("Brak pól w usłudze.", 3000, Notification.Position.MIDDLE);
                return;
            }
            Set<String> alreadyUsed = mappingsList.stream()
                    .map(ImportFieldMapping::getTargetFieldName)
                    .filter(Objects::nonNull)
                    .filter(n -> !n.isBlank())
                    .collect(Collectors.toSet());
            String firstFree = serviceFields.stream()
                    .map(JtuxedoServiceField::getName)
                    .filter(n -> !alreadyUsed.contains(n))
                    .findFirst()
                    .orElse(serviceFields.get(0).getName());
            ImportFieldMapping newMapping = new ImportFieldMapping();
            newMapping.setTargetFieldName(firstFree);
            newMapping.setSourceType(SourceType.DATABASE_FIELD);
            newMapping.setSourceField("");
            mappingsList.add(newMapping);
            refreshRef[0].run();
        });
        addDbFieldBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        viewCombo.addValueChangeListener(ev -> refreshRef[0].run());

        sourceSelect.addValueChangeListener(e -> {
            boolean db = e.getValue() == ImportSource.DATABASE;
            dbOptions.setVisible(db);
            dbComponentSelect.setVisible(db);
            viewCombo.setVisible(db);
            fileTypeSelect.setVisible(!db);
            txtOptions.setVisible(!db && FileType.TXT.equals(fileTypeSelect.getValue()));
            updateMappingButtonsVisibility(addMappingBtn, addDbFieldBtn, addConstantBtn, db);
            refreshRef[0].run();
        });

        updateMappingButtonsVisibility(addMappingBtn, addDbFieldBtn, addConstantBtn, bean.getImportSource() == ImportSource.DATABASE);
        refreshRef[0].run();

        VerticalLayout mappingsSection = new VerticalLayout();
        mappingsSection.add(new H3("Mapowania (pole usługi Tuxedo ← źródło)"));
        HorizontalLayout mappingButtons = new HorizontalLayout(addMappingBtn, addDbFieldBtn, addConstantBtn);
        mappingButtons.setSpacing(true);
        mappingsSection.add(mappingButtons);
        mappingsSection.add(mappingsContainer);
        mappingsSection.setWidthFull();

        FormLayout form = new FormLayout(nameField, sourceSelect, dbOptions, fileTypeSelect, txtOptions);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        VerticalLayout content = new VerticalLayout(form, mappingsSection);
        content.setSpacing(true);

        Button saveBtn = new Button("Zapisz", e -> {
            try {
                binder.writeBean(bean);
                if (bean.getImportSource() == ImportSource.DATABASE) {
                    ServerService dbComp = dbComponentSelect.getValue();
                    if (dbComp == null) {
                        Notification.show("Wybierz komponent bazy danych", 3000, Notification.Position.MIDDLE);
                        return;
                    }
                    bean.setDbServerServiceId(dbComp.getId());
                    String vn = viewCombo.getValue();
                    if (vn == null || vn.isBlank()) {
                        Notification.show("Podaj nazwę widoku", 3000, Notification.Position.MIDDLE);
                        return;
                    }
                    bean.setViewName(vn.trim());
                } else {
                    bean.setDbServerServiceId(null);
                    bean.setViewName(null);
                }
                bean.getMappings().clear();
                for (ImportFieldMapping m : mappingsList) {
                    m.setImportDefinition(bean);
                    bean.getMappings().add(m);
                }
                importDefinitionService.save(bean);
                refreshDefinitionsGrid();
                dialog.close();
                Notification.show("Definicja zapisana", 2000, Notification.Position.MIDDLE);
            } catch (ValidationException vex) {
                Notification.show("Uzupełnij nazwę definicji", 3000, Notification.Position.MIDDLE);
            } catch (Exception ex) {
                String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                Throwable c = ex.getCause();
                if (c != null && c.getMessage() != null && !c.getMessage().isBlank()) {
                    msg = c.getMessage();
                }
                Notification.show("Błąd zapisu definicji: " + msg, 6000, Notification.Position.MIDDLE);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Anuluj", ev -> dialog.close());

        dialog.add(content);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void deleteDefinition(ImportDefinition def) {
        importDefinitionService.deleteById(def.getId());
        refreshDefinitionsGrid();
        Notification.show("Definicja usunięta", 2000, Notification.Position.MIDDLE);
    }

    private static void updateMappingButtonsVisibility(Button addMappingBtn, Button addDbFieldBtn, Button addConstantBtn, boolean fromDatabase) {
        addMappingBtn.setVisible(!fromDatabase);
        addDbFieldBtn.setVisible(fromDatabase);
        addConstantBtn.setVisible(true);
    }
}
