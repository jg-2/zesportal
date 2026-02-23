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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import pl.pekao.zesportal.entity.SshKey;
import pl.pekao.zesportal.repository.SshKeyRepository;

@Route(value = "configuration/klucze-ssh", layout = MainLayout.class)
@PageTitle("Klucze SSH | Zesportal")
public class SshKeysView extends VerticalLayout {

    private final SshKeyRepository sshKeyRepository;
    private final Grid<SshKey> grid = new Grid<>(SshKey.class, false);

    public SshKeysView(SshKeyRepository sshKeyRepository) {
        this.sshKeyRepository = sshKeyRepository;
        setPadding(true);
        setSpacing(true);
        setSizeFull();

        H2 title = new H2("Klucze SSH");
        Button addButton = new Button("Dodaj klucz", e -> openDialog(null));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout header = new HorizontalLayout(title, addButton);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        grid.addColumn(SshKey::getName).setHeader("Nazwa").setWidth("200px");
        grid.addColumn(SshKey::getPathOrIdentifier).setHeader("Ścieżka / identyfikator");
        grid.addColumn(SshKey::getDescription).setHeader("Opis");
        grid.addComponentColumn(k -> {
            Button editBtn = new Button("Edytuj", ev -> openDialog(k));
            Button deleteBtn = new Button("Usuń", ev -> deleteKey(k));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("Akcje").setWidth("160px");
        grid.setWidthFull();
        grid.setHeight("600px");

        add(header);
        add(grid);
        refreshGrid();
    }

    private void refreshGrid() {
        grid.setItems(sshKeyRepository.findAllByOrderByNameAsc());
    }

    private void openDialog(SshKey existing) {
        boolean isNew = existing == null;
        SshKey toEdit = new SshKey();
        if (existing != null) {
            toEdit.setId(existing.getId());
            toEdit.setName(existing.getName());
            toEdit.setPathOrIdentifier(existing.getPathOrIdentifier());
            toEdit.setDescription(existing.getDescription());
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "Nowy klucz SSH" : "Edycja klucza");
        dialog.setWidth("400px");

        TextField nameField = new TextField("Nazwa");
        nameField.setRequired(true);
        nameField.setWidthFull();
        TextField pathField = new TextField("Ścieżka / identyfikator");
        pathField.setPlaceholder("np. ~/.ssh/id_rsa lub nazwa w agentcie");
        pathField.setWidthFull();
        TextArea descriptionField = new TextArea("Opis");
        descriptionField.setWidthFull();

        BeanValidationBinder<SshKey> binder = new BeanValidationBinder<>(SshKey.class);
        binder.forField(nameField).bind("name");
        binder.forField(pathField).bind("pathOrIdentifier");
        binder.forField(descriptionField).bind("description");
        binder.readBean(toEdit);

        FormLayout form = new FormLayout(nameField, pathField, descriptionField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button saveBtn = new Button("Zapisz", e -> {
            try {
                binder.writeBean(toEdit);
                sshKeyRepository.save(toEdit);
                refreshGrid();
                dialog.close();
                Notification.show("Klucz zapisany", 2000, Notification.Position.MIDDLE);
            } catch (ValidationException ex) {
                Notification.show("Wypełnij nazwę", 3000, Notification.Position.MIDDLE);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.add(form);
        dialog.getFooter().add(new Button("Anuluj", ev -> dialog.close()), saveBtn);
        dialog.open();
    }

    private void deleteKey(SshKey k) {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Potwierdzenie");
        confirm.add("Usunąć klucz \"" + k.getName() + "\"?");
        Button deleteBtn = new Button("Usuń", e -> {
            sshKeyRepository.delete(k);
            refreshGrid();
            confirm.close();
            Notification.show("Klucz usunięty", 2000, Notification.Position.MIDDLE);
        });
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        confirm.getFooter().add(new Button("Anuluj", ev -> confirm.close()), deleteBtn);
        confirm.open();
    }
}
