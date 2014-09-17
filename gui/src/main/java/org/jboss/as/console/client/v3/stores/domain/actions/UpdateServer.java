package org.jboss.as.console.client.v3.stores.domain.actions;

import org.jboss.as.console.client.domain.model.Server;
import org.jboss.gwt.circuit.Action;

import java.util.Map;

/**
 * @author Heiko Braun
 * @date 15/07/14
 */
public class UpdateServer implements Action {

    private final Server server;
    private final Map<String, Object> changedValues;

    public UpdateServer(Server server, Map<String, Object> changedValues) {
        this.server = server;
        this.changedValues = changedValues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UpdateServer)) return false;

        UpdateServer that = (UpdateServer) o;

        if (!changedValues.equals(that.changedValues)) return false;
        if (!server.equals(that.server)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = server.hashCode();
        result = 31 * result + changedValues.hashCode();
        return result;
    }

    public Server getServer() {
        return server;
    }

    public Map<String, Object> getChangedValues() {
        return changedValues;
    }
}
