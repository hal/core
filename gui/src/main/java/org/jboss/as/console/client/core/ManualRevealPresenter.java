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

import com.google.gwt.user.client.Command;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.ManualRevealFunctions.CreateSecurityContext;
import org.jboss.as.console.client.core.ManualRevealFunctions.ReadResourceDescriptions;
import org.jboss.as.console.client.plugins.RequiredResourcesRegistry;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.mbui.widgets.ModelDrivenRegistry;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.flow.client.Progress;
import org.useware.kernel.gui.behaviour.StatementContext;

/**
 * @author Harald Pehl
 */
public abstract class ManualRevealPresenter<V extends View, Proxy_ extends ProxyPlace<?>> extends Presenter<V, Proxy_> {

    private final boolean rrd;

    // TODO Replace "static" injections
    private final Progress progress = Footer.PROGRESS_ELEMENT;
    private final DispatchAsync dispatcher = Console.MODULES.getDispatchAsync();
    private final StatementContext statementContext = Console.MODULES.getCoreGUIContext();
    private final NameTokenRegistry nameTokenRegistry = Console.MODULES.getNameTokenRegistry();
    private final SecurityFramework securityFramework = Console.MODULES.getSecurityFramework();
    private final ModelDrivenRegistry modelDrivenRegistry = Console.MODULES.getModelDrivenRegistry();
    private final RequiredResourcesRegistry requiredResourcesRegistry = Console.MODULES.getRequiredResourcesRegistry();

    protected ManualRevealPresenter(EventBus eventBus, V view, Proxy_ proxy, boolean rrd) {
        super(eventBus, view, proxy);
        this.rrd = rrd;
    }

    @Override
    public final boolean useManualReveal() {
        return true;
    }

    @Override
    public final void prepareFromRequest(final PlaceRequest request) {
        super.prepareFromRequest(request);

        final String token = request.getNameToken();
        if (!nameTokenRegistry.wasRevealed(token)) {
            ManualRevealFlow flow = new ManualRevealFlow(this, progress);
            flow.addFunction(new CreateSecurityContext(token, securityFramework));
            if (rrd) {
                flow.addFunction(new ReadResourceDescriptions(token, requiredResourcesRegistry, modelDrivenRegistry,
                        dispatcher, statementContext));
            }
            flow.execute(new Command() {
                @Override
                public void execute() {
                    nameTokenRegistry.reveal(token);
                    withRequest(request);
                }
            });
        } else {
            withRequest(request);
        }
    }

    protected void withRequest(final PlaceRequest request) {
        // noop
    }
}
