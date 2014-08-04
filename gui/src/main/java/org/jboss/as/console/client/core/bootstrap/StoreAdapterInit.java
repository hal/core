package org.jboss.as.console.client.core.bootstrap;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

/**
 * The only purpose of this class is to invoke the ctor of the adapter classes.
 *
 * @author Heiko Braun
 * @date 15/07/14
 */
public class StoreAdapterInit implements Function<BootstrapContext> {

    public StoreAdapterInit() {
        Console.MODULES.getLogStoreAdapter();
        Console.MODULES.getHostStoreAdapter();
        Console.MODULES.getServerStoreAdapter();
    }

    @Override
    public void execute(Control<BootstrapContext> control) {
        control.proceed();
    }
}
