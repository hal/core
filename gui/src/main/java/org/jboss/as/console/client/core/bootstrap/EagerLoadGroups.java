package org.jboss.as.console.client.core.bootstrap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.ServerGroupStore;
import org.jboss.gwt.flow.client.Control;
import org.jboss.gwt.flow.client.Function;

import java.util.List;

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
