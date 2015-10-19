package org.jboss.as.console.client.shared.runtime.ws;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Scheduler;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.CircuitPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.LoggingCallback;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.ws.EndpointRegistry;
import org.jboss.as.console.client.shared.subsys.ws.model.WebServiceEndpoint;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 1/23/12
 */
public class WebServiceRuntimePresenter
        extends CircuitPresenter<WebServiceRuntimePresenter.MyView, WebServiceRuntimePresenter.MyProxy> {

    @ProxyCodeSplit
    @NameToken(NameTokens.WebServiceRuntimePresenter)
    @SearchIndex(keywords = {"webservices", "jaxws", "endpoint", "jbossws"})
    @AccessControl(resources = {"/{implicit.host}/{selected.server}/deployment=*/subsystem=webservices"})
    public interface MyProxy extends Proxy<WebServiceRuntimePresenter>, Place {}


    public interface MyView extends View {
        void setPresenter(WebServiceRuntimePresenter presenter);
        void updateEndpoints(List<WebServiceEndpoint> endpoints);
    }


    private EndpointRegistry endpointRegistry;
    private RevealStrategy revealStrategy;

    @Inject
    public WebServiceRuntimePresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            PlaceManager placeManager, EndpointRegistry registry,
            RevealStrategy revealStrategy, Dispatcher circuit) {
        super(eventBus, view, proxy, circuit);

        this.endpointRegistry = registry;
        this.revealStrategy = revealStrategy;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        addChangeHandler(Console.MODULES.getServerStore());
    }

    @Override
    protected void onAction(Action action) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                loadEndpoints(false);

            }
        });    }

    @Override
    protected void onReset() {
        super.onReset();

        loadEndpoints(false);
    }

    public void loadEndpoints(boolean msg) {

        endpointRegistry.create().refreshEndpoints(new LoggingCallback<List<WebServiceEndpoint>>() {

            @Override
            public void onFailure(Throwable caught) {
                Log.error(caught.getMessage());
            }

            @Override
            public void onSuccess(List<WebServiceEndpoint> result) {
                getView().updateEndpoints(result);

                if(msg) Console.info("Successfully refreshed statistics.");

            }
        });
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInRuntimeParent(this);
    }
}
