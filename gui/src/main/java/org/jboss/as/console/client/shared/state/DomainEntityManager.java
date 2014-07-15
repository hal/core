package org.jboss.as.console.client.shared.state;

import static org.jboss.dmr.client.ModelDescriptionConstants.NOT_SET;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.Host;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.ServerInstance;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.BeanFactory;

/**
 * Domain entity manager corresponds to the picker (host, server) that rely on the host data:
 * The manager loads the data, each picker provides the selection, which then is retained within this instance.
 * This way there is a single instance that maintains the global host and server selections.
 *
 * @author Heiko Braun
 * @date 12/10/12
 */
@Singleton
public class DomainEntityManager implements
        GlobalHostSelection.HostSelectionListener,
        GlobalServerSelection.ServerSelectionListener, StaleGlobalModel.StaleModelListener {


    private String selectedHost;
    private ServerSelection selectedServer;

    private final HostInformationStore hostInfo;
    private final EventBus eventBus;
    private final BeanFactory factory;

    @Inject
    public DomainEntityManager(HostInformationStore hostInfo, EventBus eventBus, BeanFactory factory) {
        this.hostInfo = hostInfo;
        this.eventBus = eventBus;
        this.factory = factory;

        eventBus.addHandler(GlobalHostSelection.TYPE, this);
        eventBus.addHandler(GlobalServerSelection.TYPE, this);
        eventBus.addHandler(StaleGlobalModel.TYPE, this);
    }

    public class NoHostsAvailable extends IllegalStateException {
        public NoHostsAvailable() {
            super("No hosts available!");
        }
    }

    public class NoServerAvailable extends IllegalStateException {
        public NoServerAvailable() {
            super("No server available!");
        }
    }

    public void getHosts(final AsyncCallback<HostList> callback) {
        hostInfo.getHosts(new SimpleCallback<List<Host>>() {
            @Override
            public void onSuccess(List<Host> hosts) {
                Host host = null;
                try {
                    host = getSelectedHost(hosts);
                    callback.onSuccess(new HostList(host, hosts));
                } catch (RuntimeException e) {
                    callback.onFailure(e);
                }

            }
        });
    }

    public void getServerInstances(String hostName, final AsyncCallback<ServerInstanceList> callback) {
        hostInfo.getServerInstances(hostName, new SimpleCallback<List<ServerInstance>>() {
            @Override
            public void onSuccess(List<ServerInstance> serverInstances) {

                if(serverInstances.isEmpty())
                {
                    ServerInstance blank = factory.serverInstance().as();
                    blank.setHost(NOT_SET);
                    blank.setName(NOT_SET);
                    callback.onSuccess(new ServerInstanceList(blank, Collections.EMPTY_LIST));
                }
                else
                {
                    ServerInstance server = getSelectedServerInstance(serverInstances);
                    callback.onSuccess(new ServerInstanceList(server, serverInstances));
                }
            }
        });
    }

    public void getServerConfigurations(String hostName, final AsyncCallback<ServerConfigList> callback) {
        hostInfo.getServerConfigurations(hostName, new SimpleCallback<List<Server>>() {
            @Override
            public void onSuccess(List<Server> serverConfigs) {

                if (serverConfigs.isEmpty()) {
                    // no server at all on this host
                    Server blank = factory.server().as();
                    blank.setName(NOT_SET);
                    callback.onSuccess(new ServerConfigList(blank, Collections.EMPTY_LIST));
                } else {
                    Server s = getSelectedServerConfig(serverConfigs);
                    callback.onSuccess(new ServerConfigList(s, serverConfigs));
                }
            }
        });
    }

    public String getSelectedHost() {

        if(null==selectedHost)
            throw new IllegalStateException("host should not be null");

        return selectedHost;
    }

    public String getSelectedServer() {

        if(null==selectedServer)
            Log.warn("server selection is null");//throw new IllegalStateException("server should not be null");

        return selectedServer!=null ? selectedServer.getName() : NOT_SET;
    }

    public ServerSelection getSelectedServerStatus() {

        if(null==selectedServer)
            return new ServerSelection("n/a", false);
        else
            return selectedServer;
    }

    @Override
    public void onStaleModel(String modelName) {

        // TODO: Needed?

        if(StaleGlobalModel.SERVER_INSTANCES.equals(modelName))
        {
            // server instances carry started/stopped state
        }
        else if(StaleGlobalModel.SERVER_GROUPS.equals(modelName))
        {
            // do groups have relevant state?
        }
        else if(StaleGlobalModel.SERVER_CONFIGURATIONS.equals(modelName))
        {
            // do configs have relevant state?
        }
    }

    /**
     * Entry point for explicit host selection (user initiated)
     * @param hostName
     */
    @Override
    public void onHostSelection(String hostName) {
        selectedHost = hostName;

        // fire stale model event
        eventBus.fireEvent(new HostSelectionChanged());
    }


    /**
     * Entry point for explicit server selection (user initiated)
     * @param server
     */
    @Override
    public void onServerSelection(ServerInstance server) {
        // replace host selection, server selection has precedence
        selectedHost = server.getHost();
        selectedServer = new ServerSelection(server.getName(), server.isRunning());

        // check server state
        if(!server.isRunning())
            Console.warning("The selected server is not running: "+server.getName());

        // fire stale model
        eventBus.fireEvent(new ServerSelectionChanged(server.isRunning()));
    }

    private Host getSelectedHost(List<Host> hosts) {

        if(hosts.isEmpty()) throw new NoHostsAvailable();

        Host matched = null;

        // match by preselection
        for(Host host : hosts)
        {
            if(host.getName().equals(selectedHost))
            {
                matched = host;
                break;
            }
        }

        // fallback match
        if(null==matched)
            matched = hosts.get(0);

        selectedHost = matched.getName();

        return matched;
    }

    private ServerInstance getSelectedServerInstance(List<ServerInstance> serverInstances) {

        if(serverInstances.isEmpty()) throw new IllegalStateException("No server instances available!");

        ServerInstance matched = null;

        ServerInstance active = null;

        // match by preselection
        for(ServerInstance server : serverInstances)
        {
            if(server.isRunning())
                active = server;

            if(selectedServer!=null &&
                    server.getName().equals(selectedServer.getName()))
            {
                matched = server;
                break;
            }
        }

        // match first running server
        if(active!=null && null==matched)  {
            for(ServerInstance server : serverInstances) {
                if (server.isRunning()) {
                    matched = server;
                    break;
                }
            }
        }

        // fallback match: no preselection and no active server > pick any
        if(null==matched)
            matched = serverInstances.get(0);

        selectedHost = matched.getHost();
        selectedServer = new ServerSelection(matched.getName(), matched.isRunning());

        return matched;
    }

    private Server getSelectedServerConfig(List<Server> serverConfigs) {

        if(serverConfigs.isEmpty()) throw new IllegalStateException("No server configs available!");

        Server matched = null;

        // match by preselection
        for(Server s : serverConfigs)
        {
            if(selectedServer!=null &&
                    s.getName().equals(selectedServer.getName()))
            {
                matched = s;
                break;
            }
        }

        // fallback match
        if(null==matched)
            matched = serverConfigs.get(0);

        selectedServer = new ServerSelection(matched.getName(), matched.isStarted());

        return matched;
    }

    public class ServerSelection {
        private String name;
        private boolean running;

        ServerSelection(String name, boolean running) {
            this.name = name;
            this.running = running;
        }

        public String getName() {
            return name;
        }

        public boolean isRunning() {
            return running;
        }
    }
}
