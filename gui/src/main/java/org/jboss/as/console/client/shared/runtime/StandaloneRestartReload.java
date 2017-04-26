/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

package org.jboss.as.console.client.shared.runtime;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.web.bindery.event.shared.EventBus;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.message.Message;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.schedule.LongRunningTask;
import org.jboss.as.console.client.shared.state.ReloadEvent;
import org.jboss.as.console.client.shared.state.ReloadState;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.AsyncCommand;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;
import static org.jboss.dmr.client.ModelDescriptionConstants.ADDRESS;
import static org.jboss.dmr.client.ModelDescriptionConstants.RESULT;


public class StandaloneRestartReload {

    private final DispatchAsync dispatchAsync;
    private final EventBus eventBus;
    private final ReloadState reloadState;

    public StandaloneRestartReload() {
        this.reloadState =  Console.MODULES.getReloadState();;
        this.dispatchAsync = Console.MODULES.getDispatchAsync();
        this.eventBus = Console.MODULES.getEventBus();
    }

    public void onReloadServer() {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("reload");
        operation.get(ADDRESS).setEmptyList();

        dispatchAsync.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Reload Server"), response.getFailureDescription());
                } else {
                    pollState("Waiting for the server to reload", "Reload Server");
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                Console.error(Console.MESSAGES.failed("Reload Server"), caught.getMessage());
            }
        });
    }

    public void onRestartServer() {
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("shutdown");
        operation.get(ADDRESS).setEmptyList();
        operation.get("restart").set(true);

        dispatchAsync.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Restart Server"), response.getFailureDescription());
                } else {
                    pollState("Waiting for the server to restart","Restart Server");
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                Console.error(Console.MESSAGES.failed("Restart Server"), caught.getMessage());
            }
        });
    }

    private void pollState(final String standBy, final String success) {

        LongRunningTask poll = new LongRunningTask(new AsyncCommand<Boolean>() {
            @Override
            public void execute(final AsyncCallback<Boolean> callback) {
                checkServerState(callback, standBy, success);
            }
        }, 10);

        // kick of the polling request
        poll.schedule(500);
    }

    /**
     * Simply query the process state attribute to get to the required headers
     */
    public void checkServerState(final AsyncCallback<Boolean> callback, final String standBy, final String success) {

        // :read-attribute(name=process-type)
        final ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("server-state");
        operation.get(ADDRESS).setEmptyList();

        dispatchAsync.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {

                ModelNode response = result.get();

                if (response.isFailure()) {
                    callback.onFailure(new RuntimeException("Failed to poll server state"));
                } else {
                    // TODO: only works when this response changes the reload state
                    String outcome = response.get(RESULT).asString();
                    boolean keepRunning = !outcome.equalsIgnoreCase("running");//reloadState.isStaleModel();

                    if (!keepRunning) {

                        // clear state
                        reloadState.reset();

                        Console.info(Console.MESSAGES.successful(success));

                        // clear reload state
                        eventBus.fireEvent(new ReloadEvent());
                    }

                    callback.onSuccess(keepRunning);
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                Console.getMessageCenter().notify(new Message(standBy, caught.getMessage(), Message.Severity.Warning));
            }
        });
    }

}
