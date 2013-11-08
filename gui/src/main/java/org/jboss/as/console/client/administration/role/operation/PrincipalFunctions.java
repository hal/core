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

import org.jboss.as.console.client.administration.role.model.ModelHelper;
import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.Role;
import org.jboss.as.console.client.shared.flow.FunctionCallback;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

/**
 * Functions to add and remove a principal to an include / exclude section of a role assignment.
 *
 * @author Harald Pehl
 */
public final class PrincipalFunctions {

    private PrincipalFunctions() {}

    public static class Check implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final Role role;
        private final Principal principal;
        private final String realm;
        private final String includeExclude;

        public Check(final DispatchAsync dispatcher, final Role role,
                final Principal principal, final String realm, final String includeExclude) {
            this.dispatcher = dispatcher;
            this.role = role;
            this.principal = principal;
            this.realm = realm;
            this.includeExclude = includeExclude;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            ModelNode node = ModelHelper.includeExclude(role, principal, includeExclude);
            node.get(OP).set(READ_RESOURCE_OPERATION);
            dispatcher.execute(new DMRAction(node), new FunctionCallback(control){
                @Override
                protected void proceed() {
                    // principal exists - next function will skip its DMR operation
                    control.getContext().push(true);
                    control.proceed();
                }

                @Override
                protected void abort() {
                    // no principal - create it in the next function
                    control.getContext().push(false);
                    control.proceed();
                }
            });
        }
    }

    public static class Add implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final Role role;
        private final Principal principal;
        private final String realm;
        private final String includeExclude;

        public Add(final DispatchAsync dispatcher, final Role role,
                final Principal principal, final String realm, final String includeExclude) {
            this.dispatcher = dispatcher;
            this.role = role;
            this.principal = principal;
            this.realm = realm;
            this.includeExclude = includeExclude;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            boolean principalExists = Boolean.valueOf(control.getContext().pop().toString());
            if (principalExists) {
                control.proceed();
            } else {
                ModelNode node = ModelHelper.includeExclude(role, principal, includeExclude);
                node.get("name").set(ModelType.STRING, principal.getName());
                node.get("type").set(ModelType.STRING, principal.getType().name());
                if (realm != null && realm.length() != 0) {
                    node.get("realm").set(ModelType.STRING, realm);
                }
                node.get(OP).set(ADD);
                dispatcher.execute(new DMRAction(node), new FunctionCallback(control));
            }
        }
    }

    public static class Remove implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final Role role;
        private final Principal principal;
        private final String includeExclude;

        public Remove(final DispatchAsync dispatcher, final Role role, final Principal principal,
                final String includeExclude) {
            this.dispatcher = dispatcher;
            this.role = role;
            this.principal = principal;
            this.includeExclude = includeExclude;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            ModelNode node = ModelHelper.includeExclude(role, principal, includeExclude);
            node.get(OP).set(REMOVE);
            dispatcher.execute(new DMRAction(node), new FunctionCallback(control));
        }
    }
}
