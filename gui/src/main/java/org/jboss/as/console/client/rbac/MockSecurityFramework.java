package org.jboss.as.console.client.rbac;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.plugins.AccessControlRegistry;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.ballroom.client.rbac.AuthorisationDecision;
import org.jboss.ballroom.client.rbac.Facet;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.dmr.client.dispatch.DispatchAsync;

import java.util.Set;

/**
 * @author Heiko Braun
 * @date 8/22/13
 */
public class MockSecurityFramework extends SecurityFrameworkImpl {

    @Inject
    public MockSecurityFramework(
            AccessControlRegistry accessControlReg,
            DispatchAsync dispatcher,
            CoreGUIContext statementContext) {

        super(accessControlReg, dispatcher, statementContext);
    }

    final static SecurityContext NoopContext = new SecurityContext() {
        @Override
        public Facet getFacet() {
            return Facet.CONFIGURATION;
        }

        @Override
        public AuthorisationDecision getReadPriviledge() {
            return new AuthorisationDecision(true);
        }

        @Override
        public AuthorisationDecision getWritePriviledge() {
            return new AuthorisationDecision(true);
        }

        @Override
        public AuthorisationDecision getAttributeWritePriviledge(String name) {
            return new AuthorisationDecision(true);
        }

        @Override
        public void seal() {

        }

        @Override
        public AuthorisationDecision getOperationPriviledge(String resourceAddress, String operationName) {
            return new AuthorisationDecision(true);
        }
    };

    @Override
    public void createSecurityContext(String id, AsyncCallback<SecurityContext> callback) {
        Console.warning("Using MockSecurityFramework");
        contextMapping.put(id, NoopContext);
        callback.onSuccess(NoopContext);
    }

    @Override
    public void createSecurityContext(String id, Set<String> requiredResources, AsyncCallback<SecurityContext> callback) {
        Console.warning("Using MockSecurityFramework");
        contextMapping.put(id, NoopContext);
        callback.onSuccess(NoopContext);
    }
}
