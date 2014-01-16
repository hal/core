package org.jboss.as.console.client.plugins;

import java.util.Set;

/**
 * @author Heiko Braun
 * @date 7/2/13
 */
public interface AccessControlRegistry {

    public Set<String> getTokens();
    public Set<String> getResources(String token);
    public Set<String> getOperations(String token);
    public boolean isRecursive(String token);
}
