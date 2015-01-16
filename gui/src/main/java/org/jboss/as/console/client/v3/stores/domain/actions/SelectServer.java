package org.jboss.as.console.client.v3.stores.domain.actions;

import org.jboss.gwt.circuit.Action;

/**
 * @author Heiko Braun
 * @since 16/01/15
 */
public class SelectServer implements Action {

    private final String server;
    private final String host;

    public SelectServer(String host, String server) {
        this.host = host;
        this.server = server;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SelectServer that = (SelectServer) o;

        if (!host.equals(that.host)) return false;
        if (!server.equals(that.server)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = server.hashCode();
        result = 31 * result + host.hashCode();
        return result;
    }

    public String getServer() {
        return server;
    }

    public String getHost() {
        return host;
    }
}
