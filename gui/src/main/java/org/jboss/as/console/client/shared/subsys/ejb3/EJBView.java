package org.jboss.as.console.client.shared.subsys.ejb3;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 11/05/15
 */
public class EJBView extends SuspendableViewImpl implements EJB3Presenter.MyView {

    private EJB3Presenter presenter;
    private BeanPoolView poolView;
    private ServicesView servicesView;
    private SubsystemView subsystemView;
    private StateView stateView;

    @Override
    public void setPresenter(EJB3Presenter presenter) {

        this.presenter = presenter;
    }

    @Override
    public void updateContainer(ModelNode container) {
        subsystemView.updateContainerView(container);
    }

    @Override
    public void updateBeanPools(List<Property> pools) {
        poolView.setData(pools);
    }

    @Override
    public void updateThreadPools(List<Property> pools) {
        subsystemView.updateThreadPools(pools);
    }

    @Override
    public void updateRemotingProfiles(List<Property> properties) {
        subsystemView.updateRemotingProfiles(properties);
    }

    @Override
    public void updateTimerSvc(ModelNode service) {
        servicesView.updateTimerSvc(service);
    }

    @Override
    public void updateRemoteSvc(ModelNode service) {
        servicesView.updateRemoteSvc(service);
    }

    @Override
    public void updateIIOPSvc(ModelNode service) {
        servicesView.updateIIOPSvc(service);
    }

    @Override
    public void updateAsyncSvc(ModelNode modelNode) {
        servicesView.updateAsyncSvc(modelNode);
    }

    @Override
    public void updateCaches(List<Property> properties) {
        stateView.updateCaches(properties);
    }

    @Override
    public void updatePassivationStores(List<Property> properties) {
        stateView.updatePassivationStore(properties);
    }

    @Override
    public void updateFilePassivationStore(List<Property> properties) {
        stateView.updateFilePassivationStore(properties);
    }

    @Override
    public void updateClusterPassivationStore(List<Property> properties) {
        stateView.updateClusterPassivationStore(properties);
    }

    @Override
    public Widget createWidget() {

        subsystemView = new SubsystemView(presenter);
        poolView = new BeanPoolView(presenter);
        servicesView = new ServicesView(presenter);
        stateView = new StateView(presenter);

        DefaultTabLayoutPanel tabLayoutpanel = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabLayoutpanel.addStyleName("default-tabpanel");

        tabLayoutpanel.add(subsystemView.asWidget(), "Container", true);
        tabLayoutpanel.add(poolView.asWidget(), "Bean Pools", true);
        tabLayoutpanel.add(stateView.asWidget(), "State Management", true);
        tabLayoutpanel.add(servicesView.asWidget(), "Services", true);

        tabLayoutpanel.selectTab(0);

        return tabLayoutpanel;
    }
}
