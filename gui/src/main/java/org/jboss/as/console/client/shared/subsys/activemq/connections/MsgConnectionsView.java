package org.jboss.as.console.client.shared.subsys.activemq.connections;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqAcceptor;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqBridge;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnector;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnectorService;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.ballroom.client.widgets.tabs.FakeTabPanel;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 4/2/12
 */
public class MsgConnectionsView extends SuspendableViewImpl implements MsgConnectionsPresenter.MyView {

    private MsgConnectionsPresenter presenter;
    private AcceptorOverview acceptorOverview;
    private ConnectorOverview connectorOverview;
    private ConnectorServiceList connectorServiceList;
    private BridgesList bridgesList;

    @Override
    public Widget createWidget() {

        LayoutPanel layout = new LayoutPanel();

        FakeTabPanel titleBar = new FakeTabPanel("Messaging Connections");
        layout.add(titleBar);

        PagedView panel = new PagedView(true);

        acceptorOverview = new AcceptorOverview(presenter);
        connectorOverview = new ConnectorOverview(presenter);
        connectorServiceList = new ConnectorServiceList(presenter);
        bridgesList = new BridgesList(presenter);

        panel.addPage("Acceptor", acceptorOverview.asWidget()) ;
        panel.addPage("Connector", connectorOverview.asWidget()) ;
        panel.addPage("Connector Services", connectorServiceList.asWidget()) ;
        panel.addPage("Bridges", bridgesList.asWidget()) ;

        // default page
        panel.showPage(0);

        Widget panelWidget = panel.asWidget();
        layout.add(panelWidget);

        layout.setWidgetTopHeight(titleBar, 0, Style.Unit.PX, 40, Style.Unit.PX);
        layout.setWidgetTopHeight(panelWidget, 40, Style.Unit.PX, 100, Style.Unit.PCT);

        return layout;
    }

    @Override
    public void setPresenter(MsgConnectionsPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setSelectedProvider(String selectedProvider) {
        presenter.loadDetails(selectedProvider);
    }

    @Override
    public void setProvider(List<Property> provider) {}

    @Override
    public void setGenericAcceptors(List<ActivemqAcceptor> genericAcceptors) {
        acceptorOverview.setGenericAcceptors(genericAcceptors);
    }

    @Override
    public void setRemoteAcceptors(List<ActivemqAcceptor> remote) {
        acceptorOverview.setRemoteAcceptors(remote);
    }

    @Override
    public void setInvmAcceptors(List<ActivemqAcceptor> invm) {
        acceptorOverview.setInvmAcceptors(invm);
    }

    @Override
    public void setGenericConnectors(List<ActivemqConnector> generic) {
        connectorOverview.setGenericConnectors(generic);
    }

    @Override
    public void setRemoteConnectors(List<ActivemqConnector> remote) {
        connectorOverview.setRemoteConnectors(remote);
    }

    @Override
    public void setInvmConnectors(List<ActivemqConnector> invm) {
        connectorOverview.setInvmConnectors(invm);
    }

    @Override
    public void setConnetorServices(List<ActivemqConnectorService> services) {
        connectorServiceList.setConnectorServices(services);
    }

    @Override
    public void setBridges(List<ActivemqBridge> bridges) {
        bridgesList.setBridges(bridges);
    }
}
