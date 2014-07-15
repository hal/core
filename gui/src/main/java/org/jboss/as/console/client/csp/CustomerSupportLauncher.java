/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.jboss.as.console.client.csp;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.core.ApplicationProperties;
import org.jboss.as.console.client.core.BootstrapContext;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;

public class CustomerSupportLauncher {

    private final BootstrapContext botstrapContext;

    public CustomerSupportLauncher(BootstrapContext bootstrap) {
        this.botstrapContext = bootstrap;
    }

    public Widget asWidget() {
        HTML button = new HTML("<div class='header-textlink'><i style='color:#cecece' class='icon-comment-alt'></i>&nbsp;Support</div>");
        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                openCSPView();
            }
        });
        button.getElement().setAttribute("style", "cursor:pointer");
        return button;
    }

    private void openCSPView() {
        final DefaultWindow window = new DefaultWindow("Red Hat Support Network");

        //VerticalPanel panel = new VerticalPanel();
        //panel.setStyleName("window-content");

        final TabPanel tabs = new TabPanel();
        tabs.setStyleName("default-tabpanel");

        String cspUrl = botstrapContext.getProperty(ApplicationProperties.CSP_API);
        Frame searchFrame = new Frame(cspUrl + "/search.html");
        searchFrame.setWidth("100%");
        searchFrame.setHeight("100%");

        searchFrame.addLoadHandler(new LoadHandler() {
            @Override
            public void onLoad(LoadEvent loadEvent) {
                int w = window.getOffsetWidth()-50;
                int h = window.getOffsetHeight()-160;
                tabs.getDeckPanel().setSize(w +"px", h +"px");
            }
        });

        Frame supportFrame = new Frame(cspUrl + "/support.html");
        supportFrame.setWidth("100%");
        supportFrame.setHeight("100%");


        tabs.add(searchFrame, "Knowledge Base");
        tabs.add(supportFrame, "Support Tickets");
        tabs.selectTab(0);

        ClickHandler submitHandler = new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                window.hide();
            }
        };

        DialogueOptions options = new DialogueOptions("Close Support",submitHandler, "Done",submitHandler).showCancel(false);

        window.trapWidget(new WindowContentBuilder(tabs, options).build());

        window.setGlassEnabled(true);

        window.center();


    }
}
