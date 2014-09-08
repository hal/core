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
package org.jboss.as.console.client.shared.subsys.io.worker;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.jboss.as.console.client.shared.subsys.io.ModifyPayload;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.behaviour.CrudOperationDelegate;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
@Store
public class WorkerStore extends ChangeSupport {

    private static final String RESOURCE_ADDRESS = "{selected.profile}/subsystem=io/worker=*";
    private final DispatchAsync dispatcher;
    private final List<Property> workers;
    private final CrudOperationDelegate operationDelegate;
    private final CoreGUIContext statementContext;

    @Inject
    public WorkerStore(DispatchAsync dispatcher, CoreGUIContext statementContext) {
        this.statementContext = statementContext;
        this.operationDelegate = new CrudOperationDelegate(statementContext, dispatcher);
        this.dispatcher = dispatcher;
        this.workers = new ArrayList<>();
    }


    // ------------------------------------------------------ process methods

    @Process(actionType = AddWorker.class)
    public void add(final ModelNode newWorker, final Dispatcher.Channel channel) {
        operationDelegate.onCreateResource(RESOURCE_ADDRESS, newWorker, new CrudOperationDelegate.Callback() {
            @Override
            public void onSuccess(ResourceAddress address, String name) {
                refresh(channel);
            }

            @Override
            public void onFailure(ResourceAddress address, String name, Throwable t) {
                channel.nack(t);
            }
        });
    }

    @Process(actionType = ModifyWorker.class)
    public void modify(final ModifyPayload modifyPayload, final Dispatcher.Channel channel) {
        operationDelegate.onSaveResource(RESOURCE_ADDRESS, modifyPayload.getName(), modifyPayload.getChangedValues(),
                new CrudOperationDelegate.Callback() {
                    @Override
                    public void onSuccess(ResourceAddress address, String name) {
                        refresh(channel);
                    }

                    @Override
                    public void onFailure(ResourceAddress address, String name, Throwable t) {
                        channel.nack(t);
                    }
                });
    }

    @Process(actionType = RefreshWorkers.class)
    public void refresh(final Dispatcher.Channel channel) {
        final ResourceAddress op = new ResourceAddress("{selected.profile}/subsystem=io/", statementContext);
        op.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        op.get(CHILD_TYPE).set("worker");
        op.get(INCLUDE_RUNTIME).set(true);

        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                channel.nack(caught);
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    channel.nack(new RuntimeException("Failed to read worker using " + op + ": " +
                            response.getFailureDescription()));
                } else {
                    workers.clear();
                    workers.addAll(response.get(RESULT).asPropertyList());
                    channel.ack();
                }
            }
        });
    }

    @Process(actionType = RemoveWorker.class)
    public void remove(final String name, final Dispatcher.Channel channel) {
        operationDelegate.onRemoveResource(RESOURCE_ADDRESS, name, new CrudOperationDelegate.Callback() {
            @Override
            public void onSuccess(ResourceAddress address, String name) {
                refresh(channel);
            }

            @Override
            public void onFailure(ResourceAddress address, String name, Throwable t) {
                channel.nack(t);
            }
        });
    }


    // ------------------------------------------------------ state access

    public List<Property> getWorkers() {
        return workers;
    }
}
