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
import com.google.inject.Inject;
import org.jboss.as.console.client.plugins.RequiredResourcesRegistry;
import org.jboss.as.console.client.rbac.NoGatekeeperContext;
import org.jboss.as.console.client.rbac.SecurityContextImpl;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Outcome;
import org.useware.kernel.gui.behaviour.FilteringStatementContext;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Harald Pehl
 */
public class RequiredResourcesProcessor {

    /**
     * Number of required resources which are loaded as part of one composite operation.
     */
    private final static int BATCH_SIZE = 3;

    private final DispatchAsync dispatcher;
    private final RequiredResourcesRegistry requiredResourcesRegistry;
    private final NameTokenRegistry nameTokenRegistry;
    private final ResourceDescriptionRegistry resourceDescriptionRegistry;
    private final SecurityFramework securityFramework;
    private final StatementContext statementContext;

    @Inject
    protected RequiredResourcesProcessor(DispatchAsync dispatcher,
                                         BootstrapContext bootstrap,
                                         NameTokenRegistry nameTokenRegistry,
                                         RequiredResourcesRegistry requiredResourcesRegistry,
                                         ResourceDescriptionRegistry resourceDescriptionRegistry,
                                         SecurityFramework securityFramework,
                                         CoreGUIContext statementContext) {
        this.dispatcher = dispatcher;
        this.nameTokenRegistry = nameTokenRegistry;
        this.requiredResourcesRegistry = requiredResourcesRegistry;
        this.resourceDescriptionRegistry = resourceDescriptionRegistry;
        this.securityFramework = securityFramework;
        this.statementContext = new FilteringStatementContext(statementContext, new RequiredResourcesFilter(bootstrap));
    }

    public void process(final String token, final AsyncCallback<Void> callback) {
        if (!nameTokenRegistry.wasRevealed(token)) {

            Set<AddressTemplate> requiredResources = new HashSet<>();
            for (String s : requiredResourcesRegistry.getResources(token)) {
                requiredResources.add(AddressTemplate.of(s));
            }

            boolean recursive = requiredResourcesRegistry.isRecursive(token);
            if (requiredResources.isEmpty()) {
                finishWithContext(token, new NoGatekeeperContext(), callback);

            } else {
                List<ReadRequiredResources> functions = partition(requiredResources, recursive, BATCH_SIZE);

                System.out.println("Num partitions: "+functions.size());

                RequiredResourcesContext context = new RequiredResourcesContext(token);
                Outcome<RequiredResourcesContext> outcome = new Outcome<RequiredResourcesContext>() {
                    @Override
                    public void onFailure(RequiredResourcesContext context) {
                        callback.onFailure(context.getError());
                    }

                    @Override
                    public void onSuccess(RequiredResourcesContext context) {

                        // push to registry
                        for (AddressTemplate addressTemplate : context.getDescriptions().keySet()) {
                            resourceDescriptionRegistry.add(addressTemplate, context.getDescriptions().get(addressTemplate));
                        }

                        finishWithContext(token, context, callback);
                    }
                };
                //noinspection unchecked
                // Unfortunately we cannot use Async.parallel() here unless someone finds a way
                // to unambiguously map parallel r-r-d operations to their results (multiple "step-1" results)
                new Async<RequiredResourcesContext>(Footer.PROGRESS_ELEMENT).waterfall(context, outcome,
                        functions.toArray(new ReadRequiredResources[functions.size()]));
            }
        } else {
            callback.onSuccess(null);
        }
    }

    private void finishWithContext(String token, SecurityContext securityContext, AsyncCallback<Void> callback) {
        nameTokenRegistry.revealed(token);
        securityFramework.assignContext(token, securityContext);
        callback.onSuccess(null);
    }

    private void finishWithContext(String token, RequiredResourcesContext context, AsyncCallback<Void> callback) {
        nameTokenRegistry.revealed(token);
        SecurityContextImpl securityContext = new SecurityContextImpl(token, context.getDescriptions().keySet());
        context.mergeWith(securityContext);
        securityFramework.assignContext(token, securityContext);
        callback.onSuccess(null);
    }

    private List<ReadRequiredResources> partition(Set<AddressTemplate> requiredResources, boolean recursive, int batchSize) {
        int index = 0;
        List<ReadRequiredResources> functions = new ArrayList<>();

        ReadRequiredResources rrr = null;
        for (Iterator<AddressTemplate> iterator = requiredResources.iterator(); iterator.hasNext(); index++) {
            if (index % batchSize == 0) {
                rrr = new ReadRequiredResources(dispatcher, statementContext);
                functions.add(rrr);
            }
            assert rrr != null;
            rrr.add(iterator.next(), recursive);
        }
        return functions;
    }


    // ------------------------------------------------------ inner classes

    private static class RequiredResourcesFilter implements FilteringStatementContext.Filter {

        private final BootstrapContext bootstrap;

        private RequiredResourcesFilter(BootstrapContext bootstrap) {
            this.bootstrap = bootstrap;
        }

        @Override
        public String filter(String key) {
            switch (key) {
                case "selected.entity":
                    return "*";
                default:
                    return null;
            }
        }

        @Override
        public String[] filterTuple(String key) {
            return null;
        }
    }
}
