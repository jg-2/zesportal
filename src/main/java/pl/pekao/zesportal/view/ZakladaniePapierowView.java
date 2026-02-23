package pl.pekao.zesportal.view;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;

@Route(value = "papiery/zakladanie", layout = MainLayout.class)
@PageTitle("Zakładanie papierów | Zesportal")
public class ZakladaniePapierowView extends VerticalLayout {

    public ZakladaniePapierowView() {
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Zakładanie papierów");
        Paragraph description = new Paragraph("Moduł zakładania papierów - do implementacji.");

        add(title, description);
    }
}
