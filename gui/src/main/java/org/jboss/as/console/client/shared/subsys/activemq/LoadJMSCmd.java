package org.jboss.as.console.client.shared.subsys.activemq;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.LoggingCallback;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnectionFactory;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqJMSEndpoint;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqQueue;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.AsyncCommand;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 12/10/11
 */
public class LoadJMSCmd implements AsyncCommand<AggregatedJMSModel> {

    private DispatchAsync dispatcher;
    private BeanFactory factory;
    private EntityAdapter<ActivemqConnectionFactory> factoryAdapter;

    public LoadJMSCmd(DispatchAsync dispatcher, BeanFactory factory, ApplicationMetaData metaData) {
        this.dispatcher = dispatcher;
        this.factory = factory;
        this.factoryAdapter = new EntityAdapter<>(ActivemqConnectionFactory.class, metaData);
    }

    @Override
    public void execute(AsyncCallback<AggregatedJMSModel> topicsAndQueuesAsyncCallback) {
        throw new RuntimeException("Use overridden method instead!");
    }

    public void execute(ModelNode address, final AsyncCallback<AggregatedJMSModel> callback) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(RECURSIVE).set(Boolean.TRUE);
        operation.get(ADDRESS).set(address);

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    callback.onFailure(
                            new RuntimeException("Failed to load JMS endpoints:" + response.getFailureDescription()));
                } else {
                    ModelNode payload = response.get("result").asObject();

                    List<ActivemqConnectionFactory> factories = parseFactories(payload);
                    List<ActivemqQueue> queues = parseQueues(payload);
                    List<ActivemqJMSEndpoint> topics = parseTopics(payload);

                    AggregatedJMSModel model = new AggregatedJMSModel(factories, queues, topics);
                    callback.onSuccess(model);
                }
            }
        });
    }

    private List<ActivemqConnectionFactory> parseFactories(ModelNode response) {
        List<ActivemqConnectionFactory> factoryModels = new ArrayList<>();
        try {
            // factories
            if (response.hasDefined("connection-factory")) {
                List<Property> factories = response.get("connection-factory").asPropertyList();

                for (Property factoryProp : factories) {
                    String name = factoryProp.getName();

                    ModelNode factoryValue = factoryProp.getValue();
                    String jndi = factoryValue.get("entries").asList().get(0).asString();

                    ActivemqConnectionFactory connectionFactory = factoryAdapter.fromDMR(factoryValue);
                    connectionFactory.setName(name);
                    connectionFactory.setJndiName(jndi);

                    if (factoryValue.hasDefined("connector")) {
                        List<Property> items = factoryValue.get("connector").asPropertyList();
                        String list = "";
                        for (Property item : items) { list += " " + item.getName(); }

                        connectionFactory.setConnector(list);
                    }
                    factoryModels.add(connectionFactory);
                }
            }
        } catch (Throwable e) {
            Console.error("Failed to parse connection factories: " + e.getMessage());
        }
        return factoryModels;
    }

    private List<ActivemqQueue> parseQueues(ModelNode response) {
        List<ActivemqQueue> queues = new ArrayList<>();

        if (response.hasDefined("jms-queue")) {
            List<Property> propList = response.get("jms-queue").asPropertyList();

            for (Property prop : propList) {
                ActivemqQueue queue = factory.activemqQueue().as();
                queue.setName(prop.getName());

                ModelNode propValue = prop.getValue();
                List<ModelNode> entries = propValue.get("entries").asList();
                List<String> values = new ArrayList<>(entries.size());
                for (ModelNode entry : entries) {
                    values.add(entry.asString());
                }
                queue.setEntries(values);

                if (propValue.hasDefined("durable")) { queue.setDurable(propValue.get("durable").asBoolean()); }
                if (propValue.hasDefined("selector")) { queue.setSelector(propValue.get("selector").asString()); }

                queues.add(queue);
            }
        }
        return queues;

    }

    private List<ActivemqJMSEndpoint> parseTopics(ModelNode response) {
        List<ActivemqJMSEndpoint> topics = new ArrayList<>();

        if (response.hasDefined("jms-topic")) {
            List<Property> propList = response.get("jms-topic").asPropertyList();

            for (Property prop : propList) {
                ActivemqJMSEndpoint topic = factory.activemqTopic().as();
                topic.setName(prop.getName());

                List<ModelNode> entries = prop.getValue().get("entries").asList();
                List<String> values = new ArrayList<>(entries.size());
                for (ModelNode entry : entries) {
                    values.add(entry.asString());
                }
                topic.setEntries(values);
                topics.add(topic);
            }
        }
        return topics;
    }
}
