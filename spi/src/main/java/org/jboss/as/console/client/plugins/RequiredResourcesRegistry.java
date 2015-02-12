package org.jboss.as.console.client.plugins;

import java.util.Set;

/**
 * @author Heiko Braun
 * @date 7/2/13
 */
public interface RequiredResourcesRegistry {

    Set<String> getTokens();
    Set<String> getResources(String token);
    Set<String> getOperations(String token);
    boolean isRecursive(String token);
}
