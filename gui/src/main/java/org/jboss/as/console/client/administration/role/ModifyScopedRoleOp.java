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

import static java.util.Arrays.asList;
import static org.jboss.as.console.client.administration.role.model.ScopedRole.Type.HOST;
import static org.jboss.as.console.client.administration.role.model.ScopedRole.Type.SERVER_GROUP;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.console.client.administration.role.model.ScopedRole;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.dmr.client.ModelDescriptionConstants;
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
public class ModifyScopedRoleOp implements ManagementOperation<Object> {

    private final DispatchAsync dispatcher;
    private final ScopedRole scopedRole;
    private final ScopedRole oldValue;
    private final Operation op;

    public ModifyScopedRoleOp(final DispatchAsync dispatcher, final ScopedRole scopedRole,
            final ScopedRole oldValue, final Operation op) {
        this.dispatcher = dispatcher;
        this.scopedRole = scopedRole;
        this.oldValue = oldValue;
        this.op = op;
    }

    @Override
    public void extecute(final Outcome<Object> outcome) {
        List<Function<Object>> functions = new ArrayList<Function<Object>>();
        switch (op) {
            case ADD:
                functions.add(new AddRoleFunction(scopedRole));
                break;
            case RENAME:
                functions.add(new RemoveRoleFunction(oldValue));
                functions.add(new AddRoleFunction(scopedRole));
                break;
            case MODIFY:
                functions.add(new ModifyRoleFunction(scopedRole));
                break;
            case REMOVE:
                functions.add(new RemoveRoleFunction(scopedRole));
                break;
        }
        new Async<Object>().series(outcome, functions.toArray(new Function[functions.size()]));
    }

    @Override
    public boolean isPending() {
        throw new UnsupportedOperationException("not implemented");
    }

    abstract class ScopedRoleFunction<T> implements Function<T> {

        protected final ScopedRole role;

        protected ScopedRoleFunction(final ScopedRole role) {this.role = role;}

        protected ModelNode roleNode() {
            ModelNode node = new ModelNode();
            node.get(ADDRESS).add("core-service", "management");
            node.get(ADDRESS).add("access", "authorization");
            if (role.getType() == HOST) {
                node.get(ADDRESS).add("host-scoped-role", role.getName());
            } else if (role.getType() == SERVER_GROUP) {
                node.get(ADDRESS).add("server-group-scoped-role", role.getName());
            }
            return node;
        }
    }

    class AddRoleFunction extends ScopedRoleFunction<Object> {

        protected AddRoleFunction(final ScopedRole role) {
            super(role);
        }

        @Override
        public void execute(final Control<Object> control) {
            ModelNode roleNode = roleNode();
            roleNode.get("base-role").set(role.getBaseRole().name());
            String scope = role.getType() == HOST ? "hosts" : "server-groups";
            for (String s : role.getScope()) {
                roleNode.get(scope).add(s);
            }
            roleNode.get(OP).set(ADD);

            dispatcher.execute(new DMRAction(roleNode), new SimpleCallback<DMRResponse>() {
                @Override
                public void onFailure(final Throwable caught) {
                    control.abort();
                }

                @Override
                public void onSuccess(DMRResponse response) {
                    control.proceed();
                }
            });
        }
    }

    class ModifyRoleFunction extends ScopedRoleFunction<Object> {

        protected ModifyRoleFunction(final ScopedRole role) {
            super(role);
        }

        @Override
        public void execute(final Control<Object> control) {
            ModelNode baseRoleNode = ModelHelper.scopedRole(role);
            baseRoleNode.get("base-role").set(role.getBaseRole().name());
            baseRoleNode.get(OP).set(WRITE_ATTRIBUTE_OPERATION);

            ModelNode scopeNode = ModelHelper.scopedRole(role);
            String scope = role.getType() == HOST ? "hosts" : "server-groups";
            for (String s : role.getScope()) {
                scopeNode.get(scope).add(s);
            }
            scopeNode.get(OP).set(WRITE_ATTRIBUTE_OPERATION);

            ModelNode compositeNode = new ModelNode();
            compositeNode.get(OP).set(COMPOSITE);
            compositeNode.get(ADDRESS).setEmptyList();
            List<ModelNode> steps = new ArrayList<ModelNode>();
            steps.addAll(asList(baseRoleNode, scopeNode));
            compositeNode.get(STEPS).set(steps);

            dispatcher.execute(new DMRAction(compositeNode), new SimpleCallback<DMRResponse>() {
                @Override
                public void onFailure(final Throwable caught) {
                    control.abort();
                }

                @Override
                public void onSuccess(DMRResponse response) {
                    control.proceed();
                }
            });
        }
    }

    class RemoveRoleFunction extends ScopedRoleFunction<Object> {

        protected RemoveRoleFunction(final ScopedRole role) {
            super(role);
        }

        @Override
        public void execute(final Control<Object> control) {
            ModelNode roleNode = ModelHelper.scopedRole(role);
            roleNode.get(OP).set(ModelDescriptionConstants.REMOVE);

            dispatcher.execute(new DMRAction(roleNode), new SimpleCallback<DMRResponse>() {
                @Override
                public void onFailure(final Throwable caught) {
                    control.abort();
                }

                @Override
                public void onSuccess(DMRResponse response) {
                    control.proceed();
                }
            });
        }
    }
}
