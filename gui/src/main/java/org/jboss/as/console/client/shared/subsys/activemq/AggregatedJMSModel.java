package org.jboss.as.console.client.shared.subsys.activemq;

import java.util.List;

import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnectionFactory;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqCoreQueue;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqJMSEndpoint;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqJMSQueue;

/**
 * @author Heiko Braun
 * @date 12/10/11
 */
public class AggregatedJMSModel {

    private List<ActivemqConnectionFactory> factories;
    private List<ActivemqJMSQueue> jmsQueues;
    private List<ActivemqCoreQueue> coreQueues;
    private List<ActivemqJMSEndpoint> topics;

    public AggregatedJMSModel(
            List<ActivemqConnectionFactory> factories,
            List<ActivemqJMSQueue> jmsQueues,
            List<ActivemqJMSEndpoint> topics,
            List<ActivemqCoreQueue> queues) {
        this.factories = factories;
        this.jmsQueues = jmsQueues;
        this.topics = topics;
        this.coreQueues = queues;
    }

    public List<ActivemqConnectionFactory> getFactories() {
        return factories;
    }

    public List<ActivemqJMSQueue> getJMSQueues() {
        return jmsQueues;
    }

    public List<ActivemqJMSEndpoint> getTopics() {
        return topics;
    }

    public List<ActivemqCoreQueue> getQueues() {
        return coreQueues;
    }
}
