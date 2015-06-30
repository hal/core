/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.shared.subsys.jca.model;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
public class ModulesDriverStrategy implements DriverStrategy {

    private DispatchAsync dispatcher;
    private BeanFactory beanFactory;

    @Inject
    public ModulesDriverStrategy(DispatchAsync dispatcher, BeanFactory beanFactory) {
        this.dispatcher = dispatcher;
        this.beanFactory = beanFactory;
    }

    @Override
    public void refreshDrivers(final AsyncCallback<List<JDBCDriver>> callback) {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "datasources");
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(CHILD_TYPE).set("jdbc-driver");

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    callback.onFailure(new RuntimeException(result.getFailureDescription()));
                } else {
                    List<JDBCDriver> drivers = new ArrayList<>();
                    List<Property> driverNodes = result.get(RESULT).asPropertyList();
                    for (Property driverNode : driverNodes) {
                        drivers.add(driverFor(driverNode.getValue()));
                    }
                    callback.onSuccess(drivers);
                }
            }

            private JDBCDriver driverFor(final ModelNode driverNode) {
                JDBCDriver driver = beanFactory.jdbcDriver().as();
                driver.setName(driverNode.get("driver-name").asString());
                if (driverNode.hasDefined("driver-class-name")) {
                    driver.setDriverClass(driverNode.get("driver-class-name").asString());
                }
                if (driverNode.hasDefined("driver-xa-datasource-class-name")) {
                    driver.setXaDataSourceClass(driverNode.get("driver-xa-datasource-class-name").asString());
                }
                if (driverNode.hasDefined("driver-module-name")) {
                    driver.setDriverModuleName(driverNode.get("driver-module-name").asString());
                }
                if (driverNode.hasDefined("deployment-name")) {
                    driver.setDeploymentName(driverNode.get("deployment-name").asString());
                }
                if (driverNode.hasDefined("driver-major-version")) {
                    driver.setMajorVersion(driverNode.get("driver-major-version").asInt());
                }
                if (driverNode.hasDefined("driver-minor-version")) {
                    driver.setMinorVersion(driverNode.get("driver-minor-version").asInt());
                }
                return driver;
            }
        });
    }
}