package org.jboss.as.console.client.shared.subsys.undertow;

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
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
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
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.FilteringStatementContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @since 05/09/14
 */
public class ServletPresenter extends Presenter<ServletPresenter.MyView, ServletPresenter.MyProxy> {

    private final PlaceManager placeManager;
    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final CrudOperationDelegate operationDelegate;
    private final FilteringStatementContext statementContext;

    private String currentContainer = null;
    private DefaultWindow window;

    CrudOperationDelegate.Callback defaultOpCallbacks = new CrudOperationDelegate.Callback() {
        @Override
        public void onSuccess(AddressTemplate address, String name) {
            if(address.getResourceType().equals("servlet-container"))
                loadContainer();
            else
                loadDetails();
        }

        @Override
        public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {

        }
    };

    private SecurityFramework securityFramework;
    private ResourceDescriptionRegistry descriptionRegistry;

    public SecurityFramework getSecurityFramework() {
        return securityFramework;
    }

    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return descriptionRegistry;
    }

    public void clearContainer() {
        currentContainer = null;
    }



    @RequiredResources(
            resources = {
                    "{selected.profile}/subsystem=undertow/servlet-container=*",
                    "{selected.profile}/subsystem=undertow/servlet-container=*/setting=jsp",
                    "{selected.profile}/subsystem=undertow/servlet-container=*/setting=websockets",
                    "{selected.profile}/subsystem=undertow/servlet-container=*/setting=persistent-sessions",
                    "{selected.profile}/subsystem=undertow/servlet-container=*/setting=session-cookie"
            }
    )
    @ProxyCodeSplit
    @NameToken(NameTokens.ServletPresenter)
    public interface MyProxy extends Proxy<ServletPresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(ServletPresenter presenter);
        void setServletContainer(List<Property> container);
        void setServerSelection(String name) ;
        void setJSPSettings(ModelNode data) ;
        void setWSSettings(ModelNode modelNode);

        void setCookieSettings(ModelNode setting);

        void setSessionSettings(ModelNode setting);
    }

    @Inject
    public ServletPresenter(
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

        this.statementContext =  new FilteringStatementContext(
                statementContext,
                new FilteringStatementContext.Filter() {
                    @Override
                    public String filter(String key) {
                        if ("selected.container".equals(key))
                            return currentContainer;
                        else
                            return null;
                    }

                    @Override
                    public String[] filterTuple(String key) {
                        return null;
                    }
                }
        ) {

        };

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
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "undertow");
        operation.get(CHILD_TYPE).set("servlet-container");

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Log.error("Failed to load http server", response.getFailureDescription());
                    getView().setServletContainer(Collections.EMPTY_LIST);
                } else {
                    getView().setServletContainer(response.get(RESULT).asPropertyList());
                    getView().setServerSelection(currentContainer);
                }

            }
        });
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    @Override
    public void prepareFromRequest(PlaceRequest request) {
        currentContainer = request.getParameter("name", null);
    }

    public void loadDetails() {

        // no server selected
        if(null==currentContainer)
            return;

        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "undertow");
        operation.get(ADDRESS).add("servlet-container", currentContainer);
        operation.get(RECURSIVE).set(true);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if(response.isFailure())
                {
                    Log.error("Failed to load servlet container details", response.getFailureDescription());
                }
                else
                {
                    ModelNode data = response.get(RESULT);
                    getView().setJSPSettings(data.get("setting").get("jsp"));
                    getView().setWSSettings(data.get("setting").get("websockets"));
                    getView().setSessionSettings(data.get("setting").get("persistent-sessions"));
                    getView().setCookieSettings(data.get("setting").get("session-cookie"));
                }

            }
        });
    }

    // -----------------------

    /*public void onLaunchAddResourceDialog(final AddressTemplate address) {


        String type = address.getResourceType();

        window = new DefaultWindow(Console.MESSAGES.createTitle(type.toUpperCase()));
        window.setWidth(480);
        window.setHeight(360);

        SecurityContext securityContext =
                Console.MODULES.getSecurityFramework().getSecurityContext(NameTokens.HttpPresenter);

        window.setWidget(
                new AddResourceDialog(
                        securityContext,
                        descriptionRegistry.lookup(address),
                        new AddResourceDialog.Callback() {

                            @Override
                            public void onAdd(ModelNode payload) {
                                operationDelegate.onCreateResource(
                                        address,
                                        payload.get("name").asString(),
                                        payload, defaultOpCallbacks
                                );
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

    }*/

    public void onRemoveResource(final AddressTemplate address, final String name) {

        operationDelegate.onRemoveResource(address, name, defaultOpCallbacks);
    }

    public void onCreateContainerSettings(AddressTemplate address, ModelNode entity) {
        ResourceAddress fqAddress = address.resolve(statementContext, currentContainer);

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

    public void onSaveContainerSettings(final AddressTemplate address, Map<String, Object> changeset) {

        operationDelegate.onSaveResource(address, currentContainer, changeset, defaultOpCallbacks);
    }

    public void onSaveContainer(AddressTemplate address, String name, Map changeset) {
        operationDelegate.onSaveResource(address, name, changeset, defaultOpCallbacks);
    }

    public PlaceManager getPlaceManager() {
        return placeManager;
    }
}
