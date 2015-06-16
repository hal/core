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
package org.jboss.as.console.client.administration.accesscontrol.store;

import com.allen_sauer.gwt.log.client.Log;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.rbac.StandardRole;
import org.jboss.as.console.client.shared.flow.FunctionCallback;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.v3.dmr.Composite;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher.Channel;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * Store that holds all management ops for the access control resources.
 *
 * @author Harald Pehl
 */
@Store
public class AccessControlStore extends ChangeSupport {

    /**
     * Flow outcome which calls {@link #reload(ReloadAccessControl, Channel)} if successful.
     */
    class ReloadOutcome implements Outcome<FunctionContext> {

        private final Channel channel;

        ReloadOutcome(final Channel channel) {this.channel = channel;}

        @Override
        public void onFailure(final FunctionContext context) {
            channel.nack(context.getError());
        }

        @Override
        public void onSuccess(final FunctionContext context) {
            reload(new ReloadAccessControl(), channel);
        }
    }


    /**
     * Async callback which calls {@link #reload(ReloadAccessControl, Channel)} if successful.
     */
    class ReloadCallback implements AsyncCallback<DMRResponse> {

        private final Channel channel;

        ReloadCallback(final Channel channel) {this.channel = channel;}

        @Override
        public void onFailure(final Throwable caught) {
            channel.nack(caught);
        }

        @Override
        public void onSuccess(final DMRResponse response) {
            ModelNode result = response.get();
            if (!result.hasDefined(OUTCOME) || result.isFailure()) {
                channel.nack(result.getFailureDescription());
            } else {
                reload(new ReloadAccessControl(), channel);
            }
        }
    }


    final static String LOCAL_USERNAME = "$local";

    private final DispatchAsync dispatcher;
    private final BootstrapContext bootstrapContext;
    private boolean rbacProvider;
    private final Roles roles;
    private final Principals principals;
    private final Assignments assignments;

    @Inject
    public AccessControlStore(DispatchAsync dispatcher, BootstrapContext bootstrapContext) {
        this.dispatcher = dispatcher;
        this.bootstrapContext = bootstrapContext;

        this.rbacProvider = false;
        this.roles = new Roles();
        this.principals = new Principals();
        this.assignments = new Assignments();
    }


    // ------------------------------------------------------ read / parse management model

    @Process(actionType = ReloadAccessControl.class)
    public void reload(final ReloadAccessControl action, final Channel channel) {
        ResourceAddress address = AddressHelper.root();
        Operation providerOp = new Operation.Builder(READ_ATTRIBUTE_OPERATION, address)
                .param(NAME, "provider")
                .build();
        Operation standardRolesOp = new Operation.Builder(READ_ATTRIBUTE_OPERATION, address)
                .param(NAME, "standard-role-names")
                .build();
        Operation hostScopedRolesOp = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, address)
                .param(CHILD_TYPE, "host-scoped-role")
                .param(RECURSIVE, true)
                .build();
        Operation serverGroupScopedRolesOp = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, address)
                .param(CHILD_TYPE, "server-group-scoped-role")
                .param(RECURSIVE, true)
                .build();
        Operation assignmentsOp = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, address)
                .param(CHILD_TYPE, "role-mapping")
                .param(RECURSIVE, true)
                .build();

        Composite composite = bootstrapContext.isStandalone() ?
                new Composite(providerOp, standardRolesOp, assignmentsOp) :
                new Composite(providerOp, standardRolesOp, hostScopedRolesOp, serverGroupScopedRolesOp, assignmentsOp);

        dispatcher.execute(new DMRAction(composite), new AsyncCallback<DMRResponse>() { // do not use ReloadCallback!
            @Override
            public void onFailure(final Throwable caught) {
                channel.nack(caught);
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (!result.hasDefined(OUTCOME) || result.isFailure()) {
                    channel.nack(result.getFailureDescription());
                }

                reset();
                int stepIndex = 1;
                ModelNode payload = result.get(RESULT);

                // provider
                ModelNode provider = payload.get("step-" + stepIndex).get(RESULT);
                rbacProvider = provider.isDefined() && "rbac".equals(provider.asString());
                stepIndex++;

                // standard roles
                List<ModelNode> nodes = payload.get("step-" + stepIndex).get(RESULT).asList();
                for (ModelNode node : nodes) {
                    StandardRole standardRole = StandardRole.add(node.asString());
                    roles.add(new Role(standardRole));
                }
                stepIndex++;

                List<Property> properties;
                if (!bootstrapContext.isStandalone()) {
                    // host scoped roles
                    properties = payload.get("step-" + stepIndex).get(RESULT).asPropertyList();
                    for (Property property : properties) {
                        addScopedRole(property, "hosts", Role.Type.HOST);
                    }
                    stepIndex++;

                    // server group scoped roles
                    properties = payload.get("step-" + stepIndex).get(RESULT).asPropertyList();
                    for (Property property : properties) {
                        addScopedRole(property, "server-groups", Role.Type.SERVER_GROUP);
                    }
                    stepIndex++;
                }

                // assignments
                properties = payload.get("step-" + stepIndex).get(RESULT).asPropertyList();
                for (Property property : properties) {
                    Role role = roles.get(property.getName());
                    if (role != null) {
                        ModelNode assignmentNode = property.getValue();
                        if (assignmentNode.hasDefined("include-all")) {
                            role.setIncludeAll(assignmentNode.get("include-all").asBoolean());
                        }
                        if (assignmentNode.hasDefined("include")) {
                            properties = assignmentNode.get("include").asPropertyList();
                            for (Property include : properties) {
                                addAssignment(include, role, true);
                            }
                        }
                        if (assignmentNode.hasDefined("exclude")) {
                            properties = assignmentNode.get("exclude").asPropertyList();
                            for (Property exclude : properties) {
                                addAssignment(exclude, role, false);
                            }
                        }
                    } else {
                        Log.error("Cannot add assignment for role " + property.getName() + ": No matching role found!");
                    }
                }

                channel.ack();
            }
        });
    }

    private void addScopedRole(final Property property, final String scopeAttribute, final Role.Type type) {
        ModelNode node = property.getValue();
        String baseRoleName = node.get("base-role").asString();
        List<String> scope = new ArrayList<>();
        List<ModelNode> scopeNodes = node.get(scopeAttribute).asList();
        for (ModelNode scopeNode : scopeNodes) {
            scope.add(scopeNode.asString());
        }
        // Use matchId here since the configuration might contain roles in mixed / lower / upper case
        Role scopedRole = new Role(property.getName(), property.getName(), StandardRole.matchId(baseRoleName),
                type, scope);
        roles.add(scopedRole);
    }

    private void addAssignment(final Property property, final Role role, final boolean include) {
        String id = property.getName();
        ModelNode node = property.getValue();

        String name = node.get(NAME).asString();
        if (LOCAL_USERNAME.equals(name)) {
            return; // skip '$local' assignment
        }
        Principal.Type type = Principal.Type.valueOf(node.get("type").asString().toUpperCase());
        String realm = node.hasDefined("realm") ? node.get("realm").asString() : null;
        Principal principal = Principal.persistentPrincipal(type, id, name, realm);
        principals.add(principal);

        Assignment assignment = new Assignment(principal, role, include);
        assignments.add(assignment);
    }

    private void reset() {
        StandardRole.clearValues();
        roles.clear();
        principals.clear();
        assignments.clear();
    }


    // ------------------------------------------------------ principals

    @Process(actionType = AddPrincipal.class)
    public void addPrincipal(final AddPrincipal action, final Channel channel) {
        Principal principal = action.getPrincipal();
        if (principals.contains(principal)) {
            channel.nack(new DuplicateResourceException(principal.getName()));
        } else {
            // Add a transient principal; the management model is not changed.
            // Only if an assignment is added to this user, we have enough information to update the management model.
            principals.add(principal);
            channel.ack();
        }
    }

    @Process(actionType = RemovePrincipal.class)
    public void removePrincipal(final RemovePrincipal action, final Channel channel) {
        Principal principal = action.getPrincipal();
        if (principal.isTransient()) {
            // the easy part :)
            principals.remove(principal);
            channel.ack();
        } else {
            List<Operation> ops = new ArrayList<>();
            for (Assignment assignment : assignments) {
                if (assignment.getPrincipal().equals(principal)) {
                    ops.add(new Operation.Builder(REMOVE, AddressHelper.assignment(assignment)).build());
                }
            }
            Composite composite = new Composite(ops);
            dispatcher.execute(new DMRAction(composite), new ReloadCallback(channel));
        }
    }


    // ------------------------------------------------------ roles

    @Process(actionType = ModifyStandardRole.class)
    public void modifyStandardRole(final ModifyStandardRole action, Channel channel) {
        Role role = action.getRole();
        new Async<FunctionContext>().waterfall(new FunctionContext(), new ReloadOutcome(channel),
                new AccessControlFunctions.CheckAssignment(dispatcher, role),
                new AccessControlFunctions.AddAssignment(dispatcher, role, status -> status == 404),
                new AccessControlFunctions.ModifyIncludeAll(dispatcher, role));
    }

    @Process(actionType = AddScopedRole.class)
    public void addScopedRole(final AddScopedRole action, Channel channel) {
        Role role = action.getRole();
        String scopeType = role.getType() == Role.Type.HOST ? "hosts" : "server-groups";
        Collection<ModelNode> scope = Collections2.transform(role.getScope(), input -> new ModelNode().set(input));
        Operation addScopedRoleOp = new Operation.Builder(ADD, AddressHelper.scopedRole(role))
                .param("base-role", role.getBaseRole().getId())
                .param(scopeType, scope)
                .build();

        if (role.isIncludeAll()) {
            // Create the scoped role and after that create an empty role mapping with includeAll=true
            Function<FunctionContext> addScopedRoleFn =
                    control -> dispatcher.execute(new DMRAction(addScopedRoleOp), new FunctionCallback(control));

            new Async<FunctionContext>().waterfall(new FunctionContext(), new ReloadOutcome(channel),
                    addScopedRoleFn,
                    new AccessControlFunctions.CheckAssignment(dispatcher, role),
                    new AccessControlFunctions.AddAssignment(dispatcher, role, status -> status == 404),
                    new AccessControlFunctions.ModifyIncludeAll(dispatcher, role));

        } else {
            // Just create the scoped role
            dispatcher.execute(new DMRAction(addScopedRoleOp), new ReloadCallback(channel));
        }
    }

    @Process(actionType = ModifyScopedRole.class)
    public void modifyScopedRole(final ModifyScopedRole action, Channel channel) {
        Role role = action.getRole();
        ResourceAddress scopedRoleAddress = AddressHelper.scopedRole(role);
        String scopeType = role.getType() == Role.Type.HOST ? "hosts" : "server-groups";
        Collection<ModelNode> scope = Collections2.transform(role.getScope(), input -> new ModelNode().set(input));

        Operation op1 = new Operation.Builder(WRITE_ATTRIBUTE_OPERATION, scopedRoleAddress)
                .param(NAME, "base-role")
                .param(VALUE, role.getBaseRole().getId())
                .build();
        Operation op2 = new Operation.Builder(WRITE_ATTRIBUTE_OPERATION, scopedRoleAddress)
                .param(NAME, scopeType)
                .param(VALUE, scope)
                .build();
        Composite composite = new Composite(op1, op2);

        Function<FunctionContext> modifyScopedRoleFn =
                control -> dispatcher.execute(new DMRAction(composite), new FunctionCallback(control));

        new Async<FunctionContext>().waterfall(new FunctionContext(), new ReloadOutcome(channel),
                modifyScopedRoleFn,
                new AccessControlFunctions.CheckAssignment(dispatcher, role),
                new AccessControlFunctions.AddAssignment(dispatcher, role, status -> status == 404),
                new AccessControlFunctions.ModifyIncludeAll(dispatcher, role));
    }

    @Process(actionType = RemoveScopedRole.class)
    public void removeScopedRole(final RemoveScopedRole action, Channel channel) {
        Role role = action.getRole();
        if (!role.isScoped()) {
            channel.nack(new IllegalArgumentException("Standard roles cannot be removed!"));
        }

        int usage = 0;
        for (Assignment assignment : assignments) {
            if (assignment.getRole().equals(role)) {
                usage++;
            }
        }
        if (usage > 0) {
            channel.nack(new RoleInUseException(usage));

        } else {
            // Although the role is not used in any assignments there might be an empty includeAll mapping
            Operation removeScopedRoleOp = new Operation.Builder(REMOVE, AddressHelper.scopedRole(role)).build();
            Function<FunctionContext> removeScopedRoleFn =
                    control -> dispatcher.execute(new DMRAction(removeScopedRoleOp), new FunctionCallback(control));

            new Async<FunctionContext>().waterfall(new FunctionContext(), new ReloadOutcome(channel),
                    new AccessControlFunctions.CheckAssignment(dispatcher, role),
                    new AccessControlFunctions.RemoveAssignment(dispatcher, role, status -> status == 200),
                    removeScopedRoleFn);
        }
    }


    // ------------------------------------------------------ assignment

    @Process(actionType = AddAssignment.class)
    public void addAssignment(final AddAssignment action, Channel channel) {
        Assignment assignment = action.getAssignment();

        Operation.Builder builder = new Operation.Builder(ADD, AddressHelper.assignment(assignment))
                .param(NAME, assignment.getPrincipal().getName())
                .param("type", assignment.getPrincipal().getType().name());
        if (assignment.getPrincipal().getRealm() != null) {
            builder.param("realm", assignment.getPrincipal().getRealm());
        }
        Operation addOp = builder.build();
        Function<FunctionContext> addFn =
                control -> dispatcher.execute(new DMRAction(addOp), new FunctionCallback(control));

        new Async<FunctionContext>().waterfall(new FunctionContext(), new ReloadOutcome(channel),
                new AccessControlFunctions.CheckAssignment(dispatcher, assignment.getRole()),
                new AccessControlFunctions.AddAssignment(dispatcher, assignment.getRole(), status -> status == 404),
                addFn);
    }

    @Process(actionType = RemoveAssignment.class)
    public void removeAssignment(final RemoveAssignment action, Channel channel) {
        Assignment assignment = action.getAssignment();
        Operation operation = new Operation.Builder(REMOVE, AddressHelper.assignment(assignment)).build();
        dispatcher.execute(new DMRAction(operation), new ReloadCallback(channel));
        // TODO Cleanup empty mappings?
    }


    // ------------------------------------------------------ state access

    public boolean isRbacProvider() {
        return rbacProvider;
    }

    public Principals getPrincipals() {
        return principals;
    }

    public Iterable<Principal> getPrincipals(Role role, boolean include) {
        List<Principal> principals = new ArrayList<>();
        Iterable<Assignment> assignments = getAssignments(role, include);
        for (Assignment assignment : assignments) {
            principals.add(assignment.getPrincipal());
        }
        return principals;
    }

    public Roles getRoles() {
        return roles;
    }

    public Iterable<Assignment> getAssignments(Principal principal) {
        if (principal == null) {
            return Collections.emptyList();
        }
        return Iterables.filter(assignments, assignment -> assignment.getPrincipal().equals(principal));
    }

    public Iterable<Assignment> getAssignments(Principal principal, boolean include) {
        if (principal == null) {
            return Collections.emptyList();
        }
        return Iterables.filter(assignments,
                assignment -> assignment.getPrincipal().equals(principal) && assignment.isInclude() == include);
    }

    public Iterable<Assignment> getAssignments(Role role) {
        if (role == null) {
            return Collections.emptyList();
        }
        return Iterables.filter(assignments, assignment -> assignment.getRole().equals(role));
    }

    public Iterable<Assignment> getAssignments(Role role, boolean include) {
        if (role == null) {
            return Collections.emptyList();
        }
        return Iterables.filter(assignments,
                assignment -> assignment.getRole().equals(role) && assignment.isInclude() == include);
    }
}
