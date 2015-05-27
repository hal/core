package org.jboss.as.console.client.shared.subsys.undertow;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 01/04/15
 */
public class HttpMetricView extends SuspendableViewImpl implements HttpMetricPresenter.MyView {

    private HttpMetricPresenter presenter;
    private PagedView panel;
    private ServerList serverList;
    private ConnectorMetricView connectorView;

    @Override
    public Widget createWidget() {
        DefaultTabLayoutPanel tabLayoutpanel = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabLayoutpanel.addStyleName("default-tabpanel");

        panel = new PagedView();

        serverList = new ServerList(presenter, true);
        connectorView = new ConnectorMetricView(presenter);

        panel.addPage(Console.CONSTANTS.common_label_back(), serverList.asWidget());
        panel.addPage("Connectors", connectorView.asWidget());

        // default page
        panel.showPage(0);

        tabLayoutpanel.add(panel.asWidget(), "HTTP Server");

        return tabLayoutpanel;
    }

    @Override
    public void setPresenter(HttpMetricPresenter presenter) {

        this.presenter = presenter;
    }

    @Override
    public void clearSamples() {

    }

    @Override
    public void setServer(List list) {
        serverList.setServer(list);
    }

    @Override
    public void setConnectors(List<Property> connectors) {
        connectorView.setData(connectors);

    }

    @Override
    public void setServerSelection(String name) {
        if(null==name)
        {
            panel.showPage(0);
        }
        else{

            presenter.loadDetails();

            if(0==panel.getPage())
                panel.showPage(1);
        }
    }
}
