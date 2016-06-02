/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.shared.runtime.tx;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Scheduler;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.CircuitPresenter;
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
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 11/3/11
 */
public class TXMetricPresenter extends CircuitPresenter<TXMetricPresenter.MyView, TXMetricPresenter.MyProxy>
        implements TXMetricManagement {

    @ProxyCodeSplit
    @NameToken(NameTokens.TXMetrics)
    @RuntimeExtension(name="Transactions", group=RuntimeGroup.METRICS, key="transactions")
    @AccessControl(resources = {"/{implicit.host}/{selected.server}/subsystem=transactions"})
    @SearchIndex(keywords = {"transaction", "commit", "failure", "transaction-log"})
    public interface MyProxy extends Proxy<TXMetricPresenter>, Place {}


    public interface MyView extends TXMetricView  {
        void setPresenter(TXMetricManagement presenter);
        void setTxMetric(Metric txMetric);
        void setGeneralMetric(ModelNode txModel);
        void setRollbackMetric(Metric rollbackMetric);
        void clearSamples();
    }


    private DispatchAsync dispatcher;
    private AddressBinding addressBinding;
    private EntityAdapter<TransactionManager> entityAdapter;
    private RevealStrategy revealStrategy;


    @Inject
    public TXMetricPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            DispatchAsync dispatcher, Dispatcher circuit,
            ApplicationMetaData metaData, RevealStrategy revealStrategy) {
        super(eventBus, view, proxy, circuit);

        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;

        this.addressBinding = metaData.getBeanMetaData(TransactionManager.class).getAddress();
        this.entityAdapter = new EntityAdapter<TransactionManager>(TransactionManager.class, metaData);
    }


    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        addChangeHandler(Console.MODULES.getServerStore());
    }

    @Override
    protected void onAction(Action action) {
        Scheduler.get().scheduleDeferred(() -> refresh());
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

                if (result.isFailure()) {
                    Log.error("Failed to load TX metrics: "+ result.getFailureDescription());
                } else {

                    ModelNode txAttributesNode = result.get(RESULT);
                    TransactionManager metrics = entityAdapter.fromDMR(txAttributesNode);

                    getView().setTxMetric(new Metric(
                            metrics.getNumTransactions(),
                            metrics.getNumCommittedTransactions(),
                            metrics.getNumAbortedTransactions(),
                            metrics.getNumTimeoutTransactions(),
                            metrics.getNumHeuristics()
                    ));
                    
                    getView().setGeneralMetric(txAttributesNode);

                    getView().setRollbackMetric(new Metric(
                            metrics.getNumApplicationRollback() + metrics.getNumResourceRollback(),
                            metrics.getNumApplicationRollback(),
                            metrics.getNumResourceRollback()
                    ));


                }

            }
        });
    }

}
