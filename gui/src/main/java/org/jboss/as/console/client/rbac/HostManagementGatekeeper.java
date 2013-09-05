package org.jboss.as.console.client.rbac;

import com.gwtplatform.mvp.client.proxy.Gatekeeper;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;

/**
 * @author Heiko Braun
 * @date 9/5/13
 */
public class HostManagementGatekeeper implements Gatekeeper {
    @Override
    public boolean canReveal() {
        BootstrapContext bootstrapContext = Console.MODULES.getBootstrapContext();
        return !bootstrapContext.isHostManagementDisabled()
                && !bootstrapContext.isGroupManagementDisabled();
    }
}
