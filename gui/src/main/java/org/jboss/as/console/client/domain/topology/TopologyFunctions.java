package org.jboss.as.console.client.domain.topology;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.RuntimeState;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.ServerFlag;
import org.jboss.as.console.client.domain.model.ServerInstance;
import org.jboss.as.console.client.domain.model.SrvState;
import org.jboss.as.console.client.domain.model.SuspendState;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.flow.FunctionCallback;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.v3.dmr.Composite;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * Collection of topology related functions under a common namespace.
 *
 * @author Harald Pehl
 */
public final class TopologyFunctions {

    public static final String HOSTS_KEY = TopologyFunctions.class.getName() + ".hosts";
    public static final String GROUPS_KEY = TopologyFunctions.class.getName() + ".groups";
    public static final String GROUP_TO_PROFILE_KEY = TopologyFunctions.class.getName() + ".groupToProfile";


    private TopologyFunctions() {

    }

    /**
     * Reads the hosts and groups and puts them as a list of {@link HostInfo} and {@link ServerGroup} instances under
     * the keys {@link #HOSTS_KEY} and {@link #GROUPS_KEY} into the context. Creates a {@code Map&lt;String,
     * String&gt;} between group and profile names and put it under the key {@link #GROUP_TO_PROFILE_KEY} into the
     * context.
     */
    public static class ReadHostsAndGroups implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;

        public ReadHostsAndGroups(final DispatchAsync dispatcher) {
            this.dispatcher = dispatcher;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            Operation hostsOp = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, ResourceAddress.ROOT)
                    .param(CHILD_TYPE, "host")
                    .build();

            Operation groupsOp = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, ResourceAddress.ROOT)
                    .param(CHILD_TYPE, "server-group")
                    .build();

            dispatcher.execute(new DMRAction(new Composite(hostsOp, groupsOp), false), new FunctionCallback(control) {
                @Override
                public void onSuccess(final ModelNode result) {
                    ModelNode stepsResult = result.get(RESULT);

                    List<HostInfo> hosts = new LinkedList<>();
                    ModelNode currentStep = stepsResult.get("step-1");
                    if (currentStep.get(RESULT).isDefined()) {
                        List<Property> properties = currentStep.get(RESULT).asPropertyList();
                        for (Property property : properties) {
                            String name = property.getName();
                            ModelNode hostModel = property.getValue();
                            boolean isController = hostModel.get("domain-controller").hasDefined("local");
                            HostInfo hostInfo = new HostInfo(name, isController);
                            hosts.add(hostInfo);
                        }
                    }
                    context.set(HOSTS_KEY, hosts);

                    List<ServerGroup> groups = new LinkedList<>();
                    Map<String, String> groupToProfile = new HashMap<>();
                    currentStep = stepsResult.get("step-2");
                    if (currentStep.get(RESULT).isDefined()) {
                        List<Property> properties = currentStep.get(RESULT).asPropertyList();
                        for (Property property : properties) {
                            String groupName = property.getName();
                            ModelNode groupModel = property.getValue();
                            String profile = groupModel.get("profile").asString();
                            ServerGroup serverGroup = new ServerGroup(groupName, profile);
                            groups.add(serverGroup);
                            groupToProfile.put(groupName, profile);
                        }
                    }
                    context.set(GROUPS_KEY, groups);
                    context.set(GROUP_TO_PROFILE_KEY, groupToProfile);
                }
            });
        }
    }


    /**
     * Reads all server configs of all hosts found in the context under the key {@link #HOSTS_KEY}. Updates the server
     * configs in the hosts using {@link HostInfo#setServerInstances(List)}. Depends on the keys {@link #HOSTS_KEY} and
     * {@link #GROUP_TO_PROFILE_KEY}.
     */
    public static class ReadServerConfigs implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final BeanFactory beanFactory;
        private final EntityAdapter<Server> serverAdapter;

        public ReadServerConfigs(final DispatchAsync dispatcher, final BeanFactory beanFactory) {

            this.dispatcher = dispatcher;
            this.beanFactory = beanFactory;
            this.serverAdapter = new EntityAdapter<Server>(Server.class, Console.MODULES.getApplicationMetaData());
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            int step = 1;
            List<Operation> steps = new LinkedList<>();
            Map<String, HostInfo> stepToHost = new HashMap<>();
            List<HostInfo> hosts = control.getContext().get(HOSTS_KEY);
            Map<String, String> groupToProfile = control.getContext().get(GROUP_TO_PROFILE_KEY);
            for (HostInfo hostInfo : hosts) {
                ResourceAddress address = new ResourceAddress().add("host", hostInfo.getName());
                Operation scOp = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, address)
                        .param(INCLUDE_RUNTIME, true)
                        .param(CHILD_TYPE, "server-config")
                        .build();
                steps.add(scOp);
                stepToHost.put("step-" + step, hostInfo);
                step++;
            }

            dispatcher.execute(new DMRAction(new Composite(steps)), new FunctionCallback(control) {
                @Override
                public void onSuccess(final ModelNode result) {
                    ModelNode stepsResult = result.get(RESULT);
                    for (Map.Entry<String, HostInfo> entry : stepToHost.entrySet()) {
                        String step = entry.getKey();
                        HostInfo hostInfo = entry.getValue();
                        ModelNode currentStep = stepsResult.get(step);

                        if (currentStep.get(RESULT).isDefined()) {

                            List<Server> serverConfigs = new LinkedList<>();
                            List<ServerInstance> servers = new LinkedList<>();
                            List<Property> properties = currentStep.get(RESULT).asPropertyList();

                            for (Property property : properties) {
                                String name = property.getName();
                                ModelNode scModel = property.getValue();

                                // SERVER CONFIG
                                Server server = serverAdapter.fromDMR(scModel);
                                server.setHostName(hostInfo.getName());
                                server.setStarted(scModel.get("status").asString().equalsIgnoreCase("STARTED"));
                                server.setRuntimeState(RuntimeState.valueOf(scModel.get("status").asString()));

                                // Does not exists (yet)
                                //server.setServerState(SrvState.valueOf(model.get("server-state").asString().replace("-", "_").toUpperCase()));

                                server.setSuspendState(SuspendState.UNKOWN);   // TODO: https://issues.jboss.org/browse/WFLY-4910
                                server.setProfile(groupToProfile.get(server.getGroup()));
                                serverConfigs.add(server);

                                // SERVER INSTANCE
                                String group = scModel.get("group").asString();

                                ServerInstance serverInstance = beanFactory.serverInstance().as();
                                serverInstance.setName(name);
                                serverInstance.setServer(name);
                                serverInstance.setGroup(group);
                                serverInstance.setProfile(groupToProfile.get(group));
                                serverInstance.setHost(hostInfo.getName());
                                serverInstance.setRunning(scModel.get("status").asString().equalsIgnoreCase("STARTED"));
                                serverInstance.setSocketBindings(new HashMap<>());

                                servers.add(serverInstance);
                            }

                            hostInfo.setServerConfigs(serverConfigs);
                            hostInfo.setServerInstances(servers);
                        }
                    }
                }
            });
        }
    }


    /**
     * Looks for running server instances across the domain. Depends on the list of hosts stored under the key
     * {@link #HOSTS_KEY} in the context. For each running server the state, socket bindings and port offset is
     * read and stored in the related {@link ServerInstance} instance.
     */
    public static class FindRunningServerInstances implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;

        public FindRunningServerInstances(final DispatchAsync dispatcher) {
            this.dispatcher = dispatcher;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            int step = 1;
            List<Operation> steps = new LinkedList<>();
            Map<String, ServerInstance> stepToServer = new HashMap<>();
            List<HostInfo> hosts = control.getContext().get(HOSTS_KEY);

            for (HostInfo hostInfo : hosts) {

                for (ServerInstance serverInstance : hostInfo.getServerInstances()) {
                    if (serverInstance.isRunning()) {

                        ResourceAddress address = new ResourceAddress()
                                .add("host", hostInfo.getName())
                                .add("server", serverInstance.getName());

                        Operation serverStateOp = new Operation.Builder(READ_ATTRIBUTE_OPERATION, address)
                                .param(NAME, "server-state")
                                .build();
                        steps.add(serverStateOp);
                        stepToServer.put("step-" + step, serverInstance);
                        step++;

                        Operation suspendState = new Operation.Builder(READ_ATTRIBUTE_OPERATION, address)
                                .param(NAME, "suspend-state")
                                .build();
                        steps.add(suspendState);
                        stepToServer.put("step-" + step, serverInstance);
                        step++;

                        Operation socketBindingOp = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, address)
                                .param(INCLUDE_RUNTIME, true)
                                .param(CHILD_TYPE, "socket-binding-group")
                                .build();
                        steps.add(socketBindingOp);
                        stepToServer.put("step-" + step, serverInstance);
                        step++;
                    }
                }

            }

            // HAL-921: no running server instances
            if(!steps.isEmpty()) {
                dispatcher.execute(new DMRAction(new Composite(steps)), new FunctionCallback(control) {
                    @Override
                    public void onSuccess(final ModelNode result) {
                        ModelNode stepsResult = result.get(RESULT);
                        if (stepsResult.isDefined()) {
                            List<Property> properties = stepsResult.asPropertyList();
                            for (Iterator<Property> iterator = properties.iterator(); iterator.hasNext(); ) {
                                Property property = iterator.next();
                                String step = property.getName();
                                ModelNode node = property.getValue();
                                ServerInstance serverInstance = stepToServer.get(step);
                                if (serverInstance != null) {

                                    // step-n: server state
                                    if (node.get(RESULT).isDefined()) {
                                        String state = node.get(RESULT).asString();

                                        serverInstance.setServerState(
                                                SrvState.valueOf(state.replace("-", "_").toUpperCase())
                                        );

                                        if (state.equals("reload-required")) {
                                            serverInstance.setFlag(ServerFlag.RELOAD_REQUIRED);
                                        } else if (state.equals("restart-required")) {
                                            serverInstance.setFlag(ServerFlag.RESTART_REQUIRED);
                                        }
                                    }

                                    // step-n + 1: socket binding groups
                                    property = iterator.next();
                                    node = property.getValue();
                                    if (node.get(RESULT).isDefined()) {
                                        String state = node.get(RESULT).asString();
                                        serverInstance.setSuspendState(SuspendState.valueOf(state));
                                    }

                                    // step-n + 1: socket binding groups
                                    property = iterator.next();
                                    node = property.getValue();
                                    if (node.get(RESULT).isDefined()) {
                                        List<Property> sockets = node.get(RESULT).asPropertyList();
                                        for (Property socket : sockets) {
                                            serverInstance.getSocketBindings()
                                                    .put(socket.getName(), socket.getValue().get("port-offset").asString());
                                        }
                                    }
                                }
                            }
                        } // else no running servers!
                        context.push(hosts);
                    }


                });
            }
            else {
                control.getContext().push(hosts);
                control.proceed();
            }
        }
    }

    public static class EffectivePortOffset implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;

        public EffectivePortOffset(final DispatchAsync dispatcher) {
            this.dispatcher = dispatcher;
        }

        @Override
        public void execute(Control<FunctionContext> control) {
            final ModelNode node = new ModelNode();
            node.get(ADDRESS).setEmptyList();
            node.get(OP).set(COMPOSITE);
            List<ModelNode> steps = new LinkedList<>();

            int step = 1;
            final Map<String, ServerInstance> stepToServer = new HashMap<>();
            final Map<String, HostInfo> stepToHost = new HashMap<>();
            final List<HostInfo> hosts = control.getContext().get(HOSTS_KEY);
            for (HostInfo hostInfo : hosts) {
                for (ServerInstance serverInstance : hostInfo.getServerInstances()) {
                    if (serverInstance.isRunning() && !serverInstance.getSocketBindings().isEmpty()) {
                        String socketBindingGroup = serverInstance.getSocketBindings().keySet().iterator().next();
                        ModelNode portOffsetOp = new ModelNode();
                        portOffsetOp.get(OP).set(READ_RESOURCE_OPERATION);
                        portOffsetOp.get(ADDRESS).add("host", hostInfo.getName());
                        portOffsetOp.get(ADDRESS).add("server", serverInstance.getName());
                        portOffsetOp.get(ADDRESS).add("socket-binding-group", socketBindingGroup);

                        steps.add(portOffsetOp);
                        String stepKey = "step-" + step;
                        stepToHost.put(stepKey, hostInfo);
                        stepToServer.put(stepKey, serverInstance);
                        step++;
                    }
                }
            }


            node.get(STEPS).set(steps);
            dispatcher.execute(new DMRAction(node), new FunctionCallback(control) {
                @Override
                public void onSuccess(final ModelNode response) {
                    ModelNode result = response.get(RESULT);
                    if (result.isDefined()) {
                        for (String stepKey : result.keys()) {
                            ModelNode stepResult = result.get(stepKey).get(RESULT);

                            if (!stepResult.isDefined() || !stepResult.hasDefined("port-offset"))
                                continue;

                            String portOffset = stepResult.get("port-offset").asString();
                            ServerInstance stepServer = stepToServer.get(stepKey);
                            HostInfo hostInfo = stepToHost.get(stepKey);
                            for (Server server : hostInfo.getServerConfigs()) {
                                if (server.getName().equals(stepServer.getName())) {
                                    server.setEffectivePortOffset(portOffset);
                                    break;
                                }
                            }
                        }
                    }
                }
            });
        }
    }
}
