package org.jboss.as.console.client.shared.runtime;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.v3.stores.domain.ServerStore;
import org.jboss.dmr.client.ModelNode;

import javax.inject.Inject;

/**
 * @author Heiko Braun
 * @date 12/9/11
 */
public class RuntimeBaseAddress {

    @Inject private static RuntimeBaseAddress instance;

    @Inject private static BootstrapContext bootstrap;

    public static ModelNode get() {
        return instance.getAddress();
    }

    public ModelNode getAddress() {

        HostStore hostStore = Console.MODULES.getHostStore();
        ServerStore serverStore = Console.MODULES.getServerStore();

        ModelNode baseAddress = new ModelNode();
        baseAddress.setEmptyList();

        if(!bootstrap.isStandalone())
        {
            baseAddress.add("host", hostStore.getSelectedHost());
            baseAddress.add("server", hostStore.getSelectedServer());
        }

        return baseAddress;
    }
}
