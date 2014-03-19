package org.jboss.as.console.client.core.bootstrap;

import java.util.Set;
import java.util.TreeSet;

import com.allen_sauer.gwt.log.client.Log;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.domain.model.Host;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.state.DomainEntityManager;
import org.jboss.as.console.client.shared.state.HostList;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

/**
 * The main function of this bootstrap step is to provide a host preselection.
 *
 */
public class EagerLoadHosts implements Function<BootstrapContext> {

    private final DomainEntityManager domainManager;

    public EagerLoadHosts(DomainEntityManager domainManager) {
        this.domainManager = domainManager;
    }

    @Override
    public void execute(final Control<BootstrapContext> control) {
        final BootstrapContext context = control.getContext();

        if(!context.isStandalone())
        {
            domainManager.getHosts(new SimpleCallback<HostList>() {

                @Override
                public void onFailure(Throwable caught) {
                    if(caught instanceof DomainEntityManager.NoHostsAvailable)
                    {
                        // this is expected (host scoped roles)
                        context.setHostManagementDisabled(true);
                        control.proceed();
                    }
                    else
                    {
                        context.setlastError(caught);
                        control.abort();
                    }
                }

                @Override
                public void onSuccess(HostList hostList) {
                    Log.info("Identified " + hostList.getHosts().size() + " hosts in this domain");
                    context.setInitialHosts(hostList);
                    if(hostList.isEmpty()) {
                        context.setHostManagementDisabled(true);
                    }

                    Set<String> hosts = new TreeSet<String>();
                    for(Host host : hostList.getHosts()) hosts.add(host.getName());
                    control.getContext().setAddressableHosts(hosts);
                    control.proceed();

                }
            });
        }
        else
        {
            // standalone
            control.proceed();
        }
    }

}
