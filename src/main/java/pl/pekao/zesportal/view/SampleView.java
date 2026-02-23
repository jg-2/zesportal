package pl.pekao.zesportal.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import pl.pekao.zesportal.entity.SampleEntity;
import pl.pekao.zesportal.service.SampleEntityService;

@Route(value = "sample", layout = MainLayout.class)
@PageTitle("Sample Data | Zesportal")
public class SampleView extends VerticalLayout {

    public SampleView(SampleEntityService sampleEntityService) {
        Grid<SampleEntity> grid = new Grid<>(SampleEntity.class, false);
        grid.addColumn(SampleEntity::getId).setHeader("ID").setWidth("80px").setFlexGrow(0);
        grid.addColumn(SampleEntity::getName).setHeader("Name");
        grid.addColumn(SampleEntity::getDescription).setHeader("Description");
        grid.addColumn(e -> e.getCreatedAt() != null ? e.getCreatedAt().toString() : "")
                .setHeader("Created");

        grid.setItems(sampleEntityService.findAll());
        grid.setWidthFull();

        add(grid);
        setPadding(true);
        setSizeFull();
    }
}
