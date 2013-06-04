package org.jboss.as.console.client;

/**
 * Can be removed as soon as the proxy is gone.
 * @author Heiko Braun
 * @date 9/24/12
 */
@Deprecated
public interface ProductConfig {

    String getCoreVersion(); // Can be inlined with 'org.jboss.as.console.client.Build.VERSION'
    String getDevHost(); // Is only used for proxy URL setup
}
