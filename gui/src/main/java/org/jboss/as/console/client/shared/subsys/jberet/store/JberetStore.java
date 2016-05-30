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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.subsys.jberet.Job;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.Composite;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.dmr.client.ModelDescriptionConstants;
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

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Harald Pehl
 */
@Store
public class JberetStore extends ChangeSupport {

    class ChannelCallback implements CrudOperationDelegate.Callback {

        private final StatementContext context;
        private final Channel channel;

        ChannelCallback(StatementContext context, final Channel channel) {
            this.context = context;
            this.channel = channel;
        }

        @Override
        public void onSuccess(final AddressTemplate address, final String name) {
            Console.info(
                    Console.MESSAGES.successfullyModifiedResource(address.resolve(statementContext, name).toString()));
            init(channel);
        }

        @Override
        public void onFailure(final AddressTemplate address, final String name, final Throwable t) {
            Console.error(Console.MESSAGES.failedToModifyResource(address.resolve(statementContext, name).toString()),
                    t.getMessage());
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
    public static final AddressTemplate THREAD_POOL_METRICS_ADDRESS = AddressTemplate.of(METRICS_ROOT)
            .append("thread-pool=*");

    public static final String DEPLOYMENT = "{implicit.host}/{selected.server}/deployment=*/subsystem=batch-jberet";
    public static final String SUBDEPLOYMENT = "{implicit.host}/{selected.server}/deployment=*/subdeployment=*/subsystem=batch-jberet";
    public static final String JOB_DEPLOYMENT = "{implicit.host}/{selected.server}/deployment=*/subsystem=batch-jberet/job=*/execution=*";
    public static final String JOB_SUBDEPLOYMENT = "{implicit.host}/{selected.server}/deployment=*/subdeployment=*/subsystem=batch-jberet/job=*/execution=*";
    public static final AddressTemplate DEPLOYMENT_ADDRESS = AddressTemplate.of(DEPLOYMENT);
    public static final AddressTemplate SUBDEPLOYMENT_ADDRESS = AddressTemplate.of(SUBDEPLOYMENT);
    public static final AddressTemplate JOB_DEPLOYMENT_ADDRESS = AddressTemplate.of(JOB_DEPLOYMENT);
    public static final AddressTemplate JOB_SUBDEPLOYMENT_ADDRESS = AddressTemplate.of(JOB_SUBDEPLOYMENT);

    private final DispatchAsync dispatcher;
    private final StatementContext statementContext;
    private final CrudOperationDelegate operationDelegate;
    private ModelNode defaults;
    private final List<Property> inMemoryRepositories;
    private final List<Property> jdbcRepositories;
    private final List<Property> threadFactories;
    private final List<Property> threadPools;
    private final List<Property> threadPoolMetrics;
    private final List<Job> jobsMetrics;
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
        this.jobsMetrics = new ArrayList<>();
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
        operationDelegate.onSaveResource(ROOT_ADDRESS, null, action.getChangedValues(),
                new ChannelCallback(statementContext, channel));
    }


    // ------------------------------------------------------ in memory repositories

    @Process(actionType = AddInMemoryRepository.class)
    public void addInMemoryRepository(final AddInMemoryRepository action, final Channel channel) {
        operationDelegate.onCreateResource(IN_MEMORY_REPOSITORY_ADDRESS,
                action.getProperty().getName(), action.getProperty().getValue(),
                new ChannelCallback(statementContext, channel));
    }

    @Process(actionType = RemoveInMemoryRepository.class)
    public void removeInMemoryRepository(final RemoveInMemoryRepository action, final Channel channel) {
        operationDelegate.onRemoveResource(IN_MEMORY_REPOSITORY_ADDRESS, action.getName(),
                new ChannelCallback(statementContext, channel));
    }


    // ------------------------------------------------------ jdbc repositories

    @Process(actionType = AddJdbcRepository.class)
    public void addJdbcRepository(final AddJdbcRepository action, final Channel channel) {
        operationDelegate.onCreateResource(JDBC_REPOSITORY_ADDRESS,
                action.getProperty().getName(), action.getProperty().getValue(),
                new ChannelCallback(statementContext, channel));
    }

    @Process(actionType = ModifyJdbcRepository.class)
    public void modifyJdbcRepository(final ModifyJdbcRepository action, final Channel channel) {
        operationDelegate.onSaveResource(JDBC_REPOSITORY_ADDRESS, action.getName(), action.getChangedValues(),
                new ChannelCallback(statementContext, channel));
    }

    @Process(actionType = RemoveJdbcRepository.class)
    public void removeJdbcRepository(final RemoveJdbcRepository action, final Channel channel) {
        operationDelegate.onRemoveResource(JDBC_REPOSITORY_ADDRESS, action.getName(),
                new ChannelCallback(statementContext, channel));
    }


    // ------------------------------------------------------ thread factories

    @Process(actionType = AddThreadFactory.class)
    public void addThreadFactory(final AddThreadFactory action, final Channel channel) {
        operationDelegate.onCreateResource(THREAD_FACTORY_ADDRESS,
                action.getProperty().getName(), action.getProperty().getValue(),
                new ChannelCallback(statementContext, channel));
    }

    @Process(actionType = ModifyThreadFactory.class)
    public void modifyThreadFactory(final ModifyThreadFactory action, final Channel channel) {
        operationDelegate.onSaveResource(THREAD_FACTORY_ADDRESS, action.getName(), action.getChangedValues(),
                new ChannelCallback(statementContext, channel));
    }

    @Process(actionType = RemoveThreadFactory.class)
    public void removeThreadFactory(final RemoveThreadFactory action, final Channel channel) {
        operationDelegate.onRemoveResource(THREAD_FACTORY_ADDRESS, action.getName(),
                new ChannelCallback(statementContext, channel));
    }


    // ------------------------------------------------------ thread pools

    @Process(actionType = AddThreadPool.class)
    public void addThreadPool(final AddThreadPool action, final Channel channel) {
        operationDelegate.onCreateResource(THREAD_POOL_ADDRESS,
                action.getProperty().getName(), action.getProperty().getValue(),
                new ChannelCallback(statementContext, channel));
    }

    @Process(actionType = ModifyThreadPool.class)
    public void modifyThreadPool(final ModifyThreadPool action, final Channel channel) {
        operationDelegate.onSaveResource(THREAD_POOL_ADDRESS, action.getName(), action.getChangedValues(),
                new ChannelCallback(statementContext, channel));
    }

    @Process(actionType = RemoveThreadPool.class)
    public void removeThreadPool(final RemoveThreadPool action, final Channel channel) {
        operationDelegate.onRemoveResource(THREAD_POOL_ADDRESS, action.getName(),
                new ChannelCallback(statementContext, channel));

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

    @Process(actionType = LoadJobsMetrics.class)
    public void loadJobsMetrics(final LoadJobsMetrics action, Channel channel) {

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).setEmptyList();
        operation.get(OP).set(COMPOSITE);

        ResourceAddress address = DEPLOYMENT_ADDRESS.resolve(statementContext);
        Operation opDeployments = new Operation.Builder(READ_RESOURCE_OPERATION, address)
                .param(INCLUDE_RUNTIME, true)
                .param(RECURSIVE, true)
                .build();

        address = SUBDEPLOYMENT_ADDRESS.resolve(statementContext);
        Operation opSubDeployments = new Operation.Builder(READ_RESOURCE_OPERATION, address)
                .param(INCLUDE_RUNTIME, true)
                .param(RECURSIVE, true)
                .build();

        List<ModelNode> steps = new ArrayList<>();
        steps.add(opDeployments);
        steps.add(opSubDeployments);

        operation.get(STEPS).set(steps);

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                channel.nack(caught);
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode compositeResponse = response.get();

                jobsMetrics.clear();

                if (compositeResponse.isFailure()) {
                    channel.nack(compositeResponse.getFailureDescription());
                } else {
                    ModelNode compositeResult = compositeResponse.get(RESULT);

                    ModelNode mainResponse = compositeResult.get("step-1");
                    ModelNode subdeploymentResponse = compositeResult.get("step-2");

                    parseJobResults(mainResponse);
                    parseJobResults(subdeploymentResponse);
                    channel.ack();

                }
            }
        });
    }

    private void parseJobResults(ModelNode response) {
        ModelNode result = response.get(RESULT);
        List<ModelNode> deploymentList = result.asList();
        for (ModelNode deploymentNode : deploymentList) {
            List<ModelNode> addressList = deploymentNode.get(ADDRESS).asList();
            String deploymentName;
            String subdeploymentName = "";
            
            if (addressList.size() == 3) {
                // constains subdeployment
                deploymentName = addressList.get(0).get(ModelDescriptionConstants.DEPLOYMENT).asString();
                subdeploymentName = addressList.get(1).get(ModelDescriptionConstants.SUBDEPLOYMENT).asString();
            } else {
                deploymentName = addressList.get(0).get(ModelDescriptionConstants.DEPLOYMENT).asString();

            }
            ModelNode jobNode = deploymentNode.get(RESULT).get(ModelDescriptionConstants.JOB);
            for (Property jobProperty : jobNode.asPropertyList()) {
                String jobName = jobProperty.getName();
                
                // if the job had run, get the runtime attributes
                if (jobProperty.getValue().get("instance-count").asInt() > 0) {
                    
                    for (Property ins : jobProperty.getValue().get("execution").asPropertyList()) {
                        Job job = new Job(ins.getValue());
                        job.setName(jobName);
                        job.setExecutionId(ins.getName());
                        job.setDeploymentName(deploymentName);
                        job.setSubdeploymentName(subdeploymentName);
                        jobsMetrics.add(job);
                    }
                } else {
                    Job job = new Job(new ModelNode());
                    job.setName(jobName);
                    job.setDeploymentName(deploymentName);
                    job.setSubdeploymentName(subdeploymentName);
                    jobsMetrics.add(job);
                }
            }
        }
    }

    @Process(actionType = StartJob.class)
    void startJob(final StartJob action, Channel channel) {

        ResourceAddress address;
        if (action.getSubDeploymentName().length() == 0) {
            address = DEPLOYMENT_ADDRESS.replaceWildcards(action.getDeploymentName())
                .resolve(statementContext);
        } else {
            address = SUBDEPLOYMENT_ADDRESS.replaceWildcards(action.getDeploymentName(), action.getSubDeploymentName())
                .resolve(statementContext);
        }

        Operation opJob = new Operation.Builder(START_JOB, address)
            .param(JOB_XML_NAME, action.getJobName())
            .build();

        dispatcher.execute(new DMRAction(opJob), new AsyncCallback<DMRResponse>() {
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
                    loadJobsMetrics(null, channel);
                    channel.ack();
                }
            }
        });
    }
    
    @Process(actionType = StopJob.class)
    void stopJob(final StopJob action, Channel channel) {

        ResourceAddress address;
        if (action.getSubDeploymentName().length() == 0) {
            address = JOB_DEPLOYMENT_ADDRESS.replaceWildcards(action.getDeploymentName(), action.getJobName(), 
                    action.getExecutionId())
                .resolve(statementContext);
        } else {
            address = JOB_SUBDEPLOYMENT_ADDRESS.replaceWildcards(action.getDeploymentName(), action.getSubDeploymentName(), 
                    action.getJobName(), action.getExecutionId())
                .resolve(statementContext);
        }

        Operation opJob = new Operation.Builder(STOP_JOB, address)
            .build();

        dispatcher.execute(new DMRAction(opJob), new AsyncCallback<DMRResponse>() {
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
                    loadJobsMetrics(null, channel);
                    channel.ack();
                }
            }
        });
    }

    @Process(actionType = RestartJob.class)
    void restartJob(final RestartJob action, Channel channel) {

        ResourceAddress address;
        if (action.getSubDeploymentName().length() == 0) {
            address = JOB_DEPLOYMENT_ADDRESS.replaceWildcards(action.getDeploymentName(), action.getJobName(), 
                    action.getExecutionId())
                .resolve(statementContext);
        } else {
            address = JOB_SUBDEPLOYMENT_ADDRESS.replaceWildcards(action.getDeploymentName(), action.getSubDeploymentName(), 
                    action.getJobName(), action.getExecutionId())
                .resolve(statementContext);
        }

        Operation opJob = new Operation.Builder(RESTART_JOB, address)
            .build();

        dispatcher.execute(new DMRAction(opJob), new AsyncCallback<DMRResponse>() {
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
                    loadJobsMetrics(null, channel);
                    channel.ack();
                }
            }
        });
    }

    @Process(actionType = ModifyComplexAttribute.class)
    public void onModifyComplexAttribute(ModifyComplexAttribute action, Channel channel) {
        ResourceAddress address = THREAD_POOL_ADDRESS.resolve(statementContext, action.getParentName());
        Operation operation = new Operation.Builder(WRITE_ATTRIBUTE_OPERATION, address)
                .param(NAME, action.getName())
                .param(VALUE, action.getPayload())
                .build();
        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                new ChannelCallback(statementContext, channel)
                        .onFailure(THREAD_POOL_ADDRESS, action.getParentName(), caught);
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                new ChannelCallback(statementContext, channel).onSuccess(THREAD_POOL_ADDRESS, action.getParentName());
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

    public List<Job> getJobsMetrics() {
        return jobsMetrics;
    }

}