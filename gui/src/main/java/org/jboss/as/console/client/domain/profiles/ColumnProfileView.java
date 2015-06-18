package org.jboss.as.console.client.domain.profiles;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.resources.client.ExternalTextResource;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
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
import org.jboss.as.console.client.domain.events.ProfileSelectionEvent;
import org.jboss.as.console.client.domain.model.ProfileRecord;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.plugins.SubsystemExtensionMetaData;
import org.jboss.as.console.client.plugins.SubsystemRegistry;
import org.jboss.as.console.client.preview.PreviewContent;
import org.jboss.as.console.client.preview.PreviewContentFactory;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.shared.model.SubsystemReference;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.ContextualCommand;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.FinderItem;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.as.console.client.widgets.nav.v3.PreviewFactory;
import org.jboss.as.console.client.widgets.nav.v3.ValueProvider;
import org.jboss.ballroom.client.layout.LHSNavTreeItem;
import org.jboss.ballroom.client.widgets.window.Feedback;

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
        implements ProfileMgmtPresenter.MyView {

    /**
     * These items will reveal a nested finder contribution
     */
    private final static String[] subsystemFolders = new String[] {
            NameTokens.MailFinder,
            NameTokens.CacheFinderPresenter,
            NameTokens.HornetqFinder,
            NameTokens.SecDomains,
            NameTokens.UndertowFinder
    };

    private static final String PROFILES = "Profiles";
    private static final String INTERFACES = "Interfaces";
    private static final String SOCKET_BINDING = "Socket Binding";
    private static final String PATHS = "Paths";
    private static final String SYSTEM_PROPERTIES = "System Properties";

    private final FinderColumn<ProfileRecord> profiles;
    private final FinderColumn<FinderItem> config;
    private final FinderColumn<SubsystemLink> subsystems;

    private final ArrayList<FinderItem> configLinks;
    private final ColumnManager columnManager;
    private final Widget profileColWidget;
    private final Widget subsystColWidget;
    private final Widget configColWidget;
    private final PlaceManager placeManager;
    private final PreviewContentFactory contentFactory;

    private SplitLayoutPanel splitlayout;
    private LayoutPanel contentCanvas;
    private ProfileMgmtPresenter presenter;

    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title);

        @Template("<div class=\"{0}\">{1}<br><span style='font-size:10px'>{2}</div>")
        SafeHtml profile(String cssClass, String title, String inclusions);
    }

    interface SubsystemTemplate extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title, String group);

        @Template("<div title='{3}' class=\"{0}\">{1}</div>")
        SafeHtml includedItem(String cssClass, String title, String group, String form);
    }

    private static final Template TEMPLATE = GWT.create(Template.class);

    private static final SubsystemTemplate SUBSYS_TEMPLATE = GWT.create(SubsystemTemplate.class);


    @Inject
    public ColumnProfileView(final PlaceManager placeManager, PreviewContentFactory contentFactory) {
        super();
        this.placeManager = placeManager;
        this.contentFactory = contentFactory;

        contentCanvas = new LayoutPanel();

        splitlayout = new SplitLayoutPanel(2);
        columnManager = new ColumnManager(splitlayout, FinderColumn.FinderId.CONFIGURATION);

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
                        return data.getTitle().equals(PROFILES) ? "no-menu" : "";
                    }
                },
                new ProvidesKey<FinderItem>() {
                    @Override
                    public Object getKey(FinderItem item) {
                        return item.getTitle();
                    }
                }, NameTokens.ProfileMgmtPresenter);

        config.setPreviewFactory(new PreviewFactory<FinderItem>() {

            @Override
            public void createPreview(FinderItem data, AsyncCallback<SafeHtml> callback) {

                final PreviewContent previewContent = PreviewContent.INSTANCE;

                switch (data.getTitle())
                {
                    case PROFILES:
                        contentFactory.createContent(
                                previewContent.profiles_profile(),
                                callback
                        );
                        break;
                    case SOCKET_BINDING:
                        contentFactory.createContent(
                                previewContent.sockets(),
                                callback
                        );
                        break;
                    case INTERFACES:
                        contentFactory.createContent(
                                previewContent.interfaces(),
                                callback
                        );
                        break;
                    case PATHS:
                        contentFactory.createContent(
                                previewContent.paths(),
                                callback
                        );
                        break;
                    case SYSTEM_PROPERTIES:
                        contentFactory.createContent(
                                previewContent.properties(),
                                callback
                        );
                        break;
                }
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
                new FinderItem(PROFILES,
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
                new FinderItem(INTERFACES,
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
                new FinderItem(SOCKET_BINDING,
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
                new FinderItem(PATHS,
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
                new FinderItem(SYSTEM_PROPERTIES,
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
                FinderColumn.FinderId.CONFIGURATION,
                "Profile",
                new FinderColumn.Display<ProfileRecord>() {
                    @Override
                    public boolean isFolder(ProfileRecord data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(String baseCss, ProfileRecord data) {
                        String inclusions = data.getIncludes().isEmpty() ? "" : "Includes: "+ data.getIncludes().toString();
                        return TEMPLATE.profile(baseCss, data.getName(), inclusions);
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
                }, NameTokens.ProfileMgmtPresenter).setShowSize(true);

        profiles.setPreviewFactory(new PreviewFactory<ProfileRecord>() {

            @Override
            public void createPreview(ProfileRecord data, AsyncCallback<SafeHtml> callback) {

                final PreviewContent previewContent = PreviewContent.INSTANCE;
                contentFactory.createContent(
                        previewContent.profiles_profile(),
                        callback
                );
            }
        });

        profiles.setMenuItems(
                new MenuDelegate<ProfileRecord>("Clone", new ContextualCommand<ProfileRecord>() {
                    @Override
                    public void executeOn(ProfileRecord profileRecord) {
                        presenter.onCloneProfile(profileRecord);
                    }
                }, MenuDelegate.Role.Operation)

                /*, new MenuDelegate<ProfileRecord>("Remove", new ContextualCommand<ProfileRecord>() {
                    @Override
                    public void executeOn(ProfileRecord profileRecord) {

                        Feedback.confirm("Remove Profile", "Really remove profile " + profileRecord.getName()+"?", new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if(isConfirmed)
                                {
                                    presenter.onRemoveProfile(profileRecord);
                                }
                            }
                        });

                    }
                }, MenuDelegate.Role.Operation)*/
        );
        profileColWidget = profiles.asWidget();


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
                        if (data.isIncuded())
                            return SUBSYS_TEMPLATE.includedItem(baseCss, data.getTitle(), data.getGroupName(), "Included from "+data.getIncludedFrom());
                        else
                            return SUBSYS_TEMPLATE.item(baseCss, data.getTitle(), data.getGroupName());
                    }

                    @Override
                    public String rowCss(SubsystemLink data) {
                        if(data.isIncuded())
                        {
                            return "no-menu paused";
                        }
                        else if (data.isFolder())
                        {
                            return "no-menu";
                        }
                        else
                        {
                            return "";
                        }

                    }
                },
                new ProvidesKey<SubsystemLink>() {
                    @Override
                    public Object getKey(SubsystemLink item) {
                        return item.getToken();
                    }
                }, NameTokens.ProfileMgmtPresenter).setShowSize(true);

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

        subsystems.setPreviewFactory(new PreviewFactory<SubsystemLink>() {
            @Override
            public void createPreview(SubsystemLink data, final AsyncCallback<SafeHtml> callback) {
                PreviewContent content = PreviewContent.INSTANCE;
                ExternalTextResource resource = (ExternalTextResource)content.getResource(data.getToken().replace("-", "_"));
                if(resource!=null) {
                    contentFactory.createContent(resource, new SimpleCallback<SafeHtml>() {

                        @Override
                        public void onSuccess(SafeHtml safeHtml) {

                            if(data.isIncuded()) {
                                SafeHtmlBuilder builder = new SafeHtmlBuilder();
                                builder.appendHtmlConstant("<div class='preview-content'><h3>");
                                builder.appendEscaped("Included from profile: ").appendEscaped(data.getIncludedFrom());
                                builder.appendHtmlConstant("</h3>");
                                builder.append(safeHtml);
                                builder.appendHtmlConstant("</div>");
                                callback.onSuccess(builder.toSafeHtml());
                            }
                            else {
                                callback.onSuccess(safeHtml);
                            }
                        }
                    });
                }
                else
                {
                    SafeHtmlBuilder builder = new SafeHtmlBuilder();
                    String icon = "icon-folder-close-alt";
                    builder.appendHtmlConstant("<center><i class='" + icon + "' style='font-size:48px;top:100px;position:relative'></i></center>");
                    callback.onSuccess(builder.toSafeHtml());
                }

            }
        });

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

                    clearNestedPresenter();

                    if("Profiles".equals(item.getTitle())) {

                        columnManager.appendColumn(profileColWidget);
                        presenter.loadProfiles();
                    }
                }
                else
                {
                    startupContent();
                }
            }

        });

        profiles.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                if(profiles.hasSelectedItem()) {

                    final ProfileRecord selectedProfile = profiles.getSelectedItem();

                    clearNestedPresenter();

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

    private void startupContent() {

        contentFactory.createContent(
                PreviewContent.INSTANCE.profiles_empty(),
                new SimpleCallback<SafeHtml>() {
                    @Override
                    public void onSuccess(SafeHtml previewContent) {
                        setPreview(previewContent);
                    }
                }
        );
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
    public void setInSlot(Object slot, IsWidget content) {

        if (slot == ProfileMgmtPresenter.TYPE_MainContent) {
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
    public void setProfiles(List<ProfileRecord> profileRecords) {
        profiles.updateFrom(profileRecords, false);
    }

    @Override
    public void setSubsystems(List<SubsystemReference> subsystemRecords)
    {
        subsystems.updateFrom(matchSubsystems(subsystemRecords), false);
    }

    @Override
    public void clearActiveSelection() {
        configColWidget.getElement().removeClassName("active");
        profileColWidget.getElement().removeClassName("active");
        subsystColWidget.getElement().removeClassName("active");
    }

    private void clearNestedPresenter() {

        presenter.clearSlot(ProfileMgmtPresenter.TYPE_MainContent);

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
        private final String groupName;
        private final SubsystemReference ref;

        public SubsystemLink(String title, String token, boolean isFolder, String groupName, SubsystemReference ref) {
            this.title = title;
            this.token = token;
            this.isFolder = isFolder;
            this.groupName = groupName;
            this.ref = ref;
        }

        public String getTitle() {
            return title;
        }

        public String getToken() {
            return token;
        }

        public String getGroupName() {
            return groupName;
        }

        public boolean isFolder() {
            return isFolder;
        }

        public boolean isIncuded(){return ref.isInclude();}

        public String getIncludedFrom() {
            return ref.getIncludedFrom();
        }
    }

    private List<SubsystemLink> matchSubsystems(List<SubsystemReference> subsystems)
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
                boolean match = false;
                for(SubsystemReference ref: subsystems)
                {
                    SubsystemRecord actual = ref.getDelegate();
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
                                new SubsystemLink(candidate.getName(), candidate.getToken(), isFolder, groupName, ref)
                        );
                        match = true;
                    }
                }
                /*if (!match) {
                    System.out.println("Skip subsystem " + candidate.getKey() + ", " + candidate.getName() + ", #" + candidate.getToken());
                }*/
            }

        }

        return matches;
    }

    @Override
    public void setPresenter(ProfileMgmtPresenter presenter) {

        this.presenter = presenter;
    }

    @Override
    public void setPreview(final SafeHtml html) {

        if(contentCanvas.getWidgetCount()==0) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    contentCanvas.add(new HTML(html));
                }
            });
        }

    }

    @Override
    public void toogleScrolling(boolean enforceScrolling, int requiredSize) {
        columnManager.toogleScrolling(enforceScrolling, requiredSize);
    }
}
