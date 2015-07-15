package org.jboss.as.console.client.shared.subsys.activemq;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.AsyncCommand;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.Collections;
import java.util.List;

import static org.jboss.as.console.client.shared.subsys.activemq.ActivemqFinder.ROOT_TEMPLATE;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * Loads a list of activemq server instance names.
 */
public class LoadActivemqServersCmd implements AsyncCommand<List<Property>> {

    private final DispatchAsync dispatcher;
    private final StatementContext statementContext;

    public LoadActivemqServersCmd(DispatchAsync dispatcher, StatementContext statementContext) {
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
    }

    @Override
    public void execute(final AsyncCallback<List<Property>> callback) {
        Operation operation = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION,
                ROOT_TEMPLATE.resolve(statementContext)).param(CHILD_TYPE, "server").build();
        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Log.error("Failed to load activemq server", response.getFailureDescription());
                    callback.onSuccess(Collections.emptyList());
                } else {
                    callback.onSuccess(response.get(RESULT).asPropertyList());
                }
            }
        });
    }
}
