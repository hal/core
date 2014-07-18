package org.jboss.as.console.client.v3.stores.domain;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.stores.domain.actions.HostSelection;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshHosts;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshServer;
import org.jboss.as.console.client.v3.stores.domain.actions.SelectServerInstance;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 15/07/14
 */
@Store
public class HostStore extends ChangeSupport {

    private final DispatchAsync dispatcher;

    private String selectedHost;
    private String selectedServer;

    private Topology topology;

    @Inject
    public HostStore( DispatchAsync dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void init(final AsyncCallback<Set<String>> callback) {
        synchonizeHosts(new AsyncCallback<Boolean>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(Boolean result) {

                // TODO: can this actually happen?
                Set<String> hostNames = topology.getHostNames();
                if(hostNames.isEmpty())
                    throw new NoHostsAvailable();

                // default host selection
                selectedHost = hostNames.iterator().next();

                defaultServerSelection();

                callback.onSuccess(hostNames);
            }
        });
    }

    // ------------------------
    //  internal data structures

    /**
     * A mapping of hosts and _active_ servers
     */
    public class Topology {
        private Map<String, Set<String>> mapping = new HashMap<>();

        private void addServer(String hostName, String server) {
            mapping.get(hostName).add(server);
        }

        private void addHost(String hostName) {
            mapping.put(hostName, new HashSet<String>());
        }

        public Set<String> getHostNames() {
            return mapping.keySet();
        }

        public Set<String> getServerNames(String hostName) {
            return mapping.get(hostName);
        }

        public boolean hasServer(String hostName) {
            return !mapping.get(hostName).isEmpty();
        }
    }

    // ------------------------

    private void synchonizeHosts(final AsyncCallback<Boolean> callback) {
        ModelNode op = new ModelNode();
        op.get(ADDRESS).setEmptyList();
        op.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
        op.get(CHILD_TYPE).set("host");

        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    callback.onFailure(new RuntimeException("Failed to synchronize host model: "+response.getFailureDescription()));
                } else {
                    List<ModelNode> items = response.get(RESULT).asList();
                    List<String> hostNames = new ArrayList<String>(items.size());
                    for (ModelNode item : items) {
                        hostNames.add(item.asString());
                    }

                    // synchronize servers
                    synchronizeServerModel(hostNames, callback);
                }
            }

        });
    }

    class ServerFn implements Function<Topology> {

        private final String hostName;

        ServerFn(String hostName) {
            this.hostName = hostName;
        }

        @Override
        public void execute(final Control<Topology> control) {

            ModelNode op = new ModelNode();
            op.get(ADDRESS).add("host", hostName);
            op.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
            op.get(CHILD_TYPE).set("server");

            dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
                @Override
                public void onFailure(Throwable caught) {
                    Console.error("Failed to synchronize host model");
                    control.abort();
                }

                @Override
                public void onSuccess(DMRResponse result) {
                    ModelNode response = result.get();
                    control.getContext().addHost(hostName); // TODO: seems to be hacky
                    if (response.isFailure()) {
                        // can happen when no server instances are running
                        control.proceed();
                    } else {
                        List<ModelNode> items = response.get(RESULT).asList();

                        for (ModelNode item : items) {
                            String serverName = item.asString();
                            control.getContext().addServer(hostName, serverName);
                        }

                        control.proceed();
                    }
                }

            });

        }
    }

    private void synchronizeServerModel(final List<String> hostNames, final AsyncCallback<Boolean> callback) {


        List<ServerFn> functions = new ArrayList<>(hostNames.size());
        for(String hostName : hostNames)
            functions.add(new ServerFn(hostName));

        Outcome<Topology> outcome = new Outcome<Topology>() {
            @Override
            public void onFailure(Topology context) {
                callback.onFailure(new RuntimeException("Failed to synchronize host model"));
            }

            @Override
            public void onSuccess(Topology context) {

                HostStore.this.topology = context;
                callback.onSuccess(true);
            }
        };

        new Async().waterfall(new Topology(), outcome, functions.toArray(new Function[functions.size()]));
    }

    @Process(actionType = RefreshServer.class)
    public void onRefreshServer(final Dispatcher.Channel channel) {

        // a) provide at least a selected host
        if(null==selectedHost)
            throw new IllegalStateException("no host selected!");

        // b) (optional) refresh hosts before the sever store loads the servers

        channel.ack();
    }

    @Process(actionType = RefreshHosts.class)
    public void onRefreshHosts(final Dispatcher.Channel channel) {

        synchonizeHosts(new AsyncCallback<Boolean>() {
            @Override
            public void onFailure(Throwable caught) {
                channel.nack(caught);
            }

            @Override
            public void onSuccess(Boolean result) {

                // verify the selected server is still among the list of active servers
                if(!topology.getServerNames(selectedHost).contains(selectedServer))
                    selectedServer = null;

                channel.ack();
                fireChanged(HostStore.class);
            }
        });
    }

    @Process(actionType = HostSelection.class)
    public void onSelectHost(String hostName, final Dispatcher.Channel channel) {

        selectedHost = hostName;

        defaultServerSelection();

        channel.ack();
        fireChanged(HostStore.class);

    }

    @Process(actionType = SelectServerInstance.class)
    public void onSelectedServer(final String serverInstance, final Dispatcher.Channel channel) {
        this.selectedServer = serverInstance;
        channel.ack();
        fireChanged(HostStore.class);
    }

    private void defaultServerSelection() {

        Set<String> instancesOnHost  = topology.getServerNames(selectedHost);

        if(instancesOnHost.size()>0)
        {
            selectedServer = instancesOnHost.iterator().next();
        }
        else if(instancesOnHost.isEmpty())
        {
            // no selection possible
            selectedServer = null;
        }


    }

    // -----------------------------------------
    // data access

    public boolean hasSelectedServer() {
        return selectedServer != null;
    }

    public String getSelectedServer() {
        if(null== selectedServer)
            throw new IllegalStateException("No server instance selected");

        return selectedServer;
    }

    public Set<String> getHostNames() {
        return topology.getHostNames();
    }

    public Topology getTopology() {
        return topology;
    }

    public boolean hasSelecteHost() {
        return selectedHost!=null;
    }

    public String getSelectedHost() {
        return selectedHost;
    }

    public class NoHostsAvailable extends RuntimeException {

    }
}
