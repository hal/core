/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.console.client.shared.subsys.jca.model;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.domain.model.ServerInstance;
import org.jboss.as.console.client.domain.topology.HostInfo;
import org.jboss.as.console.client.domain.topology.TopologyFunctions;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.flow.FunctionCallback;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 */
public class DomainDriverStrategy implements DriverStrategy {

    private DispatchAsync dispatcher;
    private BeanFactory beanFactory;

    @Inject
    public DomainDriverStrategy(DispatchAsync dispatcher, BeanFactory beanFactory) {
        this.dispatcher = dispatcher;
        this.beanFactory = beanFactory;
    }

    @Override
    public void refreshDrivers(final AsyncCallback<List<JDBCDriver>> callback) {
        Outcome<FunctionContext> outcome = new Outcome<FunctionContext>() {
            @Override
            public void onFailure(final FunctionContext context) {
                callback.onFailure(context.getError());
            }

            @Override
            public void onSuccess(final FunctionContext context) {
                List<JDBCDriver> driver = context.pop();
                callback.onSuccess(driver);
            }
        };

        new Async<FunctionContext>().waterfall(new FunctionContext(), outcome,
                new TopologyFunctions.HostsAndGroups(dispatcher), // we need only hosts here, but still better to DRY
                new TopologyFunctions.ServerConfigs(dispatcher, beanFactory),
                new DriversOnRunningServers(dispatcher, beanFactory));
    }

    static class DriversOnRunningServers implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final BeanFactory beanFactory;
        private final List<JDBCDriver> drivers;

        DriversOnRunningServers(final DispatchAsync dispatcher, final BeanFactory beanFactory) {
            this.dispatcher = dispatcher;
            this.beanFactory = beanFactory;
            this.drivers = new LinkedList<JDBCDriver>();
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            final ModelNode node = new ModelNode();
            node.get(ADDRESS).setEmptyList();
            node.get(OP).set(COMPOSITE);
            List<ModelNode> steps = new LinkedList<ModelNode>();

            final List<HostInfo> hosts = control.getContext().get(TopologyFunctions.HOSTS_KEY);
            for (HostInfo hostInfo : hosts) {
                for (ServerInstance serverInstance : hostInfo.getServerInstances()) {
                    if (serverInstance.isRunning()) {
                        ModelNode dsOp = new ModelNode();
                        dsOp.get(OP).set("installed-drivers-list");
                        dsOp.get(ADDRESS).add("host", hostInfo.getName());
                        dsOp.get(ADDRESS).add("server", serverInstance.getName());
                        dsOp.get(ADDRESS).add("subsystem", "datasources");
                        steps.add(dsOp);
                    }
                }
            }

            node.get(STEPS).set(steps);
            dispatcher.execute(new DMRAction(node), new FunctionCallback(control) {
                @Override
                protected void onSuccess(final ModelNode result) {
                    /* For a composite op the "installed-drivers-list" reads as
                     * "server-groups" => {"<groupName>" => {"host" => {"<hostName>" => {"<serverName>" => {"response" => {
                     *   "outcome" => "success",
                     *   "result" => {"step-x" => {
                     *     "outcome" => "success",
                     *     "result" => [{
                     *       "driver-name" => "h2",
                     *       ...
                     *     }]
                     *   }}
                     * }}}}}}
                     */
                    ModelNode serverGroups = result.get("server-groups");
                    if (serverGroups.isDefined()) {
                        List<Property> groupProperties = serverGroups.asPropertyList();
                        for (Property groupProperty : groupProperties) {
                            String groupName = groupProperty.getName();
                            ModelNode groupNode = groupProperty.getValue();
                            ModelNode hostNode = groupNode.get("host");
                            if (hostNode.isDefined()) {
                                List<Property> hostProperties = hostNode.asPropertyList();
                                for (Property hostProperty : hostProperties) {
                                    hostNode = hostProperty.getValue();
                                    List<Property> serverProperties = hostNode.asPropertyList();
                                    for (Property serverProperty : serverProperties) {
                                        ModelNode serverNode = serverProperty.getValue();
                                        addDrivers(groupName, serverNode);
                                    }
                                }
                            }
                        }
                    }
                    control.getContext().push(drivers);
                }

                private void addDrivers(final String groupName, final ModelNode serverNode) {
                    ModelNode response = serverNode.get(RESPONSE);
                    if (response.isDefined()) {
                        ModelNode result = response.get(RESULT);
                        List<Property> steps = result.asPropertyList();
                        if (!steps.isEmpty()) {
                            ModelNode stepNode = steps.get(0).getValue();
                            result = stepNode.get(RESULT);
                            if (result.isDefined()) {
                                List<ModelNode> driverNodes = result.asList();
                                for (ModelNode driverNode : driverNodes) {
                                    String name = driverNode.get("driver-name").asString();
                                    if (!alreadyExists(name, groupName)) {
                                        JDBCDriver driver = driverFor(groupName, driverNode);
                                        drivers.add(driver);
                                    }
                                }
                            }
                        }
                    }
                }

                private boolean alreadyExists(String name, String group) {
                    for (JDBCDriver existing : drivers) {
                        if (existing.getName().equals(name) && existing.getGroup().equals(group)) {
                            return true;
                        }
                    }
                    return false;
                }

                private JDBCDriver driverFor(final String groupName, final ModelNode driverNode) {
                    JDBCDriver driver = beanFactory.jdbcDriver().as();
                    driver.setGroup(groupName);
                    driver.setDriverClass(driverNode.get("driver-class-name").asString());
                    driver.setName(driverNode.get("driver-name").asString());
                    driver.setDeploymentName(driverNode.get("deployment-name").asString());
                    driver.setMajorVersion(driverNode.get("driver-major-version").asInt());
                    driver.setMinorVersion(driverNode.get("driver-minor-version").asInt());

                    if (driverNode.hasDefined("driver-xa-datasource-class-name")) {
                        driver.setXaDataSourceClass(
                                driverNode.get("driver-xa-datasource-class-name").asString());
                    }
                    return driver;
                }
            });
        }
    }
}
