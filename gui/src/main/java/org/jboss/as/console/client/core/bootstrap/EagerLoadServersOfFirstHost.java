package org.jboss.as.console.client.core.bootstrap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.shared.state.DomainEntityManager;
import org.jboss.as.console.client.shared.state.HostList;
import org.jboss.as.console.client.shared.state.ServerInstanceList;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

/**
 * The main function of this bootstrap step is to provide a server preselection.
 * Depends on {@link org.jboss.as.console.client.core.bootstrap.EagerLoadHosts}
 */
public class EagerLoadServersOfFirstHost implements Function<BootstrapContext> {

    private final DomainEntityManager domainManager;

    public EagerLoadServersOfFirstHost(DomainEntityManager domainManager) {
        this.domainManager = domainManager;
    }

    @Override
    public void execute(final Control<BootstrapContext> control) {
        final BootstrapContext context = control.getContext();

        if (!context.isStandalone()) {
            HostList initialHosts = context.getInitialHosts();
            if (!initialHosts.isEmpty()) {
                domainManager
                        .getServerInstances(initialHosts.getSelectedHost().getName(), new AsyncCallback<ServerInstanceList>() {
                            @Override
                            public void onFailure(final Throwable caught) {
                                control.proceed();
                            }

                            @Override
                            public void onSuccess(final ServerInstanceList result) {
                                context.setInitialServer(result.getSelectedServer());
                                control.proceed();
                            }
                        });
            } else {
                control.proceed();
            }
        } else {
            // standalone
            control.proceed();
        }
    }

}
