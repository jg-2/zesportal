package pl.pekao.zesportal.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import pl.pekao.zesportal.entity.ImportDefinition;
import pl.pekao.zesportal.entity.ImportDefinition.FileType;
import pl.pekao.zesportal.entity.ImportFieldMapping;
import pl.pekao.zesportal.entity.ImportFieldMapping.SourceType;
import pl.pekao.zesportal.entity.JtuxedoService;
import pl.pekao.zesportal.entity.JtuxedoServiceField;
import pl.pekao.zesportal.service.ImportDefinitionService;
import pl.pekao.zesportal.service.JtuxedoServiceConfigService;

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

    private final ComboBox<JtuxedoService> serviceSelect = new ComboBox<>("Usługa");
    private final Grid<ImportDefinition> definitionsGrid = new Grid<>(ImportDefinition.class, false);
    private final Button addDefinitionButton = new Button("Dodaj definicję");

    public DefinicjeImportowView(JtuxedoServiceConfigService jtuxedoServiceConfigService,
                                ImportDefinitionService importDefinitionService) {
        this.jtuxedoServiceConfigService = jtuxedoServiceConfigService;
        this.importDefinitionService = importDefinitionService;

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

        definitionsGrid.addColumn(ImportDefinition::getName).setHeader("Nazwa").setWidth("200px").setFlexGrow(0);
        definitionsGrid.addColumn(d -> d.getFileType().getDisplayName()).setHeader("Rodzaj pliku").setWidth("100px").setFlexGrow(0);
        definitionsGrid.addColumn(ImportDefinition::getFieldSeparator).setHeader("Separator pól").setWidth("120px").setFlexGrow(0);
        definitionsGrid.addColumn(ImportDefinition::getLineSeparator).setHeader("Separator linii").setWidth("120px").setFlexGrow(0);
        definitionsGrid.addColumn(ImportDefinition::getFileNameRegex).setHeader("Regex nazwy pliku").setFlexGrow(1);
        definitionsGrid.addComponentColumn(def -> {
            Button editBtn = new Button("Edytuj", ev -> openDefinitionDialog(serviceSelect.getValue(), def));
            Button deleteBtn = new Button("Usuń", ev -> deleteDefinition(def));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("Akcje").setWidth("180px").setFlexGrow(0);
        definitionsGrid.setWidthFull();
        definitionsGrid.setHeight("400px");

        HorizontalLayout topBar = new HorizontalLayout(serviceSelect, addDefinitionButton);
        topBar.setAlignItems(FlexComponent.Alignment.END);
        topBar.setSpacing(true);

        add(topBar);
        add(definitionsGrid);
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

        Select<FileType> fileTypeSelect = new Select<>();
        fileTypeSelect.setLabel("Rodzaj pliku");
        fileTypeSelect.setItems(FileType.values());
        fileTypeSelect.setItemLabelGenerator(FileType::getDisplayName);
        fileTypeSelect.setValue(FileType.TXT);
        fileTypeSelect.setWidthFull();

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
        txtOptions.setVisible(bean.getFileType() == FileType.TXT);
        fileTypeSelect.addValueChangeListener(e -> txtOptions.setVisible(FileType.TXT.equals(e.getValue())));

        Binder<ImportDefinition> binder = new Binder<>(ImportDefinition.class);
        binder.forField(nameField).bind("name");
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

                Select<SourceType> sourceTypeSelect = new Select<>();
                sourceTypeSelect.setLabel("Źródło");
                sourceTypeSelect.setItems(SourceType.values());
                sourceTypeSelect.setItemLabelGenerator(SourceType::getDisplayName);
                sourceTypeSelect.setValue(m.getSourceType());
                sourceTypeSelect.setWidth("140px");
                sourceTypeSelect.addValueChangeListener(ev -> m.setSourceType(ev.getValue()));

                TextField sourceValueField = new TextField();
                sourceValueField.setPlaceholder(m.getSourceType() == SourceType.CONSTANT ? "Wartość stała" : "Kolumna (np. 0, 1)");
                sourceValueField.setWidth("180px");
                if (m.getSourceType() == SourceType.CONSTANT) {
                    sourceValueField.setValue(m.getConstantValue() != null ? m.getConstantValue() : "");
                } else {
                    sourceValueField.setValue(m.getSourceField() != null ? m.getSourceField() : "");
                }
                sourceValueField.addValueChangeListener(ev -> {
                    if (m.getSourceType() == SourceType.CONSTANT) m.setConstantValue(ev.getValue());
                    else m.setSourceField(ev.getValue());
                });
                sourceTypeSelect.addValueChangeListener(ev -> sourceValueField.setPlaceholder(
                        ev.getValue() == SourceType.CONSTANT ? "Wartość stała" : "Kolumna (np. 0, 1)"));

                Button removeBtn = new Button("Usuń", ev -> {
                    mappingsList.remove(idx);
                    refreshRef[0].run();
                });
                removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

                HorizontalLayout row = new HorizontalLayout(targetSelect, sourceTypeSelect, sourceValueField, removeBtn);
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

        refreshRef[0].run();

        VerticalLayout mappingsSection = new VerticalLayout();
        mappingsSection.add(new H3("Mapowania"));
        mappingsSection.add(addMappingBtn);
        mappingsSection.add(addConstantBtn);
        mappingsSection.add(mappingsContainer);
        mappingsSection.setWidthFull();

        FormLayout form = new FormLayout(nameField, fileTypeSelect);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        VerticalLayout content = new VerticalLayout(form, txtOptions, mappingsSection);
        content.setSpacing(true);

        Button saveBtn = new Button("Zapisz", e -> {
            try {
                binder.writeBean(bean);
                bean.getMappings().clear();
                for (ImportFieldMapping m : mappingsList) {
                    m.setImportDefinition(bean);
                    bean.getMappings().add(m);
                }
                importDefinitionService.save(bean);
                refreshDefinitionsGrid();
                dialog.close();
                Notification.show("Definicja zapisana", 2000, Notification.Position.MIDDLE);
            } catch (Exception ex) {
                Notification.show("Uzupełnij nazwę definicji", 3000, Notification.Position.MIDDLE);
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
}
