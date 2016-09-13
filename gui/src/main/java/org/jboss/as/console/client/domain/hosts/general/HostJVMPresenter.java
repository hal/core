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

package org.jboss.as.console.client.domain.hosts.general;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.jvm.JvmManagement;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.as.console.spi.OperationMode;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.rbac.SecurityContextChangedEvent;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jboss.as.console.spi.OperationMode.Mode.DOMAIN;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 5/18/11
 */
public class HostJVMPresenter extends Presenter<HostJVMPresenter.MyView, HostJVMPresenter.MyProxy>
        implements JvmManagement {

    static final String ROOT_ADDRESS = "{selected.host}/jvm=*";
    static final AddressTemplate ROOT_ADDRESS_TEMPLATE = AddressTemplate.of(ROOT_ADDRESS);

    @ProxyCodeSplit
    @NameToken(NameTokens.HostJVMPresenter)
    @RequiredResources(resources = ROOT_ADDRESS)
    @OperationMode(DOMAIN)
    @SearchIndex(keywords = {"jvm", "heap", "xmx", "xms", "xss"})
    public interface MyProxy extends ProxyPlace<HostJVMPresenter>, Place {}


    public interface MyView extends View {
        void setPresenter(HostJVMPresenter presenter);
        void updateModel(List<Property> jvms);
    }


    private final DispatchAsync dispatcher;
    private final HostStore hostStore;
    private final CoreGUIContext statementContext;
    private DefaultWindow window;
    private CrudOperationDelegate operationDelegate;
    private SecurityFramework securityFramework;
    private ResourceDescriptionRegistry descriptionRegistry;

    @Inject
    public HostJVMPresenter(EventBus eventBus, MyView view, MyProxy proxy, DispatchAsync dispatcher,
                            HostStore hostStore, CoreGUIContext statementContext, SecurityFramework securityFramework,
                            ResourceDescriptionRegistry descriptionRegistry) {

        super(eventBus, view, proxy);
        this.dispatcher = dispatcher;
        this.hostStore = hostStore;
        this.statementContext = statementContext;
        this.operationDelegate = new CrudOperationDelegate(statementContext, dispatcher);
        this.securityFramework = securityFramework;
        this.descriptionRegistry = descriptionRegistry;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        hostStore.addChangeHandler(action -> {
            if (isVisible()) {
                loadModel();
            }
        });
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
    public void prepareFromRequest(PlaceRequest request) {
        SecurityContextChangedEvent.AddressResolver resolver = new SecurityContextChangedEvent.AddressResolver<AddressTemplate>() {
            @Override
            public String resolve(AddressTemplate template) {
                return template.resolveAsKey(statementContext);
            }
        };

        Command cmd = () -> getProxy().manualReveal(HostJVMPresenter.this);

        // RBAC: context change propagation
        SecurityContextChangedEvent.fire(
                HostJVMPresenter.this,
                cmd,
                resolver
        );
    }

    @Override
    protected void onReset() {
        super.onReset();
        Scheduler.get().scheduleDeferred(this::loadModel);
    }

    @Override
    public void onCreateJvm(String reference, ModelNode jvm) {
        String name = jvm.get(NAME).asString();
        operationDelegate.onCreateResource(ROOT_ADDRESS_TEMPLATE, name, jvm, new CrudOperationDelegate.Callback() {
            @Override
            public void onSuccess(AddressTemplate addressTemplate, String name) {
                Console.info(Console.MESSAGES.added("JVM Configuration"));
                loadModel();
            }

            @Override
            public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
                Console.info(Console.MESSAGES.addingFailed("JVM Configuration"));
            }
        });
    }

    private void loadModel() {

        getView().updateModel(Collections.emptyList());

        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(ADDRESS).add("host", hostStore.getSelectedHost());
        operation.get(CHILD_TYPE).set(JVM);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("JVM Configurations"), response.getFailureDescription());
                } else {
                    getView().updateModel(response.get(RESULT).asPropertyList());
                }
            }
        });
    }

    @Override
    public void onDeleteJvm(String reference, String name) {
        if (name.equals("default")) {
            Console.error(Console.MESSAGES.deletionFailed("JVM Configurations"),
                    Console.CONSTANTS.hosts_jvm_err_deleteDefault());
            return;
        }

        operationDelegate.onRemoveResource(ROOT_ADDRESS_TEMPLATE, name, new CrudOperationDelegate.Callback() {
            @Override
            public void onSuccess(AddressTemplate addressTemplate, String name) {
                Console.info(Console.MESSAGES.deleted("JVM Configuration"));
                loadModel();
            }

            @Override
            public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
                Console.error(Console.MESSAGES.deletionFailed("JVM Configurations"), t.getMessage());
            }
        });
    }

    @Override
    public void onUpdateJvm(String reference, String jvmName, Map<String, Object> changedValues) {
        operationDelegate.onSaveResource(ROOT_ADDRESS_TEMPLATE, jvmName, changedValues,
                new CrudOperationDelegate.Callback() {
                    @Override
                    public void onSuccess(AddressTemplate addressTemplate, String name) {
                        Console.info(Console.MESSAGES.modified("JVM Configuration"));
                        loadModel();
                    }

                    @Override
                    public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
                        Console.error(Console.MESSAGES.modificationFailed("JVM Configuration"), t.getMessage());
                    }
                });
    }

    public void launchNewJVMDialogue() {
        SecurityContext securityContext = securityFramework.getSecurityContext(getProxy().getNameToken());
        ResourceDescription resourceDescription = descriptionRegistry.lookup(ROOT_ADDRESS_TEMPLATE);

        ModelNode defaults = new ModelNode();
        defaults.get("heap-size").set("64m");
        defaults.get("max-heap-size").set("256m");

        ModelNodeFormBuilder.FormAssets formAssets = new ModelNodeFormBuilder()
                .setCreateMode(true)
                .setResourceDescription(resourceDescription)
                .setRequiredOnly(true)
                .setSecurityContext(securityContext).build();
        formAssets.getForm().setEnabled(true);
        formAssets.getForm().editTransient(defaults);

        window = new DefaultWindow(Console.MESSAGES.createTitle("JVM Configuration"));
        window.setWidth(480);
        window.setHeight(360);

        window.trapWidget(
                new AddResourceDialog(formAssets, resourceDescription, new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        window.hide();
                        onCreateJvm("", payload);
                    }

                    @Override
                    public void onCancel() {
                        window.hide();
                    }
                }).asWidget()
        );

        window.setGlassEnabled(true);
        window.center();
    }

    public void closeDialogue() {
        if (window != null && window.isShowing()) { window.hide(); }
    }
}
