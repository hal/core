package org.jboss.as.console.client.v3.stores.domain.actions;

import org.jboss.gwt.circuit.Action;

/**
 * @author Heiko Braun
 * @date 15/07/14
 */
public class SelectServerInstance implements Action<String> {
    private String server;

    public SelectServerInstance(String server) {
        this.server = server;
    }

    @Override
    public String getPayload() {
        return server;
    }
}
