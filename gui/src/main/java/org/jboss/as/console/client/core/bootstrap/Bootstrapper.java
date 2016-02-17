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
package org.jboss.as.console.client.core.bootstrap;

import com.google.gwt.core.client.Scheduler;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.core.bootstrap.cors.BootstrapServerSetup;
import org.jboss.as.console.client.core.bootstrap.hal.BootstrapSteps;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Outcome;

/**
 * Bootstrap process. At this point GIN is ready to be used.
 *
 * @author Harald Pehl
 */
public class Bootstrapper {

    private final BootstrapServerSetup serverSetup;
    private final BootstrapContext bootstrapContext;
    private final SecurityFramework securityFramework;
    private final BootstrapSteps bootstrapSteps;

    @Inject
    public Bootstrapper(BootstrapServerSetup serverSetup,
                        BootstrapContext bootstrapContext,
                        SecurityFramework securityFramework,
                        BootstrapSteps bootstrapSteps) {

        this.serverSetup = serverSetup;
        this.bootstrapContext = bootstrapContext;
        this.securityFramework = securityFramework;
        this.bootstrapSteps = bootstrapSteps;
    }

    public void go(final Outcome<BootstrapContext> outcome) {
        prepareSecurityContext(
                () -> serverSetup.select(
                        () -> new Async<BootstrapContext>().waterfall(bootstrapContext, outcome, bootstrapSteps.steps())));
    }

    private void prepareSecurityContext(Scheduler.ScheduledCommand andThen) {
        // creates an empty (always true) security context for the bootstrap steps
        /*securityFramework.createSecurityContext(null, Collections.<String>emptySet(), false,
                new AsyncCallback<SecurityContext>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        andThen.execute();
                    }

                    @Override
                    public void onSuccess(SecurityContext result) {
                        andThen.execute();
                    }
                });*/
        andThen.execute();
    }
}
