package org.jboss.as.console.client.core.bootstrap.server;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

import java.util.List;

/**
 * @author Harald Pehl
 * @date 02/28/2013
 */
public class ConnectPage implements IsWidget
{
    private final BootstrapServerSetup serverSetup;
    private final BootstrapServerStore bootstrapServerStore;
    private VerticalPanel page;
    private DialogueOptions options;
    private BootstrapServerTable table;

    public ConnectPage(final BootstrapServerSetup serverSetup)
    {
        this.serverSetup = serverSetup;
        this.bootstrapServerStore = new BootstrapServerStore();
        initUI();
    }

    private void initUI()
    {
        page = new VerticalPanel();
        page.setStyleName("window-content");

        Label description = new Label("Pick a server from the list below or add a new server");
        description.getElement().setAttribute("style", "padding-bottom:15px;");
        page.add(description);

        table = new BootstrapServerTable(serverSetup);
        page.add(table);

        final Label connectErrorMessages = new Label();
        connectErrorMessages.setStyleName("error-panel");
        page.add(connectErrorMessages);

        options = new DialogueOptions(
                "Connect",
                new ClickHandler()
                {
                    @Override
                    public void onClick(ClickEvent event)
                    {
                        final BootstrapServer server = table.getSelectedServer();
                        if (server == null)
                        {
                            connectErrorMessages.setText("Please select a server.");
                        }
                        else
                        {
                            serverSetup.pingServer(server, new AsyncCallback<Void>()
                            {
                                @Override
                                public void onFailure(final Throwable caught)
                                {
                                    connectErrorMessages.setText("The selected server is not running.");
                                }

                                @Override
                                public void onSuccess(final Void result)
                                {
                                    serverSetup.onConnect(server);
                                }
                            });
                        }
                    }
                },
                "",
                new ClickHandler()
                {
                    @Override
                    public void onClick(final ClickEvent event)
                    {
                        // not used
                    }
                }
        ).showCancel(false);
    }

    @Override
    public Widget asWidget()
    {
        return new WindowContentBuilder(page, options).build();
    }

    void reset()
    {
        List<BootstrapServer> servers = bootstrapServerStore.load();
        table.getDataProvider().setList(servers);
        table.getCellTable().selectDefaultEntity();
    }
}
