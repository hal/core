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

import com.google.gwt.inject.client.AsyncProvider;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import org.jboss.as.console.client.plugins.RequiredResourcesRegistry;
import org.jboss.as.console.client.rbac.NoGatekeeperContext;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.widgets.ResourceDescriptionRegistry;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Outcome;
import org.useware.kernel.gui.behaviour.FilteringStatementContext;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.ArrayList;
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
    private final PlaceManager placeManager;
    private final RequiredResourcesRegistry requiredResourcesRegistry;
    private final NameTokenRegistry nameTokenRegistry;
    private final ResourceDescriptionRegistry resourceDescriptionRegistry;
    private final SecurityFramework securityFramework;
    private final StatementContext statementContext;

    @Inject
    protected RequiredResourcesProcessor(DispatchAsync dispatcher, PlaceManager placeManager, BootstrapContext bootstrap,
                                         NameTokenRegistry nameTokenRegistry, RequiredResourcesRegistry requiredResourcesRegistry,
                                         ResourceDescriptionRegistry resourceDescriptionRegistry,  SecurityFramework securityFramework,
                                         CoreGUIContext statementContext) {
        this.dispatcher = dispatcher;
        this.placeManager = placeManager;
        this.nameTokenRegistry = nameTokenRegistry;
        this.requiredResourcesRegistry = requiredResourcesRegistry;
        this.resourceDescriptionRegistry = resourceDescriptionRegistry;
        this.securityFramework = securityFramework;
        this.statementContext = new FilteringStatementContext(statementContext, new RequiredResourcesFilter(bootstrap));
    }

    public <P extends Presenter<?, ?>> void process(final AsyncProvider<P> provider, final AsyncCallback<P> callback) {
        final String token = placeManager.getCurrentPlaceRequest().getNameToken();

        if (!nameTokenRegistry.wasRevealed(token)) {
            Set<String> requiredResources = requiredResourcesRegistry.getResources(token);
            if (requiredResources.isEmpty()) {
                finishWithContext(token, new NoGatekeeperContext(), provider, callback);

            } else {
                List<ReadRequiredResources> functions = partition(requiredResources, BATCH_SIZE);
                RequiredResourcesContext context = new RequiredResourcesContext(token, requiredResources,
                        resourceDescriptionRegistry);
                Outcome<RequiredResourcesContext> outcome = new Outcome<RequiredResourcesContext>() {
                    @Override
                    public void onFailure(RequiredResourcesContext context) {
                        callback.onFailure(context.getError());
                    }

                    @Override
                    public void onSuccess(RequiredResourcesContext context) {
                        finishWithContext(token, context.getSecurityContext(), provider, callback);
                    }
                };
                //noinspection unchecked
                new Async<RequiredResourcesContext>(Footer.PROGRESS_ELEMENT).parallel(context, outcome,
                        functions.toArray(new ReadRequiredResources[functions.size()]));
            }
        } else {
            provider.get(callback);
        }
    }

    private <P extends Presenter<?, ?>> void finishWithContext(String token, SecurityContext securityContext,
                                                               AsyncProvider<P> provider, AsyncCallback<P> callback) {
        nameTokenRegistry.revealed(token);
        securityContext.seal();
        securityFramework.assignContext(token, securityContext);
        provider.get(callback);
    }

    private List<ReadRequiredResources> partition(Set<String> requiredResources, int batchSize) {
        int index = 0;
        List<ReadRequiredResources> functions = new ArrayList<>();

        for (Iterator<String> iterator = requiredResources.iterator(); iterator.hasNext(); index++) {
            ReadRequiredResources rrr = null;
            if (index % batchSize == 0) {
                rrr = new ReadRequiredResources(dispatcher, statementContext);
                functions.add(rrr);
            }
            assert rrr != null;
            rrr.add(iterator.next());
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
                case "addressable.group":
                    return bootstrap.getAddressableGroups().isEmpty() ? "*" : bootstrap
                            .getAddressableGroups().iterator().next();
                case "addressable.host":
                    return bootstrap.getAddressableHosts().isEmpty() ? "*" : bootstrap.getAddressableHosts()
                            .iterator().next();
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
