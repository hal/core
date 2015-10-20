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
package org.jboss.as.console.client.v3.deployment;

import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher.Channel;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;

/**
 * Store for holding deployment relevant state like reference servers or selected (sub)deployments.
 *
 * @author Harald Pehl
 */
@Store
public class DeploymentStore extends ChangeSupport {

    private Deployment selectedDeployment;
    private Subdeployment selectedSubdeployment;

    public DeploymentStore() {
        this.selectedDeployment = null;
        this.selectedSubdeployment = null;
    }

    @Process(actionType = SelectDeploymentAction.class)
    public void selectDeployment(final SelectDeploymentAction action, final Channel channel) {
        selectedDeployment = action.getDeployment();
        selectedSubdeployment = action.getSubdeployment();
        channel.ack();
    }

    public ResourceAddress getSelectedDeploymentAddress() {
        ResourceAddress resourceAddress = null;
        if (selectedSubdeployment != null && selectedSubdeployment.getParent().getReferenceServer() != null) {
            resourceAddress = new ResourceAddress(selectedSubdeployment.getParent().getReferenceServer().getAddress())
                    .add("deployment", selectedSubdeployment.getParent().getName())
                    .add("subdeployment", selectedSubdeployment.getName());
        } else if (selectedDeployment != null && selectedDeployment.getReferenceServer() != null) {
            resourceAddress = new ResourceAddress(selectedDeployment.getReferenceServer().getAddress())
                    .add("deployment", selectedDeployment.getName());
        }
        return resourceAddress;
    }
}
