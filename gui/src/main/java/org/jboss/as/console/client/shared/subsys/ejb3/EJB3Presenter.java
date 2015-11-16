package org.jboss.as.console.client.shared.subsys.ejb3;

import com.allen_sauer.gwt.log.client.Log;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.client.v3.widgets.AddResourceDialog;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @since 05/09/14
 */
public class EJB3Presenter extends Presenter<EJB3Presenter.MyView, EJB3Presenter.MyProxy> {

    private final PlaceManager placeManager;
    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final CrudOperationDelegate operationDelegate;
    private final StatementContext statementContext;

    private DefaultWindow window;

    CrudOperationDelegate.Callback defaultOpCallbacks = new CrudOperationDelegate.Callback() {
        @Override
        public void onSuccess(AddressTemplate address, String name) {

            Console.info("Successfully saved resource "+address.resolve(statementContext, name));
            loadContainer();
        }

        @Override
        public void onFailure(AddressTemplate address, String name, Throwable t) {
            Console.error("Failed to save resource "+address.resolve(statementContext, name)+": "+t.getMessage());
        }
    };

    private SecurityFramework securityFramework;
    private ResourceDescriptionRegistry descriptionRegistry;


    @RequiredResources(
            resources = {
                    "{selected.profile}/subsystem=ejb3",
                    "{selected.profile}/subsystem=ejb3/remoting-profile=*",
                    "{selected.profile}/subsystem=ejb3/thread-pool=*",
                    "{selected.profile}/subsystem=ejb3/service=timer-service",
                    "{selected.profile}/subsystem=ejb3/service=remote",
                    "{selected.profile}/subsystem=ejb3/service=iiop",
                    "{selected.profile}/subsystem=ejb3/service=async",
                    "{selected.profile}/subsystem=ejb3/cache=*",
                    "{selected.profile}/subsystem=ejb3/file-passivation-store=*",
                    "{selected.profile}/subsystem=ejb3/cluster-passivation-store=*",
                    "{selected.profile}/subsystem=ejb3/passivation-store=*",
                    "{selected.profile}/subsystem=ejb3/strict-max-bean-instance-pool=*"
            }
    )
    @ProxyCodeSplit
    @NameToken(NameTokens.EJB3Presenter)
    public interface MyProxy extends Proxy<EJB3Presenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(EJB3Presenter presenter);
        void updateContainer(ModelNode container);
        void updateBeanPools(List<Property> properties);
        void updateThreadPools(List<Property> properties);

        void updateRemotingProfiles(List<Property> properties);

        void updateTimerSvc(ModelNode service);

        void updateRemoteSvc(ModelNode modelNode);

        void updateIIOPSvc(ModelNode modelNode);

        void updateAsyncSvc(ModelNode modelNode);

        void updateCaches(List<Property> properties);

        void updatePassivationStores(List<Property> properties);

        void updateFilePassivationStore(List<Property> properties);

        void updateClusterPassivationStore(List<Property> properties);
    }

    @Inject
    public EJB3Presenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            PlaceManager placeManager, RevealStrategy revealStrategy,
            DispatchAsync dispatcher, CoreGUIContext statementContext,
            SecurityFramework securityFramework, ResourceDescriptionRegistry descriptionRegistry) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.revealStrategy = revealStrategy;
        this.dispatcher = dispatcher;
        this.securityFramework = securityFramework;
        this.descriptionRegistry = descriptionRegistry;

        this.statementContext = statementContext;

        this.operationDelegate = new CrudOperationDelegate(this.statementContext, dispatcher);

    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }


    @Override
    protected void onReset() {
        super.onReset();
        loadContainer();
    }

    private void loadContainer() {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "ejb3");
        operation.get("recursive-depth").set(2);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Log.error("Failed to load ejb container ", response.getFailureDescription());
                } else {
                    ModelNode payload = response.get(RESULT);
                    getView().updateContainer(payload);

                    getView().updateBeanPools(payload.get("strict-max-bean-instance-pool").asPropertyList());
                    getView().updateThreadPools(payload.get("thread-pool").asPropertyList());
                    getView().updateRemotingProfiles(payload.get("remoting-profile").asPropertyList());
                    getView().updateCaches(payload.get("cache").asPropertyList());
                    getView().updatePassivationStores(payload.get("passivation-store").asPropertyList());
                    getView().updateFilePassivationStore(payload.get("file-passivation-store").asPropertyList());
                    getView().updateClusterPassivationStore(payload.get("cluster-passivation-store").asPropertyList());

                    // singleton resources
                    ModelNode service = payload.get("service");
                    if(service.hasDefined("timer-service"))
                        getView().updateTimerSvc(service.get("timer-service"));
                    else
                        getView().updateTimerSvc(new ModelNode());

                    if(service.hasDefined("remote"))
                        getView().updateRemoteSvc(service.get("remote"));
                    else
                        getView().updateRemoteSvc(new ModelNode());

                    if(service.hasDefined("iiop"))
                        getView().updateIIOPSvc(service.get("iiop"));
                    else
                        getView().updateIIOPSvc(new ModelNode());

                    if(service.hasDefined("async"))
                        getView().updateAsyncSvc(service.get("async"));
                    else
                        getView().updateAsyncSvc(new ModelNode());
                }

            }
        });
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }


    // -----------------------

    public void onRemoveResource(final AddressTemplate address, final String name) {

        operationDelegate.onRemoveResource(address, name, defaultOpCallbacks);
    }

    public void onCreateResource(AddressTemplate address, String name, ModelNode entity) {
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

                loadContainer();
            }
        });
    }

    public void onSaveResource(final AddressTemplate address, Map<String, Object> changeset) {

        operationDelegate.onSaveResource(address, null, changeset, defaultOpCallbacks);
    }

    public void onSaveResource(AddressTemplate address, String name, Map changeset) {
        operationDelegate.onSaveResource(address, name, changeset, defaultOpCallbacks);
    }

    public PlaceManager getPlaceManager() {
        return placeManager;
    }

    public SecurityFramework getSecurityFramework() {
        return securityFramework;
    }

    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return descriptionRegistry;
    }

    public void onLaunchAddResourceDialog(AddressTemplate address) {
        String type = address.getResourceType();

        window = new DefaultWindow(Console.MESSAGES.createTitle(type.toUpperCase()));
        window.setWidth(480);
        window.setHeight(360);

        window.setWidget(
                new AddResourceDialog(
                        Console.MODULES.getSecurityFramework().getSecurityContext(getProxy().getNameToken()),
                        descriptionRegistry.lookup(address),
                        new AddResourceDialog.Callback() {
                            @Override
                            public void onAdd(ModelNode payload) {
                                window.hide();
                                operationDelegate.onCreateResource(
                                        address, payload.get("name").asString(), payload, defaultOpCallbacks);
                            }

                            @Override
                            public void onCancel() {
                                window.hide();
                            }
                        }
                )
        );

        window.setGlassEnabled(true);
        window.center();
    }

}
