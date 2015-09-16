package org.jboss.as.console.client.shared.subsys.ejb3;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 10/09/14
 */
public class EEServicesView {

    private final EEPresenter presenter;
    private ServiceViewTemplate contextView;
    private PagedView panel;
    private ServiceViewTemplate executorView;
    private ServiceViewTemplate scheduledView;
    private ServiceViewTemplate threadView;

    public EEServicesView(EEPresenter presenter) {

        this.presenter = presenter;
    }

    Widget asWidget() {

        LayoutPanel layout = new LayoutPanel();

        panel = new PagedView(true);

        contextView = new ServiceViewTemplate(presenter, "Context Services", AddressTemplate.of("{selected.profile}/subsystem=ee/context-service=*"));
        executorView = new ServiceViewTemplate(presenter, "Executor", AddressTemplate.of("{selected.profile}/subsystem=ee/managed-executor-service=*"));
        scheduledView = new ServiceViewTemplate(presenter, "Scheduler Executor", AddressTemplate.of("{selected.profile}/subsystem=ee/managed-scheduled-executor-service=*"));
        threadView = new ServiceViewTemplate(presenter, "Thread factories", AddressTemplate.of("{selected.profile}/subsystem=ee/managed-thread-factory=*"));
         // TODO: default bindings

        panel.addPage("Context Service", contextView.asWidget());
        panel.addPage("Executor", executorView.asWidget());
        panel.addPage("Scheduled Executor", scheduledView.asWidget());
        panel.addPage("Thread Factories", threadView.asWidget());

        // default page
        panel.showPage(0);

        Widget panelWidget = panel.asWidget();
        layout.add(panelWidget);

        layout.setWidgetTopHeight(panelWidget, 0, Style.Unit.PX, 100, Style.Unit.PCT);

        return layout;
    }

    public void setContextServices(List<Property> contextServices) {
        contextView.setData(contextServices);
    }

    public void setThreadFactories(List<Property> data) {
        threadView.setData(data);
    }

    public void setExecutor(List<Property> data) {
        executorView.setData(data);
    }


    public void setScheduledExecutor(List<Property> data) {
        scheduledView.setData(data );
    }
}
