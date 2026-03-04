package pl.pekao.zesportal.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import pl.pekao.zesportal.entity.Task;
import pl.pekao.zesportal.entity.TaskTemplate;
import pl.pekao.zesportal.entity.TaskTemplate.TaskTemplateType;
import pl.pekao.zesportal.service.TaskService;
import pl.pekao.zesportal.service.TaskTemplateService;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "tasks/list", layout = MainLayout.class)
@PageTitle("Task list | Zesportal")
public class ListaZadanView extends VerticalLayout {

    private final TaskService taskService;
    private final TaskTemplateService taskTemplateService;
    private final Grid<Task> grid;

    public ListaZadanView(TaskService taskService, TaskTemplateService taskTemplateService) {
        this.taskService = taskService;
        this.taskTemplateService = taskTemplateService;
        this.grid = new Grid<>(Task.class, false);

        setPadding(true);
        setSpacing(true);
        setSizeFull();

        add(createHeader());
        add(createGrid());
        refreshGrid();

        addAttachListener(e -> {
            if (e.getUI() != null) {
                e.getUI().setPollInterval(2000);
                e.getUI().addPollListener(ev -> refreshGrid());
            }
        });
        addDetachListener(e -> {
            if (e.getUI() != null) {
                e.getUI().setPollInterval(-1);
            }
        });
    }

    private HorizontalLayout createHeader() {
        H2 title = new H2("Lista zadań");
        Button refreshBtn = new Button("Odśwież", VaadinIcon.REFRESH.create(), e -> {
            refreshGrid();
            Notification.show("Lista odświeżona", 1500, Notification.Position.BOTTOM_START);
        });
        Button newTaskBtn = new Button("Nowe zadanie", e -> openNewTaskDialog());
        newTaskBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout header = new HorizontalLayout(title, refreshBtn, newTaskBtn);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);
        return header;
    }

    private void openNewTaskDialog() {
        List<TaskTemplate> templates = taskTemplateService.findAll();
        Task taskToCreate = new Task();
        taskToCreate.setPriority(Task.TaskPriority.NORMAL);

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nowe zadanie");

        TextField nameField = new TextField("Nazwa");
        nameField.setRequired(true);
        nameField.setWidthFull();

        TextArea descriptionField = new TextArea("Opis");
        descriptionField.setWidthFull();

        Select<Task.TaskPriority> prioritySelect = new Select<>();
        prioritySelect.setLabel("Priorytet");
        prioritySelect.setItems(Task.TaskPriority.values());
        prioritySelect.setItemLabelGenerator(Task.TaskPriority::getDisplayName);
        prioritySelect.setValue(Task.TaskPriority.NORMAL);
        prioritySelect.setWidthFull();

        Select<TaskTemplate> templateSelect = new Select<>();
        templateSelect.setLabel("Szablon (opcjonalnie)");
        templateSelect.setItemLabelGenerator(t -> t == null ? "— brak —" : t.getName() + " (" + t.getType().getDisplayName() + ")");
        templateSelect.setEmptySelectionAllowed(true);
        templateSelect.setEmptySelectionCaption("— brak —");
        templateSelect.setWidthFull();
        templateSelect.setItems(templates);
        if (templates.isEmpty()) {
            templateSelect.setPlaceholder("Brak zdefiniowanych szablonów (dodaj w Szablony zadań)");
        }
        dialog.addOpenedChangeListener(e -> {
            if (e.isOpened()) {
                List<TaskTemplate> fresh = taskTemplateService.findAll();
                templateSelect.setItems(fresh);
            }
        });

        Checkbox saveResultCheckbox = new Checkbox("Zapisz pełny rezultat (JSON) po zakończeniu");
        saveResultCheckbox.setValue(false);
        saveResultCheckbox.setWidthFull();

        BeanValidationBinder<Task> binder = new BeanValidationBinder<>(Task.class);
        binder.forField(nameField).bind("name");
        binder.forField(descriptionField).bind("description");
        binder.forField(prioritySelect).bind("priority");
        binder.forField(templateSelect).bind("taskTemplate");
        binder.forField(saveResultCheckbox).bind("saveResult");
        binder.readBean(taskToCreate);

        FormLayout form = new FormLayout(nameField, descriptionField, prioritySelect, templateSelect, saveResultCheckbox);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button saveBtn = new Button("Utwórz i uruchom", e -> {
            try {
                binder.writeBean(taskToCreate);
                Task created = taskService.createTask(
                        taskToCreate.getName(),
                        taskToCreate.getDescription(),
                        taskToCreate.getPriority(),
                        taskToCreate.getTaskTemplate() != null ? taskToCreate.getTaskTemplate().getId() : null,
                        taskToCreate.getConfig(),
                        taskToCreate.getSaveResult()
                );
                taskService.executeTaskImmediately(created);
                refreshGrid();
                dialog.close();
                Notification.show("Zadanie utworzone i uruchomione", 2000, Notification.Position.MIDDLE);
            } catch (ValidationException ex) {
                Notification.show("Wypełnij nazwę zadania", 3000, Notification.Position.MIDDLE);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(form);
        dialog.getFooter().add(
                new Button("Anuluj", ev -> dialog.close()),
                new Button("Tylko do kolejki", ev -> {
                    try {
                        binder.writeBean(taskToCreate);
                        Task created = taskService.createTask(
                                taskToCreate.getName(),
                                taskToCreate.getDescription(),
                                taskToCreate.getPriority(),
                                taskToCreate.getTaskTemplate() != null ? taskToCreate.getTaskTemplate().getId() : null,
                                taskToCreate.getConfig(),
                                taskToCreate.getSaveResult()
                        );
                        taskService.addTaskToQueue(created);
                        refreshGrid();
                        dialog.close();
                        Notification.show("Zadanie dodane do kolejki", 2000, Notification.Position.MIDDLE);
                    } catch (ValidationException ex) {
                        Notification.show("Wypełnij nazwę zadania", 3000, Notification.Position.MIDDLE);
                    }
                }),
                saveBtn
        );
        dialog.open();
    }

    private Grid<Task> createGrid() {
        grid.addColumn(Task::getId).setHeader("ID").setWidth("70px").setFlexGrow(0);
        grid.addColumn(Task::getName).setHeader("Nazwa").setWidth("180px");
        grid.addColumn(Task::getDescription).setHeader("Opis");
        grid.addColumn(t -> t.getType() != null ? t.getType().getDisplayName() : (t.getTaskTemplate() != null ? t.getTaskTemplate().getType().getDisplayName() : "—"))
                .setHeader("Typ").setWidth("90px").setFlexGrow(0);
        grid.addColumn(t -> t.getTaskTemplate() != null ? t.getTaskTemplate().getName() : "-")
                .setHeader("Szablon").setWidth("140px").setFlexGrow(0);
        grid.addColumn(t -> t.getPriority().getDisplayName()).setHeader("Priorytet").setWidth("100px").setFlexGrow(0);
        grid.addComponentColumn(this::createStatusIcon).setHeader("").setWidth("36px").setFlexGrow(0);
        grid.addColumn(t -> t.getStatus().getDisplayName()).setHeader("Status").setWidth("100px").setFlexGrow(0);
        grid.addColumn(t -> t.getCreatedAt() != null ?
                t.getCreatedAt().toString().substring(0, 19).replace("T", " ") : "")
                .setHeader("Utworzono").setWidth("160px").setFlexGrow(0);
        grid.addColumn(t -> t.getCompletedAt() != null ?
                t.getCompletedAt().toString().substring(0, 19).replace("T", " ") : "")
                .setHeader("Zakończono").setWidth("160px").setFlexGrow(0);
        grid.addColumn(t -> truncateResult(t.getResult())).setHeader("Rezultat").setFlexGrow(1);

        grid.addItemDoubleClickListener(e -> openTaskDetailsDialog(e.getItem()));

        grid.setWidthFull();
        grid.setHeight("600px");
        return grid;
    }

    private static String truncateResult(String result) {
        if (result == null || result.isEmpty()) return "";
        int max = 120;
        if (result.length() <= max) return result;
        return result.substring(0, max) + "…";
    }

    private void openTaskDetailsDialog(Task task) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Szczegóły zadania: " + (task.getName() != null ? task.getName() : "—"));
        dialog.setWidth("700px");
        dialog.setHeight("85vh");

        FormLayout form = new FormLayout();
        TextField nameField = new TextField("Nazwa");
        nameField.setValue(task.getName() != null ? task.getName() : "");
        nameField.setReadOnly(true);
        nameField.setWidthFull();
        TextField typeField = new TextField("Typ");
        typeField.setValue(task.getType() != null ? task.getType().getDisplayName() : "—");
        typeField.setReadOnly(true);
        typeField.setWidthFull();
        TextField statusField = new TextField("Status");
        statusField.setValue(task.getStatus().getDisplayName());
        statusField.setReadOnly(true);
        statusField.setWidthFull();
        TextField priorityField = new TextField("Priorytet");
        priorityField.setValue(task.getPriority().getDisplayName());
        priorityField.setReadOnly(true);
        priorityField.setWidthFull();
        form.add(nameField, typeField, statusField, priorityField);

        String created = task.getCreatedAt() != null
                ? task.getCreatedAt().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : "—";
        String started = task.getStartedAt() != null
                ? task.getStartedAt().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : "—";
        String completed = task.getCompletedAt() != null
                ? task.getCompletedAt().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : "—";
        TextField createdField = new TextField("Utworzono");
        createdField.setValue(created);
        createdField.setReadOnly(true);
        createdField.setWidthFull();
        TextField startedField = new TextField("Start");
        startedField.setValue(started);
        startedField.setReadOnly(true);
        startedField.setWidthFull();
        TextField completedField = new TextField("Zakończono");
        completedField.setValue(completed);
        completedField.setReadOnly(true);
        completedField.setWidthFull();
        form.add(createdField, startedField, completedField);
        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            TextArea descArea = new TextArea("Opis", task.getDescription(), "");
            descArea.setReadOnly(true);
            descArea.setWidthFull();
            form.add(descArea);
        }
        if (task.getConfig() != null && !task.getConfig().isBlank()) {
            TextArea configArea = new TextArea("Config", task.getConfig(), "");
            configArea.setReadOnly(true);
            configArea.setWidthFull();
            configArea.setMaxHeight("80px");
            form.add(configArea);
        }

        TextArea resultArea = new TextArea("Rezultat", task.getResult() != null ? task.getResult() : "(brak)", "");
        resultArea.setReadOnly(true);
        resultArea.setWidthFull();
        resultArea.setMinHeight("200px");
        resultArea.getStyle().set("flex-grow", "1");

        Button saveToFileBtn = new Button("Zapisz do pliku", VaadinIcon.DOWNLOAD.create(), e -> {
            String config = "{\"sourceTaskId\": " + task.getId() + "}";
            Task saveTask = taskService.createTask(
                    "Zapis wyniku: " + (task.getName() != null ? task.getName() : "zadanie " + task.getId()),
                    "Zapis wyniku zadania " + task.getId() + " do pliku",
                    Task.TaskPriority.LOW,
                    null,
                    config,
                    false,
                    TaskTemplateType.SAVE_FILE
            );
            taskService.addTaskToQueue(saveTask);
            Notification.show("Zadanie zapisu do pliku dodane do kolejki (niski priorytet)", 3000, Notification.Position.MIDDLE);
            dialog.close();
        });
        saveToFileBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout content = new VerticalLayout();
        content.add(form);
        content.add(resultArea);
        content.setFlexGrow(1, resultArea);
        content.setSizeFull();
        content.setPadding(false);

        dialog.getFooter().add(saveToFileBtn, new Button("Zamknij", ev -> dialog.close()));
        dialog.add(content);
        dialog.open();
    }

    private HorizontalLayout createStatusIcon(Task task) {
        Icon icon;
        String color;
        String title;
        switch (task.getStatus()) {
            case PENDING -> {
                icon = VaadinIcon.CLOCK.create();
                color = "#64748b";
                title = "Oczekuje";
            }
            case RUNNING -> {
                icon = VaadinIcon.PLAY_CIRCLE.create();
                color = "#2563eb";
                title = "W trakcie";
            }
            case COMPLETED -> {
                icon = VaadinIcon.CHECK_CIRCLE.create();
                color = "#16a34a";
                title = "Zakończone";
            }
            case FAILED -> {
                icon = VaadinIcon.CLOSE_CIRCLE.create();
                color = "#dc2626";
                title = "Błąd";
            }
            default -> {
                icon = VaadinIcon.CIRCLE_THIN.create();
                color = "#94a3b8";
                title = task.getStatus().getDisplayName();
            }
        }
        icon.setSize("20px");
        icon.getStyle().set("color", color);
        icon.getElement().setAttribute("title", title);
        HorizontalLayout wrap = new HorizontalLayout(icon);
        wrap.setAlignItems(FlexComponent.Alignment.CENTER);
        wrap.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        wrap.setSpacing(false);
        wrap.setPadding(false);
        return wrap;
    }

    private void refreshGrid() {
        if (!isAttached() || getUI().isEmpty()) return;
        List<Task> items = taskService.findAll();
        grid.setItems(items);
        grid.getDataProvider().refreshAll();
    }
}
