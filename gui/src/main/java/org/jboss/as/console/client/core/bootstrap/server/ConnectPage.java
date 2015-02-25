package org.jboss.as.console.client.core.bootstrap.server;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.ballroom.client.widgets.ContentHeaderLabel;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import java.util.List;

/**
 * @author Harald Pehl
 */
public class ConnectPage implements IsWidget {

    private final BootstrapServerSetup serverSetup;
    private final BootstrapServerDialog serverDialog;
    private final BootstrapServerStore serverStore;
    private BootstrapServerTable table;
    private HTML connectStatus;

    public ConnectPage(final BootstrapServerSetup serverSetup, final BootstrapServerDialog serverDialog) {
        this.serverSetup = serverSetup;
        this.serverDialog = serverDialog;
        this.serverStore = new BootstrapServerStore();
    }

    @Override
    public Widget asWidget() {
        FlowPanel content = new FlowPanel();
        content.add(new ContentHeaderLabel(Console.CONSTANTS.bs_connect_interface_header()));
        content.add(new ContentDescription(Console.CONSTANTS.bs_connect_interface_desc()));

        table = new BootstrapServerTable(serverDialog);
        content.add(table);

        connectStatus = new HTML();
        content.add(connectStatus);

        DialogueOptions options = new DialogueOptions(
                Console.CONSTANTS.bs_connect_interface_connect(),
                new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        final BootstrapServer server = table.getSelectedServer();
                        if (server == null) {
                            connectStatus.setHTML(StatusMessage.error(Console.CONSTANTS.bs_connect_interface_no_selection()));
                        } else {
                            serverSetup.pingServer(server, new AsyncCallback<Void>() {
                                @Override
                                public void onFailure(final Throwable caught) {
                                    connectStatus.setHTML(StatusMessage.warning(
                                            Console.MESSAGES.bs_interface_warning(serverSetup.getBaseUrl())));
                                }

                                @Override
                                public void onSuccess(final Void result) {
                                    serverSetup.onConnect(server);
                                }
                            });
                        }
                    }
                },
                Console.CONSTANTS.common_label_cancel(),
                new ClickHandler() {
                    @Override
                    public void onClick(final ClickEvent event) {
                        Window.Location.replace(GWT.getHostPageBaseURL());
                    }
                }
        );

        return new WindowContentBuilder(content, options).build();
    }

    void reset() {
        List<BootstrapServer> servers = serverStore.load();
        table.getDataProvider().setList(servers);
        BootstrapServer selection = serverStore.restoreSelection();
        if (selection != null) {
            table.select(selection);
        } else {
            table.getCellTable().selectDefaultEntity();
        }
        connectStatus.setVisible(false);
    }
}
