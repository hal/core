package org.jboss.as.console.client.v3.stores.domain.actions;

import org.jboss.gwt.circuit.Action;

/**
 * @author Heiko Braun
 * @date 15/07/14
 */
public class HostSelection implements Action<String> {

    private String hostName;

    public HostSelection(String hostName) {
        this.hostName = hostName;
    }

    @Override
    public String getPayload() {
        return hostName;
    }
}
