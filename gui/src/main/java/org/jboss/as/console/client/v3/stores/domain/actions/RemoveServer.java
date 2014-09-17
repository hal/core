package org.jboss.as.console.client.v3.stores.domain.actions;

import org.jboss.as.console.client.domain.model.Server;
import org.jboss.gwt.circuit.Action;

/**
 * @author Heiko Braun
 * @date 15/07/14
 */
public class RemoveServer implements Action {

    private Server server;

    public RemoveServer(Server server) {
        this.server = server;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemoveServer)) return false;

        RemoveServer that = (RemoveServer) o;

        if (!server.equals(that.server)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return server.hashCode();
    }

    public Server getServer() {
        return server;
    }
}
