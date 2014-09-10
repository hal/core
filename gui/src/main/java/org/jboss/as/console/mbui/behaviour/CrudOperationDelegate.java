package org.jboss.as.console.mbui.behaviour;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * Default CRUD operations that complement the {@link org.jboss.as.console.mbui.behaviour.DefaultPresenterContract}.
 * Use the {@link org.jboss.as.console.mbui.behaviour.CrudOperationDelegate.Callback} for operations that need to happen after the
 * delegate has performed work (i.e. refreshing the views)
 *
 * TODO Change the signatures to use ResourceAddress
 * TODO Use a common ResourceAddress as constructor parameter (the statement context is already shared)?
 *
 * @author Heiko Braun
 * @since 08/09/14
 */
public class CrudOperationDelegate {

    public interface Callback {
        void onSuccess(ResourceAddress address, String name);
        void onFailure(ResourceAddress address, String name, Throwable t);
    }


    private final StatementContext statementContext;
    private final DispatchAsync dispatcher;

    public CrudOperationDelegate(StatementContext statementContext, DispatchAsync dispatcher) {
        this.statementContext = statementContext;
        this.dispatcher = dispatcher;
    }

    public void onCreateResource(final String addressString, final ModelNode payload, final Callback... callback) {

        final String name = payload.get(NAME).asString();
        final ResourceAddress address = new ResourceAddress(addressString, statementContext);
        ModelNode op = address.asOperation(payload);
        op.get(OP).set(ADD);

        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                for (Callback cb : callback) {
                    cb.onFailure(address, name, caught);
                }
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error("Failed to add resource " + name, response.getFailureDescription());
                    for (Callback cb : callback) {
                        cb.onFailure(address, name, new RuntimeException("Failed to add resource " + name +":"+ response.getFailureDescription()));
                    }
                } else {
                    Console.info("Added resource " + name);
                    for (Callback cb : callback) {
                        cb.onSuccess(address, name);
                    }
                }
            }
        });
    }

    public void onRemoveResource(final String addressString, final String name, final Callback... callback) {

        final ResourceAddress address = new ResourceAddress(addressString, statementContext);
        ModelNode op = address.asFqAddress(name);
        op.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                for (Callback cb : callback) {
                    cb.onFailure(address, name, caught);
                }
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error("Failed to remove resource " + name, response.getFailureDescription());
                    for (Callback cb : callback) {
                        cb.onFailure(address, name, new RuntimeException("Failed to add resource " + name + ":" + response.getFailureDescription()));
                    }
                } else {
                    Console.info("Removed resource " + name);
                    for (Callback cb : callback) {
                        cb.onSuccess(address, name);
                    }
                }
            }
        });
    }

    public void onSaveResource(final String addressString, final String name, Map<String, Object> changedValues,
                               final Callback... callback) {

        final ResourceAddress address = new ResourceAddress(addressString, statementContext);
        final ModelNodeAdapter adapter = new ModelNodeAdapter();

        // name can be omitted
        ModelNode operation = name!=null ?
                adapter.fromChangeset(changedValues, address.asFqAddress(name)) :
                adapter.fromChangeset(changedValues, address);

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                for (Callback cb : callback) {
                    cb.onFailure(address, name, caught);
                }
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    Console.error("Failed to save " + address.toString(), response.getFailureDescription());
                    for (Callback cb : callback) {
                        cb.onFailure(address, name, new RuntimeException("Failed to add resource " + name +":"+ response.getFailureDescription()));
                    }
                }
                else {
                    Console.info("Successfully saved " + address.toString());
                    for (Callback cb : callback) {
                        cb.onSuccess(address, name);
                    }
                }
            }
        });
    }
}
