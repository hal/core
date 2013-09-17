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
import java.util.List;
import java.util.Stack;

import org.jboss.as.console.client.administration.role.model.Role;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;

/**
 * @author Harald Pehl
 */
public class ModifyRoleOp implements ManagementOperation<Stack<Boolean>> {

    private final DispatchAsync dispatcher;
    private final Role role;
    private final Role oldValue;
    private final Operation op;

    public ModifyRoleOp(final DispatchAsync dispatcher, final Role role, final Role oldValue,
            final Operation op) {
        this.dispatcher = dispatcher;
        this.role = role;
        this.oldValue = oldValue;
        this.op = op;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(final Outcome<Stack<Boolean>> outcome) {
        List<Function<Stack<Boolean>>> functions = new ArrayList<Function<Stack<Boolean>>>();
        switch (op) {
            case ADD:
                if (role.isStandard()) {
                    throw new UnsupportedOperationException("Cannot add standard role");
                }
                functions.add(new ScopedRoleFunctions.Add(dispatcher, role));
                break;
            case RENAME:
                if (role.isStandard()) {
                    throw new UnsupportedOperationException("Cannot rename standard role");
                }
                functions.add(new ScopedRoleFunctions.Remove(dispatcher, oldValue));
                functions.add(new ScopedRoleFunctions.Add(dispatcher, role));
                break;
            case MODIFY:
                if (role.isScoped()) {
                    // only modify scoped roles
                    functions.add(new ScopedRoleFunctions.Modify(dispatcher, role));
                }
                break;
            case REMOVE:
                if (role.isStandard()) {
                    throw new UnsupportedOperationException("Cannot remove standard role");
                }
                functions.add(new ScopedRoleFunctions.Remove(dispatcher, role));
                break;
        }
        // Set the include-all flag on related role assignment
        functions.add(new RoleAssignmentFunctions.Check(dispatcher, role));
        functions.add(new RoleAssignmentFunctions.Add(dispatcher, role));
        functions.add(new RoleAssignmentFunctions.IncludeAll(dispatcher, role));

        new Async<Stack<Boolean>>()
                .waterfall(new Stack<Boolean>(), outcome, functions.toArray(new Function[functions.size()]));
    }

    @Override
    public boolean isPending() {
        throw new UnsupportedOperationException("not implemented");
    }
}
