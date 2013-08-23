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

package org.jboss.as.console.client.core;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.layout.client.Layout;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DeckPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.PlaceRequest;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.ProductConfig;
import org.jboss.as.console.client.core.message.MessageBar;
import org.jboss.as.console.client.core.message.MessageCenter;
import org.jboss.as.console.client.core.message.MessageCenterView;
import org.jboss.as.console.client.rbac.RBACContextView;
import org.jboss.as.console.client.widgets.popups.DefaultPopup;
import org.jboss.ballroom.client.widgets.window.Feedback;

/**
 * Top level header, gives access to main applications.
 *
 * @author Heiko Braun
 * @date 1/28/11
 */
public class Header implements ValueChangeHandler<String> {

    private HTMLPanel linksPane;
    private String currentHighlightedSection = null;

    //private DeckPanel subnavigation;

    public static final String[][] SECTIONS = {
            new String[]{NameTokens.ProfileMgmtPresenter, Console.CONSTANTS.common_label_profiles()},
            new String[]{NameTokens.HostMgmtPresenter, "Hosts"},
            new String[]{NameTokens.DomainRuntimePresenter, "Runtime"},
            new String[]{NameTokens.AdministrationPresenter, Console.CONSTANTS.administration_label()},
    };

    public static final String[][] SECTIONS_STANADLONE = {
            new String[]{NameTokens.serverConfig, "Profile"},
            new String[]{NameTokens.StandaloneRuntimePresenter, "Runtime"},
            new String[]{NameTokens.AdministrationPresenter, Console.CONSTANTS.administration_label()},
    };

    private MessageBar messageBar;

    //private Map<String,Widget> appLinks = new HashMap<String, Widget>();

    private ProductConfig productConfig;
    private BootstrapContext bootstrap;
    private MessageCenter messageCenter;
    private PlaceManager placeManager;

    @Inject
    public Header(MessageCenter messageCenter, ProductConfig productConfig, BootstrapContext bootstrap,
                  PlaceManager placeManager) {
        this.messageBar = new MessageBar(messageCenter);
        this.productConfig = productConfig;
        this.bootstrap = bootstrap;
        this.messageCenter = messageCenter;
        this.placeManager = placeManager;
        History.addValueChangeHandler(this);

    }

    public Widget asWidget() {

        LayoutPanel outerLayout = new LayoutPanel();
        outerLayout.addStyleName("page-header");

        Widget logo = getProductSection();
        Widget links = getLinksSection();

        LayoutPanel line = new LayoutPanel();
        line.setStyleName("header-line");
        LayoutPanel top = new LayoutPanel();
        top.setStyleName("header-top");
        LayoutPanel bottom = new LayoutPanel();
        bottom.setStyleName("header-bottom");

        outerLayout.add(line);
        outerLayout.add(top);
        outerLayout.add(bottom);

        outerLayout.setWidgetTopHeight(line, 0, Style.Unit.PX, 4, Style.Unit.PX);
        outerLayout.setWidgetTopHeight(top, 4, Style.Unit.PX, 32, Style.Unit.PX);
        outerLayout.setWidgetTopHeight(bottom, 36, Style.Unit.PX, 44, Style.Unit.PX);

        top.add(logo);
        top.setWidgetLeftWidth(logo, 15, Style.Unit.PX, 500, Style.Unit.PX);
        top.setWidgetTopHeight(logo, 0, Style.Unit.PX, 32, Style.Unit.PX);

        bottom.add(links);

        // Debug tools
        VerticalPanel debugTools = new VerticalPanel();

        if(!GWT.isScript())
        {
            HTML rbac = new HTML("<i title='RBAC Diagnostics' style='cursor:pointer;color:#cecece;font-size:30px;font-weight:normal!important' class='icon-eye-open'></i>");
            debugTools.add(rbac);

            rbac.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    RBACContextView.launch();
                }
            });
        }

        bottom.add(debugTools);
        bottom.setWidgetLeftWidth(links, 0, Style.Unit.PX, 500, Style.Unit.PX);
        bottom.setWidgetTopHeight(links, 0, Style.Unit.PX, 44, Style.Unit.PX);

        bottom.setWidgetRightWidth(debugTools, 0, Style.Unit.PX, 50, Style.Unit.PX);
        bottom.setWidgetTopHeight(debugTools, 0, Style.Unit.PX, 44, Style.Unit.PX);


        HorizontalPanel tools = new HorizontalPanel();


        //messages
        MessageCenterView messageCenterView = new MessageCenterView(messageCenter);
        Widget messageCenter = messageCenterView.asWidget();
        tools.add(messageCenter);

        // user menu

        // current user
        String userHtml = "<i style='color:#cecece' class='icon-user'></i>&nbsp;"+Console.getBootstrapContext().getPrincipal();
        HTML roleHtml = new HTML("<i class='icon-tags'></i>&nbsp;"+Console.getBootstrapContext().getRole());
        SafeHtml principal = new SafeHtmlBuilder().appendHtmlConstant("<div class='header-textlink'>"+userHtml+"</div>").toSafeHtml();
        final HTML userButton = new HTML(principal);
        tools.add(userButton);

        final DefaultPopup menuPopup = new DefaultPopup(DefaultPopup.Arrow.TOP);

        ClickHandler clickHandler = new ClickHandler() {
            public void onClick(ClickEvent event) {

                int width = 120;
                int height = 50;

                menuPopup.setPopupPosition(
                        userButton.getAbsoluteLeft() - (width+2- userButton.getOffsetWidth()) ,
                        userButton.getAbsoluteTop() + 25
                );

                menuPopup.show();

                menuPopup.setWidth(width+"px");
                menuPopup.setHeight(height+"px");
            }
        };

        userButton.addClickHandler(clickHandler);
        HTML logoutHtml = new HTML("<i class='icon-signout'></i>&nbsp;" + Console.CONSTANTS.common_label_logout());
        logoutHtml.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                Feedback.confirm(
                        Console.CONSTANTS.common_label_logout(),
                        Console.CONSTANTS.logout_confirm(),
                        new Feedback.ConfirmationHandler() {
                            @Override
                            public void onConfirmation(boolean isConfirmed) {
                                if (isConfirmed) {
                                    new LogoutCmd().execute();
                                }
                            }
                        }
                );
            }
        });

        VerticalPanel usermenu = new VerticalPanel();
        usermenu.setStyleName("fill-layout-width");
        usermenu.add(roleHtml);
        usermenu.add(logoutHtml);
        menuPopup.setWidget(usermenu);

        top.add(tools);
        top.setWidgetRightWidth(tools, 15, Style.Unit.PX, 700, Style.Unit.PX);
        top.setWidgetTopHeight(tools, 0, Style.Unit.PX, 32, Style.Unit.PX);
        top.setWidgetHorizontalPosition(tools, Layout.Alignment.END);

        outerLayout.getElement().setAttribute("role", "navigation");
        outerLayout.getElement().setAttribute("aria-label", "Toplevel Categories");
        return outerLayout;
    }

    private Widget getProductSection() {

        HorizontalPanel panel = new HorizontalPanel();
        panel.getElement().setAttribute("role", "presentation");
        panel.getElement().setAttribute("aria-hidden", "true");

        HTML productName = new HTML(productConfig.getProductName());
        productName.setStyleName("header-product-name");
        panel.add(productName);

        HTML productVersion = new HTML(productConfig.getProductVersion());
        productVersion.setStyleName("header-product-version");
        panel.add(productVersion);

        return panel;
    }

    private Widget getLinksSection() {
        linksPane = new HTMLPanel(createLinks());
        linksPane.getElement().setId("header-links-section");
        linksPane.getElement().setAttribute("role", "menubar");
        linksPane.getElement().setAttribute("aria-controls", "main-content-area");

        String[][] sections = bootstrap.isStandalone() ? SECTIONS_STANADLONE : SECTIONS;

        for (String[] section : sections) {
            final String name = section[0];
            final String id = "header-" + name;

            SafeHtmlBuilder html = new SafeHtmlBuilder();
            html.appendHtmlConstant("<div class='header-link-label'>");
            html.appendHtmlConstant("<span role='menuitem'>");
            html.appendHtmlConstant(section[1]);
            html.appendHtmlConstant("</span>");
            html.appendHtmlConstant("</div>");
            HTML widget = new HTML(html.toSafeHtml());
            widget.setStyleName("fill-layout");

            widget.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    placeManager.revealPlace(
                            new PlaceRequest(name)
                    );
                }
            });
            linksPane.add(widget, id);

        }

        //subnavigation = createSubnavigation();
        //linksPane.add(subnavigation, "subnavigation");

        return linksPane;
    }

    private String createLinks() {

        String[][] sections = bootstrap.getProperty(BootstrapContext.STANDALONE).equals("true") ? SECTIONS_STANADLONE : SECTIONS;

        SafeHtmlBuilder headerString = new SafeHtmlBuilder();

        if(sections.length>0)
        {
            headerString.appendHtmlConstant("<table border=0 class='header-links' cellpadding=0 cellspacing=0 border=0>");
            headerString.appendHtmlConstant("<tr id='header-links-ref'>");

            headerString.appendHtmlConstant("<td><img src=\"images/blank.png\" width=1/></td>");
            for (String[] section : sections) {

                final String name = section[0];
                final String id = "header-" + name;
                String styleClass = "header-link";
                String styleAtt = "vertical-align:middle; text-align:center";

                String td =  "<td style='"+styleAtt+"' id='" + id +"' class='"+styleClass+"'></td>";

                headerString.appendHtmlConstant(td);
                //headerString.append(title);

                //headerString.appendHtmlConstant("<td ><img src=\"images/blank.png\" width=1 height=32/></td>");

            }

            headerString.appendHtmlConstant("</tr>");
            headerString.appendHtmlConstant("</table>");
            headerString.appendHtmlConstant("<div id='subnavigation' style='float:right;clear:right;'/>");
        }

        return headerString.toSafeHtml().asString();
    }

    @Override
    public void onValueChange(ValueChangeEvent<String> event) {
        String historyToken = event.getValue();
        if(historyToken.equals(currentHighlightedSection))
            return;
        else
            currentHighlightedSection = historyToken;

        if(historyToken.indexOf("/")!=-1)
        {
            highlight(historyToken.substring(0, historyToken.indexOf("/")));
        }
        else
        {
            highlight(historyToken);
        }
    }

    public void highlight(String name)
    {
        toggleSubnavigation(name);

        com.google.gwt.user.client.Element target = linksPane.getElementById("header-links-ref");
        if(target!=null) // standalone doesn't provide any top level links
        {
            NodeList<Node> childNodes = target.getChildNodes();
            for(int i=0; i<childNodes.getLength(); i++)
            {
                Node n = childNodes.getItem(i);
                if(Node.ELEMENT_NODE == n.getNodeType())
                {
                    Element element = (Element) n;
                    if(element.getId().equals("header-"+name))
                    {
                        element.addClassName("header-link-selected");
                        element.setAttribute("aria-selected", "true");
                    }
                    else {
                        element.removeClassName("header-link-selected");
                        element.setAttribute("aria-selected", "false");
                    }
                }
            }
        }

    }

    private void toggleSubnavigation(String name) {

    }

    public DeckPanel createSubnavigation() {

        DeckPanel subnavigation = new DeckPanel();

        // TODO: fill in contents

        return subnavigation;
    }
}
