package org.jboss.as.console.client.domain.runtime;

import com.gwtplatform.mvp.client.proxy.Gatekeeper;
import org.jboss.as.console.client.rbac.HostManagementGatekeeper;
import org.jboss.as.console.client.v3.stores.domain.HostStore;

import javax.inject.Inject;

/**
 * @author Heiko Braun
 * @since 16/07/14
 */
public class DomainRuntimegateKeeper implements Gatekeeper {

    private final HostManagementGatekeeper hostKeeper;
    private final HostStore hostStore;

    @Inject
    public DomainRuntimegateKeeper(HostStore hostStore, HostManagementGatekeeper hostKeeper) {
        this.hostStore = hostStore;
        this.hostKeeper = hostKeeper;
    }

    @Override
    public boolean canReveal() {
        boolean parentAllows = hostKeeper.canReveal();
        return parentAllows && hostStore.hasSelectedServer();
    }
}
