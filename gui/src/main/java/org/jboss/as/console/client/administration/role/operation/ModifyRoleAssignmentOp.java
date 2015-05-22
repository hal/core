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

import org.jboss.as.console.client.administration.accesscontrol.store.Role;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.core.Footer;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Harald Pehl
 */
public class ModifyRoleAssignmentOp implements ManagementOperation<FunctionContext> {

    private final DispatchAsync dispatcher;
    private final RoleAssignment assignment;
    private final RoleAssignment oldValue;
    private final Operation operation;

    public ModifyRoleAssignmentOp(final DispatchAsync dispatcher, final RoleAssignment assignment,
            final Operation operation) {
        this(dispatcher, assignment, null, operation);
    }

    public ModifyRoleAssignmentOp(final DispatchAsync dispatcher, final RoleAssignment assignment,
            final RoleAssignment oldValue, Operation operation) {
        this.dispatcher = dispatcher;
        this.assignment = assignment;
        this.operation = operation;
        this.oldValue = oldValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(final Outcome<FunctionContext> outcome) {
        List<Function<FunctionContext>> functions = new ArrayList<Function<FunctionContext>>();

        switch (operation) {
            case ADD: {
                for (Role role : assignment.getRoles()) {
                    checkAndAdd(functions, role, "include");
                }
                for (Role exclude : assignment.getExcludes()) {
                    checkAndAdd(functions, exclude, "exclude");
                }
                break;
            }
            case MODIFY: {
                if (oldValue == null) {
                    throw new IllegalStateException("No old value provided");
                }
                // Calculate the change set between assignment and oldValue
                Set<Role> addedRoles = added(assignment.getRoles(), oldValue.getRoles());
                for (Role role : addedRoles) {
                    checkAndAdd(functions, role, "include");
                }
                Set<Role> addedExcludes = added(assignment.getExcludes(), oldValue.getExcludes());
                for (Role exclude : addedExcludes) {
                    checkAndAdd(functions, exclude, "exclude");
                }
                Set<Role> removedRoles = removed(assignment.getRoles(), oldValue.getRoles());
                for (Role removedRole : removedRoles) {
                    functions.add(new PrincipalFunctions.Remove(dispatcher, removedRole, assignment.getPrincipal(),
                            "include"));
                }
                Set<Role> removedExcludes = removed(assignment.getExcludes(), oldValue.getExcludes());
                for (Role removedExclude : removedExcludes) {
                    functions.add(
                            new PrincipalFunctions.Remove(dispatcher, removedExclude, assignment.getPrincipal(),
                                    "exclude"));
                }
                // Clear empty / unused role assignments (this could be optimized!)
                functions.add(new RoleAssignmentFunctions.Find(dispatcher));
                functions.add(new RoleAssignmentFunctions.RemoveMatching(dispatcher));
                break;
            }
            case REMOVE:
                functions.add(new RoleAssignmentFunctions.Remove(dispatcher, assignment));
                // Clear empty / unused role assignments (this could be optimized!)
                functions.add(new RoleAssignmentFunctions.Find(dispatcher));
                functions.add(new RoleAssignmentFunctions.RemoveMatching(dispatcher));
                break;
        }
        new Async<FunctionContext>(Footer.PROGRESS_ELEMENT)
                .waterfall(new FunctionContext(), outcome, functions.toArray(new Function[functions.size()]));
    }

    private void checkAndAdd(final List<Function<FunctionContext>> functions, final Role role,
            final String includeExclude) {
        functions.add(new RoleAssignmentFunctions.Check(dispatcher, role));
        functions.add(new RoleAssignmentFunctions.Add(dispatcher, role));

        functions.add(new PrincipalFunctions.Check(dispatcher, role, assignment.getPrincipal(),
                assignment.getRealm(), includeExclude));
        functions.add(new PrincipalFunctions.Add(dispatcher, role, assignment.getPrincipal(),
                assignment.getRealm(), includeExclude));
    }

    private Set<Role> added(final Set<Role> current, final Set<Role> old) {
        Set<Role> added = new HashSet<Role>(current);
        added.removeAll(old);
        return added;
    }

    private Set<Role> removed(final Set<Role> current, final Set<Role> old) {
        Set<Role> removed = new HashSet<Role>(old);
        removed.removeAll(current);
        return removed;
    }

    @Override
    public boolean isPending() {
        throw new UnsupportedOperationException("not implemented");
    }
}
