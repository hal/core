package org.jboss.as.console.client.standalone.runtime;

/**
 * @author Heiko Braun
 * @since 16/06/15
 */
public class StandaloneServer {
    boolean requiresReload;

    public StandaloneServer(boolean requiresReload) {

        this.requiresReload = requiresReload;
    }

    public boolean isRequiresReload() {
        return requiresReload;
    }

    public void setRequiresReload(boolean requiresReload) {
        this.requiresReload = requiresReload;
    }

    public String getTitle() {
        return "Standalone Server";
    }
}
