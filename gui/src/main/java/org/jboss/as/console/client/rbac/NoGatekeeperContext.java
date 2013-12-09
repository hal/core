package org.jboss.as.console.client.rbac;

import org.jboss.ballroom.client.rbac.AuthorisationDecision;
import org.jboss.ballroom.client.rbac.SecurityContext;

/**
 * Used with presenter that rely on @NoGatekeeper
 *
 * @author Heiko Braun
 * @date 9/5/13
 */
public class NoGatekeeperContext implements SecurityContext {

    private static final AuthorisationDecision GRANTED = new AuthorisationDecision(true);

    @Override
    public AuthorisationDecision getReadPriviledge() {
        return GRANTED;
    }

    @Override
    public AuthorisationDecision getWritePriviledge() {
        return GRANTED;
    }

    @Override
    public AuthorisationDecision getAttributeWritePriviledge(String name) {
        return GRANTED;
    }

    @Override
    public AuthorisationDecision getAttributeWritePriviledge(String resourceAddress, String attributeName) {
        return GRANTED;
    }

    @Override
    public void seal() {

    }

    @Override
    public AuthorisationDecision getOperationPriviledge(String resourceAddress, String operationName) {
        return GRANTED;
    }

    @Override
    public AuthorisationDecision getReadPrivilege(String resourceAddress) {
        return GRANTED;
    }

    @Override
    public AuthorisationDecision getWritePrivilege(String resourceAddress) {
        return GRANTED;
    }

    @Override
    public boolean hasChildContext(final String resourceAddress) {
        return false;
    }

    @Override
    public SecurityContext getChildContext(final String resourceAddress) {
        return null;
    }
}
