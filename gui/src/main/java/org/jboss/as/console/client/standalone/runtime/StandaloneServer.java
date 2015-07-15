package org.jboss.as.console.client.standalone.runtime;

import org.jboss.as.console.client.domain.model.SuspendState;

/**
 * @author Heiko Braun
 * @since 16/06/15
 */
public class StandaloneServer {
    boolean requiresReload;
    private final SuspendState suspendState;

    public StandaloneServer(boolean requiresReload, SuspendState suspendState) {

        this.requiresReload = requiresReload;
        this.suspendState = suspendState;
    }

    public boolean isRequiresReload() {
        return requiresReload;
    }

    public SuspendState getSuspendState() {
        return suspendState;
    }

    public String getTitle() {
        return "Standalone Server";
    }
}
