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
package org.jboss.as.console.client.shared.patching.wizard;

import com.google.gwt.user.client.ui.IsWidget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.topology.LifecycleCallback;
import org.jboss.dmr.client.dispatch.DispatchAsync;

/**
 * @author Harald Pehl
 */
public class StoppingServersStep extends WizardStep {

    private final DispatchAsync dispatcher;

    public StoppingServersStep(final ApplyPatchWizard wizard, final DispatchAsync dispatcher) {
        super(wizard, Console.CONSTANTS.patch_manager_stopping_servers_title());
        this.dispatcher = dispatcher;
    }

    @Override
    protected IsWidget body() {
        return new Pending(Console.CONSTANTS.patch_manager_stopping_servers_body());
    }

    @Override
    void onShow(final WizardContext context) {
        // reset old state
        context.stopFailed = false;
        context.stopError = null;
        context.stopErrorDetails = null;

        // stop running servers
        new StopServersOp(dispatcher, context.host, context.runningServers, new LifecycleCallback() {
            @Override
            public void onSuccess() {
                wizard.next();
            }

            @Override
            public void onTimeout() {
                wizard.context.stopFailed = true;
                wizard.context.stopError = Console.CONSTANTS.patch_manager_stop_server_timeout();
                wizard.next();
            }

            @Override
            public void onAbort() {
                // must never be called!
            }

            @Override
            public void onError(final Throwable caught) {
                    wizard.context.stopFailed = true;
                wizard.context.stopError = Console.CONSTANTS.patch_manager_stop_server_unknown_error();
                wizard.context.stopErrorDetails = caught.getMessage();
                wizard.next();
            }
        }).run();
    }
}
