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

import static java.util.Arrays.asList;
import static org.jboss.as.console.client.administration.role.model.Role.Type.HOST;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.jboss.as.console.client.administration.role.model.ModelHelper;
import org.jboss.as.console.client.administration.role.model.Role;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

/**
 * Functions to add, modify and remove scoped roles.
 *
 * @author Harald Pehl
 */
public final class ScopedRoleFunctions {

    private ScopedRoleFunctions() {}

    static class Add implements Function<Stack<Object>> {

        private final DispatchAsync dispatcher;
        private final Role role;

        protected Add(final DispatchAsync dispatcher, final Role role) {
            this.dispatcher = dispatcher;
            this.role = role;
        }

        @Override
        public void execute(final Control<Stack<Object>> control) {
            ModelNode node = ModelHelper.scopedRole(role);
            node.get("base-role").set(role.getBaseRole().name());
            String scope = role.getType() == HOST ? "hosts" : "server-groups";
            for (String s : role.getScope()) {
                node.get(scope).add(s);
            }
            node.get(OP).set(ADD);
            dispatcher.execute(new DMRAction(node), new FunctionCallback<Stack<Object>>(control));
        }
    }

    public static class Modify implements Function<Stack<Object>> {

        private final DispatchAsync dispatcher;
        private final Role role;

        public Modify(final DispatchAsync dispatcher, final Role role) {
            this.dispatcher = dispatcher;
            this.role = role;
        }

        @Override
        public void execute(final Control<Stack<Object>> control) {
            ModelNode baseRoleNode = ModelHelper.scopedRole(role);
            baseRoleNode.get(NAME).set("base-role");
            baseRoleNode.get(VALUE).set(role.getBaseRole().name());
            baseRoleNode.get(OP).set(WRITE_ATTRIBUTE_OPERATION);

            ModelNode scopeNode = ModelHelper.scopedRole(role);
            String scope = role.getType() == HOST ? "hosts" : "server-groups";
            scopeNode.get(NAME).set(scope);
            for (String s : role.getScope()) {
                scopeNode.get(VALUE).add(s);
            }
            scopeNode.get(OP).set(WRITE_ATTRIBUTE_OPERATION);

            ModelNode compositeNode = new ModelNode();
            compositeNode.get(OP).set(COMPOSITE);
            compositeNode.get(ADDRESS).setEmptyList();
            List<ModelNode> steps = new ArrayList<ModelNode>();
            steps.addAll(asList(baseRoleNode, scopeNode));
            compositeNode.get(STEPS).set(steps);
            dispatcher.execute(new DMRAction(compositeNode), new FunctionCallback<Stack<Object>>(control));
        }
    }

    public static class Remove implements Function<Stack<Object>> {

        private final DispatchAsync dispatcher;
        private final Role role;

        protected Remove(final DispatchAsync dispatcher, final Role role) {
            this.dispatcher = dispatcher;
            this.role = role;
        }

        @Override
        public void execute(final Control<Stack<Object>> control) {
            ModelNode node = ModelHelper.scopedRole(role);
            node.get(OP).set(ModelDescriptionConstants.REMOVE);
            dispatcher.execute(new DMRAction(node), new FunctionCallback<Stack<Object>>(control));
        }
    }
}
