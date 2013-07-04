package org.jboss.as.console.client.plugins;

import java.util.Set;

/**
 * @author Heiko Braun
 * @date 7/2/13
 */
public interface AccessControlRegistry {

    public Set<String> getResources(String token);
}
