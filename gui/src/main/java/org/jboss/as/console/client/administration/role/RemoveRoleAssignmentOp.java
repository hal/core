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

import java.util.LinkedList;
import java.util.List;

import org.jboss.as.console.client.administration.role.model.ModelHelper;
import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.Role;
import org.jboss.dmr.client.ModelNode;
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
public class RemoveRoleAssignmentOp implements ManagementOperation<Object> {

    private final DispatchAsync dispatcher;
    private final RoleAssignment assignment;
    private boolean pending;

    public RemoveRoleAssignmentOp(final DispatchAsync dispatcher, final RoleAssignment assignment) {
        this.dispatcher = dispatcher;
        this.assignment = assignment;
    }

    @Override
    public void extecute(final Outcome<Object> outcome) {
        pending = true;
        new Async<StringBuilder>().parallel(outcome, new RemoveFunction());
    }

    @Override
    public boolean isPending() {
        return pending;
    }

    class RemoveFunction implements Function<Void> {

        @Override
        public void execute(final Control<Void> control) {
            ModelNode operation = new ModelNode();
            operation.get(ADDRESS).setEmptyList();
            operation.get(OP).set(COMPOSITE);
            List<ModelNode> steps = new LinkedList<ModelNode>();

            for (Role role : assignment.getRoles()) {
                ModelNode deleteIncludeOp = principalNode(role, assignment.getPrincipal(), "include");
                steps.add(deleteIncludeOp);

                if (assignment.getExcludes().get(role.getName()) != null) {
                    for (Principal exclude : assignment.getExcludes().get(role.getName())) {
                        ModelNode deleteExcludeOp = principalNode(role, exclude, "exclude");
                        steps.add(deleteExcludeOp);
                    }
                }
            }

            operation.get(STEPS).set(steps);
            dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
                @Override
                public void onSuccess(final DMRResponse result) {
                    pending = false;
                    control.proceed();
                }

                @Override
                public void onFailure(final Throwable caught) {
                    pending = false;
                    control.abort();
                }
            });
        }

        private ModelNode principalNode(final Role role, final Principal exclude, String includeExclude) {
            ModelNode removeOp = new ModelNode();
            removeOp.get(ADDRESS).add("core-service", "management");
            removeOp.get(ADDRESS).add("access", "authorization");
            removeOp.get(ADDRESS).add("role-mapping", role.getName());
            removeOp.get(ADDRESS).add(includeExclude, ModelHelper.principalIdentifier(exclude));
            removeOp.get(OP).set(REMOVE);
            return removeOp;
        }
    }
}
