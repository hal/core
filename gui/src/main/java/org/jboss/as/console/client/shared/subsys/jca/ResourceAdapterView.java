package org.jboss.as.console.client.shared.subsys.jca;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.subsys.jca.model.ResourceAdapter;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 7/19/11
 */
public class ResourceAdapterView extends SuspendableViewImpl implements ResourceAdapterPresenter.MyView {

    private ResourceAdapterPresenter presenter;
    private PagedView panel;
    private AdapterList adapterList;

    private ConnectionList connectionList;
    private AdminObjectList adminObjects;

    @Override
    public void setPresenter(ResourceAdapterPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {

        DefaultTabLayoutPanel layout  = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        layout.addStyleName("default-tabpanel");
        panel = new PagedView(true);

        this.adapterList = new AdapterList(presenter);
        this.connectionList = new ConnectionList(presenter);
        this.adminObjects = new AdminObjectList(presenter);

        panel.addPage("Configuration", adapterList.asWidget());
        panel.addPage("Connection Definitions", connectionList.asWidget());
        panel.addPage("Admin Objects", adminObjects.asWidget()) ;

        // default page
        panel.showPage(0);

        Widget panelWidget = panel.asWidget();
        layout.add(panelWidget, "Resource Adapter");

        return layout;
    }

    @Override
    public void setAdapter(ResourceAdapter ra) {

        adapterList.setAdapter(ra);
        connectionList.setAdapter(ra);
        adminObjects.setAdapter(ra);

    }
}
