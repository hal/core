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

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.jboss.as.console.client.administration.role.model.ModelHelper;
import org.jboss.as.console.client.administration.role.model.Role;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

/**
 * Functions to check, add (if not present) and remove a role assignements; supposed two work inside a call to {@link
 * org.jboss.gwt.flow.client.Async#waterfall(Object, org.jboss.gwt.flow.client.Outcome,
 * org.jboss.gwt.flow.client.Function[])}
 *
 * @author Harald Pehl
 */
public final class RoleAssignmentFunctions {

    private RoleAssignmentFunctions() {}

    public static class Check implements Function<Stack<Boolean>> {

        private final DispatchAsync dispatcher;
        private final Role role;

        Check(final DispatchAsync dispatcher, final Role role) {
            this.dispatcher = dispatcher;
            this.role = role;
        }

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            ModelNode node = ModelHelper.roleMapping(role);
            node.get(OP).set(READ_RESOURCE_OPERATION);
            dispatcher.execute(new DMRAction(node), new FunctionCallback<Stack<Boolean>>(control) {
                @Override
                protected void proceed() {
                    // role exists - next function will skip its DMR operation
                    control.getContext().push(true);
                    control.proceed();
                }

                @Override
                protected void abort() {
                    // no role - create it in the next function
                    control.getContext().push(false);
                    control.proceed();
                }
            });
        }
    }

    public static class Add implements Function<Stack<Boolean>> {

        private final DispatchAsync dispatcher;
        private final Role role;

        Add(final DispatchAsync dispatcher, final Role role) {
            this.dispatcher = dispatcher;
            this.role = role;
        }

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            boolean roleExists = control.getContext().pop();
            if (roleExists) {
                control.proceed();
            } else {
                ModelNode node = ModelHelper.roleMapping(role);
                node.get(OP).set(ADD);
                System.out.println(node);
                dispatcher.execute(new DMRAction(node), new FunctionCallback<Stack<Boolean>>(control));
            }
        }
    }

    public static class IncludeAll implements Function<Stack<Boolean>> {

        private final DispatchAsync dispatcher;
        private final Role role;

        public IncludeAll(final DispatchAsync dispatcher, final Role role) {
            this.dispatcher = dispatcher;
            this.role = role;
        }

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            ModelNode node = ModelHelper.roleMapping(role);
            node.get(NAME).set("include-all");
            node.get(VALUE).set(role.isIncludeAll());
            node.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            dispatcher.execute(new DMRAction(node), new FunctionCallback<Stack<Boolean>>(control));
        }
    }

    public static class Remove implements Function<Stack<Boolean>> {

        private final DispatchAsync dispatcher;
        private final RoleAssignment assignment;

        public Remove(final DispatchAsync dispatcher, final RoleAssignment assignment) {
            this.dispatcher = dispatcher;
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
            dispatcher.execute(new DMRAction(operation), new FunctionCallback<Stack<Boolean>>(control));
        }
    }
}
