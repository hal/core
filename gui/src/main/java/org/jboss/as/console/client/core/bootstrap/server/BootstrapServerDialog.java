package org.jboss.as.console.client.core.bootstrap.server;

/**
 * Modal dialog to manage bootstrap servers. The dialog offers a page to connect to an existing server and a page to
 * add new servers.
 *
 * @author Harald Pehl
 */
public class BootstrapServerDialog {
    private final BootstrapServerSetup serverSetup;
    private ConnectPage connectPage;
    private ConfigurePage configurePage;


    public BootstrapServerDialog(final BootstrapServerSetup serverSetup) {
        this.serverSetup = serverSetup;
        initUI();
    }

    public void initUI() {
        connectPage = new ConnectPage(serverSetup);
        configurePage = new ConfigurePage(serverSetup);
    }

    public ConnectPage getConnectPage() {
        return connectPage;
    }

    public ConfigurePage getConfigurePage() {
        return configurePage;
    }
}
