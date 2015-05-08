package org.jboss.as.console.client.shared.subsys.messaging.cluster;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.subsys.messaging.model.BroadcastGroup;
import org.jboss.as.console.client.shared.subsys.messaging.model.ClusterConnection;
import org.jboss.as.console.client.shared.subsys.messaging.model.DiscoveryGroup;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.ballroom.client.widgets.tabs.FakeTabPanel;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 4/18/12
 */
public class MsgClusteringView extends SuspendableViewImpl implements MsgClusteringPresenter.MyView {
    private MsgClusteringPresenter presenter;
    private PagedView panel;
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

        panel = new PagedView(true);

        broadcastGroupList = new BroadcastGroupList(presenter);
        discoveryGroupList = new DiscoveryGroupList(presenter);
        clusterConnectionList = new ClusterConnectionList(presenter);

        panel.addPage("Broadcast", broadcastGroupList.asWidget()) ;
        panel.addPage("Discovery", discoveryGroupList.asWidget()) ;
        panel.addPage("Connections", clusterConnectionList.asWidget()) ;


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
        presenter.loadDetails(selectedProvider);
    }

    @Override
    public void setProvider(List<Property> provider) {

    }

    @Override
    public void setBroadcastGroups(List<BroadcastGroup> groups) {
        broadcastGroupList.setBroadcastGroups(groups);
    }

    @Override
    public void setDiscoveryGroups(List<DiscoveryGroup> groups) {
        discoveryGroupList.setDiscoveryGroups(groups);
    }

    @Override
    public void setClusterConnection(List<ClusterConnection> groups) {
        clusterConnectionList.setClusterConnections(groups);
    }
}
