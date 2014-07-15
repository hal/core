package org.jboss.as.console.client.v3.stores.domain.actions;

import org.jboss.as.console.client.domain.model.Server;
import org.jboss.gwt.circuit.Action;

/**
 * @author Heiko Braun
 * @date 15/07/14
 */
public class RemoveServer implements Action<Server> {

    private Server server;

    public RemoveServer(Server server) {
        this.server = server;
    }

    @Override
    public Server getPayload() {
        return server;
    }
}
