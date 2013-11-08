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

import org.jboss.as.console.client.administration.role.model.ModelHelper;
import org.jboss.as.console.client.administration.role.model.Role;
import org.jboss.as.console.client.administration.role.model.RoleAssignment;
import org.jboss.as.console.client.shared.flow.FunctionCallback;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.dmr.client.ModelDescriptionConstants;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

/**
 * Functions to check, and add (if not present) a role assignments; supposed to work inside a call to {@link
 * org.jboss.gwt.flow.client.Async#waterfall(Object, org.jboss.gwt.flow.client.Outcome,
 * org.jboss.gwt.flow.client.Function[])}
 *
 * @author Harald Pehl
 */
public final class RoleAssignmentFunctions {

    private RoleAssignmentFunctions() {}

    public static class Check implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final Role role;

        Check(final DispatchAsync dispatcher, final Role role) {
            this.dispatcher = dispatcher;
            this.role = role;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            ModelNode node = ModelHelper.roleMapping(role);
            node.get(OP).set(READ_RESOURCE_OPERATION);
            dispatcher.execute(new DMRAction(node), new FunctionCallback(control) {
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

    public static class Add implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final Role role;

        Add(final DispatchAsync dispatcher, final Role role) {
            this.dispatcher = dispatcher;
            this.role = role;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            boolean roleExists = Boolean.valueOf(control.getContext().pop().toString());
            if (roleExists) {
                control.proceed();
            } else {
                ModelNode node = ModelHelper.roleMapping(role);
                node.get(OP).set(ADD);
                dispatcher.execute(new DMRAction(node), new FunctionCallback(control));
            }
        }
    }

    public static class IncludeAll implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final Role role;

        public IncludeAll(final DispatchAsync dispatcher, final Role role) {
            this.dispatcher = dispatcher;
            this.role = role;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            ModelNode node = ModelHelper.roleMapping(role);
            node.get(NAME).set("include-all");
            node.get(VALUE).set(role.isIncludeAll());
            node.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            dispatcher.execute(new DMRAction(node), new FunctionCallback(control));
        }
    }

    public static class Remove implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final RoleAssignment assignment;

        public Remove(final DispatchAsync dispatcher, final RoleAssignment assignment) {
            this.dispatcher = dispatcher;
            this.assignment = assignment;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            ModelNode node = new ModelNode();
            node.get(ADDRESS).setEmptyList();
            node.get(OP).set(COMPOSITE);
            List<ModelNode> steps = new LinkedList<ModelNode>();

            for (Role role : assignment.getRoles()) {
                ModelNode step = ModelHelper.includeExclude(role, assignment.getPrincipal(), "include");
                step.get(OP).set(REMOVE);
                steps.add(step);
            }
            for (Role exclude : assignment.getExcludes()) {
                ModelNode step = ModelHelper.includeExclude(exclude, assignment.getPrincipal(), "exclude");
                step.get(OP).set(REMOVE);
                steps.add(step);
            }

            node.get(STEPS).set(steps);
            dispatcher.execute(new DMRAction(node), new FunctionCallback(control));
        }
    }

    public static class Find implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;
        private final String name;

        public Find(final DispatchAsync dispatcher) {
            this(dispatcher, null);
        }

        public Find(final DispatchAsync dispatcher, final String name) {
            this.dispatcher = dispatcher;
            this.name = name;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            final List<String> matchingRoles = new LinkedList<String>();
            control.getContext().push(matchingRoles);

            ModelNode node = new ModelNode();
            node.get(ADDRESS).add("core-service", "management");
            node.get(ADDRESS).add("access", "authorization");
            node.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
            node.get(CHILD_TYPE).set("role-mapping");
            node.get("recursive-depth").set("2");
            dispatcher.execute(new DMRAction(node), new FunctionCallback(control) {

                @Override
                protected void onSuccess(final ModelNode result) {
                    List<ModelNode> roleMappings = result.get(RESULT).asList();
                    for (ModelNode node : roleMappings) {
                        Property property = node.asProperty();
                        String roleName = property.getName();

                        if (name != null) {
                            if (name.equals(roleName)) {
                                matchingRoles.add(roleName);
                                break;
                            }
                        } else {
                            // check if the role is empty and does not have include-all=true
                            boolean match = true;
                            ModelNode assignmentNode = property.getValue();
                            if (assignmentNode.hasDefined("include-all")) {
                                match = !assignmentNode.get("include-all").asBoolean();
                            }
                            if (match) {
                                if (assignmentNode.hasDefined("include")) {
                                    List<Property> inclusions = assignmentNode.get("include").asPropertyList();
                                    match = inclusions.isEmpty();
                                }
                                if (match) {
                                    if (assignmentNode.hasDefined("exclude")) {
                                        List<Property> exclusions = assignmentNode.get("exclude").asPropertyList();
                                        match = exclusions.isEmpty();
                                    }
                                }
                            }
                            if (match) {
                                matchingRoles.add(roleName);
                            }
                        }
                    }
                }
            });
        }
    }

    public static class RemoveMatching implements Function<FunctionContext> {

        private final DispatchAsync dispatcher;

        public RemoveMatching(final DispatchAsync dispatcher) {this.dispatcher = dispatcher;}

        @Override
        @SuppressWarnings("unchecked")
        public void execute(final Control<FunctionContext> control) {
            List<String> roles = control.getContext().pop();
            if (roles.isEmpty()) {
                control.proceed();
            } else {
                ModelNode comp = new ModelNode();
                comp.get(ADDRESS).setEmptyList();
                comp.get(OP).set(COMPOSITE);
                List<ModelNode> steps = new LinkedList<ModelNode>();
                for (String role : roles) {
                    ModelNode node = new ModelNode();
                    node.get(ADDRESS).add("core-service", "management");
                    node.get(ADDRESS).add("access", "authorization");
                    node.get(ADDRESS).add("role-mapping", role);
                    node.get(OP).set(ModelDescriptionConstants.REMOVE);
                    steps.add(node);
                }
                comp.get(STEPS).set(steps);
                dispatcher.execute(new DMRAction(comp), new FunctionCallback(control));
            }
        }
    }
}
