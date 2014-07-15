package org.jboss.as.console.client.v3.stores.domain.actions;

import org.jboss.as.console.client.domain.model.ServerInstance;
import org.jboss.gwt.circuit.Action;

/**
 * @author Heiko Braun
 * @date 15/07/14
 */
public class RefreshServerInstances implements Action<ServerInstance> {
    private ServerInstance server;

    public RefreshServerInstances(ServerInstance server) {
        this.server = server;
    }

    @Override
    public ServerInstance getPayload() {
        return server;
    }
}
