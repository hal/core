package org.jboss.as.console.client.standalone.runtime;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.plugins.RuntimeExtensionMetaData;
import org.jboss.as.console.client.plugins.RuntimeExtensionRegistry;
import org.jboss.as.console.client.plugins.RuntimeGroup;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.widgets.nav.Predicate;
import org.jboss.as.console.client.widgets.tree.GroupItem;
import org.jboss.ballroom.client.layout.LHSHighlightEvent;
import org.jboss.ballroom.client.layout.LHSNavTree;
import org.jboss.ballroom.client.layout.LHSNavTreeItem;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Heiko Braun
 * @date 11/2/11
 */
public class StandaloneRuntimeNavigation implements LHSHighlightEvent.NavItemSelectionHandler {

    private VerticalPanel stack;
    private VerticalPanel layout;

    private List<Predicate> metricPredicates = new ArrayList<Predicate>();
    private List<Predicate> runtimePredicates = new ArrayList<Predicate>();

    private ScrollPanel scroll;
    private LHSNavTree serverTree;
    private LHSNavTree runtimeTree;
    private LHSHighlightEvent highlightEvent;

    public Widget asWidget()
    {
        layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");

        stack = new VerticalPanel();
        stack.setStyleName("fill-layout-width");
        stack.getElement().getStyle().setBackgroundColor("#ffffff");

        // ----------------------------------------------------


        serverTree = new LHSNavTree("server-runtime");
        serverTree.getElement().setAttribute("aria-label", "Runtime Tasks");

        // ----------------------------------------------------

        LHSNavTreeItem server = new LHSNavTreeItem("Overview", NameTokens.StandaloneServerPresenter);
        serverTree.addItem(server);


        // -------------

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
                Log.warn("Invalid runtime group for extension: "+ext.getGroup());
            }
        }

        // ---

        runtimeTree = new LHSNavTree("subsystem-runtime");

        serverTree.expandTopLevel();

        HTML serverTitle = new HTML("Server");
        serverTitle.setStyleName("server-picker-section-header");

        stack.add(serverTitle);
        stack.add(serverTree);

        HTML statusTitle = new HTML("System Status");
        statusTitle.setStyleName("server-picker-section-header");

        stack.add(statusTitle);
        stack.add(runtimeTree);

        layout.add(stack);

        scroll = new ScrollPanel(layout);

        return scroll;
    }



    public void setSubsystems(List<SubsystemRecord> subsystems) {

        runtimeTree.removeItems();
        runtimeTree.setVisible(true);

        if(subsystems.isEmpty()) return;

        final GroupItem platformGroup = new GroupItem("Platform");
        platformGroup.addItem(new LHSNavTreeItem("JVM", NameTokens.VirtualMachine));
        platformGroup.addItem(new LHSNavTreeItem("Environment", NameTokens.EnvironmentPresenter));
        platformGroup.addItem(new LHSNavTreeItem("Log Viewer", NameTokens.LogFiles));

        runtimeTree.addItem(platformGroup);

        final GroupItem subsystemGroup = new GroupItem("Subsystems");
        // match subsystems
        for(SubsystemRecord subsys : subsystems) {
            for(Predicate predicate : metricPredicates) {
                if(predicate.matches(subsys.getKey()))
                    subsystemGroup.addItem(predicate.getNavItem());
            }

            for(Predicate predicate : runtimePredicates) {
                if(predicate.matches(subsys.getKey()))
                    runtimeTree.addItem(predicate.getNavItem());
            }
        }

        runtimeTree.addItem(subsystemGroup);
        subsystemGroup.setState(true);
        platformGroup.setState(true);

        // empty runtime operations
        runtimeTree.setVisible(runtimeTree.getItemCount()>0);
        serverTree.expandTopLevel();

        if(highlightEvent!=null)
        {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    runtimeTree.onSelectedNavTree(highlightEvent);
                }
            });
        }
    }

    @Override
    public void onSelectedNavTree(LHSHighlightEvent event) {
        this.highlightEvent = event;
    }
}
