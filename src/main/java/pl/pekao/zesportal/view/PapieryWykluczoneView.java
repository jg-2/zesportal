package pl.pekao.zesportal.view;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "papiery/wykluczone", layout = MainLayout.class)
@PageTitle("Papiery wykluczone | Zesportal")
public class PapieryWykluczoneView extends VerticalLayout {

    public PapieryWykluczoneView() {
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Wykluczone papiery");
        Paragraph description = new Paragraph("Widok wykluczonych papierów - do implementacji.");

        add(title, description);
    }
}

