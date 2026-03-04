package pl.pekao.zesportal.view;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "papiery/spolki", layout = MainLayout.class)
@PageTitle("Spółki | Zesportal")
public class PapierySpolkiView extends VerticalLayout {

    public PapierySpolkiView() {
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Spółki");
        Paragraph description = new Paragraph("Widok spółek powiązanych z papierami - do implementacji.");

        add(title, description);
    }
}

