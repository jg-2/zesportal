package pl.pekao.zesportal.view;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;

@Route(value = "papiery/zakladanie", layout = MainLayout.class)
@PageTitle("Nowe papiery | Zesportal")
public class ZakladaniePapierowView extends VerticalLayout {

    public ZakladaniePapierowView() {
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Nowe papiery");
        Paragraph description = new Paragraph("Moduł Nowe papiery - do implementacji.");

        add(title, description);
    }
}
