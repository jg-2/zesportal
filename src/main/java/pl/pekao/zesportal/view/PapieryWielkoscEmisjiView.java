package pl.pekao.zesportal.view;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "papiery/wielkosc-emisji", layout = MainLayout.class)
@PageTitle("Wielkość emisji | Zesportal")
public class PapieryWielkoscEmisjiView extends VerticalLayout {

    public PapieryWielkoscEmisjiView() {
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Wielkość emisji");
        Paragraph description = new Paragraph("Widok wielkości emisji papierów - do implementacji.");

        add(title, description);
    }
}

