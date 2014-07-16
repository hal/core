package org.jboss.as.console.client.v3.stores.domain;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.domain.model.Host;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.v3.stores.domain.actions.HostSelection;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshHosts;
import org.jboss.as.console.client.v3.stores.domain.actions.RefreshServer;
import org.jboss.gwt.circuit.ChangeSupport;
import org.jboss.gwt.circuit.Dispatcher;
import org.jboss.gwt.circuit.meta.Process;
import org.jboss.gwt.circuit.meta.Store;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Heiko Braun
 * @date 15/07/14
 */
@Store
public class HostStore extends ChangeSupport {

    private HostInformationStore hostInfo;

    private List<Host> hostModel = new ArrayList<>();
    private Host selectedHost;


    @Inject
    public HostStore(HostInformationStore hostInfo) {
        this.hostInfo = hostInfo;
    }

    public void init(final AsyncCallback<List<Host>> callback) {
        hostInfo.getHosts(new SimpleCallback<List<Host>>() {
            @Override
            public void onSuccess(List<Host> hosts) {
                HostStore.this.hostModel = hosts;

                if(hostModel.isEmpty()) throw new NoHostsAvailable();

                selectedHost = hosts.get(0);
                callback.onSuccess(hosts);
            }
        });
    }


    @Process(actionType = RefreshServer.class)
    public void onRefreshServer(final Dispatcher.Channel channel) {

        // a) provide at least a selected host
        if(null==selectedHost)
            throw new IllegalStateException("no host selected!");

        // b) (optional) refresh hosts before the sever store loads the servers

        channel.ack();
    }

    @Process(actionType = RefreshHosts.class)
    public void onRefreshHosts(final Dispatcher.Channel channel) {

        hostInfo.getHosts(new SimpleCallback<List<Host>>() {
            @Override
            public void onSuccess(List<Host> hosts) {
                HostStore.this.hostModel = hosts;
                channel.ack();
                fireChanged(HostStore.class);
            }

            @Override
            public void onFailure(Throwable caught) {
                channel.nack(caught);
            }
        });
    }

    @Process(actionType = HostSelection.class)
    public void onSelectHost(String hostName, final Dispatcher.Channel channel) {

        for(Host h : hostModel) {
            if(h.getName().equals(hostName))
            {
                selectedHost = h;
                break;
            }
        }

        channel.ack();
        fireChanged(HostStore.class);
    }


    // data access


    public List<Host> getHostModel() {
        return hostModel;
    }

    public String getSelectedHost() {
        return selectedHost.getName();
    }

    public Host getSelectedHostInstance() {
        return selectedHost;
    }

    public boolean hasSelecteHost() {
        return selectedHost!=null;
    }

    public class NoHostsAvailable extends RuntimeException {

    }
}
