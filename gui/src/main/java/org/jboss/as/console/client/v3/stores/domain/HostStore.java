package org.jboss.as.console.client.v3.stores.domain;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.v3.stores.domain.actions.HostSelection;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshHosts;
import org.jboss.as.console.client.v3.stores.domain.actions.SelectServer;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
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

    private Topology topology;

    @Inject
    public HostStore( DispatchAsync dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void init(final AsyncCallback<Set<String>> callback) {
        synchronizeHosts(new AsyncCallback<Boolean>() {
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

                System.out.println("<< default host selection: "+ selectedHost +" >>");

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

    private void synchronizeHosts(final AsyncCallback<Boolean> callback) {
        ModelNode op = new ModelNode();
        op.get(ADDRESS).setEmptyList();
        op.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
        op.get(CHILD_TYPE).set("host");

        Footer.PROGRESS_ELEMENT.reset();

        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                Footer.PROGRESS_ELEMENT.finish();
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Footer.PROGRESS_ELEMENT.finish();
                    callback.onFailure(new RuntimeException("Failed to synchronize host model: " + response.getFailureDescription()));
                } else {
                    List<ModelNode> items = response.get(RESULT).asList();
                    List<String> hostNames = new ArrayList<String>(items.size());
                    for (ModelNode item : items) {
                        hostNames.add(item.asString());
                    }

                    Footer.PROGRESS_ELEMENT.finish();

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
            op.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
            op.get(CHILD_TYPE).set("server-config");
            op.get(INCLUDE_RUNTIME).set(true);

            dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
                @Override
                public void onFailure(Throwable caught) {
                    Console.error("Failed to synchronize server model: "+caught.getMessage());
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
                        List<Property> servers = response.get(RESULT).asPropertyList();

                        for(Property server : servers)
                        {
                            if(server.getValue().get("status").asString().equalsIgnoreCase("started"))
                                control.getContext().addServer(hostName, server.getName());
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
                callback.onFailure(new RuntimeException("Store synchronisation error (Check the logs for further details)"));
            }

            @Override
            public void onSuccess(Topology context) {

                HostStore.this.topology = context;
                callback.onSuccess(true);
            }
        };

        new Async(Footer.PROGRESS_ELEMENT).waterfall(new Topology(), outcome, functions.toArray(new Function[functions.size()]));
    }

    @Process(actionType = RefreshHosts.class)
    public void onRefreshHosts(final Dispatcher.Channel channel) {

        synchronizeHosts(new AsyncCallback<Boolean>() {
            @Override
            public void onFailure(Throwable caught) {
                channel.nack(caught);
            }

            @Override
            public void onSuccess(Boolean result) {

                channel.ack();
            }
        });
    }

    @Process(actionType = HostSelection.class)
    public void onSelectHost(HostSelection action, final Dispatcher.Channel channel) {
        selectedHost = action.getHostName();
        channel.ack();
    }

    @Process(actionType = SelectServer.class)
    public void onSelectServer(SelectServer action, final Dispatcher.Channel channel) {
        selectedHost = action.getHost();
        channel.ack();
    }

    // -----------------------------------------
    // data access

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
