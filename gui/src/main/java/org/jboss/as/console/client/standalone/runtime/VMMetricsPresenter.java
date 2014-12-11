package org.jboss.as.console.client.standalone.runtime;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.annotations.CustomProvider;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.client.proxy.RevealContentEvent;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.CircuitPresenter;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.RequiredResourcesProvider;
import org.jboss.as.console.client.shared.BeanFactory;
import org.jboss.as.console.client.shared.jvm.LoadJVMMetricsCmd;
import org.jboss.as.console.client.shared.jvm.model.CompositeVMMetric;
import org.jboss.as.console.client.shared.runtime.Metric;
import org.jboss.as.console.client.shared.runtime.vm.VMMetricsManagement;
import org.jboss.as.console.client.shared.runtime.vm.VMView;
import org.jboss.as.console.client.widgets.forms.ApplicationMetaData;
import org.jboss.as.console.spi.OperationMode;
import org.jboss.as.console.spi.RequiredResources;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.dispatch.DispatchAsync;
import org.jboss.gwt.circuit.Action;
import org.jboss.gwt.circuit.Dispatcher;

import static org.jboss.as.console.spi.OperationMode.Mode.STANDALONE;

/**
 * @author Heiko Braun
 * @date 9/28/11
 */
public class VMMetricsPresenter
        extends CircuitPresenter<VMView, VMMetricsPresenter.MyProxy>
        implements VMMetricsManagement {

    @ProxyCodeSplit
    @OperationMode(STANDALONE)
    @NameToken(NameTokens.VirtualMachine)
    @CustomProvider(RequiredResourcesProvider.class)
    @RequiredResources(
            resources = {
                    "/{selected.host}/{selected.server}/core-service=platform-mbean/type=runtime",
                    "/{selected.host}/{selected.server}/core-service=platform-mbean/type=threading",
                    "/{selected.host}/{selected.server}/core-service=platform-mbean/type=memory",
                    "/{selected.host}/{selected.server}/core-service=platform-mbean/type=operating-system"
            }
    )
    public interface MyProxy extends ProxyPlace<VMMetricsPresenter> {}

    public interface MyView extends VMView {}


    private ApplicationMetaData metaData;
    private LoadJVMMetricsCmd loadMetricCmd;

    @Inject
    public VMMetricsPresenter(
            EventBus eventBus, MyView view, MyProxy proxy,
            DispatchAsync dispatcher, BeanFactory factory, ApplicationMetaData propertyMetaData, Dispatcher circuit) {
        super(eventBus, view, proxy, circuit);

        this.metaData = propertyMetaData;
        this.loadMetricCmd = new LoadJVMMetricsCmd(dispatcher, factory, new ModelNode(), metaData);
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
        addChangeHandler(Console.MODULES.getServerStore());
    }

    @Override
    protected void onAction(Action action) {
        refresh();
    }

    @Override
    protected void onHide() {
        super.onHide();
    }


    @Override
    protected void onReset() {
        super.onReset();
        loadVMStatus();

    }

    @Override
    public void refresh() {
         loadVMStatus();
    }

    public void loadVMStatus() {

        getView().clearSamples();

        loadMetricCmd.execute(new AsyncCallback<CompositeVMMetric>() {

            @Override
            public void onFailure(Throwable caught) {
                Console.warning(caught.getMessage());
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

                getView().setThreads(new Metric(
                        result.getThreads().getCount(),
                        result.getThreads().getDaemonCount()
                ));

                getView().setOSMetric(result.getOs());
                getView().setRuntimeMetric(result.getRuntime());
            }
        });

    }

    @Override
    protected void revealInParent() {
        RevealContentEvent.fire(this, StandaloneRuntimePresenter.TYPE_MainContent, this);
    }
}
