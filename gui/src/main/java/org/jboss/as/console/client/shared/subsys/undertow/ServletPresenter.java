package org.jboss.as.console.client.shared.subsys.undertow;

import com.allen_sauer.gwt.log.client.Log;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.ManualRevealPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.mbui.behaviour.CrudOperationDelegate;
import org.jboss.as.console.mbui.behaviour.DefaultPresenterContract;
import org.jboss.as.console.mbui.dmr.ResourceAddress;
import org.jboss.as.console.mbui.widgets.AddResourceDialog;
import org.jboss.as.console.spi.RequiredResources;
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
public class ServletPresenter extends ManualRevealPresenter<ServletPresenter.MyView, ServletPresenter.MyProxy>
        implements DefaultPresenterContract {

    private final PlaceManager placeManager;
    private final RevealStrategy revealStrategy;
    private final DispatchAsync dispatcher;
    private final CrudOperationDelegate operationDelegate;
    private final FilteringStatementContext statementContext;

    private String currentContainer = null;
    private DefaultWindow window;

    CrudOperationDelegate.Callback defaultOpCallbacks = new CrudOperationDelegate.Callback() {
        @Override
        public void onSuccess(ResourceAddress address, String name) {
            if(address.getResourceType().equals("servlet-container"))
                loadContainer();
            else
                loadDetails();
        }

        @Override
        public void onFailure(ResourceAddress address, String name, Throwable t) {
            // noop
        }
    };

    @RequiredResources(resources = {"{selected.profile}/subsystem=undertow/servlet-container=*"})
    @ProxyCodeSplit
    @NameToken(NameTokens.ServletPresenter)
    public interface MyProxy extends ProxyPlace<ServletPresenter> {
    }

    public interface MyView extends View {
        void setPresenter(ServletPresenter presenter);
        void setServletContainer(List<Property> container);
        void setServerSelection(String name) ;
        void setJSPSettings(ModelNode data) ;
    }

    @Inject
    public ServletPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            PlaceManager placeManager, RevealStrategy revealStrategy,
            DispatchAsync dispatcher, CoreGUIContext statementContext) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.revealStrategy = revealStrategy;
        this.dispatcher = dispatcher;

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
    protected void withRequest(PlaceRequest request) {
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
        operation.get(ADDRESS).add("setting", "jsp");

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
                    getView().setJSPSettings(data);
                }

            }
        });
    }

    // -----------------------

    @Override
    public void onLaunchAddResourceDialog(final String addressString) {

        ResourceAddress address = new ResourceAddress(addressString, statementContext);
        String type = address.getResourceType();

        window = new DefaultWindow(Console.MESSAGES.createTitle(type.toUpperCase()));
        window.setWidth(480);
        window.setHeight(360);

        window.setWidget(
                new AddResourceDialog(
                        addressString,
                        statementContext,
                        Console.MODULES.getSecurityFramework().getSecurityContext(NameTokens.HttpPresenter),
                        new AddResourceDialog.Callback() {
                            @Override
                            public void onAddResource(ResourceAddress address, ModelNode payload) {
                                operationDelegate.onCreateResource(addressString, payload, defaultOpCallbacks);
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

    @Override
    public void onRemoveResource(final String addressString, final String name) {

        operationDelegate.onRemoveResource(addressString, name, defaultOpCallbacks);
    }

    @Override
    public void onSaveResource(final String addressString, String name, Map<String, Object> changeset) {

        operationDelegate.onSaveResource(addressString, name, changeset, defaultOpCallbacks);
    }

    public PlaceManager getPlaceManager() {
        return placeManager;
    }
}
