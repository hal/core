package org.jboss.as.console.client.domain.runtime;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.gwtplatform.mvp.client.ViewImpl;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.message.Message;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.plugins.RuntimeExtensionMetaData;
import org.jboss.as.console.client.plugins.RuntimeExtensionRegistry;
import org.jboss.as.console.client.plugins.RuntimeGroup;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.v3.stores.domain.actions.FilterType;
import org.jboss.as.console.client.v3.stores.domain.actions.SelectServer;
import org.jboss.as.console.client.widgets.nav.v3.ContextualCommand;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.as.console.client.widgets.nav.v3.NavigationColumn;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Heiko Braun
 */
public class DomainRuntimeView extends ViewImpl implements DomainRuntimePresenter.MyView {

    private LayoutPanel contentCanvas;
    private NavigationColumn<Server> serverColumn;
    private DomainRuntimePresenter presenter;
    private NavigationColumn<RuntimeLink> subsystemColumn;

    private List<Predicate> metricPredicates = new ArrayList<Predicate>();
    private List<Predicate> runtimePredicates = new ArrayList<Predicate>();
    private List<RuntimeLink> platformLinks = new ArrayList<RuntimeLink>();

    interface ServerTemplate extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}&nbsp;<span style='font-size:8px'>({2})</span></div>")
        SafeHtml item(String cssClass, String server, String host);
    }

    interface SubsystemTemplate extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</span></div>")
        SafeHtml item(String cssClass, String server);
    }


    private static final ServerTemplate SERVER_TEMPLATE = GWT.create(ServerTemplate.class);

    private static final SubsystemTemplate SUBSYSTEM_TEMPLATE = GWT.create(SubsystemTemplate.class);

    @Inject
    public DomainRuntimeView() {
        super();
        contentCanvas = new LayoutPanel();

        RuntimeLink datasources = new RuntimeLink("Datasources", NameTokens.DataSourceMetricPresenter);
        RuntimeLink jmsQueues = new RuntimeLink("JMS Destinations", NameTokens.JmsMetricPresenter);
        RuntimeLink web = new RuntimeLink("Web", NameTokens.WebMetricPresenter);
        RuntimeLink jpa = new RuntimeLink("JPA", NameTokens.JPAMetricPresenter);
        RuntimeLink ws = new RuntimeLink("Webservices", NameTokens.WebServiceRuntimePresenter);
        RuntimeLink naming = new RuntimeLink("JNDI View", NameTokens.JndiPresenter);

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
                                ext.getKey(), new RuntimeLink(ext.getName(), ext.getToken())
                        )
                );
            }
            else if(RuntimeGroup.OPERATiONS.equals(ext.getGroup()))
            {
                runtimePredicates.add(
                        new Predicate(
                                ext.getKey(), new RuntimeLink(ext.getName(), ext.getToken())
                        )
                );
            }
            else
            {
                Log.warn("Invalid runtime group for extension: " + ext.getGroup());
            }
        }

        // default links
        platformLinks.add(new RuntimeLink("JVM", NameTokens.HostVMMetricPresenter));
        platformLinks.add(new RuntimeLink("Environment", NameTokens.EnvironmentPresenter));
        platformLinks.add(new RuntimeLink("Log Viewer", NameTokens.LogFiles));

    }

    @Override
    public Widget asWidget() {

        SplitLayoutPanel splitlayout = new SplitLayoutPanel(2);

        serverColumn = new NavigationColumn<Server>(
                "Server",
                new NavigationColumn.Display<Server>() {
                    @Override
                    public SafeHtml render(String baseCss, Server data) {
                        String context = presenter.getFilter().equals(FilterType.HOST) ? data.getGroup() : data.getHostName();
                        return SERVER_TEMPLATE.item(baseCss, data.getName(), context);
                    }
                },
                new ProvidesKey<Server>() {
                    @Override
                    public Object getKey(Server item) {
                        return item.getName() + item.getHostName();
                    }
                });


        subsystemColumn = new NavigationColumn<RuntimeLink>(
                "Views",
                new NavigationColumn.Display<RuntimeLink>() {
                    @Override
                    public SafeHtml render(String baseCss, RuntimeLink data) {
                        return SUBSYSTEM_TEMPLATE.item(baseCss, data.getTitle());
                    }
                },
                new ProvidesKey<RuntimeLink>() {
                    @Override
                    public Object getKey(RuntimeLink item) {
                        return item.getTitle();
                    }
                });


        serverColumn.setTopMenuItems(
                new MenuDelegate<Server>(
                        "<i class=\"icon-refresh\" style='color:black'></i>&nbsp;Refresh", new ContextualCommand<Server>() {
                    @Override
                    public void executeOn(Server server) {
                        // TODO
                    }
                })
        );

        splitlayout.addWest(serverColumn.asWidget(), 217);
        splitlayout.addWest(subsystemColumn.asWidget(), 217);
        splitlayout.add(contentCanvas);

        // selection handling

        serverColumn.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                if (serverColumn.hasSelectedItem()) {

                    final Server selectedServer = serverColumn.getSelectedItem();

                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        public void execute() {

                            Console.getCircuit().dispatch(
                                    new SelectServer(selectedServer.getHostName(), selectedServer.getName())
                            );
                        }
                    });
                }
            }
        });

        subsystemColumn.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
                  @Override
                  public void onSelectionChange(SelectionChangeEvent event) {

                      if (subsystemColumn.hasSelectedItem()) {

                          final RuntimeLink selectedLink = subsystemColumn.getSelectedItem();

                          Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                              public void execute() {

                                  Console.getPlaceManager().revealPlace(new PlaceRequest(selectedLink.getToken()));
                              }
                          });
                      }
                  }
              });


        return splitlayout.asWidget();
    }


    @Override
    public void setInSlot(Object slot, IsWidget  content) {
        if (slot == DomainRuntimePresenter.TYPE_MainContent) {
            if(content!=null)
                setContent(content);

        } else {
            Console.MODULES.getMessageCenter().notify(
                    new Message("Unknown slot requested:" + slot)
            );
        }
    }

    private void setContent(IsWidget  newContent) {
        contentCanvas.clear();
        contentCanvas.add(newContent);
    }

    @Override
    public void setPresenter(DomainRuntimePresenter presenter) {
        this.presenter = presenter;
    }


    @Override
    public void updateServerList(List<Server> serverModel) {
        serverColumn.updateFrom(serverModel, true);
    }

    private class RuntimeLink {

        private String title;
        private String token;

        public RuntimeLink(String title, String token) {
            this.title = title;
            this.token = token;
        }

        public String getTitle() {
            return title;
        }

        public String getToken() {
            return token;
        }
    }

    public final class Predicate {
        private String subsysName;
        private RuntimeLink link;

        public Predicate(String subsysName, RuntimeLink navItem) {
            this.subsysName = subsysName;
            this.link = navItem;
        }

        public boolean matches(String current) {
            return current.equals(subsysName);
        }

        public RuntimeLink getLink() {
            return link;
        }
    }

    @Override
    public void setSubsystems(List<SubsystemRecord> subsystems) {

        List<RuntimeLink> runtimeLinks = new ArrayList<>(platformLinks);

        for(SubsystemRecord subsys : subsystems)
        {

            for(Predicate predicate : metricPredicates)
            {
                if(predicate.matches(subsys.getKey()))
                    runtimeLinks.add(predicate.getLink());
            }
        }

        subsystemColumn.updateFrom(runtimeLinks, true);
    }

}
