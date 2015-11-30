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
package org.jboss.as.console.client.standalone.deploymentscanner;

import com.google.gwt.core.client.GWT;
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
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.spi.OperationMode;
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

import static org.jboss.as.console.spi.OperationMode.Mode.STANDALONE;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
public class DeploymentScannerPresenter
        extends Presenter<DeploymentScannerPresenter.MyView, DeploymentScannerPresenter.MyProxy> {

    // @formatter:off
    @ProxyCodeSplit
    @OperationMode(STANDALONE)
    @NameToken(NameTokens.DeploymentScanner)
    @SearchIndex(keywords = {"deployment", "scanner"})
    @RequiredResources(resources = {ROOT_ADDRESS, SCANNER_ADDRESS})
    public interface MyProxy extends ProxyPlace<DeploymentScannerPresenter> {}


    public interface MyView extends View, HasPresenter<DeploymentScannerPresenter> {
        void update(List<Property> scanners);
    }
    // @formatter:on


    public static final String ROOT_ADDRESS = "{selected.profile}/subsystem=deployment-scanner";
    public static final String SCANNER_ADDRESS = "{selected.profile}/subsystem=deployment-scanner/scanner=*";
    public static final AddressTemplate ROOT_TEMPLATE = AddressTemplate.of(ROOT_ADDRESS);
    public static final AddressTemplate SCANNER_TEMPLATE = AddressTemplate.of(SCANNER_ADDRESS);

    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final StatementContext statementContext;
    private final SecurityFramework securityFramework;
    private final ResourceDescriptionRegistry resourceDescriptionRegistry;
    private final CrudOperationDelegate crud;

    private DefaultWindow window;
    private AddResourceDialog addResourceDialog;

    @Inject
    public DeploymentScannerPresenter(final EventBus eventBus, final DeploymentScannerPresenter.MyView view,
            final DeploymentScannerPresenter.MyProxy proxy, final RevealStrategy revealStrategy,
            final StatementContext statementContext, final DispatchAsync dispatcher,
            final SecurityFramework securityFramework, final ResourceDescriptionRegistry resourceDescriptionRegistry) {
        super(eventBus, view, proxy);
        this.revealStrategy = revealStrategy;
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
        this.securityFramework = securityFramework;
        this.resourceDescriptionRegistry = resourceDescriptionRegistry;
        this.crud = new CrudOperationDelegate(statementContext, dispatcher);
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        reload();
    }

    private void reload() {
        Operation op = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, ROOT_TEMPLATE.resolve(statementContext))
                .param(CHILD_TYPE, "scanner")
                .build();
        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                Console.error(Console.CONSTANTS.cannotReadDeploymentScanner(), caught.getMessage());
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    Console.error(Console.CONSTANTS.cannotReadDeploymentScanner(), result.getFailureDescription());
                } else {
                    List<Property> scanners = result.get(RESULT).asPropertyList();
                    getView().update(scanners);
                }
            }
        });
    }

    public void launchAddDialog() {
        if (addResourceDialog == null) {
            SecurityContext securityContext = securityFramework.getSecurityContext(getProxy().getNameToken());
            ResourceDescription resourceDescription = resourceDescriptionRegistry.lookup(SCANNER_TEMPLATE);
            addResourceDialog = new AddResourceDialog(securityContext, resourceDescription,
                    new AddResourceDialog.Callback() {
                        @Override
                        public void onAdd(final ModelNode payload) {
                            window.hide();
                            addDeploymentScanner(payload.get(NAME).asString(), payload);
                        }

                        @Override
                        public void onCancel() {
                            window.hide();
                        }
                    });
        } else {
            addResourceDialog.clearValues();
        }

        window = new DefaultWindow("Worker");
        window.setWidth(480);
        window.setHeight(360);
        window.setWidget(addResourceDialog);
        window.setGlassEnabled(true);
        window.center();
    }

    public void addDeploymentScanner(String name, ModelNode payload) {
        crud.onCreateResource(SCANNER_TEMPLATE, name, payload, new CrudOperationDelegate.Callback() {
            @Override
            public void onFailure(final AddressTemplate addressTemplate, final String name, final Throwable t) {
                Console.error(Console.MESSAGES.addingFailed(name), t.getMessage());
            }

            @Override
            public void onSuccess(final AddressTemplate addressTemplate, final String name) {
                reload();
            }
        });
    }

    public void save(String name, Map<String, Object> changedValues) {
        crud.onSaveResource(SCANNER_TEMPLATE, name, changedValues, new CrudOperationDelegate.Callback() {
            @Override
            public void onFailure(final AddressTemplate addressTemplate, final String name, final Throwable t) {
                Console.error(Console.MESSAGES.saveFailed(name), t.getMessage());
            }

            @Override
            public void onSuccess(final AddressTemplate addressTemplate, final String name) {
                reload();
            }
        });
    }

    public void remove(String name) {
        crud.onRemoveResource(SCANNER_TEMPLATE, name, new CrudOperationDelegate.Callback() {
            @Override
            public void onFailure(final AddressTemplate addressTemplate, final String name, final Throwable t) {
                Console.error(Console.MESSAGES.deletionFailed(name), t.getMessage());
            }

            @Override
            public void onSuccess(final AddressTemplate addressTemplate, final String name) {
                reload();
            }
        });
    }
}
