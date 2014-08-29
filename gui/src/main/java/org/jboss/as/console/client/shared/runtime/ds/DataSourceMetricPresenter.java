package org.jboss.as.console.client.shared.runtime.ds;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Scheduler;
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
import org.jboss.as.console.client.domain.model.LoggingCallback;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.model.ResponseWrapper;
import org.jboss.as.console.client.shared.runtime.Metric;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.subsys.RevealStrategy;
import org.jboss.as.console.client.shared.subsys.jca.ConnectionWindow;
import org.jboss.as.console.client.shared.subsys.jca.model.DataSource;
import org.jboss.as.console.client.v3.stores.domain.ServerStore;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.client.widgets.forms.EntityAdapter;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.dmr.client.dispatch.impl.DMRAction;
import org.jboss.dmr.client.dispatch.impl.DMRResponse;
import org.jboss.gwt.circuit.PropagatesChange;

import java.util.Collections;
import java.util.List;

import static org.jboss.as.console.client.shared.subsys.jca.VerifyConnectionOp.VerifyResult;
import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 12/19/11
 */
public class DataSourceMetricPresenter extends Presenter<DataSourceMetricPresenter.MyView,
        DataSourceMetricPresenter.MyProxy>        {

    private final PlaceManager placeManager;
    private DispatchAsync dispatcher;
    private RevealStrategy revealStrategy;
    private DataSource selectedDS;
    private BeanFactory factory;
    private EntityAdapter<DataSource> dataSourceAdapter;

    private LoadDataSourceCmd loadDSCmd;
    private DataSource selectedXA;
    private final ServerStore serverStore;

    @ProxyCodeSplit
    @NameToken(NameTokens.DataSourceMetricPresenter)
    @AccessControl(
            resources = {
                    "/{selected.host}/{selected.server}/subsystem=datasources/data-source=*",
                    "/{selected.host}/{selected.server}/subsystem=datasources/xa-data-source=*"
            }
    )
    @SearchIndex(keywords = {
            "data-source", "pool", "pool-usage"
    })
    public interface MyProxy extends Proxy<DataSourceMetricPresenter>, Place {
    }

    public interface MyView extends View {
        void setPresenter(DataSourceMetricPresenter presenter);
        void clearSamples();
        void setDatasources(List<DataSource> datasources, boolean isXA);
        void setDSPoolMetric(Metric poolMetric, boolean isXA);
        void setDSCacheMetric(Metric metric, boolean isXA);
    }

    @Inject
    public DataSourceMetricPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            PlaceManager placeManager,  DispatchAsync dispatcher,
            ApplicationMetaData metaData, RevealStrategy revealStrategy,
            ServerStore serverStore, BeanFactory factory) {
        super(eventBus, view, proxy);

        this.placeManager = placeManager;

        this.dispatcher = dispatcher;
        this.revealStrategy = revealStrategy;
        this.serverStore = serverStore;
        this.factory = factory;

        this.loadDSCmd = new LoadDataSourceCmd(dispatcher, metaData);

    }

    public void refreshDatasources() {

        getView().clearSamples();
        getView().setDatasources(Collections.EMPTY_LIST, true);
        getView().setDatasources(Collections.EMPTY_LIST, false);

        // Regular Datasources
        loadDSCmd.execute(new LoggingCallback<List<DataSource>>() {

            @Override
            public void onFailure(Throwable caught) {
                Log.error(caught.getMessage());
            }

            @Override
            public void onSuccess(List<DataSource> result) {
                getView().setDatasources(result, false);
            }
        }, false);

        // XA Data Sources
        loadDSCmd.execute(new LoggingCallback<List<DataSource>>() {

            @Override
            public void onFailure(Throwable caught) {
                Log.error(caught.getMessage());
            }

            @Override
            public void onSuccess(List<DataSource> result) {
                getView().setDatasources(result, true);
            }
        }, true);
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        serverStore.addChangeHandler(new PropagatesChange.Handler() {
            @Override
            public void onChange(Class<?> source) {
                getView().clearSamples();

                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        if(isVisible()) refreshDatasources();
                    }
                });
            }
        });
    }


    @Override
    protected void onReset() {
        super.onReset();
        refreshDatasources();
    }

    @Override
    protected void revealInParent() {
        revealStrategy.revealInRuntimeParent(this);
    }

    public void setSelectedDS(DataSource currentSelection, boolean xa) {

        if(!currentSelection.isEnabled())
        {
            Console.error(Console.MESSAGES.subsys_jca_err_ds_notEnabled(currentSelection.getName()));
            getView().clearSamples();
            return;
        }

        if(xa) {
            this.selectedXA = currentSelection;
            if(selectedXA!=null)
                loadMetrics(true);
        }
        else {
            this.selectedDS = currentSelection;
            if(selectedDS!=null)
                loadMetrics(false);
        }
    }

    private void loadMetrics(boolean isXA) {
        loadDSPoolMetrics(isXA);
        loadDSCacheMetrics(isXA);
    }

    private void loadDSPoolMetrics(final boolean isXA) {

        DataSource target = isXA ? selectedXA : selectedDS;
        if(null==target)
            throw new RuntimeException("DataSource selection is null!");

        getView().clearSamples();

        String subresource = isXA ? "xa-data-source": "data-source";
        String name = target.getName();

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(RuntimeBaseAddress.get());
        operation.get(ADDRESS).add("subsystem", "datasources");
        operation.get(ADDRESS).add(subresource, name);
        operation.get(ADDRESS).add("statistics", "pool");

        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();

                if(response.isFailure())
                {
                    Console.error(Console.MESSAGES.failed("Datasource Metrics"), response.getFailureDescription());
                }
                else
                {
                    ModelNode result = response.get(RESULT).asObject();

                    long avail = result.get("AvailableCount").asLong();
                    long active = result.get("ActiveCount").asLong();
                    long max = result.get("MaxUsedCount").asLong();

                    Metric poolMetric = new Metric(
                            avail,active,max
                    );

                    getView().setDSPoolMetric(poolMetric, isXA);
                }
            }
        });
    }

    private void loadDSCacheMetrics(final boolean isXA) {

        DataSource target = isXA ? selectedXA : selectedDS;
        if(null==target)
            throw new RuntimeException("DataSource selection is null!");

        getView().clearSamples();

        String subresource = isXA ? "xa-data-source": "data-source";
        String name = target.getName();

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(RuntimeBaseAddress.get());
        operation.get(ADDRESS).add("subsystem", "datasources");
        operation.get(ADDRESS).add(subresource, name);
        operation.get(ADDRESS).add("statistics", "jdbc");

        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(INCLUDE_RUNTIME).set(true);

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();

                if(response.isFailure())
                {
                    Console.error(Console.MESSAGES.failed("Datasource Metrics"), response.getFailureDescription());
                }
                else
                {
                    ModelNode result = response.get(RESULT).asObject();

                    long size = result.get("PreparedStatementCacheAccessCount").asLong();
                    long hit = result.get("PreparedStatementCacheHitCount").asLong();
                    long miss = result.get("PreparedStatementCacheMissCount").asLong();

                    Metric metric = new Metric(
                            size,hit,miss
                    );

                    getView().setDSCacheMetric(metric, isXA);
                }
            }
        });
    }

    public void verifyConnection(final String dsName, boolean isXA) {
        String subresource = isXA ? "xa-data-source": "data-source";

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(RuntimeBaseAddress.get());
        operation.get(ADDRESS).add("subsystem", "datasources");
        operation.get(ADDRESS).add(subresource, dsName);
        operation.get(OP).set("test-connection-in-pool");

        dispatcher.execute(new DMRAction(operation), new AsyncCallback<DMRResponse>() {
            @Override
            public void onFailure(final Throwable caught) {
                show(new VerifyResult(caught));
            }

            @Override
            public void onSuccess(DMRResponse response) {
                VerifyResult verifyResult;
                ModelNode result = response.get();
                ResponseWrapper<Boolean> wrapped = new ResponseWrapper<Boolean>(!result.isFailure(), result);

                if (wrapped.getUnderlying()) {
                    verifyResult = new VerifyResult(true,
                            Console.MESSAGES.verify_datasource_successful_message(dsName));
                } else {
                    verifyResult = new VerifyResult(false, Console.MESSAGES.verify_datasource_failed_message(dsName),
                            result.getFailureDescription());
                }
                show(verifyResult);
            }

            private void show(VerifyResult result) {
                new ConnectionWindow(dsName, result).show();
            }
        });
    }

    public void flush(final String dsName, boolean isXA) {


        String subresource = isXA ? "xa-data-source": "data-source";

        ModelNode operation = new ModelNode();
        operation.get(ADDRESS).set(RuntimeBaseAddress.get());
        operation.get(ADDRESS).add("subsystem", "datasources");
        operation.get(ADDRESS).add(subresource, dsName);
        operation.get(OP).set("flush-all-connection-in-pool");

        dispatcher.execute(new DMRAction(operation), new LoggingCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                ModelNode response = dmrResponse.get();

                if(response.isFailure())
                {
                    Console.error(Console.MESSAGES.failed("Flush connections error for " + dsName), response.getFailureDescription());
                }
                else
                {
                    Console.info(Console.MESSAGES.successful("Flush connections for " + dsName));
                }
            }
        });

    }
}
