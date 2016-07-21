package org.jboss.as.console.client.domain.runtime;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.SrvState;
import org.jboss.as.console.client.domain.model.SuspendState;
import org.jboss.as.console.client.domain.model.impl.LifecycleOperation;
import org.jboss.as.console.client.plugins.RuntimeExtensionMetaData;
import org.jboss.as.console.client.plugins.RuntimeExtensionRegistry;
import org.jboss.as.console.client.plugins.RuntimeGroup;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.v3.stores.domain.actions.FilterType;
import org.jboss.as.console.client.v3.stores.domain.actions.SelectServer;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.ContextualCommand;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.FinderItem;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.as.console.client.widgets.nav.v3.PreviewFactory;
import org.jboss.as.console.client.widgets.nav.v3.PreviewState;
import org.jboss.as.console.client.widgets.nav.v3.ValueProvider;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.dmr.client.dispatch.DispatchAsync;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Heiko Braun
 */
public class DomainRuntimeView extends SuspendableViewImpl implements DomainRuntimePresenter.MyView {

    private final SplitLayoutPanel splitlayout;
    private final PlaceManager placeManager;
    private final DispatchAsync dispatcher;

    private Widget subsysColWidget;
    private Widget statusColWidget;
    private Widget serverColWidget;


    private LayoutPanel contentCanvas;
    private FinderColumn<Server> serverColumn;
    private DomainRuntimePresenter presenter;
    private FinderColumn<FinderItem> statusColumn;
    private FinderColumn<PlaceLink> subsystemColumn;

    private List<Predicate> metricPredicates = new ArrayList<Predicate>();
    private List<Predicate> runtimePredicates = new ArrayList<Predicate>();
    private List<FinderItem> statusLinks = new ArrayList<FinderItem>();
    private List<SubsystemRecord> subsystems;

    private ColumnManager columnManager;

    private final static SafeHtml BLANK = new SafeHtmlBuilder().toSafeHtml();

    interface ServerTemplate extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\" style='line-height:0.9em'>{2}&nbsp;<i class='{1}'></i><br/><span style='font-size:8px'>({3})</span></div>")
        SafeHtml item(String cssClass, String icon, String server, String host);
    }

    interface SubsystemTemplate extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\"><i class='{1}' style='display:none'></i>&nbsp;{2}</span></div>")
        SafeHtml item(String cssClass, String icon, String server);
    }

    interface StatusTemplate extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\"><i class='{1}' style='display:none'></i>&nbsp;{2}</span></div>")
        SafeHtml item(String cssClass, String icon, String title);
    }

    private static final ServerTemplate SERVER_TEMPLATE = GWT.create(ServerTemplate.class);

    private static final StatusTemplate STATUS_TEMPLATE = GWT.create(StatusTemplate.class);

    private static final SubsystemTemplate SUBSYSTEM_TEMPLATE = GWT.create(SubsystemTemplate.class);

    @Inject
    public DomainRuntimeView(final PlaceManager placeManager, DispatchAsync dispatcher) {
        super();

        this.placeManager = placeManager;
        this.dispatcher = dispatcher;
        contentCanvas = new LayoutPanel();
        splitlayout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(splitlayout, FinderColumn.FinderId.RUNTIME);

        PlaceLink datasources = new PlaceLink("Datasources", NameTokens.DataSourceMetricPresenter);
        PlaceLink jmsQueues = new PlaceLink("Messaging", NameTokens.JmsMetricPresenter);
        PlaceLink activemq = new PlaceLink("Messaging - ActiveMQ", NameTokens.ActivemqMetricPresenter);
        PlaceLink web = new PlaceLink("Web/HTTP - Undertow", NameTokens.HttpMetrics);
        PlaceLink jpa = new PlaceLink("JPA", NameTokens.JPAMetricPresenter);
        PlaceLink batch = new PlaceLink("Batch", NameTokens.BatchJberetMetrics);
        PlaceLink ws = new PlaceLink("Webservices", NameTokens.WebServiceRuntimePresenter);
        PlaceLink naming = new PlaceLink("JNDI View", NameTokens.JndiPresenter);

        metricPredicates.add(new Predicate("datasources", datasources));
        metricPredicates.add(new Predicate("messaging", jmsQueues));
        metricPredicates.add(new Predicate("messaging-activemq", activemq));
        metricPredicates.add(new Predicate("undertow", web));
        metricPredicates.add(new Predicate("jpa", jpa));
        metricPredicates.add(new Predicate("batch-jberet", batch));
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

    /*@Override
    public void onClearActiveSelection(ClearFinderSelectionEvent event) {
        serverColWidget.getElement().removeClassName("active");
        subsysColWidget.getElement().removeClassName("active");
        statusColWidget.getElement().removeClassName("active");
    }*/

    @Override
    public Widget createWidget() {

        serverColumn = new FinderColumn<Server>(
                FinderColumn.FinderId.RUNTIME,
                "Server",
                new FinderColumn.Display<Server>() {

                    @Override
                    public boolean isFolder(Server data) {
                        return data.isStarted();
                    }

                    @Override
                    public SafeHtml render(String baseCss, Server server) {
                        String context = presenter.getFilter().equals(FilterType.HOST) ? server.getGroup() : server.getHostName();
                        return SERVER_TEMPLATE.item(baseCss, "",server.getName(), context);
                    }

                    @Override
                    public String rowCss(Server server) {

                        String css = "";
                        // TODO: reload state
                        if(!server.isStarted())
                        {
                            css = "paused";
                        }
                        else if(server.getServerState()== SrvState.RELOAD_REQUIRED)
                        {
                            css = "warn";
                        }
                        else if(server.getServerState()== SrvState.RESTART_REQUIRED)
                        {
                            css = "warn";
                        }
                        else if(server.getSuspendState()==SuspendState.SUSPENDED)
                        {
                            css = "info";
                        }
                        else if(server.isStarted())
                        {
                            css = "good";
                        }

                        return css;
                    }
                },
                new ProvidesKey<Server>() {
                    @Override
                    public Object getKey(Server item) {
                        return item.getName() + item.getHostName() + ":" + item.getServerState();
                    }
                }, presenter.getProxy().getNameToken());

        serverColumn.setShowSize(true);
        serverColumn.setFilter((item, token) -> {
            return item.getName().contains(token);
        });

        serverColumn.setValueProvider(new ValueProvider<Server>() {
            @Override
            public String get(Server item) {
                return item.getName();
            }
        });

        serverColumn.setTopMenuItems(
                new MenuDelegate<Server>(
                        Console.CONSTANTS.common_label_add(), new ContextualCommand<Server>() {
                    @Override
                    public void executeOn(Server server) {
                        presenter.launchNewConfigDialoge();
                    }
                }, MenuDelegate.Role.Operation),
                new MenuDelegate<Server>(
                        Console.CONSTANTS.common_label_refresh(), new ContextualCommand<Server>() {
                    @Override
                    public void executeOn(Server server) {
                        presenter.refreshServer();
                    }
                }, MenuDelegate.Role.Navigation)
        );

        serverColumn.setPreviewFactory(new PreviewFactory<Server>() {

            @Override
            public void createPreview(Server data, AsyncCallback<SafeHtml> callback) {
                SafeHtmlBuilder html = new SafeHtmlBuilder();
                html.appendHtmlConstant("<div class='preview-content'>");

                html.appendHtmlConstant("<h2>");
                html.appendEscaped("Server Configuration");
                html.appendHtmlConstant("</h2>");


                html.appendEscaped(Console.CONSTANTS.serverDescription());

                // TODO: reload state
                if (!data.isStarted()) {
                    PreviewState.paused(html, "Server is stopped");
                } else if (data.getServerState() == SrvState.RELOAD_REQUIRED) {
                    PreviewState.warn(html, Console.CONSTANTS.server_instance_reloadRequired());
                } else if (data.getServerState() == SrvState.RESTART_REQUIRED) {
                    PreviewState.warn(html, "Server needs to be restarted");
                } else if (data.getSuspendState() == SuspendState.SUSPENDED) {
                    PreviewState.info(html, "Server is suspended");
                } else if (data.getServerState() == SrvState.RUNNING) {
                    String id = "port-offset-" + data.getGroup() + "-" + data.getName();
                    html.appendHtmlConstant("<p>")
                            .appendEscaped("Port offset: ")
                            .appendHtmlConstant("<span id=\"" + id + "\">").appendHtmlConstant("</span>")
                            .appendHtmlConstant("</p>");
                    new ReadPortOffsetOp(dispatcher).execute(data, id);
                }

                html.appendHtmlConstant("</div>");
                callback.onSuccess(html.toSafeHtml());
            }

        });


        serverColumn.setMenuItems(
                new MenuDelegate<Server>(
                        "View", new ContextualCommand<Server>() {
                    @Override
                    public void executeOn(final Server server) {

                        placeManager.revealRelativePlace(
                                new PlaceRequest(NameTokens.ServerPresenter).with("action", "edit")
                        );

                    }
                }),
                new MenuDelegate<Server>(
                        "Remove", new ContextualCommand<Server>() {
                    @Override
                    public void executeOn(final Server server) {

                        Feedback.confirm(
                                "Remove server",
                                "Do you really want to remove server " + server.getName() + "?",
                                new Feedback.ConfirmationHandler() {

                                    @Override
                                    public void onConfirmation(boolean isConfirmed) {
                                        if (isConfirmed)
                                            presenter.tryDelete(presenter.getSelectedServer());
                                        else {
                                            presenter.closeWindow();
                                        }
                                    }
                                });
                    }
                }, MenuDelegate.Role.Operation),
                new MenuDelegate<Server>(
                        "Copy", new ContextualCommand<Server>() {
                    @Override
                    public void executeOn(Server server) {
                        presenter.onLaunchCopyWizard(server);
                    }
                }, MenuDelegate.Role.Operation),
                new MenuDelegate<Server>(
                        "Start/Stop", new ContextualCommand<Server>() {
                    @Override
                    public void executeOn(Server server) {

                        LifecycleOperation op = server.isStarted() ? LifecycleOperation.STOP : LifecycleOperation.START;
                        Feedback.confirm(
                                "Server " + op.name(),
                                "Do you really want to " + op.name() + " server " + server.getName() + "?",
                                new Feedback.ConfirmationHandler() {

                                    @Override
                                    public void onConfirmation(boolean isConfirmed) {
                                        if (isConfirmed)
                                            presenter.onServerInstanceLifecycle(server.getHostName(), server.getName(), op);
                                    }
                                });

                    }

                }, MenuDelegate.Role.Operation) {

                    @Override
                    public String render(Server server) {
                        return server.isStarted() ? "Stop" : "Start";
                    }
                }.setOperationAddress("/{implicit.host}/server-config=*", "start"),

                new MenuDelegate<Server>(
                        "Suspend or not", new ContextualCommand<Server>() {
                    @Override
                    public void executeOn(Server server) {

                        LifecycleOperation op = server.getSuspendState() == SuspendState.SUSPENDED ?
                                LifecycleOperation.RESUME : LifecycleOperation.SUSPEND;

                        if (LifecycleOperation.RESUME == op) {
                            presenter.onServerInstanceLifecycle(server.getHostName(), server.getName(), op);
                        } else {
                            presenter.onLaunchSuspendDialogue(server);
                        }


                    }

                }, MenuDelegate.Role.Operation) {

                    @Override
                    public String render(Server server) {
                        return server.getSuspendState() == SuspendState.SUSPENDED ? "Resume" : "Suspend";
                    }
                }.setOperationAddress("/{implicit.host}/server-config=*","resume"),

                new MenuDelegate<Server>(
                        "Reload", new ContextualCommand<Server>() {
                    @Override
                    public void executeOn(Server server) {

                        Feedback.confirm(
                                "Reload Server",
                                "Do you really want to reload server " + server.getName() + "?",
                                new Feedback.ConfirmationHandler() {

                                    @Override
                                    public void onConfirmation(boolean isConfirmed) {
                                        if (isConfirmed)
                                            presenter.onServerInstanceLifecycle(server.getHostName(), server.getName(), LifecycleOperation.RELOAD);
                                    }
                                });

                    }
                }, MenuDelegate.Role.Operation)
                        .setOperationAddress("/{implicit.host}/server-config=*","reload"),

                new MenuDelegate<Server>(
                        "Restart", new ContextualCommand<Server>() {
                    @Override
                    public void executeOn(Server server) {
                        Feedback.confirm(
                                "Restart Server",
                                "Do you really want to restart server " + server.getName() + "?",
                                new Feedback.ConfirmationHandler() {

                                    @Override
                                    public void onConfirmation(boolean isConfirmed) {
                                        if (isConfirmed)
                                            presenter.onServerInstanceLifecycle(server.getHostName(), server.getName(), LifecycleOperation.RESTART);
                                    }
                                });


                    }
                }, MenuDelegate.Role.Operation)
                        .setOperationAddress("/{implicit.host}/server-config=*","restart")
        );

        serverColumn.setTooltipDisplay(new FinderColumn.TooltipDisplay<Server>() {
            @Override
            public SafeHtml render(Server server) {
                String message = server.isStarted() ? "running" : "not running";
                SafeHtmlBuilder sb = new SafeHtmlBuilder();
                /*if(data.isStarted())
                    sb.appendHtmlConstant("<i class=\"icon-ok\" style='color:#3F9C35'></i>&nbsp;");
                else
                    sb.appendHtmlConstant("<i class=\"icon-ban-circle\" style='color:#CC0000'></i>&nbsp;");*/
                sb.appendEscaped("Server is ").appendEscaped(message);

                if(server.getServerState()==SrvState.RELOAD_REQUIRED)
                {
                    sb.appendEscaped(". "+Console.CONSTANTS.server_instance_reloadRequired());
                }
                else if(server.getServerState()==SrvState.RESTART_REQUIRED)
                {
                    sb.appendEscaped(". " + Console.CONSTANTS.server_instance_servers_needRestart());
                }
                else if (server.getSuspendState() == SuspendState.SUSPENDED)
                    sb.appendEscaped(", but suspended");

                return sb.toSafeHtml();
            }
        });

        serverColWidget = serverColumn.asWidget();

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
                }, presenter.getProxy().getNameToken());

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
                }, presenter.getProxy().getNameToken());

        subsystemColumn.setPreviewFactory(new PreviewFactory<PlaceLink>() {
            @Override
            public void createPreview(PlaceLink data, AsyncCallback<SafeHtml> callback) {
                SafeHtmlBuilder builder = new SafeHtmlBuilder();
                builder.appendHtmlConstant("<div class='preview-content'><span style='font-size:24px;'><i class='icon-bar-chart' style='font-size:48px;vertical-align:middle'></i>&nbsp;"+data.getTitle()+"</span></center>");
                builder.appendHtmlConstant("</div>");
                callback.onSuccess(builder.toSafeHtml());
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
        columnManager.addWest(serverColWidget);
        columnManager.addWest(statusColWidget);
        columnManager.addWest(subsysColWidget);
        columnManager.add(contentCanvas);

        columnManager.setInitialVisible(1);

        // selection handling

        serverColumn.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                // column handling
                columnManager.reduceColumnsTo(1);

                if (serverColumn.hasSelectedItem()) {

                    // selection
                    columnManager.updateActiveSelection(serverColWidget);
                    final Server selectedServer = serverColumn.getSelectedItem();

                    // action
                    if(selectedServer.isStarted()) {
                        columnManager.appendColumn(statusColWidget);
                    }

                    Console.getCircuit().dispatch(
                            new SelectServer(selectedServer.getHostName(), selectedServer.getName())
                    );
                }
            }
        });

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

                } else {
                    columnManager.reduceColumnsTo(2);
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
    public void setInSlot(Object slot, IsWidget  content) {
        if (slot == DomainRuntimePresenter.TYPE_MainContent) {
            if(content!=null)
                setContent(content);
            else
                contentCanvas.clear();

        }
    }

    private void setContent(IsWidget  newContent) {
        contentCanvas.clear();
        contentCanvas.add(newContent);
    }

    @Override
    public void setPreview(final SafeHtml html) {

        if (presenter.getPlaceManager().getCurrentPlaceRequest().getNameToken().equals(serverColumn.getToken())) {
            Scheduler.get().scheduleDeferred(() -> {
                contentCanvas.clear();
                contentCanvas.add(new ScrollPanel(new HTML(html)));
            });
        }

    }

    @Override
    public void setPresenter(DomainRuntimePresenter presenter) {
        this.presenter = presenter;
    }


    @Override
    public void updateServerList(List<Server> serverModel) {
        columnManager.reduceColumnsTo(1);

        Server oldServer = serverColumn.getSelectedItem();
        Server newServer = null;

        if (oldServer != null) {
            for (Server s : serverModel) {
                if (s.getName().equals(oldServer.getName())) {
                    newServer = s;
                    break;
                }
            }
        }
        serverColumn.updateFrom(serverModel, newServer);
        //previewCanvas.clear();
    }

    @Override
    public void clearServerList() {
        columnManager.reduceColumnsTo(1);
        serverColumn.updateFrom(Collections.EMPTY_LIST, false);
        //previewCanvas.clear();
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
