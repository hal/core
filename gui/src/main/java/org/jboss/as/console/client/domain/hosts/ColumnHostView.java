package org.jboss.as.console.client.domain.hosts;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.StackLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.domain.model.ProfileRecord;
import org.jboss.as.console.client.domain.model.ServerGroupRecord;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.v3.stores.domain.ServerStore;
import org.jboss.as.console.client.v3.stores.domain.actions.FilterType;
import org.jboss.as.console.client.v3.stores.domain.actions.GroupSelection;
import org.jboss.as.console.client.v3.stores.domain.actions.HostSelection;
import org.jboss.as.console.client.widgets.nav.v3.NavigationColumn;
import org.jboss.ballroom.client.layout.LHSHighlightEvent;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Heiko Braun
 * @since 09/01/15
 */
public class ColumnHostView extends SuspendableViewImpl
        implements HostMgmtPresenter.MyView, LHSHighlightEvent.NavItemSelectionHandler {

    private final NavigationColumn<String> hosts;
    private final NavigationColumn<ServerGroupRecord> groups;

    private SplitLayoutPanel layout;
    private LayoutPanel contentCanvas;

    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title);
    }

    private static final Template TEMPLATE = GWT.create(Template.class);

    @Inject
    public ColumnHostView(final HostStore hostStore, final ServerStore serverStore) {
        super();

        contentCanvas = new LayoutPanel();
        layout = new SplitLayoutPanel(2);
        hosts = new NavigationColumn<String>(
                "Hosts",
                new NavigationColumn.Display<String>() {
                    @Override
                    public SafeHtml render(String baseCss, String data) {
                        return TEMPLATE.item(baseCss, data);
                    }
                },
                new ProvidesKey<String>() {
                    @Override
                    public Object getKey(String item) {
                        return item;
                    }
                }).setPlain(true);


        groups = new NavigationColumn<ServerGroupRecord>(
                "Server Groups",
                new NavigationColumn.Display<ServerGroupRecord>() {
                    @Override
                    public SafeHtml render(String baseCss, ServerGroupRecord data) {
                        return TEMPLATE.item(baseCss, data.getName());
                    }
                },
                new ProvidesKey<String>() {
                    @Override
                    public Object getKey(String item) {
                        return item;
                    }
                }).setPlain(true);

        StackLayoutPanel stack = new StackLayoutPanel(Style.Unit.PX);

        HTML hostsHeader = new HTML("Hosts");
        hostsHeader.addStyleName("server-picker-section-header");

        final HTML groupsHeader = new HTML("Server Groups");
        groupsHeader.addStyleName("server-picker-section-header");
        groupsHeader.getElement().setAttribute("style", "border-top: 1px solid #CFCFCF");

        stack.add(hosts.asWidget(), hostsHeader, 40);
        stack.add(groups.asWidget(), groupsHeader, 40);


        stack.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> event) {
                if(event.getSelectedItem()>0)
                {
                    // server groups selected
                    groupsHeader.getElement().removeAttribute("style");
                    Console.getCircuit().dispatch(new FilterType(FilterType.GROUP));

                }
                else {

                    // hosts selected
                    groupsHeader.getElement().setAttribute("style", "border-top: 1px solid #CFCFCF");
                    Console.getCircuit().dispatch(new FilterType(FilterType.HOST));

                }
            }
        });

        layout.addWest(stack, 217);
        //layout.addWest(server.asWidget(), 217);
        layout.add(contentCanvas);

        // selection handling
        hosts.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                if (hosts.hasSelectedItem()) {

                    final String selectedHost = hosts.getSelectedItem();

                    if(!hostStore.getSelectedHost().equals(selectedHost)) {

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
            }
        });

        groups.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                if (groups.hasSelectedItem()) {

                    final ServerGroupRecord selectedGroup = groups.getSelectedItem();

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
    public void setPresenter(HostMgmtPresenter presenter) {

    }

    @Override
    public void updateHosts(String selectedHost, Set<String> hostNames) {
        // TODO API Compatibility: remove need for list wrapper
        hosts.updateFrom(new ArrayList<String>(hostNames), true);
    }

    @Override
    public void onSelectedNavTree(LHSHighlightEvent event) {
        //server.selectByKey(event.getToken());
    }

    @Override
    public void updateProfiles(List<ProfileRecord> profiles) {

    }

    @Override
    public void updateSocketBindings(List<String> socketBindings) {

    }

    @Override
    public void setServerGroups(List<ServerGroupRecord> serverGroups) {
        groups.updateFrom(serverGroups, true);
    }
}

