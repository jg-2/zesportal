package pl.pekao.zesportal.view;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

/**
 * Redirect from legacy path jtuxed0/tasks to tasks/list.
 */
@Route(value = "jtuxed0/tasks", layout = MainLayout.class)
public class TasksView implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.forwardTo(ListaZadanView.class);
    }
}
