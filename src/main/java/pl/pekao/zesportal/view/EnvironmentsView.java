package pl.pekao.zesportal.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import pl.pekao.zesportal.entity.Environment;
import pl.pekao.zesportal.service.EnvironmentService;

import java.util.Optional;

@Route(value = "configuration/environments", layout = MainLayout.class)
@PageTitle("Environments | Zesportal")
public class EnvironmentsView extends VerticalLayout {

    private final EnvironmentService environmentService;
    private final Grid<Environment> grid;
    private final BeanValidationBinder<Environment> binder;

    public EnvironmentsView(EnvironmentService environmentService) {
        this.environmentService = environmentService;
        this.binder = new BeanValidationBinder<>(Environment.class);
        this.grid = new Grid<>(Environment.class, false);

        setPadding(true);
        setSpacing(true);
        setSizeFull();

        add(createHeader());
        add(createGrid());
    }

    private HorizontalLayout createHeader() {
        H2 title = new H2("Environments");
        Button addButton = new Button("Add Environment", e -> openDialog(new Environment()));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout header = new HorizontalLayout(title, addButton);
        header.setWidthFull();
        header.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);

        return header;
    }

    private Grid<Environment> createGrid() {
        grid.addColumn(Environment::getCode).setHeader("Code").setWidth("150px").setFlexGrow(0);
        grid.addColumn(Environment::getName).setHeader("Name");
        grid.addColumn(Environment::getDescription).setHeader("Description");
        grid.addColumn(e -> e.getStatus().getDisplayName()).setHeader("Status").setWidth("120px").setFlexGrow(0);
        grid.addComponentColumn(env -> {
            Button editBtn = new Button("Edit", e -> openDialog(env));
            Button deleteBtn = new Button("Delete", e -> deleteEnvironment(env));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("Actions").setWidth("180px").setFlexGrow(0);

        grid.setItems(environmentService.findAll());
        grid.setWidthFull();
        grid.setHeight("600px");

        return grid;
    }

    private void openDialog(Environment environment) {
        // Tworzymy kopię obiektu do edycji, żeby nie modyfikować oryginału przed zapisem
        Environment environmentToEdit = new Environment();
        boolean isNew = environment.getId() == null;
        
        if (!isNew) {
            // Przy edycji kopiujemy dane z istniejącego obiektu
            environmentToEdit.setId(environment.getId());
            environmentToEdit.setCode(environment.getCode());
            environmentToEdit.setName(environment.getName());
            environmentToEdit.setDescription(environment.getDescription());
            environmentToEdit.setStatus(environment.getStatus());
            environmentToEdit.setCreatedAt(environment.getCreatedAt());
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "New Environment" : "Edit Environment");

        TextField codeField = new TextField("Code");
        codeField.setRequired(true);
        codeField.setEnabled(isNew); // Code cannot be changed when editing
        
        TextField nameField = new TextField("Name");
        nameField.setRequired(true);
        
        TextArea descriptionField = new TextArea("Description");
        descriptionField.setWidthFull();
        
        Select<Environment.EnvironmentStatus> statusSelect = new Select<>();
        statusSelect.setLabel("Status");
        statusSelect.setItems(Environment.EnvironmentStatus.values());
        statusSelect.setItemLabelGenerator(Environment.EnvironmentStatus::getDisplayName);
        // Note: Select component doesn't have setRequired() method in Vaadin 24
        // Validation is handled by the binder

        // Reset bindera przed użyciem
        binder.removeBean();
        binder.forField(codeField).bind("code");
        binder.forField(nameField).bind("name");
        binder.forField(descriptionField).bind("description");
        binder.forField(statusSelect).bind("status");
        binder.readBean(environmentToEdit);

        FormLayout formLayout = new FormLayout();
        formLayout.add(codeField, nameField, descriptionField, statusSelect);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        Button saveButton = new Button("Save", e -> {
            try {
                binder.writeBean(environmentToEdit);
                
                // Check code uniqueness
                if (isNew) {
                    if (environmentService.findByCode(environmentToEdit.getCode()).isPresent()) {
                        Notification.show("Environment with code " + environmentToEdit.getCode() + " already exists", 
                                3000, Notification.Position.MIDDLE);
                        return;
                    }
                } else {
                    // When editing, check if code is used by another environment
                    Optional<Environment> existing = environmentService.findByCode(environmentToEdit.getCode());
                    if (existing.isPresent() && !existing.get().getId().equals(environmentToEdit.getId())) {
                        Notification.show("Environment with code " + environmentToEdit.getCode() + " already exists", 
                                3000, Notification.Position.MIDDLE);
                        return;
                    }
                }
                
                environmentService.save(environmentToEdit);
                refreshGrid();
                dialog.close();
                Notification.show("Environment saved", 2000, Notification.Position.MIDDLE);
            } catch (ValidationException ex) {
                Notification.show("Please fill all required fields", 3000, Notification.Position.MIDDLE);
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", e -> dialog.close());

        dialog.add(formLayout);
        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private void deleteEnvironment(Environment environment) {
        Dialog confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Confirm Deletion");
        confirmDialog.add("Are you sure you want to delete environment: " + environment.getName() + "?");
        
        Button confirmButton = new Button("Delete", e -> {
            environmentService.deleteById(environment.getId());
            refreshGrid();
            confirmDialog.close();
            Notification.show("Environment deleted", 2000, Notification.Position.MIDDLE);
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        
        Button cancelButton = new Button("Cancel", e -> confirmDialog.close());
        
        confirmDialog.getFooter().add(cancelButton, confirmButton);
        confirmDialog.open();
    }

    private void refreshGrid() {
        grid.setItems(environmentService.findAll());
    }
}
