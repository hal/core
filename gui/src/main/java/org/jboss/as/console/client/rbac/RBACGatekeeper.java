package org.jboss.as.console.client.rbac;

import com.allen_sauer.gwt.log.client.Log;
import com.gwtplatform.mvp.client.proxy.Gatekeeper;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import org.jboss.as.console.client.plugins.RequiredResourcesRegistry;
import org.jboss.ballroom.client.rbac.AuthorisationDecision;
import org.jboss.ballroom.client.rbac.SecurityContext;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Heiko Braun
 */
@Singleton
public class RBACGatekeeper implements Gatekeeper {

    private final RequiredResourcesRegistry accessControlMetaData;
    private final PlaceManager placemanager;
    private final SecurityFramework securityFramework;

    @Inject
    public RBACGatekeeper(
            final RequiredResourcesRegistry accessControlMetaData,
            final PlaceManager placemanager,
            final SecurityFramework securityManager) {
        this.accessControlMetaData = accessControlMetaData;
        this.placemanager = placemanager;
        this.securityFramework = securityManager;

    }

    @Override
    public boolean canReveal() {
        boolean outcome = false;
        String token = placemanager.getCurrentPlaceRequest().getNameToken();

        if (securityFramework.hasContext(token)) {
            try {
                SecurityContext securityContext = securityFramework.getSecurityContext(token);
                final AuthorisationDecision readPrivilege = securityContext.getReadPriviledge();

                // bootstrap operations
                boolean bootstrapRequirementsSatisfied = true;
                for (String op : accessControlMetaData.getOperations(token)) {
                    int idx = op.indexOf("#");
                    AuthorisationDecision opPrivilege = securityContext.getOperationPriviledge(
                            op.substring(0, idx),
                            op.substring(idx + 1, op.length())
                    );

                    if (!opPrivilege.isGranted()) {
                        bootstrapRequirementsSatisfied = false;
                        break;
                    }
                }
                outcome = readPrivilege.isGranted() && bootstrapRequirementsSatisfied;
            } catch (Throwable e) {
                // placemanager might be locked
                placemanager.unlock();
                Log.error("Failed to check security context in RBACGatekeeper.canReveal() for " + token + ": " + e.getMessage());
            }
        }
        return outcome;
    }
}
