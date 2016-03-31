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
import org.jboss.as.console.client.shared.subsys.ws.store.CreateConfig;
import org.jboss.as.console.client.shared.subsys.ws.store.CreateHandler;
import org.jboss.as.console.client.shared.subsys.ws.store.DeleteConfig;
import org.jboss.as.console.client.shared.subsys.ws.store.DeleteHandler;
import org.jboss.as.console.client.shared.subsys.ws.store.InitWebServices;
import org.jboss.as.console.client.shared.subsys.ws.store.ModifyProviderConfiguration;
import org.jboss.as.console.client.shared.subsys.ws.store.ReadConfig;
import org.jboss.as.console.client.shared.subsys.ws.store.ReadHandler;
import org.jboss.as.console.client.shared.subsys.ws.store.UpdateHandler;
import org.jboss.as.console.client.shared.subsys.ws.store.WebServicesStore;
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

    // the endpoint/client config name, it is set in SubResourcePropertyManager
    private AddressTemplate extractConfigAddress(AddressTemplate addressTemplate, String configName) {
        // we need to strip the /property=* from the address template and reload only the endpoint/client config selection
        // retrieve only the /subsystem=webservices address
        AddressTemplate rootResourceAddress = addressTemplate.subTemplate(0, 2);
        // retrieve only the endpoint/client config, to get its resource type
        AddressTemplate configAddress = addressTemplate.subTemplate(0, 3);

        // assemble the address template for the endpoint/client config
        configAddress = rootResourceAddress.append(configAddress.getResourceType() + "=" + configName);
        return configAddress;
    }

    @Override
    public void onPropertyRemoved(PropertyRemovedEvent event) {
        refresh(extractConfigAddress(event.getAddressTemplate(), configName));
    }

    @Override
    public void onPropertyAdded(PropertyAddedEvent event) {
        refresh(extractConfigAddress(event.getAddressTemplate(), configName));
    }

    private void refresh(AddressTemplate resourceAddress) {
        circuit.dispatch(new ReadConfig(resourceAddress));
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
