package org.jboss.as.console.client.rbac;

import org.jboss.ballroom.client.rbac.AuthorisationDecision;
import org.jboss.ballroom.client.rbac.Facet;
import org.jboss.ballroom.client.rbac.SecurityContext;

/**
 * Acts as a fallback when the context creation fails due to an error.
 *
 * @author Heiko Braun
 * @date 9/5/13
 */
public class ReadOnlyContext implements SecurityContext {

    private static final AuthorisationDecision GRANTED = new AuthorisationDecision(true);
    private static final AuthorisationDecision DENIED = new AuthorisationDecision(false);

    @Override
    public Facet getFacet() {
        return Facet.RUNTIME;
    }

    @Override
    public AuthorisationDecision getReadPriviledge() {
        return GRANTED;
    }

    @Override
    public AuthorisationDecision getWritePriviledge() {
        return DENIED;
    }

    @Override
    public AuthorisationDecision getAttributeWritePriviledge(String name) {
        return DENIED;
    }

    @Override
    public void seal() {

    }

    @Override
    public AuthorisationDecision getOperationPriviledge(String resourceAddress, String operationName) {
        return DENIED;
    }
}
