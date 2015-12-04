package org.jboss.as.console.client.v3.stores.domain;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.core.UIMessages;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.ServerInstance;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.domain.topology.HostInfo;
import org.jboss.as.console.client.domain.topology.ServerGroup;
import org.jboss.as.console.client.domain.topology.TopologyFunctions;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.shared.model.ModelAdapter;
import org.jboss.as.console.client.shared.util.DMRUtil;
import org.jboss.as.console.client.v3.stores.domain.actions.AddServer;
import org.jboss.as.console.client.v3.stores.domain.actions.CopyServer;
import org.jboss.as.console.client.v3.stores.domain.actions.FilterType;
import org.jboss.as.console.client.v3.stores.domain.actions.GroupSelection;
import org.jboss.as.console.client.v3.stores.domain.actions.HostSelection;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshServer;
import org.jboss.as.console.client.v3.stores.domain.actions.RemoveServer;
import org.jboss.as.console.client.v3.stores.domain.actions.SelectServer;
import org.jboss.as.console.client.v3.stores.domain.actions.UpdateServer;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.PropertyBinding;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Outcome;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @since 15/07/14
 */
@Store
public class ServerStore extends ChangeSupport {

    private final DispatchAsync dispatcher;
    private final ApplicationMetaData propertyMetaData;
    private final BeanFactory beanFactory;
    private final HostInformationStore hostInfo;
    private final HostStore hostStore;

    private Map<String, ServerGroup> serverGroups = new HashMap<>();
    private Map<String, List<Server>> host2server = new HashMap<>();
    private Map<String, List<ServerInstance>> instanceModel = new HashMap<>();

    private ServerRef selectedServer;
    private String filter = FilterType.HOST;
    private String selectedGroup;

    @Inject
    public ServerStore(HostStore hostStore, HostInformationStore hostInfo,
                       DispatchAsync dispatcher, ApplicationMetaData propertyMetaData,
                       BeanFactory beanFactory) {
        this.hostStore = hostStore;
        this.hostInfo = hostInfo;
        this.dispatcher = dispatcher;
        this.propertyMetaData = propertyMetaData;
        this.beanFactory = beanFactory;
    }

    // -----------------------------------------------
    // init

    public void init(final String hostName, final AsyncCallback<List<Server>> callback) {
        synchronizeServerModel(new AsyncCallback<Boolean>() {
            @Override
            public void onFailure(Throwable throwable) {
                callback.onFailure(throwable);
            }

            @Override
            public void onSuccess(Boolean aBoolean) {
                callback.onSuccess(host2server.get(hostName));
            }
        });
    }

    // -----------------------------------------------
    // action processing

    @Process(actionType = HostSelection.class, dependencies = {HostStore.class})
    public void onSelectHost(final HostSelection action, final Dispatcher.Channel channel) {

        // load the server data on demand
        if(!host2server.containsKey(action.getHostName()))
            onRefresh(channel);
        else {

            channel.ack();
        }

    }

    @Process(actionType = GroupSelection.class)
    public void onSelectGroup(GroupSelection selection, final Dispatcher.Channel channel) {
        this.selectedGroup = selection.getGroupName();
        channel.ack();
    }

    @Process(actionType = SelectServer.class)
    public void onSelectServer(SelectServer selection, final Dispatcher.Channel channel) {

        this.selectedServer = new ServerRef(selection.getHost(), selection.getServer());
        channel.ack();

    }

    @Process(actionType = RefreshServer.class)
    public void onRefresh(final Dispatcher.Channel channel) {

        synchronizeServerModel(new AsyncCallback<Boolean>() {
            @Override
            public void onFailure(Throwable throwable) {
                channel.nack(throwable);
            }

            @Override
            public void onSuccess(Boolean aBoolean) {
                channel.ack();
            }
        });

    }

    private void synchronizeServerModel(final AsyncCallback<Boolean> callback) {

        Outcome<FunctionContext> outcome = new Outcome<FunctionContext>() {
            @Override
            public void onFailure(final FunctionContext context) {
                callback.onFailure(new RuntimeException("Unable to load topology: "+context.getErrorMessage()));
            }

            @Override
            public void onSuccess(final FunctionContext context) {


                host2server.clear();
                instanceModel.clear();

                List<HostInfo> hosts = context.pop();
                deriveGroups(hosts);

                for (HostInfo h : hosts) {
                    host2server.put(h.getName(), h.getServerConfigs());
                    instanceModel.put(h.getName(), h.getServerInstances());
                }

                callback.onSuccess(true);
            }
        };

        new Async<FunctionContext>(Footer.PROGRESS_ELEMENT).waterfall(new FunctionContext(), outcome,
                new TopologyFunctions.ReadHostsAndGroups(dispatcher),
                new TopologyFunctions.ReadServerConfigs(dispatcher, beanFactory),
                new TopologyFunctions.FindRunningServerInstances(dispatcher),
                new TopologyFunctions.EffectivePortOffset(dispatcher));
    }

    /**
     * Builds {@link ServerGroup} instances and populates the map {@link #serverGroups}
     */
    private SortedSet<ServerGroup> deriveGroups(List<HostInfo> hosts) {
        serverGroups.clear();
        for (HostInfo host : hosts) {
            List<ServerInstance> serverInstances = host.getServerInstances();
            for (ServerInstance server : serverInstances) {
                String group = server.getGroup();
                String profile = server.getProfile();
                ServerGroup serverGroup = serverGroups.get(group);
                if (serverGroup == null) {
                    serverGroup = new ServerGroup(group, profile);
                    serverGroup.fill(hosts);
                    serverGroups.put(group, serverGroup);
                }
            }
        }
        return new TreeSet<ServerGroup>(serverGroups.values());
    }

    @Process(actionType = AddServer.class)
    public void onAddServer(final AddServer action, final Dispatcher.Channel channel) {


        hostInfo.createServerConfig(action.getServer().getHostName(), action.getServer(), new SimpleCallback<Boolean>() {

            @Override
            public void onSuccess(Boolean success) {
                Console.info(Console.MESSAGES
                        .successfullyAddedServer(action.getServer().getName()));
                onRefresh(channel);
            }

            @Override
            public void onFailure(Throwable caught) {
                Console.error(Console.CONSTANTS.failedToAddServer(), caught.getMessage());
                channel.nack(caught);
            }
        });

    }

    @Process(actionType = RemoveServer.class, dependencies = {HostStore.class})
    public void onRemoveServer(final RemoveServer action, final Dispatcher.Channel channel) {

        hostInfo.deleteServerConfig(action.getServerRef().getHostName(), findServer(action.getServerRef()), new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {

                Console.info(Console.MESSAGES
                        .successfullyRemovedServer(action.getServerRef().getServerName()));
                onRefresh(channel);
            }

            @Override
            public void onFailure(Throwable caught) {
                Console.error(Console.CONSTANTS.failedToRemoveServer(), caught.getMessage());
                channel.nack(caught);
            }
        });
    }

    public Server findServer(ServerRef ref) {
        Server match = null;
        List<Server> servers = host2server.get(ref.getHostName());
        for (Server server : servers) {
            if(server.getName().equals(ref.getServerName()))
            {
                match = server;
                break;
            }
        }
        return match;
    }

    @Process(actionType = UpdateServer.class)
    public void onUpdateServer(final UpdateServer action, final Dispatcher.Channel channel) {

        if (action.getChangedValues().containsKey("portOffset")) {
            action.getChangedValues().put("socketBinding", action.getServer().getSocketBinding());
        }
        if (action.getChangedValues().containsKey("socketBinding")) {
            action.getChangedValues().put("portOffset", action.getServer().getPortOffset());
        }

        final String name = action.getServer().getName();

        ModelNode proto = new ModelNode();
        proto.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        proto.get(ADDRESS).add("host", action.getServer().getHostName());
        proto.get(ADDRESS).add(ModelDescriptionConstants.SERVER_CONFIG, name);

        List<PropertyBinding> bindings = propertyMetaData.getBindingsForType(Server.class);
        ModelNode operation = ModelAdapter.detypedFromChangeset(proto, action.getChangedValues(), bindings);

        // TODO: https://issues.jboss.org/browse/AS7-3643

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.modificationFailed("Server Configuration ") + name,
                            response.getFailureDescription());

                } else {
                    Console.info(Console.MESSAGES.modified("Server Configuration ") + name);
                }

                onRefresh(channel);
            }

            @Override
            public void onFailure(Throwable caught) {
                Console.error(Console.MESSAGES.modificationFailed("Server Configuration ") + name,  caught.getMessage());
                channel.nack(caught);
            }
        });
    }

    @Process(actionType = FilterType.class)
    public void onSetFilter(final FilterType filter, final Dispatcher.Channel channel) {
        this.filter = filter.getFilter();
        channel.ack();
    }

    @Process(actionType = CopyServer.class)
    public void onSaveCopy(final CopyServer action, final Dispatcher.Channel channel) {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(ADDRESS).setEmptyList();
        operation.get(ADDRESS).add("host", action.getNewServer().getHostName());
        operation.get(ADDRESS).add("server-config", action.getOriginal().getName());
        operation.get(RECURSIVE).set(true);

        dispatcher.execute(new DMRAction(operation, false), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                Console.error("Failed to read server-config: " + action.getOriginal().getName(), caught.getMessage());
                channel.nack(caught);
            }

            @Override
            public void onSuccess(DMRResponse result) {

                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error("Failed to read server-config: " + action.getOriginal().getName(),
                            response.getFailureDescription());
                } else {
                    ModelNode model = response.get("result").asObject();

                    // required attribute changes: portOffset & serverGroup
                    model.get("socket-binding-port-offset").set(action.getNewServer().getPortOffset());
                    model.remove("name");

                    // re-create node

                    ModelNode compositeOp = new ModelNode();
                    compositeOp.get(OP).set(COMPOSITE);
                    compositeOp.get(ADDRESS).setEmptyList();

                    List<ModelNode> steps = new ArrayList<ModelNode>();

                    final ModelNode rootResourceOp = new ModelNode();
                    rootResourceOp.get(OP).set(ADD);
                    rootResourceOp.get(ADDRESS).add("host", action.getTargetHost());
                    rootResourceOp.get(ADDRESS).add("server-config", action.getNewServer().getName());

                    steps.add(rootResourceOp);

                    DMRUtil.copyResourceValues(model, rootResourceOp, steps);

                    compositeOp.get(STEPS).set(steps);

                    dispatcher.execute(new DMRAction(compositeOp), new SimpleCallback<DMRResponse>() {
                        @Override
                        public void onSuccess(DMRResponse dmrResponse) {
                            ModelNode response = dmrResponse.get();

                            if (response.isFailure()) {
                                Console.error("Failed to copy server-config", response.getFailureDescription());
                            } else {
                                Console.info("Successfully copied server-config '" + action.getNewServer().getName() + "'");
                            }

                            onRefresh(channel);
                        }
                    });

                }

            }

        });


    }

    // -----------------------------------------------
    // data access

    public String getFilter() {
        return filter;
    }

    public String getSelectedGroup() {
        return selectedGroup;
    }

    public List<Server> getServerForHost(String host) {
        List<Server> serversOnHost = host2server.get(host);
        List<Server> servers = serversOnHost!=null ? serversOnHost : new ArrayList<Server>();
        normalizeModel(servers);
        return servers;
    }

    public List<Server> getServerForGroup(String group) {

        List<Server> matchingServer = new ArrayList<>();
        for (String host : host2server.keySet()) {
            List<Server> servers = host2server.get(host);
            for (Server server : servers) {
                if(server.getGroup().equals(group))
                {
                    matchingServer.add(server);
                }
            }
        }

        normalizeModel(matchingServer);

        return matchingServer;
    }

    /**
     * A workaround to consolidate the server-config and server resource model representations
     * TODO: https://issues.jboss.org/browse/WFLY-4910
     *
     * @param servers
     */
    private void normalizeModel(List<Server> servers) {

        for (Server server : servers) {
            ServerInstance serverInstance = getServerInstance(new ServerRef(server.getHostName(), server.getName()));
            server.setServerState(serverInstance.getServerState());
            server.setSuspendState(serverInstance.getSuspendState());
        }
    }

    public ServerInstance getServerInstance(ServerRef serverRef) {
        ServerInstance match = null;
        for(ServerInstance server : instanceModel.get(serverRef.getHostName()))
        {
            if(server.getName().equals(serverRef.getServerName()))
            {
                match = server;
                break;
            }
        }

        if(null==match)
            throw new IllegalArgumentException("No such server instance "+ serverRef);

        return match;
    }

    public List<ServerInstance> getServerInstances(String host) {
        List<ServerInstance> serverInstances = instanceModel.get(host);
        return serverInstances != null ? serverInstances : new ArrayList<ServerInstance>();
    }

    public boolean hasSelectedServer() {
        return selectedServer !=null;
    }

    public ServerRef getSelectedServer() {
        return selectedServer;
    }

    // -----------------------------------------------
    // utility

    private final static ProvidesKey<Server> SERVER_KEY = new ProvidesKey<Server>() {
        @Override
        public Object getKey(Server server) {
            return server.getName()+"_"+server.getGroup();
        }
    };

    private final static ProvidesKey<ServerInstance> SERVER_INSTANCE_KEY = new ProvidesKey<ServerInstance>() {
        @Override
        public Object getKey(ServerInstance server) {
            return server.getName()+"_"+server.getGroup();
        }
    };
}
