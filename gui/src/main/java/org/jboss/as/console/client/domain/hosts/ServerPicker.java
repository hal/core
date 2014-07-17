package org.jboss.as.console.client.domain.hosts;

import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.stores.domain.HostStore;

/**
 * @author Heiko Braun
 * @date 11/4/11
 */
public class ServerPicker {

    private HostServerTable hostServerTable;

    public void resetHostSelection() {
        hostServerTable.clearSelection();
    }

    public Widget asWidget() {

        VerticalPanel layout = new VerticalPanel();
        layout.getElement().setAttribute("title", "Select Server Instance");
        layout.setStyleName("fill-layout-width");
        //layout.addStyleName("lhs-selector");

        hostServerTable = new HostServerTable();

        hostServerTable.setPopupWidth(400);
        hostServerTable.setDescription(Console.CONSTANTS.server_instance_pleaseSelect());

        Widget widget = hostServerTable.asWidget();
        layout.add(widget);

        return layout;
    }

    public void setTopology(String selectedHost, HostStore.Topology topology) {
        hostServerTable.setTopology(selectedHost, topology);
    }
}
