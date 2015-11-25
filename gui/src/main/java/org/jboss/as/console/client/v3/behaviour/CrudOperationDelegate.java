package org.jboss.as.console.client.v3.behaviour;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * Default CRUD operations. Use the {@link CrudOperationDelegate.Callback} for operations that need to happen after the
 * delegate has performed work (i.e. refreshing the views)
 *
 * @author Harald Pehl
 */
public class CrudOperationDelegate {

    public interface Callback {
        void onSuccess(AddressTemplate addressTemplate, String name);
        void onFailure(AddressTemplate addressTemplate, String name, Throwable t);
    }


    private final StatementContext statementContext;
    private final DispatchAsync dispatcher;

    public CrudOperationDelegate(StatementContext statementContext, DispatchAsync dispatcher) {
        this.statementContext = statementContext;
        this.dispatcher = dispatcher;
    }

    /**
     * Creates a new resource using the given address, name and payload.
     * @param addressTemplate The address template for the new resource. Might end with a wildcard,
     *                        in that case the name is mandatory.
     * @param name the name of the new resource. Might be null if the address template already contains the name.
     * @param payload the payload
     * @param callback the callbacks
     */
    public void onCreateResource(final AddressTemplate addressTemplate, final String name, final ModelNode payload,
                                 final Callback... callback) {
        ModelNode op = payload.clone();
        op.get(ADDRESS).set(addressTemplate.resolve(statementContext, name));
        op.get(OP).set(ADD);

        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                for (Callback cb : callback) {
                    cb.onFailure(addressTemplate, name, caught);
                }
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    for (Callback cb : callback) {
                        cb.onFailure(addressTemplate, name,
                                new RuntimeException("Failed to add resource " +
                                        addressTemplate.replaceWildcards(name) + ":" + response.getFailureDescription()));
                    }
                } else {
                    for (Callback cb : callback) {
                        cb.onSuccess(addressTemplate, name);
                    }
                }
            }
        });
    }

    /**
     * Updates an existing resource using the given address, name and changed values.
     * @param addressTemplate The address template for the resource. Might end with a wildcard,
     *                        in that case the name is mandatory.
     * @param name the name of the new resource. Might be null if the address template already contains the name.
     * @param changedValues the changed values
     * @param callback the callbacks
     */
    public void onSaveResource(final AddressTemplate addressTemplate, final String name,
                               Map<String, Object> changedValues, final Callback... callback) {

        final ResourceAddress address = addressTemplate.resolve(statementContext, name);
        final ModelNodeAdapter adapter = new ModelNodeAdapter();
        ModelNode op = adapter.fromChangeSet(address, changedValues);

        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                for (Callback cb : callback) {
                    cb.onFailure(addressTemplate, name, caught);
                }
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.saveFailed(name), response.getFailureDescription());
                    for (Callback cb : callback) {
                        cb.onFailure(addressTemplate, name,
                                new RuntimeException(Console.MESSAGES.saveFailed(
                                        addressTemplate.replaceWildcards(name).toString()) + ":" + response.getFailureDescription()));
                    }
                } else {
                    for (Callback cb : callback) {
                        cb.onSuccess(addressTemplate, name);
                    }
                }
            }
        });
    }

    public void onSaveComplexAttribute(final AddressTemplate addressTemplate, final String name, String attributeName,
                                       ModelNode payload, final Callback... callback) {

        final ResourceAddress address = addressTemplate.resolve(statementContext, name);
        final ModelNodeAdapter adapter = new ModelNodeAdapter();
        ModelNode op = adapter.fromComplexAttribute(address, attributeName, payload);

        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                for (Callback cb : callback) {
                    cb.onFailure(addressTemplate, name, caught);
                }
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    Console.error(Console.MESSAGES.saveFailed(name), response.getFailureDescription());
                    for (Callback cb : callback) {
                        cb.onFailure(addressTemplate, name,
                                new RuntimeException(Console.MESSAGES.saveFailed(
                                        addressTemplate.replaceWildcards(name).toString()) + ":" + response.getFailureDescription()));
                    }
                } else {
                    for (Callback cb : callback) {
                        cb.onSuccess(addressTemplate, name);
                    }
                }
            }
        });
    }

    /**
     * Removes an existing resource specified by the address and name.
     * @param addressTemplate The address template for the resource. Might end with a wildcard,
     *                        in that case the name is mandatory.
     * @param name the name of the new resource. Might be null if the address template already contains the name.
     * @param callback the callbacks
     */
    public void onRemoveResource(final AddressTemplate addressTemplate, final String name, final Callback... callback) {

        ModelNode op = new ModelNode();
        op.get(ADDRESS).set(addressTemplate.resolve(statementContext, name));
        op.get(OP).set(REMOVE);

        dispatcher.execute(new DMRAction(op), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                for (Callback cb : callback) {
                    cb.onFailure(addressTemplate, name, caught);
                }
            }

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    for (Callback cb : callback) {
                        cb.onFailure(addressTemplate, name,
                                new RuntimeException("Failed to remove resource " +
                                        addressTemplate.replaceWildcards(name) + ":" + response.getFailureDescription()));
                    }
                } else {
                    for (Callback cb : callback) {
                        cb.onSuccess(addressTemplate, name);
                    }
                }
            }
        });
    }
}
