package org.jboss.as.console.client.shared.runtime.jms;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Scheduler;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.LoggingCallback;
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
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.PropagatesChange;

import java.util.Collections;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 12/9/11
 */
public class JMSMetricPresenter extends Presenter<JMSMetricPresenter.MyView, JMSMetricPresenter.MyProxy>
        {

    private DispatchAsync dispatcher;
    private RevealStrategy revealStrategy;
    private JMSEndpoint selectedTopic;
    private BeanFactory factory;
    private LoadJMSCmd loadJMSCmd;
    private Queue selectedQueue;
    private final ServerStore serverStore;


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
    }

    @Inject
    public JMSMetricPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            DispatchAsync dispatcher,
            ApplicationMetaData metaData, RevealStrategy revealStrategy,
            ServerStore serverStore, BeanFactory factory) {
        super(eventBus, view, proxy);

        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
        this.serverStore = serverStore;
        this.factory = factory;

        this.loadJMSCmd = new LoadJMSCmd(dispatcher, factory, metaData);
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

    public void refresh() {


        getView().clearSamples();
        getView().setTopics(Collections.EMPTY_LIST);
        getView().setQueues(Collections.EMPTY_LIST);

        ModelNode address = RuntimeBaseAddress.get();
        address.add("subsystem", "messaging");
        address.add("hornetq-server", "default");

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
        serverStore.addChangeHandler(new PropagatesChange.Handler() {
            @Override
            public void onChange(Class<?> source) {
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                            @Override
                            public void execute() {
                                if(isVisible()) refresh();
                            }
                        });
            }
        });
    }


    @Override
    protected void onReset() {
        super.onReset();
        refresh();
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

                refresh();
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

                refresh();
            }
        });

    }

}
