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

package org.jboss.as.console.client.shared.subsys.activemq;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.model.ModelAdapter;
import org.jboss.as.console.client.shared.model.ResponseWrapper;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqAddressingPattern;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnectionFactory;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqDivert;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqJMSEndpoint;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqMessagingProvider;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqQueue;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqSecurityPattern;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqTopic;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.widgets.forms.AddressBinding;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.as.console.client.widgets.forms.PropertyBinding;
import org.jboss.as.console.mbui.behaviour.ModelNodeAdapter;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.widgets.AddResourceDialog;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

public class MsgDestinationsPresenter
        extends Presenter<MsgDestinationsPresenter.MyView, MsgDestinationsPresenter.MyProxy>
        implements MessagingAddress, CommonMsgPresenter {

    // ------------------------------------------------------ proxy & view
    // @formatter:off
    @ProxyCodeSplit
    @NameToken(NameTokens.ActivemqMessagingPresenter)
    @RequiredResources(resources = PROVIDER_ADDRESS)
    @SearchIndex(keywords = {"topic", "queue", "jms", "messaging", "publish", "subscribe"})
    public interface MyProxy extends Proxy<MsgDestinationsPresenter>, Place {}

    public interface MyView extends View, HasPresenter<MsgDestinationsPresenter> {
        void setProviderDetails(ActivemqMessagingProvider provider);
        void setSecurityConfig(List<ActivemqSecurityPattern> secPatterns);
        void setAddressingConfig(List<ActivemqAddressingPattern> addrPatterns);
        void setProvider(List<Property> names);
        void setSelectedProvider(String selectedProvider);
        void setDiverts(List<ActivemqDivert> diverts);
    }

    public interface JMSView {
        void setQueues(List<ActivemqQueue> queues);
        void setTopics(List<ActivemqJMSEndpoint> topics);
        void setConnectionFactories(List<ActivemqConnectionFactory> factories);
        void enableEditQueue(boolean b);
        void enableEditTopic(boolean b);
    }
    // @formatter:on


    private final PlaceManager placeManager;
    private final DispatchAsync dispatcher;
    private final RevealStrategy revealStrategy;
    private final ApplicationMetaData metaData;
    private final Scheduler scheduler;
    private final SecurityFramework securityFramework;
    private final ResourceDescriptionRegistry descriptionRegistry;
    private final StatementContext statementContext;
    private List<ActivemqSecurityPattern> securitySettings;

    private EntityAdapter<ActivemqMessagingProvider> providerAdapter;
    private EntityAdapter<ActivemqSecurityPattern> securityAdapter;
    private EntityAdapter<ActivemqAddressingPattern> addressingAdapter;
    private EntityAdapter<ActivemqConnectionFactory> factoryAdapter;
    private EntityAdapter<ActivemqDivert> divertAdapter;
    private EntityAdapter<ActivemqQueue> queueAdapter;
    private EntityAdapter<ActivemqTopic> topicAdapter;

    private String currentServer;
    private LoadJMSCmd loadJMSCmd;
    private DefaultWindow window;


    @Inject
    public MsgDestinationsPresenter(EventBus eventBus, MyView view, MyProxy proxy,
            PlaceManager placeManager, DispatchAsync dispatcher, BeanFactory factory,
            RevealStrategy revealStrategy, ApplicationMetaData propertyMetaData, Scheduler scheduler,
            SecurityFramework securityFramework, ResourceDescriptionRegistry descriptionRegistry,
            StatementContext statementContext) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
        this.metaData = propertyMetaData;
        this.scheduler = scheduler;
        this.securityFramework = securityFramework;
        this.descriptionRegistry = descriptionRegistry;
        this.statementContext = statementContext;
        this.securitySettings = new ArrayList<>();

        this.providerAdapter = new EntityAdapter<>(ActivemqMessagingProvider.class, propertyMetaData);
        this.securityAdapter = new EntityAdapter<>(ActivemqSecurityPattern.class, propertyMetaData);
        this.addressingAdapter = new EntityAdapter<>(ActivemqAddressingPattern.class, propertyMetaData);

        this.factoryAdapter = new EntityAdapter<>(ActivemqConnectionFactory.class, metaData);
        this.divertAdapter = new EntityAdapter<>(ActivemqDivert.class, metaData);
        this.queueAdapter = new EntityAdapter<>(ActivemqQueue.class, metaData);
        this.topicAdapter = new EntityAdapter<>(ActivemqTopic.class, metaData);

        this.loadJMSCmd = new LoadJMSCmd(dispatcher, factory, metaData);
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {
        currentServer = request.getParameter("name", null);
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadProvider();
    }

    @Override
    public PlaceManager getPlaceManager() {
        return placeManager;
    }

    public void loadDetails() {
        loadProviderDetails();
        loadSecurityConfig();
        loadAddressingConfig();
        loadJMSConfig();
        loadDiverts();
    }

    private void loadDiverts() {
        AddressBinding address = metaData.getBeanMetaData(ActivemqMessagingProvider.class).getAddress();
        ModelNode operation = address.asResource(Baseadress.get(), getCurrentServer());
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(CHILD_TYPE).set("divert");

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                List<ModelNode> modelNodes = response.get(RESULT).asList();
                List<ActivemqDivert> diverts = new ArrayList<>(modelNodes.size());
                for (ModelNode node : modelNodes) {
                    ActivemqDivert divert = divertAdapter.fromDMR(node);
                    diverts.add(divert);
                }
                getView().setDiverts(diverts);
            }
        });
    }

    private void loadProvider() {
        new LoadActivemqServersCmd(dispatcher, statementContext).execute(
                new AsyncCallback<List<Property>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Console.error("Failed to load messaging server names", caught.getMessage());
                    }

                    @Override
                    public void onSuccess(List<Property> result) {
                        getView().setProvider(result);
                        getView().setSelectedProvider(currentServer);
                    }
                }
        );
    }

    public void loadProviderDetails() {
        AddressBinding address = metaData.getBeanMetaData(ActivemqMessagingProvider.class).getAddress();
        ModelNode operation = address.asResource(Baseadress.get(), getCurrentServer());
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(RECURSIVE).set(Boolean.TRUE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                ActivemqMessagingProvider provider = providerAdapter.fromDMR(response.get(RESULT));
                provider.setName(currentServer);
                getView().setProviderDetails(provider);

            }
        });
    }

    private void loadSecurityConfig() {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", getCurrentServer());
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(CHILD_TYPE).set("security-setting");
        operation.get(RECURSIVE).set(true);
        EntityAdapter<ActivemqSecurityPattern> adapter = new EntityAdapter<>(ActivemqSecurityPattern.class, metaData);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                List<Property> patterns = response.get(RESULT).asPropertyList();
                List<ActivemqSecurityPattern> payload = new LinkedList<>();

                for (Property pattern : patterns) {
                    String patternName = pattern.getName();
                    ModelNode patternValue = pattern.getValue().asObject();

                    if (patternValue.hasDefined("role")) {
                        List<Property> roles = patternValue.get("role").asPropertyList();

                        for (Property role : roles) {
                            String roleName = role.getName();
                            ModelNode roleValue = role.getValue().asObject();

                            ActivemqSecurityPattern securityPattern = adapter.fromDMR(roleValue);
                            securityPattern.setPattern(patternName);
                            securityPattern.setRole(roleName);
                            payload.add(securityPattern);
                        }
                    }
                }
                securitySettings = payload;
                getView().setSecurityConfig(payload);
            }
        });
    }

    private void loadAddressingConfig() {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", getCurrentServer());
        operation.get(CHILD_TYPE).set("address-setting");
        operation.get(RECURSIVE).set(Boolean.TRUE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                List<ActivemqAddressingPattern> addrPatterns = new ArrayList<>();
                List<Property> payload = response.get(RESULT).asPropertyList();

                for (Property prop : payload) {
                    String pattern = prop.getName();
                    ModelNode value = prop.getValue().asObject();

                    ActivemqAddressingPattern model = addressingAdapter.fromDMR(value);
                    model.setPattern(pattern);
                    addrPatterns.add(model);
                }
                getView().setAddressingConfig(addrPatterns);
            }
        });
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    public void launchNewSecDialogue() {
        window = new DefaultWindow(Console.MESSAGES.createTitle("Security Setting"));
        window.setWidth(480);
        window.setHeight(360);
        window.trapWidget(new NewSecurityPatternWizard(this).asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void onCreateSecPattern(final ActivemqSecurityPattern newEntity) {
        closeDialogue();

        ModelNode composite = new ModelNode();
        composite.get(OP).set(COMPOSITE);
        composite.get(ADDRESS).setEmptyList();
        List<ModelNode> steps = new ArrayList<>();

        // the parent resource, if needed
        boolean parentDoesExist = false;
        for (ActivemqSecurityPattern setting : securitySettings) {
            if (setting.getPattern().equals(newEntity.getPattern())) {
                parentDoesExist = true;
                break;
            }
        }

        if (!parentDoesExist) {
            // insert a step to create the parent
            ModelNode createParentOp = new ModelNode();
            createParentOp.get(OP).set(ADD);
            createParentOp.get(ADDRESS).set(Baseadress.get());
            createParentOp.get(ADDRESS).add("subsystem", "messaging-activemq");
            createParentOp.get(ADDRESS).add("server", getCurrentServer());
            createParentOp.get(ADDRESS).add("security-setting", newEntity.getPattern());
            steps.add(createParentOp);
        }

        // the child resource
        AddressBinding address = metaData.getBeanMetaData(ActivemqSecurityPattern.class).getAddress();
        ModelNode addressModel = address.asResource(
                Baseadress.get(),
                getCurrentServer(),
                newEntity.getPattern(), newEntity.getRole()
        );

        ModelNode createChildOp = securityAdapter.fromEntity(newEntity);
        createChildOp.get(OP).set(ADD);
        createChildOp.get(ADDRESS).set(addressModel.get(ADDRESS).asObject());
        steps.add(createChildOp);

        composite.get(STEPS).set(steps);
        dispatcher.execute(new DMRAction(composite), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                boolean successful = response.get(OUTCOME).asString().equals(SUCCESS);
                if (successful) { Console.info(Console.MESSAGES.added("Security Setting")); } else {
                    Console.error(Console.MESSAGES.addingFailed("Security Setting" + newEntity.getPattern()),
                            response.toString());
                }
                loadSecurityConfig();
            }
        });
    }

    public void onSaveSecDetails(final ActivemqSecurityPattern pattern, Map<String, Object> changedValues) {
        AddressBinding address = metaData.getBeanMetaData(ActivemqSecurityPattern.class).getAddress();
        ModelNode proto = address
                .asResource(Baseadress.get(), getCurrentServer(), pattern.getPattern(), pattern.getRole());
        proto.get(OP).set(WRITE_ATTRIBUTE_OPERATION);

        List<PropertyBinding> bindings = metaData.getBindingsForType(ActivemqSecurityPattern.class);
        ModelNode operation = ModelAdapter.detypedFromChangeset(proto, changedValues, bindings);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ResponseWrapper<Boolean> response = ModelAdapter.wrapBooleanResponse(result);
                if (response.getUnderlying()) {
                    Console.info(Console.MESSAGES.saved("Security Setting " + pattern.getPattern()));
                } else {
                    Console.error(Console.MESSAGES.saveFailed("Security Setting " + pattern.getPattern()),
                            response.getResponse().toString());
                }
                loadSecurityConfig();
            }
        });
    }

    public void onDeleteSecDetails(final ActivemqSecurityPattern pattern) {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).setEmptyList();
        operation.get(OP).set(COMPOSITE);

        List<ModelNode> steps = new ArrayList<>(2);

        ModelNode deleteRoleOp = new ModelNode();
        deleteRoleOp.get(OP).set(REMOVE);
        deleteRoleOp.get(ADDRESS).set(Baseadress.get());
        deleteRoleOp.get(ADDRESS).add("subsystem", "messaging-activemq");
        deleteRoleOp.get(ADDRESS).add("server", getCurrentServer());
        deleteRoleOp.get(ADDRESS).add("security-setting", pattern.getPattern());
        deleteRoleOp.get(ADDRESS).add("role", pattern.getRole());
        steps.add(deleteRoleOp);

        // verify if pattern can be removed as well
        boolean remains = false;
        for (ActivemqSecurityPattern remaining : securitySettings) {
            if (remaining.getPattern().equals(pattern.getPattern())
                    && !remaining.getRole().equals(pattern.getRole())) {
                remains = true;
                break;
            }
        }

        if (!remains) {
            ModelNode deletePatternOp = new ModelNode();
            deletePatternOp.get(OP).set(REMOVE);
            deletePatternOp.get(ADDRESS).set(Baseadress.get());
            deletePatternOp.get(ADDRESS).add("subsystem", "messaging-activemq");
            deletePatternOp.get(ADDRESS).add("server", getCurrentServer());
            deletePatternOp.get(ADDRESS).add("security-setting", pattern.getPattern());
            steps.add(deletePatternOp);
        }

        operation.get(STEPS).set(steps);
        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                boolean successful = response.get(OUTCOME).asString().equals(SUCCESS);
                if (successful) { Console.info(Console.MESSAGES.deleted("Security Setting")); } else {
                    Console.error(Console.MESSAGES.deletionFailed("Security Setting " + pattern.getPattern()),
                            response.toString());
                }
                loadSecurityConfig();
            }
        });
    }

    public void onDeleteAddressDetails(final ActivemqAddressingPattern addressingPattern) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(REMOVE);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", getCurrentServer());
        operation.get(ADDRESS).add("address-setting", addressingPattern.getPattern());

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                boolean successful = response.get(OUTCOME).asString().equals(SUCCESS);
                if (successful) {
                    Console.info(Console.MESSAGES.deleted("Address setting"));
                } else {
                    Console.error(Console.MESSAGES.deletionFailed("Address Setting " + addressingPattern.getPattern()),
                            response.toString());
                }
                loadAddressingConfig();
            }
        });
    }

    public void launchNewAddrDialogue() {
        window = new DefaultWindow(Console.MESSAGES.createTitle("Addressing Setting"));
        window.setWidth(480);
        window.setHeight(360);
        window.addCloseHandler(event -> {});
        window.trapWidget(new NewAddressPatternWizard(this).asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void onSaveAddressDetails(final ActivemqAddressingPattern entity, Map<String, Object> changedValues) {
        ModelNode proto = new ModelNode();
        proto.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        proto.get(ADDRESS).set(Baseadress.get());
        proto.get(ADDRESS).add("subsystem", "messaging-activemq");
        proto.get(ADDRESS).add("server", getCurrentServer());
        proto.get(ADDRESS).add("address-setting", entity.getPattern());

        List<PropertyBinding> bindings = metaData.getBindingsForType(ActivemqAddressingPattern.class);
        ModelNode operation = ModelAdapter.detypedFromChangeset(proto, changedValues, bindings);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ResponseWrapper<Boolean> response = ModelAdapter.wrapBooleanResponse(result);
                if (response.getUnderlying()) {
                    Console.info(Console.MESSAGES.saved("Address Setting " + entity.getPattern()));
                } else {
                    Console.error(Console.MESSAGES.saveFailed("Address Setting " + entity.getPattern()),
                            response.getResponse().toString());
                }
                loadAddressingConfig();
            }
        });
    }

    public void onCreateAddressPattern(final ActivemqAddressingPattern address) {
        closeDialogue();

        ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", getCurrentServer());
        operation.get(ADDRESS).add("address-setting", address.getPattern());

        operation.get("dead-letter-address").set(address.getDeadLetterQueue());
        operation.get("expiry-address").set(address.getExpiryQueue());
        operation.get("max-delivery-attempts").set(address.getMaxDelivery());
        operation.get("redelivery-delay").set(address.getRedeliveryDelay());

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                boolean successful = response.get(OUTCOME).asString().equals(SUCCESS);
                if (successful) { Console.info(Console.MESSAGES.added("Address Setting")); } else {
                    Console.error(Console.MESSAGES.addingFailed("Address Setting") + address.getPattern(),
                            response.toString());
                }
                loadAddressingConfig();
            }
        });
    }

    void loadJMSConfig() {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());

        loadJMSCmd.execute(address, new SimpleCallback<AggregatedJMSModel>() {
            @Override
            public void onSuccess(AggregatedJMSModel result) {
                getJMSView().setConnectionFactories(result.getFactories());
                getJMSView().setQueues(result.getQueues());
                getJMSView().setTopics(result.getTopics());
            }
        });
    }

    public void onEditQueue() {
        getJMSView().enableEditQueue(true);
    }

    public void onSaveQueue(final String name, Map<String, Object> changedValues) {
        getJMSView().enableEditQueue(false);

        if (changedValues.isEmpty()) { return; }

        ModelNode proto = new ModelNode();
        proto.get(ADDRESS).set(Baseadress.get());
        proto.get(ADDRESS).add("subsystem", "messaging-activemq");
        proto.get(ADDRESS).add("server", getCurrentServer());
        proto.get(ADDRESS).add("jms-queue", name);

        // selector hack
        //if(changedValues.containsKey("selector") && changedValues.get("selector").equals(""))
        //    changedValues.put("selector", "undefined");

        ModelNode operation = queueAdapter.fromChangeset(changedValues, proto);
        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                caught.printStackTrace();
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                boolean successful = response.get(OUTCOME).asString().equals(SUCCESS);
                if (successful) {
                    Console.info(Console.MESSAGES.saved("queue " + name));
                } else {
                    Console.error(Console.MESSAGES.saveFailed("queue " + name), response.toString());
                }
                loadJMSConfig();
            }
        });
    }

    public void onCreateQueue(final ActivemqQueue entity) {
        closeDialogue();

        ModelNode queue = new ModelNode();
        queue.get(OP).set(ADD);
        queue.get(ADDRESS).set(Baseadress.get());
        queue.get(ADDRESS).add("subsystem", "messaging-activemq");
        queue.get(ADDRESS).add("server", getCurrentServer());
        queue.get(ADDRESS).add("jms-queue", entity.getName());

        List<String> jndiNames = entity.getEntries();
        if (jndiNames != null) {
            for (String jndiName : jndiNames) {
                queue.get("entries").add(jndiName);
            }
        }
        queue.get("durable").set(entity.isDurable());

        if (entity.getSelector() != null && !entity.getSelector().equals("")) {
            queue.get("selector").set(entity.getSelector());
        }

        dispatcher.execute(new DMRAction(queue), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                boolean successful = response.get(OUTCOME).asString().equals(SUCCESS);
                if (successful) {
                    Console.info(Console.MESSAGES.added("Queue " + entity.getName()));
                } else {
                    Console.error(Console.MESSAGES.addingFailed("Queue " + entity.getName()), response.toString());
                }
                scheduler.scheduleDeferred(MsgDestinationsPresenter.this::loadJMSConfig);
            }
        });
    }

    public void onDeleteQueue(final ActivemqQueue entity) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(REMOVE);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", getCurrentServer());
        operation.get(ADDRESS).add("jms-queue", entity.getName());

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                boolean successful = response.get(OUTCOME).asString().equals(SUCCESS);
                if (successful) {
                    Console.info(Console.MESSAGES.deleted("Queue " + entity.getName()));
                } else {
                    Console.error(Console.MESSAGES.deletionFailed("Queue " + entity.getName()), response.toString());
                }
                loadJMSConfig();
            }
        });
    }

    public void launchNewQueueDialogue() {
        window = new DefaultWindow(Console.MESSAGES.createTitle("JMS Queue"));
        window.setWidth(480);
        window.setHeight(360);
        window.addCloseHandler(event -> {});
        window.trapWidget(new NewQueueWizard(this).asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void onDeleteTopic(final ActivemqJMSEndpoint entity) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(REMOVE);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", getCurrentServer());
        operation.get(ADDRESS).add("jms-topic", entity.getName());

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                boolean successful = response.get(OUTCOME).asString().equals(SUCCESS);
                if (successful) {
                    Console.info(Console.MESSAGES.deleted("Topic" + entity.getName()));
                } else {
                    Console.error(Console.MESSAGES.deletionFailed("Topic " + entity.getName()), response.toString());
                }
                loadJMSConfig();
            }
        });
    }

    public void onEditTopic() {
        getJMSView().enableEditTopic(true);
    }

    public void onSaveTopic(final String name, Map<String, Object> changedValues) {
        getJMSView().enableEditTopic(false);

        if (changedValues.isEmpty()) { return; }

        ModelNode proto = new ModelNode();
        proto.get(ADDRESS).set(Baseadress.get());
        proto.get(ADDRESS).add("subsystem", "messaging-activemq");
        proto.get(ADDRESS).add("server", getCurrentServer());
        proto.get(ADDRESS).add("jms-topic", name);

        ModelNode operation = topicAdapter.fromChangeset(changedValues, proto);
        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                boolean successful = response.get(OUTCOME).asString().equals(SUCCESS);
                if (successful) {
                    Console.info(Console.MESSAGES.saved("Topic " + name));
                } else {
                    Console.error(Console.MESSAGES.saveFailed("Topic " + name), response.toString());
                }
                loadJMSConfig();
            }
        });
    }

    public void launchNewTopicDialogue() {
        window = new DefaultWindow(Console.MESSAGES.createTitle("JMS Topic"));
        window.setWidth(480);
        window.setHeight(360);
        window.addCloseHandler(event -> {});
        window.trapWidget(new NewTopicWizard(this).asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void closeDialogue() {
        window.hide();
    }

    public void onCreateTopic(final ActivemqJMSEndpoint entity) {
        closeDialogue();

        ModelNode topic = new ModelNode();
        topic.get(OP).set(ADD);
        topic.get(ADDRESS).set(Baseadress.get());
        topic.get(ADDRESS).add("subsystem", "messaging-activemq");
        topic.get(ADDRESS).add("server", getCurrentServer());
        topic.get(ADDRESS).add("jms-topic", entity.getName());

        List<String> jndiNames = entity.getEntries();
        if (jndiNames != null) {
            for (String jndiName : jndiNames) {
                topic.get("entries").add(jndiName);
            }
        }

        dispatcher.execute(new DMRAction(topic), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                boolean successful = response.get(OUTCOME).asString().equals(SUCCESS);
                if (successful) {
                    Console.info(Console.MESSAGES.added("Topic " + entity.getName()));
                } else {
                    Console.error(Console.MESSAGES.addingFailed("Topic " + entity.getName()), response.toString());
                }
                scheduler.scheduleDeferred(MsgDestinationsPresenter.this::loadJMSConfig);
            }
        });
    }

    private JMSView getJMSView() {
        return (JMSView) getView();
    }

    public void onSaveProviderConfig(Map<String, Object> changeset) {
        ModelNode proto = new ModelNode();
        proto.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        proto.get(ADDRESS).set(Baseadress.get());
        proto.get(ADDRESS).add("subsystem", "messaging-activemq");
        proto.get(ADDRESS).add("server", getCurrentServer());

        List<PropertyBinding> bindings = metaData.getBindingsForType(ActivemqMessagingProvider.class);
        ModelNode operation = ModelAdapter.detypedFromChangeset(proto, changeset, bindings);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ResponseWrapper<Boolean> response = ModelAdapter.wrapBooleanResponse(result);
                if (response.getUnderlying()) {
                    Console.info(Console.MESSAGES.saved("Provider Configuration " + getCurrentServer()));
                } else {
                    Console.error(Console.MESSAGES.saveFailed("Provider Configuration " + getCurrentServer()),
                            response.getResponse().toString());
                }
                loadProviderDetails();
            }
        });
    }

    public String getCurrentServer() {
        return currentServer;
    }

    @SuppressWarnings("unchecked")
    public void saveConnnectionFactory(String name, Map<String, Object> changeset) {
        ModelNode address = new ModelNode();
        address.get(ADDRESS).set(Baseadress.get());
        address.get(ADDRESS).add("subsystem", "messaging-activemq");
        address.get(ADDRESS).add("server", getCurrentServer());
        address.get(ADDRESS).add("connection-factory", name);

        ModelNode operation = factoryAdapter.fromChangeset(changeset, address);
        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.saveFailed("Connection Factory " + getCurrentServer()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.saved("Connection Factory " + getCurrentServer()));
                }
                loadJMSConfig();
            }
        });
    }

    public void onDeleteCF(final String name) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("connection-factory", name);

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.deletionFailed("Connection Factory " + name),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.deleted("Connection Factory " + name));
                }
                loadJMSConfig();
            }
        });
    }

    public void launchNewCFWizard() {
        window = new DefaultWindow(Console.MESSAGES.createTitle("Connection Factory"));
        window.setWidth(480);
        window.setHeight(360);
        window.trapWidget(new NewCFWizard(this).asWidget());
        window.setGlassEnabled(true);
        window.center();
    }

    public void launchNewDivertWizard() {
        loadExistingQueueNames(new AsyncCallback<List<String>>() {
            @Override
            public void onFailure(Throwable throwable) {
                Console.error("Failed to load queue names", throwable.getMessage());
            }

            @Override
            public void onSuccess(List<String> names) {
                window = new DefaultWindow(Console.MESSAGES.createTitle("Divert"));
                window.setWidth(480);
                window.setHeight(360);
                window.trapWidget(new NewDivertWizard(MsgDestinationsPresenter.this, names).asWidget());
                window.setGlassEnabled(true);
                window.center();
            }
        });
    }

    public void onCreateCF(final ActivemqConnectionFactory entity) {
        window.hide();

        // default values
        entity.setUseGlobalPools(true);

        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("connection-factory", entity.getName());

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(ADD);

        // jndi names
        operation.get("entries").setEmptyList();
        operation.get("entries").add(entity.getJndiName());

        // connectors
        for (String connector : entity.getConnectors()) {
            operation.get("connectors").add(connector);
        }

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.addingFailed("Connection Factory " + entity.getName()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.added("Connection Factory " + entity.getName()));
                }
                loadJMSConfig();
            }
        });
    }

    public void onCreateDivert(final ActivemqDivert entity) {
        window.hide();

        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("divert", entity.getRoutingName());

        ModelNode operation = divertAdapter.fromEntity(entity);
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(ADD);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.addingFailed("Divert " + entity.getRoutingName()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.added("Divert " + entity.getRoutingName()));
                }
                loadDiverts();
            }
        });
    }

    public void onDeleteDivert(final String name) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());
        address.add("divert", name);

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.deletionFailed("Divert " + name), response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.deleted("Divert " + name));
                }
                loadDiverts();
            }
        });
    }

    public void onSaveDivert(String name, Map<String, Object> changeset) {
        ModelNode address = new ModelNode();
        address.get(ADDRESS).set(Baseadress.get());
        address.get(ADDRESS).add("subsystem", "messaging-activemq");
        address.get(ADDRESS).add("server", getCurrentServer());
        address.get(ADDRESS).add("divert", name);

        ModelNode operation = divertAdapter.fromChangeset(changeset, address);
        //System.out.println(operation);
        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.saveFailed("Divert " + getCurrentServer()),
                            response.getFailureDescription());
                } else {
                    Console.info(Console.MESSAGES.saved("Divert " + getCurrentServer()));
                }
                loadDiverts();
            }
        });
    }

    public void loadExistingQueueNames(final AsyncCallback<List<String>> callback) {
        ModelNode address = Baseadress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", getCurrentServer());

        loadJMSCmd.execute(address, new SimpleCallback<AggregatedJMSModel>() {
            @Override
            public void onSuccess(AggregatedJMSModel result) {
                final List<String> names = new ArrayList<>();
                for (ActivemqQueue queue : result.getQueues()) {
                    names.add(queue.getName());
                }
                for (ActivemqJMSEndpoint topic : result.getTopics()) {
                    names.add(topic.getName());
                }
                callback.onSuccess(names);
            }
        });

    }

    @Override
    public void onAddResource(ResourceAddress address, final ModelNode payload) {
        // ignore address, currently one use supported only
        window.hide();

        ModelNode op = address.asOperation(payload);
        op.get(OP).set(ADD);
        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error("Failed to add messaging provider", response.getFailureDescription());

                } else {
                    Console.info("Added messaging provider " + payload.get(NAME).asString());
                    currentServer = null;
                    loadProvider();
                }
            }
        });
    }

    @Override
    public void launchAddProviderDialog() {
        window = new DefaultWindow(Console.MESSAGES.createTitle("Messaging Provider"));
        window.setWidth(480);
        window.setHeight(360);

        window.trapWidget(
                new AddResourceDialog(
                        "{selected.profile}/subsystem=messaging-activemq/hornetq=*",
                        Console.MODULES.getSecurityFramework()
                                .getSecurityContext(NameTokens.ActivemqMessagingPresenter),
                        MsgDestinationsPresenter.this
                )
        );
        window.setGlassEnabled(true);
        window.center();
    }

    @Override
    public void removeProvider(final String name) {
        ModelNode op = new ModelNode();
        op.get(ADDRESS).set(Baseadress.get());
        op.get(ADDRESS).add("subsystem", "messaging-activemq");
        op.get(ADDRESS).add("server", name);
        op.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error("Failed to remove messaging provider", response.getFailureDescription());
                } else {
                    if (name.equals(currentServer)) { currentServer = null; }
                    Console.info("Removed messaging provider " + name);
                    loadProvider();
                }
            }
        });
    }

    @Override
    public void onSaveProvider(final String name, Map<String, Object> changeset) {
        ModelNodeAdapter adapter = new ModelNodeAdapter();

        ModelNode address = new ModelNode();
        address.get(ADDRESS).set(Baseadress.get());
        address.get(ADDRESS).add("subsystem", "messaging-activemq");
        address.get(ADDRESS).add("server", name);
        ModelNode operation = adapter.fromChangeset(changeset, address);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();

                if (response.isFailure()) {
                    Console.error("Failed to save provider " + name, response.getFailureDescription());
                } else {
                    Console.info("Successfully saved provider " + name);
                    loadProvider(); // refresh
                }
            }
        });
    }

    @Override
    public SecurityFramework getSecurityFramework() {
        return securityFramework;
    }

    @Override
    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return descriptionRegistry;
    }

    @Override
    public String getNameToken() {
        return getProxy().getNameToken();
    }
}
