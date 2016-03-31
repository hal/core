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

import java.util.List;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.ws.store.WebServicesStore;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.circuit.Dispatcher;
import org.useware.kernel.gui.behaviour.StatementContext;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * @author Heiko Braun
 * @date 6/10/11
 */
public class WebServiceView extends SuspendableViewImpl implements WebServicePresenter.MyView {

    private WebServicePresenter presenter;
    private ProviderEditor providerEditor;
    private ConfigEditorWS endpointConfigEditor;
    private ConfigEditorWS clientConfigEditor;
    private HandlerEditor preHandlerEndpointEditor;
    private HandlerEditor postHandlerEndpointEditor;
    private HandlerEditor preHandlerClientEditor;
    private HandlerEditor postHandlerClientEditor;

    private final DispatchAsync dispatcher;
    private final Dispatcher circuit;
    private final SecurityFramework securityFramework;
    private final StatementContext statementContext;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private PagedView endpointHandlerPages;
    private PagedView clientHandlerPages;
    private DefaultTabLayoutPanel tabLayoutpanel;

    @Inject
    public WebServiceView(DispatchAsync dispatcher, Dispatcher circuit, SecurityFramework securityFramework,
            StatementContext statementContext, ResourceDescriptionRegistry descriptionRegistry) {

        this.dispatcher = dispatcher;
        this.circuit = circuit;
        this.securityFramework = securityFramework;
        this.statementContext = statementContext;
        this.descriptionRegistry = descriptionRegistry;
    }

    @Override
    public Widget createWidget() {

        createEndpointConfiguration();
        createClientConfiguration();

        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());

        providerEditor = new ProviderEditor(circuit, securityContext, descriptionRegistry.lookup(WebServicesStore.WS_SUBSYSTEM));

        tabLayoutpanel = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabLayoutpanel.addStyleName("default-tabpanel");
        tabLayoutpanel.add(providerEditor.asWidget(), Console.CONSTANTS.subsys_ws_provider(), true);
        tabLayoutpanel.add(endpointHandlerPages.asWidget(), "Endpoint Configuration", true);
        tabLayoutpanel.add(clientHandlerPages.asWidget(), "Client Configuration", true);
        tabLayoutpanel.selectTab(0);
        tabLayoutpanel.addSelectionHandler(new SelectionHandler<Integer>() {

            @Override
            public void onSelection(SelectionEvent<Integer> event) {
                if (1 == event.getSelectedItem()) {
                    endpointConfigEditor.notifyDefaultSelection();
                } else if (2 == event.getSelectedItem()) {
                    clientConfigEditor.notifyDefaultSelection();
                }
            }
        });


        return tabLayoutpanel;
    }

    static java.util.logging.Logger LOG = java.util.logging.Logger.getLogger("org.jboss");

    private void createEndpointConfiguration() {
        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());

        ResourceDescription endpointResourceDescription = descriptionRegistry.lookup(WebServicesStore.ENDPOINT_CONFIG_ADDRESS);
        endpointConfigEditor = new ConfigEditorWS(dispatcher, circuit, securityContext, statementContext,
                WebServicesStore.ENDPOINT_CONFIG_ADDRESS, endpointResourceDescription, "Endpoint Configuration", presenter);

        // ------ pre/post handler for endpoint configuration
        AddressTemplate preHandlerEndpointAddress = WebServicesStore.ENDPOINT_CONFIG_ADDRESS.append("pre-handler-chain=*");
        ResourceDescription preHandlerEndpointResourceDescription = endpointResourceDescription.getChildDescription("pre-handler-chain");
        preHandlerEndpointEditor = new HandlerEditor(dispatcher, circuit, securityContext, statementContext,
                preHandlerEndpointAddress, preHandlerEndpointResourceDescription, presenter);

        AddressTemplate postHandlerEndpointAddress = WebServicesStore.ENDPOINT_CONFIG_ADDRESS.append("post-handler-chain=*");
        ResourceDescription postHandlerEndpointResourceDescription = endpointResourceDescription.getChildDescription("post-handler-chain");
        postHandlerEndpointEditor = new HandlerEditor(dispatcher, circuit, securityContext, statementContext,
                postHandlerEndpointAddress, postHandlerEndpointResourceDescription, presenter);

        // PagedView for endpoint config. It is the left column page
        endpointHandlerPages = new PagedView();
        endpointHandlerPages.addPage("Endpoint Configuration", endpointConfigEditor.asWidget());
        endpointHandlerPages.addPage("Pre Handler Chain", preHandlerEndpointEditor.asWidget());
        endpointHandlerPages.addPage("Post Handler Chain", postHandlerEndpointEditor.asWidget());
        endpointHandlerPages.showPage(0);

    }

    private void createClientConfiguration() {
        SecurityContext securityContext = securityFramework.getSecurityContext(presenter.getProxy().getNameToken());

        ResourceDescription clientResourceDescription = descriptionRegistry.lookup(WebServicesStore.CLIENT_CONFIG_ADDRESS);
        clientConfigEditor = new ConfigEditorWS(dispatcher, circuit, securityContext, statementContext,
                WebServicesStore.CLIENT_CONFIG_ADDRESS, clientResourceDescription, "Client Configuration", presenter);

        // ------ pre/post handler for client configuration
        AddressTemplate preHandlerClientAddress = WebServicesStore.CLIENT_CONFIG_ADDRESS.append("pre-handler-chain=*");
        ResourceDescription preHandlerClientResourceDescription = clientResourceDescription.getChildDescription("pre-handler-chain");
        preHandlerClientEditor = new HandlerEditor(dispatcher, circuit, securityContext, statementContext,
                preHandlerClientAddress, preHandlerClientResourceDescription, presenter);

        AddressTemplate postHandlerClientAddress = WebServicesStore.CLIENT_CONFIG_ADDRESS.append("post-handler-chain=*");
        ResourceDescription postHandlerClientResourceDescription = clientResourceDescription.getChildDescription("post-handler-chain");
        postHandlerClientEditor = new HandlerEditor(dispatcher, circuit, securityContext, statementContext,
                postHandlerClientAddress, postHandlerClientResourceDescription, presenter);

        // PagedView for client config. It is the left column page
        clientHandlerPages = new PagedView();
        clientHandlerPages.addPage("Client Configuration", clientConfigEditor.asWidget());
        clientHandlerPages.addPage("Pre Handler Chain", preHandlerClientEditor.asWidget());
        clientHandlerPages.addPage("Post Handler Chain", postHandlerClientEditor.asWidget());
        clientHandlerPages.showPage(0);
    }

    @Override
    public void select(AddressTemplate addressTemplate, String name) {
        switch (addressTemplate.getResourceType()) {
            case WebServicesStore.ENDPOINT_CONFIG:
                endpointConfigEditor.select(name);
                break;
            case WebServicesStore.CLIENT_CONFIG:
                clientConfigEditor.select(name);
                break;
        }
    }

    @Override
    public void update(AddressTemplate addressTemplate, ModelNode model) {
        if ("subsystem".equals(addressTemplate.getResourceType())) {
            providerEditor.update(model);
        }
    }

    /**
     * This operation updates the list of endpoint/client configurations, when a a endpoint/client is added/removed.
     *
     * @see org.jboss.as.console.client.v3.widgets.AddressableResourceView#update(org.jboss.as.console.client.v3.dmr.AddressTemplate, java.util.List)
     */
    @Override
    public void update(AddressTemplate addressTemplate, List<Property> model) {
        switch (addressTemplate.getResourceType()) {
            case WebServicesStore.ENDPOINT_CONFIG:
                endpointConfigEditor.updateMaster(model);
                break;
            case WebServicesStore.CLIENT_CONFIG:
                clientConfigEditor.updateMaster(model);
                break;
        }
    }

    @Override
    public void setPresenter(WebServicePresenter presenter) {
        this.presenter = presenter;
    }

    /**
     * to show a specific panel accordingly to the selected tab
     */
    public void navigateHandlerView() {
        // if endpoint tab
        if (tabLayoutpanel.getSelectedIndex() == 1) {
            if (endpointHandlerPages.getPage() == 0)
                endpointHandlerPages.showPage(1);
        // else the client tab
        } else if (tabLayoutpanel.getSelectedIndex() == 2) {
            if (clientHandlerPages.getPage() == 0)
                clientHandlerPages.showPage(1);
        }
    }

    /**
     *  Updates the child resources for a specific endpoint/client selected, when some opearation is
     *  performed in the properties, pre/post handlers.
     *
     */
    public void updateConfig(AddressTemplate addressTemplate, ModelNode currentConfig) {

        String configName = null;
        // update the editor endpoint/client config properties
        switch (addressTemplate.getResourceType()) {
            case WebServicesStore.ENDPOINT_CONFIG:
                endpointConfigEditor.updateDetail(currentConfig);
                configName = addressTemplate.getTemplate().substring(addressTemplate.getTemplate().lastIndexOf("=") + 1);
                break;
            case WebServicesStore.CLIENT_CONFIG:
                clientConfigEditor.updateDetail(currentConfig);
                configName = addressTemplate.getTemplate().substring(addressTemplate.getTemplate().lastIndexOf("=") + 1);
                break;
            default:
                // when some handlers actions kicks in, need to strip the pre/post-handlers at the end
                addressTemplate = addressTemplate.subTemplate(2, 3);
                configName = addressTemplate.getTemplate().substring(addressTemplate.getTemplate().lastIndexOf("=") + 1);
                break;
        }

        // update the editor endpoint/client pre/post handlers
        switch (addressTemplate.getResourceType()) {
            case WebServicesStore.ENDPOINT_CONFIG:
                preHandlerEndpointEditor.updateMaster(configName, currentConfig, "pre-handler-chain");
                postHandlerEndpointEditor.updateMaster(configName, currentConfig, "post-handler-chain");
                break;
            case WebServicesStore.CLIENT_CONFIG:
                preHandlerClientEditor.updateMaster(configName, currentConfig, "pre-handler-chain");
                postHandlerClientEditor.updateMaster(configName, currentConfig, "post-handler-chain");
                break;
        }


    }

}
