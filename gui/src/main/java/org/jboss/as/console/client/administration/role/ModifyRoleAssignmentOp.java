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
import java.util.Set;
import java.util.Stack;

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
public class ModifyRoleAssignmentOp implements ManagementOperation<Stack<Boolean>> {

    private final DispatchAsync dispatcher;
    private final RoleAssignment assignment;
    private final Collection<Role> removedRoles;
    private final Collection<Role> removedExcludes;

    public ModifyRoleAssignmentOp(final DispatchAsync dispatcher, final RoleAssignment assignment,
            final Set<Role> removedRoles, final Set<Role> removedExcludes) {
        this.dispatcher = dispatcher;
        this.assignment = assignment;
        this.removedRoles = removedRoles;
        this.removedExcludes = removedExcludes;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void extecute(final Outcome<Stack<Boolean>> outcome) {
        List<Function<Stack<Boolean>>> functions = new ArrayList<Function<Stack<Boolean>>>();
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

    abstract class RoleFunction<T> implements Function<T> {

        protected final Role role;

        RoleFunction(final Role role) {this.role = role;}

        protected ModelNode roleNode() {
            ModelNode node = new ModelNode();
            node.get(ADDRESS).add("core-service", "management");
            node.get(ADDRESS).add("access", "authorization");
            node.get(ADDRESS).add("role-mapping", role.getName());
            return node;
        }
    }

    class ReadRoleFunction extends RoleFunction<Stack<Boolean>> {

        ReadRoleFunction(final Role role) {
            super(role);
        }

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            ModelNode realRoleOp = roleNode();
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

    class AddRoleFunction extends RoleFunction<Stack<Boolean>> {

        AddRoleFunction(final Role role) {
            super(role);
        }

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            boolean roleExists = control.getContext().pop();
            if (roleExists) {
                control.proceed();
            } else {
                ModelNode addRoleOp = roleNode();
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

    abstract class PrincipalFunction<T> implements Function<T> {

        protected final Role role;
        protected final Principal principal;
        protected final String principalId;
        protected final String realm;
        protected final String includeExclude;

        public PrincipalFunction(final Role role, final Principal principal, final String realm,
                final String includeExclude) {
            this.role = role;
            this.principal = principal;
            this.realm = realm;
            StringBuilder id = new StringBuilder(principal.getId());
            if (realm != null) {
                id.append("@").append(realm);
            }
            this.principalId = id.toString();
            this.includeExclude = includeExclude;
        }

        protected ModelNode principalNode() {
            ModelNode node = new ModelNode();
            node.get(ADDRESS).add("core-service", "management");
            node.get(ADDRESS).add("access", "authorization");
            node.get(ADDRESS).add("role-mapping", role.getName());
            node.get(ADDRESS).add(includeExclude, principalId);
            return node;
        }
    }

    class ReadPrincipalFunction extends PrincipalFunction<Stack<Boolean>> {

        public ReadPrincipalFunction(final Role role,
                final Principal principal, final String realm, final String includeExclude) {
            super(role, principal, realm, includeExclude);
        }

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            ModelNode node = principalNode();
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

    class AddPrincipalFunction extends PrincipalFunction<Stack<Boolean>> {

        public AddPrincipalFunction(final Role role,
                final Principal principal, final String realm, final String includeExclude) {
            super(role, principal, realm, includeExclude);
        }

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            boolean principalExists = control.getContext().pop();
            if (principalExists) {
                control.proceed();
            } else {
                ModelNode node = principalNode();
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

    class RemovePrincipalFunction extends PrincipalFunction<Stack<Boolean>> {

        public RemovePrincipalFunction(final Role role,
                final Principal principal, final String realm, final String includeExclude) {
            super(role, principal, realm, includeExclude);
        }

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            ModelNode node = principalNode();
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
}
