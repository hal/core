package org.jboss.as.console.client.core.bootstrap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.ServerGroupStore;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EagerLoadGroups implements Function<BootstrapContext> {

    private final ServerGroupStore groupStore;

    public EagerLoadGroups(ServerGroupStore groupStore) {
        this.groupStore = groupStore;
    }

    @Override
    public void execute(final Control<BootstrapContext> control) {
        final BootstrapContext context = control.getContext();

        if(!context.isStandalone())
        {
            groupStore.loadServerGroups(new AsyncCallback<List<ServerGroupRecord>>() {
                @Override
                public void onFailure(Throwable caught) {
                    context.setlastError(caught);
                    control.abort();
                }

                @Override
                public void onSuccess(List<ServerGroupRecord> result) {
                    if(result.isEmpty())
                    {
                        context.setGroupManagementDisabled(true);
                    }

                    Set<String> groups = new HashSet<String>(result.size());
                    for(ServerGroupRecord group : result) groups.add(group.getName());
                    control.getContext().setAdressableGroups(groups);
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
