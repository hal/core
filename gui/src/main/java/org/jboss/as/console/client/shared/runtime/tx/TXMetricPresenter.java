package org.jboss.as.console.client.shared.runtime.tx;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Scheduler;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.LoggingCallback;
import org.jboss.as.console.client.plugins.RuntimeGroup;
import org.jboss.as.console.client.shared.runtime.Metric;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.tx.model.TransactionManager;
import org.jboss.as.console.client.widgets.forms.AddressBinding;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.RuntimeExtension;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.PropagatesChange;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 11/3/11
 */
public class TXMetricPresenter extends Presenter<TXMetricPresenter.MyView, TXMetricPresenter.MyProxy>
        implements TXMetricManagement {

    private DispatchAsync dispatcher;
    private AddressBinding addressBinding;
    private EntityAdapter<TransactionManager> entityAdapter;
    private RevealStrategy revealStrategy;

    @ProxyCodeSplit
    @NameToken(NameTokens.TXMetrics)
    @RuntimeExtension(name="Transactions", group=RuntimeGroup.METRICS, key="transactions")
    @AccessControl(
            resources = {
                    "/{selected.host}/{selected.server}/subsystem=transactions"
            }
    )
    @SearchIndex(keywords = {
            "transaction", "commit", "failure", "transaction-log"
    })
    public interface MyProxy extends Proxy<TXMetricPresenter>, Place {
    }

    public interface MyView extends TXMetricView  {
        void setPresenter(TXMetricManagement presenter);
        void setTxMetric(Metric txMetric);
        void setRollbackMetric(Metric rollbackMetric);
        void clearSamples();
    }

    @Inject
    public TXMetricPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            DispatchAsync dispatcher,
            ApplicationMetaData metaData, RevealStrategy revealStrategy) {
        super(eventBus, view, proxy);

        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;

        this.addressBinding = metaData.getBeanMetaData(TransactionManager.class).getAddress();
        this.entityAdapter = new EntityAdapter<TransactionManager>(TransactionManager.class, metaData);
    }


    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        Console.MODULES.getServerStore().addChangeHandler(new PropagatesChange.Handler() {
            @Override
            public void onChange(Class<?> source) {
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                           @Override
                           public void execute() {
                               if(isVisible()) refresh();
                           }
                       });
            }
        });
    }

    @Override
    protected void onReset() {
        super.onReset();
        refresh();
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInRuntimeParent(this);
    }

    @Override
    public void refresh() {

        getView().clearSamples();

        ModelNode operation = addressBinding.asResource(RuntimeBaseAddress.get());
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode result = dmrResponse.get();

                if(result.isFailure())
                {
                    Log.error("Failed to load TX metrics: "+ result.getFailureDescription());
                }
                else
                {

                    TransactionManager metrics = entityAdapter.fromDMR(result.get(RESULT));

                    getView().setTxMetric(new Metric(
                            metrics.getNumTransactions(),
                            metrics.getNumCommittedTransactions(),
                            metrics.getNumAbortedTransactions(),
                            metrics.getNumTimeoutTransactions()
                    ));

                    getView().setRollbackMetric(new Metric(
                            metrics.getNumApplicationRollback(),
                            metrics.getNumResourceRollback()
                    ));


                }

            }
        });
    }

}
