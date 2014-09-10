package org.jboss.as.console.client.shared.subsys.ejb3;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.widgets.tabs.DefaultTabLayoutPanel;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 11/28/11
 */
public class EESubsystemView extends SuspendableViewImpl implements EEPresenter.MyView {

    private EEPresenter presenter;
    private EEModulesView moduleView;
    private EEGlobalAttributesView attributesView;
    private EEServicesView servicesView;
    private BindingsView bindingsView;

    @Override
    public Widget createWidget() {

        moduleView = new EEModulesView(presenter);
        attributesView = new EEGlobalAttributesView(presenter);
        bindingsView = new BindingsView(presenter);
        servicesView = new EEServicesView(presenter);

        DefaultTabLayoutPanel tabLayoutpanel = new DefaultTabLayoutPanel(40, Style.Unit.PX);
        tabLayoutpanel.addStyleName("default-tabpanel");

        OneToOneLayout layout = new OneToOneLayout()
                .setPlain(true)
                .setHeadline("Global EE Settings")
                .setDescription(Console.CONSTANTS.subsys_ee_desc())
                .addDetail("Deployments", attributesView.asWidget())
                .addDetail("Default Bindings", bindingsView.asWidget())
                .addDetail("Global Modules", moduleView.asWidget());

        tabLayoutpanel.add(layout.build(), "EE Subsystem", true);
        tabLayoutpanel.add(servicesView.asWidget(), "Services", true);

        tabLayoutpanel.selectTab(0);

        return tabLayoutpanel;
    }

    @Override
    public void setPresenter(EEPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void updateFrom(ModelNode data) {
        attributesView.setData(data);
    }

    @Override
    public void setModules(List<ModelNode> modules) {
        moduleView.setModules(modules);
    }

    @Override
    public void setContextServices(List<Property> services) {
        servicesView.setContextServices(services);
    }

    @Override
    public void setThreadFactories(List<Property> data) {
        servicesView.setThreadFactories(data);
    }

    @Override
    public void setExecutor(List<Property> data) {
        servicesView.setExecutor(data);
    }

    @Override
    public void setScheduledExecutor(List<Property> data) {
        servicesView.setScheduledExecutor(data);
    }

    public void setBindings(ModelNode data) {
        bindingsView.setData(data);
    }
}
