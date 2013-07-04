package org.jboss.as.console.client.rbac;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * @author Heiko Braun
 * @date 7/3/13
 */
public interface SecurityService {

    /**
     * Get the security context associated with the current {@link com.gwtplatform.mvp.client.proxy.PlaceRequest}
     * @see com.gwtplatform.mvp.client.proxy.PlaceManager
     * @return the current security context
     */
    SecurityContext getSecurityContext(String nameToken);


    /**
     * Create a security context for a particular place.
     * @param nameToken
     * @param callback
     */
    void createSecurityContext(String nameToken, AsyncCallback<SecurityContext> callback);

    /**
     * Check wether or not a context exists.
     *
     * @param nameToken
     * @return
     */
    boolean hasContext(String nameToken);

    /**
     * Removes a context and forces re-creation
     * @param nameToken
     */
    void flushContext(String nameToken);
}
