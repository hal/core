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
package org.jboss.as.console.client.shared.subsys.picketlink;

import com.allen_sauer.gwt.log.client.Log;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate.Callback;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.client.v3.widgets.PropertyAddedEvent;
import org.jboss.as.console.client.v3.widgets.PropertyAddedEvent.PropertyAddedHandler;
import org.jboss.as.console.client.v3.widgets.PropertyRemovedEvent;
import org.jboss.as.console.client.v3.widgets.PropertyRemovedEvent.PropertyRemovedHandler;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
public class ServiceProviderPresenter
        extends Presenter<ServiceProviderPresenter.MyView, ServiceProviderPresenter.MyProxy>
        implements PicketLinkDirectory, PropertyAddedHandler, PropertyRemovedHandler {

    // @formatter:off
    @ProxyCodeSplit
    @NameToken(NameTokens.PicketLinkServiceProvider)
    @RequiredResources(resources = {SERVICE_PROVIDER_ADDRESS, SERVICE_PROVIDER_HANDLER_ADDRESS}, recursive = true)
    public interface MyProxy extends Proxy<ServiceProviderPresenter>, Place {
    }

    public interface MyView extends View, HasPresenter<ServiceProviderPresenter> {
        void update(ModelNode serviceProvider);
    }
    // @formatter:on


    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final SecurityContext securityContext;
    private final StatementContext statementContext;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private final CrudOperationDelegate crud;
    private String federation;
    private String serviceProvider;


    // ------------------------------------------------------ presenter lifecycle

    @Inject
    public ServiceProviderPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final RevealStrategy revealStrategy, final DispatchAsync dispatcher,
            final SecurityFramework securityFramework, final StatementContext statementContext,
            final ResourceDescriptionRegistry descriptionRegistry) {
        super(eventBus, view, proxy);

        this.revealStrategy = revealStrategy;
        this.dispatcher = dispatcher;
        this.securityContext = securityFramework.getSecurityContext(NameTokens.PicketLinkFederation);
        this.statementContext = statementContext;
        this.descriptionRegistry = descriptionRegistry;
        this.crud = new CrudOperationDelegate(statementContext, dispatcher);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onBind() {
        super.onBind();
        addRegisteredHandler(PropertyAddedEvent.TYPE, this);
        addRegisteredHandler(PropertyRemovedEvent.TYPE, this);
        getView().setPresenter(this);
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    @Override
    public void prepareFromRequest(final PlaceRequest request) {
        federation = request.getParameter(FEDERATION_REQUEST_PARAM, null);
        serviceProvider = request.getParameter(SERVICE_PROVIDER_REQUEST_PARAM, null);
    }

    @Override
    protected void onReset() {
        super.onReset();
        readServiceProvider();
    }


    // ------------------------------------------------------ federation

    private void readServiceProvider() {
        if (federation != null && serviceProvider != null) {
            Operation operation = new Operation.Builder(READ_RESOURCE_OPERATION,
                    SERVICE_PROVIDER_TEMPLATE.resolve(statementContext, federation, serviceProvider))
                    .param(RECURSIVE, true).build();
            dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
                @Override
                public void onSuccess(final DMRResponse dmrResponse) {
                    ModelNode response = dmrResponse.get();

                    if (response.isFailure()) {
                        Log.error("Failed to read service provider " + serviceProvider +
                                " from federation " + federation, response.getFailureDescription());
                    } else {
                        ModelNode serviceProvider = response.get(RESULT);
                        getView().update(serviceProvider);
                    }
                }
            });
        }
    }


    // ------------------------------------------------------ service provider & handlers

    public void modifyServiceProvider(final Map<String, Object> changedValues) {
        AddressTemplate template = SERVICE_PROVIDER_TEMPLATE.replaceWildcards(federation, serviceProvider);
        modify(template, serviceProvider, changedValues);
    }

    public void launchNewHandlerDialog() {
        ResourceDescription description = descriptionRegistry.lookup(SERVICE_PROVIDER_HANDLER_TEMPLATE);
        AddressTemplate template = SERVICE_PROVIDER_HANDLER_TEMPLATE.replaceWildcards(federation, serviceProvider);
        launchAddDialog("SAML Handler", template, description);
    }

    public void removeHandler(final String name) {
        AddressTemplate template = SERVICE_PROVIDER_HANDLER_TEMPLATE.replaceWildcards(federation, serviceProvider);
        remove(template, name);
    }


    // ------------------------------------------------------ handler parameter

    @Override
    public void onPropertyAdded(final PropertyAddedEvent event) {
        readServiceProvider();
    }

    @Override
    public void onPropertyRemoved(final PropertyRemovedEvent event) {
        readServiceProvider();
    }


    // ------------------------------------------------------ generic crud

    private void launchAddDialog(final String resourceName, final AddressTemplate template,
            final ResourceDescription resourceDescription, String... attributes) {
        DefaultWindow dialog = new DefaultWindow("New " + resourceName);
        //noinspection Duplicates
        AddResourceDialog addDialog = new AddResourceDialog(securityContext, resourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        dialog.hide();

                        String name = payload.get("name").asString();
                        ResourceAddress address = template.resolve(statementContext, name);
                        payload.get(OP).set(ADD);
                        payload.get(ADDRESS).set(address);
                        dispatcher.execute(new DMRAction(payload), new SimpleCallback<DMRResponse>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                super.onFailure(caught);
                                readServiceProvider();
                            }

                            @Override
                            public void onSuccess(DMRResponse dmrResponse) {
                                Console.info("Successfully added " + name);
                                readServiceProvider();
                            }
                        });
                    }

                    @Override
                    public void onCancel() {
                        dialog.hide();
                    }
                }).include(attributes);

        dialog.setWidth(640);
        dialog.setHeight(480);
        dialog.setWidget(addDialog);
        dialog.setGlassEnabled(true);
        dialog.center();
    }

    private void modify(final AddressTemplate template, final String name, final Map<String, Object> changedValues) {
        crud.onSaveResource(template, name, changedValues,
                new Callback() {
                    @Override
                    public void onSuccess(final AddressTemplate addressTemplate, final String name) {
                        Console.info("Successfully modified " + name);
                        readServiceProvider();
                    }

                    @Override
                    public void onFailure(final AddressTemplate addressTemplate, final String name, final Throwable t) {
                        Console.error("Unable to modify " + name, t.getMessage());
                        readServiceProvider();
                    }
                });
    }

    private void remove(final AddressTemplate template, final String name) {
        crud.onRemoveResource(template, name, new Callback() {
            @Override
            public void onSuccess(final AddressTemplate addressTemplate, final String name) {
                Console.info("Successfully removed " + name);
                readServiceProvider();
            }

            @Override
            public void onFailure(final AddressTemplate addressTemplate, final String name, final Throwable t) {
                readServiceProvider();
                Console.error("Unable to remove " + name, t.getMessage());
            }
        });
    }


    // ------------------------------------------------------ properties

    public String getFederation() {
        return federation;
    }

    public String getServiceProvider() {
        return serviceProvider;
    }
}
