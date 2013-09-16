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

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.Role;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;

/**
 * @author Harald Pehl
 */
public class ModifyRoleAssignmentOp implements ManagementOperation<Stack<Boolean>> {

    private final DispatchAsync dispatcher;
    private final RoleAssignment assignment;
    private final Operation operation;
    private final Collection<Role> removedRoles;
    private final Collection<Role> removedExcludes;

    public ModifyRoleAssignmentOp(final DispatchAsync dispatcher, final RoleAssignment assignment,
            final Operation operation) {
        this(dispatcher, assignment, operation, Collections.<Role>emptySet(), Collections.<Role>emptySet());
    }

    public ModifyRoleAssignmentOp(final DispatchAsync dispatcher, final RoleAssignment assignment, Operation operation,
            final Set<Role> removedRoles, final Set<Role> removedExcludes) {
        this.dispatcher = dispatcher;
        this.assignment = assignment;
        this.operation = operation;
        this.removedRoles = removedRoles;
        this.removedExcludes = removedExcludes;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void extecute(final Outcome<Stack<Boolean>> outcome) {
        List<Function<Stack<Boolean>>> functions = new ArrayList<Function<Stack<Boolean>>>();

        switch (operation) {
            case ADD:
            case MODIFY: {
                for (Role role : assignment.getRoles()) {
                    addFunctions(functions, role, "include");
                }
                for (Role exclude : assignment.getExcludes()) {
                    addFunctions(functions, exclude, "exclude");
                }
                for (Role removedRole : removedRoles) {
                    functions.add(new RemovePrincipalFunction(removedRole, assignment.getPrincipal(), assignment.getRealm(),
                            "include"));
                }
                for (Role removedExclude : removedExcludes) {
                    functions.add(new RemovePrincipalFunction(removedExclude, assignment.getPrincipal(), assignment.getRealm(),
                            "exclude"));
                }
                break;
            }
            case REMOVE:
                functions.add(new RemoveRoleAssignmentFunction(assignment));
                break;
            case RENAME:
                throw new UnsupportedOperationException("Cannot rename a role assignment");
        }
        new Async<Stack<Boolean>>()
                .waterfall(new Stack<Boolean>(), outcome, functions.toArray(new Function[functions.size()]));
    }

    private void addFunctions(final List<Function<Stack<Boolean>>> functions, final Role role,
            final String includeExclude) {
        functions.add(new ReadRoleFunction(role));
        functions.add(new AddRoleFunction(role));
        functions
                .add(new ReadPrincipalFunction(role, assignment.getPrincipal(), assignment.getRealm(), includeExclude));
        functions.add(new AddPrincipalFunction(role, assignment.getPrincipal(), assignment.getRealm(), includeExclude));
    }

    @Override
    public boolean isPending() {
        throw new UnsupportedOperationException("not implemented");
    }

    class ReadRoleFunction implements Function<Stack<Boolean>> {

        private final Role role;

        ReadRoleFunction(final Role role) {
            this.role = role;
        }

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            ModelNode realRoleOp = ModelHelper.roleMapping(role);
            realRoleOp.get(OP).set(READ_RESOURCE_OPERATION);

            dispatcher.execute(new DMRAction(realRoleOp), new SimpleCallback<DMRResponse>() {
                @Override
                public void onSuccess(DMRResponse response) {
                    // role exists - next function will skip its DMR operation
                    control.getContext().push(true);
                    control.proceed();
                }

                @Override
                public void onFailure(final Throwable caught) {
                    // no role - create it in the next function
                    control.getContext().push(false);
                    control.proceed();
                }
            });
        }
    }

    class AddRoleFunction implements Function<Stack<Boolean>> {

        private final Role role;

        AddRoleFunction(final Role role) {
            this.role = role;
        }

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            boolean roleExists = control.getContext().pop();
            if (roleExists) {
                control.proceed();
            } else {
                ModelNode addRoleOp = ModelHelper.roleMapping(role);
                addRoleOp.get(OP).set(ADD);

                dispatcher.execute(new DMRAction(addRoleOp), new SimpleCallback<DMRResponse>() {
                    @Override
                    public void onSuccess(DMRResponse response) {
                        control.proceed();
                    }

                    @Override
                    public void onFailure(final Throwable caught) {
                        control.abort();
                    }
                });
            }
        }
    }

    class ReadPrincipalFunction implements Function<Stack<Boolean>> {

        private final Role role;
        private final Principal principal;
        private final String realm;
        private final String includeExclude;

        public ReadPrincipalFunction(final Role role,
                final Principal principal, final String realm, final String includeExclude) {
            this.role = role;
            this.principal = principal;
            this.realm = realm;
            this.includeExclude = includeExclude;
        }

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            ModelNode node = ModelHelper.includeExclude(role, principal, realm, includeExclude);
            node.get(OP).set(READ_RESOURCE_OPERATION);

            dispatcher.execute(new DMRAction(node), new SimpleCallback<DMRResponse>() {
                @Override
                public void onSuccess(DMRResponse response) {
                    // assignment exists - next function will skip its DMR operation
                    control.getContext().push(true);
                    control.proceed();
                }

                @Override
                public void onFailure(final Throwable caught) {
                    // no assignment - create it in the next function
                    control.getContext().push(false);
                    control.proceed();
                }
            });
        }
    }

    class AddPrincipalFunction implements Function<Stack<Boolean>> {

        private final Role role;
        private final Principal principal;
        private final String realm;
        private final String includeExclude;

        public AddPrincipalFunction(final Role role,
                final Principal principal, final String realm, final String includeExclude) {
            this.role = role;
            this.principal = principal;
            this.realm = realm;
            this.includeExclude = includeExclude;
        }

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            boolean principalExists = control.getContext().pop();
            if (principalExists) {
                control.proceed();
            } else {
                ModelNode node = ModelHelper.includeExclude(role, principal, realm, includeExclude);
                node.get("name").set(ModelType.STRING, principal.getName());
                node.get("type").set(ModelType.STRING, principal.getType().name());
                if (realm != null && realm.length() != 0) {
                    node.get("realm").set(ModelType.STRING, realm);
                }
                node.get(OP).set(ADD);

                dispatcher.execute(new DMRAction(node), new SimpleCallback<DMRResponse>() {
                    @Override
                    public void onSuccess(DMRResponse response) {
                        control.proceed();
                    }

                    @Override
                    public void onFailure(final Throwable caught) {
                        // TODO Error handling
                        control.abort();
                    }
                });
            }
        }
    }

    class RemovePrincipalFunction implements Function<Stack<Boolean>> {

        private final Role role;
        private final Principal principal;
        private final String realm;
        private final String includeExclude;

        public RemovePrincipalFunction(final Role role,
                final Principal principal, final String realm, final String includeExclude) {
            this.role = role;
            this.principal = principal;
            this.realm = realm;
            this.includeExclude = includeExclude;
        }

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            ModelNode node = ModelHelper.includeExclude(role, principal, realm, includeExclude);
            node.get(OP).set(REMOVE);

            dispatcher.execute(new DMRAction(node), new SimpleCallback<DMRResponse>() {
                @Override
                public void onSuccess(DMRResponse response) {
                    control.proceed();
                }

                @Override
                public void onFailure(final Throwable caught) {
                    control.abort();
                }
            });
        }
    }

    class RemoveRoleAssignmentFunction implements Function<Stack<Boolean>> {

        private final RoleAssignment assignment;

        public RemoveRoleAssignmentFunction(final RoleAssignment assignment) {

            this.assignment = assignment;
        }

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            ModelNode operation = new ModelNode();
            operation.get(ADDRESS).setEmptyList();
            operation.get(OP).set(COMPOSITE);
            List<ModelNode> steps = new LinkedList<ModelNode>();

            for (Role role : assignment.getRoles()) {
                ModelNode step = ModelHelper.includeExclude(role, assignment.getPrincipal(),
                        assignment.getRealm(), "include");
                step.get(OP).set(REMOVE);
                steps.add(step);
            }
            for (Role exclude : assignment.getExcludes()) {
                ModelNode step = ModelHelper.includeExclude(exclude, assignment.getPrincipal(),
                        assignment.getRealm(), "exclude");
                step.get(OP).set(REMOVE);
                steps.add(step);
            }

            operation.get(STEPS).set(steps);
            dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
                @Override
                public void onSuccess(final DMRResponse result) {
                    control.proceed();
                }

                @Override
                public void onFailure(final Throwable caught) {
                    control.abort();
                }
            });
        }
    }
}
