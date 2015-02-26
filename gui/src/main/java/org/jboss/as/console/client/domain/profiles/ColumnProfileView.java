package org.jboss.as.console.client.domain.profiles;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.Command;
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
import org.jboss.as.console.client.domain.events.ProfileSelectionEvent;
import org.jboss.as.console.client.domain.model.ProfileRecord;
import org.jboss.as.console.client.plugins.SubsystemExtensionMetaData;
import org.jboss.as.console.client.plugins.SubsystemRegistry;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.ContextualCommand;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.FinderItem;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.as.console.client.widgets.tree.GroupItem;
import org.jboss.ballroom.client.layout.LHSHighlightEvent;
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
public class ColumnProfileView extends SuspendableViewImpl
        implements ProfileMgmtPresenter.MyView, LHSHighlightEvent.NavItemSelectionHandler {

    private final FinderColumn<ProfileRecord> profiles;
    private final FinderColumn<SubsystemLink> subsystems;
    private final FinderColumn<FinderItem> config;
    private final ArrayList<FinderItem> configLinks;
    private final ColumnManager columnManager;
    private final Widget profileColWidget;
    private final Widget subsystColWidget;
    private final Widget configColWidget;
    private final PlaceManager placeManager;

    private SplitLayoutPanel splitlayout;
    private LayoutPanel contentCanvas;
    private ProfileMgmtPresenter presenter;

    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title);
    }

    private static final Template TEMPLATE = GWT.create(Template.class);


    @Inject
    public ColumnProfileView(final PlaceManager placeManager) {
        super();
        this.placeManager = placeManager;

        contentCanvas = new LayoutPanel();
        splitlayout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(splitlayout);

        config = new FinderColumn<FinderItem>(
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
                        return data.getTitle().equals("Profiles") ? "no-menu" : "";
                    }
                },
                new ProvidesKey<FinderItem>() {
                    @Override
                    public Object getKey(FinderItem item) {
                        return item.getTitle();
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
                new FinderItem("Profiles",
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

        profiles = new FinderColumn<ProfileRecord>(
                "Profiles",
                new FinderColumn.Display<ProfileRecord>() {
                    @Override
                    public boolean isFolder(ProfileRecord data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(String baseCss, ProfileRecord data) {
                        return TEMPLATE.item(baseCss, data.getName());
                    }

                    @Override
                    public String rowCss(ProfileRecord data) {
                        return "";
                    }
                },
                new ProvidesKey<ProfileRecord>() {
                    @Override
                    public Object getKey(ProfileRecord item) {
                        return item.getName();
                    }
                });

        profileColWidget = profiles.asWidget();

        subsystems = new FinderColumn<SubsystemLink>(
                "Subsystems",
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
                        return "";
                    }
                },
                new ProvidesKey<SubsystemLink>() {
                    @Override
                    public Object getKey(SubsystemLink item) {
                        return item.getToken();
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
        columnManager.addWest(profileColWidget);
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

                    if("Profiles".equals(item.getTitle())) {

                        columnManager.appendColumn(profileColWidget);
                        presenter.onRefreshProfiles();
                    }
                }
            }
        });

        profiles.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                if(profiles.hasSelectedItem()) {

                    final ProfileRecord selectedProfile = profiles.getSelectedItem();

                    columnManager.updateActiveSelection(profileColWidget);
                    columnManager.reduceColumnsTo(2);
                    columnManager.appendColumn(subsystColWidget);

                    Scheduler.get().scheduleDeferred(
                            new Scheduler.ScheduledCommand() {
                                @Override
                                public void execute() {
                                    Console.getEventBus().fireEvent(
                                            new ProfileSelectionEvent(selectedProfile.getName())
                                    );
                                }
                            }
                    );

                }
            }
        });

        subsystems.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                if(subsystems.hasSelectedItem()) {

                    columnManager.updateActiveSelection(subsystColWidget);

                    final SubsystemLink selectedSubsystem = subsystems.getSelectedItem();

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
                config.updateFrom(configLinks, true);
            }
        });
        return widget;
    }

    @Override
    public void setInSlot(Object slot, IsWidget content) {

        if (slot == ProfileMgmtPresenter.TYPE_MainContent) {
            if(content!=null)
                setContent(content);
        }
    }

    private void setContent(IsWidget newContent) {
        contentCanvas.clear();
        contentCanvas.add(newContent);
    }

    @Override
    public void setProfiles(List<ProfileRecord> profileRecords) {
        profiles.updateFrom(profileRecords, false);
    }

    @Override
    public void setSubsystems(List<SubsystemRecord> subsystemRecords)
    {
        subsystems.updateFrom(matchSubsystems(subsystemRecords), false);
    }

    @Override
    public void setPreselection(String preselection) {

    }

    @Override
    public void onSelectedNavTree(LHSHighlightEvent event) {
        subsystems.selectByKey(event.getToken());
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

        int includedSubsystems = 0;


        Collections.sort(groupNames);

        // build groups first
        for(String groupName : groupNames)
        {
            List<SubsystemExtensionMetaData> items = grouped.get(groupName);

            final GroupItem groupTreeItem = new GroupItem(groupName);

            for(SubsystemExtensionMetaData candidate : items)
            {
                for(SubsystemRecord actual: subsystems)
                {
                    if(actual.getKey().equals(candidate.getKey()))
                    {
                        includedSubsystems++;

                        final LHSNavTreeItem link = new LHSNavTreeItem(candidate.getName(), candidate.getToken());
                        link.setKey(candidate.getKey());
                        link.getElement().setAttribute("title", candidate.getName()+" "+
                                actual.getMajor()+"."+
                                actual.getMinor()+"."+
                                actual.getMicro());

                            /*if(compatibleVersion(actual, candidate)) {
                                groupTreeItem.addItem(link);
                            }*/

                        matches.add(
                                new SubsystemLink(candidate.getName(), candidate.getToken(), false)
                        );

                    }
                }
            }


            // skip empty groups
               /* if(groupTreeItem.getChildCount()>0)
                    matches.add(new SubsystemLink());*/

        }

            /*// fallback in case no manageable subsystems exist
            if(includedSubsystems==0)
            {
                HTML explanation = new HTML("No manageable subsystems exist.");
                explanation.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        displaySubsystemHelp(subsysTree);
                    }
                });
                subsysTree.addItem(new TreeItem(explanation));
            }*/

        return matches;
    }

    @Override
    public void setPresenter(ProfileMgmtPresenter presenter) {

        this.presenter = presenter;
    }
}
