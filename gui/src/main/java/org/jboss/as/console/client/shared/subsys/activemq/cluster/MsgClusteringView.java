package org.jboss.as.console.client.shared.subsys.activemq.cluster;

import java.util.List;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.subsys.activemq.GenericResourceView;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.ballroom.client.widgets.tabs.FakeTabPanel;
import org.jboss.dmr.client.Property;

import static org.jboss.as.console.client.shared.subsys.activemq.cluster.MsgClusteringPresenter.BROADCASTGROUP_ADDRESS;
import static org.jboss.as.console.client.shared.subsys.activemq.cluster.MsgClusteringPresenter.CLUSTERCONNECTION_ADDRESS;
import static org.jboss.as.console.client.shared.subsys.activemq.cluster.MsgClusteringPresenter.DISCOVERYGROUP_ADDRESS;

/**
 * @author Heiko Braun
 * @date 4/18/12
 */
public class MsgClusteringView extends SuspendableViewImpl implements MsgClusteringPresenter.MyView {

    private MsgClusteringPresenter presenter;
    private BroadcastGroupList broadcastGroupList;
    private DiscoveryGroupList discoveryGroupList;
    private ClusterConnectionList clusterConnectionList;

    @Override
    public void setPresenter(MsgClusteringPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        LayoutPanel layout = new LayoutPanel();

        FakeTabPanel titleBar = new FakeTabPanel("Messaging Clustering");
        layout.add(titleBar);

        PagedView panel = new PagedView(true);

        ResourceDescription broadcastDefinition = presenter.getDescriptionRegistry().lookup(BROADCASTGROUP_ADDRESS);
        ResourceDescription discoveryDefinition = presenter.getDescriptionRegistry().lookup(DISCOVERYGROUP_ADDRESS);
        ResourceDescription clusterDefinition = presenter.getDescriptionRegistry().lookup(CLUSTERCONNECTION_ADDRESS);

        broadcastGroupList = new BroadcastGroupList(broadcastDefinition, presenter, "Broadcast Groups", BROADCASTGROUP_ADDRESS);
        discoveryGroupList = new DiscoveryGroupList(discoveryDefinition, presenter, "Discovery Groups", DISCOVERYGROUP_ADDRESS);
        clusterConnectionList = new ClusterConnectionList(clusterDefinition, presenter, "Cluster Connections", CLUSTERCONNECTION_ADDRESS);

        panel.addPage("Broadcast Group", broadcastGroupList.asWidget());
        panel.addPage("Discovery Group", discoveryGroupList.asWidget());
        panel.addPage("Cluster Connection", clusterConnectionList.asWidget());

        // default page
        panel.showPage(0);

        Widget panelWidget = panel.asWidget();
        layout.add(panelWidget);

        layout.setWidgetTopHeight(titleBar, 0, Style.Unit.PX, 40, Style.Unit.PX);
        layout.setWidgetTopHeight(panelWidget, 40, Style.Unit.PX, 100, Style.Unit.PCT);

        return layout;
    }

    @Override
    public void setSelectedProvider(String selectedProvider) {
        presenter.loadDetails();
    }

    @Override
    public void setProvider(List<Property> provider) {}

    @Override
    public void setBroadcastGroups(List<Property> groups) {
        broadcastGroupList.update(groups);
    }

    @Override
    public void setDiscoveryGroups(List<Property> groups) {
        discoveryGroupList.update(groups);
    }

    @Override
    public void setClusterConnection(List<Property> groups) {
        clusterConnectionList.update(groups);
    }
}
