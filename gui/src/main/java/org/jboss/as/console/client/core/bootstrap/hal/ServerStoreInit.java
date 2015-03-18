package org.jboss.as.console.client.core.bootstrap.hal;

import com.google.inject.Inject;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.v3.stores.domain.ServerStore;
import org.jboss.gwt.flow.client.Control;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 15/07/14
 */
public class ServerStoreInit implements BootstrapStep {

    private final ServerStore serverStore;
    private final HostStore hostStore;

    @Inject
    public ServerStoreInit(HostStore hostStore, ServerStore serverStore) {
        this.hostStore = hostStore;
        this.serverStore = serverStore;
    }

    @Override
    public void execute(final Control<BootstrapContext> control) {

        final BootstrapContext context = control.getContext();
        if (!context.isStandalone()) {
            serverStore.init(hostStore.getSelectedHost(), new SimpleCallback<List<Server>>() {

                @Override
                public void onFailure(Throwable caught) {
                    control.abort();
                }

                @Override
                public void onSuccess(List<Server> hosts) {
                    control.proceed();
                }
            });
        } else {
            // standalone
            control.proceed();
        }
    }
}

