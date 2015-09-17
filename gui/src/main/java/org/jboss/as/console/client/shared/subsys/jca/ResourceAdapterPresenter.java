package org.jboss.as.console.client.shared.subsys.jca;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.jca.model.AdminObject;
import org.jboss.as.console.client.shared.subsys.jca.model.ConnectionDefinition;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.behaviour.ModelNodeAdapter;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 */
public class ResourceAdapterPresenter
        extends Presenter<ResourceAdapterPresenter.MyView, ResourceAdapterPresenter.MyProxy> {

    private final PlaceManager placeManager;
    private RevealStrategy revealStrategy;
    private DispatchAsync dispatcher;
    private DefaultWindow window;

    private final ResourceDescriptionRegistry descriptionRegistry;
    private final SecurityFramework securityFramework;
    private final CoreGUIContext statementContext;

    private String selectedAdapter;

    public StatementContext getStatementContext() {
        return statementContext;
    }

    public DispatchAsync getDispatcher() {
        return dispatcher;
    }


    @ProxyCodeSplit
    @NameToken(NameTokens.ResourceAdapterPresenter)
    @RequiredResources(
            resources = {
                    "{selected.profile}/subsystem=resource-adapters/resource-adapter=*",
                    "{selected.profile}/subsystem=resource-adapters/resource-adapter=*/config-properties=*",
                    "{selected.profile}/subsystem=resource-adapters/resource-adapter=*/admin-objects=*",
                    "{selected.profile}/subsystem=resource-adapters/resource-adapter=*/connection-definitions=*"
            }
    )
    @SearchIndex(keywords = {"jca", "resource-adapter", "connector", "workmanager", "bootstrap-context"})
    public interface MyProxy extends Proxy<ResourceAdapterPresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(ResourceAdapterPresenter presenter);
        void setAdapter(Property payload);
    }

    @Inject
    public ResourceAdapterPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            PlaceManager placeManager, RevealStrategy revealStrategy,
            DispatchAsync dispatcher, ResourceDescriptionRegistry descriptionRegistry, SecurityFramework securityFramework,
            CoreGUIContext statementContext) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.revealStrategy = revealStrategy;
        this.dispatcher = dispatcher;

        this.descriptionRegistry = descriptionRegistry;
        this.securityFramework = securityFramework;
        this.statementContext = statementContext;

    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {
        this.selectedAdapter = request.getParameter("name", null);
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadAdapter();
    }

    private void loadAdapter() {

        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "resource-adapters");
        operation.get(ADDRESS).add("resource-adapter", selectedAdapter);
        operation.get(RECURSIVE).set(true);


        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse response) {
                ModelNode result = response.get();
                ModelNode resourceAdapter = result.get(RESULT).asObject();
                getView().setAdapter(new Property(selectedAdapter, resourceAdapter));
            }
        });
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }


    /*public void onCreate(AddressTemplate address, String name, ModelNode entity) {

        ResourceAddress fqAddress = address.resolve(statementContext, name);

        entity.get(OP).set(ADD);
        entity.get(ADDRESS).set(fqAddress);

        dispatcher.execute(new DMRAction(entity), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error("Failed to create resource " + fqAddress, response.getFailureDescription());
                } else {

                    Console.info("Successfully created " + fqAddress);
                }

                loadAdapter();
            }
        });
    }*/

    public void onCreateProperty(AddressTemplate address, ModelNode entity, String... names) {
        LinkedList<String> args = new LinkedList<>();
        args.add(0, selectedAdapter);
        for (String name : names) {
            args.add(name);
        }

        ResourceAddress fqAddress = address.resolve(statementContext, args);
        entity.get(OP).set(ADD);
        entity.get(ADDRESS).set(fqAddress);

        dispatcher.execute(new DMRAction(entity), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error("Failed to create resource " + fqAddress, response.getFailureDescription());
                } else {

                    Console.info("Successfully created " + fqAddress);
                }

                loadAdapter();
            }
        });
    }



    public void onSaveChildResource(AddressTemplate address, String name, Map changeset) {
        ResourceAddress fqAddress = address.resolve(statementContext, selectedAdapter, name);

        final ModelNodeAdapter adapter = new ModelNodeAdapter();
        ModelNode operation = adapter.fromChangeset(changeset, fqAddress);

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                Console.error("Failed to modify resource " + fqAddress, caught.getMessage());
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    Console.error("Failed to modify resource " + fqAddress, response.getFailureDescription());
                } else {
                    Console.info("Successfully saved " + fqAddress);
                }

                loadAdapter();
            }
        });


    }

    public void onRemoveChildResource(AddressTemplate address, Property resource) {

        ResourceAddress fqAddress = address.resolve(statementContext, selectedAdapter, resource.getName());

        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(ADDRESS).set(fqAddress);

        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {

            @Override
            public void onFailure(Throwable caught) {
                super.onFailure(caught);
                loadAdapter();
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {

                ModelNode response = dmrResponse.get();
                if(response.isFailure())
                {
                    Console.error("Failed to remove resource "+fqAddress, response.getFailureDescription());
                }
                else
                {
                    Console.info("Successfully removed " + fqAddress);
                }

                loadAdapter();
            }
        });


    }


    public void onRemoveProperty(AddressTemplate address, String... names) {

        LinkedList<String> args = new LinkedList<>();
        args.add(0, selectedAdapter);
        for (String name : names) {
            args.add(name);
        }

        ResourceAddress fqAddress = address.resolve(statementContext, args);
        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(ADDRESS).set(fqAddress);

        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error("Failed to remove resource " + fqAddress, response.getFailureDescription());
                } else {

                    Console.info("Successfully removed " + fqAddress);
                }

                loadAdapter();
            }
        });
    }


    public void onSave(AddressTemplate address, String name, Map<String, Object> changeset) {
        ResourceAddress fqAddress = address.resolve(statementContext, name);

        final ModelNodeAdapter adapter = new ModelNodeAdapter();
        ModelNode operation = adapter.fromChangeset(changeset, fqAddress);

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(Throwable caught) {
                Console.error("Failed to modify resource " + fqAddress, caught.getMessage());
            }

            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    Console.error("Failed to modify resource " + fqAddress, response.getFailureDescription());
                } else {
                    Console.info("Successfully saved " + fqAddress);
                }

                loadAdapter();
            }
        });

    }

    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return descriptionRegistry;
    }

    public SecurityFramework getSecurityFramework() {
        return securityFramework;
    }

    public void onLaunchAddWizard(AddressTemplate address) {


        final SecurityContext securityContext =
                getSecurityFramework().getSecurityContext(getProxy().getNameToken());

        final ResourceDescription resourceDescription = getDescriptionRegistry().lookup(address);

        final DefaultWindow dialog = new DefaultWindow("New "+address.getResourceType());
        AddResourceDialog addDialog = new AddResourceDialog(securityContext, resourceDescription,
                new AddResourceDialog.Callback() {
                    @Override
                    public void onAdd(ModelNode payload) {
                        dialog.hide();

                        final ResourceAddress fqAddress =
                                address.resolve(statementContext, selectedAdapter, payload.get("name").asString());

                        payload.get(OP).set(ADD);
                        payload.get(ADDRESS).set(fqAddress);

                        dispatcher.execute(new DMRAction(payload), new SimpleCallback<DMRResponse>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                super.onFailure(caught);
                                loadAdapter();
                            }

                            @Override
                            public void onSuccess(DMRResponse dmrResponse) {
                                Console.info("Successfully added "+fqAddress);
                                loadAdapter();
                            }
                        });


                    }

                    @Override
                    public void onCancel() {
                        dialog.hide();
                    }
                });
        dialog.setWidth(640);
        dialog.setHeight(480);
        dialog.setWidget(addDialog);
        dialog.setGlassEnabled(true);
        dialog.center();
    }

    public void closeDialoge() {
        window.hide();
    }



    public PlaceManager getPlaceManager() {
        return placeManager;
    }

    public void onDoFlush(ConnectionDefinition entity, String flushOp) {

       /* ModelNode operation = connectionMetaData.getAddress().asResource(
                Baseadress.get(), selectedAdapter, entity.getName());

        operation.get(OP).set(flushOp);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {

                ModelNode response  = result.get();
                if(response.isFailure())
                    Console.error(Console.MESSAGES.failed("Flush Pool"), response.getFailureDescription());
                else
                    Console.info(Console.MESSAGES.successful("Flush Pool"));
            }
        });*/
    }

    // https://issues.jboss.org/browse/AS7-3259
    public void enOrDisbaleConnection(ConnectionDefinition selection) {
        /*ModelNode operation = connectionMetaData.getAddress().asResource(
                Baseadress.get(), selectedAdapter, selection.getName());


        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("enabled");
        operation.get(VALUE).set(selection.isEnabled());

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if(response.isFailure())
                    Console.error(Console.MESSAGES.modificationFailed("Connection Definition"), response.getFailureDescription());
                else
                    Console.info(Console.MESSAGES.modified("Connection Definition"));
                loadAdapter(true);
            }
        });*/
    }

    public void enOrDisbaleAdminObject(AdminObject selection) {
       /* ModelNode operation = adminMetaData.getAddress().asResource(
                Baseadress.get(), selectedAdapter, selection.getName());

        operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("enabled");
        operation.get(VALUE).set(selection.isEnabled());

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if(response.isFailure())
                    Console.error(Console.MESSAGES.modificationFailed("Admin Object"), response.getFailureDescription());
                else
                    Console.info(Console.MESSAGES.modified("Admin Object"));
                loadAdapter(true);
            }
        });*/
    }
}
