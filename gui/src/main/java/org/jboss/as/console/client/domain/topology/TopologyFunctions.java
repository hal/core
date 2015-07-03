package org.jboss.as.console.client.domain.topology;

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
public final class TopologyFunctions {

    public static final String HOSTS_KEY = "hosts";
    public static final String GROUPS_KEY = "groups";

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

                    Map<String, ServerGroup> groups = new HashMap<String, ServerGroup>();
                    currentStep = stepsResult.get("step-2");
                    if (currentStep.get(RESULT).isDefined()) {
                        List<Property> properties = currentStep.get(RESULT).asPropertyList();
                        for (Property property : properties) {
                            String name = property.getName();
                            ModelNode groupModel = property.getValue();
                            String profile = groupModel.get("profile").asString();
                            String socketBindingGroup = groupModel.get("socket-binding-group").asString();
                            ServerGroup serverGroup = new ServerGroup(name, profile, socketBindingGroup);
                            groups.put(name, serverGroup);
                        }
                    }
                    control.getContext().set(GROUPS_KEY, groups);
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
            final Map<String, ServerGroup> groups = control.getContext().get(GROUPS_KEY);
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
                                String groupName = scModel.get("group").asString();
                                ServerGroup group = groups.get(groupName);

                                ServerInstance serverInstance = beanFactory.serverInstance().as();
                                serverInstance.setName(name);
                                serverInstance.setServer(name);
                                serverInstance.setGroup(groupName);
                                serverInstance.setProfile(group.getProfile());
                                serverInstance.setHost(hostInfo.getName());
                                serverInstance.setRunning(scModel.get("status").asString().equalsIgnoreCase("STARTED"));

                                // FIXME We just need the socket binding group name here not the actual socket bindings
                                // Using a map is not necessary
                                Map<String, String> socketBindings = new HashMap<>();
                                socketBindings.put(group.getSocketBindingGroup(),
                                        scModel.get("socket-binding-port-offset").asString());
                                serverInstance.setSocketBindings(socketBindings);
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
            final HashSet<String> processedGroups = new HashSet<>();
            final Map<String, ServerGroup> groups = control.getContext().get(GROUPS_KEY);
            final Map<String, ServerInstance> stepToServer = new HashMap<String, ServerInstance>();
            final List<HostInfo> hosts = control.getContext().get(HOSTS_KEY);
            for (HostInfo hostInfo : hosts) {
                for (ServerInstance serverInstance : hostInfo.getServerInstances()) {
                    // find a server which we haven't processed so far - no matter which host
                    if (serverInstance.isRunning() && !processedGroups.contains(serverInstance.getGroup())) {
                        ModelNode serverStateOp = new ModelNode();
                        serverStateOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
                        serverStateOp.get(NAME).set("server-state");
                        serverStateOp.get(ADDRESS).add("host", hostInfo.getName());
                        serverStateOp.get(ADDRESS).add("server", serverInstance.getName());
                        steps.add(serverStateOp);
                        stepToServer.put("step-" + step, serverInstance);
                        step++;

                        processedGroups.add(serverInstance.getGroup());
                    }
                }
            }

            node.get(STEPS).set(steps);
            dispatcher.execute(new DMRAction(node), new FunctionCallback(control) {
                @Override
                protected void onSuccess(final ModelNode result) {
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
                                    if (state.equals("reload-required")) {
                                        serverInstance.setFlag(ServerFlag.RELOAD_REQUIRED);
                                    } else if (state.equals("restart-required")) {
                                        serverInstance.setFlag(ServerFlag.RESTART_REQUIRED);
                                    }
                                }

                            }

                            // update likeminded (same group)
                            for (HostInfo hostInfo : hosts) {
                                for (ServerInstance s : hostInfo.getServerInstances()) {
                                    if (s.getGroup().equals(serverInstance.getGroup())) {
                                        s.setFlag(serverInstance.getFlag());
                                    }
                                }
                            }
                        }
                    } // else no running servers!
                    control.getContext().push(hosts);
                }
            });
        }
    }
}
