package org.jboss.as.console.client.shared.subsys.infinispan.v3;

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

import com.allen_sauer.gwt.log.client.Log;
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
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.remoting.store.CreateConnection;
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
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.FilteringStatementContext;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * The Presenter for Caches
 *
 * @author Heiko Braun
 */
public class CachesPresenter extends Presenter<CachesPresenter.MyView, CachesPresenter.MyProxy> {

    private RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private final SecurityFramework securityFramework;
    private final StatementContext statementContext;



    @ProxyCodeSplit
    @NameToken(NameTokens.CachesPresenter)
    @RequiredResources(resources = {
            "{selected.profile}/subsystem=infinispan/cache-container=*",

            "{selected.profile}/subsystem=infinispan/cache-container=*/local-cache=*",
            "{selected.profile}/subsystem=infinispan/cache-container=*/local-cache=*/transaction=TRANSACTION",
            "{selected.profile}/subsystem=infinispan/cache-container=*/local-cache=*/locking=LOCKING",
            "{selected.profile}/subsystem=infinispan/cache-container=*/local-cache=*/file-store=FILE_STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/local-cache=*/eviction=EVICTION",
            "{selected.profile}/subsystem=infinispan/cache-container=*/local-cache=*/store=STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/local-cache=*/remote-store=REMOTE_STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/local-cache=*/expiration=EXPIRATION",
            "{selected.profile}/subsystem=infinispan/cache-container=*/local-cache=*/mixed-keyed-jdbc-store=MIXED_KEYED_JDBC_STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/local-cache=*/binary-keyed-jdbc-store=BINARY_KEYED_JDBC_STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/local-cache=*/string-keyed-jdbc-store=STRING_KEYED_JDBC_STORE",

            "{selected.profile}/subsystem=infinispan/cache-container=*/distributed-cache=*",
            "{selected.profile}/subsystem=infinispan/cache-container=*/distributed-cache=*/transaction=TRANSACTION",
            "{selected.profile}/subsystem=infinispan/cache-container=*/distributed-cache=*/locking=LOCKING",
            "{selected.profile}/subsystem=infinispan/cache-container=*/distributed-cache=*/file-store=FILE_STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/distributed-cache=*/eviction=EVICTION",
            "{selected.profile}/subsystem=infinispan/cache-container=*/distributed-cache=*/store=STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/distributed-cache=*/remote-store=REMOTE_STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/distributed-cache=*/expiration=EXPIRATION",
            "{selected.profile}/subsystem=infinispan/cache-container=*/distributed-cache=*/mixed-keyed-jdbc-store=MIXED_KEYED_JDBC_STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/distributed-cache=*/binary-keyed-jdbc-store=BINARY_KEYED_JDBC_STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/distributed-cache=*/string-keyed-jdbc-store=STRING_KEYED_JDBC_STORE",

            "{selected.profile}/subsystem=infinispan/cache-container=*/replicated-cache=*",
            "{selected.profile}/subsystem=infinispan/cache-container=*/replicated-cache=*/transaction=TRANSACTION",
            "{selected.profile}/subsystem=infinispan/cache-container=*/replicated-cache=*/locking=LOCKING",
            "{selected.profile}/subsystem=infinispan/cache-container=*/replicated-cache=*/file-store=FILE_STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/replicated-cache=*/eviction=EVICTION",
            "{selected.profile}/subsystem=infinispan/cache-container=*/replicated-cache=*/store=STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/replicated-cache=*/remote-store=REMOTE_STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/replicated-cache=*/expiration=EXPIRATION",
            "{selected.profile}/subsystem=infinispan/cache-container=*/replicated-cache=*/mixed-keyed-jdbc-store=MIXED_KEYED_JDBC_STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/replicated-cache=*/binary-keyed-jdbc-store=BINARY_KEYED_JDBC_STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/replicated-cache=*/string-keyed-jdbc-store=STRING_KEYED_JDBC_STORE",


            "{selected.profile}/subsystem=infinispan/cache-container=*/invalidation-cache=*",
            "{selected.profile}/subsystem=infinispan/cache-container=*/invalidation-cache=*/transaction=TRANSACTION",
            "{selected.profile}/subsystem=infinispan/cache-container=*/invalidation-cache=*/locking=LOCKING",
            "{selected.profile}/subsystem=infinispan/cache-container=*/invalidation-cache=*/file-store=FILE_STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/invalidation-cache=*/eviction=EVICTION",
            "{selected.profile}/subsystem=infinispan/cache-container=*/invalidation-cache=*/store=STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/invalidation-cache=*/remote-store=REMOTE_STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/invalidation-cache=*/expiration=EXPIRATION",
            "{selected.profile}/subsystem=infinispan/cache-container=*/invalidation-cache=*/mixed-keyed-jdbc-store=MIXED_KEYED_JDBC_STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/invalidation-cache=*/binary-keyed-jdbc-store=BINARY_KEYED_JDBC_STORE",
            "{selected.profile}/subsystem=infinispan/cache-container=*/invalidation-cache=*/string-keyed-jdbc-store=STRING_KEYED_JDBC_STORE",

    })
    @SearchIndex(keywords = {
            "cache", "ejb", "hibernate", "web", "transport"
    })
    public interface MyProxy extends Proxy<CachesPresenter>, Place {
    }

    private String container;

    public interface MyView extends View {
        void setPresenter(CachesPresenter presenter);
        //public void updateFrom(List<Property> list);
        void setPreview(final SafeHtml html);
        void updateLocalCache(ModelNode modelNode);
        void updateDistributedCache(ModelNode modelNode);

        void updateReplicatedCaches(ModelNode modelNode);

        void updateInvalidationCache(ModelNode modelNode);
    }

    @Inject
    public CachesPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            RevealStrategy revealStrategy, DispatchAsync dispatcher,
            ResourceDescriptionRegistry descriptionRegistry, SecurityFramework securityFramework, CoreGUIContext delegate) {
        super(eventBus, view, proxy);

        this.revealStrategy = revealStrategy;

        this.dispatcher = dispatcher;
        this.descriptionRegistry = descriptionRegistry;
        this.securityFramework = securityFramework;
        this.statementContext =  new FilteringStatementContext(
                delegate,
                new FilteringStatementContext.Filter() {
                    @Override
                    public String filter(String key) {
                        if ("selected.container".equals(key))
                            return container;
                        else
                            return null;
                    }

                    @Override
                    public String[] filterTuple(String key) {
                        return null;
                    }
                }
        ) {

        };

    }

    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return descriptionRegistry;
    }

    public String getContainer() {
        return container;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {
        super.prepareFromRequest(request);
        container = request.getParameter("container", null);
    }

    @Override
    protected void onReset() {
        super.onReset();

        loadContainer();

    }

    private void loadContainer() {

        if(null==container)
            throw new RuntimeException("No container specified");

        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "infinispan");
        operation.get(ADDRESS).add("cache-container", container);
        operation.get(RECURSIVE).set(true);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure())
                {
                    Log.error("Failed to load cache container "+container, response.getFailureDescription());
                }
                else
                {

                    ModelNode payload = response.get(RESULT);
                    getView().updateLocalCache(payload.get("local-cache"));
                    getView().updateInvalidationCache(payload.get("invalidation-cache"));
                    getView().updateReplicatedCaches(payload.get("replicated-cache"));
                    getView().updateDistributedCache(payload.get("distributed-cache"));
                }

            }
        });
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    public SecurityFramework getSecurityFramework() {
        return securityFramework;
    }


    public void onCreate(AddressTemplate address, String name, ModelNode entity) {

        ResourceAddress fqAddress = address.resolve(statementContext, container,name);

        entity.get(OP).set(ADD);
        entity.get(ADDRESS).set(fqAddress);

        dispatcher.execute(new DMRAction(entity), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure())
                {
                    Console.error("Failed to create resource "+fqAddress, response.getFailureDescription());
                }
                else
                {

                    Console.info("Successfully created "+fqAddress);
                }

                loadContainer();
            }
        });
    }

    public void onSave(AddressTemplate address, String name, Map<String, Object> changeset) {
        ResourceAddress fqAddress = address.resolve(statementContext, container, name);

        final ModelNodeAdapter adapter = new ModelNodeAdapter();
        ModelNode operation = adapter.fromChangeset(changeset, fqAddress);

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                Console.error("Failed to modify resource "+fqAddress, caught.getMessage());
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    Console.error("Failed to modify resource " + fqAddress, response.getFailureDescription());
                }
                else {
                    Console.info("Successfully modified "+fqAddress);
                }
            }
        });

    }

    public void onLaunchAddWizard(AddressTemplate cacheType) {

        final SecurityContext securityContext =
                getSecurityFramework().getSecurityContext(getProxy().getNameToken());

        final ResourceDescription resourceDescription = getDescriptionRegistry().lookup(cacheType);

        final DefaultWindow dialog = new DefaultWindow("New Cache Configuration");
        AddResourceDialog addDialog = new AddResourceDialog(securityContext, resourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        dialog.hide();

                        final ResourceAddress fqAddress =
                                cacheType.resolve(statementContext, container, payload.get("name").asString());

                        payload.get(OP).set(ADD);
                        payload.get(ADDRESS).set(fqAddress);

                        dispatcher.execute(new DMRAction(payload), new SimpleCallback<DMRResponse>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                super.onFailure(caught);
                                loadContainer();
                            }

                            @Override
                            public void onSuccess(DMRResponse dmrResponse) {
                                Console.info("Successfully added "+fqAddress);
                                loadContainer();
                            }
                        });


                    }

            @Override
            public void onCancel() {
                dialog.hide();
            }
        });
        dialog.setWidth(640);
        dialog.setHeight(480);
        dialog.setWidget(addDialog);
        dialog.setGlassEnabled(true);
        dialog.center();
    }

    public void onRemoveCache(AddressTemplate cacheType, String name) {

        ResourceAddress fqAddress = cacheType.resolve(statementContext, container, name);

        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(ADDRESS).set(fqAddress);

        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {

            @Override
            public void onFailure(Throwable caught) {
                super.onFailure(caught);
                loadContainer();
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {

                ModelNode response = dmrResponse.get();
                if(response.isFailure())
                {
                    Console.error("Failed to remove resource "+fqAddress, response.getFailureDescription());
                }
                else
                {
                    Console.info("Successfully removed " + fqAddress);
                }

                loadContainer();
            }
        });


    }

}

