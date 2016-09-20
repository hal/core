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

package org.jboss.as.console.client.shared.subsys.activemq;

import java.util.List;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqCoreQueue;
import org.jboss.as.console.client.widgets.ContentDescription;

/**
 * @author Claudio Miranda
 */
public class CoreQueueEditor {

    private MsgDestinationsPresenter presenter;
    private CoreQueueList queueList;

    private HTML serverName;

    public CoreQueueEditor(MsgDestinationsPresenter presenter) {
        this.presenter = presenter;
    }

    public Widget asWidget() {
        LayoutPanel layout = new LayoutPanel();

        VerticalPanel panel = new VerticalPanel();
        panel.setStyleName("rhs-content-panel");

        ScrollPanel scroll = new ScrollPanel(panel);
        layout.add(scroll);
        layout.setWidgetTopHeight(scroll, 0, Style.Unit.PX, 100, Style.Unit.PCT);

        serverName = new HTML("Replace me");
        serverName.setStyleName("content-header-label");

        panel.add(serverName);
        panel.add(new ContentDescription("Configuration for core queues."));

        TabPanel bottomLayout = new TabPanel();
        bottomLayout.addStyleName("default-tabpanel");
        bottomLayout.addStyleName("master_detail-detail");

        queueList = new CoreQueueList(presenter);
        bottomLayout.add(queueList.asWidget(), "Queues");

        bottomLayout.selectTab(0);

        panel.add(bottomLayout);

        return layout;
    }

    public void setQueues(List<ActivemqCoreQueue> queues) {
        queueList.setQueues(queues);
        serverName.setText("Core Queues: Provider " + presenter.getCurrentServer());
    }

}
