package org.jboss.as.console.client.domain.hosts;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.domain.model.Server;
import org.jboss.as.console.client.v3.stores.domain.HostStore;
import org.jboss.as.console.client.v3.stores.domain.ServerStore;
import org.jboss.as.console.client.v3.stores.domain.actions.HostSelection;
import org.jboss.as.console.client.v3.stores.domain.actions.SelectServer;
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
    private final NavigationColumn<Server> server;
    private final HostStore hostStore;
    private final ServerStore serverStore;

    private SplitLayoutPanel layout;
    private LayoutPanel contentCanvas;
    private HostMgmtPresenter presenter;

    interface Template extends SafeHtmlTemplates {
        @Template("<div class=\"{0}\">{1}</div>")
        SafeHtml item(String cssClass, String title);
    }

    interface ServerTemplate extends SafeHtmlTemplates {
            @Template("<div class=\"{0}\">{1}&nbsp;<span style='font-size:8px'>({2})</span></div>")
            SafeHtml item(String cssClass, String server, String host);
        }

    private static final Template TEMPLATE = GWT.create(Template.class);
    private static final ServerTemplate SERVER_TEMPLATE = GWT.create(ServerTemplate.class);

    @Inject
    public ColumnHostView(final HostStore hostStore, final ServerStore serverStore) {
        super();
        this.hostStore = hostStore;
        this.serverStore = serverStore;

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
                });

        server = new NavigationColumn<Server>(
                "Server",
                new NavigationColumn.Display<Server>() {
                    @Override
                    public SafeHtml render(String baseCss, Server data) {
                        return SERVER_TEMPLATE.item(baseCss, data.getName(), data.getHostName());
                    }
                },
                new ProvidesKey<Server>() {
                    @Override
                    public Object getKey(Server item) {
                        return item.getName()+item.getHostName();
                    }
                });

        layout.addWest(hosts.asWidget(), 217);
        layout.addWest(server.asWidget(), 217);
        layout.add(contentCanvas);

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

        server.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                if (server.hasSelectedItem()) {

                    final Server selectedServer = server.getSelectedItem();

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
        this.presenter = presenter;
    }

    @Override
    public void updateHosts(String selectedHost, Set<String> hostNames) {
        // TODO API Compatibility: remove need for list wrapper
        hosts.updateFrom(new ArrayList<String>(hostNames), true);
    }

    @Override
    public void updateServer(List<Server> serverModel) {
        server.updateFrom(serverModel, true);
    }

    @Override
    public void onSelectedNavTree(LHSHighlightEvent event) {
        //server.selectByKey(event.getToken());
    }

}

