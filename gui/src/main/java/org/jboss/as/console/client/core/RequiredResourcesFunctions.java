/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.core;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.plugins.RequiredResourcesRegistry;
import org.jboss.as.console.client.rbac.ReadOnlyContext;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.dmr.ResourceDefinition;
import org.jboss.as.console.mbui.widgets.ModelDrivenContext;
import org.jboss.as.console.mbui.widgets.ModelDrivenRegistry;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
class RequiredResourcesFunctions {

    static class CreateSecurityContext implements Function<FunctionContext> {

        private final String token;
        private final SecurityFramework securityFramework;

        CreateSecurityContext(String token, SecurityFramework securityFramework) {
            this.token = token;
            this.securityFramework = securityFramework;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            SecurityContext context = securityFramework.getSecurityContext(token);
            if (context == null || (context instanceof ReadOnlyContext)) {
                securityFramework.createSecurityContext(token, new AsyncCallback<SecurityContext>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        control.getContext().setError(caught);
                        control.abort();
                    }

                    @Override
                    public void onSuccess(SecurityContext securityContext) {
                        // security context was successfully registered in SecurityFramework
                        control.proceed();
                    }
                });
            } else {
                control.proceed();
            }
        }
    }


    static class ReadResourceDescriptions implements Function<FunctionContext> {

        private final String token;
        private final RequiredResourcesRegistry rrRegistry;
        private final ModelDrivenRegistry mdRegistry;
        private final DispatchAsync dispatcher;
        private final StatementContext statementContext;

        ReadResourceDescriptions(String token, RequiredResourcesRegistry rrRegistry, ModelDrivenRegistry mdRegistry,
                                 DispatchAsync dispatcher, StatementContext statementContext) {
            this.token = token;
            this.rrRegistry = rrRegistry;
            this.mdRegistry = mdRegistry;
            this.dispatcher = dispatcher;
            this.statementContext = statementContext;
        }

        @Override
        public void execute(final Control<FunctionContext> control) {
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(COMPOSITE);
            operation.get(ADDRESS).setEmptyList();

            final List<ModelNode> steps = new LinkedList<ModelNode>();
            final Map<String, ModelDrivenContext> step2context = new HashMap<>();
            for (String template : rrRegistry.getResources(token)) {
                // Skip already registered model driven contexts
                if (mdRegistry.contains(template)) {
                    continue;
                }

                ResourceAddress address = new ResourceAddress(template, statementContext);
                ModelDrivenContext context = new ModelDrivenContext(template, address);
                step2context.put("step-" + (steps.size() + 1), context);

                ModelNode node = address.clone();
                node.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
                node.get(OPERATIONS).set(true);
                steps.add(node);
            }

            if (!steps.isEmpty()) {
                operation.get(STEPS).set(steps);
                dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        control.getContext().setError(caught);
                        control.abort();
                    }

                    @Override
                    public void onSuccess(DMRResponse dmr) {
                        ModelNode response = dmr.get();
                        if (response.isFailure()) {
                            control.getContext().setError(new RuntimeException(response.getFailureDescription()));
                            control.abort();
                            return;
                        }

                        ModelNode compositeResult = response.get(RESULT);
                        for (int i = 1; i <= steps.size(); i++) {
                            String step = "step-" + i;
                            if (compositeResult.hasDefined(step)) {
                                ModelNode stepResult = compositeResult.get(step).get(RESULT);
                                ModelNode description;

                                // it's a List response when asking for '<resourceType>=*"
                                if (stepResult.getType() == ModelType.LIST) {
                                    List<ModelNode> nodes = stepResult.asList();
                                    // TODO: exactly match and verify address (this is an assumption)
                                    description = nodes.get(0).get(RESULT);
                                    if (description == null) {
                                        control.getContext().setError(new RuntimeException("Unexpected response format"));
                                        control.abort();
                                    }
                                } else {
                                    description = stepResult;
                                }
                                ModelDrivenContext context = step2context.get(step);
                                context.setDefinition(new ResourceDefinition(description));
                                mdRegistry.add(context);
                            }
                        }
                        control.proceed();
                    }
                });
            } else {
                // all model driven contexts already registered -> no need to dispatch any async calls
                control.proceed();
            }
        }
    }
}
