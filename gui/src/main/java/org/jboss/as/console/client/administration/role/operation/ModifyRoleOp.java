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

import org.jboss.as.console.client.administration.role.model.Role;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.ConsoleProgress;
import org.jboss.gwt.flow.client.Function;
import org.jboss.gwt.flow.client.Outcome;

/**
 * @author Harald Pehl
 */
public class ModifyRoleOp implements ManagementOperation<FunctionContext> {

    private final DispatchAsync dispatcher;
    private final Role role;
    private final Operation op;

    public ModifyRoleOp(final DispatchAsync dispatcher, final Role role, final Operation op) {
        this.dispatcher = dispatcher;
        this.role = role;
        this.op = op;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(final Outcome<FunctionContext> outcome) {
        List<Function<FunctionContext>> functions = new ArrayList<Function<FunctionContext>>();
        switch (op) {
            case ADD:
                if (role.isStandard()) {
                    throw new UnsupportedOperationException("Cannot add standard role");
                }
                functions.add(new ScopedRoleFunctions.Add(dispatcher, role));
                functions.add(new RoleAssignmentFunctions.Check(dispatcher, role));
                functions.add(new RoleAssignmentFunctions.Add(dispatcher, role));
                functions.add(new RoleAssignmentFunctions.IncludeAll(dispatcher, role));
                break;
            case MODIFY:
                if (role.isScoped()) {
                    // only modify scoped roles
                    functions.add(new ScopedRoleFunctions.Modify(dispatcher, role));
                }
                functions.add(new RoleAssignmentFunctions.Check(dispatcher, role));
                functions.add(new RoleAssignmentFunctions.Add(dispatcher, role));
                functions.add(new RoleAssignmentFunctions.IncludeAll(dispatcher, role));
                break;
            case REMOVE:
                if (role.isStandard()) {
                    throw new UnsupportedOperationException("Cannot remove standard role");
                }
                // <Workaround for WFLY-2270>
                if (role.isIncludeAll()) {
                    Role includeAllFake = new Role(role.getId(), role.getName(), role.getBaseRole(), role.getType(),
                            role.getScope());
                    includeAllFake.setIncludeAll(false);
                    functions.add(new RoleAssignmentFunctions.IncludeAll(dispatcher, includeAllFake));
                }
                // </Workaround for WFLY-2270>
                functions.add(new RoleAssignmentFunctions.Find(dispatcher, role.getId()));
                functions.add(new RoleAssignmentFunctions.RemoveMatching(dispatcher));
                functions.add(new ScopedRoleFunctions.Remove(dispatcher, role));
                break;
        }
        // Clear empty / unused role assignments (this could be optimized!)
        functions.add(new RoleAssignmentFunctions.Find(dispatcher));
        functions.add(new RoleAssignmentFunctions.RemoveMatching(dispatcher));

        new Async<FunctionContext>(new ConsoleProgress("modifyRole"))
                .waterfall(new FunctionContext(), outcome, functions.toArray(new Function[functions.size()]));
    }

    @Override
    public boolean isPending() {
        throw new UnsupportedOperationException("not implemented");
    }
}
