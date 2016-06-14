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
package org.jboss.as.console.client.shared.subsys.activemq;

import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.client.v3.widgets.PropertyAddedEvent;
import org.jboss.as.console.client.v3.widgets.PropertyRemovedEvent;
import org.jboss.as.console.mbui.behaviour.ModelNodeAdapter;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.StatementContext;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Claudio Miranda
 */
public class JMSBridgePresenter
        extends Presenter<JMSBridgePresenter.MyView, JMSBridgePresenter.MyProxy>
        implements MessagingAddress, PropertyAddedEvent.PropertyAddedHandler,
        PropertyRemovedEvent.PropertyRemovedHandler {

    // ------------------------------------------------------ proxy & view
    // @formatter:off
    @ProxyCodeSplit
    @NameToken(NameTokens.JMSBridge)
    @RequiredResources(resources = {JMSBRIDGE_ADDRESS, JMSBRIDGE_ENTITY_ADDRESS})
    public interface MyProxy extends Proxy<JMSBridgePresenter>, Place {}

    public interface MyView extends View, HasPresenter<JMSBridgePresenter> {
        void setBridges(List<Property> bridges);
    }
    // @formatter:on

    private final DispatchAsync dispatcher;
    private final RevealStrategy revealStrategy;
    private final SecurityFramework securityFramework;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private final StatementContext statementContext;

    @Inject
    public JMSBridgePresenter(EventBus eventBus, MyView view, MyProxy proxy,
            DispatchAsync dispatcher, RevealStrategy revealStrategy, SecurityFramework securityFramework, 
            ResourceDescriptionRegistry descriptionRegistry, StatementContext statementContext) {
        super(eventBus, view, proxy);

        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
        this.securityFramework = securityFramework;
        this.descriptionRegistry = descriptionRegistry;
        this.statementContext = statementContext;
    }

    @Override
    protected void onBind() {
        super.onBind();
        // property management is not handled by the store, but using MapAttributePropertyManager
        // so register handlers to get notified about property modifications
        addVisibleHandler(PropertyAddedEvent.TYPE, this);
        addVisibleHandler(PropertyRemovedEvent.TYPE, this);
        
        getView().setPresenter(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadBridges();
    }

    private void loadBridges() {
        Operation operation = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION,
                ROOT_TEMPLATE.resolve(statementContext))
                .param(CHILD_TYPE, JMS_BRIDGE).build();

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                getView().setBridges(response.get(RESULT).asPropertyList());
            }
        });
    }

    void onSaveJmsBridge(final String jmsBridgeName, Map<String, Object> changeset) {
        ModelNodeAdapter adapter = new ModelNodeAdapter();

        ModelNode address = new ModelNode();
        address.get(ADDRESS).set(Baseadress.get());
        address.get(ADDRESS).add("subsystem", "messaging-activemq");
        address.get(ADDRESS).add("jms-bridge", jmsBridgeName);
        ModelNode operation = adapter.fromChangeset(changeset, address);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();

                if (response.isFailure()) {
                    Console.error("Failed to save JMS Bridge " + jmsBridgeName, response.getFailureDescription());
                } else {
                    Console.info("Successfully saved provider " + jmsBridgeName);
                    loadBridges(); // refresh
                }
            }
        });
    }

    void onDeleteJmsBridge(final String name) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(REMOVE);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("jms-bridge", name);

        Operation operation2 = new Operation.Builder(REMOVE,
                JMSBRIDGE_TEMPLATE.replaceWildcards(name).resolve(statementContext))
                .build();

        dispatcher.execute(new DMRAction(operation2), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                boolean successful = response.get(OUTCOME).asString().equals(SUCCESS);
                if (successful) {
                    Console.info(Console.MESSAGES.deleted("JMS Bridge " + name));
                } else {
                    Console.error(Console.MESSAGES.deletionFailed("JMS Bridge " + name), response.toString());
                }
                loadBridges();
            }
        });

    }
    
    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return descriptionRegistry;
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }
    
    public DispatchAsync getDispatcher() {
        return dispatcher;
    }

    @Override
    public void onPropertyAdded(final PropertyAddedEvent event) {
        loadBridges();
    }

    @Override
    public void onPropertyRemoved(final PropertyRemovedEvent event) {
        loadBridges();
    }


    void onLaunchAddJMSBridgeDialog() {
        final SecurityContext securityContext =
                securityFramework.getSecurityContext(getProxy().getNameToken());

        final ResourceDescription resourceDescription = descriptionRegistry.lookup(JMSBRIDGE_TEMPLATE);
        final DefaultWindow dialog = new DefaultWindow("New JMS Bridge");
        ModelNodeFormBuilder.FormAssets resourceAdapterAssets = new ModelNodeFormBuilder()
                .setCreateMode(true)
                .setConfigOnly()
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext)
                .build();

        resourceAdapterAssets.getForm().setEnabled(true);

        AddResourceDialog addDialog = new AddResourceDialog(resourceAdapterAssets, resourceDescription, new AddResourceDialog.Callback() {
            @Override
            public void onAdd(ModelNode payload) {
                dialog.hide();
                
                final ResourceAddress fqAddress =
                        JMSBRIDGE_TEMPLATE.resolve(statementContext, payload.get(NAME).asString());

                payload.get(OP).set(ADD);
                payload.get(ADDRESS).set(fqAddress);

                dispatcher.execute(new DMRAction(payload), new SimpleCallback<DMRResponse>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        super.onFailure(caught);
                        loadBridges();
                    }

                    @Override
                    public void onSuccess(DMRResponse dmrResponse) {
                        Console.info("Successfully added " + payload.get(NAME).asString());
                        loadBridges();
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
}
