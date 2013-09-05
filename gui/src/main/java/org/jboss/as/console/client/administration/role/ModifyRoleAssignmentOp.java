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
import java.util.List;
import java.util.Stack;

import org.jboss.as.console.client.administration.role.model.ModelHelper;
import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.Role;
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
public class ModifyRoleAssignmentOp {

    private final DispatchAsync dispatcher;
    private final RoleAssignment assignment;
    private final Collection<Role> removedRoles;
    private final Collection<Principal> removedExcludes;

    public ModifyRoleAssignmentOp(final DispatchAsync dispatcher, final RoleAssignment assignment,
            final Collection<Role> removedRoles, final Collection<Principal> removedExcludes) {
        this.dispatcher = dispatcher;
        this.assignment = assignment;
        this.removedRoles = removedRoles;
        this.removedExcludes = removedExcludes;
    }

    @SuppressWarnings("unchecked")
    public void extecute(final Outcome<Stack<Boolean>> outcome) {
        List<Function<Stack<Boolean>>> functions = new ArrayList<Function<Stack<Boolean>>>();
        for (Role role : assignment.getRoles()) {
            functions.add(new ReadRoleFunction(role));
            functions.add(new AddRoleFunction(role));
            functions.add(new ReadPrincipalFunction(role, assignment.getPrincipal(), true));
            functions.add(new AddPrincipalFunction(role, assignment.getPrincipal(), true));
            if (assignment.getExcludes() != null) {
                for (Principal exclude : assignment.getExcludes()) {
                    functions.add(new ReadPrincipalFunction(role, exclude, false));
                    functions.add(new AddPrincipalFunction(role, exclude, false));
                }
            }
        }
        Collection<Role> roles;
        if (removedRoles.isEmpty() && !removedExcludes.isEmpty()) {
            for (Role role : assignment.getRoles()) {
                for (Principal exclude : removedExcludes) {
                    functions.add(new RemovePrincipalFunction(role, exclude, false));
                }
            }
        } else {
            for (Role role : removedRoles) {
                functions.add(new RemovePrincipalFunction(role, assignment.getPrincipal(), true));
                for (Principal exclude : removedExcludes) {
                    functions.add(new RemovePrincipalFunction(role, exclude, false));
                }
            }
        }

        new Async<Stack<Boolean>>()
                .waterfall(new Stack<Boolean>(), outcome, functions.toArray(new Function[functions.size()]));
    }

    class ReadRoleFunction implements Function<Stack<Boolean>> {

        private final Role role;

        ReadRoleFunction(final Role role) {this.role = role;}

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            ModelNode realRoleOp = new ModelNode();
            realRoleOp.get(ADDRESS).add("core-service", "management");
            realRoleOp.get(ADDRESS).add("access", "authorization");
            realRoleOp.get(ADDRESS).add("role-mapping", role.getName());
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

        AddRoleFunction(final Role role) {this.role = role;}

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            boolean roleExists = control.getContext().pop();
            if (roleExists) {
                control.proceed();
            } else {
                ModelNode addRoleOp = new ModelNode();
                addRoleOp.get(ADDRESS).add("core-service", "management");
                addRoleOp.get(ADDRESS).add("access", "authorization");
                addRoleOp.get(ADDRESS).add("role-mapping", role.getName());
                addRoleOp.get(OP).set(ADD);

                dispatcher.execute(new DMRAction(addRoleOp), new SimpleCallback<DMRResponse>() {
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

    class ReadPrincipalFunction implements Function<Stack<Boolean>> {

        private final Role role;
        private final Principal principal;
        private final boolean include;

        public ReadPrincipalFunction(final Role role, final Principal principal, final boolean include) {
            this.role = role;
            this.principal = principal;
            this.include = include;
        }

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            ModelNode assignmentOp = new ModelNode();
            assignmentOp.get(ADDRESS).add("core-service", "management");
            assignmentOp.get(ADDRESS).add("access", "authorization");
            assignmentOp.get(ADDRESS).add("role-mapping", role.getName());
            assignmentOp.get(ADDRESS).add(include ? "include" : "exclude", ModelHelper.principalIdentifier(principal));
            assignmentOp.get(OP).set(READ_RESOURCE_OPERATION);

            dispatcher.execute(new DMRAction(assignmentOp), new SimpleCallback<DMRResponse>() {
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
        private final boolean include;

        public AddPrincipalFunction(final Role role, final Principal principal, final boolean include) {
            this.role = role;
            this.principal = principal;
            this.include = include;
        }

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            boolean principalExists = control.getContext().pop();
            if (principalExists) {
                control.proceed();
            } else {
                ModelNode assignmentOp = new ModelNode();
                assignmentOp.get(ADDRESS).add("core-service", "management");
                assignmentOp.get(ADDRESS).add("access", "authorization");
                assignmentOp.get(ADDRESS).add("role-mapping", role.getName());
                assignmentOp.get(ADDRESS)
                        .add(include ? "include" : "exclude", ModelHelper.principalIdentifier(principal));
                assignmentOp.get("name").set(ModelType.STRING, principal.getName());
                assignmentOp.get("type").set(ModelType.STRING, principal.getType().name());
                if (principal.getRealm() != null && principal.getRealm().length() != 0) {
                    assignmentOp.get("realm").set(ModelType.STRING, principal.getRealm());
                }
                assignmentOp.get(OP).set(ADD);

                dispatcher.execute(new DMRAction(assignmentOp), new SimpleCallback<DMRResponse>() {
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
        private final boolean include;

        public RemovePrincipalFunction(final Role role, final Principal principal, final boolean include) {
            this.role = role;
            this.principal = principal;
            this.include = include;
        }

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            ModelNode assignmentOp = new ModelNode();
            assignmentOp.get(ADDRESS).add("core-service", "management");
            assignmentOp.get(ADDRESS).add("access", "authorization");
            assignmentOp.get(ADDRESS).add("role-mapping", role.getName());
            assignmentOp.get(ADDRESS).add(include ? "include" : "exclude", ModelHelper.principalIdentifier(principal));
            assignmentOp.get(OP).set(REMOVE);

            dispatcher.execute(new DMRAction(assignmentOp), new SimpleCallback<DMRResponse>() {
                @Override
                public void onSuccess(DMRResponse response) {
                    control.proceed();
                }

                @Override
                public void onFailure(final Throwable caught) {
                    // Ignore removing none existing principals
                    control.proceed();
                }
            });
        }
    }
}
