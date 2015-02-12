package org.jboss.as.console.client.v3.stores.domain;

/**
 * @author Heiko Braun
 * @since 12/02/15
 */
public final class ServerRef {
    String hostName;
    String serverName;

    public ServerRef(String hostName, String serverName) {
        this.hostName = hostName;
        this.serverName = serverName;
    }

    public String getHostName() {
        return hostName;
    }

    public String getServerName() {
        return serverName;
    }
}
