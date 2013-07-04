package org.jboss.as.console.client.rbac;

import java.util.Set;

/**
 * @author Heiko Braun
 * @date 7/3/13
 */
public class SecurityContext {

    private String nameToken;
    private Set<String> requiredResources;
    private boolean grantPlaceAccess;

    public SecurityContext(String nameToken, Set<String> requiredResources) {
        this.nameToken = nameToken;
        this.requiredResources = requiredResources;
    }

    public boolean doesGrantPlaceAccess() {
        return grantPlaceAccess;
    }

    void setGrantPlaceAccess (boolean b) {
        this.grantPlaceAccess= b;
    }
}
