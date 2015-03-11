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
package org.jboss.as.console.client.shared.subsys.remoting.store;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.jboss.as.console.client.shared.flow.FunctionContext;
import org.jboss.as.console.client.shared.subsys.remoting.functions.CreateConnectorFn;
import org.jboss.as.console.client.shared.subsys.remoting.functions.CreateSaslSingleton;
import org.jboss.as.console.client.shared.subsys.remoting.functions.ModifySaslSingleton;
import org.jboss.as.console.client.shared.subsys.remoting.functions.VerifySaslSingleton;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;
import org.jboss.gwt.flow.client.Async;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Outcome;
import org.jboss.gwt.flow.client.Progress;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * Circuit store for the remoting subsystem. In particular this store manages the following
 * resources:
 * <ul>
 * <li>Attributes of {@code {selected.profile}/subsystem=remoting/configuration=endpoint} (RU)</li>
 * <li>Child resources of {@code {selected.profile}/subsystem=remoting/connector=*} (CRUD)</li>
 * <li>Child resources of {@code {selected.profile}/subsystem=remoting/http-connector=*} (CRUD)</li>
 * <li>Child resources of {@code {selected.profile}/subsystem=remoting/local-outbound-connection=*} (CRUD)</li>
 * <li>Child resources of {@code {selected.profile}/subsystem=remoting/outbound-connection=*} (CRUD)</li>
 * <li>Child resources of {@code {selected.profile}/subsystem=remoting/remote-outbound-connection=*} (CRUD)</li>
 * </ul>
 *
 * @author Harald Pehl
 */
@Store
@SuppressWarnings("unused")
public class RemotingStore extends ChangeSupport {

    // base address
    public static final AddressTemplate REMOTING_SUBSYSTEM = AddressTemplate.of("{selected.profile}/subsystem=remoting");

    // endpoint configuration
    public static final String CONFIGURATION = "configuration";
    public static final AddressTemplate ENDPOINT_CONFIGURATION_ADDRESS = REMOTING_SUBSYSTEM.append(CONFIGURATION + "=endpoint");

    // connectors
    public static final String REMOTE_CONNECTOR = "connector";
    public static final AddressTemplate REMOTE_CONNECTOR_ADDRESS = REMOTING_SUBSYSTEM.append(REMOTE_CONNECTOR + "=*");
    public static final String REMOTE_HTTP_CONNECTOR = "http-connector";
    public static final AddressTemplate REMOTE_HTTP_CONNECTOR_ADDRESS = REMOTING_SUBSYSTEM.append(REMOTE_HTTP_CONNECTOR + "=*");

    // security / sasl-policy
    public static final String SASL_SINGLETON = "sasl";
    public static final String POLICY_SINGLETON = "policy";

    // outbound connections
    public static final String LOCAL_OUTBOUND_CONNECTION = "local-outbound-connection";
    public static final AddressTemplate LOCAL_OUTBOUND_CONNECTION_ADDRESS = REMOTING_SUBSYSTEM.append(LOCAL_OUTBOUND_CONNECTION + "=*");
    public static final String OUTBOUND_CONNECTION = "outbound-connection";
    public static final AddressTemplate OUTBOUND_CONNECTION_ADDRESS = REMOTING_SUBSYSTEM.append(OUTBOUND_CONNECTION + "=*");
    public static final String REMOTE_OUTBOUND_CONNECTION = "remote-outbound-connection";
    public static final AddressTemplate REMOTE_OUTBOUND_CONNECTION_ADDRESS = REMOTING_SUBSYSTEM.append(REMOTE_OUTBOUND_CONNECTION + "=*");


    // ------------------------------------------------------ state & initialization

    private final DispatchAsync dispatcher;
    private final StatementContext statementContext;
    private final CrudOperationDelegate operationDelegate;

    private ModelNode endpointConfiguration;
    private List<Property> remoteConnectors;
    private List<Property> remoteHttpConnectors;
    private List<Property> localOutboundConnections;
    private List<Property> outboundConnections;
    private List<Property> remoteOutboundConnections;

    private String lastModifiedInstance;

    @Inject
    public RemotingStore(DispatchAsync dispatcher, StatementContext statementContext) {
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
        this.operationDelegate = new CrudOperationDelegate(statementContext, dispatcher);

        this.endpointConfiguration = new ModelNode();
        this.remoteConnectors = new ArrayList<>();
        this.remoteHttpConnectors = new ArrayList<>();
        this.localOutboundConnections = new ArrayList<>();
        this.outboundConnections = new ArrayList<>();
        this.remoteOutboundConnections = new ArrayList<>();
    }

    @Process(actionType = InitRemoting.class)
    public void init(final Dispatcher.Channel channel) {
        List<ModelNode> steps = new ArrayList<>();

        steps.add(readEndpointConfiguration());
        steps.add(readChildResourcesOp(REMOTE_CONNECTOR));
        steps.add(readChildResourcesOp(REMOTE_HTTP_CONNECTOR));
        steps.add(readChildResourcesOp(LOCAL_OUTBOUND_CONNECTION));
        steps.add(readChildResourcesOp(OUTBOUND_CONNECTION));
        steps.add(readChildResourcesOp(REMOTE_OUTBOUND_CONNECTION));

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
                    channel.nack(new RuntimeException("Failed to initialize remoting store using " + comp + ": " +
                            response.getFailureDescription()));
                } else {
                    ModelNode result = response.get(RESULT);
                    ModelNode stepResult = result.get("step-1");
                    if (stepResult.get(RESULT).isDefined()) {
                        endpointConfiguration = stepResult.get(RESULT);
                    }
                    stepResult = result.get("step-2");
                    if (stepResult.get(RESULT).isDefined()) {
                        remoteConnectors.clear();
                        remoteConnectors.addAll(stepResult.get(RESULT).asPropertyList());
                    }
                    stepResult = result.get("step-3");
                    if (stepResult.get(RESULT).isDefined()) {
                        remoteHttpConnectors.clear();
                        remoteHttpConnectors.addAll(stepResult.get(RESULT).asPropertyList());
                    }
                    stepResult = result.get("step-4");
                    if (stepResult.get(RESULT).isDefined()) {
                        localOutboundConnections.clear();
                        localOutboundConnections.addAll(stepResult.get(RESULT).asPropertyList());
                    }
                    stepResult = result.get("step-5");
                    if (stepResult.get(RESULT).isDefined()) {
                        outboundConnections.clear();
                        outboundConnections.addAll(stepResult.get(RESULT).asPropertyList());
                    }
                    stepResult = result.get("step-6");
                    if (stepResult.get(RESULT).isDefined()) {
                        remoteOutboundConnections.clear();
                        remoteOutboundConnections.addAll(stepResult.get(RESULT).asPropertyList());
                    }
                    channel.ack();
                }
            }
        });
    }


    // ------------------------------------------------------ endpoint configuration

    @Process(actionType = ModifyEndpointConfiguration.class)
    public void modifyEndpointConfiguration(final ModifyEndpointConfiguration action, final Dispatcher.Channel channel) {
        operationDelegate.onSaveResource(ENDPOINT_CONFIGURATION_ADDRESS, null, action.getChangedValues(),
                new CrudOperationDelegate.Callback() {
                    @Override
                    public void onSuccess(final AddressTemplate addressTemplate, final String name) {
                        final ModelNode op = readEndpointConfiguration();
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
                                    endpointConfiguration = response.get(RESULT);
                                    channel.ack();
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
                        channel.nack(t);
                    }
                });
    }


    // ------------------------------------------------------ connectors

    @Process(actionType = CreateConnector.class)
    public void createConnector(final CreateConnector action, final Dispatcher.Channel channel) {

        final List<Property> connectors = getModelsFor(action.getAddressTemplate());
        // First create the connector, then create the security=sasl singleton and finally
        // create the sasl-policy=policy singleton
        Outcome<FunctionContext> outcome = new Outcome<FunctionContext>() {
            @Override
            public void onFailure(FunctionContext context) {
                channel.nack(context.getError());
            }

            @Override
            public void onSuccess(FunctionContext context) {
                lastModifiedInstance = action.getInstanceName();
                read(action.getAddressTemplate().getResourceType(), remoteConnectors, channel);
            }
        };

        AddressTemplate securityAddress = action.getAddressTemplate().append("security=" + SASL_SINGLETON);
        AddressTemplate policyAddress = securityAddress.append("sasl-policy=" + POLICY_SINGLETON);
        new Async<FunctionContext>(new Progress.Nop()).waterfall(new FunctionContext(), outcome,
                new CreateConnectorFn(dispatcher, statementContext, action.getAddressTemplate(),
                        action.getInstanceName(), action.getNewModel()),
                new CreateSaslSingleton(dispatcher, statementContext, action.getInstanceName(), securityAddress),
                new CreateSaslSingleton(dispatcher, statementContext, action.getInstanceName(), policyAddress));
    }

    @Process(actionType = ReadConnector.class)
    public void readConnector(final ReadConnector action, final Dispatcher.Channel channel) {
        read(action.getAddressTemplate().getResourceType(), getModelsFor(action.getAddressTemplate()), channel);
    }

    @Process(actionType = UpdateConnector.class)
    public void updateConnector(final UpdateConnector action, final Dispatcher.Channel channel) {
        update(action.getAddressTemplate(), action.getInstanceName(), action.getChangedValues(),
                getModelsFor(action.getAddressTemplate()), channel);
    }

    @Process(actionType = DeleteConnector.class)
    public void deleteConnector(final DeleteConnector action, final Dispatcher.Channel channel) {
        delete(action.getAddressTemplate(), action.getInstanceName(), getModelsFor(action.getAddressTemplate()),
                channel);
    }


    // ------------------------------------------------------ connector singleton resources

    @Process(actionType = ModifySaslSecurity.class)
    public void modifySaslSecurity(final ModifySaslSecurity action, final Dispatcher.Channel channel) {

        final List<Property> connectors = getModelsFor(action.getConnectorAddress());
        Outcome<FunctionContext> outcome = new Outcome<FunctionContext>() {
            @Override
            public void onFailure(FunctionContext context) {
                channel.nack(context.getError());
            }

            @Override
            public void onSuccess(FunctionContext context) {
                lastModifiedInstance = action.getConnectorName();
                read(action.getConnectorAddress().getResourceType(), connectors, channel);
            }
        };

        // It's not guaranteed that the security singleton already exists. If the parent connector
        // was created by the console, it does exists. However if the parent connector was created using
        // the CLI we need to create the security singleton before we try to modify it.
        AddressTemplate securityAddress = action.getConnectorAddress().append("security=" + SASL_SINGLETON);
        new Async<FunctionContext>(new Progress.Nop()).waterfall(new FunctionContext(), outcome,
                new VerifySaslSingleton(dispatcher, statementContext, action.getConnectorName(), securityAddress),
                new CreateSaslSingleton(dispatcher, statementContext, action.getConnectorName(), securityAddress) {
                    @Override
                    public void execute(Control<FunctionContext> control) {
                        int status = control.getContext().pop();
                        if (status == 200) {
                            control.proceed();
                        } else {
                            super.execute(control);
                        }
                    }
                },
                new ModifySaslSingleton(dispatcher, statementContext, action.getConnectorName(), securityAddress, 
                        action.getChangedValues()));
    }

    @Process(actionType = ModifySaslPolicy.class)
    public void modifySaslPolicy(final ModifySaslPolicy action, final Dispatcher.Channel channel) {

        final List<Property> connectors = getModelsFor(action.getConnectorAddress());
        Outcome<FunctionContext> outcome = new Outcome<FunctionContext>() {
            @Override
            public void onFailure(FunctionContext context) {
                channel.nack(context.getError());
            }

            @Override
            public void onSuccess(FunctionContext context) {
                lastModifiedInstance = action.getConnectorName();
                read(action.getConnectorAddress().getResourceType(), connectors, channel);
            }
        };

        // Same as above. Use a series of functions for fail safe modification of the policy singleton
        AddressTemplate securityAddress = action.getConnectorAddress().append("security=" + SASL_SINGLETON);
        AddressTemplate policyAddress = securityAddress.append("sasl-policy=" + POLICY_SINGLETON);
        new Async<FunctionContext>(new Progress.Nop()).waterfall(new FunctionContext(), outcome,
                new VerifySaslSingleton(dispatcher, statementContext, action.getConnectorName(), securityAddress),
                new CreateSaslSingleton(dispatcher, statementContext, action.getConnectorName(), securityAddress) {
                    @Override
                    public void execute(Control<FunctionContext> control) {
                        int status = control.getContext().pop();
                        if (status == 200) {
                            control.proceed();
                        } else {
                            super.execute(control);
                        }
                    }
                },
                new VerifySaslSingleton(dispatcher, statementContext, action.getConnectorName(), policyAddress),
                new CreateSaslSingleton(dispatcher, statementContext, action.getConnectorName(), policyAddress) {
                    @Override
                    public void execute(Control<FunctionContext> control) {
                        int status = control.getContext().pop();
                        if (status == 200) {
                            control.proceed();
                        } else {
                            super.execute(control);
                        }
                    }
                },
                new ModifySaslSingleton(dispatcher, statementContext, action.getConnectorName(), policyAddress, 
                        action.getChangedValues()));
    }


    // ------------------------------------------------------ connections

    @Process(actionType = CreateConnection.class)
    public void createConnection(final CreateConnection action, final Dispatcher.Channel channel) {
        create(action.getAddressTemplate(), action.getInstanceName(), action.getNewModel(),
                getModelsFor(action.getAddressTemplate()), channel);
    }

    @Process(actionType = ReadConnection.class)
    public void readConnection(final ReadConnection action, final Dispatcher.Channel channel) {
        read(action.getAddressTemplate().getResourceType(), getModelsFor(action.getAddressTemplate()), channel);
    }

    @Process(actionType = UpdateConnection.class)
    public void updateConnection(final UpdateConnection action, final Dispatcher.Channel channel) {
        update(action.getAddressTemplate(), action.getInstanceName(), action.getChangedValues(),
                getModelsFor(action.getAddressTemplate()), channel);
    }

    @Process(actionType = DeleteConnection.class)
    public void deleteConnection(final DeleteConnection action, final Dispatcher.Channel channel) {
        delete(action.getAddressTemplate(), action.getInstanceName(), getModelsFor(action.getAddressTemplate()),
                channel);
    }


    // ------------------------------------------------------ create, read, update, delete

    private void create(final AddressTemplate addressTemplate, final String instanceName, final ModelNode newModel,
                        final List<Property> list, final Dispatcher.Channel channel) {
        operationDelegate.onCreateResource(addressTemplate, instanceName, newModel,
                new CrudOperationDelegate.Callback() {
                    @Override
                    public void onFailure(AddressTemplate template, String name, Throwable t) {
                        channel.nack(t);
                    }

                    @Override
                    public void onSuccess(AddressTemplate addressTemplate, String name) {
                        lastModifiedInstance = instanceName;
                        read(addressTemplate.getResourceType(), list, channel);
                    }
                });
    }

    private void read(final String resourceName, final List<Property> list, final Dispatcher.Channel channel) {
        final ModelNode op = readChildResourcesOp(resourceName);
        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                channel.nack(caught);
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    channel.nack(new RuntimeException("Failed to read child resources using " + op + ": " +
                            response.getFailureDescription()));
                } else {
                    list.clear();
                    list.addAll(response.get(RESULT).asPropertyList());
                    channel.ack();
                }
            }
        });
    }

    private void update(final AddressTemplate addressTemplate, final String instanceName,
                        final Map<String, Object> changedValues, final List<Property> list,
                        final Dispatcher.Channel channel) {
        operationDelegate.onSaveResource(addressTemplate, instanceName, changedValues,
                new CrudOperationDelegate.Callback() {
                    @Override
                    public void onFailure(AddressTemplate addressTemplate1, String name, Throwable t) {
                        channel.nack(t);
                    }

                    @Override
                    public void onSuccess(AddressTemplate addressTemplate1, String name) {
                        lastModifiedInstance = instanceName;
                        read(addressTemplate.getResourceType(), list, channel);
                    }
                });
    }

    private void delete(final AddressTemplate addressTemplate, final String instanceName,
                        final List<Property> list, final Dispatcher.Channel channel) {
        operationDelegate.onRemoveResource(addressTemplate, instanceName,
                new CrudOperationDelegate.Callback() {
                    @Override
                    public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
                        channel.nack(t);
                    }

                    @Override
                    public void onSuccess(AddressTemplate addressTemplate, String name) {
                        lastModifiedInstance = null;
                        read(addressTemplate.getResourceType(), list, channel);
                    }
                });
    }


    // ------------------------------------------------------ operation factory methods

    private ModelNode readEndpointConfiguration() {
        ModelNode op = new ModelNode();
        op.get(ADDRESS).set(ENDPOINT_CONFIGURATION_ADDRESS.resolve(statementContext));
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(INCLUDE_RUNTIME).set(true);
        op.get(RECURSIVE).set(true);
        return op;
    }

    private ModelNode readChildResourcesOp(String childType) {
        ModelNode op = new ModelNode();
        op.get(ADDRESS).set(REMOTING_SUBSYSTEM.resolve(statementContext));
        op.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        op.get(CHILD_TYPE).set(childType);
        op.get(INCLUDE_RUNTIME).set(true);
        op.get(RECURSIVE).set(true);
        return op;
    }


    // ------------------------------------------------------ state access

    public ModelNode getEndpointConfiguration() {
        return endpointConfiguration;
    }

    public List<Property> getLocalOutboundConnections() {
        return localOutboundConnections;
    }

    public List<Property> getOutboundConnections() {
        return outboundConnections;
    }

    public List<Property> getRemoteConnectors() {
        return remoteConnectors;
    }

    public List<Property> getRemoteHttpConnectors() {
        return remoteHttpConnectors;
    }

    public List<Property> getRemoteOutboundConnections() {
        return remoteOutboundConnections;
    }

    public List<Property> getModelsFor(AddressTemplate addressTemplate) {
        switch (addressTemplate.getResourceType()) {
            case REMOTE_CONNECTOR:
                return remoteConnectors;
            case REMOTE_HTTP_CONNECTOR:
                return remoteHttpConnectors;
            case LOCAL_OUTBOUND_CONNECTION:
                return localOutboundConnections;
            case OUTBOUND_CONNECTION:
                return outboundConnections;
            case REMOTE_OUTBOUND_CONNECTION:
                return remoteOutboundConnections;
            default:
                return new ArrayList<>();
        }
    }

    public String getLastModifiedInstance() {
        return lastModifiedInstance;
    }
}