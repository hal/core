package org.jboss.as.console.client.shared.subsys.tx;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.HasPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.v3.behaviour.CrudOperationDelegate;
import org.jboss.as.console.client.v3.dmr.AddressTemplate;
import org.jboss.as.console.client.v3.dmr.Composite;
import org.jboss.as.console.client.v3.dmr.Operation;
import org.jboss.as.console.client.v3.dmr.ResourceAddress;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.as.console.spi.SubsystemExtension;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.Map;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;


/**
 * @author Heiko Braun
 * @date 10/25/11
 */
public class TransactionPresenter extends Presenter<TransactionPresenter.MyView, TransactionPresenter.MyProxy> {

    private class SwitchCallback implements AsyncCallback<DMRResponse> {

        @Override
        public void onFailure(final Throwable caught) {
            Console.error(Console.CONSTANTS.common_error_unknownError(), caught.getMessage());
        }

        @Override
        public void onSuccess(final DMRResponse response) {
            ModelNode result = response.get();
            if (result.isFailure()) {
                Console.error("Unable to switch process id", result.getFailureDescription());
            } else {
                loadModel();
            }
        }
    }


    static final String ROOT_ADDRESS = "{selected.profile}/subsystem=transactions";
    static final AddressTemplate ROOT_ADDRESS_TEMPLATE = AddressTemplate.of(ROOT_ADDRESS);

    static final String PROCESS_ID_UUID = "process-id-uuid";
    static final String PROCESS_ID_SOCKET_BINDING = "process-id-socket-binding";
    static final String PROCESS_ID_SOCKET_MAX_PORTS = "process-id-socket-max-ports";


    // @formatter:off
    @ProxyCodeSplit
    @NameToken(NameTokens.TransactionPresenter)
    @RequiredResources(resources = ROOT_ADDRESS)
    @SearchIndex(keywords = {"transaction", "log-store"})
    @SubsystemExtension(name = "Transactions", group = "Container", key = "transactions")
    public interface MyProxy extends Proxy<TransactionPresenter>, Place {}

    public interface MyView extends View, HasPresenter<TransactionPresenter> {
        void updateModel(ModelNode modelNode);
    }
    // @formatter:on


    private final RevealStrategy revealStrategy;
    private final StatementContext statementContext;
    private final DispatchAsync dispatcher;
    private final CrudOperationDelegate operationDelegate;

    @Inject
    public TransactionPresenter(EventBus eventBus, MyView view, MyProxy proxy, RevealStrategy revealStrategy,
            StatementContext statementContext, DispatchAsync dispatcher) {
        super(eventBus, view, proxy);

        this.revealStrategy = revealStrategy;
        this.dispatcher = dispatcher;
        this.statementContext = statementContext;
        this.operationDelegate = new CrudOperationDelegate(statementContext, dispatcher);
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadModel();
    }

    private void loadModel() {
        Operation operation = new Operation.Builder(READ_RESOURCE_OPERATION,
                ROOT_ADDRESS_TEMPLATE.resolve(statementContext)).build();

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                if (response.isFailure()) {
                    Console.error(Console.CONSTANTS.common_error_unknownError(), response.getFailureDescription());
                } else {
                    getView().updateModel(response.get(RESULT));
                }
            }
        });

    }

    public void saveConfig(Map<String, Object> changeset) {
        operationDelegate.onSaveResource(ROOT_ADDRESS_TEMPLATE, null, changeset,
                new CrudOperationDelegate.Callback() {
                    @Override
                    public void onSuccess(final AddressTemplate addressTemplate, final String name) {
                        Console.info(Console.MESSAGES.modified("Transaction Manager"));
                    }

                    @Override
                    public void onFailure(final AddressTemplate addressTemplate, final String name, final Throwable t) {
                        Console.error(Console.MESSAGES.modificationFailed("Transaction Manager"), t.getMessage());
                    }
                });
    }

    public void saveProcessSettings(Boolean uuid, String socketBinding, Integer maxPorts) {
        if (uuid != null && uuid && socketBinding == null) {
            switchToUuid();
        } else if (socketBinding != null && (uuid == null || !uuid)) {
            switchToSocketBinding(socketBinding, maxPorts);
        } else {
            Console.error("Please set either UUID or socket binding");
        }
    }

    private void switchToUuid() {
        ResourceAddress address = TransactionPresenter.ROOT_ADDRESS_TEMPLATE.resolve(statementContext);
        Operation op = new Operation.Builder(WRITE_ATTRIBUTE_OPERATION, address)
                .param(NAME, PROCESS_ID_UUID)
                .param(VALUE, true)
                .build();
        dispatcher.execute(new DMRAction(op), new SwitchCallback());
    }

    private void switchToSocketBinding(String socketBinding, Integer maxPorts) {
        Composite composite;
        ResourceAddress address = TransactionPresenter.ROOT_ADDRESS_TEMPLATE.resolve(statementContext);

        Operation writeSocketBinding = new Operation.Builder(WRITE_ATTRIBUTE_OPERATION, address)
                .param(NAME, PROCESS_ID_SOCKET_BINDING)
                .param(VALUE, socketBinding)
                .build();
        Operation undefineUuid = new Operation.Builder("undefine-attribute", address)
                .param(NAME, PROCESS_ID_UUID)
                .build();
        if (maxPorts != null) {
            Operation writeMaxPorts = new Operation.Builder(WRITE_ATTRIBUTE_OPERATION, address)
                    .param(NAME, PROCESS_ID_SOCKET_MAX_PORTS)
                    .param(VALUE, maxPorts)
                    .build();
            composite = new Composite(undefineUuid, writeSocketBinding, writeMaxPorts);
        } else {
            composite = new Composite(undefineUuid, writeSocketBinding);
        }
        dispatcher.execute(new DMRAction(composite), new SwitchCallback());
    }
}
