/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.jboss.as.console.client.administration.role;

import static org.jboss.as.console.client.administration.role.model.Principal.Type.GROUP;
import static org.jboss.as.console.client.administration.role.model.Principal.Type.USER;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.PrincipalStore;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.administration.role.model.RoleAssignmentStore;
import org.jboss.as.console.client.administration.role.model.RoleStore;
import org.jboss.as.console.client.administration.role.model.ScopedRole;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.message.Message;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.Role;
import org.jboss.as.console.client.rbac.StandardRole;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.model.ModelAdapter;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Outcome;

/**
 * There are some constraints when managing role assignments in the console:
 * <ol>
 * <li>There has to be at least one inclusion for the role assignment</li>
 * <li>An exclusion can only contain users excluded from a group</li>
 * </ol>
 * <p>Role assignment which do not met these constraints, won't be visible in the conole and have to be
 * managed using other tools (e.g. the CLI)</p>
 *
 * @author Harald Pehl
 */
public class RoleAssignmentPresenter
        extends Presenter<RoleAssignmentPresenter.MyView, RoleAssignmentPresenter.MyProxy> {

    private final DispatchAsync dispatcher;
    private final RevealStrategy revealStrategy;
    private final BeanFactory beanFactory;
    private final PrincipalStore principals;
    private final RoleAssignmentStore assignments;
    private final RoleStore roles;
    private DefaultWindow window;


    @Inject
    public RoleAssignmentPresenter(final EventBus eventBus, final MyView view, final MyProxy proxy,
            final DispatchAsync dispatcher, final RevealStrategy revealStrategy, final BeanFactory beanFactory) {
        super(eventBus, view, proxy);

        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
        this.beanFactory = beanFactory;
        this.principals = new PrincipalStore();
        this.assignments = new RoleAssignmentStore(beanFactory);
        this.roles = new RoleStore();
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

        principals.clear();
        assignments.clear();
        roles.clear();

        loadRolesAndMapping();
    }

    private void loadRolesAndMapping() {
        // load scoped roles (hosts and server groups)
        // load role mappings
        // extract principals
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).setEmptyList();
        operation.get(OP).set(COMPOSITE);
        List<ModelNode> steps = new LinkedList<ModelNode>();

        ModelNode hostScopeOp = new ModelNode();
        hostScopeOp.get(ADDRESS).add("core-service", "management").add("access", "authorization");
        hostScopeOp.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        hostScopeOp.get(CHILD_TYPE).set("host-scoped-role");
        steps.add(hostScopeOp);

        ModelNode serverGroupScopeOp = new ModelNode();
        serverGroupScopeOp.get(ADDRESS).add("core-service", "management").add("access", "authorization");
        serverGroupScopeOp.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        serverGroupScopeOp.get(CHILD_TYPE).set("server-group-scoped-role");
        steps.add(serverGroupScopeOp);

        ModelNode mappingOp = new ModelNode();
        mappingOp.get(ADDRESS).add("core-service", "management").add("access", "authorization");
        mappingOp.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        mappingOp.get(CHILD_TYPE).set("role-mapping");
        mappingOp.get("recursive-depth").set("2");
        steps.add(mappingOp);

        operation.get(STEPS).set(steps);
        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(final DMRResponse result) {
                ModelNode response = result.get();
                if (ModelAdapter.wasSuccess(response)) {
                    ModelNode stepsResult = response.get(RESULT);

                    // the order of processing is important!
                    List<ModelNode> hostScopedRoles = stepsResult.get("step-1").get(RESULT).asList();
                    for (ModelNode node : hostScopedRoles) {
                        addScopedRole(node.asProperty(), "hosts", ScopedRole.Type.HOST);
                    }
                    List<ModelNode> serverGroupScopedRoles = stepsResult.get("step-2").get(RESULT).asList();
                    for (ModelNode node : serverGroupScopedRoles) {
                        addScopedRole(node.asProperty(), "server-groups", ScopedRole.Type.SERVER_GROUP);
                    }
                    List<ModelNode> roleMappings = stepsResult.get("step-3").get(RESULT).asList();
                    for (ModelNode node : roleMappings) {
                        addManagementModelRoleAssignment(node.asProperty());
                    }
                    // All entities are read - now transform the role assignements from the management model to
                    // role assignments used in the UI and update the view
                    assignments.transform(principals);
                    getView().update(assignments, roles);
                }
            }
        });
    }

    private void addScopedRole(final Property property, final String scopeName, final ScopedRole.Type type) {
        ScopedRole scopedRole = beanFactory.scopedRole().as();
        scopedRole.setName(property.getName());
        scopedRole.setType(type);
        ModelNode node = property.getValue();
        String baseRoleName = node.get("base-role").asString();
        scopedRole.setBaseRole(StandardRole.valueOf(baseRoleName));

        List<String> scope = new ArrayList<String>();
        List<ModelNode> scopeNodes = node.get(scopeName).asList();
        for (ModelNode scopeNode : scopeNodes) {
            scope.add(scopeNode.asString());
        }
        scopedRole.setScope(scope);
        roles.add(scopedRole);
    }

    private void addManagementModelRoleAssignment(final Property property) {
        String roleName = property.getName();
        Role role = roles.getRole(roleName);
        if (role != null) {
            RoleAssignment.ManagementModel managementModel = new RoleAssignment.ManagementModel(role);
            ModelNode assignmentNode = property.getValue();
            if (assignmentNode.hasDefined("include")) {
                List<Property> inclusions = assignmentNode.get("include").asPropertyList();
                for (Property inclusion : inclusions) {
                    Principal principal = mapPrincipal(inclusion.getValue());
                    managementModel.include(principal);
                }
            } else {
                return; // no empty inclusions (see constraints)!
            }
            if (assignmentNode.hasDefined("exclude")) {
                List<Property> exclusions = assignmentNode.get("exclude").asPropertyList();
                for (Property exclusion : exclusions) {
                    Principal principal = mapPrincipal(exclusion.getValue());
                    managementModel.exclude(principal);
                }
            }

            // check other constraints
            for (Principal principal : managementModel.getExcludes()) {
                if (principal.getType() == GROUP) {
                    return;
                }
            }
            assignments.add(managementModel);
        }
    }

    private Principal mapPrincipal(final ModelNode node) {
        Principal principal = beanFactory.principal().as();
        principal.setName(node.get("name").asString());
        if (node.hasDefined("realm")) {
            principal.setRealm(node.get("realm").asString());
        }
        Principal.Type type = Principal.Type.valueOf(node.get("type").asString());
        principal.setType(type);
        principals.add(principal);
        return principal;
    }

    public void launchAddDialg(final StandardRole role, final RoleAssignment roleAssignment,
            final Principal.Type principalType) {
        String title = principalType == USER ? Console.CONSTANTS.role_assignment_add_user() : Console
                .CONSTANTS.role_assignment_add_group();
        window = new DefaultWindow(title);
        window.setWidth(480);
        window.setHeight(300);
        window.trapWidget(new AddPrincipalWizard(this, role, roleAssignment, principalType).asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void onAdd(final StandardRole role, final RoleAssignment roleAssignment, final Principal principal) {
        closeDialog();
        //        System.out.println("About to add " + principal.getType() + " " + principal
        //                .getName() + " to role " + role + " / " + (roleAssignment.isInclude() ? "includes" : "exludes"));

        AddPrincipalOperation addPrincipalOperation = new AddPrincipalOperation(dispatcher);
        addPrincipalOperation.extecute(role, roleAssignment, principal, new Outcome<Stack<Boolean>>() {
            @Override
            public void onFailure(final Stack<Boolean> context) {
                // TODO Error handling
                Console.MODULES.getMessageCenter().notify(new Message("Cannot add principal", Message.Severity.Error));
            }

            @Override
            public void onSuccess(final Stack<Boolean> context) {
                //                getView().reset();
            }
        });
    }

    public void onDelete(final StandardRole role, final RoleAssignment roleAssignment, final Principal principal) {
        //        System.out.println("About to delete " + principal.getType() + " " + principal
        //                .getName() + " from role " + role + " / " + (roleAssignment.isInclude() ? "includes" : "exludes"));

        final ModelNode operation = new ModelNode();
        StringBuilder principalKey = new StringBuilder();
        boolean realmGiven = principal.getRealm() != null && principal.getRealm().length() != 0;
        principalKey.append(principal.getType().name().toLowerCase()).append("-").append(principal.getName());
        if (realmGiven) {
            principalKey.append("@").append(principal.getRealm());
        }
        operation.get(ADDRESS).add("core-service", "management");
        operation.get(ADDRESS).add("access", "authorization");
        operation.get(ADDRESS).add("role-mapping", role.name());
        //        operation.get(ADDRESS).add(roleAssignment.isInclude() ? "include" : "exclude", principalKey.toString());
        operation.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse response) {
                //                getView().reset();
            }
        });
    }

    public void closeDialog() {
        if (window != null) {
            window.hide();
        }
    }

    @ProxyCodeSplit
    @NameToken(NameTokens.RoleAssignmentPresenter)
    @AccessControl(resources = {"/core-service=management/access=authorization"})
    public interface MyProxy extends Proxy<RoleAssignmentPresenter>, Place {
    }

    public interface MyView extends View {

        void setPresenter(RoleAssignmentPresenter presenter);

        void update(RoleAssignmentStore assignments, RoleStore roles);
    }
}
