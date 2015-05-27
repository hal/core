package org.jboss.as.console.client.domain.hosts;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.impl.LifecycleOperation;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.v3.stores.domain.ServerStore;
import org.jboss.as.console.client.v3.stores.domain.actions.FilterType;
import org.jboss.as.console.client.v3.stores.domain.actions.GroupSelection;
import org.jboss.as.console.client.v3.stores.domain.actions.HostSelection;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
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
        @Template("<div class=\"{0}\"><i class='{1}' style='display:none'></i>&nbsp;{2}</span></div>")
        SafeHtml item(String cssClass, String icon, String title);
    }

    private static final StatusTemplate STATUS_TEMPLATE = GWT.create(StatusTemplate.class);

    @Inject
    public ColumnHostView(final HostStore hostStore, final ServerStore serverStore) {
        super();

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
                });


      /*  hosts.setTopMenuItems(
                new MenuDelegate<String>(          // TODO permissions
                        "Patching", new ContextualCommand<String>() {
                    @Override
                    public void executeOn(final String host) {
                        Console.getPlaceManager().revealRelativePlace(
                                new PlaceRequest(NameTokens.PatchingPresenter)
                        );
                    }
                })
        );*/

        hosts.setPreviewFactory(new PreviewFactory<String>() {
            @Override
            public void createPreview(String data, AsyncCallback<SafeHtml> callback) {
                SafeHtmlBuilder html = new SafeHtmlBuilder();
                html.appendHtmlConstant("<div class='preview-content'><h2>").appendEscaped("Host Configuration").appendHtmlConstant("</h2>");
                html.appendEscaped("One of the primary new features of WildFly 8 is the ability to manage multiple WildFly instances from a single control point. A collection of such servers is referred to as the members of a \"domain\" with a single Domain Controller process acting as the central management control point. All of the WildFly 8 instances in the domain share a common management policy, with the Domain Controller acting to ensure that each server is configured according to that policy. Domains can span multiple physical (or virtual) machines, with all WildFly 8 instances on a given host under the control of a special Host Controller process. One Host Controller instance is configured to act as the central Domain Controller. The Host Controller on each host interacts with the Domain Controller to control the lifecycle of the application server instances running on its host and to assist the Domain Controller in managing them.");
                html.appendHtmlConstant("</div>");
                callback.onSuccess(html.toSafeHtml());
            }
        });

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
                });

        groups.setTopMenuItems(new MenuDelegate<ServerGroupRecord>("Add",
                        new ContextualCommand<ServerGroupRecord>() {
                            @Override
                            public void executeOn(ServerGroupRecord group) {
                                // TODO "/server-group=*", "add" permission
                                presenter.launchNewGroupDialog();
                            }
                        })
        );

        groups.setPreviewFactory(new PreviewFactory<ServerGroupRecord>() {
            @Override
            public void createPreview(ServerGroupRecord data, AsyncCallback<SafeHtml> callback) {
                SafeHtmlBuilder html = new SafeHtmlBuilder();
                html.appendHtmlConstant("<div class='preview-content'><h2>").appendEscaped("Server Groups").appendHtmlConstant("</h2>");
                html.appendEscaped("A server group is set of server instances that will be managed and configured as one. In a managed domain each application server instance is a member of a server group. (Even if the group only has a single server, the server is still a member of a group.) It is the responsibility of the Domain Controller and the Host Controllers to ensure that all servers in a server group have a consistent configuration. They should all be configured with the same profile and they should have the same deployment content deployed.");
                html.appendHtmlConstant("</div>");
                callback.onSuccess(html.toSafeHtml());
            }
        });

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
                });



        Widget browseWidget = browseColumn.asWidget();
        columnManager.addWest(browseWidget);
        columnManager.addWest(hostColWidget);
        columnManager.addWest(groupsColWidget);

        columnManager.setInitialVisible(1);


        browseColumn.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if(browseColumn.hasSelectedItem())
                {
                    columnManager.reduceColumnsTo(1);
                    columnManager.updateActiveSelection(browseWidget);

                    clearNestedPresenter();

                    presenter.getPlaceManager().revealPlace(
                            new PlaceRequest(NameTokens.HostMgmtPresenter)
                    );

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

                    presenter.getPlaceManager().revealPlace(
                            new PlaceRequest(NameTokens.DomainRuntimePresenter)
                    );

                    Scheduler.get().scheduleDeferred(
                            new Scheduler.ScheduledCommand() {
                                @Override
                                public void execute() {
                                    Console.getCircuit().dispatch(new HostSelection(selectedHost));
                                }
                            }
                    );

                }
                else
                {
                    clearNestedPresenter();
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

                    presenter.getPlaceManager().revealPlace(
                            new PlaceRequest(NameTokens.DomainRuntimePresenter)
                    );

                    Scheduler.get().scheduleDeferred(
                            new Scheduler.ScheduledCommand() {
                                @Override
                                public void execute() {
                                    Console.getCircuit().dispatch(new GroupSelection(selectedGroup.getName()));

                                }
                            }
                    );

                }
                else
                {
                    clearNestedPresenter();
                }
            }
        });

        groups.setMenuItems(
                new MenuDelegate<ServerGroupRecord>(          // TODO permissions
                        "Edit", new ContextualCommand<ServerGroupRecord>() {
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
                        "Remove",
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
                        }),
                new MenuDelegate<ServerGroupRecord>(          // TODO permissions   "/server-group=*", "add"
                        "Copy", new ContextualCommand<ServerGroupRecord>() {
                    @Override
                    public void executeOn(final ServerGroupRecord group) {
                        Console.getCircuit().dispatch(new GroupSelection(group.getName()));
                        presenter.launchCopyWizard(group);
                    }
                }),
                new MenuDelegate<ServerGroupRecord>(
                        "Start Group", new ContextualCommand<ServerGroupRecord>() {
                    @Override
                    public void executeOn(final ServerGroupRecord group) {
                        presenter.onGroupLifecycle(group.getName(), LifecycleOperation.START);
                    }
                }),
                new MenuDelegate<ServerGroupRecord>(
                        "Stop Group", new ContextualCommand<ServerGroupRecord>() {
                    @Override
                    public void executeOn(final ServerGroupRecord group) {

                        presenter.onGroupLifecycle(group.getName(), LifecycleOperation.STOP);
                    }
                })
        );

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
        return layout;

    }

    @Override
    public void setInSlot(Object slot, IsWidget content) {
        if (slot == HostMgmtPresenter.TYPE_MainContent) {
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

    @Override
    public void preview(SafeHtml html) {
        // TODO remove
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
        groups.updateFrom(serverGroups, false);
    }

    @Override
    public void toggleScrolling(boolean enforceScrolling, int requiredWidth) {
        columnManager.toogleScrolling(enforceScrolling, requiredWidth);
    }
}

