package org.jboss.as.console.client.core.bootstrap.hal;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.ServerGroupStore;
import org.jboss.gwt.flow.client.Control;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class EagerLoadGroups implements BootstrapStep {

    private final ServerGroupStore groupStore;

    @Inject
    public EagerLoadGroups(ServerGroupStore groupStore) {
        this.groupStore = groupStore;
    }

    @Override
    public void execute(final Control<BootstrapContext> control) {
        final BootstrapContext context = control.getContext();

        if (!context.isStandalone()) {
            groupStore.loadServerGroups(new AsyncCallback<List<ServerGroupRecord>>() {
                @Override
                public void onFailure(Throwable caught) {
                    context.setlastError(caught);
                    control.abort();
                }

                @Override
                public void onSuccess(List<ServerGroupRecord> result) {
                        Set<String> groups = new TreeSet<String>();
                        for (ServerGroupRecord group : result) { groups.add(group.getName()); }
                        context.setAdressableGroups(groups);
                        control.proceed();
                }
            });
        } else {
            // standalone
            control.proceed();
        }
    }
}
