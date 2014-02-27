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

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

import java.util.LinkedList;
import java.util.List;

import com.google.gwt.user.client.ui.IsWidget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.flow.TimeoutOperation;
import org.jboss.as.console.client.shared.patching.StopServersOp;
import org.jboss.as.console.client.shared.patching.ui.Pending;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;

/**
 * @author Harald Pehl
 */
public class StoppingServersStep<C extends CommonPatchContext, S extends Enum<S>> extends PatchWizardStep<C, S> {

    private final DispatchAsync dispatcher;

    public StoppingServersStep(final PatchWizard<C, S> wizard, final DispatchAsync dispatcher) {
        super(wizard, Console.CONSTANTS.patch_manager_stop_server_title(), new WizardButton(false),
                new WizardButton(Console.CONSTANTS.common_label_cancel()));
        this.dispatcher = dispatcher;
    }

    @Override
    protected IsWidget body(final C context) {
        return new Pending(Console.CONSTANTS.patch_manager_stopping_servers_body());
    }

    @Override
    protected void onShow(final C context) {
        // reset old state
        context.stopFailed = false;
        context.stopError = null;
        context.stopErrorDetails = null;

        final ModelNode stopServersNode = new ModelNode();
        stopServersNode.get(ADDRESS).setEmptyList();
        stopServersNode.get(OP).set(COMPOSITE);
        List<ModelNode> steps = new LinkedList<ModelNode>();
        for (String server : context.runningServers) {
            ModelNode stop = new ModelNode();
            stop.get(ADDRESS).add("host", context.host);
            stop.get(ADDRESS).add("server-config", server);
            stop.get(OP).set("stop");
            steps.add(stop);
        }
        stopServersNode.get(STEPS).set(steps);

        TimeoutOperation stopServersOp = new StopServersOp(dispatcher, context.host, context.runningServers);
        stopServersOp.start(dispatcher, stopServersNode, new TimeoutOperation.Callback() {
            @Override
            public void onSuccess() {
                context.serversStopped = true;
                wizard.next();
            }

            @Override
            public void onTimeout() {
                context.stopFailed = true;
                context.stopError = Console.CONSTANTS.patch_manager_stop_server_timeout();
                wizard.next();
            }

            @Override
            public void onError(final Throwable caught) {
                context.stopFailed = true;
                context.stopError = Console.CONSTANTS.patch_manager_stop_server_unknown_error();
                context.stopErrorDetails = caught.getMessage();
                wizard.next();
            }
        });
    }
}
