package org.jboss.as.console.client.v3.stores.domain.actions;

import org.jboss.as.console.client.domain.model.Server;
import org.jboss.gwt.circuit.Action;

import java.util.Map;

/**
 * @author Heiko Braun
 * @date 15/07/14
 */
public class UpdateServer implements Action<UpdateServer.Values> {
    private Server server;
    private Map<String, Object> changedValues;

    public UpdateServer(Server server, Map<String, Object> changedValues) {
        this.server = server;
        this.changedValues = changedValues;
    }

    @Override
    public Values getPayload() {
        return new Values(server, changedValues);
    }

    public class Values {
        private Server server;
        private Map<String, Object> changedValues;

        Values(Server server, Map<String, Object> changedValues) {
            this.server = server;
            this.changedValues = changedValues;
        }

        public Server getServer() {
            return server;
        }

        public Map<String, Object> getChangedValues() {
            return changedValues;
        }
    }
}
