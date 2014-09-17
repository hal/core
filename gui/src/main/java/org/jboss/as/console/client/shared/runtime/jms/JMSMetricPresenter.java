package org.jboss.as.console.client.shared.runtime.jms;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Scheduler;
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
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.messaging.AggregatedJMSModel;
import org.jboss.as.console.client.shared.subsys.messaging.LoadJMSCmd;
import org.jboss.as.console.client.shared.subsys.messaging.model.JMSEndpoint;
import org.jboss.as.console.client.shared.subsys.messaging.model.Queue;
import org.jboss.as.console.client.v3.stores.domain.ServerStore;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.Collections;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 12/9/11
 */
public class JMSMetricPresenter extends CircuitPresenter<JMSMetricPresenter.MyView, JMSMetricPresenter.MyProxy>
{

    private final PlaceManager placemanager;
    private DispatchAsync dispatcher;
    private RevealStrategy revealStrategy;
    private JMSEndpoint selectedTopic;
    private BeanFactory factory;
    private LoadJMSCmd loadJMSCmd;
    private Queue selectedQueue;
    private final ServerStore serverStore;
    private String currentServer;

    @ProxyCodeSplit
    @NameToken(NameTokens.JmsMetricPresenter)
    @AccessControl(
            resources = {
                    "/{selected.host}/{selected.server}/subsystem=messaging/hornetq-server=*"
            }
    )
    @SearchIndex(keywords = {
            "jms", "queue", "topic", "size"
    })
    public interface MyProxy extends Proxy<JMSMetricPresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(JMSMetricPresenter presenter);
        void clearSamples();

        void setTopics(List<JMSEndpoint> topics);
        void setQueues(List<Queue> queues);

        void updateQueueMetrics(ModelNode result);

        void updateTopicMetrics(ModelNode result);

        void updateProvider(List<Property> provider);

        void setSelectedProvider(String name);
    }

    @Inject
    public JMSMetricPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            DispatchAsync dispatcher, Dispatcher circuit,
            ApplicationMetaData metaData, RevealStrategy revealStrategy,
            ServerStore serverStore, BeanFactory factory, PlaceManager placemanager) {
        super(eventBus, view, proxy, circuit);

        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
        this.serverStore = serverStore;
        this.factory = factory;
        this.placemanager = placemanager;

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
        operation.get(ADDRESS).add("subsystem", "messaging");
        operation.get(CHILD_TYPE).set("hornetq-server");

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if(response.isFailure())
                {
                    Log.error("Failed to load hornetq server", response.getFailureDescription());
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

    public void refreshQueuesAndTopics(String selectedProvider) {

        getView().clearSamples();
        getView().setTopics(Collections.EMPTY_LIST);
        getView().setQueues(Collections.EMPTY_LIST);

        ModelNode address = RuntimeBaseAddress.get();
        address.add("subsystem", "messaging");
        address.add("hornetq-server", selectedProvider);

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
    }

    private void loadQueueMetrics() {
        if(null==selectedQueue)
            throw new RuntimeException("Queue selection is null!");

        getView().clearSamples();

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(RuntimeBaseAddress.get());
        operation.get(ADDRESS).add("subsystem", "messaging");
        operation.get(ADDRESS).add("hornetq-server", "default");
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
        operation.get(ADDRESS).add("subsystem", "messaging");
        operation.get(ADDRESS).add("hornetq-server", "default");
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
        operation.get(ADDRESS).add("subsystem", "messaging");
        operation.get(ADDRESS).add("hornetq-server", "default");
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
        operation.get(ADDRESS).add("subsystem", "messaging");
        operation.get(ADDRESS).add("hornetq-server", "default");
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


}
