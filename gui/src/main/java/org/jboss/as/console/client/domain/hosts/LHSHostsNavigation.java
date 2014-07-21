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

package org.jboss.as.console.client.domain.hosts;

import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.ballroom.client.layout.LHSNavTree;
import org.jboss.ballroom.client.layout.LHSNavTreeItem;

import java.util.Set;

/**
 * @author Heiko Braun
 * @date 3/2/11
 */
class LHSHostsNavigation {

    private VerticalPanel layout;
    private VerticalPanel stack;

    private DisclosurePanel panel;
    private LHSNavTree hostTree;

    private HostSelector hostSelector;
    private ScrollPanel scroll;
    private LHSNavTree domainNav;
    private LHSNavTree hostNav;

    public LHSHostsNavigation() {


        layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");

        stack = new VerticalPanel();
        stack.setStyleName("fill-layout-width");

        domainNav = new LHSNavTree("hosts");
        domainNav.getElement().setAttribute("aria-label", "Domain Tasks");
        domainNav.getElement().setAttribute("style", "padding-top:10px; padding-bottom:10px");

        hostNav = new LHSNavTree("hosts");
        hostNav.getElement().setAttribute("aria-label", "Host Tasks");
        hostNav.getElement().setAttribute("style", "padding-top:10px; padding-bottom:10px");

        // --------

        HTML title = new HTML("Domain");
        title.setStyleName("domain-section-header");

        domainNav.addItem(new LHSNavTreeItem("Overview", NameTokens.Topology));
        domainNav.addItem(new LHSNavTreeItem("Server Groups", NameTokens.ServerGroupPresenter));

        hostSelector = new HostSelector();


        // --------


        LHSNavTreeItem serversItem = new LHSNavTreeItem(Console.CONSTANTS.common_label_serverConfigs(), NameTokens.ServerPresenter);
        hostNav.addItem(serversItem);
        hostNav.addItem(new LHSNavTreeItem("Patch Management", NameTokens.PatchingPresenter));

        // --------

        //LHSNavTreeItem paths = new LHSNavTreeItem(Console.CONSTANTS.common_label_paths(), "hosts/host-paths");
        LHSNavTreeItem jvms = new LHSNavTreeItem(Console.CONSTANTS.common_label_virtualMachines(), "host-jvms");
        LHSNavTreeItem interfaces = new LHSNavTreeItem(Console.CONSTANTS.common_label_interfaces(), "host-interfaces");
        LHSNavTreeItem properties = new LHSNavTreeItem("Host Properties", "host-properties");


        hostNav.addItem(jvms);
        hostNav.addItem(interfaces);
        hostNav.addItem(properties);


        // --------
        stack.add(title);
        stack.add(domainNav);

        stack.add(hostSelector.asWidget());
        stack.add(hostNav);

        domainNav.expandTopLevel();
        hostNav.expandTopLevel();

        // --------


        layout.add(stack);

        scroll = new ScrollPanel(layout);

    }

    public Widget asWidget()
    {
        return scroll;
    }

    public void setHosts(String selectedHost, Set<String> hostNames) {
        hostSelector.setHosts(selectedHost, hostNames);
        domainNav.expandTopLevel();
    }
}
