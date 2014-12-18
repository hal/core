package org.jboss.as.console.client.domain.runtime;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.layout.SimpleLayout;
import org.jboss.as.console.client.shared.homepage.ContentBox;
import org.jboss.as.console.client.tools.UUID;
import org.jboss.as.console.client.v3.stores.domain.actions.HostSelection;
import org.jboss.as.console.client.widgets.popups.ComboPicker;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.common.DefaultButton;
import org.jboss.gwt.circuit.Dispatcher;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Heiko Braun
 * @since 16/07/14
 */
public class NoServerView extends SuspendableViewImpl implements NoServerPresenter.MyView {


    private final Dispatcher circuit;
    private ContentHeaderLabel header;
    private ComboPicker hosts;
    private ContentBox choseHost;

    private PlaceManager placeManager;

    @Inject
    public NoServerView(PlaceManager placeManager, Dispatcher circuit) {
        this.placeManager = placeManager;
        this.circuit = circuit;
    }

    @Override
    public Widget createWidget() {

        header = new ContentHeaderLabel();


        SafeHtmlBuilder startServerDesc = new SafeHtmlBuilder();
        startServerDesc.appendEscaped("Start a server that can be monitored.").appendHtmlConstant("<p/>");

        ContentBox startServer = new ContentBox(
                UUID.uuid(), "Start Server",
                startServerDesc.toSafeHtml(),
                "Domain Overview", NameTokens.Topology
        );

        // ----

        SafeHtmlBuilder createServerDesc = new SafeHtmlBuilder();
        createServerDesc.appendEscaped("A server configuration does specify the overall configuration of a server. A server configuration can be started and perform work.").appendHtmlConstant("<p/>");

        ContentBox createServer = new ContentBox(
                UUID.uuid(), "Create Server",
                createServerDesc.toSafeHtml(),
                "Server Configurations", NameTokens.ServerPresenter
        );

        // -----

        SafeHtmlBuilder choseHostDesc = new SafeHtmlBuilder();
        choseHostDesc.appendEscaped("Only active servers can be monitored. Chose another host with active servers.").appendHtmlConstant("<p/>");


        hosts = new ComboPicker();
        Widget hWidget = hosts.asWidget();
        hWidget.getElement().setAttribute("style", "margin-bottom:10px");
        hWidget.getElement().addClassName("table-picker");

        VerticalPanel inner = new VerticalPanel();
        inner.setStyleName("fill-layout-width");
        inner.add(hWidget);
        inner.add(new DefaultButton("Proceed", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if(hosts.getSelectedValue()!=null && !hosts.getSelectedValue().equals(""))
                {

                    circuit.dispatch(new HostSelection(hosts.getSelectedValue()));

                    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                        @Override
                        public void execute() {
                            placeManager.revealPlace(new PlaceRequest(NameTokens.DomainRuntimePresenter));
                        }
                    });
                }
            }
        }));

        choseHost = new ContentBox(
                UUID.uuid(), "Change Host",
                choseHostDesc.toSafeHtml(),
                inner
        );

        // -----

        SimpleLayout layout = new SimpleLayout()
                .setHeadlineWidget(header)
                .setPlain(true)
                .setDescription("You can only monitor active server instances. Either there is no server configured for this host, or no server instance is running.")
                .addContent("", choseHost)
                .addContent("", createServer)
                .addContent("", startServer);

        return layout.build();
    }


    @Override
    public void setAvailableHosts(List<String> hostWithServers) {
        choseHost.setVisible(hostWithServers.size()>0);

        hosts.setValues(hostWithServers);
        hosts.setItemSelected(0, true, false);
    }

    @Override
    public void setHostName(String selectedHost) {
        header.setText("No server running at host '"+selectedHost+"'");
    }
}
