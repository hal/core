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
package org.jboss.as.console.client.shared.subsys.jberet.store;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.Composite;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher.Channel;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
@Store
public class JberetStore extends ChangeSupport {

    class ChannelCallback implements CrudOperationDelegate.Callback {

        private final Channel channel;

        ChannelCallback(final Channel channel) {this.channel = channel;}

        @Override
        public void onSuccess(final AddressTemplate addressTemplate, final String name) {
            init(channel);
        }

        @Override
        public void onFailure(final AddressTemplate addressTemplate, final String name, final Throwable t) {
            channel.nack(t);
        }
    }


    public static final String ROOT = "{selected.profile}/subsystem=batch-jberet";
    public static final AddressTemplate ROOT_ADDRESS = AddressTemplate.of(ROOT);
    public static final AddressTemplate IN_MEMORY_REPOSITORY_ADDRESS = AddressTemplate.of(ROOT)
            .append("in-memory-job-repository=*");
    public static final AddressTemplate JDBC_REPOSITORY_ADDRESS = AddressTemplate.of(ROOT)
            .append("jdbc-job-repository=*");
    public static final AddressTemplate THREAD_FACTORY_ADDRESS = AddressTemplate.of(ROOT).append("thread-factory=*");
    public static final AddressTemplate THREAD_POOL_ADDRESS = AddressTemplate.of(ROOT).append("thread-pool=*");

    public static final String METRICS_ROOT = "{implicit.host}/{selected.server}/subsystem=batch-jberet";
    public static final AddressTemplate METRICS_ROOT_ADDRESS = AddressTemplate.of(METRICS_ROOT);
    public static final AddressTemplate THREAD_POOL_METRICS_ADDRESS = AddressTemplate.of(METRICS_ROOT).append("thread-pool=*");

    private final DispatchAsync dispatcher;
    private final StatementContext statementContext;
    private final CrudOperationDelegate operationDelegate;
    private ModelNode defaults;
    private final List<Property> inMemoryRepositories;
    private final List<Property> jdbcRepositories;
    private final List<Property> threadFactories;
    private final List<Property> threadPools;
    private final List<Property> threadPoolMetrics;
    private ModelNode currentThreadPoolMetric;

    @Inject
    public JberetStore(final DispatchAsync dispatcher, StatementContext statementContext) {
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
        this.operationDelegate = new CrudOperationDelegate(statementContext, dispatcher);

        this.defaults = new ModelNode();
        this.inMemoryRepositories = new ArrayList<>();
        this.jdbcRepositories = new ArrayList<>();
        this.threadFactories = new ArrayList<>();
        this.threadPools = new ArrayList<>();
        this.threadPoolMetrics = new ArrayList<>();
    }


    // ------------------------------------------------------ init

    @Process(actionType = InitJberet.class)
    public void init(final Channel channel) {
        ResourceAddress rootAddress = ROOT_ADDRESS.resolve(statementContext);
        Operation readRoot = new Operation.Builder(READ_RESOURCE_OPERATION, rootAddress).build();
        Operation inMemoryRepository = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, rootAddress)
                .param(CHILD_TYPE, "in-memory-job-repository")
                .build();
        Operation jdbcRepository = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, rootAddress)
                .param(CHILD_TYPE, "jdbc-job-repository")
                .build();
        Operation threadFactory = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, rootAddress)
                .param(CHILD_TYPE, "thread-factory")
                .build();
        Operation threadPool = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, rootAddress)
                .param(CHILD_TYPE, "thread-pool")
                .build();
        Composite composite = new Composite(readRoot, inMemoryRepository, jdbcRepository, threadFactory, threadPool);
        dispatcher.execute(new DMRAction(composite), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                channel.nack(caught);
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    channel.nack(result.getFailureDescription());
                } else {
                    ModelNode payload = result.get(RESULT);
                    ModelNode step = payload.get("step-1");
                    if (step.get(RESULT).isDefined()) {
                        defaults = step.get(RESULT);
                    }
                    step = payload.get("step-2");
                    if (step.get(RESULT).isDefined()) {
                        inMemoryRepositories.clear();
                        inMemoryRepositories.addAll(step.get(RESULT).asPropertyList());
                    }
                    step = payload.get("step-3");
                    if (step.get(RESULT).isDefined()) {
                        jdbcRepositories.clear();
                        jdbcRepositories.addAll(step.get(RESULT).asPropertyList());
                    }
                    step = payload.get("step-4");
                    if (step.get(RESULT).isDefined()) {
                        threadFactories.clear();
                        threadFactories.addAll(step.get(RESULT).asPropertyList());
                    }
                    step = payload.get("step-5");
                    if (step.get(RESULT).isDefined()) {
                        threadPools.clear();
                        threadPools.addAll(step.get(RESULT).asPropertyList());
                    }
                    channel.ack();
                }
            }
        });
    }


    // ------------------------------------------------------ defaults

    @Process(actionType = ModifyDefaults.class)
    public void modifyDefaults(final ModifyDefaults action, final Channel channel) {
        operationDelegate.onSaveResource(ROOT_ADDRESS, null, action.getChangedValues(), new ChannelCallback(channel));
    }


    // ------------------------------------------------------ in memory repositories

    @Process(actionType = AddInMemoryRepository.class)
    public void addInMemoryRepository(final AddInMemoryRepository action, final Channel channel) {
        operationDelegate.onCreateResource(IN_MEMORY_REPOSITORY_ADDRESS,
                action.getProperty().getName(), action.getProperty().getValue(), new ChannelCallback(channel));
    }

    @Process(actionType = RemoveInMemoryRepository.class)
    public void removeInMemoryRepository(final RemoveInMemoryRepository action, final Channel channel) {
        operationDelegate.onRemoveResource(IN_MEMORY_REPOSITORY_ADDRESS, action.getName(),
                new ChannelCallback(channel));
    }


    // ------------------------------------------------------ jdbc repositories

    @Process(actionType = AddJdbcRepository.class)
    public void addJdbcRepository(final AddJdbcRepository action, final Channel channel) {
        operationDelegate.onCreateResource(JDBC_REPOSITORY_ADDRESS,
                action.getProperty().getName(), action.getProperty().getValue(), new ChannelCallback(channel));
    }

    @Process(actionType = ModifyJdbcRepository.class)
    public void modifyJdbcRepository(final ModifyJdbcRepository action, final Channel channel) {
        operationDelegate.onSaveResource(JDBC_REPOSITORY_ADDRESS, action.getName(), action.getChangedValues(),
                new ChannelCallback(channel));
    }

    @Process(actionType = RemoveJdbcRepository.class)
    public void removeJdbcRepository(final RemoveJdbcRepository action, final Channel channel) {
        operationDelegate.onRemoveResource(JDBC_REPOSITORY_ADDRESS, action.getName(),
                new ChannelCallback(channel));
    }


    // ------------------------------------------------------ thread factories

    @Process(actionType = AddThreadFactory.class)
    public void addThreadFactory(final AddThreadFactory action, final Channel channel) {
        operationDelegate.onCreateResource(THREAD_FACTORY_ADDRESS,
                action.getProperty().getName(), action.getProperty().getValue(), new ChannelCallback(channel));
    }

    @Process(actionType = ModifyThreadFactory.class)
    public void modifyThreadFactory(final ModifyThreadFactory action, final Channel channel) {
        operationDelegate.onSaveResource(THREAD_FACTORY_ADDRESS, action.getName(), action.getChangedValues(),
                new ChannelCallback(channel));
    }

    @Process(actionType = RemoveThreadFactory.class)
    public void removeThreadFactory(final RemoveThreadFactory action, final Channel channel) {
        operationDelegate.onRemoveResource(THREAD_FACTORY_ADDRESS, action.getName(),
                new ChannelCallback(channel));
    }


    // ------------------------------------------------------ thread pools

    @Process(actionType = AddThreadPool.class)
    public void addThreadPool(final AddThreadPool action, final Channel channel) {
        operationDelegate.onCreateResource(THREAD_POOL_ADDRESS,
                action.getProperty().getName(), action.getProperty().getValue(), new ChannelCallback(channel));
    }

    @Process(actionType = ModifyThreadPool.class)
    public void modifyThreadPool(final ModifyThreadPool action, final Channel channel) {
        operationDelegate.onSaveResource(THREAD_POOL_ADDRESS, action.getName(), action.getChangedValues(),
                new ChannelCallback(channel));
    }

    @Process(actionType = RemoveThreadPool.class)
    public void removeThreadPool(final RemoveThreadPool action, final Channel channel) {
        operationDelegate.onRemoveResource(THREAD_POOL_ADDRESS, action.getName(),
                new ChannelCallback(channel));

    }


    // ------------------------------------------------------ metrics

    @Process(actionType = LoadThreadPoolMetrics.class)
    public void loadMetrics(final LoadThreadPoolMetrics action, Channel channel) {
        ResourceAddress address = METRICS_ROOT_ADDRESS.resolve(statementContext);
        Operation operation = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, address)
                .param(CHILD_TYPE, "thread-pool")
                .param(INCLUDE_RUNTIME, true)
                .build();
        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                channel.nack(caught);
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    channel.nack(result.getFailureDescription());
                } else {
                    threadPoolMetrics.clear();
                    threadPoolMetrics.addAll(result.get(RESULT).asPropertyList());
                    channel.ack();
                }
            }
        });
    }

    @Process(actionType = RefreshThreadPoolMetric.class)
    public void refreshThreadPoolMetric(RefreshThreadPoolMetric action, Channel channel) {
        ResourceAddress address = THREAD_POOL_METRICS_ADDRESS.resolve(statementContext, action.getName());
        Operation operation = new Operation.Builder(READ_RESOURCE_OPERATION, address)
                .param(INCLUDE_RUNTIME, true)
                .build();
        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                channel.nack(caught);
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    channel.nack(result.getFailureDescription());
                } else {
                    currentThreadPoolMetric = result.get(RESULT);
                    channel.ack();
                }
            }
        });
    }


    // ------------------------------------------------------ state access

    public ModelNode getDefaults() {
        return defaults;
    }

    public List<Property> getInMemoryRepositories() {
        return inMemoryRepositories;
    }

    public List<Property> getJdbcRepositories() {
        return jdbcRepositories;
    }

    public List<Property> getThreadFactories() {
        return threadFactories;
    }

    public List<Property> getThreadPools() {
        return threadPools;
    }

    public List<Property> getThreadPoolMetrics() {
        return threadPoolMetrics;
    }

    public ModelNode getCurrentThreadPoolMetric() {
        return currentThreadPoolMetric;
    }
}
