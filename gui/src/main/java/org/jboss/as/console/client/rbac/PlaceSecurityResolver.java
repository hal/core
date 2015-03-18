package org.jboss.as.console.client.rbac;

import org.jboss.as.console.client.Console;

/**
 * Resolves security context from place requests
 *
 * @author Heiko Braun
 * @date 8/12/13
 */
public class PlaceSecurityResolver implements ContextKeyResolver {

    @Override
    public String resolveKey() {
        return Console.MODULES.getPlaceManager().getCurrentPlaceRequest().getNameToken();
    }
}
