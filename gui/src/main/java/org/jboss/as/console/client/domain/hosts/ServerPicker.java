package org.jboss.as.console.client.domain.hosts;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.Host;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.v3.stores.domain.ServerStore;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.PropagatesChange;

import java.util.List;
import java.util.Set;

/**
 * @author Heiko Braun
 * @date 11/4/11
 */
public class ServerPicker {

    private final ServerStore serverStore;
    private final HostStore hostStore;
    private final Dispatcher circuit;
    private HostServerTable hostServerTable;

    private Label label;

    public void resetHostSelection() {
        hostServerTable.clearSelection();
    }

    public ServerPicker() {
        this.serverStore  = Console.MODULES.getServerStore();
        this.hostStore = Console.MODULES.getHostStore();
        this.circuit = Console.MODULES.getCircuitDispatcher();

        serverStore.addChangeHandler(new PropagatesChange.Handler() {
            @Override
            public void onChange(Class<?> source) {

                if(hostStore.hasSelecteHost() && serverStore.hasSelectedServer()) {
                    String selectedHost = hostStore.getSelectedHost();

                    hostServerTable.setServer(serverStore.getSelectedServerInstance(), serverStore.getServerInstances(selectedHost));
                    hostServerTable.updateDisplay(selectedHost, serverStore.getSelectedServer());
                }
                else if(!serverStore.hasSelectedServer())
                {
                    hostServerTable.clearDisplay();
                }
            }
        });
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

    public void setHosts(String selectedHost, HostStore.Topology topology) {
        hostServerTable.setHosts(selectedHost, topology);
    }
}
