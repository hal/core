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
import org.jboss.as.console.client.shared.flow.FunctionContext;
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
import org.jboss.gwt.flow.client.Outcome;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
public class FederationPresenter
        extends Presenter<FederationPresenter.MyView, FederationPresenter.MyProxy>
        implements PicketLinkDirectory, PropertyAddedHandler, PropertyRemovedHandler {

    // @formatter:off
    @ProxyCodeSplit
    @NameToken(NameTokens.PicketLinkFederation)
    @RequiredResources(resources = {
            IDENTITY_PROVIDER_ADDRESS,
            IDENTITY_PROVIDER_HANDLER_ADDRESS,
            IDENTITY_PROVIDER_TRUST_DOMAIN_ADDRESS,
            KEY_STORE_ADDRESS,
            SAML_ADDRESS}, recursive = true)
    public interface MyProxy extends Proxy<FederationPresenter>, Place {
    }

    public interface MyView extends View, HasPresenter<FederationPresenter> {
        void update(ModelNode federation);
    }
    // @formatter:on


    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final SecurityContext securityContext;
    private final StatementContext statementContext;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private final CrudOperationDelegate crud;
    private String federation;
    private String identityProvider;


    // ------------------------------------------------------ presenter lifecycle

    @Inject
    public FederationPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
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
    }

    @Override
    protected void onReset() {
        super.onReset();
        readFederation();
    }


    // ------------------------------------------------------ federation

    private void readFederation() {
        if (federation != null) {
            Operation operation = new Operation.Builder(READ_RESOURCE_OPERATION,
                    FEDERATION_TEMPLATE.resolve(statementContext, federation)).param(RECURSIVE, true).build();
            dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
                @Override
                public void onSuccess(final DMRResponse dmrResponse) {
                    ModelNode response = dmrResponse.get();

                    if (response.isFailure()) {
                        Log.error("Failed to read federation " + federation, response.getFailureDescription());
                    } else {
                        ModelNode modelNode = response.get(RESULT);
                        identityProvider = modelNode.get("identity-provider").asProperty().getName();
                        getView().update(modelNode);
                    }
                }
            });
        }
    }


    // ------------------------------------------------------ identity provider, handlers & trust domain

    public void modifyIdentityProvider(final Map<String, Object> changedValues) {
        AddressTemplate template = IDENTITY_PROVIDER_TEMPLATE.replaceWildcards(federation, identityProvider);
        modify(template, identityProvider, changedValues);
    }

    public void launchNewHandlerDialog() {
        ResourceDescription description = descriptionRegistry.lookup(IDENTITY_PROVIDER_HANDLER_TEMPLATE);
        AddressTemplate template = IDENTITY_PROVIDER_HANDLER_TEMPLATE.replaceWildcards(federation, identityProvider);
        launchAddDialog("SAML Handler", template, description);
    }

    public void removeHandler(final String name) {
        AddressTemplate template = IDENTITY_PROVIDER_HANDLER_TEMPLATE.replaceWildcards(federation, identityProvider);
        remove(template, name);
    }

    public void launchNewTrustDomainDialog() {
        ResourceDescription description = descriptionRegistry.lookup(IDENTITY_PROVIDER_TRUST_DOMAIN_TEMPLATE);
        AddressTemplate template = IDENTITY_PROVIDER_TRUST_DOMAIN_TEMPLATE
                .replaceWildcards(federation, identityProvider);
        launchAddDialog("SAML Handler", template, description);
    }

    public void modifyTrustDomain(final String name, final Map<String, Object> changedValues) {
        AddressTemplate template = IDENTITY_PROVIDER_TRUST_DOMAIN_TEMPLATE
                .replaceWildcards(federation, identityProvider);
        modify(template, name, changedValues);
    }

    public void removeTrustDomain(final String name) {
        AddressTemplate template = IDENTITY_PROVIDER_TRUST_DOMAIN_TEMPLATE
                .replaceWildcards(federation, identityProvider);
        remove(template, name);
    }


    // ------------------------------------------------------ handler parameter

    @Override
    public void onPropertyAdded(final PropertyAddedEvent event) {
        readFederation();
    }

    @Override
    public void onPropertyRemoved(final PropertyRemovedEvent event) {
        readFederation();
    }


    // ------------------------------------------------------ saml & key store configuration

    public void modifySamlConfiguration(final Map<String, Object> changedValues) {
        ResourceAddress address = SAML_TEMPLATE.resolve(statementContext, federation);
        modifySingleton("SAML configuration", address, changedValues);
    }

    public void modifyKeyStore(final Map<String, Object> changedValues) {
        ResourceAddress address = KEY_STORE_TEMPLATE.resolve(statementContext, federation);
        Operation.Builder builder = new Operation.Builder(ADD, address)
                .param("file", String.valueOf(changedValues.get("file")))
                .param("password", String.valueOf(changedValues.get("password")))
                .param("sign-key-alias", String.valueOf(changedValues.get("sign-key-alias")))
                .param("sign-key-password", String.valueOf(changedValues.get("sign-key-password")));
        if (changedValues.containsKey("relative-to")) {
            builder.param("relative-to", String.valueOf(changedValues.get("relative-to")));
        }
        modifySingleton("key store", address, changedValues, builder.build());
    }


    // ------------------------------------------------------ generic crud

    private void launchAddDialog(final String resourceName, final AddressTemplate template,
            final ResourceDescription resourceDescription, String... attributes) {
        DefaultWindow dialog = new DefaultWindow("New " + resourceName);
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
                                readFederation();
                            }

                            @Override
                            public void onSuccess(DMRResponse dmrResponse) {
                                Console.info("Successfully added " + name);
                                readFederation();
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
                        readFederation();
                    }

                    @Override
                    public void onFailure(final AddressTemplate addressTemplate, final String name, final Throwable t) {
                        Console.error("Unable to modify " + name, t.getMessage());
                        readFederation();
                    }
                });
    }

    private void modifySingleton(final String resourceName, final ResourceAddress address,
            final Map<String, Object> changedValues) {
        modifySingleton(resourceName, address, changedValues, null);
    }

    private void modifySingleton(final String resourceName, final ResourceAddress address,
            final Map<String, Object> changedValues, final Operation addOperation) {
        new SingletonFunctions(dispatcher, address, changedValues, addOperation).modify(
                new Outcome<FunctionContext>() {
                    @Override
                    public void onFailure(final FunctionContext context) {
                        Console.error("Cannot save " + resourceName, context.getErrorMessage());
                        readFederation();
                    }

                    @Override
                    public void onSuccess(final FunctionContext context) {
                        Console.info("Successfully modified " + resourceName);
                        readFederation();
                    }
                });
    }

    private void remove(final AddressTemplate template, final String name) {
        crud.onRemoveResource(template, name, new CrudOperationDelegate.Callback() {
            @Override
            public void onSuccess(final AddressTemplate addressTemplate, final String name) {
                Console.info("Successfully removed " + name);
                readFederation();
            }

            @Override
            public void onFailure(final AddressTemplate addressTemplate, final String name, final Throwable t) {
                readFederation();
                Console.error("Unable to remove " + name, t.getMessage());
            }
        });
    }


    // ------------------------------------------------------ properties

    public String getFederation() {
        return federation;
    }

    public String getIdentityProvider() {
        return identityProvider;
    }
}
