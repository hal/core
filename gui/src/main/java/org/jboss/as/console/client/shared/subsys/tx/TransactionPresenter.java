package org.jboss.as.console.client.shared.subsys.tx;

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
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.model.ModelAdapter;
import org.jboss.as.console.client.shared.subsys.Baseadress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.tx.model.TransactionManager;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.BeanMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.as.console.spi.SubsystemExtension;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;

import java.util.Map;

import static org.jboss.as.console.client.shared.subsys.tx.TransactionPresenter.JacorbState.*;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;


/**
 * @author Heiko Braun
 * @date 10/25/11
 */
public class TransactionPresenter extends Presenter<TransactionPresenter.MyView, TransactionPresenter.MyProxy> {

    static enum JacorbState {
        UNDEFINED("undefined"),
        DMR_ERROR(Console.CONSTANTS.tx_jacorb_state_dmr_error()),
        NOT_PRESENT(Console.CONSTANTS.tx_jacorb_state_not_present()),
        WRONG_VALUE(Console.CONSTANTS.tx_jacorb_state_wrong_value()),
        VALID("valid");

        private final String message;

        JacorbState(final String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }


    @ProxyCodeSplit
    @NameToken(NameTokens.TransactionPresenter)
    @SubsystemExtension(name = "Transactions", group = "Container", key = "transactions")
    @AccessControl(resources = {"{selected.profile}/subsystem=transactions"})
    @SearchIndex(keywords = {"transaction", "log-store"})
    public interface MyProxy extends Proxy<TransactionPresenter>, Place {
    }


    public interface MyView extends View {

        void setPresenter(TransactionPresenter presenter);

        void setTransactionManager(TransactionManager transactionManager);
    }


    private final PlaceManager placeManager;
    private DispatchAsync dispatcher;
    private RevealStrategy revealStrategy;
    private ApplicationMetaData metaData;
    private BeanMetaData beanMetaData;
    private EntityAdapter<TransactionManager> entityAdapter;
    private JacorbState jacorbState;

    @Inject
    public TransactionPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            PlaceManager placeManager, DispatchAsync dispatcher,
            RevealStrategy revealStrategy, ApplicationMetaData metaData) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;
        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
        this.metaData = metaData;
        this.jacorbState = UNDEFINED;

        this.beanMetaData = metaData.getBeanMetaData(TransactionManager.class);
        this.entityAdapter = new EntityAdapter<TransactionManager>(TransactionManager.class, metaData);
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void onReset() {
        super.onReset();
        loadModel();
        checkJacorb();
    }

    private void loadModel() {

        ModelNode operation = beanMetaData.getAddress().asResource(Baseadress.get());
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();
                TransactionManager transactionManager = entityAdapter.fromDMR(response.get(RESULT));
                getView().setTransactionManager(transactionManager);
            }
        });

    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInParent(this);
    }

    public void onSaveConfig(Map<String, Object> changeset) {
        ModelNode operation =
                entityAdapter.fromChangeset(
                        changeset,
                        beanMetaData.getAddress().asResource(Baseadress.get()
                        )
                );

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();

                boolean success = ModelAdapter.wasSuccess(response);
                if (success) { Console.info(Console.MESSAGES.modified("Transaction Manager")); } else {
                    Console.error(Console.MESSAGES.modificationFailed("Transaction Manager"),
                            response.getFailureDescription());
                }

                loadModel();
            }
        });
    }

    private void checkJacorb() {
        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(Baseadress.get().add("subsystem", "jacorb"));
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("transactions");

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                jacorbState = DMR_ERROR;
            }

            @Override
            public void onSuccess(final DMRResponse response) {
                ModelNode result = response.get();
                if (result.isFailure()) {
                    jacorbState = NOT_PRESENT;
                } else if (!result.get("result").asString().equals("on")) {
                    jacorbState = WRONG_VALUE;
                } else {
                    jacorbState = VALID;
                }
            }
        });
    }

    public JacorbState getJacorbState() {
        return jacorbState;
    }
}
