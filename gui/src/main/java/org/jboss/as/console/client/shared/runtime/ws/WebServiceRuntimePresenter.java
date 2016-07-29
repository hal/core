package org.jboss.as.console.client.shared.runtime.ws;

import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Scheduler;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.CircuitPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.LoggingCallback;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.ws.EndpointRegistry;
import org.jboss.as.console.client.shared.subsys.ws.model.WebServiceEndpoint;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;
import org.useware.kernel.gui.behaviour.StatementContext;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 1/23/12
 */
public class WebServiceRuntimePresenter
        extends CircuitPresenter<WebServiceRuntimePresenter.MyView, WebServiceRuntimePresenter.MyProxy> {

    private static final AddressTemplate WEBSERVICES_TEMPLATE = AddressTemplate.of("{selected.profile}/subsystem=webservices");
    
    @ProxyCodeSplit
    @NameToken(NameTokens.WebServiceRuntimePresenter)
    @SearchIndex(keywords = {"webservices", "jaxws", "endpoint", "jbossws"})
    @AccessControl(resources = {"/{implicit.host}/{selected.server}/deployment=*/subsystem=webservices"})
    public interface MyProxy extends Proxy<WebServiceRuntimePresenter>, Place {}


    public interface MyView extends View {
        void setPresenter(WebServiceRuntimePresenter presenter);
        void updateEndpoints(List<WebServiceEndpoint> endpoints);
        void setStatistcsEnabled(boolean stats);
    }

    private DispatchAsync dispatcher;
    private EndpointRegistry endpointRegistry;
    private RevealStrategy revealStrategy;
    private StatementContext statementContext;

    @Inject
    public WebServiceRuntimePresenter(
            DispatchAsync dispatcher,
            EventBus eventBus, MyView view, MyProxy proxy,
            EndpointRegistry registry, CoreGUIContext statementContext,
            RevealStrategy revealStrategy, Dispatcher circuit) {
        super(eventBus, view, proxy, circuit);

        this.dispatcher = dispatcher;
        this.endpointRegistry = registry;
        this.revealStrategy = revealStrategy;
        this.statementContext = statementContext;
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
        loadStatisticsAttribute();
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

                if(msg) Console.info(Console.CONSTANTS.successfullyRefreshedStatistics());

            }
        });
    }
    private void loadStatisticsAttribute() {
        ResourceAddress serverResource = WEBSERVICES_TEMPLATE.resolve(statementContext);

        Operation op = new Operation.Builder(READ_RESOURCE_OPERATION, serverResource)
                .build();

        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Log.error("Failed to load webservices statistics attribute", response.getFailureDescription());
                    getView().setStatistcsEnabled(false);
                } else {
                    ModelNode payload = response.get(RESULT);
                    getView().setStatistcsEnabled(payload.get("statistics-enabled").asBoolean());
                }

            }
        });
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInRuntimeParent(this);
    }
}
