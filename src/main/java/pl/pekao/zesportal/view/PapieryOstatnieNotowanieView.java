package pl.pekao.zesportal.view;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "papiery/ostatnie-notowanie", layout = MainLayout.class)
@PageTitle("Ostatnie notowanie | Zesportal")
public class PapieryOstatnieNotowanieView extends VerticalLayout {

    public PapieryOstatnieNotowanieView() {
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Ostatnie notowanie");
        Paragraph description = new Paragraph("Widok ostatniego notowania papierów - do implementacji.");

        add(title, description);
    }
}

