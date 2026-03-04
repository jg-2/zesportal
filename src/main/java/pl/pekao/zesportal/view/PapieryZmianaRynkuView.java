package pl.pekao.zesportal.view;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "papiery/zmiana-rynku", layout = MainLayout.class)
@PageTitle("Zmiana rynku | Zesportal")
public class PapieryZmianaRynkuView extends VerticalLayout {

    public PapieryZmianaRynkuView() {
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Zmiana rynku");
        Paragraph description = new Paragraph("Widok zmiany rynku dla papierów - do implementacji.");

        add(title, description);
    }
}

