package org.jboss.as.console.client.shared.runtime.elytron;

import java.util.List;

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
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.mbui.behaviour.CoreGUIContext;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;
import org.useware.kernel.gui.behaviour.StatementContext;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Claudio Miranda <claudio@redhat.com>
 */
public class ElytronRuntimePresenter
        extends CircuitPresenter<ElytronRuntimePresenter.MyView, ElytronRuntimePresenter.MyProxy> {

    public static final String ELYTRON_RUNTIME = "/{implicit.host}/{selected.server}/subsystem=elytron";
    public static final String CREDENTIAL_STORE = ELYTRON_RUNTIME + "/credential-store=*";

    public static final AddressTemplate ELYTRON_RUNTIME_TEMPLATE = AddressTemplate.of(ELYTRON_RUNTIME);
    public static final AddressTemplate CREDENTIAL_STORE_TEMPLATE = AddressTemplate.of(CREDENTIAL_STORE);

    @ProxyCodeSplit
    @NameToken(NameTokens.ElytronMetrics)
    @RequiredResources(resources = {
            ELYTRON_RUNTIME,
            CREDENTIAL_STORE
    })
    public interface MyProxy extends Proxy<ElytronRuntimePresenter>, Place {}


    public interface MyView extends View {
        void setPresenter(ElytronRuntimePresenter presenter);
        void updateCredentialReferences(List<Property> models);
        void updateCredentialReferenceAliases(List<ModelNode> models);
    }

    private DispatchAsync dispatcher;
    private RevealStrategy revealStrategy;
    private StatementContext statementContext;

    @Inject
    public ElytronRuntimePresenter(
            DispatchAsync dispatcher, EventBus eventBus, MyView view, MyProxy proxy, CoreGUIContext statementContext,
            RevealStrategy revealStrategy, Dispatcher circuit) {

        super(eventBus, view, proxy, circuit);

        this.dispatcher = dispatcher;
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
    protected void onReset() {
        super.onReset();
        loadCredentialReferences();
    }

    public void loadCredentialReferences() {
        ResourceAddress address = ELYTRON_RUNTIME_TEMPLATE.resolve(statementContext);

        Operation op = new Operation.Builder(READ_CHILDREN_RESOURCES_OPERATION, address)
                .param(CHILD_TYPE, "credential-store")
                .param(INCLUDE_RUNTIME, true)
                .build();

        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error("Failed to load credential stores from " + address.asString(), response.getFailureDescription());
                } else {
                    ModelNode payload = response.get(RESULT);
                    getView().updateCredentialReferences(payload.asPropertyList());
                }
            }
        });
    }

    public void loadAliases(AddressTemplate template, String storeName) {

        ResourceAddress serverResource = template.replaceWildcards(storeName).resolve(statementContext);
        Operation op = new Operation.Builder(READ_ALIASES_OPERATION, serverResource).build();

        dispatcher.execute(new DMRAction(op), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();

                if (response.isFailure()) {
                    Console.error("Failed to load aliases from store " + serverResource, response.getFailureDescription());
                } else {
                    ModelNode payload = response.get(RESULT);
                    getView().updateCredentialReferenceAliases(payload.asList());
                }

            }
        });
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInRuntimeParent(this);
    }

    @Override
    protected void onAction(final Action action) {
        Scheduler.get().scheduleDeferred(() -> loadCredentialReferences());
    }

    public void saveAlias(AddressTemplate template, final String storeName, final ModelNode payload,
            final boolean isEditOperation) {

        String opName = isEditOperation ? SET_SECRET_OPERATION : ADD_ALIAS_OPERATION;
        ResourceAddress address = template.replaceWildcards(storeName).resolve(statementContext);
        Operation.Builder opBuilder = new Operation.Builder(opName, address)
                .param("alias", payload.get("alias"));
        if (payload.hasDefined("secret-value"))
            opBuilder.param("secret-value", payload.get("secret-value"));
        if (payload.hasDefined("entry-type"))
            opBuilder.param("entry-type", payload.get("entry-type"));

        Operation operation = opBuilder.build();

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error("Failed to save alias for credential store: " + storeName + ". Address: " + address.asString(), response.getFailureDescription());
                } else {
                    loadAliases(template, storeName);
                }
            }
        });

    }

    public void removeAlias(final AddressTemplate template, final String storeName, final String alias) {
        ResourceAddress address = template.replaceWildcards(storeName).resolve(statementContext);
        Operation operation = new Operation.Builder(REMOVE_ALIAS_OPERATION, address)
                .param("alias", alias)
                .build();

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {

            @Override
            public void onSuccess(DMRResponse result) {
                ModelNode response = result.get();
                if (response.isFailure()) {
                    Console.error("Failed to remove alias (" + alias + ")for credential store: " + storeName + ". Address: " + address.asString(), response.getFailureDescription());
                } else {
                    loadAliases(template, storeName);
                }
            }
        });
    }
}
