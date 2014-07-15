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
package org.jboss.as.console.client.administration.role.operation;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.administration.role.RoleAssignmentPresenter;
import org.jboss.as.console.client.administration.role.model.ModelHelper;
import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.Principals;
import org.jboss.as.console.client.administration.role.model.Role;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.administration.role.model.RoleAssignments;
import org.jboss.as.console.client.administration.role.model.Roles;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.domain.model.Host;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.ServerGroupStore;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.StandardRole;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.jboss.as.console.client.administration.role.model.Role.Type.HOST;
import static org.jboss.as.console.client.administration.role.model.Role.Type.SERVER_GROUP;
import static org.jboss.as.console.client.administration.role.model.RoleAssignment.PrincipalRealmTupel;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * Loads scoped roles, reads the role mappings and extract principals. After that loads the
 * hosts and server groups.
 *
 * @author Harald Pehl
 */
public class LoadRoleAssignmentsOp implements ManagementOperation<FunctionContext> {

    public final static String PRINCIPALS = "LoadRoleAssignmentsOp.principals";
    public final static String ASSIGNMENTS = "LoadRoleAssignmentsOp.assignments";
    public final static String ROLES = "LoadRoleAssignmentsOp.roles";
    public final static String HOSTS = "LoadRoleAssignmentsOp.hosts";
    public final static String SERVER_GROUPS = "LoadRoleAssignmentsOp.serverGroups";
    public final static String ACCESS_CONTROL_PROVIDER = "LoadRoleAssignmentsOp.accessControlProvider";
    private final RoleAssignmentPresenter presenter;
    private final DispatchAsync dispatcher;
    private final HostInformationStore hostInformationStore;
    private final ServerGroupStore serverGroupStore;
    private boolean pending;

    public LoadRoleAssignmentsOp(final RoleAssignmentPresenter presenter, final DispatchAsync dispatcher,
            final HostInformationStore hostInformationStore, ServerGroupStore serverGroupStore) {
        this.presenter = presenter;
        this.dispatcher = dispatcher;
        this.hostInformationStore = hostInformationStore;
        this.serverGroupStore = serverGroupStore;
    }

    @Override
    public void execute(final Outcome<FunctionContext> outcome) {
        pending = true;
        final FunctionContext context = new FunctionContext();
        new Async<FunctionContext>(Footer.PROGRESS_ELEMENT).waterfall(context, outcome, new RolesAndMappingFunction(),
                new HostsFunction(), new ServerGroupsFunction());
    }

    public boolean isPending() {
        return pending;
    }

    class RolesAndMappingFunction implements Function<FunctionContext> {

        @Override
        public void execute(final Control<FunctionContext> control) {
            ModelNode operation = new ModelNode();
            operation.get(ADDRESS).setEmptyList();
            operation.get(OP).set(COMPOSITE);
            List<ModelNode> steps = new LinkedList<ModelNode>();

            // standard role names
            ModelNode standardRoles = new ModelNode();
            standardRoles.get(OP).set(READ_ATTRIBUTE_OPERATION);
            standardRoles.get(NAME).set("standard-role-names");
            standardRoles.get(ADDRESS).add("core-service", "management").add("access", "authorization");
            steps.add(standardRoles);

            if (!presenter.isStandalone()) {
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
            }

            ModelNode mappingOp = new ModelNode();
            mappingOp.get(ADDRESS).add("core-service", "management").add("access", "authorization");
            mappingOp.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
            mappingOp.get(CHILD_TYPE).set("role-mapping");
            mappingOp.get("recursive-depth").set("2");
            steps.add(mappingOp);

            if (!presenter.isInitialized()) {
                ModelNode accessControlProviderOp = new ModelNode();
                accessControlProviderOp.get(ADDRESS).add("core-service", "management").add("access", "authorization");
                accessControlProviderOp.get(OP).set(READ_ATTRIBUTE_OPERATION);
                accessControlProviderOp.get(NAME).set("provider");
                steps.add(accessControlProviderOp);
            }

            operation.get(STEPS).set(steps);
            dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
                @Override
                public void onSuccess(final DMRResponse response) {
                    Principals principals = new Principals();
                    RoleAssignments assignments = new RoleAssignments();
                    Roles roles = new Roles();

                    ModelNode result = response.get();
                    if (result.isFailure()) {
                        control.getContext().setError(
                                new RuntimeException("Failed to load contents: " + result.getFailureDescription()));
                        control.abort();
                    } else {
                        ModelNode stepsResult = result.get(RESULT);

                        // the order of processing is important!
                        ModelNode standardRoleNames = stepsResult.get("step-1");
                        if (standardRoleNames.get(RESULT).isDefined()) {
                            for (ModelNode node : standardRoleNames.get(RESULT).asList()) {
                                StandardRole.add(node.asString());
                            }
                            for (StandardRole standardRole : StandardRole.values()) {
                                roles.add(new Role(standardRole));
                            }
                        }

                        if (!presenter.isStandalone()) {
                            List<ModelNode> hostScopedRoles = stepsResult.get("step-2").get(RESULT).asList();
                            for (ModelNode node : hostScopedRoles) {
                                addScopedRole(roles, node.asProperty(), "hosts", HOST);
                            }

                            List<ModelNode> serverGroupScopedRoles = stepsResult.get("step-3").get(RESULT).asList();
                            for (ModelNode node : serverGroupScopedRoles) {
                                addScopedRole(roles, node.asProperty(), "server-groups", SERVER_GROUP);
                            }
                        }
                        List<ModelNode> roleMappings =
                                stepsResult.get(presenter.isStandalone() ? "step-2" : "step-4").get(RESULT)
                                        .asList();
                        for (ModelNode node : roleMappings) {
                            addInternalRoleAssignment(principals, assignments, roles, node.asProperty());
                        }
                        // All entities are read - now transform the role assignments from the management model to
                        // role assignments used in the UI
                        assignments.toUI(principals);

                        if (!presenter.isInitialized()) {
                            String provider = "undefined";
                            String step = presenter.isStandalone() ? "step-3" : "step-5";
                            ModelNode providerNode = stepsResult.get(step);
                            if (!providerNode.isFailure()) {
                                provider = providerNode.get(RESULT).asString();
                            }
                            control.getContext().set(ACCESS_CONTROL_PROVIDER, provider);
                        }

                        control.getContext().set(PRINCIPALS, principals);
                        control.getContext().set(ASSIGNMENTS, assignments);
                        control.getContext().set(ROLES, roles);
                        control.proceed();
                    }
                }

                @Override
                public void onFailure(final Throwable caught) {
                    control.getContext().setError(caught);
                    control.abort();
                }
            });
        }

        private void addScopedRole(final Roles roles, final Property property, final String scopeName,
                final Role.Type type) {
            ModelNode node = property.getValue();
            String baseRoleName = node.get("base-role").asString();
            List<String> scope = new ArrayList<String>();
            List<ModelNode> scopeNodes = node.get(scopeName).asList();
            for (ModelNode scopeNode : scopeNodes) {
                scope.add(scopeNode.asString());
            }
            // Use matchId here since the configuration might contain roles in mixed / lower / upper case
            Role scopedRole = new Role(property.getName(), property.getName(), StandardRole.matchId(baseRoleName),
                    type, scope);
            roles.add(scopedRole);
        }

        private void addInternalRoleAssignment(final Principals principals, final RoleAssignments assignments,
                final Roles roles, final Property property) {
            String roleId = property.getName();
            Role role = roles.getRole(roleId);
            if (role != null) {
                RoleAssignment.Internal internal = new RoleAssignment.Internal(role);
                ModelNode assignmentNode = property.getValue();
                if (assignmentNode.hasDefined("include-all")) {
                    role.setIncludeAll(assignmentNode.get("include-all").asBoolean());
                }
                if (assignmentNode.hasDefined("include")) {
                    List<Property> inclusions = assignmentNode.get("include").asPropertyList();
                    for (Property inclusion : inclusions) {
                        PrincipalRealmTupel principal = mapPrincipal(principals, inclusion.getName(),
                                inclusion.getValue());
                        if (principal != null) {
                            internal.include(principal);
                        }
                    }
                }
                if (assignmentNode.hasDefined("exclude")) {
                    List<Property> exclusions = assignmentNode.get("exclude").asPropertyList();
                    for (Property exclusion : exclusions) {
                        PrincipalRealmTupel principal = mapPrincipal(principals, exclusion.getName(),
                                exclusion.getValue());
                        if (principal != null) {
                            internal.exclude(principal);
                        }
                    }
                }
                assignments.add(internal);
            }
        }

        private PrincipalRealmTupel mapPrincipal(final Principals principals, final String id, final ModelNode node) {
            String name = node.get("name").asString();
            if (ModelHelper.LOCAL_USERNAME.equals(name)) {
                // Skip the local user
                return null;
            }

            Principal.Type type = Principal.Type.valueOf(node.get("type").asString());
            Principal principal = new Principal(id, name, type);
            principals.add(principal);

            String realm = null;
            if (node.hasDefined("realm")) {
                realm = node.get("realm").asString();
            }

            return new PrincipalRealmTupel(principal, realm);
        }
    }

    class HostsFunction implements Function<FunctionContext> {

        @Override
        public void execute(final Control<FunctionContext> control) {
            if (presenter.isStandalone()) {
                control.getContext().set(HOSTS, Collections.emptyList());
                control.proceed();
            } else {
                hostInformationStore.getHosts(new AsyncCallback<List<Host>>() {
                    @Override
                    public void onSuccess(final List<Host> result) {
                        List<String> hosts = new ArrayList<String>();
                        for (Host host : result) {
                            hosts.add(host.getName());
                        }
                        control.getContext().set(HOSTS, hosts);
                        control.proceed();
                    }

                    @Override
                    public void onFailure(final Throwable caught) {
                        control.getContext().setError(caught);
                        control.abort();
                    }
                });
            }
        }
    }

    class ServerGroupsFunction implements Function<FunctionContext> {

        @Override
        public void execute(final Control<FunctionContext> control) {
            if (presenter.isStandalone()) {
                control.getContext().set(SERVER_GROUPS, Collections.emptyList());
                control.proceed();
                finish();
            } else {
                serverGroupStore.loadServerGroups(new AsyncCallback<List<ServerGroupRecord>>() {
                    @Override
                    public void onSuccess(final List<ServerGroupRecord> result) {
                        List<String> serverGroups = new ArrayList<String>();
                        for (ServerGroupRecord serverGroup : result) {
                            serverGroups.add(serverGroup.getName());
                        }
                        control.getContext().set(SERVER_GROUPS, serverGroups);
                        control.proceed();
                        finish();
                    }

                    @Override
                    public void onFailure(final Throwable caught) {
                        control.getContext().setError(caught);
                        control.abort();
                        finish();
                    }
                });
            }
        }

        private void finish() {
            pending = false;
        }
    }
}
