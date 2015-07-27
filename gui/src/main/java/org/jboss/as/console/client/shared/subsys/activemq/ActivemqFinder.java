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
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.domain.profiles.ProfileMgmtPresenter;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.standalone.ServerMgmtApplicationPresenter;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.client.widgets.nav.v3.PreviewEvent;
import org.jboss.as.console.mbui.behaviour.ModelNodeAdapter;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
public class ActivemqFinder extends Presenter<ActivemqFinder.MyView, ActivemqFinder.MyProxy>
        implements MessagingAddress, PreviewEvent.Handler {

    // ------------------------------------------------------ proxy & view
    // @formatter:off
    @ProxyCodeSplit
    @NameToken(NameTokens.ActivemqFinder)
    @RequiredResources(resources = PROVIDER_ADDRESS, recursive = false)
    @SearchIndex(keywords = {"topic", "queue", "jms", "messaging", "publish", "subscribe"})
    public interface MyProxy extends Proxy<ActivemqFinder>, Place {}

    public interface MyView extends View, HasPresenter<ActivemqFinder> {
        void updateFrom(List<Property> list);
        void setPreview(final SafeHtml html);
    }
    // @formatter:on


    private final DispatchAsync dispatcher;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private final SecurityFramework securityFramework;
    private final StatementContext statementContext;

    private DefaultWindow providerDialog;
    private ProviderView providerView;


    // ------------------------------------------------------ presenter lifecycle

    @Inject
    public ActivemqFinder(EventBus eventBus,
            MyView view,
            MyProxy proxy,
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
    }

    @Override
    public void onPreview(PreviewEvent event) {
        if(isVisible())
            getView().setPreview(event.getHtml());
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        getEventBus().addHandler(PreviewEvent.TYPE, this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadProvider();
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

        final DefaultWindow dialog = new DefaultWindow("New Messaging Provider");
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
                                Console.info("Successfully added messaging provider " + name);
                                loadProvider();
                            }
                        });
                    }

                    @Override
                    public void onCancel() {
                        dialog.hide();
                    }
                })
                .include("security-enabled", "security-domain", "cluster-user", "cluster-password");

        dialog.setWidth(640);
        dialog.setHeight(480);
        dialog.setWidget(addDialog);
        dialog.setGlassEnabled(true);
        dialog.center();
    }

    private void loadProvider() {
        loadProvider(null);
    }

    private void loadProvider(String name) {
        new LoadActivemqServersCmd(dispatcher, statementContext).execute(new AsyncCallback<List<Property>>() {
            @Override
            public void onFailure(Throwable caught) {
                Console.error("Failed to load messaging server names", caught.getMessage());
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
        ResourceAddress fqAddress = PROVIDER_TEMPLATE.resolve(statementContext, provider.getName());
        ModelNodeAdapter adapter = new ModelNodeAdapter();
        ModelNode operation = adapter.fromChangeset(changeset, fqAddress);

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                Console.error("Failed to modify messaging provider  " + provider.getName(), caught.getMessage());
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    Console.error("Failed to modify messaging provider" + provider.getName(),
                            response.getFailureDescription());
                } else {
                    Console.info("Successfully modified messaging provider " + provider.getName());
                }
                loadProvider(provider.getName());
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
                    Console.error("Failed to remove messaging provider " + provider.getName(),
                            response.getFailureDescription());
                } else {
                    Console.info("Successfully removed messaging provider " + provider.getName());
                }
                loadProvider();
            }
        });
    }

    public void onLaunchProviderSettings(Property provider) {
        providerDialog = new DefaultWindow("Provider Settings");
        providerDialog.setWidth(640);
        providerDialog.setHeight(480);
        providerDialog.trapWidget(providerView.asWidget());
        providerDialog.setGlassEnabled(true);
        providerDialog.center();

        providerView.updateFrom(provider);
    }
}
