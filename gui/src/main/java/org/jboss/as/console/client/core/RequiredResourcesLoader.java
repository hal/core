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
import org.jboss.as.console.client.core.RequiredResourcesFunctions.CreateSecurityContext;
import org.jboss.as.console.client.core.RequiredResourcesFunctions.ReadResourceDescriptions;
import org.jboss.as.console.client.plugins.RequiredResourcesRegistry;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.widgets.ModelDrivenRegistry;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Outcome;
import org.useware.kernel.gui.behaviour.StatementContext;

/**
 * @author Harald Pehl
 */
public class RequiredResourcesLoader {

    private final DispatchAsync dispatcher;
    private final PlaceManager placeManager;
    private final RequiredResourcesRegistry requiredResourcesRegistry;
    private final ModelDrivenRegistry modelDrivenRegistry;
    private final NameTokenRegistry nameTokenRegistry;
    private final SecurityFramework securityFramework;
    private final StatementContext statementContext;

    @Inject
    protected RequiredResourcesLoader(DispatchAsync dispatcher, PlaceManager placeManager,
                                      RequiredResourcesRegistry requiredResourcesRegistry,
                                      ModelDrivenRegistry modelDrivenRegistry, NameTokenRegistry nameTokenRegistry,
                                      SecurityFramework securityFramework, CoreGUIContext statementContext) {
        this.dispatcher = dispatcher;
        this.placeManager = placeManager;
        this.requiredResourcesRegistry = requiredResourcesRegistry;
        this.modelDrivenRegistry = modelDrivenRegistry;
        this.nameTokenRegistry = nameTokenRegistry;
        this.securityFramework = securityFramework;
        this.statementContext = statementContext;
    }

    public <P extends Presenter<?, ?>> void loadRequiredResources(final AsyncProvider<P> provider, final AsyncCallback<P> callback) {
        final String token = placeManager.getCurrentPlaceRequest().getNameToken();

        if (!nameTokenRegistry.wasRevealed(token)) {
            Outcome<FunctionContext> outcome = new Outcome<FunctionContext>() {
                @Override
                public void onFailure(FunctionContext context) {
                    callback.onFailure(context.getError());
                }

                @Override
                public void onSuccess(FunctionContext context) {
                    nameTokenRegistry.revealed(token);
                    provider.get(callback);
                }
            };
            // TODO What functions to execute here?
            new Async<FunctionContext>(Footer.PROGRESS_ELEMENT).parallel(new FunctionContext(), outcome,
                    new CreateSecurityContext(token, securityFramework),
                    new ReadResourceDescriptions(token, requiredResourcesRegistry, modelDrivenRegistry, dispatcher, statementContext));
        } else {
            provider.get(callback);
        }
    }
}
