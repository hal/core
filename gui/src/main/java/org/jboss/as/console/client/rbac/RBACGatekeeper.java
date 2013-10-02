package org.jboss.as.console.client.rbac;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gwtplatform.mvp.client.proxy.Gatekeeper;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import org.jboss.as.console.client.plugins.AccessControlRegistry;
import org.jboss.ballroom.client.rbac.AuthorisationDecision;
import org.jboss.ballroom.client.rbac.SecurityContext;

/**
 * @author Heiko Braun
 */
@Singleton
public class RBACGatekeeper implements Gatekeeper {

    private final AccessControlRegistry accessControlMetaData;
    private final PlaceManager placemanager;
    private final SecurityFramework securityFramework;

    @Inject
    public RBACGatekeeper(
            final AccessControlRegistry accessControlMetaData,
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
        }
        return outcome;
    }
}