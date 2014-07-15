package org.jboss.as.console.client.v3.stores.domain.actions;

import org.jboss.as.console.client.domain.model.Server;
import org.jboss.gwt.circuit.Action;

/**
 * @author Heiko Braun
 * @date 15/07/14
 */
public class CopyServer implements Action<CopyServer.Values> {


    private final String targetHost;
    private final Server original;
    private final Server newServer;

    public CopyServer(String targetHost, Server original, Server newServer) {

        this.targetHost = targetHost;
        this.original = original;
        this.newServer = newServer;
    }

    @Override
    public Values getPayload() {
        return new Values(targetHost,original, newServer);
    }

    public class Values{
        private final String targetHost;
        private final Server original;
        private final Server newServer;

        Values(String targetHost, Server original, Server newServer) {
            this.targetHost = targetHost;
            this.original = original;
            this.newServer = newServer;
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
}
