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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import pl.pekao.zesportal.entity.Server;
import pl.pekao.zesportal.service.ServerService;

@Route(value = "configuration/servers", layout = MainLayout.class)
@PageTitle("Servers | Zesportal")
public class ServersView extends VerticalLayout {

    private final ServerService serverService;
    private final Grid<Server> grid;
    private final BeanValidationBinder<Server> binder = new BeanValidationBinder<>(Server.class);

    public ServersView(ServerService serverService) {
        this.serverService = serverService;
        this.grid = new Grid<>(Server.class, false);

        setPadding(true);
        setSpacing(true);
        setSizeFull();

        add(createHeader());
        add(createGrid());
    }

    private HorizontalLayout createHeader() {
        H2 title = new H2("Servers");
        Button addButton = new Button("Add Server", e -> openDialog(new Server()));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout header = new HorizontalLayout(title, addButton);
        header.setWidthFull();
        header.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        return header;
    }

    private Grid<Server> createGrid() {
        grid.addColumn(Server::getName).setHeader("Name").setWidth("180px").setFlexGrow(0);
        grid.addColumn(Server::getAddress).setHeader("Address");
        grid.addColumn(Server::getDescription).setHeader("Description");
        grid.addComponentColumn(server -> {
            Button editBtn = new Button("Edit", e -> openDialog(server));
            Button deleteBtn = new Button("Delete", e -> deleteServer(server));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("Actions").setWidth("180px").setFlexGrow(0);

        grid.setItems(serverService.findAll());
        grid.setWidthFull();
        grid.setHeight("600px");
        return grid;
    }

    private void openDialog(Server server) {
        boolean isNew = server.getId() == null;
        Server serverToEdit = new Server();
        if (!isNew) {
            serverToEdit.setId(server.getId());
            serverToEdit.setName(server.getName());
            serverToEdit.setAddress(server.getAddress());
            serverToEdit.setDescription(server.getDescription());
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "New Server" : "Edit Server");

        TextField nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setWidthFull();

        TextField addressField = new TextField("Address");
        addressField.setRequired(true);
        addressField.setWidthFull();

        TextArea descriptionField = new TextArea("Description");
        descriptionField.setWidthFull();

        binder.removeBean();
        binder.forField(nameField).bind("name");
        binder.forField(addressField).bind("address");
        binder.forField(descriptionField).bind("description");
        binder.readBean(serverToEdit);

        FormLayout form = new FormLayout(nameField, addressField, descriptionField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button saveBtn = new Button("Save", e -> {
            try {
                binder.writeBean(serverToEdit);
                serverService.save(serverToEdit);
                refreshGrid();
                dialog.close();
                Notification.show("Server saved", 2000, Notification.Position.MIDDLE);
            } catch (ValidationException ex) {
                Notification.show("Fill required fields: Name, Address", 3000, Notification.Position.MIDDLE);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.add(form);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void deleteServer(Server server) {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Confirm Deletion");
        confirm.add("Delete server \"" + server.getName() + "\"?");
        Button deleteBtn = new Button("Delete", e -> {
            serverService.deleteById(server.getId());
            refreshGrid();
            confirm.close();
            Notification.show("Server deleted", 2000, Notification.Position.MIDDLE);
        });
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        confirm.getFooter().add(
                new Button("Cancel", e -> confirm.close()),
                deleteBtn
        );
        confirm.open();
    }

    private void refreshGrid() {
        grid.setItems(serverService.findAll());
    }
}
