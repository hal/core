package org.jboss.as.console.client.domain.runtime;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.domain.hosts.ServerPicker;
import org.jboss.as.console.client.plugins.RuntimeExtensionMetaData;
import org.jboss.as.console.client.plugins.RuntimeExtensionRegistry;
import org.jboss.as.console.client.plugins.RuntimeGroup;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.widgets.nav.Predicate;
import org.jboss.as.console.client.widgets.tree.GroupItem;
import org.jboss.ballroom.client.layout.LHSNavTree;
import org.jboss.ballroom.client.layout.LHSNavTreeItem;
import org.jboss.ballroom.client.layout.LHSTreeSection;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Heiko Braun
 * @date 11/2/11
 */
class DomainRuntimeNavigation {

    private VerticalPanel stack;
    private VerticalPanel layout;

    private ServerPicker serverPicker;

    private List<Predicate> metricPredicates = new ArrayList<Predicate>();
    private List<Predicate> runtimePredicates = new ArrayList<Predicate>();

    private ScrollPanel scroll;
    private LHSNavTree navigation;
    private LHSNavTree metrics;
    //private LHSTreeSection metricLeaf;
    private LHSTreeSection runtimeLeaf;

    public Widget asWidget()
    {
        layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");

        stack = new VerticalPanel();
        stack.setStyleName("fill-layout-width");


        // ----------------------------------------------------

        LHSNavTree domainNavigation = new LHSNavTree("domain");
        domainNavigation.getElement().setAttribute("aria-label", "Domain Tasks");

        LHSTreeSection domainLeaf = new LHSTreeSection("Domain", true);
        domainLeaf.addItem(new LHSNavTreeItem("Overview", NameTokens.Topology));
        domainLeaf.addItem(new LHSNavTreeItem("Manage Deployments", NameTokens.DeploymentsPresenter));

        domainNavigation.addItem(domainLeaf);
        domainNavigation.expandTopLevel();

        stack.add(domainNavigation);

        // ----------------------------------------------------

        serverPicker = new ServerPicker();
        stack.add(serverPicker.asWidget());

        // ----------------------------------------------------

        navigation = new LHSNavTree("domain-runtime");
        navigation.getElement().setAttribute("aria-label", "Runtime Tasks");

        metrics = new LHSNavTree("metrics");
        metrics.addStyleName("server-picker-stack");
        metrics.getElement().setAttribute("style", "padding: 5px;");
        metrics.getElement().setAttribute("aria-label", "Runtime Monitoring");

        // -------------

        //metricLeaf = new LHSTreeSection("Server Status");
        //metrics.addItem(metricLeaf);

        LHSNavTreeItem datasources = new LHSNavTreeItem("Datasources", NameTokens.DataSourceMetricPresenter);
        LHSNavTreeItem jmsQueues = new LHSNavTreeItem("JMS Destinations", NameTokens.JmsMetricPresenter);
        LHSNavTreeItem web = new LHSNavTreeItem("Web", NameTokens.WebMetricPresenter);
        LHSNavTreeItem jpa = new LHSNavTreeItem("JPA", NameTokens.JPAMetricPresenter);
        LHSNavTreeItem ws = new LHSNavTreeItem("Webservices", NameTokens.WebServiceRuntimePresenter);
        LHSNavTreeItem naming = new LHSNavTreeItem("JNDI View", NameTokens.JndiPresenter);

        metricPredicates.add(new Predicate("datasources", datasources));
        metricPredicates.add(new Predicate("messaging", jmsQueues));
        metricPredicates.add(new Predicate("web", web));
        metricPredicates.add(new Predicate("jpa", jpa));
        metricPredicates.add(new Predicate("webservices", ws));
        metricPredicates.add(new Predicate("naming", naming));


        // Extension based additions
        RuntimeExtensionRegistry registry = Console.getRuntimeLHSItemExtensionRegistry();
        List<RuntimeExtensionMetaData> menuExtensions = registry.getExtensions();
        for (RuntimeExtensionMetaData ext : menuExtensions) {

            if(RuntimeGroup.METRICS.equals(ext.getGroup()))
            {
                metricPredicates.add(
                        new Predicate(
                                ext.getKey(), new LHSNavTreeItem(ext.getName(), ext.getToken())
                        )
                );
            }
            else if(RuntimeGroup.OPERATiONS.equals(ext.getGroup()))
            {
                runtimePredicates.add(
                        new Predicate(
                                ext.getKey(), new LHSNavTreeItem(ext.getName(), ext.getToken())
                        )
                );
            }
            else
            {
                Log.warn("Invalid runtime group for extension: " + ext.getGroup());
            }
        }

        // ---

        runtimeLeaf = new LHSTreeSection("Runtime Operations");
        navigation.addItem(runtimeLeaf);

        LHSNavTreeItem osgi = new LHSNavTreeItem("OSGi", NameTokens.OSGiRuntimePresenter);
        runtimePredicates.add(new Predicate("osgi", osgi));

        // ----------------------------------------------------

        navigation.expandTopLevel();
        metrics.expandTopLevel();

        stack.add(navigation);
        stack.add(metrics);

        layout.add(stack);

        scroll = new ScrollPanel(layout);

        return scroll;
    }

    public void setSubsystems(List<SubsystemRecord> subsystems) {

        metrics.removeItems();
        runtimeLeaf.removeItems();

        metrics.setVisible(false);
        runtimeLeaf.setVisible(false);

        if(subsystems.isEmpty()) return;

        metrics.setVisible(true);
        runtimeLeaf.setVisible(true);

        final GroupItem platformGroup = new GroupItem("Platform");

        platformGroup.addItem(new LHSNavTreeItem("JVM", NameTokens.HostVMMetricPresenter));
        platformGroup.addItem(new LHSNavTreeItem("Environment", NameTokens.EnvironmentPresenter));

        metrics.addItem(platformGroup);
        //platformGroup.setState(true);

        final GroupItem subsystemGroup = new GroupItem("Subsystems");
        // match subsystems
        for(SubsystemRecord subsys : subsystems)
        {

            for(Predicate predicate : metricPredicates)
            {
                if(predicate.matches(subsys.getKey()))
                    subsystemGroup.addItem(predicate.getNavItem());
            }

            for(Predicate predicate : runtimePredicates)
            {
                if(predicate.matches(subsys.getKey()))
                    runtimeLeaf.addItem(predicate.getNavItem());
            }
        }

        metrics.addItem(subsystemGroup);
        subsystemGroup.setState(true);
        platformGroup.setState(true);

        // empty runtime operations
        runtimeLeaf.setVisible(runtimeLeaf.getChildCount()>0);

        navigation.expandTopLevel();

    }

    public void resetHostSelection() {
        serverPicker.resetHostSelection();
    }

    public void setTopology(String selectedHost, HostStore.Topology topology) {
        serverPicker.setTopology(selectedHost, topology);
    }
}
