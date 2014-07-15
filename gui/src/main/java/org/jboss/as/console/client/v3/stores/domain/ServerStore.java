package org.jboss.as.console.client.v3.stores.domain;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.v3.stores.domain.actions.AddServer;
import org.jboss.as.console.client.v3.stores.domain.actions.HostSelection;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshServer;
import org.jboss.as.console.client.v3.stores.domain.actions.RemoveServer;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Heiko Braun
 * @date 15/07/14
 */
@Store
public class ServerStore extends ChangeSupport {

    private HostInformationStore hostInfo;

    private HostStore hostStore;

    private Map<String, List<Server>> serverModel = new HashMap<>();
    private Server selectedServer;

    @Inject
    public ServerStore(HostStore hostStore, HostInformationStore hostInfo) {
        this.hostStore = hostStore;
        this.hostInfo = hostInfo;
    }

    public void init(final String hostName, final AsyncCallback<List<Server>> callback) {
        hostInfo.getServerConfigurations(hostName, new SimpleCallback<List<Server>>() {
            @Override
            public void onSuccess(List<Server> servers) {
                ServerStore.this.serverModel.put(hostName, servers);
                selectedServer = servers.get(0);
                callback.onSuccess(servers);
            }
        });
    }

    @Process(actionType = HostSelection.class, dependencies = {HostStore.class})
    public void onSelectHost(String hostName, final Dispatcher.Channel channel) {

        onRefresh(channel);

    }

    @Process(actionType = RefreshServer.class, dependencies = {HostStore.class})
    public void onRefresh(final Dispatcher.Channel channel) {

        final String hostName = hostStore.getSelectedHost();

        hostInfo.getServerConfigurations(hostName, new SimpleCallback<List<Server>>() {
            @Override
            public void onSuccess(List<Server> servers) {
                serverModel.put(hostName, servers);
                channel.ack();
                fireChanged(ServerStore.class);
            }

            @Override
            public void onFailure(Throwable caught) {
                channel.nack(caught);
            }
        });

    }

    @Process(actionType = AddServer.class)
    public void onAddServer(final Server server, final Dispatcher.Channel channel) {

        hostInfo.createServerConfig(hostStore.getSelectedHost(), server, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {

                String selectedHost = hostStore.getSelectedHost();

                if(!serverModel.containsKey(selectedHost))
                    serverModel.put(selectedHost, new ArrayList<Server>());
                serverModel.get(selectedHost).add(server);

                channel.ack();
                fireChanged(ServerStore.class);
            }

            @Override
            public void onFailure(Throwable caught) {
                Console.error("Failed to add server", caught.getMessage());
                channel.nack(caught);
            }
        });

    }

    @Process(actionType = RemoveServer.class)
    public void onRemoveServer(final Server server, final Dispatcher.Channel channel) {

        hostInfo.deleteServerConfig(hostStore.getSelectedHost(), server, new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {
                boolean removed = ServerStore.this.serverModel.get(hostStore.getSelectedHost()).remove(server);
                if(!removed)
                    throw new RuntimeException("Failed to remove server");

                channel.ack();
                fireChanged(ServerStore.class);
            }

            @Override
            public void onFailure(Throwable caught) {
                channel.nack(caught);
            }
        });
    }

    // data access

    public List<Server> getServerModel(String host) {
        List<Server> servers = serverModel.get(host);
        return servers != null ? servers : Collections.<Server>emptyList();
    }

    public String getSelectedServer() {
        return selectedServer.getName();
    }

    public Server getSelectedServerInstance() {
        return selectedServer;
    }
}
