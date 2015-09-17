package org.jboss.as.console.client.shared.subsys.jca;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.dmr.client.Property;

import java.util.Collections;
import java.util.List;

/**
 * @author Heiko Braun
 * @date 7/19/11
 */
public class ResourceAdapterView extends SuspendableViewImpl implements ResourceAdapterPresenter.MyView {

    private ResourceAdapterPresenter presenter;
    private PagedView panel;

    private AdapterDetails details;
    private AdminObjectList adminObjects;
    private ConnectionDefList connectionDefs;

    @Override
    public void setPresenter(ResourceAdapterPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {

        DefaultTabLayoutPanel layout  = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        layout.addStyleName("default-tabpanel");
        panel = new PagedView(true);

        this.details = new AdapterDetails(presenter);
        this.adminObjects = new AdminObjectList(presenter);
        this.connectionDefs = new ConnectionDefList(presenter);

        panel.addPage("Configuration", details.asWidget());
        panel.addPage("Connection Definitions", connectionDefs.asWidget());
        panel.addPage("Admin Objects", adminObjects.asWidget());

        // default page
        panel.showPage(0);

        Widget panelWidget = panel.asWidget();
        layout.add(panelWidget, "Resource Adapter");

        return layout;
    }

    @Override
    public void setAdapter(Property payload) {

        details.setAdapter(payload);

        List<Property> admins = payload.getValue().hasDefined("admin-objects") ?
                payload.getValue().get("admin-objects").asPropertyList() : Collections.EMPTY_LIST;

        List<Property> cons = payload.getValue().hasDefined("connection-definitions") ?
                payload.getValue().get("connection-definitions").asPropertyList() : Collections.EMPTY_LIST;

        adminObjects.setData(admins);
        connectionDefs.setData(cons);

    }
}
