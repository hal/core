package org.jboss.as.console.client.v3.stores.domain.actions;

import org.jboss.gwt.circuit.Action;

/**
 * @author Heiko Braun
 * @date 15/07/14
 */
public class HostSelection implements Action {

    private String hostName;

    public HostSelection(String hostName) {
        this.hostName = hostName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HostSelection)) return false;

        HostSelection that = (HostSelection) o;

        if (!hostName.equals(that.hostName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return hostName.hashCode();
    }

    public String getHostName() {
        return hostName;
    }
}
