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
package org.jboss.as.console.client.shared.subsys.activemq;

import java.util.List;
import java.util.Map;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.domain.profiles.ProfileMgmtPresenter;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.standalone.ServerMgmtApplicationPresenter;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.client.v3.widgets.SuggestionResource;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.as.console.mbui.behaviour.ModelNodeAdapter;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.StatementContext;

import static org.jboss.as.console.client.meta.CoreCapabilitiesRegister.SECURITY_DOMAIN;
import static org.jboss.as.console.client.shared.subsys.activemq.MessagingAddress.PROVIDER_ADDRESS;
import static org.jboss.as.console.client.shared.subsys.activemq.MessagingAddress.PROVIDER_TEMPLATE;
import static org.jboss.as.console.client.shared.subsys.activemq.MessagingAddress.PATH_ADDRESS;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Claudio Miranda
 */
public class ActivemqFinder extends Presenter<ActivemqFinder.MyView, ActivemqFinder.MyProxy>
        implements PreviewEvent.Handler {

    @ProxyCodeSplit
    @NameToken(NameTokens.ActivemqFinder)
    @RequiredResources(resources = {MessagingAddress.ROOT_ADDRESS, PROVIDER_ADDRESS, PATH_ADDRESS}, recursive = false)
    public interface MyProxy extends Proxy<ActivemqFinder>, Place {}

    public interface MyView extends View {
        void setPresenter(ActivemqFinder presenter);
        void setPreview(SafeHtml html);
        void updateFrom(List<Property> list);
    }

    private final DispatchAsync dispatcher;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private final SecurityFramework securityFramework;
    private final StatementContext statementContext;
    private final BootstrapContext bootstrapContext;

    private DefaultWindow providerDialog;
    private ProviderView providerView;

    @Inject
    public ActivemqFinder(EventBus eventBus,
            ActivemqFinder.MyView view,
            ActivemqFinder.MyProxy proxy,
            DispatchAsync dispatcher,
            ResourceDescriptionRegistry descriptionRegistry,
            SecurityFramework securityFramework,
            StatementContext statementContext,
            BootstrapContext bootstrapContext) {

        super(eventBus, view, proxy, bootstrapContext.isStandalone() ?
                ServerMgmtApplicationPresenter.TYPE_MainContent :
                ProfileMgmtPresenter.TYPE_MainContent);

        this.dispatcher = dispatcher;
        this.descriptionRegistry = descriptionRegistry;
        this.securityFramework = securityFramework;
        this.statementContext = statementContext;
        this.providerView = new ProviderView(this);
        this.bootstrapContext = bootstrapContext;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getEventBus().addHandler(PreviewEvent.TYPE, this);
        getView().setPresenter(this);
    }

    @Override
    protected void revealInParent() {
        if(Console.getBootstrapContext().isStandalone())
            RevealContentEvent.fire(this, ServerMgmtApplicationPresenter.TYPE_MainContent, this);
        else
            RevealContentEvent.fire(this, ProfileMgmtPresenter.TYPE_MainContent, this);
    }

    @Override
    public void onPreview(PreviewEvent event) {
        if(isVisible())
            getView().setPreview(event.getHtml());
    }

    public SecurityFramework getSecurityFramework() {
        return securityFramework;
    }

    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return descriptionRegistry;
    }


    // ------------------------------------------------------ provider methods

    public void launchNewProviderWizard() {
        SecurityContext securityContext = securityFramework.getSecurityContext(getProxy().getNameToken());
        ResourceDescription resourceDescription = descriptionRegistry.lookup(PROVIDER_TEMPLATE);

        final DefaultWindow dialog = new DefaultWindow(
                Console.MESSAGES.newMessagingProvider());
        AddResourceDialog addDialog = new AddResourceDialog(securityContext, resourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        dialog.hide();
                        String name = payload.get("name").asString();
                        ResourceAddress fqAddress = PROVIDER_TEMPLATE.resolve(statementContext, name);
                        payload.get(OP).set(ADD);
                        payload.get(ADDRESS).set(fqAddress);

                        dispatcher.execute(new DMRAction(payload), new SimpleCallback<DMRResponse>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                super.onFailure(caught);
                                loadProvider();
                            }

                            @Override
                            public void onSuccess(DMRResponse dmrResponse) {
                                Console.info(Console.MESSAGES
                                        .successfullyAddedMessagingProvider(name));
                                loadProvider();
                            }
                        });
                    }

                    @Override
                    public void onCancel() {
                        dialog.hide();
                    }
                })
                .addFactory("security-domain", attributeDescription ->  {
                    SuggestionResource suggestionResource = new SuggestionResource("security-domain", "Security domain", true,
                            Console.MODULES.getCapabilities().lookup(SECURITY_DOMAIN));
                    return suggestionResource.buildFormItem();
                })
                .include("security-enabled", "security-domain", "cluster-user", "cluster-password");

        dialog.setWidth(640);
        dialog.setHeight(480);
        dialog.setWidget(addDialog);
        dialog.setGlassEnabled(true);
        dialog.center();
    }

    void loadProvider() {
        loadProvider(null);
    }

    private void loadProvider(String name) {
        new LoadActivemqServersCmd(dispatcher, statementContext).execute(new AsyncCallback<List<Property>>() {
            @Override
            public void onFailure(Throwable caught) {
                Console.error(Console.MESSAGES.failedToLoadMessagingServerNames(), caught.getMessage());
            }

            @Override
            public void onSuccess(List<Property> result) {
                getView().updateFrom(result);

                // refresh the view if still open
                if (providerDialog != null && providerDialog.isVisible()) {
                    for (Property item : result) {
                        if (item.getName().equals(name)) {
                            providerView.updateFrom(item);
                            break;
                        }
                    }
                }
            }
        });
    }

    public void onSaveProvider(Property provider, Map<String, Object> changeset) {
        onSaveProvider(provider, changeset, PROVIDER_TEMPLATE);
    }

    public void onSaveProvider(Property provider, Map<String, Object> changeset, AddressTemplate address) {
        ResourceAddress fqAddress = address.resolve(statementContext, provider.getName());
        ModelNodeAdapter adapter = new ModelNodeAdapter();
        ModelNode operation = adapter.fromChangeset(changeset, fqAddress);

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                Console.error(
                        Console.MESSAGES.failedToModifyMessagingProvider(provider.getName()), caught.getMessage());
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failedToModifyMessagingProvider(provider.getName()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES
                            .successfullyModifiedMessagingProvider(provider.getName()));
                }
                reloadProviderDialog(provider.getName());
            }
        });
    }

    public void onDeleteProvider(Property provider) {
        Operation operation = new Operation.Builder(REMOVE,
                PROVIDER_TEMPLATE.resolve(statementContext, provider.getName())).build();
        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                super.onFailure(caught);
                loadProvider();
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES
                                    .failedToRemoveMessagingProvider(provider.getName()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES
                            .successfullyRemovedMessagingProvider(provider.getName()));
                }
                loadProvider();
            }
        });
    }

    public void saveAttribute(final String complexAttributeName, final String resourceName, final Map changeset) {

        ResourceAddress address = PROVIDER_TEMPLATE.resolve(statementContext, resourceName);
        final org.jboss.as.console.client.v3.behaviour.ModelNodeAdapter adapter = new org.jboss.as.console.client.v3.behaviour.ModelNodeAdapter();
        ModelNode operation = adapter.fromComplexAttributeChangeSet(address, complexAttributeName, changeset);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failedToModifyMessagingProvider(resourceName),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES
                            .successfullyModifiedMessagingProvider(resourceName));
                    reloadProviderDialog(resourceName);
                }
            }
        });

    }

    public void undefineAttribute(final String complexAttributeName, final String resourceName) {
        ResourceAddress address = PROVIDER_TEMPLATE.resolve(statementContext, resourceName);

        Operation operation = new Operation.Builder(UNDEFINE_ATTRIBUTE_OPERATION, address)
                .param(NAME, complexAttributeName)
                .build();

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failedToModifyMessagingProvider(resourceName), response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.successfullyModifiedMessagingProvider(resourceName));
                    reloadProviderDialog(resourceName);
                }
            }
        });

    }

    public void onLaunchProviderSettings(Property provider) {
        providerDialog = new DefaultWindow(Console.MESSAGES.providerSettings());
        providerDialog.setWidth(840);
        providerDialog.setHeight(480);
        providerDialog.trapWidget(providerView.asWidget());
        providerDialog.setGlassEnabled(true);
        providerDialog.center();

        providerView.updateFrom(provider);
    }

    private void reloadProviderDialog(String name) {
        loadProvider(name);
        if (!bootstrapContext.isStandalone()) {
            providerDialog.hide();
        }
    }
}
