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
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.widgets.popups.DefaultPopup;

public class CustomerSupportLauncher {


    private final PlaceManager placeManager;
    private HTML button;

    public CustomerSupportLauncher(PlaceManager placeManager) {
        this.placeManager = placeManager;
    }

    public Widget asWidget() {
        button = new HTML("<div class='header-textlink'>Red Hat Access &nbsp;<i style='color:#cecece' class='icon-angle-down'></i></div>");
        button.getElement().setAttribute("style", "cursor:pointer");
        button.addStyleName("csp-link");

        final DefaultPopup menuPopup = new DefaultPopup(DefaultPopup.Arrow.NONE);
        menuPopup.setAutoHideEnabled(true);
        ClickHandler clickHandler = new ClickHandler() {
            public void onClick(ClickEvent event) {

                int width = 160;
                int height = 70;

                menuPopup.setPopupPosition(
                        button.getAbsoluteLeft(),
                        button.getAbsoluteTop() + 25
                );

                menuPopup.show();

                menuPopup.setWidth(width+"px");
                menuPopup.setHeight(height+"px");
            }
        };

        button.addClickHandler(clickHandler);

        VerticalPanel supportMenu = new VerticalPanel();
        supportMenu.setStyleName("fill-layout-width");
        supportMenu.addStyleName("top-level-menu");

        HTML searchLink = new HTML("Search Customer Portal");
        searchLink.addStyleName("menu-item");
        searchLink.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                reveal(menuPopup, "search");
            }
        });

        HTML openCaseLink = new HTML("Open Case");
        openCaseLink.addStyleName("menu-item");
        openCaseLink.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                reveal(menuPopup, "open");
            }
        });

        HTML modifyCaseLink = new HTML("Modify Case");
        modifyCaseLink.addStyleName("menu-item");
        modifyCaseLink.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                reveal(menuPopup, "modify");
            }
        });

        supportMenu.add(searchLink);
        supportMenu.add(openCaseLink);
        supportMenu.add(modifyCaseLink);

        menuPopup.setWidget(supportMenu);

        return button;
    }

    private void reveal(DefaultPopup menuPopup, String token) {
        Console.MODULES.getHeader().highlight("csp");
        placeManager.revealPlace(new PlaceRequest(NameTokens.CSP).with("ref", token));
        menuPopup.hide();
    }

    public void highlight(String name) {
        if("csp".equals(name))
            button.addStyleName("csp-link-selected");
        else
            button.removeStyleName("csp-link-selected");
    }

    /*private void openCSPView() {
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


    }    */
}
