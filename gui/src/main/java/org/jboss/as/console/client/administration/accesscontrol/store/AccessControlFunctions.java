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
package org.jboss.as.console.client.administration.accesscontrol.store;

import com.google.common.base.Predicate;
import org.jboss.as.console.client.shared.flow.FunctionCallback;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * Functions related to principals, roles and assignments.
 *
 * @author Harald Pehl
 */
final class AccessControlFunctions {

    private AccessControlFunctions() {}

    /**
     * Checks whether an assignment for a given role exists and pushes {@code 200} to the context stack if it exists,
     * {@code 404} otherwise.
     */
    static class CheckAssignment implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final Role role;

        CheckAssignment(final DispatchAsync dispatcher, final Role role) {
            this.dispatcher = dispatcher;
            this.role = role;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            Operation operation = new Operation.Builder(READ_RESOURCE_OPERATION, AddressHelper.roleMapping(role))
                    .build();
            dispatcher.execute(new DMRAction(operation), new FunctionCallback(control) {
                @Override
                protected void proceed() {
                    // role mapping exists
                    control.getContext().push(200);
                    control.proceed();
                }

                @Override
                protected void abort() {
                    // no role mapping found
                    control.getContext().push(404);
                    control.proceed();
                }
            });
        }
    }


    /**
     * Adds an assignment for a given role if the predicate returns {@code true}, proceeds otherwise.
     * Expects an integer status code at the top of the context stack which is used to call the predicate.
     */
    static class AddAssignment implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final Role role;
        private final Predicate<Integer> predicate;

        AddAssignment(final DispatchAsync dispatcher, final Role role, final Predicate<Integer> predicate) {
            this.dispatcher = dispatcher;
            this.role = role;
            this.predicate = predicate;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            if (control.getContext().emptyStack()) {
                control.proceed();
            } else {
                Integer status = control.getContext().pop();
                if (predicate.apply(status)) {
                    Operation operation = new Operation.Builder(ADD, AddressHelper.roleMapping(role)).build();
                    dispatcher.execute(new DMRAction(operation), new FunctionCallback(control));
                } else {
                    control.proceed();
                }
            }
        }
    }


    /**
     * Removes an assignment for a given role if the predicate returns {@code true}, proceeds otherwise.
     * Expects an integer status code at the top of the context stack which is used to call the predicate.
     */
    static class RemoveAssignment implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final Role role;
        private final Predicate<Integer> predicate;

        RemoveAssignment(final DispatchAsync dispatcher, final Role role, final Predicate<Integer> predicate) {
            this.dispatcher = dispatcher;
            this.role = role;
            this.predicate = predicate;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            if (control.getContext().emptyStack()) {
                control.proceed();
            } else {
                Integer status = control.getContext().pop();
                if (predicate.apply(status)) {
                    Operation operation = new Operation.Builder(REMOVE, AddressHelper.roleMapping(role)).build();
                    dispatcher.execute(new DMRAction(operation), new FunctionCallback(control));
                } else {
                    control.proceed();
                }
            }
        }
    }


    /**
     * Modifies the include-all flag for the assignment of the given role.
     */
    static class ModifyIncludeAll implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final Role role;

        ModifyIncludeAll(final DispatchAsync dispatcher, final Role role) {
            this.dispatcher = dispatcher;
            this.role = role;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            Operation operation = new Operation.Builder(WRITE_ATTRIBUTE_OPERATION, AddressHelper.roleMapping(role))
                    .param(NAME, "include-all")
                    .param(VALUE, role.isIncludeAll())
                    .build();
            dispatcher.execute(new DMRAction(operation), new FunctionCallback(control));
        }
    }
}
