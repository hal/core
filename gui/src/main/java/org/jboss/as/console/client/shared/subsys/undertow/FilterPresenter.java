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
package org.jboss.as.console.client.shared.subsys.undertow;

import java.util.List;
import java.util.Map;

import com.allen_sauer.gwt.log.client.Log;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.FilteringStatementContext;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 * @since 04/06/2016
 */
public class FilterPresenter extends Presenter<FilterPresenter.MyView, FilterPresenter.MyProxy> {

    private final PlaceManager placeManager;
    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final CrudOperationDelegate operationDelegate;
    private final FilteringStatementContext statementContext;

    private DefaultWindow window;

    @RequiredResources( resources = {
            "{selected.profile}/subsystem=undertow/configuration=filter" ,
            "{selected.profile}/subsystem=undertow/configuration=filter/custom-filter=*" ,
            "{selected.profile}/subsystem=undertow/configuration=filter/error-page=*" ,
            "{selected.profile}/subsystem=undertow/configuration=filter/expression-filter=*" ,
            "{selected.profile}/subsystem=undertow/configuration=filter/gzip=*" ,
            "{selected.profile}/subsystem=undertow/configuration=filter/mod-cluster=*" ,
            "{selected.profile}/subsystem=undertow/configuration=filter/request-limit=*" ,
            "{selected.profile}/subsystem=undertow/configuration=filter/response-header=*",
            "{selected.profile}/subsystem=undertow/configuration=filter/rewrite=*" },
            recursive = true)
    @ProxyCodeSplit
    @NameToken(NameTokens.UndertowFilters)
    public interface MyProxy extends Proxy<FilterPresenter>, Place {
    }

    public interface MyView extends View {
        void setFilters(List<ModelNode> filters);
        void setPresenter(FilterPresenter presenter);
    }


    CrudOperationDelegate.Callback defaultAddOpCallbacks = new CrudOperationDelegate.Callback() {
        @Override
        public void onSuccess(AddressTemplate address, String name) {
            Console.info(Console.MESSAGES.added("Undertow filter"));
            loadFilters();
        }

        @Override
        public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
            Console.error(Console.MESSAGES.addingFailed("Undertow filter"), t.getMessage());
        }
    };

    CrudOperationDelegate.Callback defaultSaveOpCallbacks = new CrudOperationDelegate.Callback() {
        @Override
        public void onSuccess(AddressTemplate address, String name) {
            Console.info(Console.MESSAGES.saved("Undertow filter"));
            loadFilters();
        }

        @Override
        public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
            Console.error(Console.MESSAGES.saveFailed("Undertow filter"), t.getMessage());
        }
    };

    private SecurityFramework securityFramework;
    private ResourceDescriptionRegistry descriptionRegistry;

    public SecurityFramework getSecurityFramework() {
        return securityFramework;
    }

    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return descriptionRegistry;
    }


    @Inject
    public FilterPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            PlaceManager placeManager, RevealStrategy revealStrategy,
            DispatchAsync dispatcher, CoreGUIContext statementContext,
            SecurityFramework securityFramework, ResourceDescriptionRegistry descriptionRegistry) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.revealStrategy = revealStrategy;
        this.dispatcher = dispatcher;
        this.securityFramework = securityFramework;
        this.descriptionRegistry = descriptionRegistry;

        this.statementContext =  new FilteringStatementContext(
                statementContext,
                new FilteringStatementContext.Filter() {
                    @Override
                    public String filter(String key) {
                        // TODO
                        return null;
                    }

                    @Override
                    public String[] filterTuple(String key) {
                        return null;
                    }
                }
        );

        this.operationDelegate = new CrudOperationDelegate(this.statementContext, dispatcher);

    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }


    @Override
    protected void onReset() {
        super.onReset();
        loadFilters();
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    public void loadFilters() {

        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "undertow");
        operation.get(ADDRESS).add("configuration", "filter");
        operation.get(RECURSIVE).set(true);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if(response.isFailure()) {
                    Log.error("Failed to load servlet container details", response.getFailureDescription());
                }
                else
                {
                    ModelNode data = response.get(RESULT);
                    List<ModelNode> resList = data.asList();
                    getView().setFilters(resList);
                }

            }
        });
    }

    // -----------------------

    public void onLaunchAddResourceDialog(final AddressTemplate address, String title) {

        String type = address.getResourceType();

        window = new DefaultWindow(Console.MESSAGES.createTitle(title));
        window.setWidth(480);
        window.setHeight(360);

        AddResourceDialog addDialog = new AddResourceDialog(
                Console.MODULES.getSecurityFramework().getSecurityContext(NameTokens.UndertowFilters),
                descriptionRegistry.lookup(address),
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        window.hide();
                        operationDelegate.onCreateResource(
                                address, payload.get("name").asString(), payload, defaultAddOpCallbacks);
                    }

                    @Override
                    public void onCancel() {
                        window.hide();
                    }
                }
        );
        window.setWidget(addDialog);
        window.setGlassEnabled(true);
        window.center();
    }
    
    public void onRemoveResource(final AddressTemplate address, final String name) {
        operationDelegate.onRemoveResource(address, name, defaultAddOpCallbacks);
    }

    /**
     * Save an existent filter.
     */
    public void onSaveFilter(AddressTemplate address, String name, Map changeset) {
        operationDelegate.onSaveResource(address, name, changeset, defaultSaveOpCallbacks);
    }

    public PlaceManager getPlaceManager() {
        return placeManager;
    }
}
