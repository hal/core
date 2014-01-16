package org.jboss.as.console.client.search;

import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.plugins.AccessControlRegistry;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.dmr.client.dispatch.DispatchAsync;

import javax.inject.Inject;

/**
 * Creates search indexes.
 *
 * @author Heiko Braun
 * @date 16/01/14
 */
public class Harvest {

    final AccessControlRegistry accessControlMetaData;
    final DispatchAsync dispatcher;
    final CoreGUIContext statementContext;
    private final BootstrapContext bootstrap;


    @Inject
    public Harvest(
            AccessControlRegistry accessControlMetaData,
            DispatchAsync dispatcher,
            CoreGUIContext statementContext, final BootstrapContext bootstrap) {

        this.accessControlMetaData = accessControlMetaData;
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
        this.bootstrap = bootstrap;
    }
}
