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
package org.jboss.as.console.client.shared.subsys.iiopopenjdk;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.widgets.PropertyAddedEvent;
import org.jboss.as.console.client.v3.widgets.PropertyAddedEvent.PropertyAddedHandler;
import org.jboss.as.console.client.v3.widgets.PropertyRemovedEvent;
import org.jboss.as.console.client.v3.widgets.PropertyRemovedEvent.PropertyRemovedHandler;
import org.jboss.as.console.spi.RequiredResources;
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
public class IiopOpenJdkPresenter extends Presenter<IiopOpenJdkPresenter.MyView, IiopOpenJdkPresenter.MyProxy>
        implements PropertyAddedHandler, PropertyRemovedHandler {

    @ProxyCodeSplit
    @NameToken(NameTokens.IiopOpenJdk)
    @RequiredResources(resources = IIOP_OPENJDK_SUBSYSTEM_ADDRESS)
    public interface MyProxy extends ProxyPlace<IiopOpenJdkPresenter> {

    }


    public interface MyView extends View, HasPresenter<IiopOpenJdkPresenter> {

        void update(ModelNode model);
    }


    public static final String IIOP_OPENJDK_SUBSYSTEM_ADDRESS = "{selected.profile}/subsystem=iiop-openjdk";
    public static final AddressTemplate IIOP_OPENJDK_SUBSYSTEM_TEMPLATE = AddressTemplate.of(
            IIOP_OPENJDK_SUBSYSTEM_ADDRESS);

    private final RevealStrategy revealStrategy;
    private final StatementContext statementContext;
    private final DispatchAsync dispatcher;
    private final CrudOperationDelegate operationDelegate;

    @Inject
    public IiopOpenJdkPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final RevealStrategy revealStrategy, final StatementContext statementContext,
            final DispatchAsync dispatcher) {
        super(eventBus, view, proxy);
        this.revealStrategy = revealStrategy;
        this.statementContext = statementContext;
        this.dispatcher = dispatcher;
        this.operationDelegate = new CrudOperationDelegate(statementContext, dispatcher);
    }


    // ------------------------------------------------------ lifecycle

    @Override
    @SuppressWarnings("unchecked")
    protected void onBind() {
        super.onBind();

        // property management is not handled by the store, but using MapAttributePropertyManager
        // so register handlers to get notified about property modifications
        addRegisteredHandler(PropertyAddedEvent.TYPE, this);
        addRegisteredHandler(PropertyRemovedEvent.TYPE, this);

        getView().setPresenter(this);
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        refresh();
    }

    private void refresh() {
        ModelNode op = new ModelNode();
        op.get(ADDRESS).set(IIOP_OPENJDK_SUBSYSTEM_TEMPLATE.resolve(statementContext));
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(INCLUDE_RUNTIME).set(true);
        op.get(RECURSIVE).set(true);

        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                Console.error(Console.MESSAGES.failed("Read IIOP Subsystem"), caught.getMessage());
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Read IIOP Subsystem"), response.getFailureDescription());
                } else {
                    ModelNode result = response.get(RESULT);
                    getView().update(result);
                }
            }
        });
    }


    // ------------------------------------------------------ iiop methods

    public void save(Map<String, Object> changedValues) {
        operationDelegate.onSaveResource(IIOP_OPENJDK_SUBSYSTEM_TEMPLATE, null, changedValues,
                new CrudOperationDelegate.Callback() {
                    @Override
                    public void onSuccess(final AddressTemplate addressTemplate, final String name) {
                        Console.info(Console.MESSAGES.modified("IIOP Subsystem"));
                        refresh();
                    }

                    @Override
                    public void onFailure(final AddressTemplate addressTemplate, final String name, final Throwable t) {
                        Console.error(Console.MESSAGES.modificationFailed("IIOP Subsystem"), t.getMessage());
                        refresh();
                    }
                });
    }

    @Override
    public void onPropertyAdded(final PropertyAddedEvent event) {
        refresh();
    }

    @Override
    public void onPropertyRemoved(final PropertyRemovedEvent event) {
        refresh();
    }
}
