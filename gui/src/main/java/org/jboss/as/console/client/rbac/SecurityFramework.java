package org.jboss.as.console.client.rbac;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.rbac.SecurityService;

import java.util.Set;

/**
 * API for core platform components to leverage security facilities.
 *
 * @author Heiko Braun
 * @date 8/12/13
 */
public interface SecurityFramework extends SecurityService {

    /**
     * Get the security context associated with the current {@link com.gwtplatform.mvp.client.proxy.PlaceRequest}
     * @see com.gwtplatform.mvp.client.proxy.PlaceManager
     * @return the current security context
     */
    SecurityContext getSecurityContext(String id);

    /**
     * Create a security context for a particular place.
     * Retrieves access control meta data from {@link org.jboss.as.console.spi.AccessControl} annotation.
     *
     * @param id
     * @param callback
     */
    void createSecurityContext(String id, AsyncCallback<SecurityContext> callback);

    /**
     * Create a security context for a particular place.
     *
     * @param id
     * @param requiredResources a list of resources to operate on
     * @param callback
     */
    void createSecurityContext(final String id, final Set<String> requiredResources, final AsyncCallback<SecurityContext> callback);

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
