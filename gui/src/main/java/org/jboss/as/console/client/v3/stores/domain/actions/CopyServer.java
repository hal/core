package org.jboss.as.console.client.v3.stores.domain.actions;

import org.jboss.as.console.client.domain.model.Server;
import org.jboss.gwt.circuit.Action;

/**
 * @author Heiko Braun
 */
public class CopyServer implements Action {

    private final String targetHost;
    private final Server original;
    private final Server newServer;

    public CopyServer(String targetHost, Server original, Server newServer) {
        this.targetHost = targetHost;
        this.original = original;
        this.newServer = newServer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CopyServer)) return false;

        CopyServer that = (CopyServer) o;

        if (!newServer.equals(that.newServer)) return false;
        if (!original.equals(that.original)) return false;
        if (!targetHost.equals(that.targetHost)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = targetHost.hashCode();
        result = 31 * result + original.hashCode();
        result = 31 * result + newServer.hashCode();
        return result;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public Server getOriginal() {
        return original;
    }

    public Server getNewServer() {
        return newServer;
    }
}
