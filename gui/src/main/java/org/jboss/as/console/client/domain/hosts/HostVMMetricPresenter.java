package org.jboss.as.console.client.domain.hosts;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Scheduler;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import org.jboss.as.console.client.core.CircuitPresenter;
import org.jboss.as.console.client.core.MainLayoutPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.LoggingCallback;
import org.jboss.as.console.client.domain.runtime.DomainRuntimePresenter;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.jvm.LoadJVMMetricsCmd;
import org.jboss.as.console.client.shared.jvm.model.CompositeVMMetric;
import org.jboss.as.console.client.shared.runtime.Metric;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.runtime.vm.VMMetricsManagement;
import org.jboss.as.console.client.shared.runtime.vm.VMView;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.OperationMode;
import org.jboss.as.console.spi.SearchIndex;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import static org.jboss.as.console.spi.OperationMode.Mode.DOMAIN;

/**
 * @author Heiko Braun
 * @date 10/7/11
 */
public class HostVMMetricPresenter extends CircuitPresenter<VMView, HostVMMetricPresenter.MyProxy>
        implements VMMetricsManagement {

    @ProxyCodeSplit
    @NameToken(NameTokens.HostVMMetricPresenter)
    @OperationMode(DOMAIN)
    @AccessControl(resources = {
            "/{selected.host}/{selected.server}/core-service=platform-mbean/type=runtime",
            "/{selected.host}/{selected.server}/core-service=platform-mbean/type=threading",
            "/{selected.host}/{selected.server}/core-service=platform-mbean/type=memory",
            "/{selected.host}/{selected.server}/core-service=platform-mbean/type=operating-system"})
    @SearchIndex(keywords = {
            "jvm", "memory-usage", "threads"})
    public interface MyProxy extends Proxy<HostVMMetricPresenter>, Place {}


    public interface MyView extends VMView {}


    private DispatchAsync dispatcher;
    private ApplicationMetaData metaData;
    private BeanFactory factory;
    private final HostStore hostStore;

    @Inject
    public HostVMMetricPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            Dispatcher circuit, HostStore hostStore,
            DispatchAsync dispatcher, BeanFactory factory,
            ApplicationMetaData metaData) {
        super(eventBus, view, proxy, circuit);

        this.hostStore = hostStore;
        this.dispatcher = dispatcher;
        this.factory = factory;
        this.metaData = metaData;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        addChangeHandler(hostStore);
    }

    @Override
    protected void onAction(Action action) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                refresh();
            }
        });
    }

    @Override
    protected void onReset() {
        super.onReset();
        refresh();
    }

    @Override
    public void refresh() {

        getView().clearSamples();
        loadVMStatus();
    }

    private LoadJVMMetricsCmd createLoadMetricCmd() {

        ModelNode address = RuntimeBaseAddress.get();
        return new LoadJVMMetricsCmd(
                dispatcher, factory,
                address,
                metaData
        );
    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, MainLayoutPresenter.TYPE_Popup, this);
    }

    public void loadVMStatus() {

        getView().clearSamples();

        createLoadMetricCmd().execute(new LoggingCallback<CompositeVMMetric>() {


            @Override
            public void onFailure(Throwable caught) {
                Log.error(caught.getMessage());
            }

            @Override
            public void onSuccess(CompositeVMMetric result) {
                getView().setHeap(new Metric(
                        result.getHeap().getMax(),
                        result.getHeap().getUsed(),
                        result.getHeap().getCommitted(),
                        result.getHeap().getInit()
                ));

                getView().setNonHeap(new Metric(
                        result.getNonHeap().getMax(),
                        result.getNonHeap().getUsed(),
                        result.getNonHeap().getCommitted(),
                        result.getNonHeap().getInit()

                ));
                getView().setOSMetric(result.getOs());
                getView().setRuntimeMetric(result.getRuntime());
                getView().setThreads(
                        new Metric(
                                result.getThreads().getCount(),
                                result.getThreads().getDaemonCount()
                        )
                );
            }
        });
    }
}
