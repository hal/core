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
package org.jboss.as.console.client.shared.subsys.ws.store;

import static org.jboss.dmr.client.ModelDescriptionConstants.ADDRESS;
import static org.jboss.dmr.client.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.dmr.client.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.dmr.client.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.dmr.client.ModelDescriptionConstants.OP;
import static org.jboss.dmr.client.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.dmr.client.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.dmr.client.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.dmr.client.ModelDescriptionConstants.RESULT;
import static org.jboss.dmr.client.ModelDescriptionConstants.STEPS;

import java.util.ArrayList;
import java.util.List;

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
import org.useware.kernel.gui.behaviour.StatementContext;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

/**
 * Circuit store for the webservices subsystem. In particular this store manages
 * the following resources:
 * <ul>
 * <li>Attributes of {@code selected.profile}/subsystem=webservices} (RU)</li>
 * <li>Child resources of {@code selected.profile}/subsystem=webservices/client-config=*} (CRUD)</li>
 * <li>Child resources of {@code selected.profile}/subsystem=webservices/endpoint-config=*} (CRUD)</li>
 * </ul>
 *
 * @author Claudio Miranda <claudio@redhat.com>
 * @date 3/31/2016
 */

@Store
public class WebServicesStore extends ChangeSupport {

    // base address
    public static final AddressTemplate WS_SUBSYSTEM = AddressTemplate.of("{selected.profile}/subsystem=webservices");

    // endpoint-config
    public static final String ENDPOINT_CONFIG = "endpoint-config";
    public static final AddressTemplate ENDPOINT_CONFIG_ADDRESS = WS_SUBSYSTEM.append(ENDPOINT_CONFIG + "=*");

    // client-config
    public static final String CLIENT_CONFIG = "client-config";
    public static final AddressTemplate CLIENT_CONFIG_ADDRESS = WS_SUBSYSTEM.append(CLIENT_CONFIG + "=*");

    private final DispatchAsync dispatcher;
    private final StatementContext statementContext;
    private final CrudOperationDelegate operationDelegate;

    private ModelNode providerConfiguration;
    private List<Property> endpointConfig;
    private List<Property> clientConfig;

    // initialized only when the endpoint/client config is manipulated
    private ModelNode currentConfig;

    private String lastModifiedInstance;

    @Inject
    public WebServicesStore(DispatchAsync dispatcher, StatementContext statementContext) {
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
        this.operationDelegate = new CrudOperationDelegate(statementContext, dispatcher);
        this.providerConfiguration = new ModelNode();
        this.endpointConfig = new ArrayList<>();
        this.clientConfig = new ArrayList<>();
    }

    @Process(actionType = InitWebServices.class)
    public void init(final Dispatcher.Channel channel) {
        List<ModelNode> steps = new ArrayList<>();

        steps.add(readProviderConfiguration());
        steps.add(readChildResourcesOp(ENDPOINT_CONFIG));
        steps.add(readChildResourcesOp(CLIENT_CONFIG));

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
                    channel.nack(new RuntimeException("Failed to initialize webservices store using " + comp + ": " + response.getFailureDescription()));
                } else {
                    ModelNode result = response.get(RESULT);
                    ModelNode stepResult = result.get("step-1");
                    if (stepResult.get(RESULT).isDefined()) {
                        providerConfiguration = stepResult.get(RESULT);
                    }
                    stepResult = result.get("step-2");
                    if (stepResult.get(RESULT).isDefined()) {
                        endpointConfig.clear();
                        endpointConfig.addAll(stepResult.get(RESULT).asPropertyList());
                    }
                    stepResult = result.get("step-3");
                    if (stepResult.get(RESULT).isDefined()) {
                        clientConfig.clear();
                        clientConfig.addAll(stepResult.get(RESULT).asPropertyList());
                    }
                    channel.ack();
                }
            }
        });
    }

    // ------------------------------------------------------ provider configuration

    @Process(actionType = ModifyProviderConfiguration.class)
    public void modifyProviderConfiguration(final ModifyProviderConfiguration action, final Dispatcher.Channel channel) {
        operationDelegate.onSaveResource(WS_SUBSYSTEM, null, action.getChangedValues(),
                new CrudOperationDelegate.Callback() {
                    @Override
                    public void onSuccess(final AddressTemplate addressTemplate, final String name) {
                        final ModelNode op = readProviderConfiguration();
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
                                    providerConfiguration = response.get(RESULT);
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

    // ------------------------------------------------------ read a single endpoint/client configuration

    @Process(actionType = ReadConfig.class)
    public void readConfig(final ReadConfig action, final Dispatcher.Channel channel) {
        readConfig(action.getAddressTemplate(), channel);
    }

    // ------------------------------------------------------ create/delete endpoint/client configuration

    @Process(actionType = CreateConfig.class)
    public void createConfig(final CreateConfig action, final Dispatcher.Channel channel) {
        create(action.getAddressTemplate(), action.getInstanceName(), action.getNewModel(),
                getModelsFor(action.getAddressTemplate()), channel);
    }

    @Process(actionType = DeleteConfig.class)
    public void deleteConfig(final DeleteConfig action, final Dispatcher.Channel channel) {
        delete(action.getAddressTemplate(), action.getInstanceName(), getModelsFor(action.getAddressTemplate()),
                channel);
    }

    @Process(actionType = ReadAllEndpointConfig.class)
    public void readAllEndpointConfig(final ReadAllEndpointConfig action, final Dispatcher.Channel channel) {
        read(action.getAddressTemplate().getResourceType(), getModelsFor(action.getAddressTemplate()), channel);
    }

    @Process(actionType = ReadAllClientConfig.class)
    public void readAllClientConfig(final ReadAllClientConfig action, final Dispatcher.Channel channel) {
        read(action.getAddressTemplate().getResourceType(), getModelsFor(action.getAddressTemplate()), channel);
    }    
    
    // ------------------------------------------------------ pre/post handlers

    @Process(actionType = CreateHandler.class)
    public void createHandler(final CreateHandler action, final Dispatcher.Channel channel) {
        operationDelegate.onCreateResource(action.getAddressTemplate(), action.getInstanceName(), action.getNewModel(),
            new CrudOperationDelegate.Callback() {
                @Override
                public void onFailure(AddressTemplate template, String name, Throwable t) {
                    channel.nack(t);
                }

                @Override
                public void onSuccess(AddressTemplate addressTemplate, String name) {
                    lastModifiedInstance = action.getInstanceName();
                    // we must strip the pre-handler-chain suffix to reload a single configuration. 
                    readConfig(action.getAddressTemplate().subTemplate(0, 3), channel);
                }
            });
    }

    @Process(actionType = ReadHandler.class)
    public void readHandler(final ReadHandler action, final Dispatcher.Channel channel) {
        readConfig(action.getAddressTemplate(), channel);
    }

    @Process(actionType = UpdateHandler.class)
    public void updateHandler(final UpdateHandler action, final Dispatcher.Channel channel) {
        operationDelegate.onSaveResource(action.getAddressTemplate(), action.getInstanceName(), action.getChangedValues(),
            new CrudOperationDelegate.Callback() {
                @Override
                public void onFailure(AddressTemplate addressTemplate1, String name, Throwable t) {
                    channel.nack(t);
                }

                @Override
                public void onSuccess(AddressTemplate addressTemplate1, String name) {
                    lastModifiedInstance = action.getInstanceName();
                    // we must strip the pre-handler-chain suffix to reload a single configuration.
                    readConfig(action.getAddressTemplate().subTemplate(0, 3), channel);
                }
            });
    }

    @Process(actionType = DeleteHandler.class)
    public void deleteHandler(final DeleteHandler action, final Dispatcher.Channel channel) {
        operationDelegate.onRemoveResource(action.getAddressTemplate(), action.getInstanceName(),
            new CrudOperationDelegate.Callback() {
                @Override
                public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
                    channel.nack(t);
                }

                @Override
                public void onSuccess(AddressTemplate addressTemplate, String name) {
                    lastModifiedInstance = null;
                    // we must strip the pre-handler-chain suffix to reload a single configuration.
                    readConfig(action.getAddressTemplate().subTemplate(0, 3), channel);
                }
            });
    }

    // ------------------------------------------------------ generic create, read, update, delete 

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

    private void read(final String childType, final List<Property> list, final Dispatcher.Channel channel) {

        final ModelNode op = readChildResourcesOp(childType);
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

    private void readConfig(final AddressTemplate configAddress, final Dispatcher.Channel channel) {

        final ModelNode op = readConfigOp(configAddress);
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
                    ModelNode result = response.get(RESULT);
                    currentConfig = result;
                    channel.ack();
                }
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

    private ModelNode readProviderConfiguration() {
        ModelNode op = new ModelNode();
        op.get(ADDRESS).set(WS_SUBSYSTEM.resolve(statementContext));
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(INCLUDE_RUNTIME).set(true);
        return op;
    }

    private ModelNode readChildResourcesOp(String childType) {
        ModelNode op = new ModelNode();
        op.get(ADDRESS).set(WS_SUBSYSTEM.resolve(statementContext));
        op.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        op.get(CHILD_TYPE).set(childType);
        op.get(INCLUDE_RUNTIME).set(true);
        op.get(RECURSIVE).set(true);
        return op;
    }

    private ModelNode readConfigOp(AddressTemplate addressTemplate) {
        ModelNode op = new ModelNode();
        op.get(ADDRESS).set(addressTemplate.resolve(statementContext));
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(RECURSIVE).set(true);
        return op;
    }


    // ------------------------------------------------------ state access

    public ModelNode getProviderConfiguration() {
        return providerConfiguration;
    }

    public List<Property> getEndpointConfig() {
        return endpointConfig;
    }

    public List<Property> getClientConfig() {
        return clientConfig;
    }

    public ModelNode getCurrentConfig() {
        return currentConfig;
    }

    public List<Property> getModelsFor(AddressTemplate addressTemplate) {
        switch (addressTemplate.getResourceType()) {
            case ENDPOINT_CONFIG:
                return endpointConfig;
            case CLIENT_CONFIG:
                return clientConfig;
            default:
                return new ArrayList<>();
        }
    }

    public String getLastModifiedInstance() {
        return lastModifiedInstance;
    }
}
