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
package org.jboss.as.console.client.shared.subsys.batch.store;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
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
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * Store for the batch subsystem. In particular this store manages the following resources
 * <ul>
 * <li>Attributes of {selected.profile}/subsystem=batch (RU)</li>
 * <li>Attributes of {selected.profile}/subsystem=batch/thread-pool=batch (RU)</li>
 * <li>Attributes of {selected.profile}/subsystem=batch/hob-repository=jdbc (RU)</li>
 * <li>Sub resources of {selected.profile}/subsystem=batch/thread-factory=* (CRUD)</li>
 * </ul>
 *
 * @author Harald Pehl
 */
@Store
public class BatchStore extends ChangeSupport {

    public static final String BATCH_ADDRESS = "{selected.profile}/subsystem=batch";
    public static final String THREAD_POOL_ADDRESS = "{selected.profile}/subsystem=batch/thread-pool=batch";
    public static final String JOB_REPOSITORY_ADDRESS = "{selected.profile}/subsystem=batch/job-repository=jdbc";
    public static final String THREAD_FACTORIES_ADDRESS = "{selected.profile}/subsystem=batch/thread-factory=*";

    private final DispatchAsync dispatcher;
    private final CoreGUIContext statementContext;
    private final CrudOperationDelegate operationDelegate;
    private ModelNode batch;
    private ModelNode threadPool;
    private ModelNode jobRepository;
    private final List<Property> threadFactories;
    private String lastModifiedThreadFactory;

    @Inject
    public BatchStore(DispatchAsync dispatcher, CoreGUIContext statementContext) {
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
        this.operationDelegate = new CrudOperationDelegate(statementContext, dispatcher);

        this.batch = new ModelNode();
        this.threadPool = new ModelNode();
        this.jobRepository = new ModelNode();
        this.threadFactories = new ArrayList<>();
    }


    // ------------------------------------------------------ process methods

    @Process(actionType = InitBatch.class)
    public void init(final Dispatcher.Channel channel) {
        List<ModelNode> steps = new ArrayList<>();
        steps.add(readResourceOp(BATCH_ADDRESS));
        steps.add(readResourceOp(THREAD_POOL_ADDRESS));
        steps.add(readResourceOp(JOB_REPOSITORY_ADDRESS));
        steps.add(readThreadFactoriesOp());

        final ModelNode comp = new ModelNode();
        comp.get(ADDRESS).setEmptyList();
        comp.get(OP).set(COMPOSITE);
        comp.get(STEPS).set(steps);

        dispatcher.execute(new DMRAction(comp), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                channel.nack(caught);
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    channel.nack(new RuntimeException("Failed to initialize batch store using " + comp + ": " +
                            response.getFailureDescription()));
                } else {
                    ModelNode result = response.get(RESULT);
                    ModelNode stepResult = result.get("step-1");
                    if (stepResult.get(RESULT).isDefined()) {
                        batch = stepResult.get(RESULT);
                    }
                    stepResult = result.get("step-2");
                    if (stepResult.get(RESULT).isDefined()) {
                        threadPool = stepResult.get(RESULT);
                    }
                    stepResult = result.get("step-3");
                    if (stepResult.get(RESULT).isDefined()) {
                        jobRepository = stepResult.get(RESULT);
                    }
                    stepResult = result.get("step-4");
                    if (stepResult.get(RESULT).isDefined()) {
                        threadFactories.clear();
                        threadFactories.addAll(stepResult.get(RESULT).asPropertyList());
                    }
                    channel.ack();
                }
            }
        });
    }

    @Process(actionType = ModifyBatch.class)
    public void modifyBatch(final ModifyBatch action, final Dispatcher.Channel channel) {
        operationDelegate.onSaveResource(BATCH_ADDRESS, null, action.getChangedValues(),
                new ReloadModelNodeCallback(BATCH_ADDRESS, channel) {
                    @Override
                    protected void onPayload(ModelNode node) {
                        batch = node;
                    }
                });
    }

    @Process(actionType = ModifyThreadPool.class)
    public void modifyThreadPool(final ModifyThreadPool action, final Dispatcher.Channel channel) {
        operationDelegate.onSaveResource(THREAD_POOL_ADDRESS, null, action.getChangedValues(),
                new ReloadModelNodeCallback(THREAD_POOL_ADDRESS, channel) {
                    @Override
                    protected void onPayload(ModelNode node) {
                        threadPool = node;
                    }
                });
    }

    @Process(actionType = ModifyJobRepository.class)
    public void modifyJobRepository(final ModifyJobRepository action, final Dispatcher.Channel channel) {
        operationDelegate.onSaveResource(JOB_REPOSITORY_ADDRESS, null, action.getChangedValues(),
                new ReloadModelNodeCallback(JOB_REPOSITORY_ADDRESS, channel) {
                    @Override
                    protected void onPayload(ModelNode node) {
                        jobRepository = node;
                    }
                });
    }

    @Process(actionType = AddThreadFactory.class)
    public void addThreadFactory(final AddThreadFactory action, final Dispatcher.Channel channel) {
        lastModifiedThreadFactory = action.getThreadFactory().get(NAME).asString();
        operationDelegate.onCreateResource(THREAD_FACTORIES_ADDRESS, action.getThreadFactory(),
                new RefreshThreadFactoriesCallback(channel));
    }

    @Process(actionType = ModifyThreadFactory.class)
    public void modifyThreadFactory(final ModifyThreadFactory action, final Dispatcher.Channel channel) {
        lastModifiedThreadFactory = action.getName();
        operationDelegate.onSaveResource(THREAD_FACTORIES_ADDRESS, lastModifiedThreadFactory, action.getChangedValues(),
                new RefreshThreadFactoriesCallback(channel));
    }

    @Process(actionType = RefreshThreadFactories.class)
    public void refreshThreadFactories(final Dispatcher.Channel channel) {
        final ModelNode op = readThreadFactoriesOp();
        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                channel.nack(caught);
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    channel.nack(new RuntimeException("Failed to read buffer pools using " + op + ": " +
                            response.getFailureDescription()));
                } else {
                    threadFactories.clear();
                    threadFactories.addAll(response.get(RESULT).asPropertyList());
                    channel.ack();
                }
            }
        });
    }

    @Process(actionType = RemoveThreadFactory.class)
    public void removeThreadFactory(final RemoveThreadFactory action, final Dispatcher.Channel channel) {
        lastModifiedThreadFactory = null;
        operationDelegate.onRemoveResource(THREAD_FACTORIES_ADDRESS, action.getName(),
                new RefreshThreadFactoriesCallback(channel));
    }


    // ------------------------------------------------------ model node factory methods

    private ModelNode readResourceOp(String addressTemplate) {
        final ResourceAddress op = new ResourceAddress(addressTemplate, statementContext);
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(INCLUDE_RUNTIME).set(true);
        op.get("attributes-only").set(true);
        return op;
    }

    private ModelNode readThreadFactoriesOp() {
        final ResourceAddress op = new ResourceAddress(BATCH_ADDRESS, statementContext);
        op.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        op.get(CHILD_TYPE).set("thread-factory");
        op.get(INCLUDE_RUNTIME).set(true);
        return op;
    }


    // ------------------------------------------------------ state access

    public ModelNode getBatch() {
        return batch;
    }

    public ModelNode getThreadPool() {
        return threadPool;
    }

    public ModelNode getJobRepository() {
        return jobRepository;
    }

    public List<Property> getThreadFactories() {
        return threadFactories;
    }

    public String getLastModifiedThreadFactory() {
        return lastModifiedThreadFactory;
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }


    // ------------------------------------------------------ inner classes

    private abstract class ReloadModelNodeCallback implements CrudOperationDelegate.Callback {
        private final String addressTemplate;
        private final Dispatcher.Channel channel;

        public ReloadModelNodeCallback(String addressTemplate, Dispatcher.Channel channel) {
            this.addressTemplate = addressTemplate;
            this.channel = channel;
        }

        @Override
        public void onSuccess(ResourceAddress address, String name) {
            final ModelNode op = readResourceOp(addressTemplate);
            dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
                @Override
                public void onFailure(Throwable caught) {
                    channel.nack(caught);
                }

                @Override
                public void onSuccess(DMRResponse dmrResponse) {
                    ModelNode response = dmrResponse.get();
                    if (response.isFailure()) {
                        channel.nack(new RuntimeException("Failed to read " + addressTemplate + " using " + op + ": " +
                                response.getFailureDescription()));
                    } else {
                        onPayload(response.get(RESULT));
                        channel.ack();
                    }
                }
            });
        }

        protected abstract void onPayload(ModelNode node);

        @Override
        public void onFailure(ResourceAddress address, String name, Throwable t) {
            channel.nack(t);
        }
    }


    private class RefreshThreadFactoriesCallback implements CrudOperationDelegate.Callback {
        private final Dispatcher.Channel channel;

        public RefreshThreadFactoriesCallback(Dispatcher.Channel channel) {
            this.channel = channel;
        }

        @Override
        public void onSuccess(ResourceAddress address, String name) {
            refreshThreadFactories(channel);
        }

        @Override
        public void onFailure(ResourceAddress address, String name, Throwable t) {
            channel.nack(t);
        }
    }
}

