package org.jboss.as.console.client.shared.subsys.undertow;

import com.allen_sauer.gwt.log.client.Log;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.core.CircuitPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.RequiredResourcesContext;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.rbac.SecurityFramework;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.v3.ResourceDescriptionRegistry;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.stores.domain.ServerStore;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;
import org.useware.kernel.gui.behaviour.FilteringStatementContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;


/**
 * @author Heiko Braun
 */
public class HttpMetricPresenter extends CircuitPresenter<HttpMetricPresenter.MyView,
        HttpMetricPresenter.MyProxy> implements CommonHttpPresenter {


    private final PlaceManager placeManager;
    private final FilteringStatementContext statementContext;
    private DispatchAsync dispatcher;
    private RevealStrategy revealStrategy;
    private final ServerStore serverStore;
    private String currentServer;
    private ResourceDescriptionRegistry descriptionRegistry;
    private final SecurityFramework securityFramework;

    public SecurityFramework getSecurityFramework() {
        return securityFramework;
    }


    @ProxyCodeSplit
    @NameToken(NameTokens.HttpMetrics)
    @RequiredResources(
            resources = {
                    "/{selected.host}/{selected.server}/subsystem=undertow",
                    "/{selected.host}/{selected.server}/subsystem=undertow/server={undertow.server}"
            }
    )
    @SearchIndex(keywords = {"web", "connections", "http"})
    public interface MyProxy extends Proxy<HttpMetricPresenter>, Place {}


    public interface MyView extends View {
        void setPresenter(HttpMetricPresenter presenter);

        void clearSamples();

        void setServer(List list);

        void setServerSelection(String currentServer);

        void setConnectors(List<Property> connectors);
    }

    @Inject
    public HttpMetricPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            PlaceManager placeManager,  DispatchAsync dispatcher, Dispatcher circuit,
            RevealStrategy revealStrategy, ServerStore serverStore,
            ResourceDescriptionRegistry descriptionRegistry, CoreGUIContext delegate, SecurityFramework securityFramework) {
        super(eventBus, view, proxy, circuit);

        this.placeManager = placeManager;

        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
        this.serverStore = serverStore;

        this.descriptionRegistry = descriptionRegistry;
        this.securityFramework = securityFramework;

        this.statementContext  = new FilteringStatementContext(
                delegate,
                new FilteringStatementContext.Filter() {
                    @Override
                    public String filter(String key) {
                        if ("undertow.server".equals(key))
                            return currentServer!=null ? currentServer : "*";
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
    }


    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        addChangeHandler(serverStore);
    }

    @Override
    protected void onAction(Action action) {
        getView().clearSamples();

    }

    @Override
    protected void onReset() {
        super.onReset();
        loadServer();

    }

    private void loadServer() {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "undertow");
        operation.get(CHILD_TYPE).set("server");

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Log.error("Failed to load http server", response.getFailureDescription());
                    getView().setServer(Collections.EMPTY_LIST);
                } else {
                    getView().setServer(response.get(RESULT).asPropertyList());

                    getView().setServerSelection(currentServer);
                }

            }
        });
    }

    public void loadDetails() {

        // no server selected
        if(null==currentServer)
            return;

        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "undertow");
        operation.get(ADDRESS).add("server", currentServer);
        operation.get(INCLUDE_RUNTIME).set(true);
        operation.get(RECURSIVE).set(true);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if(response.isFailure())
                {
                    Log.error("Failed to load http server details", response.getFailureDescription());
                }
                else
                {
                    ModelNode data = response.get(RESULT);
                    List<Property> connectors = new ArrayList<Property>();
                    connectors.addAll(failSafeGetCollection(data.get("http-listener")));
                    connectors.addAll(failSafeGetCollection(data.get("ajp-listener")));
                    connectors.addAll(failSafeGetCollection(data.get("https-listener")));

                    getView().setConnectors(connectors);
                }

            }
        });
    }

    private static List<Property> failSafeGetCollection(ModelNode item) {
        if(item.isDefined())
            return item.asPropertyList();
        else
            return Collections.EMPTY_LIST;
    }


    @Override
    public void prepareFromRequest(PlaceRequest request) {
        currentServer = request.getParameter("name", null);
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInRuntimeParent(this);
    }

    @Override
    public String getNameToken() {
        return getProxy().getNameToken();
    }

    @Override
    public PlaceManager getPlaceManager() {
        return placeManager;
    }

    @Override
    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return descriptionRegistry;
    }

    @Override
    public void onSaveResource(AddressTemplate resourceAddress, String name, Map changeset) {

    }
}
