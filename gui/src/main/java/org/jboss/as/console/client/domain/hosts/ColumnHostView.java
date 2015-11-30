package org.jboss.as.console.client.domain.hosts;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
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
import org.jboss.as.console.client.core.UIConstants;
import org.jboss.as.console.client.core.UIMessages;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.domain.model.impl.LifecycleOperation;
import org.jboss.as.console.client.preview.PreviewContent;
import org.jboss.as.console.client.preview.PreviewContentFactory;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.v3.stores.domain.ServerStore;
import org.jboss.as.console.client.v3.stores.domain.actions.FilterType;
import org.jboss.as.console.client.v3.stores.domain.actions.GroupSelection;
import org.jboss.as.console.client.v3.stores.domain.actions.HostSelection;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
import org.jboss.as.console.client.widgets.nav.v3.ColumnFilter;
import org.jboss.as.console.client.widgets.nav.v3.ColumnManager;
import org.jboss.as.console.client.widgets.nav.v3.ContextualCommand;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.FinderItem;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.as.console.client.widgets.nav.v3.PreviewFactory;
import org.jboss.ballroom.client.widgets.window.Feedback;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Heiko Braun
 * @since 09/01/15
 */
public class ColumnHostView extends SuspendableViewImpl
        implements HostMgmtPresenter.MyView, ClearFinderSelectionEvent.Handler {

    private final FinderColumn<String> hosts;
    private final FinderColumn<ServerGroupRecord> groups;
    private final Widget hostColWidget;
    private final Widget groupsColWidget;
    private final FinderColumn<FinderItem> browseColumn;
    private final PreviewContentFactory contentFactory;

    private SplitLayoutPanel layout;
    private LayoutPanel contentCanvas;
    private HostMgmtPresenter presenter;

    private ColumnManager columnManager;

    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\" title='{1}'>{1}</div>")
        SafeHtml item(String cssClass, String title);
    }

    private static final Template TEMPLATE = GWT.create(Template.class);

    interface StatusTemplate extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\"><i class='{1}' style='display:none'></i>{2}</span></div>")
        SafeHtml item(String cssClass, String icon, String title);
    }

    private static final StatusTemplate STATUS_TEMPLATE = GWT.create(StatusTemplate.class);

    @Inject
    public ColumnHostView(final HostStore hostStore, final ServerStore serverStore,
                          final PreviewContentFactory contentFactory) {
        super();
        this.contentFactory = contentFactory;

        Console.getEventBus().addHandler(ClearFinderSelectionEvent.TYPE, this);

        contentCanvas = new LayoutPanel();

        layout = new SplitLayoutPanel(2);

        columnManager = new ColumnManager(layout, FinderColumn.FinderId.RUNTIME);

        hosts = new FinderColumn<String>(
                FinderColumn.FinderId.RUNTIME,
                "Host",
                new FinderColumn.Display<String>() {

                    @Override
                    public boolean isFolder(String data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(String baseCss, String data) {
                        return TEMPLATE.item(baseCss, data);
                    }

                    @Override
                    public String rowCss(String data) {
                        return "";
                    }
                },
                new ProvidesKey<String>() {
                    @Override
                    public Object getKey(String item) {
                        return item;
                    }
                }, NameTokens.HostMgmtPresenter
        );

        hosts.setShowSize(true);

        hosts.setPreviewFactory(
                (data, callback) -> contentFactory.createContent(PreviewContent.INSTANCE.runtime_host(), callback));

        hosts.setTopMenuItems(new MenuDelegate<String>("Refresh", new ContextualCommand<String>() {
            @Override
            public void executeOn(String item) {
                presenter.refreshState();
            }
        }, MenuDelegate.Role.Navigation));

        groups = new FinderColumn<ServerGroupRecord>(
                FinderColumn.FinderId.RUNTIME,
                "Server Group",
                new FinderColumn.Display<ServerGroupRecord>() {

                    @Override
                    public boolean isFolder(ServerGroupRecord data) {
                        return true;
                    }

                    @Override
                    public SafeHtml render(String baseCss, ServerGroupRecord data) {
                        return TEMPLATE.item(baseCss, data.getName());
                    }

                    @Override
                    public String rowCss(ServerGroupRecord data) {
                        return "";
                    }
                },
                new ProvidesKey<ServerGroupRecord>() {
                    @Override
                    public Object getKey(ServerGroupRecord item) {
                        return item.getName();
                    }
                }, NameTokens.HostMgmtPresenter);

        groups.setShowSize(true);

        groups.setFilter(new ColumnFilter.Predicate<ServerGroupRecord>() {
            @Override
            public boolean matches(ServerGroupRecord item, String token) {
                return item.getName().contains(token);
            }
        });

        groups.setTopMenuItems(new MenuDelegate<ServerGroupRecord>("Add",
                        new ContextualCommand<ServerGroupRecord>() {
                            @Override
                            public void executeOn(ServerGroupRecord group) {
                                // TODO "/server-group=*", "add" permission
                                presenter.launchNewGroupDialog();
                            }
                        }, MenuDelegate.Role.Operation)
        );

        groups.setPreviewFactory((data, callback) -> contentFactory
                .createContent(PreviewContent.INSTANCE.runtime_server_group(), callback));

        hostColWidget = hosts.asWidget();
        groupsColWidget = groups.asWidget();

        browseColumn = new FinderColumn<FinderItem>(
                FinderColumn.FinderId.RUNTIME,
                "Browse Domain By",
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
                        return "";
                    }
                },
                new ProvidesKey<FinderItem>() {
                    @Override
                    public Object getKey(FinderItem item) {
                        return item.getTitle();
                    }
                }, NameTokens.HostMgmtPresenter);

        browseColumn.setPreviewFactory(new PreviewFactory<FinderItem>() {
            @Override
            public void createPreview(final FinderItem data, final AsyncCallback<SafeHtml> callback) {
                if ("Hosts".equals(data.getTitle())) {
                    contentFactory.createContent(PreviewContent.INSTANCE.runtime_hosts(), callback);
                } else if ("Server Groups".equals(data.getTitle())) {
                    contentFactory.createContent(PreviewContent.INSTANCE.runtime_server_groups(), callback);
                }
            }
        });

        Widget browseWidget = browseColumn.asWidget();
        columnManager.addWest(browseWidget);
        columnManager.addWest(hostColWidget);
        columnManager.addWest(groupsColWidget);

        columnManager.setInitialVisible(1);


        browseColumn.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                columnManager.reduceColumnsTo(1);
                if (browseColumn.hasSelectedItem()) {
                    columnManager.updateActiveSelection(browseWidget);

                    //clearNestedPresenter();

                    /*presenter.getPlaceManager().revealPlace(
                            new PlaceRequest(NameTokens.HostMgmtPresenter)
                    );*/

                    browseColumn.getSelectedItem().getCmd().execute();
                }
            }
        });

        List<FinderItem> defaults = new ArrayList<>();
        defaults.add(
                new FinderItem(
                        "Hosts",
                        new Command() {
                            @Override
                            public void execute() {
                                columnManager.appendColumn(hostColWidget);
                                Console.getCircuit().dispatch(new FilterType(FilterType.HOST));
                            }
                        },
                        true)
        );

        defaults.add(
                new FinderItem(
                        "Server Groups", new Command() {
                    @Override
                    public void execute() {
                        columnManager.appendColumn(groupsColWidget);
                        Console.getCircuit().dispatch(new FilterType(FilterType.GROUP));
                    }
                }, true)
        );

        browseColumn.updateFrom(defaults);

        layout.add(contentCanvas);

        // selection handling
        hosts.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                columnManager.reduceColumnsTo(2);

                if (hosts.hasSelectedItem()) {

                    final String selectedHost = hosts.getSelectedItem();
                    columnManager.updateActiveSelection(hostColWidget);

                    openNestedPresenter(NameTokens.DomainRuntimePresenter);

                    Scheduler.get().scheduleDeferred(
                            new Scheduler.ScheduledCommand() {
                                @Override
                                public void execute() {
                                    Console.getCircuit().dispatch(new HostSelection(selectedHost));
                                }
                            }
                    );
                }
            }
        });

        hosts.setMenuItems(
                new MenuDelegate<String>(          // TODO permissions
                        "JVM", new ContextualCommand<String>() {
                    @Override
                    public void executeOn(final String host) {
                        Console.getPlaceManager().revealRelativePlace(
                                new PlaceRequest(NameTokens.HostJVMPresenter)
                        );
                    }
                }),
                new MenuDelegate<String>(          // TODO permissions
                        "Properties", new ContextualCommand<String>() {
                    @Override
                    public void executeOn(final String host) {
                        Console.getPlaceManager().revealRelativePlace(
                                new PlaceRequest(NameTokens.HostPropertiesPresenter)
                        );
                    }
                }),
                new MenuDelegate<String>(          // TODO permissions
                        "Interfaces", new ContextualCommand<String>() {
                    @Override
                    public void executeOn(final String host) {
                        Console.getPlaceManager().revealRelativePlace(
                                new PlaceRequest(NameTokens.HostInterfacesPresenter)
                        );
                    }
                })


        );

        groups.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                columnManager.reduceColumnsTo(2);

                if (groups.hasSelectedItem()) {

                    final ServerGroupRecord selectedGroup = groups.getSelectedItem();

                    columnManager.updateActiveSelection(groupsColWidget);

                    openNestedPresenter(NameTokens.DomainRuntimePresenter);

                    Scheduler.get().scheduleDeferred(
                            new Scheduler.ScheduledCommand() {
                                @Override
                                public void execute() {
                                    Console.getCircuit().dispatch(new GroupSelection(selectedGroup.getName()));
                                }
                            }
                    );
                }
            }
        });

        groups.setMenuItems(
                new MenuDelegate<ServerGroupRecord>(          // TODO permissions
                        Console.CONSTANTS.common_label_view(), new ContextualCommand<ServerGroupRecord>() {
                    @Override
                    public void executeOn(final ServerGroupRecord group) {

                        //groups.selectByKey(group.getName());
                        Console.getCircuit().dispatch(new GroupSelection(group.getName()));

                        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                            @Override
                            public void execute() {
                                Console.getPlaceManager().revealRelativePlace(
                                        new PlaceRequest(NameTokens.ServerGroupPresenter).with("action", "edit")
                                );
                            }
                        });

                    }
                }),
                new MenuDelegate<ServerGroupRecord>(          // TODO permissions  "/server-group=*", "remove"
                        Console.CONSTANTS.common_label_delete(),
                        new ContextualCommand<ServerGroupRecord>() {
                            @Override
                            public void executeOn(final ServerGroupRecord group) {

                                Console.getCircuit().dispatch(new GroupSelection(group.getName()));

                                Feedback.confirm(
                                        Console.MESSAGES.deleteServerGroup(),
                                        Console.MESSAGES.deleteServerGroupConfirm(group.getName()),
                                        new Feedback.ConfirmationHandler() {
                                            @Override
                                            public void onConfirmation(boolean isConfirmed) {
                                                if (isConfirmed)
                                                    presenter.onDeleteGroup(group);
                                            }
                                        });
                            }
                        }, MenuDelegate.Role.Operation),
                new MenuDelegate<ServerGroupRecord>(          // TODO permissions   "/server-group=*", "add"
                        Console.CONSTANTS.common_label_copy(), new ContextualCommand<ServerGroupRecord>() {
                    @Override
                    public void executeOn(final ServerGroupRecord group) {
                        Console.getCircuit().dispatch(new GroupSelection(group.getName()));
                        presenter.launchCopyWizard(group);
                    }
                }, MenuDelegate.Role.Operation),
                new MenuDelegate<ServerGroupRecord>(
                        Console.CONSTANTS.common_label_start(), new ContextualCommand<ServerGroupRecord>() {
                    @Override
                    public void executeOn(final ServerGroupRecord group) {


                        Feedback.confirm(
                                Console.CONSTANTS.startServerGroup(),
                                Console.MESSAGES.wantToStartServerGroup(group.getName()),
                                new Feedback.ConfirmationHandler() {
                                    @Override
                                    public void onConfirmation(boolean isConfirmed) {
                                        if (isConfirmed)
                                            presenter.onGroupLifecycle(group.getName(), LifecycleOperation.START);
                                    }
                                }
                        );
                    }
                }, MenuDelegate.Role.Operation)
                        .setOperationAddress("/server-group=*", "start-servers"),

                new MenuDelegate<ServerGroupRecord>(
                        Console.CONSTANTS.common_label_stop(), new ContextualCommand<ServerGroupRecord>() {
                    @Override
                    public void executeOn(final ServerGroupRecord group) {

                        Feedback.confirm(
                                Console.CONSTANTS.stopServerGroup(),
                                Console.MESSAGES.wantToStopServerGroup(group.getName()),
                                new Feedback.ConfirmationHandler() {
                                    @Override
                                    public void onConfirmation(boolean isConfirmed) {
                                        if (isConfirmed)
                                            presenter.onGroupLifecycle(group.getName(), LifecycleOperation.STOP);
                                    }
                                }
                        );

                    }
                }, MenuDelegate.Role.Operation)
                        .setOperationAddress("/server-group=*", "stop-servers"),

                new MenuDelegate<ServerGroupRecord>(
                        Console.CONSTANTS.suspend(), new ContextualCommand<ServerGroupRecord>() {
                    @Override
                    public void executeOn(final ServerGroupRecord group) {

                        presenter.onLaunchSuspendDialogue(group);

                    }
                }, MenuDelegate.Role.Operation)
                        .setOperationAddress("/server-group=*", "suspend-servers"),

                new MenuDelegate<ServerGroupRecord>(
                        Console.CONSTANTS.resume(), new ContextualCommand<ServerGroupRecord>() {
                    @Override
                    public void executeOn(final ServerGroupRecord group) {

                        Feedback.confirm(
                                Console.CONSTANTS.resumeServerGroup(),
                                Console.MESSAGES.wantToResumeServerGroup(group.getName()),
                                new Feedback.ConfirmationHandler() {
                                    @Override
                                    public void onConfirmation(boolean isConfirmed) {
                                        if (isConfirmed)
                                            presenter.onGroupLifecycle(group.getName(), LifecycleOperation.RESUME);
                                    }
                                }
                        );

                    }
                }, MenuDelegate.Role.Operation)
                        .setOperationAddress("/server-group=*","resume-servers"),

                new MenuDelegate<ServerGroupRecord>(
                        Console.CONSTANTS.common_label_restart(), new ContextualCommand<ServerGroupRecord>() {
                    @Override
                    public void executeOn(final ServerGroupRecord group) {

                        Feedback.confirm(
                                Console.CONSTANTS.restartServerGroup(),
                                Console.MESSAGES.wantToRestartServerGroup(group.getName()),
                                new Feedback.ConfirmationHandler() {
                                    @Override
                                    public void onConfirmation(boolean isConfirmed) {
                                        if (isConfirmed)
                                            presenter.onGroupLifecycle(group.getName(), LifecycleOperation.RESTART);
                                    }
                                }
                        );

                    }
                }, MenuDelegate.Role.Operation)
                        .setOperationAddress("/server-group=*","restart-servers"),

                new MenuDelegate<ServerGroupRecord>(
                        Console.CONSTANTS.common_label_reload(), new ContextualCommand<ServerGroupRecord>() {
                    @Override
                    public void executeOn(final ServerGroupRecord group) {

                        Feedback.confirm(
                                Console.CONSTANTS.reloadServerGroup(),
                                Console.MESSAGES.wantToReloadServerGroup(group.getName()),
                                new Feedback.ConfirmationHandler() {
                                    @Override
                                    public void onConfirmation(boolean isConfirmed) {
                                        if (isConfirmed)
                                            presenter.onGroupLifecycle(group.getName(), LifecycleOperation.RELOAD);
                                    }
                                }
                        );

                    }
                }, MenuDelegate.Role.Operation)
                        .setOperationAddress("/server-group=*","reload-servers")
        );

    }

    private void openNestedPresenter(String token) {
        PlaceManager placeManager = presenter.getPlaceManager();
        List<PlaceRequest> next = new ArrayList<PlaceRequest>(2);
        next.add(placeManager.getCurrentPlaceHierarchy().get(0));
        next.add(new PlaceRequest(token));
        placeManager.revealPlaceHierarchy(next);
    }

    @Override
    public void onClearActiveSelection(ClearFinderSelectionEvent event) {
        hostColWidget.getElement().removeClassName("active");
        groupsColWidget.getElement().removeClassName("active");
    }

    @Override
    public Widget createWidget() {

        /*ScrollPanel scroll = new ScrollPanel();
        scroll.setSize( "100%", "100%" );
        //scroll.getElement().setAttribute("style", "overflow-x:auto");
        scroll.getElement().setId("scrolling");

        scroll.setWidget(layout);
        return scroll;*/

        //layout.getElement().setAttribute("style", "overflow-x:auto");
        //layout.getElement().setId("scrolling");
        //layout.setWidth("2000px");
        startupContent();
        return layout;

    }

    @Override
    public void setInSlot(Object slot, IsWidget content) {
        if (slot == HostMgmtPresenter.TYPE_MainContent) {
            if(content!=null) {
                Widget w = content.asWidget();
                w.getElement().setAttribute("presenter-view", "true");
                setContent(w);
            }
            else
                contentCanvas.clear();
        }
    }

    private void setContent(IsWidget newContent) {
        contentCanvas.clear();
        contentCanvas.add(newContent);
    }

    private void clearNestedPresenter() {

        presenter.clearSlot(HostMgmtPresenter.TYPE_MainContent);

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                if(presenter.getPlaceManager().getHierarchyDepth()>1)
                    presenter.getPlaceManager().revealRelativePlace(1);
            }
        });
    }

    private void startupContent() {
        contentFactory.createContent(PreviewContent.INSTANCE.runtime_empty_domain(),
                new SimpleCallback<SafeHtml>() {
                    @Override
                    public void onSuccess(SafeHtml previewContent) {
                        contentCanvas.add(new HTML(previewContent));
                    }
                }
        );
    }


    @Override
    public void preview(SafeHtml html) {

        if (
                (contentCanvas.getWidgetCount()>0  && !contentCanvas.getWidget(0).getElement().hasAttribute("presenter-view"))
                    || (contentCanvas.getWidgetCount() ==0)
                ) {
            Scheduler.get().scheduleDeferred(() -> {
                contentCanvas.clear();
                contentCanvas.add(new HTML(html));
            });
        }
    }

    @Override
    public void setPresenter(HostMgmtPresenter presenter) {

        this.presenter = presenter;
    }

    @Override
    public void updateHosts(String selectedHost, Set<String> hostNames) {
        // TODO API Compatibility: remove need for list wrapper
        hosts.updateFrom(new ArrayList<String>(hostNames), false);
    }



    @Override
    public void updateServerGroups(List<ServerGroupRecord> serverGroups) {
        clearNestedPresenter();
        groups.updateFrom(serverGroups, false);
    }

    @Override
    public void toggleScrolling(boolean enforceScrolling, int requiredWidth) {
        columnManager.toogleScrolling(enforceScrolling, requiredWidth);
    }
}

