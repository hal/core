package org.jboss.as.console.client.shared.subsys.activemq.connections;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnector;
import org.jboss.as.console.client.shared.subsys.activemq.model.ConnectorType;
import org.jboss.as.console.client.widgets.ContentDescription;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 4/4/12
 */
public class ConnectorOverview {

    private HTML serverName;
    private MsgConnectionsPresenter presenter;
    private ConnectorList genericConnectors;
    private ConnectorList remoteConnectors;
    private ConnectorList invmConnectors;

    public ConnectorOverview(MsgConnectionsPresenter presenter) {
        this.presenter = presenter;
    }

    Widget asWidget() {

        LayoutPanel layout = new LayoutPanel();

        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("rhs-content-panel");

        ScrollPanel scroll = new ScrollPanel(panel);
        layout.add(scroll);

        layout.setWidgetTopHeight(scroll, 0, Style.Unit.PX, 100, Style.Unit.PCT);

        serverName = new HTML("Replace me");
        serverName.setStyleName("content-header-label");

        HorizontalPanel header = new HorizontalPanel();
        header.setStyleName("fill-layout-width");
        header.add(serverName);

        final DeckPanel deck = new DeckPanel();
        deck.addStyleName("fill-layout");

        final ListBox selector = new ListBox();

        selector.addItem("Type: Remote");
        selector.addItem("Type: In-VM");
        selector.addItem("Type: Generic");

        selector.addChangeHandler(changeEvent -> deck.showWidget(selector.getSelectedIndex()));

        header.add(selector);
        selector.getElement().getParentElement().setAttribute("align", "right");

        panel.add(header);
        panel.add(new ContentDescription(((UIConstants) GWT.create(UIConstants.class)).jmsConnectorDescription()));

        genericConnectors = new ConnectorList(presenter, ConnectorType.GENERIC);
        remoteConnectors = new ConnectorList(presenter, ConnectorType.REMOTE);
        invmConnectors = new ConnectorList(presenter, ConnectorType.INVM);

        deck.add(remoteConnectors.asWidget());
        deck.add(invmConnectors.asWidget());
        deck.add(genericConnectors.asWidget());

        deck.showWidget(0);

        panel.add(deck);

        return layout;
    }

    public void setGenericConnectors(List<ActivemqConnector> list) {
        serverName.setText("Connectors: Provider "+ presenter.getCurrentServer());
        genericConnectors.setConnectors(list);
    }

    public void setRemoteConnectors(List<ActivemqConnector> remote) {
        remoteConnectors.setConnectors(remote);
    }

    public void setInvmConnectors(List<ActivemqConnector> invm) {
        invmConnectors.setConnectors(invm);
    }
}
