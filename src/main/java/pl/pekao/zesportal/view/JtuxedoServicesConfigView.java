package pl.pekao.zesportal.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import pl.pekao.zesportal.entity.JtuxedoService;
import pl.pekao.zesportal.entity.JtuxedoServiceField;
import pl.pekao.zesportal.entity.JtuxedoServiceField.FieldCardinality;
import pl.pekao.zesportal.entity.JtuxedoServiceField.FieldDataType;
import pl.pekao.zesportal.service.JtuxedoServiceConfigService;
import pl.pekao.zesportal.service.XmlServiceDefinitionParser;
import pl.pekao.zesportal.service.dto.ParsedServiceDefinition;
import pl.pekao.zesportal.service.dto.ServiceImportDiff;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Route(value = "jtuxed0/konfiguracja-uslug", layout = MainLayout.class)
@PageTitle("Konfiguracja usług | Zesportal")
public class JtuxedoServicesConfigView extends VerticalLayout {

    private final JtuxedoServiceConfigService configService;
    private final XmlServiceDefinitionParser xmlParser;
    private final Grid<JtuxedoService> grid;
    private final BeanValidationBinder<JtuxedoService> serviceBinder = new BeanValidationBinder<>(JtuxedoService.class);

    public JtuxedoServicesConfigView(JtuxedoServiceConfigService configService,
                                     XmlServiceDefinitionParser xmlParser) {
        this.configService = configService;
        this.xmlParser = xmlParser;
        this.grid = new Grid<>(JtuxedoService.class, false);

        setPadding(true);
        setSpacing(true);
        setSizeFull();

        add(createHeader());
        add(createGrid());
        refreshGrid();
    }

    private HorizontalLayout createHeader() {
        H2 title = new H2("Konfiguracja usług");
        Button addButton = new Button("Dodaj usługę", e -> openServiceDialog(new JtuxedoService()));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button importXmlButton = new Button("Import z XML", e -> openImportXmlDialog());

        HorizontalLayout header = new HorizontalLayout(title, new HorizontalLayout(addButton, importXmlButton));
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        return header;
    }

    private Grid<JtuxedoService> createGrid() {
        grid.addColumn(JtuxedoService::getName).setHeader("Nazwa").setWidth("220px").setFlexGrow(0);
        grid.addColumn(JtuxedoService::getDescription).setHeader("Opis");
        grid.addComponentColumn(svc -> {
            Button editBtn = new Button("Edytuj", e -> openServiceDialog(svc));
            Button deleteBtn = new Button("Usuń", e -> deleteService(svc));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("Akcje").setWidth("180px").setFlexGrow(0);

        grid.setWidthFull();
        grid.setHeight("600px");
        return grid;
    }

    private void refreshGrid() {
        grid.setItems(configService.findAllServices());
    }

    private void openServiceDialog(JtuxedoService service) {
        boolean isNew = service.getId() == null;
        JtuxedoService serviceToEdit = new JtuxedoService();
        if (!isNew) {
            serviceToEdit.setId(service.getId());
            serviceToEdit.setName(service.getName());
            serviceToEdit.setDescription(service.getDescription());
        }

        List<JtuxedoServiceField> currentFields = new ArrayList<>();
        if (!isNew) {
            currentFields.addAll(configService.findFieldsByServiceId(service.getId()));
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "Nowa usługa" : "Edycja usługi");
        dialog.setWidth("700px");

        TextField nameField = new TextField("Nazwa");
        nameField.setRequired(true);
        nameField.setWidthFull();

        TextArea descriptionField = new TextArea("Opis");
        descriptionField.setWidthFull();

        serviceBinder.removeBean();
        serviceBinder.forField(nameField).bind("name");
        serviceBinder.forField(descriptionField).bind("description");
        serviceBinder.readBean(serviceToEdit);

        Grid<JtuxedoServiceField> fieldsGrid = new Grid<>(JtuxedoServiceField.class, false);
        fieldsGrid.addColumn(JtuxedoServiceField::getName).setHeader("Nazwa").setWidth("180px");
        fieldsGrid.addColumn(f -> f.getDataType().getDisplayName()).setHeader("Typ").setWidth("100px");
        fieldsGrid.addColumn(JtuxedoServiceField::getMaxLength).setHeader("Max długość").setWidth("110px");
        fieldsGrid.addColumn(f -> f.getCardinality().getDisplayName()).setHeader("Liczeń").setWidth("80px");
        fieldsGrid.addColumn(JtuxedoServiceField::getDefaultValue).setHeader("Wartość domyślna").setWidth("140px").setFlexGrow(0);
        fieldsGrid.addComponentColumn(f -> {
            Button editF = new Button("Edytuj", ev -> openFieldDialog(serviceToEdit, f, currentFields, fieldsGrid));
            Button delF = new Button("Usuń", ev -> {
                currentFields.remove(f);
                fieldsGrid.getDataProvider().refreshAll();
            });
            delF.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editF, delF);
        }).setHeader("Akcje").setWidth("160px");
        fieldsGrid.setWidthFull();
        fieldsGrid.setHeight("220px");
        fieldsGrid.setItems(currentFields);

        Button addFieldBtn = new Button("Dodaj pole", e -> openFieldDialog(serviceToEdit, null, currentFields, fieldsGrid));
        addFieldBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        VerticalLayout fieldsSection = new VerticalLayout();
        fieldsSection.add(new com.vaadin.flow.component.html.H3("Pola"));
        fieldsSection.add(addFieldBtn);
        fieldsSection.add(fieldsGrid);
        fieldsSection.setWidthFull();

        FormLayout form = new FormLayout(nameField, descriptionField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        VerticalLayout content = new VerticalLayout(form, fieldsSection);
        content.setSpacing(true);

        Button saveBtn = new Button("Zapisz", e -> {
            try {
                serviceBinder.writeBean(serviceToEdit);
                configService.saveService(serviceToEdit);
                if (!isNew) {
                    configService.findFieldsByServiceId(serviceToEdit.getId()).stream()
                            .map(JtuxedoServiceField::getId)
                            .forEach(configService::deleteFieldById);
                }
                for (int i = 0; i < currentFields.size(); i++) {
                    JtuxedoServiceField f = currentFields.get(i);
                    f.setId(null);
                    f.setJtuxedoService(serviceToEdit);
                    f.setSortOrder(i);
                    configService.saveField(f);
                }
                refreshGrid();
                dialog.close();
                Notification.show("Usługa zapisana", 2000, Notification.Position.MIDDLE);
            } catch (ValidationException ex) {
                String msg = ex.getValidationErrors().stream()
                        .map(ve -> ve.getErrorMessage())
                        .findFirst()
                        .orElse("Uzupełnij wymagane pola (nazwa).");
                Notification.show(msg, 3000, Notification.Position.MIDDLE);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Anuluj", e -> dialog.close());

        dialog.add(content);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void openFieldDialog(JtuxedoService service, JtuxedoServiceField existing,
                                 List<JtuxedoServiceField> currentFields, Grid<JtuxedoServiceField> fieldsGrid) {
        JtuxedoServiceField fieldToEdit = new JtuxedoServiceField();
        boolean isNewField = (existing == null);
        if (existing != null) {
            fieldToEdit.setId(existing.getId());
            fieldToEdit.setName(existing.getName());
            fieldToEdit.setDataType(existing.getDataType());
            fieldToEdit.setMaxLength(existing.getMaxLength());
            fieldToEdit.setCardinality(existing.getCardinality());
            fieldToEdit.setDefaultValue(existing.getDefaultValue());
        }
        if (isNewField) {
            fieldToEdit.setDataType(FieldDataType.STRING);
            fieldToEdit.setCardinality(FieldCardinality.ONE_TO_ONE);
        }

        Dialog fd = new Dialog();
        fd.setHeaderTitle(isNewField ? "Nowe pole" : "Edycja pola");
        fd.setWidth("400px");

        TextField nameField = new TextField("Nazwa pola");
        nameField.setRequired(true);
        nameField.setWidthFull();

        Select<FieldDataType> typeSelect = new Select<>();
        typeSelect.setLabel("Typ danych");
        typeSelect.setItems(FieldDataType.values());
        typeSelect.setItemLabelGenerator(FieldDataType::getDisplayName);
        typeSelect.setWidthFull();

        IntegerField maxLengthField = new IntegerField("Maks. długość");
        maxLengthField.setMin(0);
        maxLengthField.setWidthFull();

        TextField defaultValueField = new TextField("Wartość domyślna (stała)");
        defaultValueField.setWidthFull();
        defaultValueField.setPlaceholder("Podpowiedź przy wywołaniu usługi");

        Select<FieldCardinality> cardinalitySelect = new Select<>();
        cardinalitySelect.setLabel("Liczeń");
        cardinalitySelect.setItems(FieldCardinality.values());
        cardinalitySelect.setItemLabelGenerator(FieldCardinality::getDisplayName);
        cardinalitySelect.setWidthFull();

        BeanValidationBinder<JtuxedoServiceField> fieldBinder = new BeanValidationBinder<>(JtuxedoServiceField.class);
        fieldBinder.forField(nameField).bind("name");
        fieldBinder.forField(typeSelect).bind("dataType");
        fieldBinder.forField(maxLengthField).bind("maxLength");
        fieldBinder.forField(defaultValueField).bind("defaultValue");
        fieldBinder.forField(cardinalitySelect).bind("cardinality");
        fieldBinder.readBean(fieldToEdit);

        FormLayout form = new FormLayout(nameField, typeSelect, maxLengthField, defaultValueField, cardinalitySelect);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button saveFieldBtn = new Button("Zapisz", e -> {
            try {
                fieldBinder.writeBean(fieldToEdit);
                if (isNewField) {
                    currentFields.add(fieldToEdit);
                } else if (existing != null) {
                    int idx = currentFields.indexOf(existing);
                    if (idx >= 0) {
                        currentFields.set(idx, fieldToEdit);
                    }
                }
                fieldsGrid.getDataProvider().refreshAll();
                fd.close();
            } catch (ValidationException ex) {
                String msg = ex.getValidationErrors().stream()
                        .map(ve -> ve.getErrorMessage())
                        .findFirst()
                        .orElse("Uzupełnij wymagane pola.");
                Notification.show(msg, 3000, Notification.Position.MIDDLE);
            }
        });
        saveFieldBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        fd.add(form);
        fd.getFooter().add(new Button("Anuluj", ev -> fd.close()), saveFieldBtn);
        fd.open();
    }

    private void openImportXmlDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Import definicji usług z pliku XML");
        dialog.setWidth("600px");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".xml", "application/xml", "text/xml");
        upload.setMaxFileSize(5 * 1024 * 1024);
        upload.setWidthFull();

        TextField searchField = new TextField();
        searchField.setPlaceholder("Szukaj po nazwie lub opisie...");
        searchField.setClearButtonVisible(true);
        searchField.setWidthFull();
        searchField.setVisible(false);

        Grid<ParsedServiceDefinition> servicesGrid = new Grid<>(ParsedServiceDefinition.class, false);
        servicesGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        servicesGrid.addColumn(ParsedServiceDefinition::getName).setHeader("Nazwa").setWidth("200px");
        servicesGrid.addColumn(ParsedServiceDefinition::getDescription).setHeader("Opis").setFlexGrow(1);
        servicesGrid.addColumn(p -> String.valueOf(p.getInputFields().size())).setHeader("Pola wejściowe").setWidth("120px");
        servicesGrid.setWidthFull();
        servicesGrid.setHeight("280px");
        servicesGrid.setVisible(false);

        AtomicReference<List<ParsedServiceDefinition>> parsedListRef = new AtomicReference<>(List.of());

        Runnable applyFilter = () -> {
            List<ParsedServiceDefinition> full = parsedListRef.get();
            String q = searchField.getValue();
            if (q == null || q.isBlank()) {
                servicesGrid.setItems(full);
            } else {
                String lower = q.trim().toLowerCase();
                List<ParsedServiceDefinition> filtered = full.stream()
                        .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(lower))
                                || (p.getDescription() != null && p.getDescription().toLowerCase().contains(lower)))
                        .toList();
                servicesGrid.setItems(filtered);
            }
        };

        searchField.addValueChangeListener(e -> applyFilter.run());

        upload.addSucceededListener(event -> {
            try (InputStream is = buffer.getInputStream()) {
                String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                List<ParsedServiceDefinition> list = xmlParser.parse(xml);
                parsedListRef.set(list);
                searchField.setValue("");
                searchField.setVisible(!list.isEmpty());
                applyFilter.run();
                servicesGrid.setVisible(!list.isEmpty());
                if (list.isEmpty()) {
                    Notification.show("W pliku nie znaleziono żadnych definicji usług (element <service>).", 4000, Notification.Position.MIDDLE);
                }
            } catch (Exception ex) {
                parsedListRef.set(List.of());
                searchField.setVisible(false);
                servicesGrid.setItems(List.of());
                servicesGrid.setVisible(false);
                Notification.show(ex.getMessage() != null ? ex.getMessage() : "Błąd odczytu pliku XML", 5000, Notification.Position.MIDDLE);
            }
        });

        Button importBtn = new Button("Importuj wybraną", e -> {
            ParsedServiceDefinition selected = servicesGrid.getSelectedItems().stream().findFirst().orElse(null);
            if (selected == null) {
                Notification.show("Wybierz usługę z listy.", 3000, Notification.Position.MIDDLE);
                return;
            }
            boolean exists = configService.findServiceByName(selected.getName()).isPresent();
            if (exists) {
                Optional<ServiceImportDiff> diffOpt = configService.getImportDiff(selected.getName(), selected);
                Dialog diffDialog = new Dialog();
                diffDialog.setHeaderTitle("Usługa \"" + selected.getName() + "\" już istnieje – podgląd zmian");
                diffDialog.setWidth("520px");

                VerticalLayout diffContent = new VerticalLayout();
                diffContent.setSpacing(true);
                diffContent.setPadding(false);
                if (diffOpt.isPresent()) {
                    ServiceImportDiff diff = diffOpt.get();
                    if (diff.hasAnyChanges()) {
                        if (diff.isDescriptionChanged()) {
                            diffContent.add(new com.vaadin.flow.component.html.H4("Opis"));
                            diffContent.add(new com.vaadin.flow.component.html.Span("Obecny: " + (diff.getDescriptionOld().isBlank() ? "—" : diff.getDescriptionOld())));
                            diffContent.add(new com.vaadin.flow.component.html.Span("Z pliku: " + (diff.getDescriptionNew().isBlank() ? "—" : diff.getDescriptionNew())));
                        }
                        if (!diff.getAddedFieldNames().isEmpty()) {
                            diffContent.add(new com.vaadin.flow.component.html.H4("Pola dodane"));
                            diffContent.add(new com.vaadin.flow.component.html.Span(String.join(", ", diff.getAddedFieldNames())));
                        }
                        if (!diff.getRemovedFieldNames().isEmpty()) {
                            diffContent.add(new com.vaadin.flow.component.html.H4("Pola usunięte"));
                            diffContent.add(new com.vaadin.flow.component.html.Span(String.join(", ", diff.getRemovedFieldNames())));
                        }
                        if (!diff.getModifiedFieldDescriptions().isEmpty()) {
                            diffContent.add(new com.vaadin.flow.component.html.H4("Pola zmienione"));
                            diff.getModifiedFieldDescriptions().forEach(s -> diffContent.add(new com.vaadin.flow.component.html.Span(s)));
                        }
                    } else {
                        diffContent.add(new com.vaadin.flow.component.html.Span("Brak różnic względem obecnej definicji."));
                    }
                }
                diffContent.add(new com.vaadin.flow.component.html.Hr());
                diffContent.add(new com.vaadin.flow.component.html.Span("Czy nadpisać definicję usługi?"));
                Button overwriteBtn = new Button("Nadpisz", ev -> {
                    configService.importFromParsed(selected, true);
                    refreshGrid();
                    diffDialog.close();
                    dialog.close();
                    Notification.show("Usługa \"" + selected.getName() + "\" została nadpisana.", 2000, Notification.Position.MIDDLE);
                });
                overwriteBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                Button cancelDiffBtn = new Button("Anuluj", ev2 -> diffDialog.close());
                diffDialog.add(diffContent);
                diffDialog.getFooter().add(cancelDiffBtn, overwriteBtn);
                diffDialog.open();
            } else {
                configService.importFromParsed(selected, false);
                refreshGrid();
                dialog.close();
                Notification.show("Usługa \"" + selected.getName() + "\" została zaimportowana.", 2000, Notification.Position.MIDDLE);
            }
        });
        importBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout content = new VerticalLayout();
        content.add(upload);
        content.add(searchField);
        content.add(servicesGrid);
        content.setWidthFull();
        dialog.add(content);
        dialog.getFooter().add(new Button("Zamknij", ev -> dialog.close()), importBtn);
        dialog.open();
    }

    private void deleteService(JtuxedoService service) {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Potwierdzenie");
        confirm.add("Usunąć usługę \"" + service.getName() + "\" i wszystkie jej pola?");
        Button deleteBtn = new Button("Usuń", e -> {
            configService.deleteServiceById(service.getId());
            refreshGrid();
            confirm.close();
            Notification.show("Usługa usunięta", 2000, Notification.Position.MIDDLE);
        });
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        confirm.getFooter().add(new Button("Anuluj", e -> confirm.close()), deleteBtn);
        confirm.open();
    }
}
