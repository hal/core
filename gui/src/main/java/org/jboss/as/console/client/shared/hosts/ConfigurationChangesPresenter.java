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

package org.jboss.as.console.client.shared.hosts;

import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.CircuitPresenter;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.ballroom.client.rbac.SecurityContextChangedEvent;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.NumberBoxItem;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Claudio Miranda
 */
public class ConfigurationChangesPresenter
        extends CircuitPresenter<ConfigurationChangesPresenter.MyView, ConfigurationChangesPresenter.MyProxy> {

    public static final String CONFIGURATION_CHANGES_ADDRESS = "/{selected.host}/core-service=management/service=configuration-changes";
    public static final AddressTemplate CONFIGURATION_CHANGES_TEMPLATE = AddressTemplate.of(CONFIGURATION_CHANGES_ADDRESS);

    @ProxyCodeSplit
    @NameToken(NameTokens.ConfigurationChangesPresenter)
    @RequiredResources(resources = {CONFIGURATION_CHANGES_ADDRESS})
    public interface MyProxy extends ProxyPlace<ConfigurationChangesPresenter>, Place {}


    public interface MyView extends View {
        void setPresenter(ConfigurationChangesPresenter presenter);
        void setChanges(List<ModelNode> changes);
        void setEnabled(boolean enabled);
    }

    private final DispatchAsync dispatcher;
    private final HostStore hostStore;
    private final CoreGUIContext statementContext;
    private final CrudOperationDelegate operationDelegate;
    private ResourceDescriptionRegistry descriptionRegistry;
    
    private DefaultWindow window;

    @Inject
    public ConfigurationChangesPresenter(EventBus eventBus, MyView view, MyProxy proxy, Dispatcher circuit,
            DispatchAsync dispatcher, HostStore hostStore, CoreGUIContext statementContext,
            ResourceDescriptionRegistry descriptionRegistry) {
        super(eventBus, view, proxy, circuit);

        this.dispatcher = dispatcher;
        this.hostStore = hostStore;
        this.descriptionRegistry = descriptionRegistry;
        this.statementContext = statementContext;
        this.operationDelegate = new CrudOperationDelegate(this.statementContext, dispatcher);
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        addChangeHandler(hostStore);

    }

    @Override
    protected void onAction(Action action) {
        Scheduler.get().scheduleDeferred(this::initialLoad);
    }

    @Override
    public boolean useManualReveal() {
        return true;
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_MainContent, this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        Scheduler.get().scheduleDeferred(this::initialLoad);
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {
        SecurityContextChangedEvent.AddressResolver resolver = new SecurityContextChangedEvent.AddressResolver<AddressTemplate>() {
            @Override
            public String resolve(AddressTemplate template) {
                String resolved = template.resolveAsKey(statementContext);
                return resolved;
            }
        };

        Command cmd = () -> getProxy().manualReveal(ConfigurationChangesPresenter.this);

        // RBAC: context change propagation
        SecurityContextChangedEvent.fire(
                ConfigurationChangesPresenter.this,
                cmd,
                resolver
        );
    }

    public void loadChanges() {
        Operation operation = new Operation.Builder(LIST_CHANGES_OPERATION,
                CONFIGURATION_CHANGES_TEMPLATE.resolve(Console.MODULES.getCoreGUIContext()))
                .build();

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onFailure(Throwable caught) {
                Console.error(Console.MESSAGES.failed("Configuration Management"), caught.getMessage());
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                List<ModelNode> payload = response.get(RESULT).asList();
                getView().setChanges(payload);
            }
        });
    }

    // at first check to see if the configuration-changes resource exists, 
    // if positive, then call :list-changes
    private void initialLoad() {
        AddressTemplate coreServiceManagement = CONFIGURATION_CHANGES_TEMPLATE.subTemplate(0, 2);
        
        Operation operation1 = new Operation.Builder(READ_CHILDREN_NAMES_OPERATION,
                coreServiceManagement.resolve(statementContext))
                .param(CHILD_TYPE, SERVICE)
                .build();

        dispatcher.execute(new DMRAction(operation1), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if(response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Configuration Management"), response.getFailureDescription());
                } else {
                    List<ModelNode> payload = response.get(RESULT).asList();
                    
                    boolean enabled = false;
                    for (ModelNode service: payload) {
                        if (CONFIGURATION_CHANGES.equals(service.asString())) {
                            enabled = true;
                            break;
                        }
                    }
                    if (enabled) {
                        loadChanges();
                    }
                    getView().setEnabled(enabled);
                }
            }
        });
    }
    
    public void enable() {

        window = new DefaultWindow("Configuration Changes");
        window.setWidth(480);
        window.setHeight(360);

        ResourceDescription resoourceDescription = descriptionRegistry.lookup(CONFIGURATION_CHANGES_TEMPLATE);

        FormItem maxHistorySizeItem = new NumberBoxItem("max-history", "Max history");
        maxHistorySizeItem.setRequired(false);

        // a modelnodeformbuilder.formassets is used to pass as parameter to AddResourceDialog 
        ModelNodeFormBuilder builder = new ModelNodeFormBuilder()
                .setResourceDescription(resoourceDescription)
                .setSecurityContext(Console.MODULES.getSecurityFramework().getSecurityContext(NameTokens.ConfigurationChangesPresenter));
        ModelNodeFormBuilder.FormAssets formAssets = builder.build();
        ModelNodeForm form = formAssets.getForm();
        form.setFields(maxHistorySizeItem);
        form.setEnabled(true);

        AddResourceDialog addResourceDialog = new AddResourceDialog(formAssets,
                resoourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        window.hide();
                        operationDelegate.onCreateResource(CONFIGURATION_CHANGES_TEMPLATE, payload.get(NAME).asString(), 
                                payload, 
                                new CrudOperationDelegate.Callback() {
                            @Override
                            public void onSuccess(AddressTemplate address, String name) {

                                Console.info(Console.MESSAGES.assignmentSuccessfullyEnabled("Configuration changes"));
                                getView().setEnabled(true);
                                loadChanges();
                            }

                            @Override
                            public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
                                Console.error(Console.MESSAGES.failedToModifyResource(addressTemplate.toString()), 
                                        t.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onCancel() {
                        window.hide();
                    }
                }
        );
        window.setWidget(addResourceDialog);
        window.setGlassEnabled(true);
        window.center();
    }

    public void disable() {
        
        Operation operation1 = new Operation.Builder(REMOVE,
                CONFIGURATION_CHANGES_TEMPLATE.resolve(statementContext))
                .build();

        dispatcher.execute(new DMRAction(operation1), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Configuration Management"), response.getFailureDescription());
                } else {
                    getView().setEnabled(false);
                }
            }
        });
    }

}
