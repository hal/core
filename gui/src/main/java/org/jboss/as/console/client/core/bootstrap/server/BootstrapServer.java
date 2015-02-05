package org.jboss.as.console.client.core.bootstrap.server;

/**
 * AutoBean which represents a server instance used when the console starts.
 *
 * @author Harald Pehl
 */
public interface BootstrapServer {
    String getName();

    void setName(String name);

    String getUrl();

    void setUrl(String url);
}
