package org.jboss.as.console.client.standalone;

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
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.plugins.SubsystemExtensionMetaData;
import org.jboss.as.console.client.plugins.SubsystemRegistry;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.ContextualCommand;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.FinderItem;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.as.console.client.widgets.nav.v3.PreviewFactory;
import org.jboss.as.console.client.widgets.nav.v3.ValueProvider;
import org.jboss.ballroom.client.layout.LHSNavTreeItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Heiko Braun
 * @since 09/01/15
 */
public class ColumnServerView extends SuspendableViewImpl
        implements ServerMgmtApplicationPresenter.ServerManagementView {

    private final FinderColumn<SubsystemLink> subsystems;
    private final FinderColumn<FinderItem> config;
    private final ArrayList<FinderItem> configLinks;
    private final ColumnManager columnManager;

    private final Widget subsystColWidget;
    private final Widget configColWidget;
    private final PlaceManager placeManager;

    private SplitLayoutPanel splitlayout;
    private LayoutPanel contentCanvas;
    private ServerMgmtApplicationPresenter presenter;

    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title);
    }

    private static final Template TEMPLATE = GWT.create(Template.class);


    @Inject
    public ColumnServerView(final PlaceManager placeManager) {
        super();
        this.placeManager = placeManager;

        contentCanvas = new LayoutPanel();

        splitlayout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(splitlayout);

        config = new FinderColumn<FinderItem>(
                FinderColumn.FinderId.CONFIGURATION,
                "Configuration",
                new FinderColumn.Display<FinderItem>() {

                    @Override
                    public boolean isFolder(FinderItem data) {
                        return data.isFolder();
                    }

                    @Override
                    public SafeHtml render(String baseCss, FinderItem data) {
                        return TEMPLATE.item(baseCss, data.getTitle());
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

        config.setPreviewFactory(new PreviewFactory<FinderItem>() {
            @Override
            public SafeHtml createPreview(FinderItem data) {

                SafeHtmlBuilder html = new SafeHtmlBuilder();
                html.appendHtmlConstant("<h2>").appendEscaped(data.getTitle()).appendHtmlConstant("</h2>");
                html.appendEscaped(resolveDescriptionFor(data.getTitle()));
                return html.toSafeHtml();
            }
        });

        config.setMenuItems(new MenuDelegate<FinderItem>("View", new ContextualCommand<FinderItem>() {
            @Override
            public void executeOn(FinderItem item) {
                item.getCmd().execute();
            }
        }));

        configLinks = new ArrayList<>();

        configLinks.add(
                new FinderItem("Subsystems",
                        new Command() {
                            @Override
                            public void execute() {
                                // noop
                            }
                        },
                        true
                )
        );

        configLinks.add(
                new FinderItem("Interfaces",
                        new Command() {
                            @Override
                            public void execute() {
                                placeManager.revealRelativePlace(new PlaceRequest(NameTokens.InterfacePresenter));
                            }
                        },
                        false
                )
        );

        configLinks.add(
                new FinderItem("Socket Binding",
                        new Command() {
                            @Override
                            public void execute() {
                                placeManager.revealRelativePlace(new PlaceRequest(NameTokens.SocketBindingPresenter));
                            }
                        },
                        false
                )
        );

        configLinks.add(
                new FinderItem("Paths",
                        new Command() {
                            @Override
                            public void execute() {
                                placeManager.revealRelativePlace(new PlaceRequest(NameTokens.PathManagementPresenter));
                            }
                        },
                        false
                )
        );

        configLinks.add(
                new FinderItem("System Properties",
                        new Command() {
                            @Override
                            public void execute() {
                                placeManager.revealRelativePlace(new PlaceRequest(NameTokens.PropertiesPresenter));
                            }
                        },
                        false
                )
        );

        subsystems = new FinderColumn<SubsystemLink>(
                FinderColumn.FinderId.CONFIGURATION,
                "Subsystem",
                new FinderColumn.Display<SubsystemLink>() {

                    @Override
                    public boolean isFolder(SubsystemLink data) {
                        return data.isFolder();
                    }

                    @Override
                    public SafeHtml render(String baseCss, SubsystemLink data) {
                        return TEMPLATE.item(baseCss, data.getTitle());
                    }

                    @Override
                    public String rowCss(SubsystemLink data) {
                        return data.isFolder() ? "no-menu" : "";
                    }
                },
                new ProvidesKey<SubsystemLink>() {
                    @Override
                    public Object getKey(SubsystemLink item) {
                        return item.getToken();
                    }
                });

        subsystems.setValueProvider(new ValueProvider<SubsystemLink>() {
            @Override
            public String get(SubsystemLink item) {
                return item.getTitle();
            }
        });

        subsystems.setMenuItems(new MenuDelegate<SubsystemLink>("View", new ContextualCommand<SubsystemLink>() {
            @Override
            public void executeOn(final SubsystemLink link) {
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        placeManager.revealRelativePlace(new PlaceRequest(link.getToken()));
                    }
                });
            }
        }));

        subsystColWidget = subsystems.asWidget();

        configColWidget = config.asWidget();

        columnManager.addWest(configColWidget);
        columnManager.addWest(subsystColWidget);
        columnManager.add(contentCanvas);

        columnManager.setInitialVisible(1);

        // event handler

        config.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent selectionChangeEvent) {
                if(config.hasSelectedItem())
                {
                    FinderItem item = config.getSelectedItem();
                    columnManager.reduceColumnsTo(1);
                    columnManager.updateActiveSelection(configColWidget);

                    clearNestedPresenter();

                    if("Subsystems".equals(item.getTitle())) {

                        columnManager.appendColumn(subsystColWidget);
                        presenter.loadSubsystems();
                    }
                }
            }
        });


        subsystems.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                if(subsystems.hasSelectedItem()) {

                    final SubsystemLink link = subsystems.getSelectedItem();
                    columnManager.updateActiveSelection(subsystColWidget);

                    if(link.isFolder())
                    {
                        placeManager.revealRelativePlace(new PlaceRequest(link.getToken()));
                    }
                    else
                    {
                        clearNestedPresenter();
                    }

                }
            }
        });
    }

    @Override
    public Widget createWidget() {
        Widget widget = splitlayout.asWidget();
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                config.updateFrom(configLinks, false);
            }
        });
        return widget;
    }

    @Override
    public void setPresenter(ServerMgmtApplicationPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setInSlot(Object slot, IsWidget content) {

        if (slot == ServerMgmtApplicationPresenter.TYPE_MainContent) {
            if(content!=null)
                setContent(content);
            else
                contentCanvas.clear();
        }
    }

    private void setContent(IsWidget newContent) {
        contentCanvas.clear();
        contentCanvas.add(newContent);
    }

    @Override
    public void updateFrom(List<SubsystemRecord> subsystemRecords) {
        subsystems.updateFrom(matchSubsystems(subsystemRecords), false);
    }

    @Override
    public void clearActiveSelection() {
        configColWidget.getElement().removeClassName("active");
        subsystColWidget.getElement().removeClassName("active");
    }

    private void clearNestedPresenter() {

        presenter.clearSlot(ServerMgmtApplicationPresenter.TYPE_MainContent);

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                if(placeManager.getHierarchyDepth()>1)
                    placeManager.revealRelativePlace(1);
            }
        });
    }

    class SubsystemLink
    {
        String title;
        String token;
        private final boolean isFolder;

        public SubsystemLink(String title, String token, boolean isFolder) {
            this.title = title;
            this.token = token;
            this.isFolder = isFolder;
        }

        public String getTitle() {
            return title;
        }

        public String getToken() {
            return token;
        }

        public boolean isFolder() {
            return isFolder;
        }
    }


    private final static String[] subsystemFolders = new String[] {
            NameTokens.MailFinder
    };

    private List<SubsystemLink> matchSubsystems(List<SubsystemRecord> subsystems)
    {

        List<SubsystemLink> matches = new ArrayList<>();

        SubsystemRegistry registry = Console.getSubsystemRegistry();

        Map<String, List<SubsystemExtensionMetaData>> grouped = new HashMap<String, List<SubsystemExtensionMetaData>>();
        List<String> groupNames = new ArrayList<String>();
        for(SubsystemExtensionMetaData ext : registry.getExtensions())
        {
            if(!grouped.containsKey(ext.getGroup()))
            {
                groupNames.add(ext.getGroup());
                grouped.put(ext.getGroup(), new ArrayList<SubsystemExtensionMetaData>());
            }

            grouped.get(ext.getGroup()).add(ext);
        }

        Collections.sort(groupNames);

        // build groups first
        for(String groupName : groupNames)
        {
            List<SubsystemExtensionMetaData> items = grouped.get(groupName);

            for(SubsystemExtensionMetaData candidate : items)
            {
                for(SubsystemRecord actual: subsystems)
                {
                    if(actual.getKey().equals(candidate.getKey()))
                    {

                        final LHSNavTreeItem link = new LHSNavTreeItem(candidate.getName(), candidate.getToken());
                        link.setKey(candidate.getKey());
                        link.getElement().setAttribute("title", candidate.getName()+" "+
                                actual.getMajor()+"."+
                                actual.getMinor()+"."+
                                actual.getMicro());


                        boolean isFolder = false;
                        for (String subsystemFolder : subsystemFolders) {
                            if(candidate.getToken().equals(subsystemFolder)) {
                                isFolder = true;
                                break;
                            }
                        }

                        matches.add(
                                new SubsystemLink(candidate.getName(), candidate.getToken(), isFolder)
                        );

                    }
                }
            }

        }

        return matches;
    }

    @Override
    public void setPreview(final SafeHtml html) {

        if(contentCanvas.getWidgetCount()==0) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    HTML widget = new HTML(html);
                    widget.getElement().setAttribute("style", "position:relative;top:100px;margin:0 auto;width:350px;overflow:hidden;padding-top:100px");
                    contentCanvas.add(widget);
                }
            });
        }

    }

    private String resolveDescriptionFor(String title) {
        if("Paths".equals(title))
            return "A logical name for a filesystem path. The domain.xml, host.xml and standalone.xml configurations all include a section where paths can be declared. Other sections of the configuration can then reference those paths by their logical name, rather than having to include the full details of the path (which may vary on different machines). For example, the logging subsystem configuration includes a reference to the \"jboss.server.log.dir\" path that points to the server's \"log\" directory.";

        else if("Subsystems".equals(title))
            return "\"A set of subsystem configurations. A subsystem is an added set of capabilities added to the core server by an extension. A subsystem provides servlet handling capabilities; a subsystem provides an EJB container; a subsystem provides JTA, etc. A profile is a named list of subsystems, along with the details of each subsystem's configuration. A profile with a large number of subsystems results in a server with a large set of capabilities. A profile with a small, focused set of subsystems will have fewer capabilities but a smaller footprint.\"";

        else if("Socket Binding".equals(title))
            return "A socket binding is a named configuration for a socket. The domain.xml and standalone.xml configurations both include a section where named socket configurations can be declared. Other sections of the configuration can then reference those sockets by their logical name, rather than having to include the full details of the socket configuration (which may vary on different machines). See Interfaces and ports for full details.";

        else if("Interfaces".equals(title))
            return "A logical name for a network interface/IP address/host name to which sockets can be bound. The domain.xml, host.xml and standalone.xml configurations all include a section where interfaces can be declared. Other sections of the configuration can then reference those interfaces by their logical name, rather than having to include the full details of the interface (which may vary on different machines). An interface configuration includes the logical name of the interface as well as information specifying the criteria to use for resolving the actual physical address to use. See Interfaces and ports for further details.";

        else if("System Properties".equals(title))
                   return "System property values can be set in a number of places in domain.xml, host.xml and standalone.xml. The values in standalone.xml are set as part of the server boot process. Values in domain.xml and host.xml are applied to servers when they are launched.";

        return "";
    }
}
