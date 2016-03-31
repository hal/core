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
package org.jboss.as.console.client.shared.subsys.remoting;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.core.CircuitPresenter;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.remoting.store.CreateConnection;
import org.jboss.as.console.client.shared.subsys.remoting.store.CreateConnector;
import org.jboss.as.console.client.shared.subsys.remoting.store.DeleteConnection;
import org.jboss.as.console.client.shared.subsys.remoting.store.DeleteConnector;
import org.jboss.as.console.client.shared.subsys.remoting.store.InitRemoting;
import org.jboss.as.console.client.shared.subsys.remoting.store.ModifyEndpointConfiguration;
import org.jboss.as.console.client.shared.subsys.remoting.store.ModifySaslSingleton;
import org.jboss.as.console.client.shared.subsys.remoting.store.ReadConnection;
import org.jboss.as.console.client.shared.subsys.remoting.store.ReadConnector;
import org.jboss.as.console.client.shared.subsys.remoting.store.RemotingStore;
import org.jboss.as.console.client.shared.subsys.remoting.store.UpdateConnection;
import org.jboss.as.console.client.shared.subsys.remoting.store.UpdateConnector;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.widgets.AddressableResourceView;
import org.jboss.as.console.client.v3.widgets.PropertyAddedEvent;
import org.jboss.as.console.client.v3.widgets.PropertyAddedEvent.PropertyAddedHandler;
import org.jboss.as.console.client.v3.widgets.PropertyRemovedEvent;
import org.jboss.as.console.client.v3.widgets.PropertyRemovedEvent.PropertyRemovedHandler;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.List;

import static org.jboss.as.console.client.shared.subsys.remoting.store.RemotingStore.*;

/**
 * TODO Human friendly error messages
 *
 * @author Harald Pehl
 */
public class RemotingPresenter extends CircuitPresenter<RemotingPresenter.MyView, RemotingPresenter.MyProxy>
        implements PropertyAddedHandler, PropertyRemovedHandler {

    public interface MyView extends View, AddressableResourceView, HasPresenter<RemotingPresenter> {
    }

    @ProxyCodeSplit
    @NameToken(NameTokens.Remoting)
    @RequiredResources(resources = {"{selected.profile}/subsystem=remoting/configuration=endpoint",
            "{selected.profile}/subsystem=remoting/connector=*",
            "{selected.profile}/subsystem=remoting/http-connector=*",
            "{selected.profile}/subsystem=remoting/local-outbound-connection=*",
            "{selected.profile}/subsystem=remoting/outbound-connection=*",
            "{selected.profile}/subsystem=remoting/remote-outbound-connection=*"})
    public interface MyProxy extends Proxy<RemotingPresenter>, Place {
    }


    private final Dispatcher circuit;
    private final RevealStrategy revealStrategy;
    private final RemotingStore remotingStore;

    @Inject
    public RemotingPresenter(EventBus eventBus, MyView view, MyProxy proxy, Dispatcher circuit,
                             RevealStrategy revealStrategy, RemotingStore remotingStore) {
        super(eventBus, view, proxy, circuit);

        this.circuit = circuit;
        this.revealStrategy = revealStrategy;
        this.remotingStore = remotingStore;
    }


    // ------------------------------------------------------ lifecycle

    @Override
    @SuppressWarnings("unchecked")
    protected void onBind() {
        super.onBind();
        addChangeHandler(remotingStore);

        // property management is not handled by the store, but using SubResourcePropertyManager
        // so register handlers to get notified about property modifications
        addVisibleHandler(PropertyAddedEvent.TYPE, this);
        addVisibleHandler(PropertyRemovedEvent.TYPE, this);

        getView().setPresenter(this);
    }

    @Override
    protected void onAction(Action action) {
        if (action instanceof InitRemoting) {
            getView().update(ENDPOINT_CONFIGURATION_ADDRESS, remotingStore.getEndpointConfiguration());
            getView().update(REMOTE_CONNECTOR_ADDRESS, remotingStore.getRemoteConnectors());
            getView().update(REMOTE_HTTP_CONNECTOR_ADDRESS, remotingStore.getRemoteHttpConnectors());
            getView().update(LOCAL_OUTBOUND_CONNECTION_ADDRESS, remotingStore.getLocalOutboundConnections());
            getView().update(OUTBOUND_CONNECTION_ADDRESS, remotingStore.getOutboundConnections());
            getView().update(REMOTE_OUTBOUND_CONNECTION_ADDRESS, remotingStore.getRemoteOutboundConnections());

        } else if (action instanceof ModifyEndpointConfiguration) {
            getView().update(ENDPOINT_CONFIGURATION_ADDRESS, remotingStore.getEndpointConfiguration());

        } else if (action instanceof CreateConnector) {
            AddressTemplate addressTemplate = ((CreateConnector) action).getAddressTemplate();
            getView().update(addressTemplate, remotingStore.getModelsFor(addressTemplate));
            getView().select(addressTemplate, remotingStore.getLastModifiedInstance());

        } else if (action instanceof ReadConnector) {
            AddressTemplate addressTemplate = ((ReadConnector) action).getAddressTemplate();
            getView().update(addressTemplate, remotingStore.getModelsFor(addressTemplate));

        } else if (action instanceof UpdateConnector) {
            AddressTemplate addressTemplate = ((UpdateConnector) action).getAddressTemplate();
            getView().update(addressTemplate, remotingStore.getModelsFor(addressTemplate));
            getView().select(addressTemplate, remotingStore.getLastModifiedInstance());

        } else if (action instanceof DeleteConnector) {
            AddressTemplate addressTemplate = ((DeleteConnector) action).getAddressTemplate();
            getView().update(addressTemplate, remotingStore.getModelsFor(addressTemplate));

        } else if (action instanceof CreateConnection) {
            AddressTemplate addressTemplate = ((CreateConnection) action).getAddressTemplate();
            getView().update(addressTemplate, remotingStore.getModelsFor(addressTemplate));
            getView().select(addressTemplate, remotingStore.getLastModifiedInstance());

        } else if (action instanceof ReadConnection) {
            AddressTemplate addressTemplate = ((ReadConnection) action).getAddressTemplate();
            getView().update(addressTemplate, remotingStore.getModelsFor(addressTemplate));

        } else if (action instanceof UpdateConnection) {
            AddressTemplate addressTemplate = ((UpdateConnection) action).getAddressTemplate();
            getView().update(addressTemplate, remotingStore.getModelsFor(addressTemplate));
            getView().select(addressTemplate, remotingStore.getLastModifiedInstance());

        } else if (action instanceof DeleteConnection) {
            AddressTemplate addressTemplate = ((DeleteConnection) action).getAddressTemplate();
            getView().update(addressTemplate, remotingStore.getModelsFor(addressTemplate));

        } else if (action instanceof ModifySaslSingleton) {
            String connectorName = ((ModifySaslSingleton) action).getConnectorName();
            AddressTemplate connectorAddress = ((ModifySaslSingleton) action).getConnectorAddress();
            List<Property> connectors = connectorAddress.getResourceType().equals(REMOTE_CONNECTOR) ?
                    remotingStore.getRemoteConnectors() : remotingStore.getRemoteHttpConnectors();
            getView().update(connectorAddress, connectors);
            getView().select(connectorAddress, connectorName);
        }
    }

    @Override
    public void onPropertyAdded(PropertyAddedEvent event) {
        refresh(event.getAddressTemplate());
    }

    @Override
    public void onPropertyRemoved(PropertyRemovedEvent event) {
        refresh(event.getAddressTemplate());
    }

    private void refresh(AddressTemplate addressTemplate) {
        // when a property was added / removed, trigger a READ action in order to refresh the view
        // the address template looks something like
        // "{selected.profile}/subsystem=remoting/<resource>={selected.entity}/.../property=*"
        // so don't use this template to dispatch the READ action!
        AddressTemplate resourceAddress = addressTemplate.subTemplate(0, 3);
        switch (resourceAddress.getResourceType()) {
            case REMOTE_CONNECTOR:
            case REMOTE_HTTP_CONNECTOR:
                circuit.dispatch(new ReadConnector(resourceAddress));
                break;
            case LOCAL_OUTBOUND_CONNECTION:
            case OUTBOUND_CONNECTION:
            case REMOTE_OUTBOUND_CONNECTION:
                circuit.dispatch(new ReadConnection(resourceAddress));
                break;
        }
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        circuit.dispatch(new InitRemoting());
    }
}
