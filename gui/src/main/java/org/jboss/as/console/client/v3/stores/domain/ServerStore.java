package org.jboss.as.console.client.v3.stores.domain;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.view.client.ProvidesKey;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.ServerInstance;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.model.ModelAdapter;
import org.jboss.as.console.client.shared.util.DMRUtil;
import org.jboss.as.console.client.v3.stores.domain.actions.AddServer;
import org.jboss.as.console.client.v3.stores.domain.actions.CopyServer;
import org.jboss.as.console.client.v3.stores.domain.actions.GroupSelection;
import org.jboss.as.console.client.v3.stores.domain.actions.HostSelection;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshServer;
import org.jboss.as.console.client.v3.stores.domain.actions.RemoveServer;
import org.jboss.as.console.client.v3.stores.domain.actions.SelectServer;
import org.jboss.as.console.client.v3.stores.domain.actions.FilterType;
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
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @since 15/07/14
 */
@Store
public class ServerStore extends ChangeSupport {

    private final DispatchAsync dispatcher;
    private final ApplicationMetaData propertyMetaData;
    private final HostInformationStore hostInfo;
    private final HostStore hostStore;

    private Map<String, List<Server>> host2server = new HashMap<>();
    private Map<String, List<ServerInstance>> instanceModel = new HashMap<>();
    private ServerRef selectServer;
    private String filter = FilterType.HOST;
    private String selectedGroup;

    @Inject
    public ServerStore(HostStore hostStore, HostInformationStore hostInfo, DispatchAsync dispatcher, ApplicationMetaData propertyMetaData) {
        this.hostStore = hostStore;
        this.hostInfo = hostInfo;
        this.dispatcher = dispatcher;
        this.propertyMetaData = propertyMetaData;
    }

    // -----------------------------------------------
    // init

    public void init(final String hostName, final AsyncCallback<List<Server>> callback) {
        synchronizeServerModel(hostName, new AsyncCallback<Boolean>() {
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
    public void onSelectGroup(String groupName, final Dispatcher.Channel channel) {
        this.selectedGroup = groupName;
        channel.ack();
    }

    @Process(actionType = SelectServer.class)
    public void onSelectServer(String server, String host, final Dispatcher.Channel channel) {

        this.selectServer = new ServerRef(host, server);
        channel.ack();

    }

    public final class ServerRef {
        String hostName;
        String serverName;

        public ServerRef(String hostName, String serverName) {
            this.hostName = hostName;
            this.serverName = serverName;
        }

        public String getHostName() {
            return hostName;
        }

        public String getServerName() {
            return serverName;
        }
    }

    class RefreshValues {
        List<Server> servers;
        List<ServerInstance> instances;
    }

    @Process(actionType = RefreshServer.class, dependencies = {HostStore.class})
    public void onRefresh(final Dispatcher.Channel channel) {

        final String hostName = hostStore.getSelectedHost();

        synchronizeServerModel(hostName, new AsyncCallback<Boolean>() {
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

    private void synchronizeServerModel(final String hostName, final AsyncCallback<Boolean> callback) {

        Function<RefreshValues> fetchServers = new Function<RefreshValues>() {
            @Override
            public void execute(final Control<RefreshValues> control) {
                hostInfo.getServerConfigurations(hostName, new SimpleCallback<List<Server>>() {
                    @Override
                    public void onSuccess(List<Server> servers) {
                        control.getContext().servers = servers;
                        control.proceed();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        control.abort();
                        Console.error("Failed to load servers", caught.getMessage());
                    }
                });

            }
        };

        Function<RefreshValues> fetchInstances = new Function<RefreshValues>() {
            @Override
            public void execute(final Control<RefreshValues> control) {
                hostInfo.getServerInstances(hostName, new SimpleCallback<List<ServerInstance>>() {
                    @Override
                    public void onSuccess(List<ServerInstance> servers) {
                        control.getContext().instances = servers;
                        control.proceed();
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        control.abort();
                        Console.error("Failed to load server instances", caught.getMessage());
                    }
                });

            }
        };

        Outcome<RefreshValues> outcome = new Outcome<RefreshValues>() {
            @Override
            public void onFailure(RefreshValues context) {
                callback.onFailure(new RuntimeException("Failed to synchronize server model"));
            }

            @Override
            public void onSuccess(RefreshValues context) {

                host2server.put(hostName, context.servers);
                instanceModel.put(hostName, context.instances);

                callback.onSuccess(true);
            }
        };

        new Async().waterfall(new RefreshValues(), outcome, fetchServers, fetchInstances);
    }

    @Process(actionType = AddServer.class)
    public void onAddServer(final AddServer action, final Dispatcher.Channel channel) {


        hostInfo.createServerConfig(action.getServer().getHostName(), action.getServer(), new SimpleCallback<Boolean>() {

            @Override
            public void onSuccess(Boolean success) {

               onRefresh(channel);
            }

            @Override
            public void onFailure(Throwable caught) {
                Console.error("Failed to add server", caught.getMessage());
                channel.nack(caught);
            }
        });

    }

    @Process(actionType = RemoveServer.class, dependencies = {HostStore.class})
    public void onRemoveServer(final RemoveServer action, final Dispatcher.Channel channel) {

        hostInfo.deleteServerConfig(action.getServer().getHostName(), action.getServer(), new SimpleCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean success) {

                onRefresh(channel);
            }

            @Override
            public void onFailure(Throwable caught) {
                Console.error("Failed to remove server", caught.getMessage());
                channel.nack(caught);
            }
        });
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
                channel.nack(caught);
            }
        });
    }

    @Process(actionType = FilterType.class)
    public void onSetFilter(final String filter, final Dispatcher.Channel channel) {
        this.filter = filter;
        channel.ack();
    }

    @Process(actionType = CopyServer.class)
    public void onSaveCopy(final CopyServer action, final Dispatcher.Channel channel) {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(ADDRESS).setEmptyList();
        operation.get(ADDRESS).add("host", action.getNewServer().getHostName());
        operation.get(ADDRESS).add("server-config", action.getNewServer().getName());
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
        List<Server> servers = host2server.get(host);
        return servers != null ? servers : new ArrayList<Server>();
    }

    public List<Server> getServerForGroup(String group) {

        List<Server> matchingServer = new ArrayList<>();
        for (Map.Entry<String, List<Server>> servers : host2server.entrySet()) {
            for (Server server : servers.getValue()) {
                if(server.getGroup().equals(group))
                {
                    matchingServer.add(server);
                }
            }
        }
        return matchingServer;
    }


    public ServerInstance getServerInstance(String name) {
        ServerInstance match = null;
        for(ServerInstance server : instanceModel.get(hostStore.getSelectedHost()))
        {
            if(server.getName().equals(name))
            {
                match = server;
                break;
            }
        }

        if(null==match)
            throw new IllegalArgumentException("No such server instance "+ name);

        return match;
    }

    public List<ServerInstance> getServerInstances(String host) {
        List<ServerInstance> serverInstances = instanceModel.get(host);
        return serverInstances != null ? serverInstances : new ArrayList<ServerInstance>();
    }

    public ServerRef getSelectServer() {
        return selectServer;
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
