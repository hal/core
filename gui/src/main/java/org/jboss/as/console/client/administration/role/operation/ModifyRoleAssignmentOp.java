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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.jboss.as.console.client.administration.role.model.Role;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Async;
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
    public void execute(final Outcome<Stack<Boolean>> outcome) {
        List<Function<Stack<Boolean>>> functions = new ArrayList<Function<Stack<Boolean>>>();

        switch (operation) {
            case ADD:
            case MODIFY: {
                for (Role role : assignment.getRoles()) {
                    checkAndAdd(functions, role, "include");
                }
                for (Role exclude : assignment.getExcludes()) {
                    checkAndAdd(functions, exclude, "exclude");
                }
                for (Role removedRole : removedRoles) {
                    functions.add(new PrincipalFuntions.Remove(dispatcher, removedRole, assignment.getPrincipal(),
                            assignment.getRealm(), "include"));
                }
                for (Role removedExclude : removedExcludes) {
                    functions.add(
                            new PrincipalFuntions.Remove(dispatcher, removedExclude, assignment.getPrincipal(),
                                    assignment.getRealm(), "exclude"));
                }
                break;
            }
            case REMOVE:
                functions.add(new RoleAssignmentFunctions.Remove(dispatcher, assignment));
                break;
            case RENAME:
                throw new UnsupportedOperationException("Cannot rename a role assignment");
        }
        new Async<Stack<Boolean>>()
                .waterfall(new Stack<Boolean>(), outcome, functions.toArray(new Function[functions.size()]));
    }

    private void checkAndAdd(final List<Function<Stack<Boolean>>> functions, final Role role,
            final String includeExclude) {
        functions.add(new RoleAssignmentFunctions.Check(dispatcher, role));
        functions.add(new RoleAssignmentFunctions.Add(dispatcher, role));
        functions
                .add(new PrincipalFuntions.Check(dispatcher, role, assignment.getPrincipal(),
                        assignment.getRealm(), includeExclude));
        functions.add(new PrincipalFuntions.Add(dispatcher, role, assignment.getPrincipal(),
                assignment.getRealm(), includeExclude));
    }

    @Override
    public boolean isPending() {
        throw new UnsupportedOperationException("not implemented");
    }
}
