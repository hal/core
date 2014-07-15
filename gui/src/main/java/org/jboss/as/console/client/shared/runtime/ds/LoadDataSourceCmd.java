package org.jboss.as.console.client.shared.runtime.ds;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.domain.model.LoggingCallback;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.AsyncCommand;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 12/19/11
 */
public class LoadDataSourceCmd implements AsyncCommand<List<DataSource>>{

    private DispatchAsync dispatcher;
    private ApplicationMetaData  metaData;
    private EntityAdapter<DataSource> adapter;

    public LoadDataSourceCmd(DispatchAsync dispatcher, ApplicationMetaData metaData) {
        this.dispatcher = dispatcher;
        this.metaData = metaData;
        this.adapter = new EntityAdapter<DataSource>(DataSource.class, metaData);
    }


    @Override
    public void execute(AsyncCallback<List<DataSource>> listAsyncCallback) {
        throw new RuntimeException("Use the overriden method instead");
    }

    public void execute(final AsyncCallback<List<DataSource>> callback, boolean isXA) {
        final ModelNode address = RuntimeBaseAddress.get();
        address.add("subsystem", "datasources");

        String subresource = isXA ? "xa-data-source": "data-source";

        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(ADDRESS).set(address);
        operation.get(CHILD_TYPE).set(subresource);

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {

                ModelNode response  = result.get();

                if(response.isFailure())
                {
                    callback.onFailure(new RuntimeException("Failed to read DS metrics: "+ response.getFailureDescription()));
                }
                else
                {
                    List<DataSource> datasources = adapter.fromDMRList(response.get(RESULT).asList());
                    callback.onSuccess(datasources);
                }
            }
        });
    }
}
