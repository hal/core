package org.jboss.as.console.client.shared.subsys.messaging;

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

import com.google.gwt.core.client.GWT;
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
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.domain.profiles.ProfileMgmtPresenter;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.standalone.ServerMgmtApplicationPresenter;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
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
 * The Presenter for hornetq providers
 *
 * @author Heiko Braun
 */
public class HornetqFinder extends Presenter<HornetqFinder.MyView, HornetqFinder.MyProxy> {

    private RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private final SecurityFramework securityFramework;
    private final StatementContext statementContext;

    private static AddressTemplate PROVIDER = AddressTemplate.of("{selected.profile}/subsystem=messaging/hornetq-server=*");

    private DefaultWindow providerDialog;
    private ProviderView providerView;


    @ProxyCodeSplit
    @NameToken(NameTokens.HornetqFinder)
    @RequiredResources(resources = {
            "{selected.profile}/subsystem=messaging/hornetq-server=*"
    }, recursive = false)
    @SearchIndex(keywords = {"topic", "queue", "jms", "messaging", "publish", "subscribe"})
    public interface MyProxy extends Proxy<HornetqFinder>, Place {
    }

    public interface MyView extends View {
        void setPresenter(HornetqFinder presenter);
        public void updateFrom(List<Property> list);
        public void setPreview(final SafeHtml html);
    }

    @Inject
    public HornetqFinder(
            EventBus eventBus, MyView view, MyProxy proxy,
            RevealStrategy revealStrategy, DispatchAsync dispatcher,
            ResourceDescriptionRegistry descriptionRegistry, SecurityFramework securityFramework, CoreGUIContext delegate) {
        super(eventBus, view, proxy);

        this.revealStrategy = revealStrategy;

        this.dispatcher = dispatcher;
        this.descriptionRegistry = descriptionRegistry;
        this.securityFramework = securityFramework;
        this.statementContext = delegate;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadProvider();
    }

    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return descriptionRegistry;
    }

    public SecurityFramework getSecurityFramework() {
        return securityFramework;
    }

    private void loadProvider() {
            loadProvider(null);

        }

    private void loadProvider(String name) {
        new LoadHornetQServersCmd(dispatcher).execute(
                new AsyncCallback<List<Property>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Console.error(((UIConstants) GWT.create(UIConstants.class)).failedToLoadServerNames(), caught.getMessage());
                    }

                    @Override
                    public void onSuccess(List<Property> result) {

                        getView().updateFrom(result);

                        // refresh the view if still open
                        if(providerDialog!=null && providerDialog.isVisible())
                        {
                            for (Property item: result) {
                                if(item.getName().equals(name))
                                {
                                    providerView.updateFrom(item);
                                    break;
                                }
                            }
                        }
                    }
                }
        );

    }

    @Override
    protected void revealInParent() {
        if(Console.getBootstrapContext().isStandalone())
            RevealContentEvent.fire(this, ServerMgmtApplicationPresenter.TYPE_MainContent, this);
        else
            RevealContentEvent.fire(this, ProfileMgmtPresenter.TYPE_MainContent, this);
    }

    public void onDeleteProvider(Property provider) {
        ResourceAddress fqAddress = PROVIDER.resolve(statementContext, provider.getName());

        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(ADDRESS).set(fqAddress);

        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {

            @Override
            public void onFailure(Throwable caught) {
                super.onFailure(caught);
                loadProvider();
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {

                ModelNode response = dmrResponse.get();
                if(response.isFailure())
                {
                    Console.error(Console.MESSAGES.failedToRemoveResource(fqAddress.toString()), response.getFailureDescription());
                }
                else
                {
                    Console.info(Console.MESSAGES.successfullyRemoved(fqAddress.toString()));
                }

                loadProvider();
            }
        });

    }

    public void launchNewProviderWizard() {
        final SecurityContext securityContext =
                securityFramework.getSecurityContext(getProxy().getNameToken());

        final ResourceDescription resourceDescription = descriptionRegistry.lookup(PROVIDER);

        final DefaultWindow dialog = new DefaultWindow(Console.MESSAGES.newTitle("Messaging Provider"));
        AddResourceDialog addDialog = new AddResourceDialog(securityContext, resourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        dialog.hide();

                        final ResourceAddress fqAddress =
                                PROVIDER.resolve(statementContext, payload.get("name").asString());

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
                                Console.info(Console.MESSAGES.successfullyAdded(fqAddress.toString()));
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

    public void onLaunchProviderSettings(Property provider) {
        providerDialog = new DefaultWindow("Provider Settings");

        providerView = new ProviderView(this);

        providerDialog.setWidth(640);
        providerDialog.setHeight(480);
        providerDialog.trapWidget(providerView.asWidget());
        providerDialog.setGlassEnabled(true);
        providerDialog.center();


        providerView.updateFrom(provider);

    }

    public void onSaveProvider(Property provider, Map<String, Object> changeset) {


        ResourceAddress fqAddress = PROVIDER.resolve(statementContext, provider.getName());

        final ModelNodeAdapter adapter = new ModelNodeAdapter();
        ModelNode operation = adapter.fromChangeset(changeset, fqAddress);

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                Console.error(Console.MESSAGES.failedToModifyResource(fqAddress.toString()), caught.getMessage());
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failedToModifyResource(fqAddress.toString()), response.getFailureDescription());
                }
                else {
                    Console.info(Console.MESSAGES.successfullyModifiedResource(fqAddress.toString()));
                }

                loadProvider(provider.getName());
            }
        });
    }


}

