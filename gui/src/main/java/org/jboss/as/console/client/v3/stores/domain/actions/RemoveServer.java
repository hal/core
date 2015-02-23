package org.jboss.as.console.client.v3.stores.domain.actions;

import org.jboss.as.console.client.v3.stores.domain.ServerRef;
import org.jboss.gwt.circuit.Action;

/**
 * @author Heiko Braun
 * @date 15/07/14
 */
public class RemoveServer implements Action {

    private final ServerRef server;

    public RemoveServer(ServerRef server) {
        this.server = server;
    }

    public ServerRef getServerRef() {
        return server;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RemoveServer that = (RemoveServer) o;

        if (!server.equals(that.server)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return server.hashCode();
    }
}
