package pl.pekao.zesportal.view;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Start | Zesportal")
public class MainView extends VerticalLayout {

    public MainView() {
        add(new H2("Welcome to ZES Portal"));
        add(new Paragraph(
                "Vaadin + Spring Boot application skeleton with H2 database. " +
                "H2 Console: /h2-console (JDBC URL: jdbc:h2:mem:zesportal, user: sa, password empty)."
        ));
        setPadding(true);
        setSpacing(true);
    }
}
