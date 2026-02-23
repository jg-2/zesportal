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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import pl.pekao.zesportal.entity.TaskTemplate;
import pl.pekao.zesportal.entity.TaskTemplate.TaskTemplateType;
import pl.pekao.zesportal.service.TaskTemplateService;

@Route(value = "tasks/templates", layout = MainLayout.class)
@PageTitle("Task templates | Zesportal")
public class SzablonyZadanView extends VerticalLayout {

    private final TaskTemplateService templateService;
    private final Grid<TaskTemplate> grid;
    private final BeanValidationBinder<TaskTemplate> binder = new BeanValidationBinder<>(TaskTemplate.class);

    public SzablonyZadanView(TaskTemplateService templateService) {
        this.templateService = templateService;
        this.grid = new Grid<>(TaskTemplate.class, false);

        setPadding(true);
        setSpacing(true);
        setSizeFull();

        add(createHeader());
        add(createGrid());
        refreshGrid();
    }

    private HorizontalLayout createHeader() {
        H2 title = new H2("Szablony zadań");
        Button addBtn = new Button("Dodaj szablon", e -> openDialog(new TaskTemplate()));
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout header = new HorizontalLayout(title, addBtn);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        return header;
    }

    private Grid<TaskTemplate> createGrid() {
        grid.addColumn(TaskTemplate::getName).setHeader("Nazwa").setWidth("200px");
        grid.addColumn(TaskTemplate::getDescription).setHeader("Opis");
        grid.addColumn(t -> t.getType().getDisplayName()).setHeader("Typ").setWidth("120px").setFlexGrow(0);
        grid.addColumn(TaskTemplate::getConfig).setHeader("Konfiguracja (JSON)").setFlexGrow(1);
        grid.addComponentColumn(t -> {
            Button editBtn = new Button("Edytuj", e -> openDialog(t));
            Button deleteBtn = new Button("Usuń", e -> deleteTemplate(t));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("Akcje").setWidth("160px").setFlexGrow(0);

        grid.setWidthFull();
        grid.setHeight("600px");
        return grid;
    }

    private void openDialog(TaskTemplate template) {
        boolean isNew = template.getId() == null;
        TaskTemplate toEdit = new TaskTemplate();
        if (!isNew) {
            toEdit.setId(template.getId());
            toEdit.setName(template.getName());
            toEdit.setDescription(template.getDescription());
            toEdit.setType(template.getType());
            toEdit.setConfig(template.getConfig());
        } else {
            toEdit.setType(TaskTemplateType.TUXEDO);
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "Nowy szablon" : "Edycja szablonu");
        dialog.setWidth("500px");

        TextField nameField = new TextField("Nazwa szablonu");
        nameField.setRequired(true);
        nameField.setWidthFull();

        TextArea descriptionField = new TextArea("Opis");
        descriptionField.setWidthFull();

        Select<TaskTemplateType> typeSelect = new Select<>();
        typeSelect.setLabel("Typ");
        typeSelect.setItems(TaskTemplateType.values());
        typeSelect.setItemLabelGenerator(TaskTemplateType::getDisplayName);
        typeSelect.setWidthFull();

        TextArea configField = new TextArea("Konfiguracja (JSON)");
        configField.setPlaceholder("np. {\"serviceId\": 1, \"serverId\": 2} dla Tuxedo\n{\"host\": \"host\", \"command\": \"ls\"} dla SSH");
        configField.setWidthFull();
        configField.setHeight("120px");

        binder.removeBean();
        binder.forField(nameField).bind("name");
        binder.forField(descriptionField).bind("description");
        binder.forField(typeSelect).bind("type");
        binder.forField(configField).bind("config");
        binder.readBean(toEdit);

        FormLayout form = new FormLayout(nameField, descriptionField, typeSelect, configField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button saveBtn = new Button("Zapisz", e -> {
            try {
                binder.writeBean(toEdit);
                templateService.save(toEdit);
                refreshGrid();
                dialog.close();
                Notification.show("Szablon zapisany", 2000, Notification.Position.MIDDLE);
            } catch (ValidationException ex) {
                Notification.show("Wypełnij nazwę szablonu", 3000, Notification.Position.MIDDLE);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(form);
        dialog.getFooter().add(new Button("Anuluj", ev -> dialog.close()), saveBtn);
        dialog.open();
    }

    private void deleteTemplate(TaskTemplate template) {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Potwierdzenie");
        confirm.add("Usunąć szablon \"" + template.getName() + "\"?");
        Button deleteBtn = new Button("Usuń", e -> {
            templateService.deleteById(template.getId());
            refreshGrid();
            confirm.close();
            Notification.show("Szablon usunięty", 2000, Notification.Position.MIDDLE);
        });
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        confirm.getFooter().add(new Button("Anuluj", ev -> confirm.close()), deleteBtn);
        confirm.open();
    }

    private void refreshGrid() {
        grid.setItems(templateService.findAll());
    }
}
