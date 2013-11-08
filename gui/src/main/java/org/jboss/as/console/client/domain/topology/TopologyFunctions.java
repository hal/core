package org.jboss.as.console.client.domain.topology;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jboss.as.console.client.domain.model.ServerFlag;
import org.jboss.as.console.client.domain.model.ServerInstance;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.flow.FunctionCallback;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

/**
 * @author Harald Pehl
 */
public final class TopologyFunctions {

    public static final String HOSTS_KEY = "hosts";
    public static final String GROUPS_KEY = "groups";
    public static final String GROUP_TO_PROFILE_KEY = "groupToProfile";

    private TopologyFunctions() {}

    public static class HostsAndGroups implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;

        public HostsAndGroups(final DispatchAsync dispatcher) {
            this.dispatcher = dispatcher;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            final ModelNode node = new ModelNode();
            node.get(ADDRESS).setEmptyList();
            node.get(OP).set(COMPOSITE);
            List<ModelNode> steps = new LinkedList<ModelNode>();

            ModelNode hostsOp = new ModelNode();
            hostsOp.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
            hostsOp.get(CHILD_TYPE).set("host");
            hostsOp.get(ADDRESS).setEmptyList();
            steps.add(hostsOp);

            ModelNode groupsOp = new ModelNode();
            groupsOp.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
            groupsOp.get(ADDRESS).setEmptyList();
            groupsOp.get(CHILD_TYPE).set("server-group");
            steps.add(groupsOp);

            node.get(STEPS).set(steps);
            dispatcher.execute(new DMRAction(node, false), new FunctionCallback(control) {
                @Override
                protected void onSuccess(final ModelNode result) {
                    ModelNode stepsResult = result.get(RESULT);

                    List<HostInfo> hosts = new LinkedList<HostInfo>();
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
                    control.getContext().set(HOSTS_KEY, hosts);

                    List<ServerGroup> groups = new LinkedList<ServerGroup>();
                    Map<String, String> groupToProfile = new HashMap<String, String>();
                    currentStep = stepsResult.get("step-2");
                    if (currentStep.get(RESULT).isDefined()) {
                        List<Property> properties = currentStep.get(RESULT).asPropertyList();
                        for (Property property : properties) {
                            String name = property.getName();
                            ModelNode groupModel = property.getValue();
                            String profile = groupModel.get("profile").asString();
                            ServerGroup serverGroup = new ServerGroup(name, profile);
                            groups.add(serverGroup);
                            groupToProfile.put(name, profile);
                        }
                    }
                    control.getContext().set(GROUPS_KEY, groups);
                    control.getContext().set(GROUP_TO_PROFILE_KEY, groupToProfile);
                }
            });
        }
    }

    public static class ServerConfigs implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final BeanFactory beanFactory;

        public ServerConfigs(final DispatchAsync dispatcher, final BeanFactory beanFactory) {
            this.dispatcher = dispatcher;
            this.beanFactory = beanFactory;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            final ModelNode node = new ModelNode();
            node.get(ADDRESS).setEmptyList();
            node.get(OP).set(COMPOSITE);
            List<ModelNode> steps = new LinkedList<ModelNode>();

            int step = 1;
            final Map<String, HostInfo> stepToHost = new HashMap<String, HostInfo>();
            final List<HostInfo> hosts = control.getContext().get(HOSTS_KEY);
            final Map<String, String> groupToProfile = control.getContext().get(GROUP_TO_PROFILE_KEY);
            for (HostInfo hostInfo : hosts) {
                final ModelNode scOp = new ModelNode();
                scOp.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
                scOp.get(INCLUDE_RUNTIME).set(true);
                scOp.get(ADDRESS).add("host", hostInfo.getName());
                scOp.get(CHILD_TYPE).set("server-config");
                steps.add(scOp);
                stepToHost.put("step-" + step, hostInfo);
                step++;
            }

            node.get(STEPS).set(steps);
            dispatcher.execute(new DMRAction(node), new FunctionCallback(control) {
                @Override
                protected void onSuccess(final ModelNode result) {
                    ModelNode stepsResult = result.get(RESULT);
                    for (Map.Entry<String, HostInfo> entry : stepToHost.entrySet()) {
                        String step = entry.getKey();
                        HostInfo hostInfo = entry.getValue();
                        ModelNode currentStep = stepsResult.get(step);
                        if (currentStep.get(RESULT).isDefined()) {
                            List<ServerInstance> servers = new LinkedList<ServerInstance>();
                            List<Property> properties = currentStep.get(RESULT).asPropertyList();
                            for (Property property : properties) {
                                String name = property.getName();
                                ModelNode scModel = property.getValue();
                                ServerInstance serverInstance = beanFactory.serverInstance().as();
                                serverInstance.setName(name);
                                serverInstance.setServer(name);
                                String group = scModel.get("group").asString();
                                serverInstance.setGroup(group);
                                serverInstance.setProfile(groupToProfile.get(group));
                                serverInstance.setHost(hostInfo.getName());
                                serverInstance.setRunning(scModel.get("status").asString().equalsIgnoreCase("STARTED"));
                                servers.add(serverInstance);
                            }
                            hostInfo.setServerInstances(servers);
                        }
                    }
                }
            });
        }
    }

    public static class RunningServerInstances implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;

        public RunningServerInstances(final DispatchAsync dispatcher) {
            this.dispatcher = dispatcher;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            final ModelNode node = new ModelNode();
            node.get(ADDRESS).setEmptyList();
            node.get(OP).set(COMPOSITE);
            List<ModelNode> steps = new LinkedList<ModelNode>();

            int step = 1;
            final Map<String, ServerInstance> stepToServer = new HashMap<String, ServerInstance>();
            final List<HostInfo> hosts = control.getContext().get(HOSTS_KEY);
            for (HostInfo hostInfo : hosts) {
                for (ServerInstance serverInstance : hostInfo.getServerInstances()) {
                    if (serverInstance.isRunning()) {
                        ModelNode serverStateOp = new ModelNode();
                        serverStateOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
                        serverStateOp.get(NAME).set("server-state");
                        serverStateOp.get(ADDRESS).add("host", hostInfo.getName());
                        serverStateOp.get(ADDRESS).add("server", serverInstance.getName());
                        steps.add(serverStateOp);
                        stepToServer.put("step-" + step, serverInstance);
                        step++;

                        ModelNode socketBindingOp = new ModelNode();
                        socketBindingOp.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
                        socketBindingOp.get(INCLUDE_RUNTIME).set(true);
                        socketBindingOp.get(ADDRESS).add("host", hostInfo.getName());
                        socketBindingOp.get(ADDRESS).add("server", serverInstance.getName());
                        socketBindingOp.get(CHILD_TYPE).set("socket-binding-group");
                        steps.add(socketBindingOp);
                        stepToServer.put("step-" + step, serverInstance);
                        step++;
                    }
                }
            }

            node.get(STEPS).set(steps);
            dispatcher.execute(new DMRAction(node), new FunctionCallback(control) {
                @Override
                protected void onSuccess(final ModelNode result) {
                    ModelNode stepsResult = result.get(RESULT);
                    List<Property> properties = stepsResult.asPropertyList();
                    for (Iterator<Property> iterator = properties.iterator(); iterator.hasNext(); ) {
                        Property property = iterator.next();
                        String step = property.getName();
                        ModelNode node = property.getValue();
                        ServerInstance serverInstance = stepToServer.get(step);

                        // step-n: server state
                        if (node.get(RESULT).isDefined()) {
                            String state = node.get(RESULT).asString();
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
                            List<Property> sockets = node.get(RESULT).asPropertyList();
                            for (Property socket : sockets) {
                                serverInstance.getSocketBindings()
                                        .put(socket.getName(), socket.getValue().get("port-offset").asString());
                            }
                        }
                    }
                    control.getContext().push(hosts);
                }
            });
        }
    }
}
