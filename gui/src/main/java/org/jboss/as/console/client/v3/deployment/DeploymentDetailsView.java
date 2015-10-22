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

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.tools.ModelBrowser;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.useware.kernel.gui.behaviour.StatementContext;

/**
 * @author Harald Pehl
 */
public class DeploymentDetailsView extends SuspendableViewImpl implements DeploymentDetailsPresenter.MyView {

    private final DispatchAsync dispatcher;
    private final StatementContext statementContext;
    private ModelBrowser modelBrowser;

    @Inject
    public DeploymentDetailsView(DispatchAsync dispatcher, StatementContext statementContext) {
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
    }

    @Override
    public Widget createWidget() {
        modelBrowser = new ModelBrowser(dispatcher, statementContext);
        return modelBrowser.asWidget();
    }

    @Override
    public void showDetails(final ResourceAddress resourceAddress) {
        modelBrowser.clearPinTo();
        modelBrowser.onReset(resourceAddress);
    }
}
