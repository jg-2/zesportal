package pl.pekao.zesportal.view;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "papiery/instrument-id", layout = MainLayout.class)
@PageTitle("InstrumentID | Zesportal")
public class PapieryInstrumentIdView extends VerticalLayout {

    public PapieryInstrumentIdView() {
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("InstrumentID");
        Paragraph description = new Paragraph("Widok InstrumentID - do implementacji.");

        add(title, description);
    }
}

