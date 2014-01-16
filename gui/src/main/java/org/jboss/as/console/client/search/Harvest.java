package org.jboss.as.console.client.search;

import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.plugins.AccessControlRegistry;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.model.mapping.AddressMapping;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.useware.kernel.gui.behaviour.FilteringStatementContext;

import javax.inject.Inject;

import java.util.Set;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

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
    private final FilteringStatementContext filteringStatementContext;

    public interface Handler {
        void onStart();
        void onHarvest(String token, String address);
        void onFinish();
        void onError(Throwable t);
    }

    @Inject
    public Harvest(
            AccessControlRegistry accessControlMetaData,
            DispatchAsync dispatcher,
            CoreGUIContext statementContext, final BootstrapContext bootstrap) {

        this.accessControlMetaData = accessControlMetaData;
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
        this.bootstrap = bootstrap;

        this.filteringStatementContext = new FilteringStatementContext(
                statementContext,
                new FilteringStatementContext.Filter() {
                    @Override
                    public String filter(String key) {

                        if ("selected.entity".equals(key)) {
                            return "*";
                        } else if ("addressable.group".equals(key)) {
                            return bootstrap.getAddressableGroups().isEmpty() ? "*" : bootstrap.getAddressableGroups().iterator().next();
                        } else if ("addressable.host".equals(key)) {
                            return bootstrap.getAddressableHosts().isEmpty() ? "*" : bootstrap.getAddressableHosts().iterator().next();
                        } else {
                            return null;
                        }
                    }

                    @Override
                    public String[] filterTuple(String key) {
                        return null;
                    }
                }
        );
    }

    public void run(Handler handler) {
        handler.onStart();
        for(String token : accessControlMetaData.getTokens())
        {

            Set<String> resources = accessControlMetaData.getResources(token);
            for(String resource : resources)
            {
                // TODO
                if(resource.startsWith("opt:")) continue;

                ModelNode op = AddressMapping.fromString(resource).asResource(filteringStatementContext);

                System.out.println(op);
                handler.onHarvest(token, resource);
            }
        }
        handler.onFinish();
    }
}
