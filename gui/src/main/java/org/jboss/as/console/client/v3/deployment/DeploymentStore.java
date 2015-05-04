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

import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.shared.deployment.model.DeploymentRecord;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;

import javax.inject.Inject;

/**
 * Circuit store used to track the selected deployment. Relies on the
 * existing {@link org.jboss.as.console.client.shared.deployment.DeploymentStore}.
 *
 * @author Harald Pehl
 */
@Store
public class DeploymentStore extends ChangeSupport {

    private final DispatchAsync dispatcher;
    private final BootstrapContext bootstrapContext;
    private DeploymentRecord selectedDeployment;

    @Inject
    public DeploymentStore(final DispatchAsync dispatcher, final BootstrapContext bootstrapContext) {
        this.dispatcher = dispatcher;
        this.bootstrapContext = bootstrapContext;
    }

    @Process(actionType = DeploymentSelection.class)
    public void onSelectDeployment(DeploymentSelection action, final Dispatcher.Channel channel) {
        selectedDeployment = action.getDeployment();
        channel.ack();
    }

    public DeploymentRecord getSelectedDeployment() {
        return selectedDeployment;
    }

    private boolean isStandalone() {
        return bootstrapContext.isStandalone();
    }

    private boolean isDomain() {
        return !bootstrapContext.isStandalone();
    }
}
