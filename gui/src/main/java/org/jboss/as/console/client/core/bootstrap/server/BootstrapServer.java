package org.jboss.as.console.client.core.bootstrap.server;

/**
 * Class which represents a server instance used when the console starts.
 *
 * @author Harald Pehl
 * @date 02/27/2013
 */
public interface BootstrapServer
{
    String getName();

    void setName(String name);

    String getUrl();

    void setUrl(String url);
}
