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
package org.jboss.as.console.client.administration.authorization;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.ModelHelper;
import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.Role;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.StandardRole;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.behaviour.CrudOperationDelegate;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.jboss.as.console.client.rbac.StandardRole.ADMINISTRATOR;
import static org.jboss.as.console.client.rbac.StandardRole.SUPER_USER;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * TODO Move all code which acts on "/core-service=management/access=authorization" in a common store
 *
 * @author Harald Pehl
 */
public class AuthorizationPresenter extends Presenter<AuthorizationPresenter.MyView, AuthorizationPresenter.MyProxy> {

    @ProxyCodeSplit
    @NameToken(NameTokens.AuthorizationPresenter)
    @AccessControl(resources = AUTHORIZATION_ADDRESS, recursive = false)
    public interface MyProxy extends Proxy<AuthorizationPresenter>, Place {
    }


    public interface MyView extends View, HasPresenter<AuthorizationPresenter> {
        void updateFrom(ModelNode data);

        ModelNode currentValues();
    }


    public final static String AUTHORIZATION_ADDRESS = "/core-service=management/access=authorization";

    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final CrudOperationDelegate operationDelegate;
    private DefaultWindow window;

    @Inject
    public AuthorizationPresenter(EventBus eventBus, MyView view, MyProxy proxy, RevealStrategy revealStrategy,
                                  DispatchAsync dispatcher, CoreGUIContext statementContext) {
        super(eventBus, view, proxy);

        this.revealStrategy = revealStrategy;
        this.dispatcher = dispatcher;
        this.operationDelegate = new CrudOperationDelegate(statementContext, dispatcher);
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInAdministration(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadAuthorization();
    }

    private void loadAuthorization() {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).add("core-service", "management");
        operation.get(ADDRESS).add("access", "authorization");
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(OP).set(READ_RESOURCE_OPERATION);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Loading authorization resource"), response.getFailureDescription());
                } else {
                    ModelNode data = response.get(RESULT).asObject();
                    List<ModelNode> standardRoles = data.get("standard-role-names").asList();
                    for (ModelNode node : standardRoles) {
                        StandardRole.add(node.asString());
                    }
                    getView().updateFrom(data);
                }
            }
        });
    }

    public void onSave(final Map<String, Object> changedValues) {
        String oldProvider = getView().currentValues().get("provider").asString();
        String newProvider = changedValues.get("provider").toString();
        final boolean providerChanged = !oldProvider.equals(newProvider);
        if ("simple".equals(oldProvider) && "rbac".equals(newProvider)) {
            // check admin roles
            verifyAdminRoles(new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {
                    openWindow("No Suitable Roles Found", 480, 220,
                            new NoAdminRolesDialog(AuthorizationPresenter.this, changedValues, providerChanged).asWidget());
                }

                @Override
                public void onSuccess(Void result) {
                    saveInternal(changedValues, providerChanged);
                }
            });
        } else {
            saveInternal(changedValues, providerChanged);
        }
    }

    void saveInternal(final Map<String, Object> changedValues, final boolean providerChanged) {
        operationDelegate.onSaveResource(AUTHORIZATION_ADDRESS, null, changedValues, new CrudOperationDelegate.Callback() {
            @Override
            public void onSuccess(ResourceAddress address, String name) {
                if (providerChanged) {
                    // TODO Ask user to restart *all* hosts in order to complete provider switch
                    // If user doesn't want to restart hosts, make clear that the console might become unusable.
                } else {
                    loadAuthorization();
                }
            }

            @Override
            public void onFailure(ResourceAddress address, String name, Throwable t) {
                Console.error("Unable to change authentication properties", t.getMessage());
            }
        });
    }

    private void verifyAdminRoles(final AsyncCallback<Void> callback) {
        // TODO Move this into a store
        ModelNode suRole = ModelHelper.roleMapping(new Role(StandardRole.fromId(SUPER_USER)));
        suRole.get("recursive-depth").set("2");
        suRole.get(OP).set(READ_RESOURCE_OPERATION);
        ModelNode adminRole = ModelHelper.roleMapping(new Role(StandardRole.fromId(ADMINISTRATOR)));
        adminRole.get("recursive-depth").set("2");
        adminRole.get(OP).set(READ_RESOURCE_OPERATION);

        ModelNode operation = new ModelNode();
        operation.get(OP).set(COMPOSITE);
        operation.get(ADDRESS).setEmptyList();
        List<ModelNode> steps = new LinkedList<ModelNode>();
        steps.add(suRole);
        steps.add(adminRole);
        operation.get(STEPS).set(steps);

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(DMRResponse result) {
                boolean adminRoles = false;
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Unable to verify admin roles"), response.getFailureDescription());
                } else {
                    ModelNode compResult = response.get(RESULT);
                    adminRoles = hasUsers(compResult.get("step-1")) || hasUsers(compResult.get("step-2"));
                }
                if (adminRoles) {
                    callback.onSuccess(null);
                } else {
                    callback.onFailure(new RuntimeException());
                }
            }

            private boolean hasUsers(ModelNode roleNode) {
                boolean hasUsers = false;
                if (!roleNode.isFailure()) {
                    ModelNode payload = roleNode.get(RESULT);
                    if (payload.hasDefined("include")) {
                        List<Property> inclusions = payload.get("include").asPropertyList();
                        for (Property inclusion : inclusions) {
                            ModelNode principalNode = inclusion.getValue();
                            String principalName = principalNode.get("name").asString();
                            if (ModelHelper.LOCAL_USERNAME.equals(principalName)) {
                                continue;
                            }
                            Principal.Type type = Principal.Type.valueOf(principalNode.get("type").asString());
                            if (type == Principal.Type.USER) {
                                hasUsers = true;
                                break;
                            }
                        }
                    }
                }
                return hasUsers;
            }
        });
    }

    public void openWindow(final String title, final int width, final int height, final Widget content) {
        closeWindow();
        window = new DefaultWindow(title);
        window.setWidth(width);
        window.setHeight(height);
        window.trapWidget(content);
        window.setGlassEnabled(true);
        window.center();
    }

    public void closeWindow() {
        if (window != null) {
            window.hide();
        }
    }
}
