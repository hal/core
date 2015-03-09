package org.jboss.as.console.client.domain.hosts;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.StackLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.domain.model.ProfileRecord;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.domain.model.impl.LifecycleOperation;
import org.jboss.as.console.client.domain.topology.ServerGroup;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.v3.stores.domain.ServerStore;
import org.jboss.as.console.client.v3.stores.domain.actions.FilterType;
import org.jboss.as.console.client.v3.stores.domain.actions.GroupSelection;
import org.jboss.as.console.client.v3.stores.domain.actions.HostSelection;
import org.jboss.as.console.client.widgets.nav.v3.ClearFinderSelectionEvent;
import org.jboss.as.console.client.widgets.nav.v3.ContextualCommand;
import org.jboss.as.console.client.widgets.nav.v3.FinderColumn;
import org.jboss.as.console.client.widgets.nav.v3.MenuDelegate;
import org.jboss.as.console.client.widgets.nav.v3.PreviewFactory;
import org.jboss.ballroom.client.layout.LHSHighlightEvent;
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
    private final HorizontalPanel groupsHeader;
    private final HTML headerTitle;
    private final HTML addGroupBtn;
    private final Widget hostColWidget;
    private final Widget groupsColWidget;

    private SplitLayoutPanel layout;
    private LayoutPanel contentCanvas;
    private HostMgmtPresenter presenter;

    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\"><i class='icon-folder-close-alt' style='display:none'></i>&nbsp;{1}</div>")
        SafeHtml item(String cssClass, String title);
    }

    private static final Template TEMPLATE = GWT.create(Template.class);

    @Inject
    public ColumnHostView(final HostStore hostStore, final ServerStore serverStore) {
        super();

        Console.getEventBus().addHandler(ClearFinderSelectionEvent.TYPE, this);

        contentCanvas = new LayoutPanel();

        layout = new SplitLayoutPanel(2);

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
                }).setPlain(true);


        hosts.setPreviewFactory(new PreviewFactory<String>() {
            @Override
            public SafeHtml createPreview(String data) {

                SafeHtmlBuilder html = new SafeHtmlBuilder();
                html.appendHtmlConstant("<h2>").appendEscaped("Host Configuration").appendHtmlConstant("</h2>");
                html.appendEscaped("One of the primary new features of WildFly 8 is the ability to manage multiple WildFly instances from a single control point. A collection of such servers is referred to as the members of a \"domain\" with a single Domain Controller process acting as the central management control point. All of the WildFly 8 instances in the domain share a common management policy, with the Domain Controller acting to ensure that each server is configured according to that policy. Domains can span multiple physical (or virtual) machines, with all WildFly 8 instances on a given host under the control of a special Host Controller process. One Host Controller instance is configured to act as the central Domain Controller. The Host Controller on each host interacts with the Domain Controller to control the lifecycle of the application server instances running on its host and to assist the Domain Controller in managing them.");
                return html.toSafeHtml();
            }
        });

        hosts.setComparisonType("filter");

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
                }).setPlain(true);

        groups.setComparisonType("filter");

        groups.setPreviewFactory(new PreviewFactory<ServerGroupRecord>() {
            @Override
            public SafeHtml createPreview(ServerGroupRecord data) {

                SafeHtmlBuilder html = new SafeHtmlBuilder();
                html.appendHtmlConstant("<h2>").appendEscaped("Server Groups").appendHtmlConstant("</h2>");
                html.appendEscaped("A server group is set of server instances that will be managed and configured as one. In a managed domain each application server instance is a member of a server group. (Even if the group only has a single server, the server is still a member of a group.) It is the responsibility of the Domain Controller and the Host Controllers to ensure that all servers in a server group have a consistent configuration. They should all be configured with the same profile and they should have the same deployment content deployed.");
                return html.toSafeHtml();
            }
        });


        StackLayoutPanel stack = new StackLayoutPanel(Style.Unit.PX);
        stack.setAnimationDuration(0);

        HTML hostsHeader = new HTML("Hosts");
        hostsHeader.addStyleName("server-picker-section-header");

        groupsHeader = new HorizontalPanel();
        groupsHeader.addStyleName("fill-layout-width");
        groupsHeader.addStyleName("server-picker-section-header");
        groupsHeader.getElement().setAttribute("style", "border-top: 1px solid #CFCFCF");

        headerTitle = new HTML("Server Groups");
        headerTitle.getElement().setAttribute("style", "height:25px");
        groupsHeader.add(headerTitle);

        // add server groups
        addGroupBtn = new HTML("<i class=\"icon-plus\" style='color:black'></i>&nbsp;New");
        addGroupBtn.getElement().setAttribute("style", "color:#0099D3; cursor:pointer;padding-right:5px");
        addGroupBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {


                // TODO "/server-group=*", "add" permission

                presenter.launchNewGroupDialog();
            }
        });

        addGroupBtn.setVisible(false);
        groupsHeader.add(addGroupBtn);
        addGroupBtn.getElement().getParentElement().setAttribute("align", "right");


        hostColWidget = hosts.asWidget();
        groupsColWidget = groups.asWidget();

        stack.add(hostColWidget, hostsHeader, 40);
        stack.add(groupsColWidget, groupsHeader, 40);

        stack.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> event) {
                if(event.getSelectedItem()>0)
                {
                    // server groups selected
                    groupsHeader.getElement().removeAttribute("style");
                    Console.getCircuit().dispatch(new FilterType(FilterType.GROUP));

                    Scheduler.get().scheduleDeferred(
                            new Scheduler.ScheduledCommand() {
                                @Override
                                public void execute() {
                                    addGroupBtn.setVisible(true);
                                }
                            }
                    );

                }
                else {

                    // hosts selected
                    groupsHeader.getElement().setAttribute("style", "border-top: 1px solid #CFCFCF");
                    Console.getCircuit().dispatch(new FilterType(FilterType.HOST));
                    addGroupBtn.setVisible(false);

                }
            }
        });

        layout.addWest(stack, 217);
        layout.add(contentCanvas);


        // selection handling
        hosts.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                if (hosts.hasSelectedItem()) {

                    final String selectedHost = hosts.getSelectedItem();
                    groupsColWidget.getElement().removeClassName("active");
                    hostColWidget.getElement().addClassName("active");

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
                        "JVM Settings", new ContextualCommand<String>() {
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

                if (groups.hasSelectedItem()) {

                    final ServerGroupRecord selectedGroup = groups.getSelectedItem();

                    hostColWidget.getElement().removeClassName("active");
                    groupsColWidget.getElement().addClassName("active");

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
        return layout.asWidget();
    }

    @Override
    public void setInSlot(Object slot, IsWidget content) {
        if (slot == HostMgmtPresenter.TYPE_MainContent) {
            if(content!=null)
                setContent(content);
        }
    }

    private void setContent(IsWidget newContent) {
        contentCanvas.clear();
        contentCanvas.add(newContent);
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
    public void updateProfiles(List<ProfileRecord> profiles) {

    }

    @Override
    public void updateSocketBindings(List<String> socketBindings) {

    }

    @Override
    public void updateServerGroups(List<ServerGroupRecord> serverGroups) {
        groups.updateFrom(serverGroups, false);
    }
}

