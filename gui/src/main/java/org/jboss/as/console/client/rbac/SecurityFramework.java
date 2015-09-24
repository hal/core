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
     * Get the security context associated with a token.
     * Usually that the current {@link com.gwtplatform.mvp.shared.proxy.PlaceRequest}
     *
     * @see com.gwtplatform.mvp.client.proxy.PlaceManager
     * @return the current security context
     */
    SecurityContext getSecurityContext(String id);

    /**
     * Assign a context for a token
     * @param id
     * @param context
     */
    void assignContext(String id, SecurityContext context);

    /**
     * Check whether or not a context exists.
     *
     * @param id
     * @return
     */
    boolean hasContext(String id);

    /**
     * Force the registered {@link org.jboss.ballroom.client.rbac.SecurityContextAware} widgets
     * to evaluate the security constraints.
     *
     * Note: This does not recompute the security context, it merely instructs the widgets re-apply the security context.
     * If you need to recompute the context trigger a {@link org.jboss.ballroom.client.rbac.SecurityContextChangedEvent} instead.
     *
     * @param id - the place token
     */
    void forceUpdate(String id);
}
