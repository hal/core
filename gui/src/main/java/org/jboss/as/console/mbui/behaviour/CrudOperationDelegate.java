package org.jboss.as.console.mbui.behaviour;

import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.widgets.AddResourceDialog;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
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
 * @author Heiko Braun
 * @since 08/09/14
 */
public class CrudOperationDelegate {

    private final StatementContext statementContext;
    private final DispatchAsync dispatcher;
    private DefaultWindow window;

    public interface Callback {
        void onSuccess(ResourceAddress address, String name);
        void onFailure(ResourceAddress address, String name, Throwable t);
    }


    public CrudOperationDelegate(StatementContext statementContext, DispatchAsync dispatcher) {
        this.statementContext = statementContext;
        this.dispatcher = dispatcher;
    }

    public void onLaunchAddResourceDialog(String token, String addressString, final Callback... callback) {

        ResourceAddress address = new ResourceAddress(addressString, statementContext);
        String type = address.getResourceType();

        window = new DefaultWindow(Console.MESSAGES.createTitle(type.toUpperCase()));
        window.setWidth(480);
        window.setHeight(360);

        window.setWidget(
                new AddResourceDialog(
                        addressString,
                        statementContext,
                        Console.MODULES.getSecurityFramework().getSecurityContext(token),
                        new AddResourceDialog.Callback() {
                            @Override
                            public void onAddResource(ResourceAddress address, ModelNode payload) {
                                CrudOperationDelegate.this.createResource(address, payload, callback);
                            }

                            @Override
                            public void closeDialogue() {
                                window.hide();
                            }
                        }
                )
        );

        window.setGlassEnabled(true);
        window.center();
    }

    private void createResource(final ResourceAddress address, final ModelNode payload, final Callback... callback) {

        window.hide();

        ModelNode op = address.asOperation(payload);
        op.get(OP).set(ADD);

        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                String name = payload.get(NAME).asString();
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

        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if(response.isFailure())
                {
                    Console.error("Failed to remove resource "+name, response.getFailureDescription());
                    for (Callback cb : callback) {
                        cb.onFailure(address, name, new RuntimeException("Failed to add resource " + name +":"+ response.getFailureDescription()));
                    }
                }
                else
                {

                    Console.info("Removed resource "+ name);
                    for (Callback cb : callback) {
                        cb.onSuccess(address, name);
                    }
                }
            }
        });
    }

    public void onSaveResource(final String addressString, final String name, Map<String, Object> changeset, final Callback... callback) {

        final ResourceAddress address = new ResourceAddress(addressString, statementContext);

        final ModelNodeAdapter adapter = new ModelNodeAdapter();
        ModelNode operation = adapter.fromChangeset(changeset, address.asFqAddress(name));

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
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
