package pl.pekao.zesportal.view;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "papiery/konfiguracja", layout = MainLayout.class)
@PageTitle("Konfiguracja papierów | Zesportal")
public class PapieryKonfiguracjaView extends VerticalLayout {

    public PapieryKonfiguracjaView() {
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Konfiguracja papierów");
        Paragraph description = new Paragraph("Konfiguracja modułu Papiery - do implementacji.");

        add(title, description);
    }
}

