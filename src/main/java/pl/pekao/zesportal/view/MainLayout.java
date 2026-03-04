package pl.pekao.zesportal.view;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.theme.lumo.LumoUtility;
import pl.pekao.zesportal.service.task.TaskProgressHolder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Scope PROTOTYPE – każda sesja/UI dostaje własną instancję layoutu,
 * co unika błędu "Unregistered node / tree corrupted" przy zamykaniu karty.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MainLayout extends AppLayout {

    private final TaskProgressHolder progressHolder;
    private final Span progressTaskName = new Span();
    private final ProgressBar progressBar = new ProgressBar(0, 1);
    private final HorizontalLayout progressRow;
    private boolean pollListenerAdded;

    public MainLayout(TaskProgressHolder progressHolder) {
        this.progressHolder = progressHolder;
        this.progressRow = new HorizontalLayout();
        this.progressRow.setWidthFull();
        this.progressRow.setAlignItems(FlexComponent.Alignment.CENTER);
        this.progressRow.setSpacing(true);
        this.progressRow.getStyle()
                .set("padding", "10px 20px")
                .set("background", "#4338ca")
                .set("color", "white")
                .set("border-top", "2px solid #3730a3")
                .set("position", "fixed")
                .set("bottom", "0")
                .set("left", "0")
                .set("right", "0")
                .set("z-index", "1000")
                .set("box-shadow", "0 -2px 10px rgba(0,0,0,0.15)");
        this.progressBar.setWidth("300px");
        this.progressBar.getStyle().set("--lumo-primary-color", "white");
        this.progressRow.add(progressTaskName, progressBar);
        this.progressRow.setVisible(false);

        createHeader();
        createDrawer();
        getElement().appendChild(progressRow.getElement());
        addAttachListener(e -> {
            if (e.getUI() == null) return;
            if (!pollListenerAdded) {
                pollListenerAdded = true;
                e.getUI().setPollInterval(500);
                e.getUI().addPollListener(ev -> updateProgressFromHolder());
            }
        });
    }

    private void updateProgressFromHolder() {
        if (!isAttached() || getUI().isEmpty()) return;
        TaskProgressHolder.ProgressSnapshot snap = progressHolder.getCurrent();
        if (snap == null || snap.progress0To1() >= 1.0) {
            progressRow.setVisible(false);
            return;
        }
        progressRow.setVisible(true);
        progressTaskName.setText(snap.taskName() + " — próba " + (snap.currentStep() + 1) + "/" + snap.maxSteps());
        progressBar.setValue(snap.progress0To1());
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        
        // Logo container with icon
        Div logoContainer = new Div();
        logoContainer.addClassNames("logo-container");
        logoContainer.getStyle()
            .set("display", "flex")
            .set("align-items", "center")
            .set("gap", "12px")
            .set("margin-left", "8px");
        
        // Icon - solid indigo background
        Div iconWrapper = new Div();
        iconWrapper.addClassNames("icon-wrapper");
        iconWrapper.getStyle()
            .set("width", "40px")
            .set("height", "40px")
            .set("border-radius", "8px")
            .set("background", "#4338ca")
            .set("display", "flex")
            .set("align-items", "center")
            .set("justify-content", "center")
            .set("box-shadow", "0 2px 6px rgba(67, 56, 202, 0.35)");
        
        Icon portalIcon = VaadinIcon.CUBES.create();
        portalIcon.setSize("24px");
        portalIcon.getStyle().set("color", "white");
        iconWrapper.add(portalIcon);
        
        // Title - solid colors for good visibility
        Div titleContainer = new Div();
        titleContainer.addClassNames("title-container");
        
        H1 title = new H1("ZES");
        title.getStyle()
            .set("font-size", "22px")
            .set("font-weight", "700")
            .set("margin", "0")
            .set("color", "#4338ca")
            .set("letter-spacing", "0.05em");
        
        Span portalText = new Span(" PORTAL");
        portalText.getStyle()
            .set("font-size", "22px")
            .set("font-weight", "600")
            .set("color", "#1e293b")
            .set("letter-spacing", "0.02em");
        
        titleContainer.add(title, portalText);
        titleContainer.getStyle()
            .set("display", "flex")
            .set("align-items", "baseline");
        
        logoContainer.add(iconWrapper, titleContainer);
        
        // Badge
        Span badge = new Span("v1.0");
        badge.getStyle()
            .set("font-size", "11px")
            .set("font-weight", "600")
            .set("color", "white")
            .set("background", "#4338ca")
            .set("padding", "3px 8px")
            .set("border-radius", "10px")
            .set("margin-left", "8px");
        
        HorizontalLayout header = new HorizontalLayout(toggle, logoContainer, badge);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.addClassNames(LumoUtility.Padding.Vertical.SMALL, LumoUtility.Padding.Horizontal.MEDIUM);
        header.getStyle()
            .set("background", "var(--lumo-base-color)")
            .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
            .set("box-shadow", "0 1px 3px rgba(0, 0, 0, 0.05)");

        addToNavbar(header);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();
        
        // Start
        nav.addItem(new SideNavItem("Start", ""));
        
        // JTuxedo Web – etykieta: J (czerwony, pogrubiony), Tuxedo (kursywa, szary), Web (niebieski)
        Span jLabel = new Span("J");
        jLabel.getStyle()
                .set("color", "#c53030")
                .set("font-weight", "700");
        Span tuxedoLabel = new Span("Tuxedo");
        tuxedoLabel.getStyle()
                .set("font-style", "italic")
                .set("color", "#64748b");
        Span webLabel = new Span(" Web");
        webLabel.getStyle()
                .set("color", "#2563eb");
        Div jtuxedoWebLabel = new Div(jLabel, tuxedoLabel, webLabel);
        jtuxedoWebLabel.getStyle()
                .set("display", "inline")
                .set("white-space", "nowrap");
        SideNavItem jtuxed0Item = new SideNavItem("", "", jtuxedoWebLabel);
        jtuxed0Item.addItem(new SideNavItem("Wywołanie usług", "jtuxed0/wywolaj-uslugi"));
        jtuxed0Item.addItem(new SideNavItem("Import", "jtuxed0/wczytaj-z-pliku"));
        jtuxed0Item.addItem(new SideNavItem("Konfiguracja usług", "jtuxed0/konfiguracja-uslug"));
        jtuxed0Item.addItem(new SideNavItem("Definicje importów", "jtuxed0/definicje-importow"));
        nav.addItem(jtuxed0Item);

        // Tasks (paths in English)
        SideNavItem tasksItem = new SideNavItem("Tasks", "");
        tasksItem.addItem(new SideNavItem("Task list", "tasks/list"));
        tasksItem.addItem(new SideNavItem("Task templates", "tasks/templates"));
        nav.addItem(tasksItem);
        
        // Papiery
        SideNavItem papieryItem = new SideNavItem("Papiery", "");
        papieryItem.addItem(new SideNavItem("Nowe papiery", "papiery/zakladanie"));
        papieryItem.addItem(new SideNavItem("Wykluczone", "papiery/wykluczone"));
        papieryItem.addItem(new SideNavItem("Ostatnie notowanie", "papiery/ostatnie-notowanie"));
        papieryItem.addItem(new SideNavItem("InstrumentID", "papiery/instrument-id"));
        papieryItem.addItem(new SideNavItem("Spółki", "papiery/spolki"));
        papieryItem.addItem(new SideNavItem("Zmiana rynku", "papiery/zmiana-rynku"));
        papieryItem.addItem(new SideNavItem("Wielkość emisji", "papiery/wielkosc-emisji"));
        papieryItem.addItem(new SideNavItem("Konfiguracja", "papiery/konfiguracja"));
        nav.addItem(papieryItem);
        
        // Configuration
        SideNavItem configurationItem = new SideNavItem("Configuration", "");
        configurationItem.addItem(new SideNavItem("Environments", "configuration/environments"));
        configurationItem.addItem(new SideNavItem("Servers", "configuration/servers"));
        configurationItem.addItem(new SideNavItem("Komponenty systemowe", "configuration/komponenty-systemowe"));
        configurationItem.addItem(new SideNavItem("Klucze SSH", "configuration/klucze-ssh"));
        nav.addItem(configurationItem);

        addToDrawer(nav);
    }
}
