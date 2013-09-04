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

import java.util.Stack;

import org.jboss.as.console.client.administration.role.model.Principal;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.StandardRole;
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
public class AddRoleAssignmentOperation {

    private final DispatchAsync dispatcher;
    private StandardRole role;
    private RoleAssignment roleAssignment;
    private Principal principal;

    public AddRoleAssignmentOperation(final DispatchAsync dispatcher, final StandardRole role,
            final RoleAssignment roleAssignment, final Principal principal) {
        this.dispatcher = dispatcher;
        this.role = role;
        this.roleAssignment = roleAssignment;
        this.principal = principal;
    }

    public void extecute(final Outcome<Stack<Boolean>> outcome) {
        new Async<Stack<Boolean>>()
                .waterfall(new Stack<Boolean>(), outcome, new ReadRoleFunction(), new AddRoleFunction(),
                        new AddPrincipalFunction());
    }

    class ReadRoleFunction implements Function<Stack<Boolean>> {

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            final ModelNode realRoleOp = new ModelNode();
            realRoleOp.get(ADDRESS).add("core-service", "management");
            realRoleOp.get(ADDRESS).add("access", "authorization");
            realRoleOp.get(ADDRESS).add("role-mapping", role.name());
            realRoleOp.get(OP).set(READ_RESOURCE_OPERATION);

            dispatcher.execute(new DMRAction(realRoleOp), new SimpleCallback<DMRResponse>() {
                @Override
                public void onSuccess(DMRResponse response) {
                    // role exists - next step will skipp DMR operation
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

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            boolean roleExists = control.getContext().pop();
            if (roleExists) {
                control.proceed();
            } else {
                final ModelNode addRoleOp = new ModelNode();
                addRoleOp.get(ADDRESS).add("core-service", "management");
                addRoleOp.get(ADDRESS).add("access", "authorization");
                addRoleOp.get(ADDRESS).add("role-mapping", role.name());
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

    class AddPrincipalFunction implements Function<Stack<Boolean>> {

        @Override
        public void execute(final Control<Stack<Boolean>> control) {
            final ModelNode assignmentOp = new ModelNode();
            StringBuilder principalKey = new StringBuilder();
            boolean realmGiven = principal.getRealm() != null && principal.getRealm().length() != 0;
            principalKey.append(principal.getType().name().toLowerCase()).append("-").append(principal.getName());
            if (realmGiven) {
                principalKey.append("@").append(principal.getRealm());
            }
            assignmentOp.get(ADDRESS).add("core-service", "management");
            assignmentOp.get(ADDRESS).add("access", "authorization");
            assignmentOp.get(ADDRESS).add("role-mapping", role.name());
            assignmentOp.get(ADDRESS)
                    .add(/*roleAssignment.isInclude() ? */"include" /*: "exclude"*/, principalKey.toString());
            assignmentOp.get("name").set(ModelType.STRING, principal.getName());
            assignmentOp.get("type").set(ModelType.STRING, principal.getType().name());
            if (realmGiven) {
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
