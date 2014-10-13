/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.console.client.shared.subsys.web;

import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.DisposableViewImpl;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.shared.subsys.web.model.HttpConnector;
import org.jboss.as.console.client.shared.subsys.web.model.VirtualServer;
import org.jboss.dmr.client.ModelNode;

import java.util.List;

/**
 * @author Heiko Braun
 */
public class WebSubsystemView extends DisposableViewImpl implements WebPresenter.MyView {

    private WebPresenter presenter;
    private ModelDrivenPanel globalsPanel;
    private ModelDrivenPanel jspPanel;
    private ConnectorList connectorList;
    private VirtualServerList serverList;

    @Override
    public Widget createWidget() {

        globalsPanel = new ModelDrivenPanel("{selected.profile}/subsystem=web", presenter);
        jspPanel = new ModelDrivenPanel("{selected.profile}/subsystem=web/configuration=jsp-configuration", presenter,
                "check-interval",
                "development",
                "disabled",
                "keep-generated",
                "display-source-fragment",
                "x-powered-by");
        connectorList = new ConnectorList(presenter);
        serverList = new VirtualServerList(presenter);

        OneToOneLayout layout = new OneToOneLayout()
                .setTitle("Servlet")
                .setHeadline("Servlet/HTTP Configuration")
                .setDescription(Console.CONSTANTS.subsys_web_desc())
                .addDetail("Global", globalsPanel)
                .addDetail("JSP", jspPanel)
                .addDetail("Connectors", connectorList.asWidget())
                .addDetail("Virtual Servers", serverList.asWidget());

        return layout.build();
    }

    @Override
    public void setPresenter(WebPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void updateGlobals(ModelNode globals) {
        globalsPanel.setData(globals);
    }

    @Override
    public void updateJsp(ModelNode jsp) {
        jspPanel.setData(jsp);
    }

    @Override
    public void setConnectors(List<HttpConnector> connectors) {
        connectorList.setConnectors(connectors);
    }

    @Override
    public void enableEditConnector(boolean b) {
        connectorList.setEnabled(b);
    }

    @Override
    public void setVirtualServers(List<VirtualServer> servers) {
        serverList.setVirtualServers(servers);
    }

    @Override
    public void enableEditVirtualServer(boolean b) {
        serverList.setEnabled(b);
    }

	@Override
	public void setSocketBindings(List<String> socketBindings) {
        connectorList.setSocketBindigs(socketBindings);
	}
}
