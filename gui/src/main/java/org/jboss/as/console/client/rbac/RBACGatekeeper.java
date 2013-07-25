package org.jboss.as.console.client.rbac;

import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.proxy.Gatekeeper;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import org.jboss.as.console.client.plugins.AccessControlRegistry;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.rbac.SecurityService;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Heiko Braun
 */
@Singleton
public class RBACGatekeeper implements Gatekeeper {

    private final AccessControlRegistry accessControlReg;
    private final PlaceManager placemanager;
    private final SecurityService securityService;

    @Inject
    public RBACGatekeeper(
            final AccessControlRegistry accessControlReg,
            final PlaceManager placemanager,
            final SecurityService securityService, EventBus eventBus) {

        this.accessControlReg = accessControlReg;
        this.placemanager = placemanager;
        this.securityService = securityService;

    }

    @Override
    public boolean canReveal() {
        String token = placemanager.getCurrentPlaceRequest().getNameToken();

        // important: without this the UnauthorisedPresenter cannot successfully navigate back
        placemanager.updateHistory(placemanager.getCurrentPlaceRequest(), true);

        boolean outcome = false;

        if(securityService.hasContext(token))
        {
            SecurityContext securityContext = securityService.getSecurityContext(token);
            outcome = securityContext.isReadable();
        }

        return outcome;
    }
}