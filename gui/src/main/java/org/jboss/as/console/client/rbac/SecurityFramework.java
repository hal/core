package org.jboss.as.console.client.rbac;

import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.rbac.SecurityService;

/**
 * API for core platform components to leverage security facilities.
 *
 * @author Heiko Braun
 * @date 8/12/13
 */
public interface SecurityFramework extends SecurityService {

    void assignContext(String id, SecurityContext context);

    /**
     * Get the security context associated with the current {@link com.gwtplatform.mvp.shared.proxy.PlaceRequest}
     * @see com.gwtplatform.mvp.client.proxy.PlaceManager
     * @return the current security context
     */
    SecurityContext getSecurityContext(String id);

    /**
     * Check wether or not a context exists.
     *
     * @param id
     * @return
     */
    boolean hasContext(String id);

    /**
     * Removes a context and forces re-creation
     * @param id
     */
    void flushContext(String id);

}
