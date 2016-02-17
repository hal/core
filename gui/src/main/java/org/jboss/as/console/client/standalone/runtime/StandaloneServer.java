package org.jboss.as.console.client.standalone.runtime;

import org.jboss.as.console.client.domain.model.SrvState;
import org.jboss.as.console.client.domain.model.SuspendState;

/**
 * @author Heiko Braun
 * @since 16/06/15
 */
public class StandaloneServer {
    private final SrvState configState;
    private final SuspendState suspendState;

    public StandaloneServer(SrvState state, SuspendState suspendState) {
        this.configState = state;
        this.suspendState = suspendState;
    }

    public boolean isRequiresReload() {
        return false;
    }

    public SuspendState getSuspendState() {
        return suspendState;
    }

    public String getTitle() {
        return "Standalone Server";
    }

    public SrvState getConfigState() {
        return configState;
    }
}
