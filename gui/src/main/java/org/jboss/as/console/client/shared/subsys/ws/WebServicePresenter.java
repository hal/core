package org.jboss.as.console.client.shared.subsys.ws;

import com.allen_sauer.gwt.log.client.Log;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
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
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 6/10/11
 */
public class WebServicePresenter extends Presenter<WebServicePresenter.MyView, WebServicePresenter.MyProxy> {



    @ProxyCodeSplit
    @NameToken(NameTokens.WebServicePresenter)
    @AccessControl(resources = {"{selected.profile}/subsystem=webservices"})
    @SearchIndex(keywords = {"web", "wsdl", "soap", "client-config", "endpoint-config"})
    public interface MyProxy extends Proxy<WebServicePresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(WebServicePresenter presenter);
        void reset();
        void updateFrom(ModelNode modelNode);
    }

    private final CrudOperationDelegate operationDelegate;
    private SecurityFramework securityFramework;
    private final ResourceDescriptionRegistry resourceDescriptionRegistry;
    private DispatchAsync dispatcher;
    private RevealStrategy revealStrategy;

    CrudOperationDelegate.Callback defaultOpCallbacks = new CrudOperationDelegate.Callback() {
        @Override
        public void onSuccess(AddressTemplate address, String name) {
            Console.info(Console.MESSAGES.successfullyModifiedResource(address.toString()));
            loadProvider();
        }

        @Override
        public void onFailure(AddressTemplate addressTemplate, String name, Throwable t) {
            Console.error(Console.MESSAGES.failedToModifyResource(addressTemplate.toString()), t.getMessage());
        }
    };


    @Inject
    public WebServicePresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            DispatchAsync dispatcher,
            RevealStrategy revealStrategy,
            SecurityFramework securityFramework,
            ResourceDescriptionRegistry resourceDescriptionRegistry, CoreGUIContext statementContext) {
        super(eventBus, view, proxy);

        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
        this.securityFramework = securityFramework;
        this.resourceDescriptionRegistry = resourceDescriptionRegistry;
        this.operationDelegate = new CrudOperationDelegate(statementContext, dispatcher);
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }


    @Override
    protected void onReset() {
        super.onReset();

        loadProvider();
    }

    private void loadProvider() {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(ADDRESS).set(Baseadress.get());
        operation.get(ADDRESS).add("subsystem", "webservices");

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Log.error("Failed to load web service provider", response.getFailureDescription());
                    getView().reset();
                } else {
                    getView().updateFrom(response.get(RESULT));
                }

            }
        });
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    public SecurityFramework getSecurityFramework() {
        return securityFramework;
    }

    public ResourceDescriptionRegistry getDescriptionRegistry() {
        return resourceDescriptionRegistry;
    }

    public void onSaveResource(final AddressTemplate address, Map<String, Object> changeset) {
        operationDelegate.onSaveResource(address, null, changeset, defaultOpCallbacks);
    }

}
