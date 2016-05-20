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
package org.jboss.as.console.client.shared.subsys.ws;

import org.jboss.as.console.client.core.CircuitPresenter;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.ws.store.*;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.widgets.AddressableResourceView;
import org.jboss.as.console.client.v3.widgets.PropertyAddedEvent;
import org.jboss.as.console.client.v3.widgets.PropertyAddedEvent.PropertyAddedHandler;
import org.jboss.as.console.client.v3.widgets.PropertyRemovedEvent;
import org.jboss.as.console.client.v3.widgets.PropertyRemovedEvent.PropertyRemovedHandler;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;

/**
 * @author Heiko Braun
 * @date 6/10/11
 */
public class WebServicePresenter extends CircuitPresenter<WebServicePresenter.MyView, WebServicePresenter.MyProxy> implements PropertyAddedHandler, PropertyRemovedHandler {

    @ProxyCodeSplit
    @NameToken(NameTokens.WebServicePresenter)
    @RequiredResources(
            resources = {
                    "{selected.profile}/subsystem=webservices",
                    "{selected.profile}/subsystem=webservices/endpoint-config=*",
                    "{selected.profile}/subsystem=webservices/endpoint-config=*/pre-handler-chain=*",
                    "{selected.profile}/subsystem=webservices/endpoint-config=*/post-handler-chain=*",
                    "{selected.profile}/subsystem=webservices/client-config=*",
                    "{selected.profile}/subsystem=webservices/client-config=*/pre-handler-chain=*",
                    "{selected.profile}/subsystem=webservices/client-config=*/post-handler-chain=*"
            })
    public interface MyProxy extends Proxy<WebServicePresenter>, Place {
    }

    public interface MyView extends View, AddressableResourceView, HasPresenter<WebServicePresenter> {
        void updateConfig(AddressTemplate addressTemplate, ModelNode currentConfig);
        void navigateHandlerView();
    }

    private final Dispatcher circuit;
    private final RevealStrategy revealStrategy;
    private final WebServicesStore webservicesStore;
    private final PlaceManager placeManager;

    // the endpoint/client configuration selected in ConfigEditorWS
    private String configName;

    @Inject
    public WebServicePresenter(
            EventBus eventBus, MyView view, MyProxy proxy, Dispatcher circuit,
            RevealStrategy revealStrategy, WebServicesStore webservicesStore,
            PlaceManager placeManager) {

        super(eventBus, view, proxy, circuit);
        this.circuit = circuit;
        this.revealStrategy = revealStrategy;
        this.webservicesStore = webservicesStore;
        this.placeManager = placeManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onBind() {
        super.onBind();

        addChangeHandler(webservicesStore);

        // property management is not handled by the store, but using SubResourcePropertyManager
        // so register handlers to get notified about property modifications
        addVisibleHandler(PropertyAddedEvent.TYPE, this);
        addVisibleHandler(PropertyRemovedEvent.TYPE, this);

        getView().setPresenter(this);
    }

    @Override
    protected void onAction(Action action) {
        if (action instanceof InitWebServices) {
            getView().update(WebServicesStore.WS_SUBSYSTEM, webservicesStore.getProviderConfiguration());
            getView().update(WebServicesStore.ENDPOINT_CONFIG_ADDRESS, webservicesStore.getEndpointConfig());
            getView().update(WebServicesStore.CLIENT_CONFIG_ADDRESS, webservicesStore.getClientConfig());

        } else if (action instanceof ModifyProviderConfiguration) {
            getView().update(WebServicesStore.WS_SUBSYSTEM, webservicesStore.getProviderConfiguration());

        // endpoint/client config
        } else if (action instanceof CreateConfig) {
            AddressTemplate addressTemplate = ((CreateConfig) action).getAddressTemplate();
            getView().update(addressTemplate, webservicesStore.getModelsFor(addressTemplate));
            getView().select(addressTemplate, webservicesStore.getLastModifiedInstance());

        } else if (action instanceof DeleteConfig) {
            AddressTemplate addressTemplate = ((DeleteConfig) action).getAddressTemplate();
            getView().update(addressTemplate, webservicesStore.getModelsFor(addressTemplate));
            
        } else if (action instanceof ReadAllEndpointConfig) {
            AddressTemplate addressTemplate = ((ReadAllEndpointConfig) action).getAddressTemplate();
            getView().update(addressTemplate, webservicesStore.getModelsFor(addressTemplate));

        } else if (action instanceof ReadAllClientConfig) {
            AddressTemplate addressTemplate = ((ReadAllClientConfig) action).getAddressTemplate();
            getView().update(addressTemplate, webservicesStore.getModelsFor(addressTemplate));

        // pre-handler
        } else if (action instanceof CreateHandler) {
            AddressTemplate addressTemplate = ((CreateHandler) action).getAddressTemplate();
            ModelNode currentConfig = webservicesStore.getCurrentConfig();
            getView().updateConfig(addressTemplate, currentConfig);

        } else if (action instanceof ReadHandler) {
            AddressTemplate addressTemplate = ((ReadHandler) action).getAddressTemplate();
            ModelNode currentConfig = webservicesStore.getCurrentConfig();
            getView().updateConfig(addressTemplate, currentConfig);
            getView().navigateHandlerView();

        } else if (action instanceof UpdateHandler) {
            AddressTemplate addressTemplate = ((UpdateHandler) action).getAddressTemplate();
            ModelNode currentConfig = webservicesStore.getCurrentConfig();
            getView().updateConfig(addressTemplate, currentConfig);

        } else if (action instanceof DeleteHandler) {
            AddressTemplate addressTemplate = ((DeleteHandler) action).getAddressTemplate();
            ModelNode currentConfig = webservicesStore.getCurrentConfig();
            getView().updateConfig(addressTemplate, currentConfig);

        } else if (action instanceof ReadConfig) {
            AddressTemplate addressTemplate = ((ReadConfig) action).getAddressTemplate();
            ModelNode currentConfig = webservicesStore.getCurrentConfig();
            getView().updateConfig(addressTemplate, currentConfig);

        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        circuit.dispatch(new InitWebServices());
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    // properties management


    @Override
    public void onPropertyRemoved(PropertyRemovedEvent event) {
        refresh(event.getAddressTemplate());
    }

    @Override
    public void onPropertyAdded(PropertyAddedEvent event) {
        refresh(event.getAddressTemplate());
    }

    /**
     * For each property modification, all endpoint/client configurations must be reloaded, 
     * because each view editor manages a list of endpoint/client configurations, so
     * the list must be reloaded.
     * 
     * @param addressTemplate The resource template containing the property=* suffix.
     */
    private void refresh(AddressTemplate addressTemplate) {
        addressTemplate = addressTemplate.subTemplate(0, 3);
        switch (addressTemplate.getResourceType()) {
            case WebServicesStore.ENDPOINT_CONFIG:
                circuit.dispatch(new ReadAllEndpointConfig(addressTemplate));
                break;
            case WebServicesStore.CLIENT_CONFIG:
                circuit.dispatch(new ReadAllClientConfig(addressTemplate));
                break;
        }
        
    }

    public PlaceManager getPlaceManager() {
        return placeManager;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    void setHandler(Property selection, AddressTemplate addressTemplate) {
        AddressTemplate configAddress = addressTemplate.replaceWildcards(selection.getName());
        circuit.dispatch(new ReadHandler(configAddress));
    }

}
