package pl.pekao.zesportal.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import pl.pekao.zesportal.entity.Environment;
import pl.pekao.zesportal.entity.Server;
import pl.pekao.zesportal.entity.ServerService;
import pl.pekao.zesportal.entity.ServerService.ServiceType;
import pl.pekao.zesportal.entity.SshKey;
import pl.pekao.zesportal.repository.SshKeyRepository;
import pl.pekao.zesportal.service.EnvironmentService;
import pl.pekao.zesportal.service.ServerServiceManagementService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Route(value = "configuration/komponenty-systemowe", layout = MainLayout.class)
@PageTitle("Komponenty systemowe | Zesportal")
public class ServerServicesView extends VerticalLayout {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ServerServiceManagementService serviceManagement;
    private final pl.pekao.zesportal.service.ServerService serverService;
    private final EnvironmentService environmentService;
    private final SshKeyRepository sshKeyRepository;
    private final Grid<ServerService> grid;

    public ServerServicesView(ServerServiceManagementService serviceManagement,
                              pl.pekao.zesportal.service.ServerService serverService,
                              EnvironmentService environmentService,
                              SshKeyRepository sshKeyRepository) {
        this.serviceManagement = serviceManagement;
        this.serverService = serverService;
        this.environmentService = environmentService;
        this.sshKeyRepository = sshKeyRepository;
        this.grid = new Grid<>(ServerService.class, false);

        setPadding(true);
        setSpacing(true);
        setSizeFull();

        add(createHeader());
        add(createGrid());
        refreshGrid();
    }

    private HorizontalLayout createHeader() {
        H2 title = new H2("Komponenty systemowe");
        Button addButton = new Button("Dodaj komponent", e -> openDialog(null));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout header = new HorizontalLayout(title, addButton);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        return header;
    }

    private Grid<ServerService> createGrid() {
        grid.addColumn(ss -> ss.getServer() != null ? ss.getServer().getName() : "")
                .setHeader("Serwer").setWidth("140px").setFlexGrow(0);
        grid.addColumn(ss -> ss.getEnvironment() != null ? ss.getEnvironment().getCode() : "")
                .setHeader("Środowisko").setWidth("120px").setFlexGrow(0);
        grid.addColumn(ServerService::getName).setHeader("Nazwa").setWidth("180px");
        grid.addColumn(ss -> ss.getType().getDisplayName()).setHeader("Typ").setWidth("100px").setFlexGrow(0);
        grid.addColumn(this::configSummary).setHeader("Konfiguracja").setFlexGrow(1);
        grid.addColumn(ServerService::getDescription).setHeader("Opis");
        grid.addComponentColumn(ss -> {
            Button editBtn = new Button("Edytuj", e -> openDialog(ss));
            Button deleteBtn = new Button("Usuń", e -> deleteService(ss));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("Akcje").setWidth("180px").setFlexGrow(0);

        grid.setWidthFull();
        grid.setHeight("600px");
        return grid;
    }

    private String configSummary(ServerService ss) {
        if (ss.getType() == ServiceType.TUXEDO) {
            Integer p = ss.getPort();
            if (p != null) return "port " + p + (ss.getPoolMin() != null || ss.getPoolMax() != null ? ", pool " + nullToEmpty(ss.getPoolMin()) + "-" + nullToEmpty(ss.getPoolMax()) : "");
            return fromConfig(ss, "port", "port %s");
        }
        if (ss.getType() == ServiceType.SSH) {
            if (ss.getUsername() != null && !ss.getUsername().isBlank()) return "user " + ss.getUsername();
            if (ss.getSshKey() != null) return "klucz " + ss.getSshKey().getName();
            return fromConfig(ss, "username", "user %s");
        }
        if (ss.getType() == ServiceType.DB) {
            String url = fromConfigKey(ss.getConfig(), "jdbcUrl");
            if (url != null && !url.isBlank()) {
                if (url.length() > 60) url = url.substring(0, 57) + "...";
                return url;
            }
            return "JDBC (skonfiguruj)";
        }
        return "—";
    }

    private static String nullToEmpty(Object o) {
        return o != null ? o.toString() : "";
    }

    private String fromConfig(ServerService ss, String key, String format) {
        if (ss.getConfig() == null || ss.getConfig().isBlank()) return "—";
        try {
            JsonNode n = JSON.readTree(ss.getConfig());
            if (n.has(key)) return String.format(format, n.get(key).asText());
        } catch (Exception ignored) {
        }
        return ss.getConfig();
    }

    private static String fromConfigKey(String config, String key) {
        if (config == null || config.isBlank()) return null;
        try {
            JsonNode n = JSON.readTree(config);
            return n.has(key) ? n.get(key).asText() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildDbConfig(String jdbcUrl, String driverClassName, String username, String password,
                                  String databaseVendor, String viewNameRegex) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (jdbcUrl != null && !jdbcUrl.isBlank()) sb.append("\"jdbcUrl\":\"").append(escapeJson(jdbcUrl)).append("\"");
        if (driverClassName != null && !driverClassName.isBlank()) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"driverClassName\":\"").append(escapeJson(driverClassName)).append("\"");
        }
        if (username != null && !username.isBlank()) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"username\":\"").append(escapeJson(username)).append("\"");
        }
        if (password != null && !password.isBlank()) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"password\":\"").append(escapeJson(password)).append("\"");
        }
        if (databaseVendor != null && !databaseVendor.isBlank()) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"databaseVendor\":\"").append(escapeJson(databaseVendor)).append("\"");
        }
        if (viewNameRegex != null && !viewNameRegex.isBlank()) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"viewNameRegex\":\"").append(escapeJson(viewNameRegex)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String buildTuxedoConfig(Integer port, Integer poolMin, Integer poolMax, String tuxedoUserName) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (port != null) sb.append("\"port\":").append(port);
        if (poolMin != null) { if (sb.length() > 1) sb.append(","); sb.append("\"poolMin\":").append(poolMin); }
        if (poolMax != null) { if (sb.length() > 1) sb.append(","); sb.append("\"poolMax\":").append(poolMax); }
        if (tuxedoUserName != null && !tuxedoUserName.isBlank()) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"tuxedoUserName\":\"").append(tuxedoUserName.replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String buildSshConfig(String username, SshKey sshKey) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (username != null && !username.isBlank()) sb.append("\"username\":\"").append(username.replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
        if (sshKey != null && sshKey.getId() != null) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"sshKeyId\":").append(sshKey.getId());
        }
        sb.append("}");
        return sb.toString();
    }

    private void openDialog(ServerService existing) {
        boolean isNew = existing == null;
        ServerService toEdit = new ServerService();
        List<Server> servers = serverService.findAll();
        List<Environment> environments = environmentService.findAll();

        if (existing != null) {
            toEdit.setId(existing.getId());
            toEdit.setName(existing.getName());
            toEdit.setType(existing.getType());
            toEdit.setPort(existing.getPort());
            toEdit.setUsername(existing.getUsername());
            toEdit.setPoolMin(existing.getPoolMin());
            toEdit.setPoolMax(existing.getPoolMax());
            toEdit.setTuxedoUserName(existing.getTuxedoUserName());
            toEdit.setTuxedoNoAuth(existing.getTuxedoNoAuth());
            toEdit.setTuxedoPassword(existing.getTuxedoPassword());
            toEdit.setSshKey(existing.getSshKey());
            toEdit.setConfig(existing.getConfig());
            toEdit.setDescription(existing.getDescription());
            if (existing.getServer() != null && existing.getServer().getId() != null) {
                servers.stream()
                        .filter(s -> s.getId().equals(existing.getServer().getId()))
                        .findFirst()
                        .ifPresent(toEdit::setServer);
            }
            if (existing.getEnvironment() != null && existing.getEnvironment().getId() != null) {
                environments.stream()
                        .filter(e -> e.getId().equals(existing.getEnvironment().getId()))
                        .findFirst()
                        .ifPresent(toEdit::setEnvironment);
            }
        }
        if (existing == null) {
            toEdit.setType(ServiceType.TUXEDO);
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(isNew ? "Nowy komponent" : "Edycja komponentu");
        dialog.setWidth("480px");

        Select<Environment> environmentSelect = new Select<>();
        environmentSelect.setLabel("Środowisko");
        environmentSelect.setItems(environments);
        environmentSelect.setItemLabelGenerator(e -> e.getName() + " (" + e.getCode() + ")");
        environmentSelect.setWidthFull();
        if (toEdit.getEnvironment() != null) {
            environments.stream().filter(e -> e.getId().equals(toEdit.getEnvironment().getId())).findFirst().ifPresent(environmentSelect::setValue);
        }

        Select<Server> serverSelect = new Select<>();
        serverSelect.setLabel("Serwer");
        serverSelect.setItems(servers);
        serverSelect.setItemLabelGenerator(Server::getName);
        serverSelect.setWidthFull();
        if (toEdit.getServer() != null) {
            servers.stream().filter(s -> s.getId().equals(toEdit.getServer().getId())).findFirst().ifPresent(serverSelect::setValue);
        }

        TextField nameField = new TextField("Nazwa komponentu");
        nameField.setRequired(true);
        nameField.setWidthFull();

        Select<ServiceType> typeSelect = new Select<>();
        typeSelect.setLabel("Typ");
        typeSelect.setItems(ServiceType.values());
        typeSelect.setItemLabelGenerator(ServiceType::getDisplayName);
        typeSelect.setWidthFull();

        IntegerField portField = new IntegerField("Port");
        portField.setMin(1);
        portField.setMax(65535);
        portField.setWidthFull();
        portField.setVisible(toEdit.getType() == ServiceType.TUXEDO);

        TextField usernameField = new TextField("Użytkownik");
        usernameField.setPlaceholder("np. tuxmae1, tuxmae2");
        usernameField.setWidthFull();
        usernameField.setVisible(toEdit.getType() == ServiceType.SSH);

        IntegerField poolMinField = new IntegerField("poolMin (Tuxedo)");
        poolMinField.setMin(0);
        poolMinField.setWidthFull();
        poolMinField.setVisible(toEdit.getType() == ServiceType.TUXEDO);

        IntegerField poolMaxField = new IntegerField("poolMax (Tuxedo)");
        poolMaxField.setMin(0);
        poolMaxField.setWidthFull();
        poolMaxField.setVisible(toEdit.getType() == ServiceType.TUXEDO);

        TextField tuxedoUserNameField = new TextField("User Name (Tuxedo)");
        tuxedoUserNameField.setWidthFull();
        tuxedoUserNameField.setVisible(toEdit.getType() == ServiceType.TUXEDO);

        Checkbox tuxedoNoAuthCheckbox = new Checkbox("Autentykacja bez hasła (NOAUTH)", Boolean.TRUE.equals(toEdit.getTuxedoNoAuth()));
        tuxedoNoAuthCheckbox.setWidthFull();
        tuxedoNoAuthCheckbox.setVisible(toEdit.getType() == ServiceType.TUXEDO);

        PasswordField tuxedoPasswordField = new PasswordField("Hasło (Tuxedo)");
        tuxedoPasswordField.setWidthFull();
        tuxedoPasswordField.setPlaceholder("Opcjonalnie, gdy wyłączona autentykacja bez hasła");
        tuxedoPasswordField.setVisible(toEdit.getType() == ServiceType.TUXEDO);
        tuxedoPasswordField.setEnabled(!Boolean.TRUE.equals(toEdit.getTuxedoNoAuth()));

        tuxedoNoAuthCheckbox.addValueChangeListener(e -> {
            boolean noAuth = Boolean.TRUE.equals(e.getValue());
            tuxedoPasswordField.setEnabled(!noAuth);
            if (noAuth) tuxedoPasswordField.clear();
        });

        List<SshKey> sshKeys = sshKeyRepository.findAllByOrderByNameAsc();
        Select<SshKey> sshKeySelect = new Select<>();
        sshKeySelect.setLabel("Klucz do podłączenia sesji (SSH)");
        sshKeySelect.setItems(sshKeys);
        sshKeySelect.setItemLabelGenerator(k -> k.getName() + (k.getPathOrIdentifier() != null && !k.getPathOrIdentifier().isBlank() ? " — " + k.getPathOrIdentifier() : ""));
        sshKeySelect.setWidthFull();
        sshKeySelect.setVisible(toEdit.getType() == ServiceType.SSH);

        Select<DatabaseVendor> dbVendorSelect = new Select<>();
        dbVendorSelect.setLabel("Typ bazy danych");
        dbVendorSelect.setItems(DatabaseVendor.values());
        dbVendorSelect.setItemLabelGenerator(DatabaseVendor::getDisplayName);
        dbVendorSelect.setWidthFull();
        dbVendorSelect.setVisible(toEdit.getType() == ServiceType.DB);

        TextField jdbcUrlField = new TextField("URL JDBC");
        jdbcUrlField.setWidthFull();
        jdbcUrlField.setPlaceholder("jdbc:informix-sqli://...");
        jdbcUrlField.setVisible(toEdit.getType() == ServiceType.DB);

        TextField driverClassField = new TextField("Klasa sterownika");
        driverClassField.setWidthFull();
        driverClassField.setPlaceholder("com.informix.jdbc.IfxDriver");
        driverClassField.setVisible(toEdit.getType() == ServiceType.DB);

        TextField jdbcUsernameField = new TextField("Użytkownik (JDBC)");
        jdbcUsernameField.setWidthFull();
        jdbcUsernameField.setVisible(toEdit.getType() == ServiceType.DB);

        PasswordField jdbcPasswordField = new PasswordField("Hasło (JDBC)");
        jdbcPasswordField.setWidthFull();
        jdbcPasswordField.setVisible(toEdit.getType() == ServiceType.DB);

        TextField viewNameRegexField = new TextField("Regex dla widoków");
        viewNameRegexField.setWidthFull();
        viewNameRegexField.setPlaceholder("np. ^V_.* – tylko widoki z zadanym prefixem (puste = bez ograniczeń)");
        viewNameRegexField.setHelperText("Aplikacja pozwala wywoływać tylko view pasujące do tego wzorca.");
        viewNameRegexField.setVisible(toEdit.getType() == ServiceType.DB);

        Button testConnectionButton = new Button("Test połączenia");
        testConnectionButton.setVisible(toEdit.getType() == ServiceType.DB);
        testConnectionButton.addClickListener(ev -> {
            String url = jdbcUrlField.getValue();
            String driver = driverClassField.getValue();
            String user = jdbcUsernameField.getValue();
            String pwd = jdbcPasswordField.getValue();
            if (pwd == null || pwd.isBlank()) pwd = existing != null ? fromConfigKey(existing.getConfig(), "password") : null;
            if (url == null || url.isBlank()) {
                Notification.show("Podaj URL JDBC", 3000, Notification.Position.MIDDLE);
                return;
            }
            testConnectionButton.setEnabled(false);
            String finalPwd = pwd;
            UI ui = UI.getCurrent();
            ClassLoader appClassLoader = getClass().getClassLoader();
            new Thread(() -> {
                String result;
                Thread currentThread = Thread.currentThread();
                ClassLoader previousCcl = currentThread.getContextClassLoader();
                try {
                    currentThread.setContextClassLoader(appClassLoader);
                    if (driver != null && !driver.isBlank()) {
                        try {
                            Class.forName(driver, true, appClassLoader);
                        } catch (ClassNotFoundException e) {
                            if (ui != null) ui.access(() -> {
                                Notification.show("Sterownik nie znaleziony: " + driver, 5000, Notification.Position.MIDDLE);
                                testConnectionButton.setEnabled(true);
                            });
                            return;
                        }
                    }
                    try (Connection c = DriverManager.getConnection(url, user != null ? user : "", finalPwd != null ? finalPwd : "")) {
                        result = "Połączenie OK";
                    }
                } catch (Exception ex) {
                    result = "Błąd: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
                } finally {
                    currentThread.setContextClassLoader(previousCcl);
                }
                String finalResult = result;
                if (ui != null) ui.access(() -> {
                    Notification.show(finalResult, finalResult.startsWith("Błąd") ? 6000 : 3000, Notification.Position.MIDDLE);
                    testConnectionButton.setEnabled(true);
                });
            }).start();
        });

        dbVendorSelect.addValueChangeListener(e -> {
            DatabaseVendor v = e.getValue();
            if (v != null) {
                jdbcUrlField.setPlaceholder(v.getUrlPlaceholder());
                driverClassField.setValue(v.getDefaultDriver() != null ? v.getDefaultDriver() : "");
            }
        });

        typeSelect.addValueChangeListener(e -> {
            ServiceType t = e.getValue();
            boolean tuxedo = t == ServiceType.TUXEDO;
            boolean ssh = t == ServiceType.SSH;
            boolean db = t == ServiceType.DB;
            portField.setVisible(tuxedo);
            usernameField.setVisible(ssh);
            poolMinField.setVisible(tuxedo);
            poolMaxField.setVisible(tuxedo);
            tuxedoUserNameField.setVisible(tuxedo);
            tuxedoNoAuthCheckbox.setVisible(tuxedo);
            tuxedoPasswordField.setVisible(tuxedo);
            if (tuxedo) tuxedoPasswordField.setEnabled(!Boolean.TRUE.equals(tuxedoNoAuthCheckbox.getValue()));
            sshKeySelect.setVisible(ssh);
            dbVendorSelect.setVisible(db);
            jdbcUrlField.setVisible(db);
            driverClassField.setVisible(db);
            jdbcUsernameField.setVisible(db);
            jdbcPasswordField.setVisible(db);
            viewNameRegexField.setVisible(db);
            testConnectionButton.setVisible(db);
            if (db && dbVendorSelect.getValue() == null) {
                dbVendorSelect.setValue(DatabaseVendor.INFORMIX);
                driverClassField.setValue(DatabaseVendor.INFORMIX.getDefaultDriver());
                jdbcUrlField.setPlaceholder(DatabaseVendor.INFORMIX.getUrlPlaceholder());
            }
        });

        if (existing != null) {
            if (existing.getPort() != null) portField.setValue(existing.getPort());
            if (existing.getUsername() != null) usernameField.setValue(existing.getUsername());
            if (existing.getPoolMin() != null) poolMinField.setValue(existing.getPoolMin());
            if (existing.getPoolMax() != null) poolMaxField.setValue(existing.getPoolMax());
            if (existing.getTuxedoUserName() != null) tuxedoUserNameField.setValue(existing.getTuxedoUserName());
            if (existing.getTuxedoNoAuth() != null) tuxedoNoAuthCheckbox.setValue(existing.getTuxedoNoAuth());
            tuxedoPasswordField.setEnabled(!Boolean.TRUE.equals(existing.getTuxedoNoAuth()));
            if (existing.getSshKey() != null && sshKeys.stream().anyMatch(k -> k.getId().equals(existing.getSshKey().getId()))) {
                sshKeys.stream().filter(k -> k.getId().equals(existing.getSshKey().getId())).findFirst().ifPresent(sshKeySelect::setValue);
            }
        }
        if (existing != null && existing.getPort() == null && existing.getUsername() == null && existing.getConfig() != null && !existing.getConfig().isBlank()) {
            try {
                JsonNode n = JSON.readTree(existing.getConfig());
                if (existing.getType() == ServiceType.TUXEDO && n.has("port")) portField.setValue(n.get("port").asInt());
                if (existing.getType() == ServiceType.SSH && n.has("username")) usernameField.setValue(n.get("username").asText());
            } catch (Exception ignored) {
            }
        }
        if (toEdit.getType() == ServiceType.DB && existing != null && existing.getConfig() != null && !existing.getConfig().isBlank()) {
            try {
                JsonNode n = JSON.readTree(existing.getConfig());
                if (n.has("databaseVendor")) {
                    String v = n.get("databaseVendor").asText();
                    for (DatabaseVendor dv : DatabaseVendor.values()) {
                        if (dv.name().equals(v)) { dbVendorSelect.setValue(dv); break; }
                    }
                }
                if (n.has("jdbcUrl")) jdbcUrlField.setValue(n.get("jdbcUrl").asText());
                if (n.has("driverClassName")) driverClassField.setValue(n.get("driverClassName").asText());
                if (n.has("username")) jdbcUsernameField.setValue(n.get("username").asText());
                if (n.has("password")) jdbcPasswordField.setValue(n.get("password").asText());
                if (n.has("viewNameRegex")) viewNameRegexField.setValue(n.get("viewNameRegex").asText());
            } catch (Exception ignored) {
            }
        }
        if (toEdit.getType() == ServiceType.DB && existing == null) {
            dbVendorSelect.setValue(DatabaseVendor.INFORMIX);
            driverClassField.setValue(DatabaseVendor.INFORMIX.getDefaultDriver());
            jdbcUrlField.setPlaceholder(DatabaseVendor.INFORMIX.getUrlPlaceholder());
        }

        TextArea descriptionField = new TextArea("Opis");
        descriptionField.setWidthFull();

        BeanValidationBinder<ServerService> binder = new BeanValidationBinder<>(ServerService.class);
        binder.forField(environmentSelect).bind("environment");
        binder.forField(serverSelect).bind("server");
        binder.forField(nameField).bind("name");
        binder.forField(typeSelect).bind("type");
        binder.forField(portField).bind("port");
        binder.forField(usernameField).bind("username");
        binder.forField(poolMinField).bind("poolMin");
        binder.forField(poolMaxField).bind("poolMax");
        binder.forField(tuxedoUserNameField).bind("tuxedoUserName");
        binder.forField(tuxedoNoAuthCheckbox).bind("tuxedoNoAuth");
        binder.forField(sshKeySelect).bind("sshKey");
        binder.forField(descriptionField).bind("description");
        binder.readBean(toEdit);

        FormLayout form = new FormLayout(
                environmentSelect, serverSelect, nameField, typeSelect,
                portField, usernameField,
                poolMinField, poolMaxField, tuxedoUserNameField,
                tuxedoNoAuthCheckbox, tuxedoPasswordField,
                sshKeySelect,
                dbVendorSelect, jdbcUrlField, driverClassField, jdbcUsernameField, jdbcPasswordField,
                viewNameRegexField, testConnectionButton,
                descriptionField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button saveBtn = new Button("Zapisz", e -> {
            try {
                binder.writeBean(toEdit);
                if (toEdit.getType() == ServiceType.TUXEDO) {
                    if (tuxedoPasswordField.getValue() != null && !tuxedoPasswordField.getValue().isBlank()) {
                        toEdit.setTuxedoPassword(tuxedoPasswordField.getValue());
                    } else if (existing != null && existing.getTuxedoPassword() != null) {
                        toEdit.setTuxedoPassword(existing.getTuxedoPassword());
                    }
                }
                if (toEdit.getEnvironment() == null || toEdit.getServer() == null) {
                    Notification.show("Wybierz środowisko i serwer", 3000, Notification.Position.MIDDLE);
                    return;
                }
                if (toEdit.getType() == ServiceType.TUXEDO) {
                    toEdit.setConfig(buildTuxedoConfig(portField.getValue(), poolMinField.getValue(), poolMaxField.getValue(), tuxedoUserNameField.getValue()));
                } else if (toEdit.getType() == ServiceType.SSH) {
                    toEdit.setConfig(buildSshConfig(usernameField.getValue(), sshKeySelect.getValue()));
                } else if (toEdit.getType() == ServiceType.DB) {
                    String jdbcUrl = jdbcUrlField.getValue();
                    if (jdbcUrl == null || jdbcUrl.isBlank()) {
                        Notification.show("Podaj URL JDBC", 3000, Notification.Position.MIDDLE);
                        return;
                    }
                    String pwd = jdbcPasswordField.getValue();
                    if ((pwd == null || pwd.isBlank()) && existing != null && existing.getConfig() != null) {
                        pwd = fromConfigKey(existing.getConfig(), "password");
                    }
                    String vendor = dbVendorSelect.getValue() != null ? dbVendorSelect.getValue().name() : null;
                    String viewRegex = viewNameRegexField.getValue();
                    if (viewRegex != null && !viewRegex.isBlank()) {
                        try {
                            Pattern.compile(viewRegex);
                        } catch (PatternSyntaxException ex) {
                            Notification.show("Nieprawidłowy regex dla widoków: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
                            return;
                        }
                    }
                    toEdit.setConfig(buildDbConfig(
                            jdbcUrl,
                            driverClassField.getValue(),
                            jdbcUsernameField.getValue(),
                            pwd,
                            vendor,
                            viewRegex));
                }
                serviceManagement.save(toEdit);
                refreshGrid();
                dialog.close();
                Notification.show("Komponent zapisany", 2000, Notification.Position.MIDDLE);
            } catch (ValidationException ex) {
                Notification.show("Wypełnij wymagane pola", 3000, Notification.Position.MIDDLE);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(form);
        dialog.getFooter().add(new Button("Anuluj", ev -> dialog.close()), saveBtn);
        dialog.open();
    }

    private void deleteService(ServerService ss) {
        Dialog confirm = new Dialog();
        confirm.setHeaderTitle("Potwierdzenie");
        confirm.add("Usunąć komponent \"" + ss.getName() + "\" na serwerze " + (ss.getServer() != null ? ss.getServer().getName() : "") + "?");
        Button deleteBtn = new Button("Usuń", e -> {
            serviceManagement.deleteById(ss.getId());
            refreshGrid();
            confirm.close();
            Notification.show("Komponent usunięty", 2000, Notification.Position.MIDDLE);
        });
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        confirm.getFooter().add(new Button("Anuluj", ev -> confirm.close()), deleteBtn);
        confirm.open();
    }

    private void refreshGrid() {
        grid.setItems(serviceManagement.findAllWithServerAndEnvironment());
    }
}
