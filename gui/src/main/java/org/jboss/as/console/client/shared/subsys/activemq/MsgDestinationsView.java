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

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.SuspendableViewImpl;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqAddressingPattern;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqConnectionFactory;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqDivert;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqJMSEndpoint;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqMessagingProvider;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqQueue;
import org.jboss.as.console.client.shared.subsys.activemq.model.ActivemqSecurityPattern;
import org.jboss.as.console.client.widgets.pages.PagedView;
import org.jboss.ballroom.client.widgets.tabs.FakeTabPanel;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 5/10/11
 */
public class MsgDestinationsView extends SuspendableViewImpl implements MsgDestinationsPresenter.MyView, MsgDestinationsPresenter.JMSView{

    private MsgDestinationsPresenter presenter;

    private JMSEditor jmsEditor;
    private ConnectionFactoryList connectionFactories;
    private SecurityDetails securitySettings;
    private AddressingDetails addressingSettings;
    private DivertList divertList;

    @Override
    public Widget createWidget() {

        LayoutPanel layout = new LayoutPanel();

        FakeTabPanel titleBar = new FakeTabPanel("Messaging Destinations");
        layout.add(titleBar);

        PagedView panel = new PagedView(true);

        jmsEditor = new JMSEditor(presenter);
        connectionFactories = new ConnectionFactoryList(presenter);
        securitySettings = new SecurityDetails(presenter);
        addressingSettings = new AddressingDetails(presenter);
        divertList = new DivertList(presenter);

        panel.addPage("Queues/Topics", jmsEditor.asWidget()) ;
        panel.addPage("Connection Factories", connectionFactories.asWidget()) ;
        panel.addPage("Security Settings", securitySettings.asWidget()) ;
        panel.addPage("Address Settings", addressingSettings.asWidget()) ;
        panel.addPage("Diverts", divertList.asWidget()) ;

        // default page
        panel.showPage(0);

        Widget panelWidget = panel.asWidget();
        layout.add(panelWidget);

        layout.setWidgetTopHeight(titleBar, 0, Style.Unit.PX, 40, Style.Unit.PX);
        layout.setWidgetTopHeight(panelWidget, 40, Style.Unit.PX, 100, Style.Unit.PCT);

        return layout;
    }

    @Override
    public void setPresenter(MsgDestinationsPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setProviderDetails(ActivemqMessagingProvider provider) {
        //providerList.setProviderDetails(provider);
    }

    @Override
    public void setQueues(List<ActivemqQueue> queues) {
        jmsEditor.setQueues(queues);
    }

    @Override
    public void setTopics(List<ActivemqJMSEndpoint> topics) {
        jmsEditor.setTopics(topics);
    }

    @Override
    public void enableEditQueue(boolean b) {
        jmsEditor.enableEditQueue(b);
    }

    @Override
    public void enableEditTopic(boolean b) {
        jmsEditor.enableEditTopic(b);
    }

    @Override
    public void setSecurityConfig(List<ActivemqSecurityPattern> secPatterns) {
        securitySettings.setSecurityConfig(secPatterns);
    }

    @Override
    public void setAddressingConfig(List<ActivemqAddressingPattern> addrPatterns) {
        addressingSettings.setAddressingConfig(addrPatterns);
    }

    @Override
    public void setProvider(List<Property> result) {
    }

    @Override
    public void setConnectionFactories(List<ActivemqConnectionFactory> factories) {
        connectionFactories.setFactories(factories);
    }

    @Override
    public void setDiverts(List<ActivemqDivert> diverts) {
        divertList.setDiverts(diverts);
    }

    @Override
    public void setSelectedProvider(String selectedProvider) {
        presenter.loadDetails();
    }
}
