package org.jboss.as.console.client.v3.stores.domain.actions;

import org.jboss.gwt.circuit.Action;

/**
 * @author Heiko Braun
 * @date 15/07/14
 */
public class SelectServerInstance implements Action {

    private final String server;

    public SelectServerInstance(String server) {
        this.server = server;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SelectServerInstance)) return false;

        SelectServerInstance that = (SelectServerInstance) o;

        if (!server.equals(that.server)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return server.hashCode();
    }

    public String getServer() {
        return server;
    }
}
