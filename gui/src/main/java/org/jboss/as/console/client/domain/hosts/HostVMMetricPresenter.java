package org.jboss.as.console.client.domain.hosts;

import static org.jboss.as.console.spi.OperationMode.Mode.DOMAIN;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Scheduler;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.Place;
import com.gwtplatform.mvp.client.proxy.Proxy;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.model.HostInformationStore;
import org.jboss.as.console.client.domain.model.LoggingCallback;
import org.jboss.as.console.client.domain.runtime.DomainRuntimePresenter;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.jvm.LoadJVMMetricsCmd;
import org.jboss.as.console.client.shared.jvm.model.CompositeVMMetric;
import org.jboss.as.console.client.shared.runtime.Metric;
import org.jboss.as.console.client.shared.runtime.RuntimeBaseAddress;
import org.jboss.as.console.client.shared.runtime.vm.VMMetricsManagement;
import org.jboss.as.console.client.shared.runtime.vm.VMView;
import org.jboss.as.console.client.shared.state.DomainEntityManager;
import org.jboss.as.console.client.shared.state.ServerSelectionChanged;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.spi.AccessControl;
import org.jboss.as.console.spi.OperationMode;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;

/**
 * @author Heiko Braun
 * @date 10/7/11
 */
public class HostVMMetricPresenter extends Presenter<VMView, HostVMMetricPresenter.MyProxy>
        implements VMMetricsManagement, ServerSelectionChanged.ChangeListener {


    private DispatchAsync dispatcher;
    private ApplicationMetaData metaData;
    private BeanFactory factory;

    private HostInformationStore hostInfoStore;
    private final DomainEntityManager domainManager;

    @ProxyCodeSplit
    @NameToken(NameTokens.HostVMMetricPresenter)
    @OperationMode(DOMAIN)
    @AccessControl(
            resources = {
                    "/{selected.host}/{selected.server}/core-service=platform-mbean/type=runtime",
                    "/{selected.host}/{selected.server}/core-service=platform-mbean/type=threading",
                    "/{selected.host}/{selected.server}/core-service=platform-mbean/type=memory",
                    "/{selected.host}/{selected.server}/core-service=platform-mbean/type=operating-system"
            }
    )
    public interface MyProxy extends Proxy<HostVMMetricPresenter>, Place {
    }

    public interface MyView extends VMView {
    }

    @Inject
    public HostVMMetricPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            DomainEntityManager domainManager,
            DispatchAsync dispatcher, BeanFactory factory,
            ApplicationMetaData metaData, HostInformationStore hostInfoStore
    ) {
        super(eventBus, view, proxy);

        this.domainManager = domainManager;
        this.dispatcher = dispatcher;
        this.factory = factory;
        this.metaData = metaData;
        this.hostInfoStore = hostInfoStore;
    }

    @Override
    public void onServerSelectionChanged(boolean isRunning) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                if(isVisible()) refresh();
            }
        });
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        getEventBus().addHandler(ServerSelectionChanged.TYPE, this);
    }

    @Override
    protected void onReset() {
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
        RevealContentEvent.fire(this, DomainRuntimePresenter.TYPE_MainContent, this);
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
