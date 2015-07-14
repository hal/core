package org.jboss.as.console.client.shared.subsys.activemq;

import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnectionFactory;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqJMSEndpoint;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqQueue;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 12/10/11
 */
public class AggregatedJMSModel {

    private List<ActivemqConnectionFactory> factories;
    private List<ActivemqQueue> queues;
    private List<ActivemqJMSEndpoint> topics;

    public AggregatedJMSModel(
            List<ActivemqConnectionFactory> factories,
            List<ActivemqQueue> queues,
            List<ActivemqJMSEndpoint> topics) {
        this.factories = factories;
        this.queues = queues;
        this.topics = topics;
    }

    public List<ActivemqConnectionFactory> getFactories() {
        return factories;
    }

    public List<ActivemqQueue> getQueues() {
        return queues;
    }

    public List<ActivemqJMSEndpoint> getTopics() {
        return topics;
    }
}
