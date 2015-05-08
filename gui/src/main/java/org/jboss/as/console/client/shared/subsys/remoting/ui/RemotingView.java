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
package org.jboss.as.console.client.shared.subsys.remoting.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.remoting.RemotingPresenter;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.circuit.Dispatcher;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.List;

import static org.jboss.as.console.client.shared.subsys.remoting.store.RemotingStore.*;

/**
 * @author Harald Pehl
 */
public class RemotingView extends SuspendableViewImpl implements RemotingPresenter.MyView {

    private final DispatchAsync dispatcher;
    private final Dispatcher circuit;
    private final SecurityFramework securityFramework;
    private final StatementContext statementContext;
    private final ResourceDescriptionRegistry descriptionRegistry;

    private RemotingPresenter presenter;
    private EndpointConfigurationEditor endpointConfigurationEditor;
    private RemotingEditor remoteConnectorEditor;
    private RemotingEditor remoteHttpConnectorEditor;
    private ConnectionEditor localOutboundConnectionEditor;
    private ConnectionEditor outboundConnectionEditor;
    private ConnectionEditor remoteOutboundConnectionEditor;

    @Inject
    public RemotingView(DispatchAsync dispatcher, Dispatcher circuit, SecurityFramework securityFramework,
                        StatementContext statementContext, ResourceDescriptionRegistry descriptionRegistry) {
        this.dispatcher = dispatcher;
        this.circuit = circuit;
        this.securityFramework = securityFramework;
        this.statementContext = statementContext;
        this.descriptionRegistry = descriptionRegistry;
    }

    @Override
    public void setPresenter(RemotingPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public Widget createWidget() {
        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());

        endpointConfigurationEditor = new EndpointConfigurationEditor(circuit, securityContext,
                descriptionRegistry.lookup(ENDPOINT_CONFIGURATION_ADDRESS));

        remoteConnectorEditor = new ConnectorEditor(dispatcher, circuit, securityContext, statementContext,
                REMOTE_CONNECTOR_ADDRESS, descriptionRegistry.lookup(REMOTE_CONNECTOR_ADDRESS), "Connector");
        remoteHttpConnectorEditor = new ConnectorEditor(dispatcher, circuit, securityContext, statementContext,
                REMOTE_HTTP_CONNECTOR_ADDRESS, descriptionRegistry.lookup(REMOTE_HTTP_CONNECTOR_ADDRESS), "HTTP Connector");

        localOutboundConnectionEditor = new ConnectionEditor(dispatcher, circuit, securityContext, statementContext,
                LOCAL_OUTBOUND_CONNECTION_ADDRESS, descriptionRegistry.lookup(LOCAL_OUTBOUND_CONNECTION_ADDRESS),
                "Local Outbound Connection");
        outboundConnectionEditor = new ConnectionEditor(dispatcher, circuit, securityContext, statementContext,
                OUTBOUND_CONNECTION_ADDRESS, descriptionRegistry.lookup(OUTBOUND_CONNECTION_ADDRESS),
                "Outbound Connection");
        remoteOutboundConnectionEditor = new ConnectionEditor(dispatcher, circuit, securityContext, statementContext,
                REMOTE_OUTBOUND_CONNECTION_ADDRESS, descriptionRegistry.lookup(REMOTE_OUTBOUND_CONNECTION_ADDRESS),
                "Remote Outbound Connection");

        PagedView remoteConnectorPages = new PagedView(true);
        remoteConnectorPages.addPage("Connectors", remoteConnectorEditor.asWidget());
        remoteConnectorPages.addPage("HTTP Connectors", remoteHttpConnectorEditor.asWidget());
        remoteConnectorPages.showPage(0);

        PagedView outboundConnectionPages = new PagedView(true);
        outboundConnectionPages.addPage("Local Outbound", localOutboundConnectionEditor.asWidget());
        outboundConnectionPages.addPage("Outbound", outboundConnectionEditor.asWidget());
        outboundConnectionPages.addPage("Remote Outbound", remoteOutboundConnectionEditor.asWidget());
        outboundConnectionPages.showPage(0);

        DefaultTabLayoutPanel tabs = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabs.addStyleName("default-tabpanel");
        tabs.add(endpointConfigurationEditor, "Endpoint Configuration");
        tabs.add(remoteConnectorPages.asWidget(), "Remote Connectors");
        tabs.add(outboundConnectionPages.asWidget(), "Outbound Connections");
        tabs.selectTab(0);

        return tabs;
    }

    @Override
    public void select(AddressTemplate addressTemplate, String name) {
        switch (addressTemplate.getResourceType()) {
            case REMOTE_CONNECTOR:
                remoteConnectorEditor.select(name);
                break;
            case REMOTE_HTTP_CONNECTOR:
                remoteHttpConnectorEditor.select(name);
                break;
            case LOCAL_OUTBOUND_CONNECTION:
                localOutboundConnectionEditor.select(name);
                break;
            case OUTBOUND_CONNECTION:
                outboundConnectionEditor.select(name);
                break;
            case REMOTE_OUTBOUND_CONNECTION:
                remoteOutboundConnectionEditor.select(name);
                break;
        }
    }

    @Override
    public void update(AddressTemplate addressTemplate, ModelNode model) {
        if (CONFIGURATION.equals(addressTemplate.getResourceType())) {
            endpointConfigurationEditor.update(model);
        }
    }

    @Override
    public void update(AddressTemplate addressTemplate, List<Property> model) {
        switch (addressTemplate.getResourceType()) {
            case REMOTE_CONNECTOR:
                remoteConnectorEditor.updateMaster(model);
                break;
            case REMOTE_HTTP_CONNECTOR:
                remoteHttpConnectorEditor.updateMaster(model);
                break;
            case LOCAL_OUTBOUND_CONNECTION:
                localOutboundConnectionEditor.updateMaster(model);
                break;
            case OUTBOUND_CONNECTION:
                outboundConnectionEditor.updateMaster(model);
                break;
            case REMOTE_OUTBOUND_CONNECTION:
                remoteOutboundConnectionEditor.updateMaster(model);
                break;
        }
    }
}
