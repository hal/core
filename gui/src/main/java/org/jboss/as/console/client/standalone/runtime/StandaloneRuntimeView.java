package org.jboss.as.console.client.standalone.runtime;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.core.message.Message;
import org.jboss.as.console.client.domain.runtime.DomainRuntimePresenter;
import org.jboss.as.console.client.plugins.RuntimeExtensionMetaData;
import org.jboss.as.console.client.plugins.RuntimeExtensionRegistry;
import org.jboss.as.console.client.plugins.RuntimeGroup;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.ContextualCommand;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.FinderItem;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.as.console.client.widgets.nav.v3.PreviewFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Heiko Braun
 */
public class StandaloneRuntimeView extends SuspendableViewImpl implements StandaloneRuntimePresenter.MyView {

    private final SplitLayoutPanel splitlayout;
    private final PlaceManager placeManager;
    private final LayoutPanel previewCanvas;
    private Widget subsysColWidget;
    private Widget statusColWidget;

    private LayoutPanel contentCanvas;
    private StandaloneRuntimePresenter presenter;
    private FinderColumn<FinderItem> statusColumn;
    private FinderColumn<PlaceLink> subsystemColumn;

    private List<Predicate> metricPredicates = new ArrayList<Predicate>();
    private List<Predicate> runtimePredicates = new ArrayList<Predicate>();
    private List<FinderItem> statusLinks = new ArrayList<FinderItem>();
    private List<SubsystemRecord> subsystems;

    private ColumnManager columnManager;

    private final static SafeHtml BLANK = new SafeHtmlBuilder().toSafeHtml();


    interface SubsystemTemplate extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\"><i class='{1}' style='display:none'></i>&nbsp;{2}</span></div>")
        SafeHtml item(String cssClass, String icon, String server);
    }

    interface StatusTemplate extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\"><i class='{1}' style='display:none'></i>&nbsp;{2}</span></div>")
        SafeHtml item(String cssClass, String icon, String title);
    }

    private static final StatusTemplate STATUS_TEMPLATE = GWT.create(StatusTemplate.class);

    private static final SubsystemTemplate SUBSYSTEM_TEMPLATE = GWT.create(SubsystemTemplate.class);

    @Inject
    public StandaloneRuntimeView(final PlaceManager placeManager) {
        super();
        this.placeManager = placeManager;
        contentCanvas = new LayoutPanel(); // TODO remove, including the widget slot in presenter
        previewCanvas = new LayoutPanel();

        splitlayout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(splitlayout);

        PlaceLink datasources = new PlaceLink("Datasources", NameTokens.DataSourceMetricPresenter);
        PlaceLink jmsQueues = new PlaceLink("JMS Destinations", NameTokens.JmsMetricPresenter);
        PlaceLink web = new PlaceLink("Web", NameTokens.WebMetricPresenter);
        PlaceLink jpa = new PlaceLink("JPA", NameTokens.JPAMetricPresenter);
        PlaceLink ws = new PlaceLink("Webservices", NameTokens.WebServiceRuntimePresenter);
        PlaceLink naming = new PlaceLink("JNDI View", NameTokens.JndiPresenter);

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
                                ext.getKey(), new PlaceLink(ext.getName(), ext.getToken())
                        )
                );
            }
            else if(RuntimeGroup.OPERATiONS.equals(ext.getGroup()))
            {
                runtimePredicates.add(
                        new Predicate(
                                ext.getKey(), new PlaceLink(ext.getName(), ext.getToken())
                        )
                );
            }
            else
            {
                Log.warn("Invalid runtime group for extension: " + ext.getGroup());
            }
        }

        // default links
        statusLinks.add(new FinderItem("JVM", new Command() {
            @Override
            public void execute() {

                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    public void execute() {

                        Console.getPlaceManager().revealRelativePlace(new PlaceRequest(NameTokens.HostVMMetricPresenter));
                    }
                });

            }
        }, false));
        statusLinks.add(new FinderItem("Environment", new Command() {
            @Override
            public void execute() {
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    public void execute() {

                        Console.getPlaceManager().revealRelativePlace(new PlaceRequest(NameTokens.EnvironmentPresenter));
                    }
                });

            }
        }, false));
        statusLinks.add(new FinderItem("Log Files", new Command() {
            @Override
            public void execute() {
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    public void execute() {

                        Console.getPlaceManager().revealRelativePlace(new PlaceRequest(NameTokens.LogFiles));
                    }
                });

            }
        }, false));

        statusLinks.add(new FinderItem("Subsystems", new Command() {
            @Override
            public void execute() {
                // noop
            }
        }, true));

    }

    @Override
    public Widget createWidget() {


        statusColumn = new FinderColumn<FinderItem>(
                FinderColumn.FinderId.RUNTIME,
                "Monitor",
                new FinderColumn.Display<FinderItem>() {

                    @Override
                    public boolean isFolder(FinderItem data) {
                        return data.isFolder();
                    }

                    @Override
                    public SafeHtml render(String baseCss, FinderItem data) {
                        String icon = data.isFolder() ? "icon-folder-close-alt" : "icon-file-alt";
                        return STATUS_TEMPLATE.item(baseCss, icon, data.getTitle());
                    }

                    @Override
                    public String rowCss(FinderItem data) {
                        return data.getTitle().equals("Subsystems") ? "no-menu" : "";
                    }
                },
                new ProvidesKey<FinderItem>() {
                    @Override
                    public Object getKey(FinderItem item) {
                        return item.getTitle();
                    }
                });

        statusColumn.setMenuItems(
                new MenuDelegate<FinderItem>("View", new ContextualCommand<FinderItem>() {
                    @Override
                    public void executeOn(FinderItem link) {
                        link.getCmd().execute();
                    }
                })
        );

        statusColWidget = statusColumn.asWidget();

        subsystemColumn = new FinderColumn<PlaceLink>(
                FinderColumn.FinderId.RUNTIME,
                "Subsystem",
                new FinderColumn.Display<PlaceLink>() {

                    @Override
                    public boolean isFolder(PlaceLink data) {
                        return false;
                    }

                    @Override
                    public SafeHtml render(String baseCss, PlaceLink data) {
                        return SUBSYSTEM_TEMPLATE.item(baseCss, "icon-file-alt", data.getTitle());
                    }

                    @Override
                    public String rowCss(PlaceLink data) {

                        return "";
                    }
                },
                new ProvidesKey<PlaceLink>() {
                    @Override
                    public Object getKey(PlaceLink item) {
                        return item.getTitle();
                    }
                });

        subsystemColumn.setPreviewFactory(new PreviewFactory<PlaceLink>() {
            @Override
            public SafeHtml createPreview(PlaceLink data) {
                SafeHtmlBuilder builder = new SafeHtmlBuilder();
                builder.appendHtmlConstant("<center><span style='font-size:24px;'><i class='icon-bar-chart' style='font-size:48px;vertical-align:middle'></i>&nbsp;"+data.getTitle()+"</span></center>");
                return builder.toSafeHtml();
            }
        });

        subsystemColumn.setMenuItems(
                new MenuDelegate<PlaceLink>("View", new ContextualCommand<PlaceLink>() {
                    @Override
                    public void executeOn(PlaceLink link) {
                        link.getCmd().execute();
                    }
                })
        );



        subsysColWidget = subsystemColumn.asWidget();

        // server column is always present
        columnManager.addWest(statusColWidget);
        columnManager.addWest(subsysColWidget);
        columnManager.add(previewCanvas);

        columnManager.setInitialVisible(1);

        // selection handling

        statusColumn.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                if (statusColumn.hasSelectedItem()) {

                    columnManager.updateActiveSelection(statusColWidget);

                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        @Override
                        public void execute() {

                            if(statusColumn.getSelectedItem().getTitle().equals("Subsystems"))
                            {
                                columnManager.appendColumn(subsysColWidget);
                                updateSubsystemColumn(subsystems);
                            }
                            else
                            {
                                columnManager.reduceColumnsTo(2);
                            }
                        }
                    });

                }
            }
        });


        subsystemColumn.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                if (subsystemColumn.hasSelectedItem()) {

                    columnManager.updateActiveSelection(subsysColWidget);
                }
            }
        });


        return splitlayout.asWidget();
    }

    @Override
    public void setInSlot(Object slot, IsWidget content) {
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
    public void setPreview(final SafeHtml html) {

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                previewCanvas.clear();
                HTML widget = new HTML(html);
                widget.getElement().setAttribute("style", "position:relative;top:100px;margin:0 auto;width:350px;overflow:hidden;padding-top:100px");
                previewCanvas.add(widget);
            }
        });

    }

    @Override
    public void setPresenter(StandaloneRuntimePresenter presenter) {
        this.presenter = presenter;
    }


    private class PlaceLink extends FinderItem {

        public PlaceLink(String title, final String token) {
            super(title, new Command() {
                @Override
                public void execute() {
                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        public void execute() {

                            Console.getPlaceManager().revealRelativePlace(new PlaceRequest(token));
                        }
                    });
                }
            }, false);

        }
    }



    public final class Predicate {
        private String subsysName;
        private PlaceLink link;

        public Predicate(String subsysName, PlaceLink navItem) {
            this.subsysName = subsysName;
            this.link = navItem;
        }

        public boolean matches(String current) {
            return current.equals(subsysName);
        }

        public PlaceLink getLink() {
            return link;
        }
    }

    @Override
    public void setSubsystems(List<SubsystemRecord> subsystems) {

        statusColumn.updateFrom(statusLinks);
        this.subsystems = subsystems;
    }

    private void updateSubsystemColumn(List<SubsystemRecord> subsystems)
    {
        List<PlaceLink> runtimeLinks = new ArrayList<>();

        for(SubsystemRecord subsys : subsystems)
        {

            for(Predicate predicate : metricPredicates)
            {
                if(predicate.matches(subsys.getKey()))
                    runtimeLinks.add(predicate.getLink());
            }
        }

        subsystemColumn.updateFrom(runtimeLinks, false);
    }

}
