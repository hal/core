package org.jboss.as.console.client.shared.runtime.activemq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.CircuitPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.LoggingCallback;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.activemq.model.PreparedTransaction;
import org.jboss.as.console.client.shared.subsys.messaging.AggregatedJMSModel;
import org.jboss.as.console.client.shared.subsys.messaging.LoadJMSCmd;
import org.jboss.as.console.client.shared.subsys.messaging.model.JMSEndpoint;
import org.jboss.as.console.client.shared.subsys.messaging.model.Queue;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.stores.domain.ServerStore;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;
import org.useware.kernel.gui.behaviour.StatementContext;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 12/9/11
 */
public class ActivemqMetricPresenter extends CircuitPresenter<ActivemqMetricPresenter.MyView, ActivemqMetricPresenter.MyProxy> {

    public static final AddressTemplate RUNTIME_MESSAGING_SERVER = AddressTemplate.
            of("/{implicit.host}/{selected.server}/subsystem=messaging-activemq/server=*");

    @ProxyCodeSplit
    @NameToken(NameTokens.ActivemqMetricPresenter)
    @RequiredResources(resources = {
        "/{implicit.host}/{selected.server}/subsystem=messaging-activemq/server=*"
    })
    @SearchIndex(keywords = {"jms", "queue", "topic", "size"})
    public interface MyProxy extends Proxy<ActivemqMetricPresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(ActivemqMetricPresenter presenter);
        void clearSamples();

        void setTopics(List<JMSEndpoint> topics);
        void setQueues(List<Queue> queues);

        void updateQueueMetrics(ModelNode result);

        void updateTopicMetrics(ModelNode result);

        void updateProvider(List<Property> provider);

        void setSelectedProvider(String name);
        void setPooledConnectionFactoryModel(List<Property> model);

        void setTransactions(List<PreparedTransaction> transactions);
    }

    private final PlaceManager placemanager;
    private SecurityFramework securityFramework;
    private StatementContext statementContext;
    private ResourceDescriptionRegistry descriptionRegistry;
    private DispatchAsync dispatcher;
    private RevealStrategy revealStrategy;
    private JMSEndpoint selectedTopic;
    private LoadJMSCmd loadJMSCmd;
    private Queue selectedQueue;
    private final ServerStore serverStore;
    private String currentServer;

    @Inject
    public ActivemqMetricPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            DispatchAsync dispatcher, Dispatcher circuit,
            ApplicationMetaData metaData, RevealStrategy revealStrategy,
            ServerStore serverStore, BeanFactory factory, PlaceManager placemanager,
            SecurityFramework securityFramework, StatementContext statementContext,
            ResourceDescriptionRegistry descriptionRegistry) {
        super(eventBus, view, proxy, circuit);

        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
        this.serverStore = serverStore;
        this.placemanager = placemanager;
        this.securityFramework = securityFramework;
        this.statementContext = statementContext;
        this.descriptionRegistry = descriptionRegistry;
        this.loadJMSCmd = new LoadJMSCmd(dispatcher, factory, metaData);
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {
           currentServer = request.getParameter("name", null);
       }

    public void setSelectedTopic(JMSEndpoint topic) {
        this.selectedTopic= topic;
        if(topic!=null)
            loadTopicMetrics();

    }

    public void setSelectedQueue(Queue queue) {
        this.selectedQueue = queue;
        if(queue!=null)
            loadQueueMetrics();

    }

    public void loadProvider() {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(ADDRESS).set(RuntimeBaseAddress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(CHILD_TYPE).set("server");

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if(response.isFailure())
                {
                    Log.error("Failed to load messaging server", response.getFailureDescription());
                    getView().updateProvider(Collections.EMPTY_LIST);
                }
                else
                {
                    getView().updateProvider(response.get(RESULT).asPropertyList());
                    getView().setSelectedProvider(currentServer);
                }
            }
        });
    }

    public void refreshResources(String selectedProvider) {
        refreshQueuesAndTopics(selectedProvider);
        loadPooledConnectionFactory(selectedProvider);
    }

    public void loadPooledConnectionFactory(String selectedProvider) {

        org.jboss.as.console.client.v3.dmr.ResourceAddress pooledAddress = RUNTIME_MESSAGING_SERVER
                .resolve(statementContext, selectedProvider);
        Operation op = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, pooledAddress)
                .param(CHILD_TYPE, "pooled-connection-factory")
                .param(RECURSIVE, true)
                .param(INCLUDE_RUNTIME, true)
                .build();

        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.failed("Loading pooled connection factory " + selectedProvider),
                            response.getFailureDescription());
                } else {
                    List<Property> model = response.get(RESULT).asPropertyList();
                    getView().setPooledConnectionFactoryModel(model);
                }
            }
        });
    }

    public void refreshQueuesAndTopics(String selectedProvider) {

        getView().clearSamples();
        getView().setTopics(Collections.EMPTY_LIST);
        getView().setQueues(Collections.EMPTY_LIST);

        ModelNode address = RuntimeBaseAddress.get();
        address.add("subsystem", "messaging-activemq");
        address.add("server", selectedProvider);

        loadJMSCmd.execute(address, new LoggingCallback<AggregatedJMSModel>() {

            @Override
            public void onFailure(Throwable caught) {
                Log.error(caught.getMessage());
            }

            @Override
            public void onSuccess(AggregatedJMSModel result) {
                getView().setTopics(result.getTopics());
                getView().setQueues(result.getQueues());
            }
        });

        loadTransactions();
    }

    private void loadQueueMetrics() {
        if(null==selectedQueue)
            throw new RuntimeException("Queue selection is null!");

        getView().clearSamples();

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(RuntimeBaseAddress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", currentServer);
        operation.get(ADDRESS).add("jms-queue", selectedQueue.getName());

        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();

                if(response.isFailure())
                {
                    Console.error("Error loading metrics", response.getFailureDescription());
                }
                else
                {
                    Console.info("Successfully refreshed metrics for queue "+ selectedQueue.getName());
                    ModelNode result = response.get(RESULT).asObject();
                    getView().updateQueueMetrics(result);

                }
            }
        });
    }

    private void loadTopicMetrics() {

        if(null==selectedTopic)
            throw new RuntimeException("Topic selection is null!");

        getView().clearSamples();

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(RuntimeBaseAddress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", currentServer);
        operation.get(ADDRESS).add("jms-topic", selectedTopic.getName());

        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();

                if(response.isFailure())
                {
                    Console.error("Error loading metrics", response.getFailureDescription());
                }
                else
                {
                    Console.info("Successfully refreshed metrics for topic "+ selectedTopic.getName());
                    ModelNode result = response.get(RESULT).asObject();
                    getView().updateTopicMetrics(result);
                }
            }
        });
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        addChangeHandler(serverStore);
    }

    @Override
    protected void onAction(Action action) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                loadProvider();
            }
        });
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadProvider();
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInRuntimeParent(this);
    }

    public void onFlushQueue(final Queue queue) {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(RuntimeBaseAddress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", currentServer);
        operation.get(ADDRESS).add("jms-queue", queue.getName());

        operation.get(OP).set("remove-messages");

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if(response.isFailure())
                {
                    Console.error("Failed to flush queue "+queue.getName());
                }
                else
                {
                    Console.info("Successfully flushed queue " + queue.getName());
                }

                refreshQueuesAndTopics(currentServer);
            }
        });

    }

    public void onFlushTopic(final JMSEndpoint topic) {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(RuntimeBaseAddress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", currentServer);
        operation.get(ADDRESS).add("jms-topic", topic.getName());

        operation.get(OP).set("remove-messages");

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if(response.isFailure())
                {
                    Console.error("Failed to flush topic "+topic.getName());
                }
                else
                {
                    Console.info("Successfully flushed topic "+topic.getName());
                }

                refreshQueuesAndTopics(currentServer);
            }
        });

    }

    public PlaceManager getPlaceManager() {
        return placemanager;
    }

    public SecurityFramework getSecurityFramework() {
        return securityFramework;
    }

    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return descriptionRegistry;
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }

    public DispatchAsync getDispatcher() {
        return dispatcher;
    }

    public String getCurrentServer() {
        return currentServer;
    }

    protected void onCommit(PreparedTransaction transaction) {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(RuntimeBaseAddress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", currentServer);
        operation.get(OP).set("commit-prepared-transaction");
        operation.get("transaction-as-base-64").set(transaction.getXid());

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error("Failed to commit transaction", response.getFailureDescription());
                }
                loadTransactions();
            }
        });
    }

    protected void onRollback(PreparedTransaction transaction) {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(RuntimeBaseAddress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", currentServer);
        operation.get(OP).set("rollback-prepared-transaction");
        operation.get("transaction-as-base-64").set(transaction.getXid());

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error("Failed to rollback transaction", response.getFailureDescription());
                }
                loadTransactions();
            }
        });
    }

    private List<PreparedTransaction> parseTransactions(List<ModelNode> transactions) {
        RegExp transactionPattern = RegExp.compile("^(.*) base64: ([^ ]*)");
        List<PreparedTransaction> preparedTransactions = new ArrayList<>();

        for(ModelNode t : transactions) {
            MatchResult match = transactionPattern.exec(t.asString());
            if (match == null) {
                Console.error("Error parsing prepared transactions");
                break;
            }
            preparedTransactions.add(new PreparedTransaction(match.getGroup(2), match.getGroup(1)));
        }
        return preparedTransactions;
    }

    public void loadTransactions() {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(RuntimeBaseAddress.get());
        operation.get(ADDRESS).add("subsystem", "messaging-activemq");
        operation.get(ADDRESS).add("server", currentServer);
        operation.get(OP).set("list-prepared-transactions");

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                ModelNode transactions = response.get(RESULT);
                if (response.isFailure()) {
                    Console.error("Unable to load transaction", response.getFailureDescription());
                } else {
                    getView().setTransactions(parseTransactions(transactions.asList()));
                }
            }
        });
    }
}
